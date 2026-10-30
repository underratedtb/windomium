package org.windomium.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.windomium.api.RenderBehaviour;
import org.windomium.api.WindomiumAPI;
import org.windomium.client.culling.CullingController;
import org.windomium.client.profiler.ClientProfilerManager;
import org.windomium.core.config.Preset;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

public final class EntityOptimiserController {
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static double maxShadowDistanceSq = 32.0 * 32.0;
    private static double maxNameTagDistanceSq = 48.0 * 48.0;
    private static double maxLayerDistanceSq = 40.0 * 40.0;
    private static boolean moduleActive = false;

    private EntityOptimiserController() {
    }

    public static void onFrameBegin() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            moduleActive = false;
            return;
        }

        boolean configEnabled = !WindomiumConfig.CLIENT_SPEC.isLoaded() || WindomiumConfig.ENABLE_ENTITY_OPTIMISER.get();
        moduleActive = configEnabled && ModuleManager.isModuleActive("entity_optimiser");
        if (!moduleActive) {
            return;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        cameraX = camPos.x;
        cameraY = camPos.y;
        cameraZ = camPos.z;

        double scale = CullingController.getDynamicQualityScale();
        Preset preset = WindomiumConfig.CLIENT_SPEC.isLoaded() ? WindomiumConfig.ACTIVE_PRESET.get() : Preset.BALANCED;

        double shadowBase = switch (preset) {
            case SAFE -> 36.0;
            case BALANCED -> 24.0;
            case EXTREME -> 14.0;
            case CUSTOM -> 24.0;
        };
        double nameTagBase = switch (preset) {
            case SAFE -> 64.0;
            case BALANCED -> 44.0;
            case EXTREME -> 28.0;
            case CUSTOM -> 44.0;
        };
        double layerBase = switch (preset) {
            case SAFE -> 48.0;
            case BALANCED -> 32.0;
            case EXTREME -> 20.0;
            case CUSTOM -> 32.0;
        };

        double scaledShadow = shadowBase * scale;
        double scaledNameTag = nameTagBase * scale;
        double scaledLayer = layerBase * scale;

        maxShadowDistanceSq = scaledShadow * scaledShadow;
        maxNameTagDistanceSq = scaledNameTag * scaledNameTag;
        maxLayerDistanceSq = scaledLayer * scaledLayer;
    }

    public static boolean shouldSkipShadow(Entity entity) {
        if (!moduleActive || entity == null || entity instanceof Player) {
            return false;
        }

        double distSq = entity.distanceToSqr(cameraX, cameraY, cameraZ);
        if (distSq > maxShadowDistanceSq) {
            ClientProfilerManager.getSectionTimings().incrementSkippedEntityLayers(1);
            return true;
        }
        return false;
    }

    public static boolean shouldSkipNameTag(Entity entity) {
        if (!moduleActive || entity == null || entity instanceof Player) {
            return false;
        }

        double distSq = entity.distanceToSqr(cameraX, cameraY, cameraZ);
        if (distSq > maxNameTagDistanceSq) {
            ClientProfilerManager.getSectionTimings().incrementSkippedEntityLayers(1);
            return true;
        }
        return false;
    }

    public static boolean shouldSkipEquipmentLayer(LivingEntity entity) {
        if (!moduleActive || entity == null || entity instanceof Player) {
            return false;
        }

        String entityId = entity.getType().toString();
        if (WindomiumAPI.getEntityBehaviour(entityId).orElse(RenderBehaviour.THROTTLEABLE) == RenderBehaviour.DO_NOT_TOUCH) {
            return false;
        }

        double distSq = entity.distanceToSqr(cameraX, cameraY, cameraZ);
        if (distSq > maxLayerDistanceSq) {
            ClientProfilerManager.getSectionTimings().incrementSkippedEntityLayers(1);
            return true;
        }
        return false;
    }
}