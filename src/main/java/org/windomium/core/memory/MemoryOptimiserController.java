package org.windomium.core.memory;

import org.windomium.client.culling.OcclusionCuller;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

public final class MemoryOptimiserController {
    private static long lastSampleTimeNs = 0L;
    private static long lastUsedBytes = 0L;
    private static long usedHeapMb = 0L;
    private static long maxHeapMb = 0L;
    private static double allocationRateMbPerSec = 0.0;
    private static boolean moduleActive = false;

    private MemoryOptimiserController() {
    }

    public static void onFrameSample() {
        boolean configEnabled = !WindomiumConfig.CLIENT_SPEC.isLoaded() || WindomiumConfig.ENABLE_MEMORY_OPTIMISER.get();
        moduleActive = configEnabled && ModuleManager.isModuleActive("memory_optimiser");

        long now = System.nanoTime();
        if (now - lastSampleTimeNs < 500_000_000L) {
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        long currentUsedBytes = runtime.totalMemory() - runtime.freeMemory();
        long currentMaxBytes = runtime.maxMemory();

        usedHeapMb = currentUsedBytes / (1024L * 1024L);
        maxHeapMb = currentMaxBytes / (1024L * 1024L);

        if (lastSampleTimeNs != 0L) {
            long deltaBytes = currentUsedBytes - lastUsedBytes;
            double elapsedSec = (now - lastSampleTimeNs) / 1_000_000_000.0;
            if (deltaBytes > 0 && elapsedSec > 0.0) {
                double instantRate = (deltaBytes / (1024.0 * 1024.0)) / elapsedSec;
                allocationRateMbPerSec = (allocationRateMbPerSec * 0.6) + (instantRate * 0.4);
            }
        }

        lastUsedBytes = currentUsedBytes;
        lastSampleTimeNs = now;

        if (moduleActive && maxHeapMb > 0L) {
            double usageRatio = (double) usedHeapMb / maxHeapMb;
            if (usageRatio > 0.88) {
                OcclusionCuller.clearCache();
                ObjectPoolManager.clearCurrentThreadPools();
            }
        }
    }

    public static void trimCaches() {
        OcclusionCuller.clearCache();
        ObjectPoolManager.clearCurrentThreadPools();
    }

    public static boolean isModuleActive() {
        return moduleActive;
    }

    public static long getUsedHeapMb() {
        return usedHeapMb;
    }

    public static long getMaxHeapMb() {
        return maxHeapMb;
    }

    public static double getAllocationRateMbPerSec() {
        return allocationRateMbPerSec;
    }
}