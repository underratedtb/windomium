package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;
import org.windomium.api.WindomiumAPI;

public final class ClientModuleBootstrap {
    private static boolean registered = false;

    private ClientModuleBootstrap() {
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        registered = true;
        WindomiumAPI.registerModule(new ChunkMesherModule());
        WindomiumAPI.registerModule(new FrustumCullingModule());
        WindomiumAPI.registerModule(new OcclusionCullingModule());
        WindomiumAPI.registerModule(new BuiltInEntityModule());
        WindomiumAPI.registerModule(new BuiltInBlockEntityModule());
        WindomiumAPI.registerModule(new ParticleOptimisationModule());
        WindomiumAPI.registerModule(new MemoryOptimisationModule());
    }

    private static final class BuiltInEntityModule implements IOptimisationModule {
        @Override
        public String getId() {
            return "entity_optimiser";
        }

        @Override
        public String getDisplayName() {
            return "Entity Layer & Shadow Optimiser";
        }

        @Override
        public OptimisationCategory getCategory() {
            return OptimisationCategory.ENTITY;
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
        }

        @Override
        public void onDisable() {
        }
    }

    private static final class BuiltInBlockEntityModule implements IOptimisationModule {
        @Override
        public String getId() {
            return "block_entity_optimiser";
        }

        @Override
        public String getDisplayName() {
            return "Block Entity Culling & Rate Limiter";
        }

        @Override
        public OptimisationCategory getCategory() {
            return OptimisationCategory.BLOCK_ENTITY;
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
        }

        @Override
        public void onDisable() {
        }
    }
}