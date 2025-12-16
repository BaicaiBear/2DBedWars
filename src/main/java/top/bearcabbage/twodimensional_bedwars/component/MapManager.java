package top.bearcabbage.twodimensional_bedwars.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import top.bearcabbage.twodimensional_bedwars.config.GameConfig;

public class MapManager {
    
    private static final int BLOCKS_PER_TICK = 50000;
    
    private static final List<ActiveRestore> activeRestores = new ArrayList<>();
    private static boolean tickingRegistered = false;

    public static void startRestore(ServerWorld source, ServerWorld dest, List<RegionPair> regions, Runnable onComplete) {
        if (source == null || dest == null) return;
        
        System.out.println("Starting Async Map Restore...");
        ActiveRestore restore = new ActiveRestore(source, dest, regions, onComplete);
        activeRestores.add(restore);
        
        if (!tickingRegistered) {
            ServerTickEvents.END_SERVER_TICK.register(server -> tick());
            tickingRegistered = true;
        }
    }
    
    private static void tick() {
        Iterator<ActiveRestore> it = activeRestores.iterator();
        while (it.hasNext()) {
            ActiveRestore restore = it.next();
            if (restore.tick()) {
                restore.finish();
                it.remove();
            }
        }
    }
    
    public static class RegionPair {
        public GameConfig.MapRegion sourceRegion;
        public GameConfig.MapRegion destRegion;
        
        public RegionPair(GameConfig.MapRegion source, GameConfig.MapRegion dest) {
            this.sourceRegion = source;
            this.destRegion = dest;
        }
    }
    
    private static class ActiveRestore {
        private final ServerWorld source;
        private final ServerWorld dest;
        private final List<RegionPair> regions;
        private final Runnable onComplete;
        
        private int currentRegionIndex = 0;
        private int currentX, currentY, currentZ;
        private boolean regionStarted = false;
        
        // Bounds for current region
        private int minX, minY, minZ, maxX, maxY, maxZ;
        // Source offset
        private int offX, offY, offZ;

        public ActiveRestore(ServerWorld source, ServerWorld dest, List<RegionPair> regions, Runnable onComplete) {
            this.source = source;
            this.dest = dest;
            this.regions = regions;
            this.onComplete = onComplete;
        }
        
        // Returns true if complete
        public boolean tick() {
            if (currentRegionIndex >= regions.size()) return true;
            
            RegionPair region = regions.get(currentRegionIndex);
            
            if (!regionStarted) {
                // Initialize pointers
                minX = region.destRegion.getMinPt().x;
                minY = region.destRegion.getMinPt().y;
                minZ = region.destRegion.getMinPt().z;
                maxX = region.destRegion.getMaxPt().x;
                maxY = region.destRegion.getMaxPt().y;
                maxZ = region.destRegion.getMaxPt().z;
                
                // Blueprint offset
                offX = region.sourceRegion.getMinPt().x - minX;
                offY = region.sourceRegion.getMinPt().y - minY;
                offZ = region.sourceRegion.getMinPt().z - minZ;
                
                currentX = minX;
                currentY = minY;
                currentZ = minZ;
                
                // Force Load Chunks
                forceLoadChunks(dest, region.destRegion, true);
                forceLoadChunks(source, region.sourceRegion, true); 
                regionStarted = true;
                System.out.println("Restoring Region " + currentRegionIndex + " Bounds: " + minX + "," + minY + "," + minZ + " to " + maxX + "," + maxY + "," + maxZ);
            }
            
            int blocksProcessed = 0;
            
            long startTime = System.currentTimeMillis();
            
            while (blocksProcessed < BLOCKS_PER_TICK) {
                // Time Check: If we take too long (> 30ms), stop to allow server tick to finish
                if (blocksProcessed % 1000 == 0 && (System.currentTimeMillis() - startTime) > 40) {
                     break; 
                }

                // Copy block
                BlockPos destPos = new BlockPos(currentX, currentY, currentZ);
                BlockPos srcPos = new BlockPos(currentX + offX, currentY + offY, currentZ + offZ);
                
                BlockState srcState = source.getBlockState(srcPos);
                BlockState currentDestState = dest.getBlockState(destPos);
                
                // OPTIMIZATION: Only update if changed
                if (srcState != currentDestState) {
                    // Flag 18 = NO_NEIGHBOR_DROPS | SEND_TO_CLIENTS
                    // Flag 82 = NO_NEIGHBOR_DROPS | SEND_TO_CLIENTS | SKIP_DROPS | MOVED (approx) - Safer to stick to 18 or adds skip_light
                    // Using 18 is safe. The biggest save is the check above.
                    dest.setBlockState(destPos, srcState, 18);
                }
                
                blocksProcessed++;
                
                // Advance pointers
                currentX++;
                if (currentX > maxX) {
                    currentX = minX;
                    currentZ++;
                    if (currentZ > maxZ) {
                        currentZ = minZ;
                        currentY++;
                        if (currentY > maxY) {
                            // Region Complete
                            // forceLoadChunks(dest, region.destRegion, false); // Unload - KEEP LOADED to prevent crashes/race conditions
                            // forceLoadChunks(source, region.sourceRegion, false);
                            
                            currentRegionIndex++;
                            regionStarted = false;
                            return false; // Continue next tick
                        }
                    }
                }
            }
            
            return false;
        }
        
        public void finish() {
            System.out.println("Async Restore Complete.");
            if (onComplete != null) onComplete.run();
        }
        
        private void forceLoadChunks(ServerWorld world, GameConfig.MapRegion region, boolean load) {
           // Simple implementation: load chunks covering the region
           int minCx = region.getMinPt().x >> 4;
           int minCz = region.getMinPt().z >> 4;
           int maxCx = region.getMaxPt().x >> 4;
           int maxCz = region.getMaxPt().z >> 4;
           
           for (int cx = minCx; cx <= maxCx; cx++) {
               for (int cz = minCz; cz <= maxCz; cz++) {
                   world.setChunkForced(cx, cz, load);
               }
           }
        }
    }
}
