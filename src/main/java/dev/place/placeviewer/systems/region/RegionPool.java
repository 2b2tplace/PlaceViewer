package dev.place.placeviewer.systems.region;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.protocol.PlaceViewerProtocol;
import dev.place.placeviewer.systems.region.jni.NativeRegionException;
import dev.place.placeviewer.systems.flashback.Epoch;
import dev.place.placeviewer.systems.region.pos.Position;
import dev.place.placeviewer.systems.region.pos.PositionEpoch;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RegionPool {

    private static final class FuturePool<K, V> {

        @NotNull
        private final Cache<K, CompletableFuture<V>> cache;

        private FuturePool(@NotNull Cache<K, CompletableFuture<V>> cache) {
            this.cache = cache;
        }

        public FuturePool(@NotNull final Duration timeout) {
            this(timeout, null, null);
        }

        public FuturePool(@NotNull final Duration timeout,
                          @Nullable final Consumer<V> cleanup, @Nullable final Predicate<K> cancelCleanup) {
            cache = Caffeine.newBuilder()
                .expireAfterAccess(timeout)
                .scheduler(Scheduler.systemScheduler())
                .removalListener((key, futureObject, cause) -> {
                    try {
                        if (key == null || futureObject == null) return;

                        onRemoval((K) key, (CompletableFuture<V>) futureObject, cause, cleanup, cancelCleanup);
                    } catch (final Throwable t) {
                        PlaceViewer.LOGGER.info("Unable to remove value from FuturePool", t);
                    }
                })
                .build();
        }

        private void onRemoval(@NotNull final K key, @NotNull final CompletableFuture<V> future, @NotNull final RemovalCause cause,
                               @Nullable final Consumer<V> cleanup, @Nullable final Predicate<K> cancelCleanup) throws ExecutionException, InterruptedException {
            if (cause == RemovalCause.EXPIRED && cancelCleanup != null && cancelCleanup.test(key)) {
                cache.put(key, future);
                return;
            }
            if (cleanup == null) return;

            final V value = future.get();
            if (value != null) cleanup.accept(value);
        }

        @NotNull
        public CompletableFuture<V> get(@NotNull final K key, @NotNull final Function<? super K, ? extends CompletableFuture<V>> loader) {
            return Objects.requireNonNull(cache.get(key, loader));
        }

        @NotNull
        public CompletableFuture<V> get(@NotNull final K key, @NotNull final Supplier<V> loader) {
            return get(key, k -> CompletableFuture.supplyAsync(loader));
        }

        @NotNull
        public Cache<K, CompletableFuture<V>> cache() {
            return cache;
        }

    }

    private FuturePool<Position, Region> regionCache = null;

    @NotNull
    private final FuturePool<PositionEpoch, byte[]> chunkPacketCache;

    public RegionPool(@NotNull final Duration regionCacheTimeout, @NotNull final Duration chunkCacheTimeout) {
        regionCache = new FuturePool<>(regionCacheTimeout, Region::close, RegionPool::findPlayersNearby);
        chunkPacketCache = new FuturePool<>(chunkCacheTimeout, null, chunkPos -> findPlayersNearby(Position.regionPosition(chunkPos.position())));
    }

    public static boolean findPlayersNearby(@NotNull final Position regionPosition) {
        final Optional<World> worldOptional = Bukkit.getWorlds()
            .stream()
            .filter(world -> world.getEnvironment() == regionPosition.dimensionType().environment())
            .findFirst();

        return worldOptional.map(world -> new ArrayList<>(world.getPlayers())
                .stream()
                .anyMatch(p -> Position.regionPosition(p.getLocation()).distance(regionPosition) < 1))
            .orElse(false);
    }

    @NotNull
    public static Region load(final int regionX, final int regionZ, @NotNull final DimensionType dimensionType) {
        return Region.load(PlaceViewer.config().parentDirectory(), regionX, regionZ, dimensionType);
    }

    @Nullable
    public static Region loadOrEmpty(final int regionX, final int regionZ, @NotNull final DimensionType dimensionType) {
        try {
            return load(regionX, regionZ, dimensionType);
        } catch (final NativeRegionException e) {
            if (e.errorCode() == 0) return null; // FILE_NOT_FOUND should be silently ignored and treated as an empty region
            throw e;
        }
    }

    @NotNull
    public CompletableFuture<Region> regionAt(@NotNull final Position regionPos) {
        return regionCache.get(regionPos, () -> loadOrEmpty(regionPos.x(), regionPos.z(), regionPos.dimensionType()));
    }

    @NotNull
    public CompletableFuture<byte[]> chunkAt(@NotNull final PositionEpoch chunkPos) {
        return chunkPacketCache.get(chunkPos, key -> regionAt(Position.regionPosition(chunkPos.position()))
            .thenApplyAsync(region -> {
                if (region == null) return null;

                final long timestamp = region.epochIndex(chunkPos.position()).closestTimestamp(chunkPos.epoch().timestamp());
                final byte[] result = region.createChunkDataPacket(chunkPos.x(), chunkPos.z(), timestamp);
                region.release();
                return result;
            }));
    }

    public long selectedChunkEpoch(@NotNull final Position chunkPos, final long timestamp) {
        final CompletableFuture<Region> optionalRegion = regionCache.cache.getIfPresent(Position.regionPosition(chunkPos));
        if (optionalRegion == null || !optionalRegion.isDone()) return timestamp;

        try {
            return optionalRegion.get().epochIndex(chunkPos).closestTimestamp(timestamp);
        } catch (final InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendChunk(@NotNull final ServerPlayer player, final int chunkX, final int chunkZ) {
        PlaceViewer.regionPool().sendChunk(player, chunkX, chunkZ, PlaceViewer.epochPool().currentEpoch(player.getUUID()));
    }

    public void sendChunk(@NotNull final ServerPlayer player, final int chunkX, final int chunkZ, @NotNull final Epoch epoch) {
        final DimensionType dimensionType = DimensionType.dimensionType(player.level().getWorld().getEnvironment());
        chunkAt(new PositionEpoch(new Position(chunkX, chunkZ, dimensionType), epoch))
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
}
