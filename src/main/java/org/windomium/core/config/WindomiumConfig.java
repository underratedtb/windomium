package org.windomium.core.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public final class WindomiumConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec SERVER_SPEC;

    public static final Client CLIENT;
    public static final Common COMMON;
    public static final Server SERVER;

    public static ModConfigSpec.EnumValue<Preset> ACTIVE_PRESET;
    public static ModConfigSpec.EnumValue<CullingMode> CULLING_MODE;
    public static ModConfigSpec.BooleanValue ENABLE_FRUSTUM_CULLING;
    public static ModConfigSpec.BooleanValue ENABLE_OCCLUSION_CULLING;
    public static ModConfigSpec.BooleanValue ENABLE_ASYNC_MESHING;
    public static ModConfigSpec.BooleanValue ENABLE_ENTITY_OPTIMISER;
    public static ModConfigSpec.BooleanValue ENABLE_BLOCK_ENTITY_OPTIMISER;
    public static ModConfigSpec.BooleanValue ENABLE_MEMORY_OPTIMISER;
    public static ModConfigSpec.BooleanValue ENABLE_RUNTIME_GOVERNOR;
    public static ModConfigSpec.BooleanValue SHOW_PROFILER_OVERLAY;
    public static ModConfigSpec.IntValue TARGET_FPS;
    public static ModConfigSpec.IntValue TARGET_ONE_PERCENT_LOW;
    public static ModConfigSpec.IntValue BLOCK_ENTITY_FRAME_BUDGET_MICROS;
    public static ModConfigSpec.IntValue BIOME_COLOUR_CACHE_SIZE;
    public static ModConfigSpec.IntValue MESHER_THREAD_COUNT;
    public static ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_CLASSES;

    public static ModConfigSpec.BooleanValue ENABLE_SERVER_OPTIMISATIONS;

    static {
        Pair<Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();

        Pair<Common, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();

        Pair<Server, ModConfigSpec> serverPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = serverPair.getLeft();
        SERVER_SPEC = serverPair.getRight();
    }

    private WindomiumConfig() {
    }

    public static final class Client {
        public Client(ModConfigSpec.Builder builder) {
            builder.push("general");

            BLACKLISTED_CLASSES = builder
                    .comment("List of fully qualified class names to bypass/blacklist from culling and optimisation")
                    .defineList("blacklistedClasses", List.of(), () -> "", item -> item instanceof String);

            MESHER_THREAD_COUNT = builder
                    .comment("Number of background threads allocated for asynchronous chunk meshing")
                    .defineInRange("mesherThreadCount", 4, 1, 16);

            ACTIVE_PRESET = builder
                    .comment("Active performance preset profile")
                    .defineEnum("activePreset", Preset.BALANCED);

            CULLING_MODE = builder
                    .comment("Active culling algorithm mode")
                    .defineEnum("cullingMode", CullingMode.COMBINED);

            ENABLE_FRUSTUM_CULLING = builder
                    .comment("Enable Frustum Culling for chunk sections and entities")
                    .define("enableFrustumCulling", true);

            ENABLE_OCCLUSION_CULLING = builder
                    .comment("Enable Ray-Marched Occlusion Culling")
                    .define("enableOcclusionCulling", true);

            ENABLE_ASYNC_MESHING = builder
                    .comment("Enable Asynchronous Multi-Threaded Chunk Meshing Pool")
                    .define("enableAsyncMeshing", true);

            ENABLE_ENTITY_OPTIMISER = builder
                    .comment("Enable Entity shadow, layer and name-tag distance culling")
                    .define("enableEntityOptimiser", true);

            ENABLE_BLOCK_ENTITY_OPTIMISER = builder
                    .comment("Enable Block Entity distance and frame-budget throttling")
                    .define("enableBlockEntityOptimiser", true);

            ENABLE_MEMORY_OPTIMISER = builder
                    .comment("Enable Fastutil memory caching and object pooling")
                    .define("enableMemoryOptimiser", true);

            ENABLE_RUNTIME_GOVERNOR = builder
                    .comment("Enable Adaptive Runtime Governor (1% low FPS stabiliser)")
                    .define("enableRuntimeGovernor", true);

            SHOW_PROFILER_OVERLAY = builder
                    .comment("Show real-time profiler overlay on screen")
                    .define("showProfilerOverlay", true);

            TARGET_FPS = builder
                    .comment("Target framerate for Runtime Governor")
                    .defineInRange("targetFps", 144, 30, 1000);

            TARGET_ONE_PERCENT_LOW = builder
                    .comment("Target 1% Low framerate for Runtime Governor")
                    .defineInRange("targetOnePercentLow", 90, 20, 500);

            BLOCK_ENTITY_FRAME_BUDGET_MICROS = builder
                    .comment("Maximum nanosecond budget per frame for Block Entities in microseconds")
                    .defineInRange("blockEntityFrameBudgetMicros", 1500, 200, 10000);

            BIOME_COLOUR_CACHE_SIZE = builder
                    .comment("Maximum entries per thread for Biome Colour Cache")
                    .defineInRange("biomeColourCacheSize", 2048, 256, 16384);

            builder.pop();
        }
    }

    public static final class Common {
        public Common(ModConfigSpec.Builder builder) {
            builder.push("common");
            builder.pop();
        }
    }

    public static final class Server {
        public Server(ModConfigSpec.Builder builder) {
            builder.push("server");

            ENABLE_SERVER_OPTIMISATIONS = builder
                    .comment("Enable server-side distant mob tick rate throttling")
                    .define("enableServerOptimisations", false);

            builder.pop();
        }
    }
}