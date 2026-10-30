package org.windomium.core.memory;

import com.mojang.logging.LogUtils;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import org.slf4j.Logger;

public final class JvmGcAdvisor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static String detectedGcName = "Unknown GC";
    private static long maxHeapMb = 0L;
    private static String recommendationSummary = "";

    private JvmGcAdvisor() {
    }

    public static void analyseJvmEnvironment() {
        Runtime runtime = Runtime.getRuntime();
        maxHeapMb = runtime.maxMemory() / (1024L * 1024L);

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        StringBuilder gcNames = new StringBuilder();
        for (int i = 0; i < gcBeans.size(); i++) {
            if (i > 0) {
                gcNames.append(", ");
            }
            gcNames.append(gcBeans.get(i).getName());
        }
        detectedGcName = !gcNames.isEmpty() ? gcNames.toString() : "Standard JVM GC";

        List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean usesZgc = inputArgs.stream().anyMatch(arg -> arg.contains("UseZGC"));
        boolean usesShenandoah = inputArgs.stream().anyMatch(arg -> arg.contains("UseShenandoahGC"));
        boolean usesG1 = inputArgs.stream().anyMatch(arg -> arg.contains("UseG1GC")) || detectedGcName.contains("G1");

        if (maxHeapMb < 3500L) {
            recommendationSummary = "Allocated RAM (" + maxHeapMb + " MB) is tight for high render distances. Recommended: 4096-6144 MB with G1GC.";
        } else if (usesZgc) {
            recommendationSummary = "Generational ZGC active (" + maxHeapMb + " MB). Optimal for ultra-low frame time variance.";
        } else if (usesShenandoah) {
            recommendationSummary = "Shenandoah GC active (" + maxHeapMb + " MB). Good low-pause configuration.";
        } else if (usesG1) {
            recommendationSummary = "G1GC active (" + maxHeapMb + " MB). Ensure -XX:MaxGCPauseMillis=30 and -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 are set.";
        } else {
            recommendationSummary = "Consider enabling Generational ZGC (-XX:+UseZGC -XX:+ZGenerational) or tuned G1GC (-XX:+UseG1GC -XX:MaxGCPauseMillis=30).";
        }

        LOGGER.info("[Windomium JVM Advisor] Detected GC: {} | Max Heap: {} MB", detectedGcName, maxHeapMb);
        LOGGER.info("[Windomium JVM Advisor] Advice: {}", recommendationSummary);
    }

    public static String getDetectedGcName() {
        return detectedGcName;
    }

    public static long getMaxHeapMb() {
        return maxHeapMb;
    }

    public static String getRecommendationSummary() {
        return recommendationSummary;
    }

    public static String getRecommendedFlagsFor4GbTo6Gb() {
        return "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=30 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:G1HeapRegionSize=32M";
    }
}