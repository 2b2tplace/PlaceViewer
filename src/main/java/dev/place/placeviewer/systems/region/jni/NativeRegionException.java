package dev.place.placeviewer.systems.region.jni;

import org.jetbrains.annotations.NotNull;

public class NativeRegionException extends RuntimeException {

    @NotNull
    private final String errorMessage;

    private final int errorCode;

    public NativeRegionException(@NotNull final String errorMessage, final int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    @NotNull
    public String errorMessage() {
        return errorMessage;
    }

    public int errorCode() {
        return errorCode;
    }

}
