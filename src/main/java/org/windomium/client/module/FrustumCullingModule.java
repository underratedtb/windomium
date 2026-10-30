package org.windomium.client.module;

import org.windomium.api.IOptimisationModule;
import org.windomium.api.OptimisationCategory;
import org.windomium.api.RiskLevel;

public final class FrustumCullingModule implements IOptimisationModule {
    @Override
    public String getId() {
        return "frustum_culling";
    }

    @Override
    public String getDisplayName() {
        return "Section & Entity Frustum Culling";
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
    }

    @Override
    public void onDisable() {
    }
}