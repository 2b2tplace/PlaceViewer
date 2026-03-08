package dev.place.placeviewer.mixin;

import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoggerChunkProgressListener.class)
public abstract class MixinLoggerChunkProgressListener {

    @Shadow
    public static LoggerChunkProgressListener createCompleted() {
        return null;
    }

    @Inject(method = "create", at = @At("HEAD"), cancellable = true)
    private static void skipStartupChunkLoading(final int radius, final CallbackInfoReturnable<LoggerChunkProgressListener> cir) {
        cir.setReturnValue(createCompleted());
    }

}
