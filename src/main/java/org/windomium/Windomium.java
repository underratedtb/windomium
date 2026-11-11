package org.windomium;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.windomium.api.WindomiumAPI;
import org.windomium.client.WindomiumClient;
import org.windomium.client.module.ClientModuleBootstrap;
import org.windomium.core.compat.CompatibilityDetector;
import org.windomium.core.compat.ModCompatibilityManager;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.memory.JvmGcAdvisor;
import org.windomium.core.module.ModuleManager;
import org.windomium.server.ServerTickModule;

@Mod(Windomium.MOD_ID)
public final class Windomium {
    public static final String MOD_ID = "windomium";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Windomium(IEventBus modEventBus, ModContainer modContainer) {
        WindomiumAPI.setModuleRegistrar(ModuleManager::register);

        modContainer.registerConfig(ModConfig.Type.CLIENT, WindomiumConfig.CLIENT_SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, WindomiumConfig.COMMON_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, WindomiumConfig.SERVER_SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientModuleBootstrap.registerAll();
            modEventBus.addListener(WindomiumClient::onRegisterKeyMappings);
        }
        WindomiumAPI.registerModule(new ServerTickModule());

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onConfigReload);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        CompatibilityDetector.detectInstalledMods();
        ModCompatibilityManager.applyBuiltInProfiles();
        JvmGcAdvisor.analyseJvmEnvironment();
        ModuleManager.syncWithConfig();
        LOGGER.info("Windomium Core initialised successfully.");
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        ModCompatibilityManager.syncUserBlacklist();
        ModuleManager.syncWithConfig();
    }
}