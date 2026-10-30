package org.windomium.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.windomium.client.culling.OcclusionCuller;
import org.windomium.core.config.CullingMode;
import org.windomium.core.config.Preset;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

public final class WindomiumConfigScreen extends Screen {
    private final Screen parentScreen;

    public WindomiumConfigScreen(Screen parentScreen) {
        super(Component.literal("Windomium Optimisation Engine Settings"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        int centreX = this.width / 2;
        int startY = 32;
        int buttonWidth = 155;
        int buttonHeight = 20;
        int rowSpacing = 24;

        this.addRenderableWidget(Button.builder(getPresetText(), button -> {
            Preset next = nextPreset(WindomiumConfig.ACTIVE_PRESET.get());
            WindomiumConfig.ACTIVE_PRESET.set(next);
            ModuleManager.applyPreset(next);
            button.setMessage(getPresetText());
        }).bounds(centreX - 160, startY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getCullingModeText(), button -> {
            CullingMode next = nextCullingMode(WindomiumConfig.CULLING_MODE.get());
            WindomiumConfig.CULLING_MODE.set(next);
            OcclusionCuller.clearCache();
            button.setMessage(getCullingModeText());
        }).bounds(centreX + 5, startY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getFrustumText(), button -> {
            boolean next = !WindomiumConfig.ENABLE_FRUSTUM_CULLING.get();
            WindomiumConfig.ENABLE_FRUSTUM_CULLING.set(next);
            ModuleManager.setModuleState("frustum_culling", next);
            button.setMessage(getFrustumText());
        }).bounds(centreX - 160, startY + rowSpacing, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getOcclusionText(), button -> {
            boolean next = !WindomiumConfig.ENABLE_OCCLUSION_CULLING.get();
            WindomiumConfig.ENABLE_OCCLUSION_CULLING.set(next);
            ModuleManager.setModuleState("occlusion_culling", next);
            button.setMessage(getOcclusionText());
        }).bounds(centreX + 5, startY + rowSpacing, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getAsyncMeshText(), button -> {
            boolean next = !WindomiumConfig.ENABLE_ASYNC_MESHING.get();
            WindomiumConfig.ENABLE_ASYNC_MESHING.set(next);
            ModuleManager.setModuleState("chunk_mesher", next);
            button.setMessage(getAsyncMeshText());
        }).bounds(centreX - 160, startY + rowSpacing * 2, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getEntityOptText(), button -> {
            boolean next = !WindomiumConfig.ENABLE_ENTITY_OPTIMISER.get();
            WindomiumConfig.ENABLE_ENTITY_OPTIMISER.set(next);
            ModuleManager.setModuleState("entity_optimiser", next);
            button.setMessage(getEntityOptText());
        }).bounds(centreX + 5, startY + rowSpacing * 2, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getBlockEntityOptText(), button -> {
            boolean next = !WindomiumConfig.ENABLE_BLOCK_ENTITY_OPTIMISER.get();
            WindomiumConfig.ENABLE_BLOCK_ENTITY_OPTIMISER.set(next);
            ModuleManager.setModuleState("block_entity_optimiser", next);
            button.setMessage(getBlockEntityOptText());
        }).bounds(centreX - 160, startY + rowSpacing * 3, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getMemoryOptText(), button -> {
            boolean next = !WindomiumConfig.ENABLE_MEMORY_OPTIMISER.get();
            WindomiumConfig.ENABLE_MEMORY_OPTIMISER.set(next);
            ModuleManager.setModuleState("memory_optimiser", next);
            button.setMessage(getMemoryOptText());
        }).bounds(centreX + 5, startY + rowSpacing * 3, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getGovernorText(), button -> {
            boolean next = !WindomiumConfig.ENABLE_RUNTIME_GOVERNOR.get();
            WindomiumConfig.ENABLE_RUNTIME_GOVERNOR.set(next);
            button.setMessage(getGovernorText());
        }).bounds(centreX - 160, startY + rowSpacing * 4, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getOverlayText(), button -> {
            boolean next = !WindomiumConfig.SHOW_PROFILER_OVERLAY.get();
            WindomiumConfig.SHOW_PROFILER_OVERLAY.set(next);
            button.setMessage(getOverlayText());
        }).bounds(centreX + 5, startY + rowSpacing * 4, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(getServerOptText(), button -> {
            if (WindomiumConfig.SERVER_SPEC.isLoaded()) {
                boolean next = !WindomiumConfig.ENABLE_SERVER_OPTIMISATIONS.get();
                WindomiumConfig.ENABLE_SERVER_OPTIMISATIONS.set(next);
                ModuleManager.setModuleState("server_tick_optimiser", next);
                button.setMessage(getServerOptText());
            }
        }).bounds(centreX - 160, startY + rowSpacing * 5, buttonWidth * 2 + 10, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> {
            WindomiumConfig.CLIENT_SPEC.save();
            if (WindomiumConfig.SERVER_SPEC.isLoaded()) {
                WindomiumConfig.SERVER_SPEC.save();
            }
            Minecraft.getInstance().setScreen(this.parentScreen);
        }).bounds(centreX - 100, startY + rowSpacing * 6 + 6, 200, buttonHeight).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0x55FF55);
    }

    private static Preset nextPreset(Preset current) {
        Preset[] values = Preset.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static CullingMode nextCullingMode(CullingMode current) {
        CullingMode[] values = CullingMode.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    private static Component getPresetText() {
        return Component.literal("Preset: " + WindomiumConfig.ACTIVE_PRESET.get().name());
    }

    private static Component getCullingModeText() {
        return Component.literal("Culling Mode: " + WindomiumConfig.CULLING_MODE.get().name());
    }

    private static Component getFrustumText() {
        return Component.literal("Frustum Culling: " + (WindomiumConfig.ENABLE_FRUSTUM_CULLING.get() ? "ON" : "OFF"));
    }

    private static Component getOcclusionText() {
        return Component.literal("Occlusion Culling: " + (WindomiumConfig.ENABLE_OCCLUSION_CULLING.get() ? "ON" : "OFF"));
    }

    private static Component getAsyncMeshText() {
        return Component.literal("Async Meshing: " + (WindomiumConfig.ENABLE_ASYNC_MESHING.get() ? "ON" : "OFF"));
    }

    private static Component getEntityOptText() {
        return Component.literal("Entity Optimiser: " + (WindomiumConfig.ENABLE_ENTITY_OPTIMISER.get() ? "ON" : "OFF"));
    }

    private static Component getBlockEntityOptText() {
        return Component.literal("BlockEntity Opt: " + (WindomiumConfig.ENABLE_BLOCK_ENTITY_OPTIMISER.get() ? "ON" : "OFF"));
    }

    private static Component getMemoryOptText() {
        return Component.literal("Memory Optimiser: " + (WindomiumConfig.ENABLE_MEMORY_OPTIMISER.get() ? "ON" : "OFF"));
    }

    private static Component getGovernorText() {
        return Component.literal("Runtime Governor: " + (WindomiumConfig.ENABLE_RUNTIME_GOVERNOR.get() ? "ON" : "OFF"));
    }

    private static Component getOverlayText() {
        return Component.literal("Profiler Overlay: " + (WindomiumConfig.SHOW_PROFILER_OVERLAY.get() ? "ON" : "OFF"));
    }

    private static Component getServerOptText() {
        boolean active = WindomiumConfig.SERVER_SPEC.isLoaded() && WindomiumConfig.ENABLE_SERVER_OPTIMISATIONS.get();
        return Component.literal("Server Distant Mob Tick Throttler (Experimental): " + (active ? "ON" : "OFF"));
    }
}