package dev.place.placeviewer.systems.entrypoint;

import dev.place.placeviewer.api.chat.ServerChat;
import dev.place.placeviewer.api.chat.message.SystemMessage;
import dev.place.placeviewer.systems.command.HereCommand;
import dev.place.placeviewer.systems.command.IgnoreCommand;
import dev.place.placeviewer.systems.command.TpaCommand;
import dev.place.placeviewer.systems.region.RegionPool;
import dev.place.placeviewer.systems.region.jni.NativeRegion;
import dev.place.placeviewer.systems.region.jni.NativeRegionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceViewer {

    @Nullable
    private static PlaceViewerConfig CONFIG;

    @NotNull
    private static final RegionPool REGION_POOL = new RegionPool();

    @NotNull
    public static final PlaceViewerDummyPlugin PLUGIN = new PlaceViewerDummyPlugin();

    @NotNull
    public static final Logger LOGGER = LoggerFactory.getLogger(PlaceViewer.class.getSimpleName());

    @NotNull
    private static final Set<String> REGISTERED_COMMANDS = ConcurrentHashMap.newKeySet();

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
        PlaceViewerManager.registerAll();
        IgnoreCommand.loadUserSettings();
        HereCommand.loadUserSettings();
        TpaCommand.loadUserSettings();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, () -> Bukkit.getOnlinePlayers().forEach(REGION_POOL::sendActionBar), 40L, 40L);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, NativeRegion::mallocTrim, 2400L, 2400L);

        for (final var announcement : config().announcerConfig().announcements()) {
            final long interval = announcement.intervalTicks();
            Bukkit.getScheduler().scheduleSyncRepeatingTask(PLUGIN, () -> {
                final Component message = announcement.hasUrl()
                    ? announcement.message().append(Component.text(announcement.url())
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(announcement.url())))
                    : announcement.message();
                ServerChat.publicMessage(SystemMessage.system(message));
            }, interval, interval);
        }
    }

    public static void shutdown() {
        IgnoreCommand.saveUserSettings();
        HereCommand.saveUserSettings();
        TpaCommand.saveUserSettings();
    }

    @NotNull
    public static RegionPool regionPool() {
        return Objects.requireNonNull(REGION_POOL);
    }

    @NotNull
    public static PlaceViewerConfig config() {
        return Objects.requireNonNull(CONFIG);
    }

    public static void register(@NotNull final BukkitCommand command) {
        REGISTERED_COMMANDS.add(command.getName());
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
        return REGISTERED_COMMANDS.contains(commandName);
    }

}
