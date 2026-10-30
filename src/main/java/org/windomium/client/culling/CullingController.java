package org.windomium.client.culling;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.windomium.api.RenderBehaviour;
import org.windomium.api.WindomiumAPI;
import org.windomium.client.profiler.ClientProfilerManager;
import org.windomium.core.compat.CompatibilityDetector;
import org.windomium.core.compat.ModCompatibilityManager;
import org.windomium.core.config.CullingMode;
import org.windomium.core.config.Preset;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;
import org.windomium.core.profiler.FrameMetricsSnapshot;

public final class CullingController {
    private static final double SUB_LEVEL_DISTANCE_SQ_THRESHOLD = 268_435_456.0;

    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static double maxSectionDistanceSq = 1024.0 * 1024.0;
    private static double maxEntityDistanceSq = 128.0 * 128.0;
    private static double maxBlockEntityDistanceSq = 96.0 * 96.0;
    private static double maxParticleDistanceSq = 64.0 * 64.0;
    private static double dynamicQualityScale = 1.0;
    private static long lastGovernorUpdateNs = 0L;
    private static int activeFrameParticles = 0;
    private static int maxParticlesPerFrame = 500;
    private static boolean frustumActive = false;
    private static boolean occlusionActive = false;
    private static CullingMode activeCullingMode = CullingMode.COMBINED;

    private CullingController() {
    }

    public static void onFrameBegin() {
        ClientProfilerManager.getSectionTimings().resetCounters();
        activeFrameParticles = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            frustumActive = false;
            occlusionActive = false;
            return;
        }

        boolean configLoaded = WindomiumConfig.CLIENT_SPEC.isLoaded();
        activeCullingMode = configLoaded ? WindomiumConfig.CULLING_MODE.get() : CullingMode.COMBINED;

        boolean frustumToggle = !configLoaded || WindomiumConfig.ENABLE_FRUSTUM_CULLING.get();
        boolean occlusionToggle = !configLoaded || WindomiumConfig.ENABLE_OCCLUSION_CULLING.get();

        frustumActive = frustumToggle
                && ModuleManager.isModuleActive("frustum_culling")
                && (activeCullingMode == CullingMode.FRUSTUM || activeCullingMode == CullingMode.COMBINED);

        occlusionActive = occlusionToggle
                && ModuleManager.isModuleActive("occlusion_culling")
                && (activeCullingMode == CullingMode.OCCLUSION || activeCullingMode == CullingMode.COMBINED);

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = camera.getPosition();
        cameraX = pos.x;
        cameraY = pos.y;
        cameraZ = pos.z;

        if (occlusionActive) {
            OcclusionCuller.onFrameBegin(cameraX, cameraY, cameraZ);
        }

        updateGovernor();

        int renderChunks = mc.options.getEffectiveRenderDistance();
        double baseSectionBlocks = (renderChunks * 16.0) + 16.0;
        double scaledSectionBlocks = baseSectionBlocks * dynamicQualityScale;
        maxSectionDistanceSq = scaledSectionBlocks * scaledSectionBlocks;

        Preset preset = configLoaded ? WindomiumConfig.ACTIVE_PRESET.get() : Preset.BALANCED;
        double entityBase = switch (preset) {
            case SAFE -> 160.0;
            case BALANCED -> 112.0;
            case EXTREME -> 72.0;
            case CUSTOM -> 112.0;
        };
        double blockEntityBase = switch (preset) {
            case SAFE -> 128.0;
            case BALANCED -> 84.0;
            case EXTREME -> 56.0;
            case CUSTOM -> 84.0;
        };
        double particleBase = switch (preset) {
            case SAFE -> 96.0;
            case BALANCED -> 64.0;
            case EXTREME -> 36.0;
            case CUSTOM -> 64.0;
        };
        maxParticlesPerFrame = switch (preset) {
            case SAFE -> 800;
            case BALANCED -> 400;
            case EXTREME -> 150;
            case CUSTOM -> 400;
        };

        double scaledEntity = entityBase * dynamicQualityScale;
        double scaledBlockEntity = blockEntityBase * dynamicQualityScale;
        double scaledParticle = particleBase * dynamicQualityScale;

