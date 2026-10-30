package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;
import org.windomium.client.render.MesherThreadPool;
import org.windomium.core.config.WindomiumConfig;

public final class ChunkMesherModule implements IOptimisationModule {
    @Override
    public String getId() {
        return "chunk_mesher";
    }

    @Override
    public String getDisplayName() {
        return "Async Chunk Mesher";
    }

    @Override
    public OptimisationCategory getCategory() {
        return OptimisationCategory.CHUNK_RENDERING;
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
        int threads = WindomiumConfig.CLIENT_SPEC.isLoaded()
                ? WindomiumConfig.MESHER_THREAD_COUNT.get()
                : Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        MesherThreadPool.ensureActive(threads);
    }

    @Override
    public void onDisable() {
        MesherThreadPool.clearQueue();
    }
}