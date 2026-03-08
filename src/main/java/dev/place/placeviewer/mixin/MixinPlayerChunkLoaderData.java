package dev.place.placeviewer.mixin;

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter;
import ca.spottedleaf.moonrise.common.util.CoordinateUtils;
import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader;
import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import dev.place.placeviewer.systems.region.RegionPool;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegionizedPlayerChunkLoader.PlayerChunkLoaderData.class)
public abstract class MixinPlayerChunkLoaderData {

    @Shadow
    @Final
    private static byte CHUNK_TICKET_STAGE_TICK;

    @Shadow
    @Final
    private ServerPlayer player;

    @Shadow
    @Final
    private LongHeapPriorityQueue sendQueue;

    @Shadow
    @Final
    private static long MAX_RATE;

    @Shadow
    @Final
    private AllocatingRateLimiter chunkSendLimiter;

    @Shadow
    private volatile boolean removed;

    @Shadow
    @Final
    private LongOpenHashSet sentChunks;

    @Inject(method = "updateQueues", at = @At("HEAD"), cancellable = true)
    private void pollSendQueue(final long time, final CallbackInfo ci) {
        final double sendRate = PlaceViewer.config().maxSendRate();
        chunkSendLimiter.tickAllocation(time, sendRate, sendRate);

        final int maxSendsThisTick = Math.min((int) chunkSendLimiter.takeAllocation(time, sendRate, MAX_RATE), sendQueue.size());
        PlaceViewer.epochPool().sentChunks(player.getUUID(), sentChunks);
        PlaceViewer.epochPool().chunkSendQueue(player.getUUID(), sendQueue);

        for (int i = 0; i < maxSendsThisTick; ++i) {
            final long pendingSend = sendQueue.firstLong();
            final int pendingSendX = CoordinateUtils.getChunkX(pendingSend);
            final int pendingSendZ = CoordinateUtils.getChunkZ(pendingSend);

            sendQueue.dequeueLong();

            if (sentChunks.add(CoordinateUtils.getChunkKey(pendingSendX, pendingSendZ)))
                RegionPool.sendChunk(player, pendingSendX, pendingSendZ);

            if (removed) return;
        }
        ci.cancel();
    }

    @Inject(method = "sendUnloadChunkRaw", at = @At("HEAD"), cancellable = true)
    private void sendUnloadChunkRaw(final int chunkX, final int chunkZ, final CallbackInfo ci) {
        player.connection.send(new ClientboundForgetLevelChunkPacket(new ChunkPos(chunkX, chunkZ)));
        ci.cancel();
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ByteOpenHashMap;get(J)B"))
    private byte skipServerSideChunkLoading(final Long2ByteOpenHashMap instance, final long k) {
        return CHUNK_TICKET_STAGE_TICK;
    }

}
