package dev.place.placeviewer.systems.flashback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public sealed interface Epoch {

    @NotNull
    Now NOW = new Now();

    long timestamp();

    @NotNull
    Component actionBar();

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

        @NotNull
        public Component actionBar() {
            return Component.text("Viewing latest snapshot", NamedTextColor.GRAY);
        }

        public int hashCode() {
            return 0;
        }

        @NotNull
        public String toString() {
            return "now (" + timestamp() + ")";
        }
    }

    final class Snapshot implements Epoch {

        private final long timestamp;
        private final String formattedTime;

        public Snapshot(final long timestamp) {
            this.timestamp = timestamp;
            this.formattedTime = EpochIndex.FORMAT.format(Date.from(Instant.ofEpochMilli(timestamp)));
        }

        @NotNull
        public Component actionBar() {
            return Component.text("Viewing snapshot from " + formattedTime, NamedTextColor.GRAY);
        }

        @NotNull
        public String formattedTime() {
            return formattedTime;
        }

        public long timestamp() {
            return timestamp;
        }

        public int hashCode() {
            return Objects.hashCode(timestamp);
        }

        @NotNull
        public String toString() {
            return "snapshot (" + timestamp() + ")";
        }

    }

}
