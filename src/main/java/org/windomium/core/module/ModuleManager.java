package org.windomium.core.module;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.windomium.api.IOptimisationModule;
import org.windomium.api.RiskLevel;
import org.windomium.core.config.Preset;
import org.windomium.core.config.WindomiumConfig;

public final class ModuleManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ModuleContainer> MODULES = new ConcurrentHashMap<>();
    private static volatile int activeModuleCount = 0;
    private static volatile int faultedModuleCount = 0;
    private static volatile BiConsumer<String, Throwable> faultNotifier;

    private ModuleManager() {
    }

    public static void setFaultNotifier(BiConsumer<String, Throwable> notifier) {
        faultNotifier = notifier;
    }

    public static void register(IOptimisationModule module) {
        ModuleContainer container = new ModuleContainer(module);
        MODULES.put(module.getId(), container);
        try {
            module.onInitialise();
            LOGGER.info("Registered Windomium module: {} [{}]", module.getDisplayName(), module.getRiskLevel().getDisplayName());
        } catch (Throwable throwable) {
            if (throwable instanceof VirtualMachineError vmError) {
                throw vmError;
            }
            container.tripBreaker(throwable);
            LOGGER.error("Failed to initialise module '{}'. Module has been isolated and disabled.", module.getId(), throwable);
        }
        recalculateCounts();
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
        recalculateCounts();
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
            container.recordSuccess();
        } catch (Throwable throwable) {
            if (throwable instanceof VirtualMachineError vmError) {
                throw vmError;
            }
            tripModuleImmediately(container, throwable);
        }
        recalculateCounts();
    }

    public static void executeSafely(String moduleId, Runnable task) {
        ModuleContainer container = MODULES.get(moduleId);
        if (container == null || !container.isEnabled()) {
            return;
        }
        try {
            task.run();
            container.recordSuccess();
        } catch (Throwable throwable) {
            if (throwable instanceof VirtualMachineError vmError) {
                throw vmError;
            }
            handleModuleException(moduleId, throwable);
        }
    }

    public static void handleModuleException(String moduleId, Throwable throwable) {
        if (throwable instanceof VirtualMachineError vmError) {
            throw vmError;
        }
        ModuleContainer container = MODULES.get(moduleId);
        if (container != null && container.recordFailure(throwable)) {
            performRollbackAndNotify(container, throwable);
        }
    }

    private static void tripModuleImmediately(ModuleContainer container, Throwable throwable) {
        if (container.tripBreaker(throwable)) {
            performRollbackAndNotify(container, throwable);
        }
    }

    private static void performRollbackAndNotify(ModuleContainer container, Throwable throwable) {
        IOptimisationModule module = container.getModule();
        try {
            module.onErrorRollback();
        } catch (Throwable rollbackError) {
            if (rollbackError instanceof VirtualMachineError vmError) {
                throw vmError;
            }
        }
        recalculateCounts();
        LOGGER.error("Circuit breaker tripped for module '{}'. Module rolled back and disabled.", module.getId(), throwable);
        BiConsumer<String, Throwable> notifier = faultNotifier;
        if (notifier != null) {
            try {
                notifier.accept(module.getDisplayName(), throwable);
            } catch (Throwable ignored) {
            }
        }
    }

    public static int resetFaultedModules() {
        int recovered = 0;
        for (ModuleContainer container : MODULES.values()) {
            if (container.isFaulted()) {
                container.resetFaultState();
                recovered++;
            }
        }
        syncWithConfig();
        recalculateCounts();
        return recovered;
    }

    private static void recalculateCounts() {
        int active = 0;
        int faulted = 0;
        for (ModuleContainer container : MODULES.values()) {
            if (container.isFaulted()) {
                faulted++;
            } else if (container.isEnabled()) {
                active++;
            }
        }
        activeModuleCount = active;
        faultedModuleCount = faulted;
    }

    public static int getActiveModuleCount() {
        return activeModuleCount;
    }

    public static int getFaultedModuleCount() {
        return faultedModuleCount;
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