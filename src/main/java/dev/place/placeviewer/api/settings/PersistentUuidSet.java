package dev.place.placeviewer.api.settings;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A {@link Set} of {@link UUID}s backed by a single flat file (one UUID per line).
 * <p>
 * Unlike {@link PersistentUserSettings}, the whole set is rewritten on every save, so removals
 * are not left behind as stale files. This makes it the right fit for simple opt-in/opt-out toggles.
 */
public final class PersistentUuidSet {

    @NotNull
    private final Set<UUID> uuids = ConcurrentHashMap.newKeySet();

    @NotNull
    private final String filename;

    public PersistentUuidSet(@NotNull final String filename) {
        this.filename = filename;
    }

    public boolean contains(@NotNull final UUID uuid) {
        return uuids.contains(uuid);
    }

    public boolean toggle(@NotNull final UUID uuid) {
        if (uuids.remove(uuid)) return false;
        uuids.add(uuid);
        return true;
    }

    @NotNull
    private File file() {
        return new File(Bukkit.getWorldContainer(), filename);
    }

    public void load() {
        uuids.clear();
        final File file = file();
        if (!file.exists()) return;
        try {
            Files.readAllLines(file.toPath()).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(UUID::fromString)
                .forEach(uuids::add);
        } catch (final IOException e) {
            PlaceViewer.LOGGER.error("Unable to read {}", filename, e);
        } catch (final Exception e) {
            PlaceViewer.LOGGER.error("Unable to parse {}", filename, e);
        }
    }

    public void save() {
        try {
            Files.writeString(file().toPath(), uuids.stream().map(UUID::toString).collect(Collectors.joining("\n")));
        } catch (final IOException e) {
            PlaceViewer.LOGGER.error("Unable to save {}", filename, e);
        }
    }

}
