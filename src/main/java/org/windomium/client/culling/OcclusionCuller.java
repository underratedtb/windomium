package org.windomium.client.culling;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.windomium.core.memory.ObjectPoolManager;

public final class OcclusionCuller {
    private static final byte UNKNOWN = 0;
    private static final byte VISIBLE = 1;
    private static final byte OCCLUDED = 2;
    private static final int MAX_CACHE_SIZE = 32768;
    private static final int MAX_RAYCASTS_PER_FRAME = 192;

    private static final Long2ByteOpenHashMap VISIBILITY_CACHE = new Long2ByteOpenHashMap(MAX_CACHE_SIZE);
    private static int camRegionX = Integer.MIN_VALUE;
    private static int camRegionY = Integer.MIN_VALUE;
    private static int camRegionZ = Integer.MIN_VALUE;
    private static int raycastsThisFrame = 0;
    private static int framesSinceCachePurge = 0;

    static {
        VISIBILITY_CACHE.defaultReturnValue(UNKNOWN);
    }

    private OcclusionCuller() {
    }

    public static void onFrameBegin(double camX, double camY, double camZ) {
        raycastsThisFrame = 0;
        framesSinceCachePurge++;

        int rx = ((int) Math.floor(camX)) >> 2;
        int ry = ((int) Math.floor(camY)) >> 2;
        int rz = ((int) Math.floor(camZ)) >> 2;

        if (rx != camRegionX || ry != camRegionY || rz != camRegionZ || framesSinceCachePurge >= 12) {
            camRegionX = rx;
            camRegionY = ry;
            camRegionZ = rz;
            framesSinceCachePurge = 0;
            VISIBILITY_CACHE.clear();
        }
    }

    public static boolean isBoxOccluded(AABB box, double camX, double camY, double camZ, boolean isSection) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || (mc.player != null && mc.player.isSpectator())) {
            return false;
        }

        double centreX = (box.minX + box.maxX) * 0.5;
        double centreY = (box.minY + box.maxY) * 0.5;
        double centreZ = (box.minZ + box.maxZ) * 0.5;

        double dx = centreX - camX;
        double dy = centreY - camY;
        double dz = centreZ - camZ;
        double totalDistSq = (dx * dx) + (dy * dy) + (dz * dz);

        if (totalDistSq < 256.0) {
            return false;
        }

        int gridX = isSection ? (((int) Math.floor(centreX)) >> 4) : ((int) Math.floor(centreX));
        int gridY = isSection ? (((int) Math.floor(centreY)) >> 4) : ((int) Math.floor(centreY));
        int gridZ = isSection ? (((int) Math.floor(centreZ)) >> 4) : ((int) Math.floor(centreZ));

        long cacheKey = packKey(camRegionX, camRegionY, camRegionZ, gridX, gridY, gridZ, isSection);

        byte cachedState = VISIBILITY_CACHE.get(cacheKey);
        if (cachedState != UNKNOWN) {
            return cachedState == OCCLUDED;
        }

        if (raycastsThisFrame >= MAX_RAYCASTS_PER_FRAME) {
            return false;
        }
        raycastsThisFrame++;

        boolean occluded;
        if (isSection) {
            occluded = isRayBlocked(level, camX, camY, camZ, centreX, centreY, centreZ, totalDistSq)
                    && isRayBlocked(level, camX, camY, camZ, centreX, box.maxY - 1.0, centreZ, totalDistSq)
                    && isRayBlocked(level, camX, camY, camZ, box.minX + 2.0, centreY, box.minZ + 2.0, totalDistSq)
                    && isRayBlocked(level, camX, camY, camZ, box.maxX - 2.0, centreY, box.maxZ - 2.0, totalDistSq);
        } else {
            occluded = isRayBlocked(level, camX, camY, camZ, centreX, centreY, centreZ, totalDistSq)
                    && isRayBlocked(level, camX, camY, camZ, centreX, box.maxY - 0.15, centreZ, totalDistSq)
                    && isRayBlocked(level, camX, camY, camZ, box.minX + 0.1, centreY, box.minZ + 0.1, totalDistSq)
                    && isRayBlocked(level, camX, camY, camZ, box.maxX - 0.1, centreY, box.maxZ - 0.1, totalDistSq)
                    && isRayBlocked(level, camX, camY, camZ, box.minX + 0.1, box.maxY - 0.15, box.maxZ - 0.1, totalDistSq);
        }

        if (VISIBILITY_CACHE.size() < MAX_CACHE_SIZE) {
            VISIBILITY_CACHE.put(cacheKey, occluded ? OCCLUDED : VISIBLE);
        }
        return occluded;
    }

    private static boolean isRayBlocked(
            ClientLevel level,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double distSq
    ) {
        double distance = Math.sqrt(distSq);
        int steps = Math.min(12, Math.max(5, (int) (distance * 0.22)));

        double stepX = (endX - startX) / steps;
        double stepY = (endY - startY) / steps;
        double stepZ = (endZ - startZ) / steps;

        double currentX = startX + stepX * 1.5;
        double currentY = startY + stepY * 1.5;
        double currentZ = startZ + stepZ * 1.5;

        BlockPos.MutableBlockPos mutablePos = ObjectPoolManager.borrowBlockPos(0, 0, 0);
        int opaqueHits = 0;

        try {
            for (int i = 2; i < steps - 1; i++) {
                mutablePos.set((int) Math.floor(currentX), (int) Math.floor(currentY), (int) Math.floor(currentZ));
                BlockState state = level.getBlockState(mutablePos);
                if (state.isSolidRender(level, mutablePos)) {
                    opaqueHits++;
                    if (opaqueHits >= 2) {
                        return true;
                    }
                }
                currentX += stepX;
                currentY += stepY;
                currentZ += stepZ;
            }
        } finally {
            ObjectPoolManager.releaseBlockPos(mutablePos);
        }

        return false;
    }

    private static long packKey(int rx, int ry, int rz, int gx, int gy, int gz, boolean isSection) {
        long camHash = ((long) (rx & 0x3FF) << 20) | ((long) (ry & 0x3FF) << 10) | (rz & 0x3FF);
        long posHash = ((long) (gx & 0xFFF) << 20) | ((long) (gy & 0xFF) << 12) | (gz & 0xFFF);
        long typeBit = isSection ? (1L << 62) : 0L;
        return typeBit | ((camHash << 30) ^ posHash);
    }

    public static void clearCache() {
        VISIBILITY_CACHE.clear();
    }
}