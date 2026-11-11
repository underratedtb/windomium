package org.windomium.core.config;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class WindomiumConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.EnumValue<Preset> ACTIVE_PRESET;
    public static final ModConfigSpec.BooleanValue SHOW_PROFILER_OVERLAY;
    public static final ModConfigSpec.IntValue TARGET_FPS;
    public static final ModConfigSpec.IntValue TARGET_ONE_PERCENT_LOW;
    public static final ModConfigSpec.BooleanValue ENABLE_RUNTIME_GOVERNOR;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_CLASSES;

    public static final ModConfigSpec.EnumValue<CullingMode> CULLING_MODE;
    public static final ModConfigSpec.BooleanValue ENABLE_FRUSTUM_CULLING;
    public static final ModConfigSpec.BooleanValue ENABLE_OCCLUSION_CULLING;
    public static final ModConfigSpec.BooleanValue ENABLE_LEAF_CULLING;
    public static final ModConfigSpec.BooleanValue ENABLE_UNFOCUSED_FPS;
    public static final ModConfigSpec.IntValue UNFOCUSED_FPS_LIMIT;

    public static final ModConfigSpec.BooleanValue ENABLE_ENTITY_OPTIMISER;
    public static final ModConfigSpec.BooleanValue ENABLE_BLOCK_ENTITY_OPTIMISER;
    public static final ModConfigSpec.DoubleValue BLOCK_ENTITY_FRAME_BUDGET_MS;

    public static final ModConfigSpec.BooleanValue ENABLE_MEMORY_OPTIMISER;
    public static final ModConfigSpec.BooleanValue ENABLE_DIAGNOSTIC_LOGGING;
    public static final ModConfigSpec.BooleanValue ENABLE_SERVER_OPTIMISATIONS;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();

        clientBuilder.push("general");
        ACTIVE_PRESET = clientBuilder.defineEnum("activePreset", Preset.BALANCED);
        SHOW_PROFILER_OVERLAY = clientBuilder.define("showProfilerOverlay", false);
        TARGET_FPS = clientBuilder.defineInRange("targetFps", 144, 30, 1000);
        TARGET_ONE_PERCENT_LOW = clientBuilder.defineInRange("targetOnePercentLow", 90, 20, 500);
        ENABLE_RUNTIME_GOVERNOR = clientBuilder.define("enableRuntimeGovernor", true);
        BLACKLISTED_CLASSES = clientBuilder.defineListAllowEmpty(
                "blacklistedClasses",
                List::of,
                () -> "",
                obj -> obj instanceof String
        );
        clientBuilder.pop();

        clientBuilder.push("rendering");
        CULLING_MODE = clientBuilder.defineEnum("cullingMode", CullingMode.COMBINED);
        ENABLE_FRUSTUM_CULLING = clientBuilder.define("enableFrustumCulling", true);
        ENABLE_OCCLUSION_CULLING = clientBuilder.define("enableOcclusionCulling", true);
        ENABLE_LEAF_CULLING = clientBuilder.define("enableLeafCulling", true);
        ENABLE_UNFOCUSED_FPS = clientBuilder.define("enableUnfocusedFpsLimiter", true);
        UNFOCUSED_FPS_LIMIT = clientBuilder.defineInRange("unfocusedFpsLimit", 15, 5, 60);
        clientBuilder.pop();

        clientBuilder.push("entities");
        ENABLE_ENTITY_OPTIMISER = clientBuilder.define("enableEntityOptimiser", true);
        ENABLE_BLOCK_ENTITY_OPTIMISER = clientBuilder.define("enableBlockEntityOptimiser", true);
        BLOCK_ENTITY_FRAME_BUDGET_MS = clientBuilder.defineInRange("blockEntityFrameBudgetMs", 1.5, 0.2, 10.0);
        clientBuilder.pop();

        clientBuilder.push("memory");
        ENABLE_MEMORY_OPTIMISER = clientBuilder.define("enableMemoryOptimiser", true);
        clientBuilder.pop();

        CLIENT_SPEC = clientBuilder.build();

        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
        commonBuilder.push("diagnostics");
        ENABLE_DIAGNOSTIC_LOGGING = commonBuilder.define("enableDiagnosticLogging", true);
        commonBuilder.pop();
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();
        serverBuilder.push("server");
        ENABLE_SERVER_OPTIMISATIONS = serverBuilder
                .comment("Throttles AI ticks of mobs >36 blocks (1/2 rate) and >64 blocks (1/4 rate) from non-spectator players. Note: This reduces distant mob farm and villager breeding rates.")
                .define("enableServerOptimisations", false);
        serverBuilder.pop();
        SERVER_SPEC = serverBuilder.build();
    }

    private WindomiumConfig() {
    }
}