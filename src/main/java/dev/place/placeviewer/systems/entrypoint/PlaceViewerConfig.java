package dev.place.placeviewer.systems.entrypoint;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.Objects;

public class PlaceViewerConfig extends YamlConfiguration {

    @NotNull
    private static final String FILENAME = "placeviewer.yml";

    public PlaceViewerConfig() {
        load();
    }

    public void load() {
        try (final Reader reader = defaultReader()) {
            setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (final IOException e) {
            PlaceViewer.LOGGER.error("Could not initialize default " + FILENAME + " file", e);
        }
        try {
            load(new File(FILENAME));
        } catch (final FileNotFoundException ignored) {
            try (final Reader reader = defaultReader()) {
                load(reader);
                save(FILENAME);
            } catch (final IOException | InvalidConfigurationException e) {
                PlaceViewer.LOGGER.error("Could not initialize default " + FILENAME + " file", e);
            }
        } catch (final IOException | InvalidConfigurationException e) {
            PlaceViewer.LOGGER.error("Could not load " + FILENAME + " file", e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static Reader defaultReader() {
        final InputStream in = Objects.requireNonNull(PlaceViewerConfig.class.getClassLoader().getResourceAsStream("placeviewer-default.yml"));
        return new InputStreamReader(in);
    }

    @NotNull
    public String parentDirectory() {
        return Objects.requireNonNull(getString("paths.zvcr-parent-directory"));
    }

    @NotNull
    public String registryDirectory() {
        return Objects.requireNonNull(getString("paths.registry-directory"));
    }

    public int maxSendRate() {
        return getInt("ratelimiter.max-send-rate");
    }

    public long regionCacheTimeout() {
        return getLong("region-cache.region-cache-timeout-millis");
    }

    public long chunkCacheTimeout() {
        return getLong("region-cache.chunk-cache-timeout-millis");
    }

    @NotNull
    public List<String> allowedCommands() {
        return getStringList("commands.allowed-commands");
    }

    public boolean hideOnlinePlayers() {
        return getBoolean("traffic.hide-online-players");
    }

}
