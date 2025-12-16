package top.bearcabbage.twodimensional_bedwars.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.BlockPos;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;

public class BedWarsTeam implements ITeam {
    private final String name;
    private final int color;
    private final Map<Integer, BlockPos> spawnPoints; // Key: Arena ID (1 or 2)
    private final Map<Integer, BlockPos> bedLocations;
    private final Map<Integer, Boolean> bedStates;    // Key: Arena ID, Value: isDestroyed
    
    private final List<UUID> members;
    private final List<BlockPos> generators;
    private final List<OreGenerator> liveGenerators; // Active tickable generators
    
    private final List<StatusEffectInstance> teamEffects;

    public BedWarsTeam(String name, int color) {
        this.name = name;
        this.color = color;
        this.spawnPoints = new HashMap<>();
        this.bedStates = new HashMap<>();
        this.bedLocations = new HashMap<>();
        
        this.members = new ArrayList<>();
        this.generators = new ArrayList<>();
        this.liveGenerators = new ArrayList<>();
        this.teamEffects = new ArrayList<>();
    }
    
    public void setSpawnPoint(int arenaId, BlockPos pos) {
        spawnPoints.put(arenaId, pos);
        // Initialize bed state as alive (false) for this arena
        bedStates.put(arenaId, false);
    }
    
    public BlockPos getSpawnPoint(int arenaId) {
        return spawnPoints.get(arenaId);
    }

    @Override
    public boolean isBedDestroyed() {
        // Legacy/Generic check: true if ALL beds are destroyed?
        // Or true if ANY? The prompt implies specific dependencies.
        // Let's return true if BOTH are destroyed for general elimination,
        // but specific logic should check specific arena beds.
        for (Boolean destroyed : bedStates.values()) {
            if (!destroyed) return false; // At least one bed is alive
        }
        return true; // All beds destroyed
    }
    
    public boolean isBedDestroyed(int arenaId) {
        return bedStates.getOrDefault(arenaId, true); // Default to destroyed if not found? Or false? Safety first.
    }

    @Override
    public void setBedDestroyed(boolean destroyed) {
        // Deprecated usage, sets all for now
        for (Integer id : bedStates.keySet()) {
            bedStates.put(id, destroyed);
        }
    }
    
    public void setBedDestroyed(int arenaId, boolean destroyed) {
        bedStates.put(arenaId, destroyed);
    }

    @Override
    public List<UUID> getMembers() {
        return members;
    }

    @Override
    public List<BlockPos> getGenerators() {
        return generators;
    }
    
    public List<OreGenerator> getLiveGenerators() {
        return liveGenerators;
    }

    @Override
    public void spawnNPCs() {
        // Logic to spawn shopkeepers would go here
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getColor() {
        return color;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }
    
    public void addGenerator(BlockPos pos) {
        generators.add(pos);
    }
    
    public void addLiveGenerator(OreGenerator generator) {
        liveGenerators.add(generator);
    }
    
    public List<StatusEffectInstance> getTeamEffects() {
        return teamEffects;
    }
    
    public void addTeamEffect(StatusEffectInstance effect) {
        teamEffects.add(effect);
    }

    @Override
    public void setBedLocation(int arenaId, BlockPos pos) {
        bedLocations.put(arenaId, pos);
    }

    @Override
    public BlockPos getBedLocation(int arenaId) {
        return bedLocations.get(arenaId);
    }
}
