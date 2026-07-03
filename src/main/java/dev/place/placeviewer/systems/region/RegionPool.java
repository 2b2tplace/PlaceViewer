package dev.place.placeviewer.systems.region;

import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.region.epoch.Epoch;
import dev.place.placeviewer.systems.protocol.PlaceViewerProtocol;
import dev.place.placeviewer.systems.region.epoch.EpochIndex;
import dev.place.placeviewer.systems.region.jni.NativeRegionException;
import dev.place.placeviewer.systems.region.pos.Position;
import dev.place.placeviewer.systems.region.pos.PositionEpoch;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

public class RegionPool {

    private record RegionEntry(@NotNull CompletableFuture<Region> future, @NotNull Map<UUID, Set<Long>> viewers) {

        public RegionEntry(@NotNull final CompletableFuture<Region> future) {
            this(future, new ConcurrentHashMap<>());
        }

    }

    private record ChunkEntry(@NotNull CompletableFuture<byte[]> future, @NotNull Position regionPosition, @NotNull Set<UUID> viewers) {

        public ChunkEntry(@NotNull final CompletableFuture<byte[]> future, @NotNull final Position regionPosition) {
            this(future, regionPosition, ConcurrentHashMap.newKeySet());
        }

    }

    @NotNull
    private final Map<Position, RegionEntry> regions = new ConcurrentHashMap<>();

    @NotNull
    private final Map<Position, Map<Epoch, ChunkEntry>> chunks = new ConcurrentHashMap<>();

    @NotNull
    private final Map<UUID, Epoch> playerEpochMap = new ConcurrentHashMap<>();

    @NotNull
    private final Map<UUID, LongOpenHashSet> playerSentChunksMap = new ConcurrentHashMap<>();

    @NotNull
    private final Map<UUID, LongHeapPriorityQueue> playerChunkSendQueueMap = new ConcurrentHashMap<>();

    @NotNull
    public CompletableFuture<byte[]> loadChunk(@NotNull final Position chunkPosition, @NotNull final Epoch epoch, @NotNull final UUID viewer) {
        final Position regionPosition = Position.regionPosition(chunkPosition);
        final RegionEntry regionEntry = regions.computeIfAbsent(
            regionPosition,
            pos -> new RegionEntry(loadRegion(pos))
        );
        final Map<Epoch, ChunkEntry> chunkEntries = chunks.computeIfAbsent(chunkPosition, p -> new ConcurrentHashMap<>());
        removeViewer(chunkPosition, epoch::equals, chunkEntries, viewer, false);

        final ChunkEntry chunkEntry = chunkEntries.computeIfAbsent(
            epoch,
            e -> new ChunkEntry(
                regionEntry.future.thenCompose(region -> loadChunk(region, chunkPosition, e)),
                regionPosition
            )
        );
        synchronized (regionEntry) {
            chunkEntry.viewers.add(viewer);
            regionEntry.viewers
                .computeIfAbsent(viewer, v -> ConcurrentHashMap.newKeySet())
                .add(chunkPosition.key());
        }
        return chunkEntry.future;
    }

    public void unloadChunk(@NotNull final Position chunkPosition, @NotNull final UUID viewer) {
        final Position regionPosition = Position.regionPosition(chunkPosition);

        final Map<Epoch, ChunkEntry> chunkEntries = chunks.get(chunkPosition);
        final RegionEntry regionEntry = regions.get(regionPosition);
        if (regionEntry == null) return;

        synchronized (regionEntry) {
            if (chunkEntries != null)
                removeViewer(chunkPosition, epoch -> false, chunkEntries, viewer, true);

            final Set<Long> viewedChunks = regionEntry.viewers.get(viewer);
            if (viewedChunks == null) return;

            viewedChunks.remove(chunkPosition.key());
            if (!viewedChunks.isEmpty()) return;

            regionEntry.viewers.remove(viewer);
            if (!regionEntry.viewers.isEmpty()) return;

            if (regions.remove(regionPosition, regionEntry))
                regionEntry.future.thenAccept(Region::close);
        }
    }

