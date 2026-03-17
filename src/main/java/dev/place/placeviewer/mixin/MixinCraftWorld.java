package dev.place.placeviewer.mixin;

import org.bukkit.craftbukkit.CraftWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftWorld.class)
public class MixinCraftWorld {

    @Inject(method = "isChunkGenerated", at = @At("HEAD"), cancellable = true)
    private void markAllChunksGenerated(final int x, final int z, final CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

}
