package dev.place.placeviewer.systems.util;

import org.jetbrains.annotations.Nullable;

public class ExpiringFlag {

    private Long beganAt;
    private final long expiringDuration;

    public ExpiringFlag(final long beganAt, final long expiringDuration) {
        this.beganAt = beganAt;
        this.expiringDuration = expiringDuration;
    }

    public ExpiringFlag(final long expiringDuration) {
        this.expiringDuration = expiringDuration;
    }

    public void reset() {
        this.beganAt = System.currentTimeMillis();
    }

    @Nullable
    public Long beganAt() {
        return beganAt;
    }

    public void beganAt(final long millis) {
        this.beganAt = millis;
    }

    public boolean expired(final long currentTime) {
        return beganAt == null || currentTime - beganAt >= expiringDuration;
    }

    public boolean expired() {
        return expired(System.currentTimeMillis());
    }

}
