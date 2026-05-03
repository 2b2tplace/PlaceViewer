package dev.place.placeviewer.mixin;

import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PlayerList.class)
public class MixinPlayerList {

    @ModifyArg(method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;IZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundEntityEventPacket;<init>(Lnet/minecraft/world/entity/Entity;B)V"), index = 1)
    private byte forceClientsideOpPermissionLevel(final byte eventId) {
        return 28;
    }

}
