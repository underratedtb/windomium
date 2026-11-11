package org.windomium.client.profiler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.windomium.client.culling.CullingController;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.memory.MemoryOptimiserController;
import org.windomium.core.module.ModuleManager;
import org.windomium.core.profiler.FrameMetricsSnapshot;
import org.windomium.core.profiler.SectionTimings;

public final class ProfilerOverlay {
    private static final int TEXT_COLOUR = 0xE0E0E0;
    private static final int ACCENT_COLOUR = 0x55FF55;
    private static final int FAULT_COLOUR = 0xFF5555;
    private static long lastTextRefreshNs = 0L;
    private static String cachedLine1 = "Windomium Engine";
    private static String cachedLine2 = "Initialising metrics...";
    private static String cachedLine3 = "";
    private static String cachedLine4 = "";
    private static String cachedLine5 = "";
    private static String cachedLine6 = "";

    private ProfilerOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        if (!WindomiumConfig.CLIENT_SPEC.isLoaded() || !WindomiumConfig.SHOW_PROFILER_OVERLAY.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }

        int faultedCount = ModuleManager.getFaultedModuleCount();
        long now = System.nanoTime();
        if (now - lastTextRefreshNs >= 250_000_000L) {
            lastTextRefreshNs = now;
            FrameMetricsSnapshot snapshot = ClientProfilerManager.getLatestSnapshot();
            SectionTimings timings = ClientProfilerManager.getSectionTimings();

            String faultSuffix = faultedCount > 0 ? (" | FAULTED: " + faultedCount) : "";
            cachedLine1 = String.format(
                    "Windomium [%s | Culling: %s] | Modules: %d%s | Scale: %.0f%%",
                    WindomiumConfig.ACTIVE_PRESET.get().name(),
                    CullingController.getActiveCullingMode().name(),
                    ModuleManager.getActiveModuleCount(),
                    faultSuffix,
                    CullingController.getDynamicQualityScale() * 100.0
            );
            cachedLine2 = String.format(
                    "FPS: %.1f | 1%% Low: %.1f | 0.1%% Low: %.1f | Frame: %.2f ms (Var: %.2f)",
                    snapshot.averageFps(),
                    snapshot.onePercentLowFps(),
                    snapshot.pointOnePercentLowFps(),
                    snapshot.averageFrameTimeMs(),
                    snapshot.frameTimeVarianceMs()
            );
            cachedLine3 = String.format(
                    "Timings -> CPU: %.2f ms | Culling: %.2f ms",
                    timings.getCpuFrameTimeMs(),
                    timings.getCullingTimeMs()
            );
            cachedLine4 = String.format(
                    "Sections: %d drawn / %d culled | Entities: %d drawn / %d culled",
                    timings.getRenderedSections(),
                    timings.getCulledSections(),
                    timings.getRenderedEntities(),
                    timings.getCulledEntities()
            );
            cachedLine5 = String.format(
                    "BlockEntities: %d drawn / %d culled | Skipped Layers/Shadows: %d",
                    timings.getRenderedBlockEntities(),
                    timings.getCulledBlockEntities(),
                    timings.getSkippedEntityLayers()
            );
            cachedLine6 = String.format(
                    "RAM: %d / %d MB | Alloc: %.1f MB/s",
                    MemoryOptimiserController.getUsedHeapMb(),
                    MemoryOptimiserController.getMaxHeapMb(),
                    MemoryOptimiserController.getAllocationRateMbPerSec()
            );
        }

        Font font = mc.font;
        int x = 6;
        int y = 6;
        int headerColour = faultedCount > 0 ? FAULT_COLOUR : ACCENT_COLOUR;
        graphics.drawString(font, cachedLine1, x, y, headerColour, true);
        graphics.drawString(font, cachedLine2, x, y + 11, TEXT_COLOUR, true);
        graphics.drawString(font, cachedLine3, x, y + 22, TEXT_COLOUR, true);
        graphics.drawString(font, cachedLine4, x, y + 33, TEXT_COLOUR, true);
        graphics.drawString(font, cachedLine5, x, y + 44, TEXT_COLOUR, true);
        graphics.drawString(font, cachedLine6, x, y + 55, TEXT_COLOUR, true);
    }
}