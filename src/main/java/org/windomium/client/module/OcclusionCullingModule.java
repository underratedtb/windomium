package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;
import org.windomium.client.culling.OcclusionCuller;

public final class OcclusionCullingModule implements IOptimisationModule {
    @Override
    public String getId() {
        return "occlusion_culling";
    }

    @Override
    public String getDisplayName() {
        return "Ray-Marched Occlusion Culling";
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
        OcclusionCuller.clearCache();
    }

    @Override
    public void onDisable() {
        OcclusionCuller.clearCache();
    }
}