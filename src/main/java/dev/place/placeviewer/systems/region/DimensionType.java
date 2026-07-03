package dev.place.placeviewer.systems.region;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum DimensionType {

    OVERWORLD("overworld", 0),
    NETHER("nether", 1),
    THE_END("the_end", 2);

    @NotNull
    private final String name;

    private final int id;

    DimensionType(@NotNull final String name, final int id) {
        this.name = name;
        this.id = id;
    }

    public int id() {
        return id;
    }

    @NotNull
    public String toString() {
        return name;
    }

    @NotNull
    public static Optional<DimensionType> ofString(@NotNull final String string) {
        for (final DimensionType dimensionType : values()) {
            if (dimensionType.name.equals(string)) return Optional.of(dimensionType);
        }
        return Optional.empty();
    }

    @Nullable
    private volatile World cachedWorld;
    private volatile boolean worldCached = false;

    @NotNull
    public Optional<World> world() {
        if (worldCached) return Optional.ofNullable(cachedWorld);
        cachedWorld = Bukkit.getServer()
            .getWorlds()
            .stream()
            .filter(w -> DimensionType.dimensionType(w.getEnvironment()) == this)
            .findFirst()
            .orElse(null);
        worldCached = true;
        return Optional.ofNullable(cachedWorld);
    }

    @NotNull
    public static DimensionType dimensionType(@NotNull final World.Environment dimensionType) {
        return switch (dimensionType) {
            case NETHER -> DimensionType.NETHER;
            case THE_END -> DimensionType.THE_END;
            default -> DimensionType.OVERWORLD;
        };
    }

    @NotNull
    public World.Environment environment() {
        return switch (this) {
            case NETHER -> World.Environment.NETHER;
            case THE_END -> World.Environment.THE_END;
            default -> World.Environment.NORMAL;
        };
    }

}
