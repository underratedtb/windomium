package org.windomium.core.compat;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.windomium.api.RenderBehaviour;
import org.windomium.api.WindomiumAPI;
import org.windomium.core.config.WindomiumConfig;

public final class ModCompatibilityManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> ACTIVE_COMPAT_PROFILES = ConcurrentHashMap.newKeySet();
    private static final Set<String> BYPASSED_CLASSES = ConcurrentHashMap.newKeySet();

    private ModCompatibilityManager() {
    }

    public static void applyBuiltInProfiles() {
        ACTIVE_COMPAT_PROFILES.clear();
        ModList modList = ModList.get();

        registerProfileIfLoaded(modList, "create", () -> {
            WindomiumAPI.registerBlockEntityBehaviour("create:kinetic", RenderBehaviour.DO_NOT_TOUCH);
            WindomiumAPI.registerEntityBehaviour("create:contraption", RenderBehaviour.DO_NOT_TOUCH);
            WindomiumAPI.registerEntityBehaviour("create:stationary_contraption", RenderBehaviour.DO_NOT_TOUCH);
        });

        registerProfileIfLoaded(modList, "mekanism", () -> {
            WindomiumAPI.registerBlockEntityBehaviour("mekanism:transmitter", RenderBehaviour.THROTTLEABLE);
            WindomiumAPI.registerBlockEntityBehaviour("mekanism:digital_miner", RenderBehaviour.DO_NOT_TOUCH);
        });

        registerProfileIfLoaded(modList, "ae2", () -> {
            WindomiumAPI.registerBlockEntityBehaviour("ae2:cable_bus", RenderBehaviour.THROTTLEABLE);
        });

        registerProfileIfLoaded(modList, "botania", () -> {
            WindomiumAPI.registerBlockEntityBehaviour("botania:mana_pylon", RenderBehaviour.CULLABLE);
        });

        registerProfileIfLoaded(modList, "immersiveengineering", () -> {
            WindomiumAPI.registerBlockEntityBehaviour("immersiveengineering:multiblock", RenderBehaviour.DO_NOT_TOUCH);
        });

        registerProfileIfLoaded(modList, "geckolib", () -> {
            LOGGER.info("[Windomium Compat] GeckoLib detected. Animated entity layer guards enabled.");
        });

        registerProfileIfLoaded(modList, "alexsmobs", () -> {
            LOGGER.info("[Windomium Compat] Alex's Mobs profile active.");
        });

        registerProfileIfLoaded(modList, "sophisticatedbackpacks", () -> {
            LOGGER.info("[Windomium Compat] Sophisticated Backpacks profile active.");
        });

        registerProfileIfLoaded(modList, "sophisticatedstorage", () -> {
            LOGGER.info("[Windomium Compat] Sophisticated Storage profile active.");
        });

        registerProfileIfLoaded(modList, "farmersdelight", () -> {
            LOGGER.info("[Windomium Compat] Farmer's Delight profile active.");
        });

        syncUserBlacklist();
    }

    private static void registerProfileIfLoaded(ModList modList, String modId, Runnable profileAction) {
        if (modList.isLoaded(modId)) {
            profileAction.run();
            ACTIVE_COMPAT_PROFILES.add(modId);
            LOGGER.info("[Windomium Compat] Loaded built-in compatibility profile for mod: {}", modId);
        }
    }

    public static void syncUserBlacklist() {
        if (!WindomiumConfig.CLIENT_SPEC.isLoaded()) {
            return;
        }
        List<? extends String> configuredList = WindomiumConfig.BLACKLISTED_CLASSES.get();
        for (String entry : configuredList) {
            if (entry != null && !entry.isBlank()) {
                String trimmed = entry.trim();
                BYPASSED_CLASSES.add(trimmed);
                WindomiumAPI.bypassRendererClass(trimmed);
            }
        }
    }

    public static boolean isClassBypassed(String className) {
        if (className == null) {
            return false;
        }
        if (BYPASSED_CLASSES.contains(className)) {
            return true;
        }
        return WindomiumAPI.getRendererBehaviour(className).orElse(RenderBehaviour.CULLABLE) == RenderBehaviour.DO_NOT_TOUCH;
    }

    public static void isolateFaultyRenderer(String className, Throwable error) {
        if (className != null && BYPASSED_CLASSES.add(className)) {
            WindomiumAPI.bypassRendererClass(className);
            LOGGER.warn("[Windomium Compat] Renderer '{}' threw an exception and was automatically added to the bypass list.", className, error);
        }
    }

    public static Set<String> getActiveCompatProfiles() {
        return Collections.unmodifiableSet(ACTIVE_COMPAT_PROFILES);
    }
}