package dev.place.placeviewer.api.settings;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public interface PersistentUserSettings<T> {

    @NotNull
    String container();

    @NotNull
    String settingsExtension();

    @NotNull
    Map<UUID, T> userSettings();

    @NotNull
    default File containerFile() {
        return new File(Bukkit.getWorldContainer(), container());
    }

    @Nullable
    String saveSingleUser(@NotNull final UUID uuid, @NotNull final T t);

    @NotNull
    T loadSingleUser(@NotNull final UUID uuid, @NotNull final String saved);

    default void saveUserSettings() {
        final File container = containerFile();
        if (!container.exists() && !container.mkdirs()) {
            PlaceViewer.LOGGER.error("Unable to create settings container directory {} at {}", container(), getClass().getName());
            return;
        }
        for (final Map.Entry<UUID, T> userSettings : userSettings().entrySet()) {
            final UUID uuid = userSettings.getKey();
            try {
                final String serializedSettings = saveSingleUser(uuid, userSettings.getValue());
                if (serializedSettings == null) continue;

                Files.writeString(new File(container, uuid + "." + settingsExtension()).toPath(), serializedSettings);
            } catch (final IOException e) {
                PlaceViewer.LOGGER.error("Unable to save settings at {} for user {}", getClass().getName(), uuid, e);
            } catch (final Exception e) {
                PlaceViewer.LOGGER.error("Unable to serialize settings at {} for user {}", getClass().getName(), uuid, e);
            }
        }
    }

    @NotNull
    default Map<UUID, T> loadUserSettings() {
        final Map<UUID, T> result = new HashMap<>();
        final File[] files = Optional.ofNullable(containerFile().listFiles()).orElse(new File[0]);

        for (final File file : files) {
            final String filename = file.getName();
            if (!filename.endsWith("." + settingsExtension())) continue;

            final String strippedFilename = filename.substring(0, filename.length() - settingsExtension().length() - 1);
            final UUID uuid = UUID.fromString(strippedFilename);

            try {
                final String content = Files.readString(file.toPath());
                final T userSettings = loadSingleUser(uuid, content);

                result.put(uuid, userSettings);
            } catch (final IOException e) {
                PlaceViewer.LOGGER.error("Unable to read settings at {} for user {}", getClass().getName(), uuid, e);
            } catch (final Exception e) {
                PlaceViewer.LOGGER.error("Unable to deserialize settings at {} for user {}", getClass().getName(), uuid, e);
            }
        }
        return result;
    }

}

