package dev.place.placeviewer.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerGamePacketListenerImpl {

    @Inject(method = "hasNewCollision", at = @At("HEAD"), cancellable = true)
    private void cancelIncorrectCollision(final ServerLevel level, final Entity entity, final AABB oldBox, final AABB newBox, final CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void cancelSendIncorrectBlockUpdate(final ServerGamePacketListenerImpl instance, final Packet<?> packet) {

    }

}
