package dev.place.placeviewer.systems.region;

import dev.place.placeviewer.systems.region.epoch.EpochIndex;
import dev.place.placeviewer.systems.region.jni.NativeRegion;
import dev.place.placeviewer.systems.region.jni.NativeRegionException;
import dev.place.placeviewer.systems.region.pos.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Region implements AutoCloseable {

    private long regionObjectID;
    private long @Nullable [] indexedTimestamps;

    @NotNull
    private final Map<Integer, EpochIndex> chunkEpochIndices = new ConcurrentHashMap<>();

    @NotNull
    private final String parentDirectory;
    private final int regionX;
    private final int regionZ;

    @NotNull
    private final DimensionType dimensionType;

    public Region(final long regionObjectID, @NotNull final String parentDirectory,
                  final int regionX, final int regionZ, @NotNull final DimensionType dimensionType) {
        this.regionObjectID = regionObjectID;
        this.parentDirectory = parentDirectory;
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.dimensionType = dimensionType;

        // Lazy initialization: only create EpochIndex objects when needed
    }

    private Region(@NotNull final String parentDirectory, final int regionX, final int regionZ,
                   @NotNull final DimensionType dimensionType) {
        this(NativeRegion.newRegionFromDisk(parentDirectory, regionX, regionZ, dimensionType.id()),
            parentDirectory, regionX, regionZ, dimensionType);

        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                final int key = 32 * x + z;
                indexedTimestamps = NativeRegion.indexRegionEpochs(regionObjectID, x, z);
                final EpochIndex chunkEpochIndex = chunkEpochIndices.computeIfAbsent(key, k -> new EpochIndex());

                if (indexedTimestamps != null) {
                    final List<Date> dates = EpochIndex.dates(indexedTimestamps);
                    chunkEpochIndex.dates(dates);
                    chunkEpochIndex.indexDates();
                }
            }
        }
    }

    @NotNull
    public EpochIndex epochIndex(@NotNull final Position absChunkPos) {
        return epochIndex(absChunkPos.x(), absChunkPos.z());
    }

    @NotNull
    public EpochIndex epochIndex(final int absChunkX, final int absChunkZ) {
        final int x = absChunkX & 31;
        final int z = absChunkZ & 31;
        return chunkEpochIndices.computeIfAbsent(32 * x + z, k -> new EpochIndex());
    }

    @NotNull
    public static Optional<Region> tryLoad(@NotNull final String parentDirectory, final int regionX, final int regionZ,
                                           @NotNull final DimensionType dimensionType) {
        final Region region = new Region(parentDirectory, regionX, regionZ, dimensionType);
        if (region.regionObjectID == 0L) return Optional.empty();

        return Optional.of(region);
    }

    @NotNull
    public static Region load(@NotNull final String parentDirectory, final int regionX, final int regionZ,
                              @NotNull final DimensionType dimensionType) {
        return tryLoad(parentDirectory, regionX, regionZ, dimensionType)
            .orElseThrow(() -> new NativeRegionException("Failed to load zvcr region file from disk for an unknown reason", -1));
    }

    @NotNull
    public static Region load(@NotNull final Path parentDirectory, final int regionX, final int regionZ,
                              @NotNull final DimensionType dimensionType) {
        return load(parentDirectory.toString(), regionX, regionZ, dimensionType);
    }

    @NotNull
    public static Region load(@NotNull final File parentDirectory, final int regionX, final int regionZ,
                              @NotNull final DimensionType dimensionType) {
        return load(parentDirectory.toPath(), regionX, regionZ, dimensionType);
    }

    public byte @Nullable [] createChunkDataPacket(final int absChunkX, final int absChunkZ, final long timestampMillis) {
        return NativeRegion.createChunkDataPacket(regionObjectID, dimensionType.id(), absChunkX, absChunkZ, timestampMillis / 1000L);
    }

    public void close() {
        NativeRegion.freeRegion(regionObjectID);
        regionObjectID = 0L;
    }

    public long regionObjectID() {
        return regionObjectID;
    }

    public long @Nullable [] indexedTimestamps() {
        return indexedTimestamps;
    }

    @NotNull
    public String parentDirectory() {
        return parentDirectory;
    }

    public int regionX() {
        return regionX;
    }

    public int regionZ() {
        return regionZ;
    }

    @NotNull
    public DimensionType dimensionType() {
        return dimensionType;
    }


}
