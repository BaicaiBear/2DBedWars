package top.bearcabbage.twodimensional_bedwars.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;

public class BedWarsTeam implements ITeam {
    private final String name;
    private final int color;
    private final Map<Integer, BlockPos> spawnPoints; // Key: Arena ID (1 or 2)
    private final Map<Integer, BlockPos> bedLocations;
    private final Map<Integer, Boolean> bedStates;    // Key: Arena ID, Value: isDestroyed
    
    private final List<UUID> members;
    private final List<BedWarsPlayer> players; // NEW
    private final List<BlockPos> generators;
    private final List<OreGenerator> liveGenerators; // Active tickable generators
    private final List<net.minecraft.entity.effect.StatusEffectInstance> teamEffects;
    
    // Upgrades
    private int sharpnessLevel = 0;
    private int protectionLevel = 0;
    private int hasteLevel = 0;
    private int forgeLevel = 0;
    
    // Shared Inventory
    private final net.minecraft.inventory.SimpleInventory enderChest;
    
    // Tick Counter
    private int tickTimer = 0;

    public BedWarsTeam(String name, int color) {
        this.name = name;
        this.color = color;
        this.spawnPoints = new HashMap<>();
        this.bedStates = new HashMap<>();
        this.bedLocations = new HashMap<>();
        
        this.members = new ArrayList<>();
        this.players = new ArrayList<>(); // NEW
        this.generators = new ArrayList<>();
        this.liveGenerators = new ArrayList<>();
        this.teamEffects = new ArrayList<>();
        
        this.enderChest = new net.minecraft.inventory.SimpleInventory(27);
    }
    
    public net.minecraft.inventory.SimpleInventory getEnderChest() {
        return enderChest;
    }
    
    public void tick(net.minecraft.server.world.ServerWorld world) {
        tickTimer++;
        
        // Tick Generators
        for (OreGenerator gen : liveGenerators) {
            gen.tick(world);
        }
        
        // Every 10 ticks: Apply Upgrades
        if (tickTimer % 10 == 0) {
            applyTeamUpgrades(world);
        }
    }
    
