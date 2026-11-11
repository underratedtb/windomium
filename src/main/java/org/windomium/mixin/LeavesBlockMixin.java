package org.windomium.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.windomium.core.config.Preset;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block {
    protected LeavesBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        if (ModuleManager.isModuleActive("leaf_culling")
                && (!WindomiumConfig.CLIENT_SPEC.isLoaded() || WindomiumConfig.ENABLE_LEAF_CULLING.get())) {
            try {
                if (adjacentBlockState.getBlock() instanceof LeavesBlock) {
                    Preset preset = WindomiumConfig.CLIENT_SPEC.isLoaded()
                            ? WindomiumConfig.ACTIVE_PRESET.get()
                            : Preset.BALANCED;
                    if (preset == Preset.EXTREME || adjacentBlockState.getBlock() == state.getBlock()) {
                        return true;
                    }
                }
            } catch (Throwable throwable) {
                ModuleManager.handleModuleException("leaf_culling", throwable);
            }
        }
        return super.skipRendering(state, adjacentBlockState, side);
    }
}