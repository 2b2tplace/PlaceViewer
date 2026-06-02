package dev.place.placeviewer.systems.flashback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

public sealed interface Epoch {

    @NotNull
    Now NOW = new Now();

    long timestamp();

    @NotNull
    static Now now() {
        return NOW;
    }

    @NotNull
    static Snapshot snapshot(final long timestamp) {
        return new Snapshot(timestamp);
    }

    record Now() implements Epoch {

        public long timestamp() {
            return System.currentTimeMillis();
        }

        public int hashCode() {
            return 0;
        }

        @NotNull
        public String toString() {
            return "now (" + timestamp() + ")";
        }
    }

    record Snapshot(long timestamp) implements Epoch {

        @NotNull
        public String toString() {
                return "snapshot (" + timestamp() + ")";
            }

    }

}
