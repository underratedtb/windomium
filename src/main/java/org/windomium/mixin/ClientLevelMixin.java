package org.windomium.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.windomium.core.memory.BiomeColourCache;
import org.windomium.core.memory.MemoryOptimiserController;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Shadow
    public abstract int calculateBlockTint(BlockPos blockPos, ColorResolver colorResolver);

    @Inject(method = "getBlockTint", at = @At("HEAD"), cancellable = true)
    private void windomium$onGetBlockTint(BlockPos blockPos, ColorResolver colorResolver, CallbackInfoReturnable<Integer> cir) {
        if (MemoryOptimiserController.isModuleActive() && blockPos != null && colorResolver != null) {
            int colour = BiomeColourCache.getOrComputeColour(
                    blockPos,
                    colorResolver,
                    () -> this.calculateBlockTint(blockPos, colorResolver)
            );
            cir.setReturnValue(colour);
        }
    }
}