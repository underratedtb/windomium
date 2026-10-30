package org.windomium.core.compat;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public final class CompatibilityDetector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean sodiumOrForkLoaded;
    private static boolean irisOrForkLoaded;

    private CompatibilityDetector() {
    }

    public static void detectInstalledMods() {
        ModList modList = ModList.get();
        sodiumOrForkLoaded = modList.isLoaded("sodium") || modList.isLoaded("embeddium") || modList.isLoaded("rubidium");
        irisOrForkLoaded = modList.isLoaded("iris") || modList.isLoaded("oculus");

        if (sodiumOrForkLoaded) {
            LOGGER.info("Detected Sodium/Embeddium. Windomium will operate in Companion Mode and disable overlapping chunk render hooks.");
        } else {
            LOGGER.info("No external chunk render engine detected. Windomium Standalone Render Pipeline is available.");
        }

        if (irisOrForkLoaded) {
            LOGGER.info("Detected Iris/Oculus shader pipeline. Shader-safe rendering guards are active. Optimisation now more powerfull");
        }
    }

    public static boolean isSodiumOrForkLoaded() {
        return sodiumOrForkLoaded;
    }

    public static boolean isIrisOrForkLoaded() {
        return irisOrForkLoaded;
    }
}
