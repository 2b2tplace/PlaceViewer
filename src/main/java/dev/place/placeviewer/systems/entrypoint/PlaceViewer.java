package dev.place.placeviewer.systems.entrypoint;

import dev.place.placeviewer.systems.flashback.EpochPool;
import dev.place.placeviewer.systems.region.RegionPool;
import dev.place.placeviewer.systems.region.jni.NativeRegion;
import dev.place.placeviewer.systems.region.jni.NativeRegionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;

public final class PlaceViewer {

    @Nullable
    private static PlaceViewerConfig CONFIG;

    @Nullable
    private static RegionPool REGION_POOL;

    @NotNull
    private static final EpochPool EPOCH_POOL = new EpochPool();

    @NotNull
    public static final PlaceViewerDummyPlugin PLUGIN = new PlaceViewerDummyPlugin();

    @NotNull
    public static final Logger LOGGER = LoggerFactory.getLogger(PlaceViewer.class.getSimpleName());

    private PlaceViewer() {}

    public static void initialize() {
        if (PlaceViewerLibrary.isLoaded()) {
            LOGGER.info("PlaceViewer native libraries have been successfully loaded.");
        } else {
            throw new IllegalStateException("PlaceViewer could not be loaded, shutting down server.");
        }
        CONFIG = new PlaceViewerConfig();
        try {
            NativeRegion.initialize(config().registryDirectory());
        } catch (final NativeRegionException e) {
            throw new IllegalStateException("Unable to load registries, shutting down server.");
        }

        REGION_POOL = new RegionPool(
            Duration.ofMillis(PlaceViewer.config().regionCacheTimeout()),
            Duration.ofMillis(PlaceViewer.config().chunkCacheTimeout())
        );
        PlaceViewerManager.registerAll();
    }

    @NotNull
    public static RegionPool regionPool() {
        return Objects.requireNonNull(REGION_POOL);
    }

    @NotNull
    public static EpochPool epochPool() {
        return Objects.requireNonNull(EPOCH_POOL);
    }

    @NotNull
    public static PlaceViewerConfig config() {
        return Objects.requireNonNull(CONFIG);
    }

}
