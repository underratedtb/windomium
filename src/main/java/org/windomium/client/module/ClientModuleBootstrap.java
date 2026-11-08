package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;
import org.windomium.api.WindomiumAPI;
import org.windomium.client.culling.OcclusionCuller;
import org.windomium.core.memory.MemoryOptimiserController;

public final class ClientModuleBootstrap {
    private static boolean registered = false;

    private ClientModuleBootstrap() {
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        WindomiumAPI.registerModule(new SimpleClientModule("frustum_culling", "Section & Entity Frustum Culling", OptimisationCategory.CHUNK_RENDERING, () -> {}, () -> {}));
        WindomiumAPI.registerModule(new SimpleClientModule("occlusion_culling", "Ray-Marched Occlusion Culling", OptimisationCategory.CHUNK_RENDERING, OcclusionCuller::clearCache, OcclusionCuller::clearCache));
        WindomiumAPI.registerModule(new SimpleClientModule("entity_optimiser", "Entity Layer & Shadow Optimiser", OptimisationCategory.ENTITY, () -> {}, () -> {}));
        WindomiumAPI.registerModule(new SimpleClientModule("block_entity_optimiser", "Block Entity Culling & Rate Limiter", OptimisationCategory.BLOCK_ENTITY, () -> {}, () -> {}));
        WindomiumAPI.registerModule(new SimpleClientModule("particle_optimiser", "Particle Distance & Budget Limiter", OptimisationCategory.PARTICLE_AND_UI, () -> {}, () -> {}));
        WindomiumAPI.registerModule(new SimpleClientModule("memory_optimiser", "Fastutil Cache & Memory Optimiser", OptimisationCategory.MEMORY_AND_LOADING, () -> {}, MemoryOptimiserController::trimCaches));
    }

    private record SimpleClientModule(
            String id,
            String displayName,
            OptimisationCategory category,
            Runnable enableAction,
            Runnable disableAction
    ) implements IOptimisationModule {
        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public OptimisationCategory getCategory() {
            return this.category;
        }

        @Override
        public RiskLevel getRiskLevel() {
            return RiskLevel.SAFE;
        }

        @Override
        public boolean guaranteesVisualParity() {
            return true;
        }

        @Override
        public boolean isClientOnly() {
            return true;
        }

        @Override
        public void onInitialise() {
        }

        @Override
        public void onEnable() {
            this.enableAction.run();
        }

        @Override
        public void onDisable() {
            this.disableAction.run();
        }
    }
}