package org.windomium.mixin;

import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.windomium.server.ServerTickOptimiser;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void windomium$onGuardEntityTick(Consumer<T> consumer, T entity, CallbackInfo ci) {
        Level self = (Level) (Object) this;
        if (ServerTickOptimiser.shouldSkipEntityTick(self, entity)) {
            ci.cancel();
        }
    }
}