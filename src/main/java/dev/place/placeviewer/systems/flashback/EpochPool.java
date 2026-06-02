package dev.place.placeviewer.systems.flashback;

import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.region.pos.Position;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EpochPool {

    @NotNull
    private final Map<UUID, Epoch> playerEpochMap = new ConcurrentHashMap<>();

    @NotNull
    private final Map<UUID, LongOpenHashSet> playerSentChunksMap = new ConcurrentHashMap<>();

    @NotNull
    private final Map<UUID, LongHeapPriorityQueue> playerChunkSendQueueMap = new ConcurrentHashMap<>();

    @NotNull
    public Map<UUID, Epoch> playerEpochMap() {
        return playerEpochMap;
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
        sortedChunksByDistance.sort(Comparator.comparingDouble(pos -> pos.distance(position)));

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
