package org.windomium.client.entity;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.windomium.api.RenderBehaviour;
import org.windomium.api.WindomiumAPI;
import org.windomium.client.culling.CullingController;
import org.windomium.client.profiler.ClientProfilerManager;
import org.windomium.core.compat.ModCompatibilityManager;
import org.windomium.core.config.Preset;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

public final class BlockEntityOptimiserController {
    private static final double SUB_LEVEL_DISTANCE_SQ_THRESHOLD = 268_435_456.0;

    private static boolean active = false;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static double maxDistanceSq = 84.0 * 84.0;
    private static double throttleStartDistSq = 36.0 * 36.0;
    private static long frameBudgetNs = 1_800_000L;
    private static long accumulatedTimeNs = 0L;
    private static long frameCounter = 0L;

    private BlockEntityOptimiserController() {
    }

    public static void onFrameBegin() {
        accumulatedTimeNs = 0L;
        frameCounter++;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            active = false;
            return;
        }

        boolean configEnabled = !WindomiumConfig.CLIENT_SPEC.isLoaded() || WindomiumConfig.ENABLE_BLOCK_ENTITY_OPTIMISER.get();
        active = configEnabled && ModuleManager.isModuleActive("block_entity_optimiser");
        if (!active) {
            return;
        }

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = camera.getPosition();
        cameraX = pos.x;
        cameraY = pos.y;
        cameraZ = pos.z;

        Preset preset = WindomiumConfig.CLIENT_SPEC.isLoaded() ? WindomiumConfig.ACTIVE_PRESET.get() : Preset.BALANCED;
        double scale = CullingController.getDynamicQualityScale();

        double baseMaxDist = switch (preset) {
            case SAFE -> 128.0;
            case BALANCED -> 84.0;
            case EXTREME -> 56.0;
            case CUSTOM -> 84.0;
        };

        double baseThrottleDist = switch (preset) {
            case SAFE -> 48.0;
            case BALANCED -> 32.0;
            case EXTREME -> 20.0;
            case CUSTOM -> 32.0;
        };

        int budgetMicros = WindomiumConfig.CLIENT_SPEC.isLoaded()
                ? WindomiumConfig.BLOCK_ENTITY_FRAME_BUDGET_MICROS.get()
                : 1500;
        frameBudgetNs = budgetMicros * 1_000L;

        double scaledMax = baseMaxDist * scale;
        double scaledThrottle = baseThrottleDist * scale;

        maxDistanceSq = scaledMax * scaledMax;
        throttleStartDistSq = scaledThrottle * scaledThrottle;
    }

    public static boolean shouldSkipBlockEntity(BlockEntity blockEntity) {
        if (!active || blockEntity == null) {
            ClientProfilerManager.getSectionTimings().incrementRenderedBlockEntities(1);
            return false;
        }

        if (ModCompatibilityManager.isClassBypassed(blockEntity.getClass().getName())) {
            ClientProfilerManager.getSectionTimings().incrementRenderedBlockEntities(1);
            return false;
        }

        String typeId = blockEntity.getType().toString();
        RenderBehaviour behaviour = WindomiumAPI.getBlockEntityBehaviour(typeId).orElse(RenderBehaviour.CULLABLE);
        if (behaviour == RenderBehaviour.DO_NOT_TOUCH) {
            ClientProfilerManager.getSectionTimings().incrementRenderedBlockEntities(1);
            return false;
        }

        BlockPos pos = blockEntity.getBlockPos();
        double dx = (pos.getX() + 0.5) - cameraX;
        double dy = (pos.getY() + 0.5) - cameraY;
        double dz = (pos.getZ() + 0.5) - cameraZ;
        double distSq = (dx * dx) + (dy * dy) + (dz * dz);

        if (distSq > SUB_LEVEL_DISTANCE_SQ_THRESHOLD) {
            ClientProfilerManager.getSectionTimings().incrementRenderedBlockEntities(1);
            return false;
        }

        if (behaviour == RenderBehaviour.CULLABLE && distSq > maxDistanceSq) {
            ClientProfilerManager.getSectionTimings().incrementCulledBlockEntities(1);
            return true;
        }

        if (accumulatedTimeNs > frameBudgetNs && distSq > throttleStartDistSq) {
            long hash = (pos.asLong() ^ frameCounter) & 3L;
            if (hash != 0L) {
                ClientProfilerManager.getSectionTimings().incrementCulledBlockEntities(1);
                return true;
            }
        }

        ClientProfilerManager.getSectionTimings().incrementRenderedBlockEntities(1);
        return false;
    }

    public static void beginRenderSample() {
    }

    public static void endRenderSample() {
    }

    public static void recordRenderDuration(long durationNs) {
        if (active && durationNs > 0L) {
            accumulatedTimeNs += durationNs;
        }
    }
}