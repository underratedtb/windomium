package org.windomium.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.windomium.client.entity.EntityOptimiserController;

@Mixin(EntityRenderer.class)
public final class EntityRendererMixin<T extends Entity> {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void windomium$onShouldShowName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (EntityOptimiserController.shouldSkipNameTag(entity)) {
            cir.setReturnValue(false);
        }
    }
}