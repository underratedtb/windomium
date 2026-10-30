package org.windomium.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.windomium.client.profiler.ClientProfilerManager;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

public final class ChunkRenderManager {
    private ChunkRenderManager() {
    }

    public static void scheduleChunkRebuild(BlockPos pos, Runnable rebuildTask) {
        if (!WindomiumConfig.CLIENT_SPEC.isLoaded() || !WindomiumConfig.ENABLE_ASYNC_MESHING.get()) {
            rebuildTask.run();
            return;
        }

        if (!ModuleManager.isModuleActive("chunk_mesher")) {
            rebuildTask.run();
            return;
        }

        int priority = calculatePriority(pos);
        RenderTask task = new RenderTask(pos.immutable(), priority, () -> processMesh(rebuildTask));
        MesherThreadPool.submit(task);
    }

    private static void processMesh(Runnable rebuildTask) {
        ModuleManager.executeSafely("chunk_mesher", () -> {
            long startNs = System.nanoTime();
            rebuildTask.run();
            long elapsedNs = System.nanoTime() - startNs;
            ClientProfilerManager.getSectionTimings().setChunkMeshTimeNs(elapsedNs);
        });
    }

    private static int calculatePriority(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.cameraEntity == null) {
            return 0;
        }

        Vec3 cameraPos = mc.cameraEntity.position();
        double distanceSq = cameraPos.distanceToSqr(pos.getX() + 8.0, pos.getY() + 8.0, pos.getZ() + 8.0);

        if (distanceSq < 256.0) {
            return 100;
        }
        if (distanceSq < 1024.0) {
            return 50;
        }
        return 10;
    }
}