    private void applyTeamUpgrades(net.minecraft.server.world.ServerWorld world) {
        net.minecraft.registry.Registry<net.minecraft.enchantment.Enchantment> registry = world.getRegistryManager().getOptional(net.minecraft.registry.RegistryKeys.ENCHANTMENT).orElseThrow();
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.enchantment.Enchantment> sharpness = registry.getEntry(net.minecraft.enchantment.Enchantments.SHARPNESS.getValue()).orElseThrow();
        net.minecraft.registry.entry.RegistryEntry<net.minecraft.enchantment.Enchantment> protection = registry.getEntry(net.minecraft.enchantment.Enchantments.PROTECTION.getValue()).orElseThrow();

        for (BedWarsPlayer bwPlayer : players) {
            net.minecraft.server.network.ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(bwPlayer.getUuid());
            if (player != null && bwPlayer.getState() == 1) { // Alive
                
                // 1. Haste Effect
                if (hasteLevel > 0) {
                    // Haste I = amplifier 0, Haste II = amplifier 1
                    int amp = hasteLevel - 1;
                    // Apply for 12 seconds (240 ticks) to ensure continuity
                    player.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.HASTE, 240, amp, true, false, true));
                }
                
                // 2. Sharpness on Swords
                if (sharpnessLevel > 0) {
                     for (int i=0; i<player.getInventory().size(); i++) {
                         net.minecraft.item.ItemStack stack = player.getInventory().getStack(i);
                         if (!stack.isEmpty() && isSword(stack)) {
                             applyEnchantment(stack, sharpness, sharpnessLevel);
                         }
                     }
                }
                
                // 3. Protection on Armor
                if (protectionLevel > 0) {
                     for (int i=0; i<4; i++) {
                         net.minecraft.item.ItemStack stack = player.getInventory().getStack(36 + i); // 36-39 are armor slots
                         if (!stack.isEmpty()) {
                              applyEnchantment(stack, protection, protectionLevel);
                         }
                     }
                }
            }
        }
    }
    
    // Helper to apply enchant safely (only if lower)
    private void applyEnchantment(net.minecraft.item.ItemStack stack, net.minecraft.registry.entry.RegistryEntry<net.minecraft.enchantment.Enchantment> enchant, int level) {
         net.minecraft.component.type.ItemEnchantmentsComponent enchants = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack);
         int current = enchants.getLevel(enchant);
         if (current < level) {
             net.minecraft.component.type.ItemEnchantmentsComponent.Builder builder = new net.minecraft.component.type.ItemEnchantmentsComponent.Builder(enchants);
             builder.set(enchant, level);
             net.minecraft.enchantment.EnchantmentHelper.set(stack, builder.build());
         }
    }
    
    private boolean isSword(net.minecraft.item.ItemStack stack) {
        net.minecraft.item.Item item = stack.getItem();
        return item == net.minecraft.item.Items.WOODEN_SWORD || 
               item == net.minecraft.item.Items.STONE_SWORD || 
               item == net.minecraft.item.Items.IRON_SWORD || 
               item == net.minecraft.item.Items.GOLDEN_SWORD || 
               item == net.minecraft.item.Items.DIAMOND_SWORD || 
               item == net.minecraft.item.Items.NETHERITE_SWORD;
    }
    
    // Upgrade Setters
    public void setSharpnessLevel(int level) { this.sharpnessLevel = level; }
    public int getSharpnessLevel() { return sharpnessLevel; }
    
    public void setProtectionLevel(int level) { this.protectionLevel = level; }
    public int getProtectionLevel() { return protectionLevel; }
    
    public void setHasteLevel(int level) { this.hasteLevel = level; }
    public int getHasteLevel() { return hasteLevel; }
    
    public void setForgeLevel(int level) { 
        this.forgeLevel = level; 
        updateGenerators();
    }
    public int getForgeLevel() { return forgeLevel; }
    
    private void updateGenerators() {
        liveGenerators.clear();
        top.bearcabbage.twodimensional_bedwars.config.GameConfig config = top.bearcabbage.twodimensional_bedwars.config.GameConfig.getInstance();
        
        // Find recipes for current forge level
        List<top.bearcabbage.twodimensional_bedwars.config.GameConfig.ForgeResource> resources = new ArrayList<>();
        if (config.forgeLevels != null) {
            for (top.bearcabbage.twodimensional_bedwars.config.GameConfig.ForgeLevel fl : config.forgeLevels) {
                if (fl.level == this.forgeLevel) {
                    resources = fl.resources;
                    break;
                }
            }
        }
        
        // Use default if nothing found (e.g. Level 0 fallback)
        if (resources.isEmpty() && this.forgeLevel == 0) {
            // Hardcoded fallback? Or just empty.
        }

        for (BlockPos pos : generators) {
            for (top.bearcabbage.twodimensional_bedwars.config.GameConfig.ForgeResource res : resources) {
                 // 1.21 Identifier fix
                 net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(res.material);
                 if (id == null) id = net.minecraft.util.Identifier.of("minecraft", "air");
                 
                 net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(id);
                 if (item != net.minecraft.item.Items.AIR) {
                     liveGenerators.add(new OreGenerator(pos, item, res.amount, res.delay, res.limit));
                 }
            }
        }
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

    @Override
    public void addPlayer(BedWarsPlayer player) {
        players.add(player);
        if (!members.contains(player.getUuid())) {
            members.add(player.getUuid());
        }
    }

    @Override
    public List<BedWarsPlayer> getPlayers() {
        return players;
    }
    
    @Override
    public BedWarsPlayer getPlayer(UUID uuid) {
        for (BedWarsPlayer p : players) {
            if (p.getUuid().equals(uuid)) {
                return p;
            }
        }
        return null;
    }
    // --- Factory / Setup Logic ---

    public static BedWarsTeam createTeam(net.minecraft.server.world.ServerWorld world, int id, String name, int colorValue, 
                                         top.bearcabbage.twodimensional_bedwars.config.GameConfig.MapPoint c1, 
                                         top.bearcabbage.twodimensional_bedwars.config.GameConfig.MapPoint c2, 
                                         top.bearcabbage.twodimensional_bedwars.config.GameConfig.TeamConfig teamConfig,
                                         String res1, String res2) {
        
        BedWarsTeam team = new BedWarsTeam(name, colorValue);
        top.bearcabbage.twodimensional_bedwars.config.GameConfig config = top.bearcabbage.twodimensional_bedwars.config.GameConfig.getInstance();
        
        // --- Arena 1 Setup ---
        // Spawn: Center + Spawn Offset
        top.bearcabbage.twodimensional_bedwars.config.GameConfig.Offset sOff = teamConfig.spawn;
        BlockPos spawn1 = new BlockPos(c1.x + sOff.dx, c1.y + sOff.dy, c1.z + sOff.dz);
        team.setSpawnPoint(1, spawn1);
        team.setBedLocation(1, spawn1); // Bed placed at spawn
        
        // Generator: Center + Generator Offset
        top.bearcabbage.twodimensional_bedwars.config.GameConfig.Offset gOff = teamConfig.generator;
        BlockPos gen1 = new BlockPos(c1.x + gOff.dx, c1.y + gOff.dy, c1.z + gOff.dz);
        team.addGenerator(gen1);
        // Live generators managed by Forge Level
        
        // Base Blocks (Bed, No Platform)
        net.minecraft.block.Block bedBlock = getBedBlock(name);
        net.minecraft.util.math.Direction facing = getFacingTowardsCenter(sOff.dx, sOff.dz);
        setupTeamBase(world, team, bedBlock, facing, spawn1);

        // --- Arena 2 Setup ---
        BlockPos spawn2 = new BlockPos(c2.x + sOff.dx, c2.y + sOff.dy, c2.z + sOff.dz);
        team.setSpawnPoint(2, spawn2);
        team.setBedLocation(2, spawn2);
        
        BlockPos gen2 = new BlockPos(c2.x + gOff.dx, c2.y + gOff.dy, c2.z + gOff.dz);
        team.addGenerator(gen2);
        // Live generators managed by Forge Level
        
        setupTeamBase(world, team, bedBlock, facing, spawn2);
        
        // Initialize Generators
        team.setForgeLevel(0);
        
        return team;
    }
    
    // Removed unused helpers: getSettingForResource, getItemForResource
    
    private static net.minecraft.block.Block getBedBlock(String name) {
        if ("Red".equalsIgnoreCase(name)) return net.minecraft.block.Blocks.RED_BED;
        if ("Blue".equalsIgnoreCase(name)) return net.minecraft.block.Blocks.BLUE_BED;
        if ("Green".equalsIgnoreCase(name)) return net.minecraft.block.Blocks.GREEN_BED;
        if ("Yellow".equalsIgnoreCase(name)) return net.minecraft.block.Blocks.YELLOW_BED;
        return net.minecraft.block.Blocks.WHITE_BED;
    }
    
    private static net.minecraft.util.math.Direction getFacingTowardsCenter(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? net.minecraft.util.math.Direction.WEST : net.minecraft.util.math.Direction.EAST;
        } else {
            return dz > 0 ? net.minecraft.util.math.Direction.NORTH : net.minecraft.util.math.Direction.SOUTH;
        }
    }
    
    private static void setupTeamBase(net.minecraft.server.world.ServerWorld world, BedWarsTeam team, net.minecraft.block.Block bedBlock, net.minecraft.util.math.Direction facing, BlockPos spawnPos) {
        // Bed Foot at Spawn, Head towards center (Facing)
        BlockPos footPos = spawnPos;
        BlockPos headPos = spawnPos.offset(facing); 
        
        world.setBlockState(headPos, bedBlock.getDefaultState().with(net.minecraft.block.BedBlock.PART, net.minecraft.block.enums.BedPart.HEAD).with(net.minecraft.block.BedBlock.FACING, facing));
        world.setBlockState(footPos, bedBlock.getDefaultState().with(net.minecraft.block.BedBlock.PART, net.minecraft.block.enums.BedPart.FOOT).with(net.minecraft.block.BedBlock.FACING, facing));
    }
}
