package org.windomium.core.profiler;

import java.util.Arrays;

public final class FrameTimeTracker {
    private static final int CAPACITY = 2048;
    private final long[] frameDurationsNs = new long[CAPACITY];
    private final long[] sortBuffer = new long[CAPACITY];
    private int writeIndex = 0;
    private int count = 0;
    private long frameStartNs = 0L;

    public void beginFrame() {
        this.frameStartNs = System.nanoTime();
    }

    public void endFrame() {
        if (this.frameStartNs == 0L) {
            return;
        }
        long duration = System.nanoTime() - this.frameStartNs;
        this.frameDurationsNs[this.writeIndex] = duration;
        this.writeIndex = (this.writeIndex + 1) & (CAPACITY - 1);
        if (this.count < CAPACITY) {
            this.count++;
        }
    }

    public FrameMetricsSnapshot createSnapshot() {
        int currentCount = this.count;
        if (currentCount == 0) {
            return FrameMetricsSnapshot.EMPTY;
        }

        System.arraycopy(this.frameDurationsNs, 0, this.sortBuffer, 0, currentCount);
        Arrays.sort(this.sortBuffer, 0, currentCount);

        long totalNs = 0L;
        for (int i = 0; i < currentCount; i++) {
            totalNs += this.sortBuffer[i];
        }

        double avgNs = (double) totalNs / currentCount;
        double avgMs = avgNs / 1_000_000.0;
        double avgFps = avgNs > 0 ? 1_000_000_000.0 / avgNs : 0.0;

        double varianceSum = 0.0;
        for (int i = 0; i < currentCount; i++) {
            double diffMs = (this.sortBuffer[i] / 1_000_000.0) - avgMs;
            varianceSum += diffMs * diffMs;
        }
        double varianceMs = varianceSum / currentCount;

        int onePercentCount = Math.max(1, currentCount / 100);
        long onePercentWorstTotalNs = 0L;
        for (int i = currentCount - onePercentCount; i < currentCount; i++) {
            onePercentWorstTotalNs += this.sortBuffer[i];
        }
        double onePercentAvgNs = (double) onePercentWorstTotalNs / onePercentCount;
        double onePercentLowFps = onePercentAvgNs > 0 ? 1_000_000_000.0 / onePercentAvgNs : 0.0;

        int pointOnePercentCount = Math.max(1, currentCount / 1000);
        long pointOnePercentWorstTotalNs = 0L;
        for (int i = currentCount - pointOnePercentCount; i < currentCount; i++) {
            pointOnePercentWorstTotalNs += this.sortBuffer[i];
        }
        double pointOnePercentAvgNs = (double) pointOnePercentWorstTotalNs / pointOnePercentCount;
        double pointOnePercentLowFps = pointOnePercentAvgNs > 0 ? 1_000_000_000.0 / pointOnePercentAvgNs : 0.0;

        return new FrameMetricsSnapshot(
                avgFps,
                onePercentLowFps,
                pointOnePercentLowFps,
                avgMs,
                varianceMs,
                currentCount
        );
    }

    public void reset() {
        this.writeIndex = 0;
        this.count = 0;
        this.frameStartNs = 0L;
    }
}