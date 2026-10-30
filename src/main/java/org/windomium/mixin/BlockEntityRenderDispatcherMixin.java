package org.windomium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.windomium.client.entity.BlockEntityOptimiserController;

@Mixin(BlockEntityRenderDispatcher.class)
public final class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void windomium$onRenderBlockEntityHead(E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (BlockEntityOptimiserController.shouldSkipBlockEntity(blockEntity)) {
            ci.cancel();
            return;
        }
        BlockEntityOptimiserController.beginRenderSample();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private <E extends BlockEntity> void windomium$onRenderBlockEntityReturn(E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        BlockEntityOptimiserController.endRenderSample();
    }
}