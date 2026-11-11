package org.windomium.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.windomium.server.ServerTickOptimiser;

@Mixin(Level.class)
public abstract class LevelMixin {
    @WrapWithCondition(
            method = "guardEntityTick",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V")
    )
    private <T extends Entity> boolean windomium$shouldAcceptEntityTick(Consumer<T> consumer, Object entityObj) {
        if (entityObj instanceof Entity entity) {
            Level self = (Level) (Object) this;
            return !ServerTickOptimiser.shouldSkipEntityTick(self, entity);
        }
        return true;
    }
}