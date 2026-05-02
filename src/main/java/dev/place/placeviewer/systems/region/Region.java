package dev.place.placeviewer.systems.region;

import dev.place.placeviewer.systems.flashback.EpochIndex;
import dev.place.placeviewer.systems.region.jni.NativeRegion;
import dev.place.placeviewer.systems.region.jni.NativeRegionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Region implements AutoCloseable {

    @NotNull
    private final AtomicBoolean force = new AtomicBoolean(true);

    private long regionObjectID;
    private long @Nullable [] indexedTimestamps;

    @NotNull
    private final EpochIndex epochIndex = new EpochIndex();

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
    }

    private Region(@NotNull final String parentDirectory, final int regionX, final int regionZ,
                   @NotNull final DimensionType dimensionType) {
        this(NativeRegion.newRegionFromDisk(parentDirectory, regionX, regionZ, dimensionType.id()),
            parentDirectory, regionX, regionZ, dimensionType);
        indexedTimestamps = NativeRegion.indexRegionEpochs(regionObjectID);

        if (indexedTimestamps != null)
            epochIndex.dates(indexedTimestamps);
    }

    @NotNull
    public EpochIndex epochIndex() {
        return epochIndex;
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

    public void release() {
        force.set(false);
    }

    public void close() {
        if (force.get()) return;

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
