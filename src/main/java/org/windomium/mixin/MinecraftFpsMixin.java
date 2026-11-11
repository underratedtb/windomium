package org.windomium.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

@Mixin(Minecraft.class)
public final class MinecraftFpsMixin {
    @Shadow
    @Final
    private Window window;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void windomium$onGetFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        if (!ModuleManager.isModuleActive("unfocused_fps")) {
            return;
        }
        if (WindomiumConfig.CLIENT_SPEC.isLoaded() && !WindomiumConfig.ENABLE_UNFOCUSED_FPS.get()) {
            return;
        }
        try {
            if (this.window != null && !((Minecraft) (Object) this).isWindowActive()) {
                int limit = WindomiumConfig.CLIENT_SPEC.isLoaded()
                        ? WindomiumConfig.UNFOCUSED_FPS_LIMIT.get()
                        : 15;
                cir.setReturnValue(limit);
            }
        } catch (Throwable throwable) {
            ModuleManager.handleModuleException("unfocused_fps", throwable);
        }
    }
}