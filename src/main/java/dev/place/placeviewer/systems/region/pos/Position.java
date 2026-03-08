package dev.place.placeviewer.systems.region.pos;

import dev.place.placeviewer.systems.region.DimensionType;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record Position(int x, int z, @NotNull DimensionType dimensionType) {

    @NotNull
    public static Position chunkPosition(@NotNull final Location location) {
        return new Position(
            location.getBlockX() >> 4,
            location.getBlockZ() >> 4,
            DimensionType.dimensionType(location.getWorld().getEnvironment())
        );
    }

    @NotNull
    public static Position regionPosition(@NotNull final Position chunkPosition) {
        return new Position(chunkPosition.x() >> 5, chunkPosition.z() >> 5, chunkPosition.dimensionType());
    }

    @NotNull
    public static Position regionPosition(@NotNull final Location location) {
        return regionPosition(chunkPosition(location));
    }

    public double distance(@NotNull final Position other) {
        if (dimensionType != other.dimensionType) return Double.POSITIVE_INFINITY;

        final double dx = Math.abs((double) x - (double) other.x);
        final double dz = Math.abs((double) z - (double) other.z);
        return Math.sqrt(dx * dx + dz * dz);
    }

}