        maxEntityDistanceSq = scaledEntity * scaledEntity;
        maxBlockEntityDistanceSq = scaledBlockEntity * scaledBlockEntity;
        maxParticleDistanceSq = scaledParticle * scaledParticle;
    }

    private static void updateGovernor() {
        if (!WindomiumConfig.CLIENT_SPEC.isLoaded() || !WindomiumConfig.ENABLE_RUNTIME_GOVERNOR.get()) {
            dynamicQualityScale = 1.0;
            return;
        }

        long now = System.nanoTime();
        if (now - lastGovernorUpdateNs < 500_000_000L) {
            return;
        }
        lastGovernorUpdateNs = now;

        FrameMetricsSnapshot snapshot = ClientProfilerManager.getLatestSnapshot();
        if (snapshot.sampleCount() < 30) {
            return;
        }

        int targetFps = WindomiumConfig.TARGET_FPS.get();
        int targetOnePercent = WindomiumConfig.TARGET_ONE_PERCENT_LOW.get();
        double currentFps = snapshot.averageFps();
        double currentOnePercent = snapshot.onePercentLowFps();

        if (currentFps < targetFps * 0.90 && currentOnePercent < targetOnePercent * 0.85) {
            dynamicQualityScale = Math.max(0.65, dynamicQualityScale - 0.04);
        } else if (currentFps >= targetFps) {
            dynamicQualityScale = Math.min(1.0, dynamicQualityScale + 0.05);
        }
    }

    public static boolean shouldCullBox(AABB box) {
        if (!frustumActive && !occlusionActive) {
            return false;
        }

        long startNs = System.nanoTime();
        try {
            double spanX = box.maxX - box.minX;
            double spanY = box.maxY - box.minY;
            double spanZ = box.maxZ - box.minZ;

            boolean isChunkSection = spanX >= 15.5 && spanX <= 16.5 && spanY >= 15.5 && spanY <= 16.5 && spanZ >= 15.5 && spanZ <= 16.5;
            if (!isChunkSection) {
                return false;
            }

            double centreX = box.minX + 8.0;
            double centreY = box.minY + 8.0;
            double centreZ = box.minZ + 8.0;

            double dx = centreX - cameraX;
            double dz = centreZ - cameraZ;
            double horizontalDistSq = (dx * dx) + (dz * dz);

            if (horizontalDistSq > SUB_LEVEL_DISTANCE_SQ_THRESHOLD) {
                ClientProfilerManager.getSectionTimings().incrementRenderedSections(1);
                return false;
            }

            if (frustumActive && horizontalDistSq > maxSectionDistanceSq) {
                ClientProfilerManager.getSectionTimings().incrementCulledSections(1);
                return true;
            }

            if (occlusionActive && !CompatibilityDetector.isIrisOrForkLoaded()) {
                double dy = cameraY - centreY;
                if (cameraY > 62.0 && centreY < 40.0 && dy > 28.0 && horizontalDistSq > 4096.0) {
                    ClientProfilerManager.getSectionTimings().incrementCulledSections(1);
                    return true;
                }
                if (horizontalDistSq > 1024.0 && OcclusionCuller.isBoxOccluded(box, cameraX, cameraY, cameraZ)) {
                    ClientProfilerManager.getSectionTimings().incrementCulledSections(1);
                    return true;
                }
            }

            ClientProfilerManager.getSectionTimings().incrementRenderedSections(1);
            return false;
        } finally {
            ClientProfilerManager.getSectionTimings().setCullingTimeNs(System.nanoTime() - startNs);
        }
    }

    public static boolean shouldCullEntity(Entity entity, double camX, double camY, double camZ) {
        if ((!frustumActive && !occlusionActive) || entity == null) {
            return false;
        }

        if (entity instanceof Player || entity.isSpectator() || entity.isCurrentlyGlowing() || entity.hasCustomName() || entity.isVehicle() || entity.isPassenger()) {
            ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
            return false;
        }

        if (ModCompatibilityManager.isClassBypassed(entity.getClass().getName())) {
            ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
            return false;
        }

        String entityId = entity.getType().toString();
        if (WindomiumAPI.getEntityBehaviour(entityId).orElse(RenderBehaviour.CULLABLE) == RenderBehaviour.DO_NOT_TOUCH) {
            ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
            return false;
        }

        double distSq = entity.distanceToSqr(camX, camY, camZ);
        if (distSq > SUB_LEVEL_DISTANCE_SQ_THRESHOLD) {
            ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
            return false;
        }

        if (frustumActive && distSq > maxEntityDistanceSq) {
            ClientProfilerManager.getSectionTimings().incrementCulledEntities(1);
            return true;
        }

        if (occlusionActive) {
            double dy = Math.abs(camY - entity.getY());
            if (dy > 36.0 && distSq > 2304.0) {
                ClientProfilerManager.getSectionTimings().incrementCulledEntities(1);
                return true;
            }
            if (distSq > 256.0 && OcclusionCuller.isBoxOccluded(entity.getBoundingBox(), camX, camY, camZ)) {
                ClientProfilerManager.getSectionTimings().incrementCulledEntities(1);
                return true;
            }
        }

        ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
        return false;
    }

    public static boolean shouldCullBlockEntity(BlockEntity blockEntity) {
        if ((!frustumActive && !occlusionActive) || blockEntity == null) {
            return false;
        }

        if (ModCompatibilityManager.isClassBypassed(blockEntity.getClass().getName())) {
            return false;
        }

        BlockPos pos = blockEntity.getBlockPos();
        double dx = (pos.getX() + 0.5) - cameraX;
        double dy = (pos.getY() + 0.5) - cameraY;
        double dz = (pos.getZ() + 0.5) - cameraZ;
        double distSq = (dx * dx) + (dy * dy) + (dz * dz);

        if (distSq > SUB_LEVEL_DISTANCE_SQ_THRESHOLD) {
            return false;
        }

        if (frustumActive && distSq > maxBlockEntityDistanceSq) {
            return true;
        }

        if (occlusionActive) {
            return Math.abs(dy) > 32.0 && distSq > 1600.0;
        }

        return false;
    }

    public static boolean shouldCullParticle(AABB box) {
        if ((!frustumActive && !occlusionActive) || box == null) {
            return false;
        }

        if (activeFrameParticles >= maxParticlesPerFrame) {
            return true;
        }

        double dx = box.minX - cameraX;
        double dy = box.minY - cameraY;
        double dz = box.minZ - cameraZ;
        double distSq = (dx * dx) + (dy * dy) + (dz * dz);

        if (distSq > SUB_LEVEL_DISTANCE_SQ_THRESHOLD) {
            activeFrameParticles++;
            return false;
        }

        if (distSq > maxParticleDistanceSq) {
            return true;
        }

        activeFrameParticles++;
        return false;
    }

    public static double getDynamicQualityScale() {
        return dynamicQualityScale;
    }

    public static CullingMode getActiveCullingMode() {
        return activeCullingMode;
    }
}