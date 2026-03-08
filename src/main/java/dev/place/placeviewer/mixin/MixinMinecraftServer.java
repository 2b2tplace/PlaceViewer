package dev.place.placeviewer.mixin;

import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Shadow
    @Final
    public static Logger LOGGER;

    @Shadow
    private long nextTickTimeNanos;

    @Shadow
    public boolean forceTicks;

    @Inject(method = "prepareLevels", at = @At("HEAD"), cancellable = true)
    private void skipPrepareLevels(final ChunkProgressListener listener, final ServerLevel serverLevel, final CallbackInfo ci) {
        LOGGER.info("Skipping preparation for dimension {}", serverLevel.dimension().location());
        nextTickTimeNanos = Util.getNanos();
        forceTicks = false;
        listener.stop();
        ci.cancel();
    }

}
