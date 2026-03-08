package dev.place.placeviewer.mixin;

import ca.spottedleaf.moonrise.patches.chunk_system.io.MoonriseRegionFileIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegionFileStorage.class)
public class MixinRegionFileStorage {

    @Inject(method = "moonrise$startWrite", at = @At("HEAD"), cancellable = true)
    private void cancelStartWrite(final int chunkX, final int chunkZ, final CompoundTag compound, final CallbackInfoReturnable<MoonriseRegionFileIO.RegionDataController.WriteData> cir) {
        cir.setReturnValue(new MoonriseRegionFileIO.RegionDataController.WriteData(
            compound, MoonriseRegionFileIO.RegionDataController.WriteData.WriteResult.DELETE,
            null, null
        ));
    }

    @Inject(method = "moonrise$getRegionFileIfExists", at = @At("HEAD"), cancellable = true)
    private void cancelGetRegionFileIfExists(final int chunkX, final int chunkZ, final CallbackInfoReturnable<RegionFile> cir) {
        cir.setReturnValue(null);
    }

    @Inject(method = "getRegionFile(Lnet/minecraft/world/level/ChunkPos;Z)Lnet/minecraft/world/level/chunk/storage/RegionFile;", at = @At("HEAD"), cancellable = true)
    private void cancelGetRegionFile(final ChunkPos chunkPos, final boolean existingOnly, final CallbackInfoReturnable<RegionFile> cir) {
        cir.setReturnValue(null);
    }

}
