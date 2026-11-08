package org.windomium.server;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.windomium.core.config.WindomiumConfig;
import org.windomium.core.module.ModuleManager;

public final class ServerTickOptimiser {
    private ServerTickOptimiser() {
    }

    public static boolean shouldSkipEntityTick(Level level, Entity entity) {
        if (level == null || entity == null || level.isClientSide()) {
            return false;
        }

        if (!WindomiumConfig.SERVER_SPEC.isLoaded() || !WindomiumConfig.ENABLE_SERVER_OPTIMISATIONS.get()) {
            return false;
        }

        if (!ModuleManager.isModuleActive("server_tick_optimiser")) {
            return false;
        }

        if (!(entity instanceof Mob mob) || mob.noCulling) {
            return false;
        }

        if (mob.isPersistenceRequired() || mob.hasCustomName() || mob.isVehicle() || mob.isPassenger() || mob.isInWaterOrBubble() || mob.hurtTime > 0) {
            return false;
        }

        long gameTime = level.getGameTime();
        long entityId = mob.getId();

        Player nearestPlayer = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 128.0, false);
        if (nearestPlayer == null) {
            return ((gameTime + entityId) & 3L) != 0L;
        }

        double distSq = mob.distanceToSqr(nearestPlayer);
        if (distSq > 64.0 * 64.0) {
            return ((gameTime + entityId) & 3L) != 0L;
        }
        if (distSq > 36.0 * 36.0) {
            return ((gameTime + entityId) & 1L) != 0L;
        }

        return false;
    }
}