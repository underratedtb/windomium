package org.windomium.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.windomium.client.culling.CullingController;

@Mixin(ParticleEngine.class)
public final class ParticleEngineMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void windomium$onAddParticle(Particle particle, CallbackInfo ci) {
        if (particle != null && CullingController.shouldCullParticle(particle.getBoundingBox())) {
            ci.cancel();
        }
    }
}