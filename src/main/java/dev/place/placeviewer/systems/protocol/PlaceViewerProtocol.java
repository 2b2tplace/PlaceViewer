package dev.place.placeviewer.systems.protocol;

import dev.place.placeviewer.systems.region.DimensionType;
import dev.place.placeviewer.systems.region.jni.NativeRegion;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public class PlaceViewerProtocol {

    private PlaceViewerProtocol() {}

    private static final int CHUNK_LEVEL_WITH_LIGHT_PACKET_ID = 0x28; // maybe don't hardcode the packet id...

    private static final byte[] EMPTY_OVERWORLD_CHUNK_DATA  = NativeRegion.createEmptyChunkData(0);
    private static final byte[] EMPTY_NETHER_END_CHUNK_DATA = NativeRegion.createEmptyChunkData(1);

    public static void writeVarInt(@NotNull final ByteBuf output, int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                output.writeByte(value);
                break;
            }
            output.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    public static void writeChunkData(@NotNull final ByteBuf packetBuf, final byte[] packetData) {
        writeVarInt(packetBuf, CHUNK_LEVEL_WITH_LIGHT_PACKET_ID);
        packetBuf.writeBytes(packetData);
    }

    public static void writeEmptyChunkData(@NotNull final ByteBuf packetBuf, final int chunkX, final int chunkZ, @NotNull final DimensionType dimensionType) {
        writeVarInt(packetBuf, CHUNK_LEVEL_WITH_LIGHT_PACKET_ID);
        packetBuf.writeInt(chunkX);
        packetBuf.writeInt(chunkZ);
        packetBuf.writeBytes(dimensionType == DimensionType.OVERWORLD ? EMPTY_OVERWORLD_CHUNK_DATA : EMPTY_NETHER_END_CHUNK_DATA);
    }

}
