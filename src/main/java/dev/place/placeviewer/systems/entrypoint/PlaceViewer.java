package dev.place.placeviewer.systems.entrypoint;

import dev.place.placeviewer.systems.flashback.EpochPool;
import dev.place.placeviewer.systems.region.RegionPool;
import dev.place.placeviewer.systems.region.jni.NativeRegion;
import dev.place.placeviewer.systems.region.jni.NativeRegionException;
import org.bukkit.Bukkit;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    @NotNull
    private static final List<BukkitCommand> REGISTERED_COMMANDS = new ArrayList<>();

    @NotNull
    public static final String COMMAND_FALLBACK_PREFIX = "placeviewer";

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
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, () -> Bukkit.getOnlinePlayers().forEach(EPOCH_POOL::sendActionBar), 40L, 40L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, NativeRegion::mallocTrim, 2400L, 2400L);
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

    public static void register(@NotNull final BukkitCommand command) {
        REGISTERED_COMMANDS.add(command);
    }

    @NotNull
    public static String stripCommandPrefix(@NotNull final String commandName) {
        return commandName.startsWith(COMMAND_FALLBACK_PREFIX + ":")
            ? commandName.substring(COMMAND_FALLBACK_PREFIX.length() + 1)
            : commandName;
    }

    @NotNull
    public static String prependCommandPrefix(@NotNull final String commandName) {
        return COMMAND_FALLBACK_PREFIX + ":" + commandName;
    }

    public static boolean isRegisteredCommand(@NotNull final String commandName) {
        return REGISTERED_COMMANDS.stream()
            .anyMatch(command -> command.getName().equals(commandName));
    }

}
