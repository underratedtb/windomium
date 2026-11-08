package org.windomium.client.culling;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class OcclusionCuller {
    private static final byte UNKNOWN = 0;
    private static final byte VISIBLE = 1;
    private static final byte OCCLUDED = 2;
    private static final int MAX_CACHE_SIZE = 32768;
    private static final int MAX_RAYCASTS_PER_FRAME = 192;

    private static final Long2ByteOpenHashMap VISIBILITY_CACHE = new Long2ByteOpenHashMap(MAX_CACHE_SIZE);
    private static final BlockPos.MutableBlockPos RAY_POS = new BlockPos.MutableBlockPos();

    private static int camBlockX = Integer.MIN_VALUE;
    private static int camBlockY = Integer.MIN_VALUE;
    private static int camBlockZ = Integer.MIN_VALUE;
    private static int raycastsThisFrame = 0;
    private static int frameAge = 0;

    static {
        VISIBILITY_CACHE.defaultReturnValue(UNKNOWN);
    }

    private OcclusionCuller() {
    }

    public static void onFrameBegin(double camX, double camY, double camZ) {
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }

        raycastsThisFrame = 0;
        frameAge++;

        int bx = (int) Math.floor(camX);
        int by = (int) Math.floor(camY);
        int bz = (int) Math.floor(camZ);

        if (bx != camBlockX || by != camBlockY || bz != camBlockZ || frameAge >= 12) {
            camBlockX = bx;
            camBlockY = by;
            camBlockZ = bz;
            frameAge = 0;
            VISIBILITY_CACHE.clear();
        }
    }

    public static boolean isBoxOccluded(AABB box, double camX, double camY, double camZ) {
        return isBoxOccluded(box, camX, camY, camZ, false);
    }

    public static boolean isBoxOccluded(AABB box, double camX, double camY, double camZ, boolean isSection) {
        if (!RenderSystem.isOnRenderThread()) {
            return false;
        }

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

        int gridShift = isSection ? 4 : 1;
        int gx = ((int) Math.floor(centreX)) >> gridShift;
        int gy = ((int) Math.floor(centreY)) >> gridShift;
        int gz = ((int) Math.floor(centreZ)) >> gridShift;
        long cacheKey = packKey(gx, gy, gz, isSection);

        byte cachedState = VISIBILITY_CACHE.get(cacheKey);
        if (cachedState != UNKNOWN) {
            return cachedState == OCCLUDED;
        }

        if (raycastsThisFrame >= MAX_RAYCASTS_PER_FRAME) {
            return false;
        }
        raycastsThisFrame++;

        boolean occluded = evaluateOcclusion(level, box, camX, camY, camZ, centreX, centreY, centreZ, totalDistSq, isSection);

        if (VISIBILITY_CACHE.size() < MAX_CACHE_SIZE) {
            VISIBILITY_CACHE.put(cacheKey, occluded ? OCCLUDED : VISIBLE);
        }
        return occluded;
    }

    private static boolean evaluateOcclusion(
            ClientLevel level,
            AABB box,
            double camX,
            double camY,
            double camZ,
            double centreX,
            double centreY,
            double centreZ,
            double distSq,
            boolean isSection
    ) {
        if (!isRayBlocked(level, camX, camY, camZ, centreX, centreY, centreZ, distSq)) {
            return false;
        }
        if (!isRayBlocked(level, camX, camY, camZ, centreX, box.maxY - 0.5, centreZ, distSq)) {
            return false;
        }
        if (!isSection) {
            return true;
        }
        return isRayBlocked(level, camX, camY, camZ, box.minX + 1.5, centreY, box.minZ + 1.5, distSq)
                && isRayBlocked(level, camX, camY, camZ, box.maxX - 1.5, centreY, box.maxZ - 1.5, distSq);
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
        int steps = Math.min(14, Math.max(5, (int) (distance * 0.22)));

        double stepX = (endX - startX) / steps;
        double stepY = (endY - startY) / steps;
        double stepZ = (endZ - startZ) / steps;

        double currentX = startX + stepX * 1.5;
        double currentY = startY + stepY * 1.5;
        double currentZ = startZ + stepZ * 1.5;

        int opaqueHits = 0;
        for (int i = 2; i < steps - 1; i++) {
            RAY_POS.set((int) Math.floor(currentX), (int) Math.floor(currentY), (int) Math.floor(currentZ));
            BlockState state = level.getBlockState(RAY_POS);
            if (state.isSolidRender(level, RAY_POS)) {
                opaqueHits++;
                if (opaqueHits >= 2) {
                    return true;
                }
            }
            currentX += stepX;
            currentY += stepY;
            currentZ += stepZ;
        }
        return false;
    }

    private static long packKey(int x, int y, int z, boolean isSection) {
        long base = (((long) (x & 0x1FFFFF)) << 42) | (((long) (y & 0xFFFFF)) << 22) | ((long) (z & 0x3FFFFF));
        return isSection ? (base & 0x7FFFFFFFFFFFFFFFL) : (base | 0x8000000000000000L);
    }

    public static void clearCache() {
        if (RenderSystem.isOnRenderThread()) {
            VISIBILITY_CACHE.clear();
        }
    }
}