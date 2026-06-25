package dev.place.placeviewer.systems.entrypoint;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.List;
import java.util.Objects;

public class PlaceViewerConfig extends YamlConfiguration {

    @NotNull
    private static final String FILENAME = "placeviewer.yml";

    @NotNull
    private final AntiSpamConfig antiSpamConfig = new AntiSpamConfig();

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

    @NotNull
    public List<String> allowedCommands() {
        return getStringList("commands.allowed-commands");
    }

    public boolean hideOnlinePlayers() {
        return getBoolean("traffic.hide-online-players");
    }

    @NotNull
    public AntiSpamConfig antiSpamConfig() {
        return antiSpamConfig;
    }

    public class AntiSpamConfig {

        @NotNull
        public static final String ANTISPAM_PREFIX = "antispam.";

        public int maxBaselineViolations() {
            return getInt(ANTISPAM_PREFIX + "max-baseline-violations");
        }

        public int maxStrictViolations() {
            return getInt(ANTISPAM_PREFIX + "max-strict-violations");
        }

        public int maxFallbackViolations() {
            return getInt(ANTISPAM_PREFIX + "max-fallback-violations");
        }

        public double baselineMinSpamConfidence() {
            return getDouble(ANTISPAM_PREFIX + "baseline-minimum-spam-confidence");
        }

        public double strictMinSpamConfidence() {
            return getDouble(ANTISPAM_PREFIX + "strict-minimum-spam-confidence");
        }

        public double fallbackMinSpamConfidence() {
            return getDouble(ANTISPAM_PREFIX + "fallback-minimum-spam-confidence");
        }

        public long baselineRatelimitMillis() {
            return getLong(ANTISPAM_PREFIX + "baseline-ratelimit-millis");
        }

        public long strictRatelimitMillis() {
            return getLong(ANTISPAM_PREFIX + "strict-ratelimit-millis");
        }

        public long spamToleranceMillis() {
            return getLong(ANTISPAM_PREFIX + "spam-tolerance-millis");
        }

        public int lengthToleranceCharcount() {
            return getInt(ANTISPAM_PREFIX + "length-tolerance-charcount");
        }

        @NotNull
        public Component kickMessage() {
            final String kickMessage = getString(ANTISPAM_PREFIX + "kick-message");
            if (kickMessage == null || kickMessage.isBlank())
                return Component.text("Disconnected");

            return LegacyComponentSerializer.legacyAmpersand().deserialize(kickMessage);
        }

        @Nullable
        public Component warnMessage() {
            final String warnMessage = getString(ANTISPAM_PREFIX + "warn-message");
            if (warnMessage == null || warnMessage.isBlank()) return null;

            return LegacyComponentSerializer.legacyAmpersand().deserialize(warnMessage);
        }

        @Nullable
        public Component warnHoverMessage() {
            final String warnHoverMessage = getString(ANTISPAM_PREFIX + "warn-hover-message");
            if (warnHoverMessage == null || warnHoverMessage.isBlank()) return null;

            return LegacyComponentSerializer.legacyAmpersand().deserialize(warnHoverMessage);
        }

        public boolean strictKick() {
            return getBoolean(ANTISPAM_PREFIX + "strict-kick");
        }

        public boolean filterPublicMessages() {
            return getBoolean(ANTISPAM_PREFIX + "filter-public-messages");
        }

        public boolean filterWhisperMessages() {
            return getBoolean(ANTISPAM_PREFIX + "filter-whisper-messages");
        }

    }

}
