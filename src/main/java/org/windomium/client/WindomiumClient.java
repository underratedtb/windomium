package org.windomium.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.windomium.Windomium;
import org.windomium.client.command.WindomiumClientCommands;
import org.windomium.client.culling.CullingController;
import org.windomium.client.culling.OcclusionCuller;
import org.windomium.client.entity.BlockEntityOptimiserController;
import org.windomium.client.entity.EntityOptimiserController;
import org.windomium.client.profiler.ClientProfilerManager;
import org.windomium.client.profiler.ProfilerOverlay;
import org.windomium.core.memory.MemoryOptimiserController;
import org.windomium.core.module.ModuleManager;

@EventBusSubscriber(modid = Windomium.MOD_ID, value = Dist.CLIENT)
public final class WindomiumClient {
    static {
        ModuleManager.setFaultNotifier((moduleName, error) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.execute(() -> {
                    if (mc.player != null) {
                        Component alert = Component.literal("[Windomium Circuit Breaker] Module '" + moduleName + "' tripped after repeated errors and was rolled back.")
                                .withStyle(ChatFormatting.RED);
                        mc.player.sendSystemMessage(alert);
                    }
                });
            }
        });
    }

    private WindomiumClient() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(WindomiumKeyMappings.TOGGLE_OVERLAY_KEY);
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        CullingController.onClientTick();
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        WindomiumKeyMappings.handleClientTick();
    }

    @SubscribeEvent
    public static void onRenderFramePre(RenderFrameEvent.Pre event) {
        ClientProfilerManager.onFrameStart();
        CullingController.onFrameBegin();
        EntityOptimiserController.onFrameBegin();
        BlockEntityOptimiserController.onFrameBegin();
        MemoryOptimiserController.onFrameSample();
    }

    @SubscribeEvent
    public static void onRenderFramePost(RenderFrameEvent.Post event) {
        ClientProfilerManager.onFrameEnd();
    }

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        ProfilerOverlay.render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        WindomiumClientCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            MemoryOptimiserController.trimCaches();
            OcclusionCuller.clearCache();
        }
    }
}