    private void removeViewer(@NotNull final Position chunkPosition, @NotNull final Predicate<Epoch> epochFilter,
                              @NotNull final Map<Epoch, ChunkEntry> chunkEntries,
                              @NotNull final UUID viewer, final boolean unloadChunk) {
        boolean allEmpty = true;
        for (final Map.Entry<Epoch, ChunkEntry> chunkEntry : chunkEntries.entrySet()) {
            if (epochFilter.test(chunkEntry.getKey())) {
                allEmpty = false;
                continue;
            }

            chunkEntry.getValue().viewers.remove(viewer);
            if (!chunkEntry.getValue().viewers.isEmpty()) allEmpty = false;
        }
        chunkEntries.entrySet().removeIf(e -> e.getValue().viewers.isEmpty());
        if (unloadChunk && allEmpty)
            chunks.remove(chunkPosition, chunkEntries);
    }

    @NotNull
    private CompletableFuture<Region> loadRegion(@NotNull final Position regionPosition) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Region.load(
                    PlaceViewer.config().parentDirectory(),
                    regionPosition.x(), regionPosition.z(),
                    regionPosition.dimensionType());
            } catch (final NativeRegionException e) {
                if (e.errorCode() == 0) return null; // FILE_NOT_FOUND should be silently ignored and treated as an empty region
                throw e;
            }
        });
    }

    @NotNull
    private CompletableFuture<byte[]> loadChunk(@Nullable final Region region, @NotNull final Position chunkPosition, @NotNull final Epoch epoch) {
        return CompletableFuture.supplyAsync(() -> {
            if (region == null) return null;

            final long timestamp = region.epochIndex(chunkPosition).closestTimestamp(epoch.timestamp());
            return region.createChunkDataPacket(chunkPosition.x(), chunkPosition.z(), timestamp);
        });
    }

    @Nullable
    public CompletableFuture<Region> regionAt(@NotNull final Position regionPos) {
        final RegionEntry entry = regions.get(regionPos);
        if (entry == null) return null;

        return entry.future;
    }

    @Nullable
    public CompletableFuture<byte[]> chunkAt(@NotNull final PositionEpoch chunkPos) {
        final Map<Epoch, ChunkEntry> entries = chunks.get(chunkPos.position());
        if (entries == null) return null;

        final ChunkEntry entry = entries.get(chunkPos.epoch());
        if (entry == null) return null;

        return entry.future;
    }

    public long selectedChunkEpoch(@NotNull final Position chunkPos, final long timestamp) {
        final CompletableFuture<Region> future = regionAt(Position.regionPosition(chunkPos));
        if (future == null || !future.isDone()) return timestamp;

        try {
            final Region region = future.get();
            if (region == null) return timestamp;

            return region.epochIndex(chunkPos).closestTimestamp(timestamp);
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static DimensionType dimensionType(@NotNull final ServerPlayer player) {
        return DimensionType.dimensionType(player.level().getWorld().getEnvironment());
    }

    public void unloadChunk(@NotNull final ServerPlayer player, final int chunkX, final int chunkZ) {
        unloadChunk(new Position(chunkX, chunkZ, dimensionType(player)), player.getUUID());
    }

    public void sendChunk(@NotNull final ServerPlayer player, final int chunkX, final int chunkZ) {
        sendChunk(player, chunkX, chunkZ, currentEpoch(player.getUUID()));
    }

    public void sendChunk(@NotNull final ServerPlayer player, final int chunkX, final int chunkZ, @NotNull final Epoch epoch) {
        final DimensionType dimensionType = dimensionType(player);
        loadChunk(new Position(chunkX, chunkZ, dimensionType), epoch, player.getUUID())
            .thenAcceptAsync(bytes -> {
                final Channel channel = player.connection.connection.channel;
                final ByteBuf packetBuf = channel.alloc().buffer();

                if (bytes == null) {
                    PlaceViewerProtocol.writeEmptyChunkData(packetBuf, chunkX, chunkZ, dimensionType);
                } else {
                    PlaceViewerProtocol.writeChunkData(packetBuf, bytes);
                }
                channel.writeAndFlush(packetBuf);
            })
            .exceptionally(err -> {
                PlaceViewer.LOGGER.error("Unable to send chunk {}, {} from {}", chunkX, chunkZ, epoch, err);
                return null;
            });
    }

    @NotNull
    public Epoch currentEpoch(@NotNull final UUID uuid) {
        return playerEpochMap.getOrDefault(uuid, Epoch.now());
    }

    public void sentChunks(@NotNull final UUID uuid, @NotNull final LongOpenHashSet chunkKeys) {
        playerSentChunksMap.put(uuid, chunkKeys);
    }

    public void chunkSendQueue(@NotNull final UUID uuid, @NotNull final LongHeapPriorityQueue chunkKeys) {
        playerChunkSendQueueMap.put(uuid, chunkKeys);
    }

    @NotNull
    public Epoch currentEpoch(@NotNull final Player player) {
        return currentEpoch(player.getUniqueId());
    }

    public void currentEpoch(@NotNull final Player player, @NotNull final Date date) {
        currentEpoch(player.getUniqueId(), date);
        sendActionBar(player);
    }

    public void currentEpoch(@NotNull final UUID uuid, @NotNull final Date date) {
        currentEpoch(uuid, Epoch.snapshot(date.getTime()));
    }

    public void currentEpoch(@NotNull final UUID uuid, @NotNull final Epoch epoch) {
        playerEpochMap.put(uuid, epoch);
    }

    public void reloadAllChunks(@NotNull final Player player) {
        final LongOpenHashSet sentChunks = playerSentChunksMap.get(player.getUniqueId());
        final LongHeapPriorityQueue sendChunkQueue = playerChunkSendQueueMap.get(player.getUniqueId());
        if (sentChunks == null || sendChunkQueue == null) return;

        final Position position = Position.chunkPosition(player.getLocation());
        final List<Position> sortedChunksByDistance = new ArrayList<>();
        for (final long reload : sentChunks) {
            final int chunkX = CoordinateUtils.getChunkX(reload);
            final int chunkZ = CoordinateUtils.getChunkZ(reload);
            sortedChunksByDistance.add(new Position(chunkX, chunkZ, position.dimensionType()));
        }
        sortedChunksByDistance.sort(Comparator.comparingDouble(pos -> pos.distanceSquared(position)));

        for (final Position chunkPos : sortedChunksByDistance) {
            final long chunkKey = CoordinateUtils.getChunkKey(chunkPos.x(), chunkPos.z());
            sentChunks.remove(chunkKey);
            sendChunkQueue.enqueue(chunkKey);
        }
    }

    @NotNull
    public Optional<Component> createActionBar(@NotNull final Player player) {
        final Epoch epoch = currentEpoch(player);
        return switch (epoch) {
            case Epoch.Now ignored -> Optional.empty();
            case Epoch.Snapshot snapshot -> {
                final Location location = player.getLocation();
                final Position chunkPos = Position.chunkPosition(location);

                final long timestamp = PlaceViewer.regionPool().selectedChunkEpoch(chunkPos, snapshot.timestamp());
                final String formattedTime = EpochIndex.FORMAT.format(Date.from(Instant.ofEpochMilli(timestamp)));
                yield Optional.of(Component.text("Viewing snapshot from " + formattedTime, NamedTextColor.GRAY));
            }
        };
    }

    public void sendActionBar(@NotNull final Player player) {
        createActionBar(player).ifPresent(player::sendActionBar);
    }

    public void remove(@NotNull final Player player) {
        remove(player.getUniqueId());
    }

    public void remove(@NotNull final UUID uuid) {
        playerEpochMap.remove(uuid);
        playerSentChunksMap.remove(uuid);
        playerChunkSendQueueMap.remove(uuid);
    }

}
