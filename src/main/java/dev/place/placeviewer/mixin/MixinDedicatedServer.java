package dev.place.placeviewer.mixin;

import dev.place.placeviewer.systems.entrypoint.PlaceViewer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class MixinDedicatedServer {

    @Inject(method = "initServer", at = @At("TAIL"))
    private void initializePlaceViewer(final CallbackInfoReturnable<Boolean> cir) {
        PlaceViewer.initialize();
    }

    @Inject(method = "onServerExit", at = @At("HEAD"))
    private void shutdownPlaceViewer(final CallbackInfo ci) {
        PlaceViewer.shutdown();
    }

}
