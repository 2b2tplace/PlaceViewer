package dev.place.placeviewer.mixin;

import ca.spottedleaf.concurrentutil.executor.Cancellable;
import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(MoonriseRegionFileIO.class)
public class MixinMoonriseRegionFileIO {

    @Inject(method = "scheduleSave(Lnet/minecraft/server/level/ServerLevel;IILjava/util/function/Consumer;Lca/spottedleaf/concurrentutil/executor/PrioritisedExecutor$PrioritisedTask;" +
        "Lca/spottedleaf/moonrise/patches/chunk_system/io/MoonriseRegionFileIO$RegionFileType;Lca/spottedleaf/concurrentutil/util/Priority;)V",
        at = @At("HEAD"), cancellable = true)
    private static void cancelScheduleSave(final ServerLevel world, final int chunkX, final int chunkZ, final Consumer<BiConsumer<CompoundTag, Throwable>> scheduler, final PrioritisedExecutor.PrioritisedTask writeTask, final MoonriseRegionFileIO.RegionFileType type, final Priority priority, final CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "loadDataAsync(Lnet/minecraft/server/level/ServerLevel;IILca/spottedleaf/moonrise/patches/chunk_system/io/MoonriseRegionFileIO$RegionFileType;" +
        "Ljava/util/function/BiConsumer;ZLca/spottedleaf/concurrentutil/util/Priority;)Lca/spottedleaf/concurrentutil/executor/Cancellable;",
        at = @At("HEAD"), cancellable = true)
    private static void cancelLoadDataAsync(final ServerLevel world, final int chunkX, final int chunkZ, final MoonriseRegionFileIO.RegionFileType type, final BiConsumer<CompoundTag, Throwable> onComplete, final boolean intendingToBlock, final Priority priority, final CallbackInfoReturnable<Cancellable> cir) {
        onComplete.accept(null, null);
        cir.setReturnValue(null);
    }

}
