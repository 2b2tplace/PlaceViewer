package dev.place.placeviewer.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkCache.class)
public class MixinServerChunkCache {

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "syncLoad", at = @At("HEAD"), cancellable = true)
    private void onSyncLoad(final int chunkX, final int chunkZ, final ChunkStatus toStatus, final CallbackInfoReturnable<ChunkAccess> cir) {
        cir.setReturnValue(new EmptyLevelChunk(level, new ChunkPos(chunkX, chunkZ), MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)));
    }

}
