package org.windomium.core.profiler;

public record FrameMetricsSnapshot(
        double averageFps,
        double onePercentLowFps,
        double pointOnePercentLowFps,
        double averageFrameTimeMs,
        double frameTimeVarianceMs,
        int sampleCount
) {
    public static final FrameMetricsSnapshot EMPTY = new FrameMetricsSnapshot(0.0, 0.0, 0.0, 0.0, 0.0, 0);
}