package top.bearcabbage.twodimensional_bedwars.mechanic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.world.ServerWorld;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;
import top.bearcabbage.twodimensional_bedwars.component.Arena;
import top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam;
import top.bearcabbage.twodimensional_bedwars.component.OreGenerator;

public class GamePlayingTask {
    private final Arena arena;
    private int bedsDestroyCountdown = 3600; // Example 1 hour? Spec says timer logic
    private int dragonSpawnCountdown = 4200;
    private int gameEndCountdown = 4800;

    public GamePlayingTask(Arena arena) {
        this.arena = arena;
    }

    private final Map<UUID, Integer> respawnTimers = new HashMap<>();
    private int tickCounter = 0;

    public void addRespawn(UUID uuid, int seconds) {
        respawnTimers.put(uuid, seconds);
    }

    public void run(ServerWorld world) {
        // Run every tick (20Hz)
        // Iterate Teams and Tick Generators
        for (ITeam team : arena.getTeams()) {
            if (team instanceof BedWarsTeam bwTeam) {
                for (OreGenerator generator : bwTeam.getLiveGenerators()) {
                    generator.tick(world);
                }
            }
        }
        
        // Tick Public Generators
        for (OreGenerator generator : arena.getPublicGenerators()) {
            generator.tick(world);
        }

        // Bed Integrity Check (Every 10 Ticks)
        if (tickCounter % 10 == 0) {
            for (ITeam team : arena.getTeams()) {
                if (team instanceof BedWarsTeam bwTeam) {
                    checkBedIntegrity(world, bwTeam, 1);
                    checkBedIntegrity(world, bwTeam, 2);
                }
            }
        }

        // Run once per second logic (20 ticks)
        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;

        // Decrement counters
        bedsDestroyCountdown--;
        dragonSpawnCountdown--;
        gameEndCountdown--;
        
        // Handle Respawn Queue
        Iterator<Map.Entry<UUID, Integer>> iterator = respawnTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int timeLeft = entry.getValue() - 1;
            if (timeLeft <= 0) {
                // Trigger respawn
                arena.respawnPlayer(entry.getKey(), world);
                iterator.remove();
            } else {
                entry.setValue(timeLeft);
                // Optionally send title/message to player about time left
            }
        }

        // Check NextEvent timer logic
        if (bedsDestroyCountdown == 0) {
            // Trigger bed destroy
            for (ITeam team : arena.getTeams()) {
                team.setBedDestroyed(true);
            }
            // Broadcast message logic would go here
        }
    }

    private void checkBedIntegrity(ServerWorld world, BedWarsTeam team, int arenaId) {
        if (team.isBedDestroyed(arenaId)) return; // Already destroyed

        net.minecraft.util.math.BlockPos bedPos = team.getBedLocation(arenaId);
        if (bedPos == null) return;

        // Check if block is a Bed
        // Note: BedBlock usually has 2 parts. We stored the 'Foot' pos (spawn).
        // If the block at spawn is NOT a bed, it's gone.
        // Even if only the head is broken, the foot should technically also break or update?
        // In Minecraft, breaking one half breaks the other.
        // So checking the stored pos is sufficient.
        
        // We use world.getBlockState(bedPos).getBlock() instanceof net.minecraft.block.BedBlock
        if (!(world.getBlockState(bedPos).getBlock() instanceof net.minecraft.block.BedBlock)) {
            // Bed is gone!
            team.setBedDestroyed(arenaId, true);
            
            // Broadcast
            world.getServer().getPlayerManager().getPlayerList().forEach(p -> 
                p.sendMessage(net.minecraft.text.Text.literal("§l§c" + team.getName() + " Bed (Arena " + arenaId + ") was destroyed (Explosion/Check)!"))
            );
            
            // Optional: Play Sound
            // world.playSound(null, bedPos, net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN, net.minecraft.sound.SoundCategory.BLOCKS, 1f, 1f);
        }
    }
}
