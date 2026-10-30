package org.windomium.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import org.windomium.client.culling.CullingController;
import org.windomium.client.culling.OcclusionCuller;
import org.windomium.client.gui.WindomiumConfigScreen;
import org.windomium.client.profiler.ClientProfilerManager;
import org.windomium.core.compat.CompatibilityDetector;
import org.windomium.core.compat.ModCompatibilityManager;
import org.windomium.core.config.CullingMode;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.memory.BiomeColourCache;
import org.windomium.core.memory.JvmGcAdvisor;
import org.windomium.core.memory.MemoryOptimiserController;
import org.windomium.core.module.ModuleContainer;
import org.windomium.core.module.ModuleManager;

public final class WindomiumClientCommands {
    private WindomiumClientCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("windomium")
                        .then(Commands.literal("config")
                                .executes(context -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    mc.tell(() -> mc.setScreen(new WindomiumConfigScreen(mc.screen)));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("culling")
                                .then(Commands.literal("frustum")
                                        .executes(context -> applyCullingMode(context.getSource(), CullingMode.FRUSTUM))
                                )
                                .then(Commands.literal("occlusion")
                                        .executes(context -> applyCullingMode(context.getSource(), CullingMode.OCCLUSION))
                                )
                                .then(Commands.literal("combined")
                                        .executes(context -> applyCullingMode(context.getSource(), CullingMode.COMBINED))
                                )
                        )
                        .then(Commands.literal("overlay")
                                .executes(context -> {
                                    boolean nextState = !WindomiumConfig.SHOW_PROFILER_OVERLAY.get();
                                    WindomiumConfig.SHOW_PROFILER_OVERLAY.set(nextState);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("[Windomium] Profiler overlay " + (nextState ? "enabled." : "disabled.")),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("profile")
                                .executes(context -> {
                                    ClientProfilerManager.startProfilingSession(30);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("[Windomium] Profiling started for 30 seconds (default)..."),
                                            false
                                    );
                                    return 1;
                                })
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 120))
                                        .executes(context -> {
                                            int seconds = IntegerArgumentType.getInteger(context, "seconds");
                                            ClientProfilerManager.startProfilingSession(seconds);
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal("[Windomium] Profiling started for " + seconds + " seconds..."),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("modules")
                                .executes(context -> {
                                    for (ModuleContainer container : ModuleManager.getAllModules()) {
                                        String status = container.isFaulted() ? "FAULTED" : (container.isEnabled() ? "ENABLED" : "DISABLED");
                                        String line = String.format(
                                                "- %s (%s) [%s]: %s",
                                                container.getModule().getDisplayName(),
                                                container.getModule().getId(),
                                                container.getModule().getRiskLevel().getDisplayName(),
                                                status
                                        );
                                        context.getSource().sendSuccess(() -> Component.literal(line), false);
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("compat")
                                .executes(context -> {
                                    var profiles = ModCompatibilityManager.getActiveCompatProfiles();
                                    String list = profiles.isEmpty() ? "None (Vanilla / Standalone)" : String.join(", ", profiles);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("[Windomium Compat] Active Mod Profiles: " + list),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("jvm")
                                .executes(context -> {
                                    String header = String.format(
                                            "[Windomium JVM] Active GC: %s | Max Heap: %d MB",
                                            JvmGcAdvisor.getDetectedGcName(),
                                            JvmGcAdvisor.getMaxHeapMb()
                                    );
                                    String advice = "[Windomium Advice] " + JvmGcAdvisor.getRecommendationSummary();
                                    String flags = "[Recommended Flags] " + JvmGcAdvisor.getRecommendedFlagsFor4GbTo6Gb();
                                    context.getSource().sendSuccess(() -> Component.literal(header), false);
                                    context.getSource().sendSuccess(() -> Component.literal(advice), false);
                                    context.getSource().sendSuccess(() -> Component.literal(flags), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("memory")
                                .executes(context -> {
                                    String stats = String.format(
                                            "[Windomium Memory] Heap: %d / %d MB | Alloc Rate: %.1f MB/s | Biome Cache Hit Ratio: %.1f%%",
                                            MemoryOptimiserController.getUsedHeapMb(),
                                            MemoryOptimiserController.getMaxHeapMb(),
                                            MemoryOptimiserController.getAllocationRateMbPerSec(),
                                            BiomeColourCache.getHitRatioPercentage()
                                    );
                                    MemoryOptimiserController.trimCaches();
                                    context.getSource().sendSuccess(() -> Component.literal(stats), false);
                                    context.getSource().sendSuccess(() -> Component.literal("[Windomium Memory] Thread-local pools and biome caches trimmed."), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("diag")
                                .executes(context -> {
                                    int loadedMods = ModList.get().size();
                                    String summary = String.format(
                                            "[Windomium Diagnostics] Preset: %s | Culling: %s | Active Modules: %d | Loaded Mods: %d | Sodium Companion: %s | Shader Guard: %s",
                                            WindomiumConfig.ACTIVE_PRESET.get().name(),
                                            CullingController.getActiveCullingMode().name(),
                                            ModuleManager.getActiveModuleCount(),
                                            loadedMods,
                                            CompatibilityDetector.isSodiumOrForkLoaded() ? "YES" : "NO",
                                            CompatibilityDetector.isIrisOrForkLoaded() ? "YES" : "NO"
                                    );
                                    context.getSource().sendSuccess(() -> Component.literal(summary), false);
                                    return 1;
                                })
                        )
        );
    }

    private static int applyCullingMode(CommandSourceStack source, CullingMode mode) {
        WindomiumConfig.CULLING_MODE.set(mode);
        WindomiumConfig.CLIENT_SPEC.save();
        OcclusionCuller.clearCache();
        source.sendSuccess(() -> Component.literal("[Windomium] Culling mode set to: " + mode.name()), false);
        return 1;
    }
}