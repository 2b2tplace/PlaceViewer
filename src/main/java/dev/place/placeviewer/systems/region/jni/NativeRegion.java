package dev.place.placeviewer.systems.region.jni;

public final class NativeRegion {

    private NativeRegion() {}

    public static native void initialize(final String registryDirectory);

    public static native long newRegionFromDisk(final String parentDirectory, final int rx, final int rz, final int dimensionType);

    public static native void freeRegion(final long regionPtr);

    public static native byte[] createChunkDataPacket(final long regionPtr, final int dimensionType, final int absChunkX, final int absChunkZ, final long timestamp);

    public static native byte[] createEmptyChunkData(final int dimensionType);

    public static native long[] indexRegionEpochs(final long regionPtr);

}
