package dev.place.placeviewer.systems.region;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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
        return Arrays.stream(values())
            .filter(dimensionType -> dimensionType.toString().equals(string))
            .findAny();
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
