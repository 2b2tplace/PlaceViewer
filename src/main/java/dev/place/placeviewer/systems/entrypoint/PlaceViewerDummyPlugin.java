package dev.place.placeviewer.systems.entrypoint;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

public class PlaceViewerDummyPlugin implements Plugin {

    @NotNull
    private static final PluginDescriptionFile DESCRIPTION_DUMMY = new PluginDescriptionFile("PlaceViewerDummy", "1.0.0", PlaceViewerDummyPlugin.class.getName());

    @NotNull
    public File getDataFolder() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public PluginDescriptionFile getDescription() {
        return DESCRIPTION_DUMMY;
    }

    @NotNull
    public PluginMeta getPluginMeta() {
        return DESCRIPTION_DUMMY;
    }

    @NotNull
    public FileConfiguration getConfig() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public InputStream getResource(@NotNull final String s) {
        throw new UnsupportedOperationException();
    }

    public void saveConfig() {
        throw new UnsupportedOperationException();
    }

    public void saveDefaultConfig() {
        throw new UnsupportedOperationException();
    }

    public void saveResource(@NotNull final String s, final boolean b) {
        throw new UnsupportedOperationException();
    }

    public void reloadConfig() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public PluginLoader getPluginLoader() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public Server getServer() {
        throw new UnsupportedOperationException();
    }

    public boolean isEnabled() {
        return true;
    }

    public void onDisable() {
        throw new UnsupportedOperationException();
    }

    public void onLoad() {
        throw new UnsupportedOperationException();
    }

    public void onEnable() {
        throw new UnsupportedOperationException();
    }

    public boolean isNaggable() {
        return false;
    }

    public void setNaggable(final boolean b) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public ChunkGenerator getDefaultWorldGenerator(@NotNull final String s, @Nullable final String s1) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public BiomeProvider getDefaultBiomeProvider(@NotNull final String s, @Nullable final String s1) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public Logger getLogger() {
        return Logger.getLogger(PlaceViewer.LOGGER.getName());
    }

    @NotNull
    public String getName() {
        return DESCRIPTION_DUMMY.getName();
    }

    @NotNull
    public LifecycleEventManager<Plugin> getLifecycleManager() {
        throw new UnsupportedOperationException();
    }

    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String s, final @NotNull String @NotNull [] strings) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public List<String> onTabComplete(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String s, @NotNull final String @NotNull [] strings) {
        throw new UnsupportedOperationException();
    }
}
