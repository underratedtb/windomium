package org.windomium.client.profiler;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.windomium.core.profiler.FrameMetricsSnapshot;
import org.windomium.core.profiler.FrameTimeTracker;
import org.windomium.core.profiler.SectionTimings;

public final class ClientProfilerManager {
    private static final FrameTimeTracker TRACKER = new FrameTimeTracker();
    private static final SectionTimings SECTION_TIMINGS = new SectionTimings();
    private static FrameMetricsSnapshot latestSnapshot = FrameMetricsSnapshot.EMPTY;
    private static long lastSnapshotUpdateNs = 0L;
    private static long cpuStartNs = 0L;

    private static boolean activeSession = false;
    private static long sessionEndNs = 0L;
    private static final FrameTimeTracker SESSION_TRACKER = new FrameTimeTracker();

    private ClientProfilerManager() {
    }

    public static void onFrameStart() {
        cpuStartNs = System.nanoTime();
        TRACKER.beginFrame();
        if (activeSession) {
            SESSION_TRACKER.beginFrame();
        }
    }

    public static void onFrameEnd() {
        long now = System.nanoTime();
        if (cpuStartNs != 0L) {
            SECTION_TIMINGS.setCpuFrameTimeNs(now - cpuStartNs);
        }

        TRACKER.endFrame();

        if (now - lastSnapshotUpdateNs >= 500_000_000L) {
            latestSnapshot = TRACKER.createSnapshot();
            lastSnapshotUpdateNs = now;
        }

        if (activeSession) {
            SESSION_TRACKER.endFrame();
            if (now >= sessionEndNs) {
                activeSession = false;
                FrameMetricsSnapshot report = SESSION_TRACKER.createSnapshot();
                sendSessionReport(report);
            }
        }
    }

    public static void startProfilingSession(int durationSeconds) {
        SESSION_TRACKER.reset();
        sessionEndNs = System.nanoTime() + (durationSeconds * 1_000_000_000L);
        activeSession = true;
    }

    private static void sendSessionReport(FrameMetricsSnapshot report) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String message = String.format(
                    "[Windomium Profile] Avg FPS: %.1f | 1%% Low: %.1f | 0.1%% Low: %.1f | Avg Frame: %.2f ms | Variance: %.3f ms² (%d frames)",
                    report.averageFps(),
                    report.onePercentLowFps(),
                    report.pointOnePercentLowFps(),
                    report.averageFrameTimeMs(),
                    report.frameTimeVarianceMs(),
                    report.sampleCount()
            );
            mc.player.sendSystemMessage(Component.literal(message));
        }
    }

    public static FrameMetricsSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    public static SectionTimings getSectionTimings() {
        return SECTION_TIMINGS;
    }
}