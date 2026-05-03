package dev.place.placeviewer.systems.region.pos;

import dev.place.placeviewer.systems.flashback.Epoch;
import dev.place.placeviewer.systems.region.DimensionType;
import org.jetbrains.annotations.NotNull;

public record PositionEpoch(@NotNull Position position, @NotNull Epoch epoch) {

    public int x() {
        return position.x();
    }

    public int z() {
        return position.z();
    }

    @NotNull
    public DimensionType dimensionType() {
        return position.dimensionType();
    }

}
