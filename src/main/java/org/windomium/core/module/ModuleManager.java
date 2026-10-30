package org.windomium.core.module;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.windomium.api.IOptimisationModule;
import org.windomium.api.RiskLevel;
import org.windomium.core.config.Preset;
import org.windomium.core.config.WindomiumConfig;

public final class ModuleManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ModuleContainer> MODULES = new ConcurrentHashMap<>();
    private static volatile int activeModuleCount = 0;

    private ModuleManager() {
    }

    public static void register(IOptimisationModule module) {
        ModuleContainer container = new ModuleContainer(module);
        MODULES.put(module.getId(), container);
        try {
            module.onInitialise();
            LOGGER.info("Registered Windomium module: {} [{}]", module.getDisplayName(), module.getRiskLevel().getDisplayName());
        } catch (Throwable throwable) {
            container.markFaulted(throwable);
            LOGGER.error("Failed to initialise module '{}'. Module has been isolated and disabled.", module.getId(), throwable);
        }
        recalculateActiveCount();
    }

    public static void applyPreset(Preset preset) {
        for (ModuleContainer container : MODULES.values()) {
            if (container.isFaulted()) {
                continue;
            }
            IOptimisationModule module = container.getModule();
            boolean shouldEnable = switch (preset) {
                case SAFE -> module.guaranteesVisualParity() && module.getRiskLevel() == RiskLevel.SAFE;
                case BALANCED -> module.guaranteesVisualParity() && module.getRiskLevel() != RiskLevel.EXPERIMENTAL;
                case EXTREME -> true;
                case CUSTOM -> container.isEnabled();
            };
            setModuleState(module.getId(), shouldEnable);
        }
        recalculateActiveCount();
    }

    public static void setModuleState(String moduleId, boolean enable) {
        ModuleContainer container = MODULES.get(moduleId);
        if (container == null || container.isFaulted()) {
            return;
        }
        if (container.isEnabled() == enable) {
            return;
        }
        try {
            if (enable) {
                container.getModule().onEnable();
                container.setEnabled(true);
            } else {
                container.getModule().onDisable();
                container.setEnabled(false);
            }
        } catch (Throwable throwable) {
            handleModuleException(moduleId, throwable);
        }
        recalculateActiveCount();
    }

    public static void executeSafely(String moduleId, Runnable task) {
        ModuleContainer container = MODULES.get(moduleId);
        if (container == null || !container.isEnabled()) {
            return;
        }
        try {
            task.run();
        } catch (Throwable throwable) {
            handleModuleException(moduleId, throwable);
        }
    }

    public static void handleModuleException(String moduleId, Throwable throwable) {
        ModuleContainer container = MODULES.get(moduleId);
        if (container != null && !container.isFaulted()) {
            container.markFaulted(throwable);
            try {
                container.getModule().onDisable();
            } catch (Throwable ignored) {
            }
            recalculateActiveCount();
            LOGGER.error("Module '{}' encountered a runtime exception and was automatically disabled to prevent a crash.", moduleId, throwable);
        }
    }

    private static void recalculateActiveCount() {
        int count = 0;
        for (ModuleContainer container : MODULES.values()) {
            if (container.isEnabled()) {
                count++;
            }
        }
        activeModuleCount = count;
    }

    public static int getActiveModuleCount() {
        return activeModuleCount;
    }

    public static boolean isModuleActive(String moduleId) {
        ModuleContainer container = MODULES.get(moduleId);
        return container != null && container.isEnabled();
    }

    public static Collection<ModuleContainer> getAllModules() {
        return Collections.unmodifiableCollection(MODULES.values());
    }

    public static void syncWithConfig() {
        if (WindomiumConfig.CLIENT_SPEC.isLoaded()) {
            applyPreset(WindomiumConfig.ACTIVE_PRESET.get());
        }
    }
}