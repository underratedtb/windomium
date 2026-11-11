package org.windomium.client.culling;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import org.windomium.core.profiler.SectionTimings;

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
    private static int activeTickParticles = 0;
    private static int maxParticlesPerTick = 220;
    private static int timingSampleCounter = 0;
    private static boolean frustumActive = false;
    private static boolean occlusionActive = false;
    private static boolean particleActive = false;
    private static CullingMode activeCullingMode = CullingMode.COMBINED;

    private CullingController() {
    }

    public static void onClientTick() {
        activeTickParticles = 0;
    }

    public static void onFrameBegin() {
        ClientProfilerManager.getSectionTimings().resetCounters();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null) {
            frustumActive = false;
            occlusionActive = false;
            particleActive = false;
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

        particleActive = ModuleManager.isModuleActive("particle_optimiser");

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 pos = camera.getPosition();
        cameraX = pos.x;
        cameraY = pos.y;
        cameraZ = pos.z;

        if (occlusionActive) {
            OcclusionCuller.onFrameBegin(cameraX, cameraY, cameraZ);
        }

        updateGovernor(mc);

        int renderChunks = mc.options.getEffectiveRenderDistance();
        double baseSectionBlocks = (renderChunks * 16.0 * 1.42) + 16.0;
        maxSectionDistanceSq = baseSectionBlocks * baseSectionBlocks;

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
        maxParticlesPerTick = switch (preset) {
            case SAFE -> 450;
            case BALANCED -> 220;
            case EXTREME -> 90;
            case CUSTOM -> 220;
        };

        double scaledEntity = entityBase * dynamicQualityScale;
        double scaledBlockEntity = blockEntityBase * dynamicQualityScale;
        double scaledParticle = particleBase * dynamicQualityScale;

        maxEntityDistanceSq = scaledEntity * scaledEntity;
        maxBlockEntityDistanceSq = scaledBlockEntity * scaledBlockEntity;
        maxParticleDistanceSq = scaledParticle * scaledParticle;
    }

    private static void updateGovernor(Minecraft mc) {
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

        boolean vsync = mc.options.enableVsync().get();
        int monitorHz = mc.getWindow().getRefreshRate();
        int vsyncCap = (vsync && monitorHz > 0) ? monitorHz : 1000;

        int userLimit = mc.options.framerateLimit().get();
        int fpsSliderCap = (userLimit > 0 && userLimit < 260) ? userLimit : 1000;

        int effectiveCeiling = Math.min(fpsSliderCap, vsyncCap);
        int targetFps = Math.min(WindomiumConfig.TARGET_FPS.get(), effectiveCeiling);
        int targetOnePercent = Math.min(WindomiumConfig.TARGET_ONE_PERCENT_LOW.get(), (int) (targetFps * 0.75));

        double currentFps = snapshot.averageFps();
        double currentOnePercent = snapshot.onePercentLowFps();

        SectionTimings timings = ClientProfilerManager.getSectionTimings();
        int activeSceneObjects = timings.getRenderedEntities() + timings.getRenderedBlockEntities();
        boolean sceneHasScalableLoad = activeSceneObjects >= 10 || timings.getCullingTimeMs() > 0.25;

        boolean belowAvgTarget = currentFps < targetFps * 0.88;
        boolean belowOnePercentTarget = currentOnePercent < targetOnePercent * 0.82;
        boolean aboveRecoveryThreshold = currentFps >= targetFps * 0.96 && currentOnePercent >= targetOnePercent * 0.90;

        if ((belowAvgTarget || belowOnePercentTarget) && sceneHasScalableLoad) {
            dynamicQualityScale = Math.max(0.65, dynamicQualityScale - 0.04);
        } else if (aboveRecoveryThreshold) {
            dynamicQualityScale = Math.min(1.0, dynamicQualityScale + 0.04);
        } else if (!sceneHasScalableLoad && dynamicQualityScale < 1.0) {
            dynamicQualityScale = Math.min(1.0, dynamicQualityScale + 0.02);
        }
    }

    public static boolean shouldCullBox(AABB box) {
        if ((!frustumActive && !occlusionActive) || box == null || !RenderSystem.isOnRenderThread()) {
            return false;
        }

        boolean sampleTime = ((timingSampleCounter++ & 63) == 0);
        long startNs = sampleTime ? System.nanoTime() : 0L;

        try {
            double spanX = box.maxX - box.minX;
            double spanY = box.maxY - box.minY;
            double spanZ = box.maxZ - box.minZ;

            boolean isChunkSection = spanX >= 15.5 && spanX <= 16.5 && spanY >= 15.5 && spanY <= 16.5 && spanZ >= 15.5 && spanZ <= 16.5;
            if (!isChunkSection) {
                return false;
            }

            double centreX = box.minX + 8.0;
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
                if (horizontalDistSq > 1024.0 && OcclusionCuller.isBoxOccluded(box, cameraX, cameraY, cameraZ, true)) {
                    ClientProfilerManager.getSectionTimings().incrementCulledSections(1);
                    return true;
                }
            }

            ClientProfilerManager.getSectionTimings().incrementRenderedSections(1);
            return false;
        } catch (Throwable throwable) {
            ModuleManager.handleModuleException(occlusionActive ? "occlusion_culling" : "frustum_culling", throwable);
            return false;
        } finally {
            if (sampleTime) {
                ClientProfilerManager.getSectionTimings().addCullingTimeNs((System.nanoTime() - startNs) * 64L);
            }
        }
    }

    public static boolean shouldCullEntity(Entity entity, double camX, double camY, double camZ) {
        if ((!frustumActive && !occlusionActive) || entity == null || !RenderSystem.isOnRenderThread()) {
            return false;
        }

        try {
            if (entity.noCulling || entity instanceof Player || entity.isSpectator() || entity.isCurrentlyGlowing() || entity.hasCustomName() || entity.isVehicle() || entity.isPassenger()) {
                ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
                return false;
            }

            if (ModCompatibilityManager.isClassBypassed(entity.getClass().getName())) {
                ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
                return false;
            }

            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (key != null && WindomiumAPI.getEntityBehaviour(key.toString()).orElse(RenderBehaviour.CULLABLE) == RenderBehaviour.DO_NOT_TOUCH) {
                ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
                return false;
            }

            double distSq = entity.distanceToSqr(camX, camY, camZ);
            if (distSq > SUB_LEVEL_DISTANCE_SQ_THRESHOLD) {
                ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
                return false;
            }

            AABB boundingBox = entity.getBoundingBox();
            double sizeScale = Math.max(1.0, Math.min(4.0, boundingBox.getSize()));
            double effectiveMaxDistSq = maxEntityDistanceSq * sizeScale * sizeScale;

            if (frustumActive && distSq > effectiveMaxDistSq) {
                ClientProfilerManager.getSectionTimings().incrementCulledEntities(1);
                return true;
            }

            if (occlusionActive && distSq > 256.0 && OcclusionCuller.isBoxOccluded(boundingBox, camX, camY, camZ, false)) {
                ClientProfilerManager.getSectionTimings().incrementCulledEntities(1);
                return true;
            }

            ClientProfilerManager.getSectionTimings().incrementRenderedEntities(1);
            return false;
        } catch (Throwable throwable) {
            ModuleManager.handleModuleException(occlusionActive ? "occlusion_culling" : "frustum_culling", throwable);
            return false;
        }
    }

    public static boolean shouldCullBlockEntity(BlockEntity blockEntity) {
        if ((!frustumActive && !occlusionActive) || blockEntity == null || !RenderSystem.isOnRenderThread()) {
            return false;
        }

        try {
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

            if (occlusionActive && distSq > 256.0) {
                AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
                return OcclusionCuller.isBoxOccluded(box, cameraX, cameraY, cameraZ, false);
            }

            return false;
        } catch (Throwable throwable) {
            ModuleManager.handleModuleException(occlusionActive ? "occlusion_culling" : "frustum_culling", throwable);
            return false;
        }
    }

    public static boolean shouldCullParticle(AABB box) {
        if (!particleActive || box == null || !RenderSystem.isOnRenderThread()) {
            return false;
        }

        try {
            if (activeTickParticles >= maxParticlesPerTick) {
                return true;
            }

            double dx = box.minX - cameraX;
            double dy = box.minY - cameraY;
            double dz = box.minZ - cameraZ;
            double distSq = (dx * dx) + (dy * dy) + (dz * dz);

            if (distSq > SUB_LEVEL_DISTANCE_SQ_THRESHOLD) {
                activeTickParticles++;
                return false;
            }

            if (distSq > maxParticleDistanceSq) {
                return true;
            }

            activeTickParticles++;
            return false;
        } catch (Throwable throwable) {
            ModuleManager.handleModuleException("particle_optimiser", throwable);
            return false;
        }
    }

    public static double getDynamicQualityScale() {
        return dynamicQualityScale;
    }

    public static CullingMode getActiveCullingMode() {
        return activeCullingMode;
    }
}