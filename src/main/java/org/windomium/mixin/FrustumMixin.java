package org.windomium.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.windomium.client.culling.CullingController;

@Mixin(Frustum.class)
public final class FrustumMixin {
    @Inject(method = "isVisible(Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
    private void windomium$onIsVisible(AABB aabb, CallbackInfoReturnable<Boolean> cir) {
        if (aabb != null && CullingController.shouldCullBox(aabb)) {
            cir.setReturnValue(false);
        }
    }
}