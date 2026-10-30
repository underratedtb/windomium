package org.windomium.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.windomium.client.gui.WindomiumConfigScreen;
import org.windomium.core.config.WindomiumConfig;

public final class WindomiumKeyMappings {
    public static final String CATEGORY = "key.categories.windomium";

    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.windomium.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_OVERLAY_KEY = new KeyMapping(
            "key.windomium.toggle_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            CATEGORY
    );

    private WindomiumKeyMappings() {
    }

    public static void handleClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        while (OPEN_CONFIG_KEY.consumeClick()) {
            if (!(mc.screen instanceof WindomiumConfigScreen)) {
                mc.setScreen(new WindomiumConfigScreen(mc.screen));
            }
        }

        while (TOGGLE_OVERLAY_KEY.consumeClick()) {
            if (WindomiumConfig.CLIENT_SPEC.isLoaded()) {
                boolean next = !WindomiumConfig.SHOW_PROFILER_OVERLAY.get();
                WindomiumConfig.SHOW_PROFILER_OVERLAY.set(next);
                WindomiumConfig.CLIENT_SPEC.save();
                mc.player.displayClientMessage(
                        Component.literal("[Windomium] Profiler overlay " + (next ? "enabled." : "disabled.")),
                        true
                );
            }
        }
    }
}