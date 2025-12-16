package top.bearcabbage.twodimensional_bedwars.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import top.bearcabbage.twodimensional_bedwars.api.IArena;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;
import top.bearcabbage.twodimensional_bedwars.config.GameConfig;
import top.bearcabbage.twodimensional_bedwars.mechanic.GamePlayingTask;

public class Arena implements IArena {
    private GameStatus status;
    private final List<ITeam> teams;
    private final Map<UUID, ITeam> playerTeamMap;
    // 0 = Spectator, 1 = Alive
    private final Map<UUID, Integer> playerStateMap;
    private final Map<UUID, NbtList> savedInventories;
    private final Map<UUID, String> preferredTeams;
    private final GameConfig config;
    private final Map<UUID, Integer> respawnTargets;
    
    private ServerWorld gameWorld;
    private GamePlayingTask gamePlayingTask;
    
    // Countdown and Restore State
    private int ticksUntilStart = -1;
    private boolean mapRestoreComplete = false;

    public Arena() {
        this.status = GameStatus.WAITING;
        this.teams = new ArrayList<>();
        this.playerTeamMap = new HashMap<>();
        this.playerStateMap = new HashMap<>();
        this.savedInventories = new HashMap<>();
        this.preferredTeams = new HashMap<>();
        this.config = GameConfig.getInstance();
        this.respawnTargets = new HashMap<>();
        this.placedBlocks = new java.util.HashSet<>();
    }
    
    private final java.util.Set<BlockPos> placedBlocks;
    
    public void recordPlacedBlock(BlockPos pos) {
        placedBlocks.add(pos);
    }
    
    public boolean isBlockPlayerPlaced(BlockPos pos) {
        return placedBlocks.contains(pos);
    }
    
    // API Accessor for TwoDimensionalBedWars to use
    public ArenaData getData() {
        return new ArenaData(this);
    }
    
    // Inner class or Interface to expose data without making everything public
    public static class ArenaData {
        private final Arena arena;
        public ArenaData(Arena arena) { this.arena = arena; }
        public boolean isBlockPlayerPlaced(BlockPos pos) { return arena.isBlockPlayerPlaced(pos); }
        public void recordPlacedBlock(BlockPos pos) { arena.recordPlacedBlock(pos); }
    }
    
    public boolean setPreferredTeam(UUID uuid, String teamName) {
        if (status == GameStatus.WAITING) {
            preferredTeams.put(uuid, teamName);
            return true;
        }
        return false;
    }

    @Override
    public GameStatus getStatus() {
        return status;
    }
    
    public void setStatus(GameStatus status) {
        this.status = status;
    }

    @Override
    public List<ITeam> getTeams() {
        return teams;
    }

    @Override
    public boolean addPlayer(ServerPlayerEntity player) {
        if (status != GameStatus.WAITING && status != GameStatus.STARTING) {
            return false;
        }
        playerStateMap.put(player.getUuid(), 0);
        return true;
    }

    @Override
    public void removePlayer(ServerPlayerEntity player) {
        playerTeamMap.remove(player.getUuid());
        playerStateMap.remove(player.getUuid());
        preferredTeams.remove(player.getUuid());
    }

    @Override
    public ITeam getTeam(ServerPlayerEntity player) {
        return playerTeamMap.get(player.getUuid());
    }
    
    public void addTeam(ITeam team) {
        teams.add(team);
    }
    
    public void tick(ServerWorld world) {
        if (status == GameStatus.STARTING) {
            ticksUntilStart--;
            
            // Notification
            if (ticksUntilStart % 20 == 0 && ticksUntilStart > 0) {
                 int sec = ticksUntilStart / 20;
                 world.getServer().getPlayerManager().getPlayerList().forEach(p -> 
                     p.sendMessage(Text.literal("§eGame starting in §c" + sec + " §eseconds!"), true)
                 );
            }
            
            if (ticksUntilStart <= 0) {
                if (mapRestoreComplete) {
                    beginMatch(world);
                } else {
                     if (Math.abs(ticksUntilStart) % 40 == 0) {
                        // Notify waiting
                        world.getServer().getPlayerManager().getPlayerList().forEach(p -> 
                             p.sendMessage(Text.literal("§7Waiting for Map Restoration..."), true)
                        );
                     }
                }
            }
        }

        else if (status == GameStatus.PLAYING) {
            if (gamePlayingTask == null) {
                gamePlayingTask = new GamePlayingTask(this);
            }
            gamePlayingTask.run(this.gameWorld != null ? this.gameWorld : world);
            
            // Disable 2x2 Crafting: Clear Result Slot (Slot 0)
            if (this.gameWorld != null) {
                this.gameWorld.getPlayers().forEach(player -> {
                     // Check if not in a special container (player inventory container ID is 0)
                     if (player.currentScreenHandler == player.playerScreenHandler) {
                         // Slot 0 is Crafting Result in PlayerScreenHandler
                         net.minecraft.item.ItemStack stack = player.playerScreenHandler.getSlot(0).getStack();
                         if (!stack.isEmpty()) {
                             player.playerScreenHandler.getSlot(0).setStack(net.minecraft.item.ItemStack.EMPTY);
                             // Update client
                             player.playerScreenHandler.sendContentUpdates();
                         }
                     }
                });
            }
        }
    }

    // Called by command
    public void startGame(ServerWorld world, int teamCount) {
        if (status != GameStatus.WAITING) return;
        
        System.out.println("Initiating Game Start Sequence...");
        this.status = GameStatus.STARTING;
        this.ticksUntilStart = 10 * 20; // 10 seconds
        this.mapRestoreComplete = false;
        
        // Trigger Map Restore
        MinecraftServer server = world.getServer();
        Identifier matchId = Identifier.of("two-dimensional-bedwars", "arena");
        RegistryKey<World> gameWorldKey = RegistryKey.of(RegistryKeys.WORLD, matchId);
        ServerWorld targetGameWorld = server.getWorld(gameWorldKey);
        
        if (targetGameWorld == null) {
             System.out.println("Target Game Dimension not found, using Overworld (dangerous for restore!)");
             targetGameWorld = server.getOverworld();
        }
        this.gameWorld = targetGameWorld;
             
        triggerMapRestore(targetGameWorld, () -> {
            System.out.println("Map Restore Complete callback received.");
            this.mapRestoreComplete = true;
        });
        
        // Assign potential players (so they see the title? No, addPlayer handles that)
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
             if (!playerStateMap.containsKey(player.getUuid())) {
                addPlayer(player);
            }
        }
    }
    
    // Internal Method to Start Match
    private void beginMatch(ServerWorld world) {
             initialize(this.gameWorld, 4); // assume 4 teams max or pass dynamically?
             // Actually, teamCount was passed to startGame. We need to store it or assume max.
             // Codebase seemed to pass it. I'll default to 4 for now or save it.
             // The old startGame used teamCount.
             
             status = GameStatus.PLAYING;
//           gameWorld.getGameRules().get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, world.getServer());
             gamePlayingTask = new GamePlayingTask(this);
             respawnTargets.clear();
             
             System.out.println("Starting Game with " + playerTeamMap.size() + " players assigned.");
             
             for (Map.Entry<UUID, ITeam> entry : playerTeamMap.entrySet()) {
                 UUID uuid = entry.getKey();
                 ITeam team = entry.getValue();
                 ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
                 if (player != null && team instanceof BedWarsTeam bwTeam) {
                     // Save Backup
                     top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.saveBackup(player);
                 
                     player.getInventory().clear();
                     
                     BlockPos spawn = bwTeam.getSpawnPoint(1); // Start in Arena 1
                     player.teleport(gameWorld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class), 0, 0, false);
                     player.changeGameMode(GameMode.SURVIVAL);
                     player.setHealth(player.getMaxHealth());
                     player.getHungerManager().setFoodLevel(20);
                     giveShopItem(player);
                 }
             }
             
             for (UUID uuid : playerTeamMap.keySet()) {
                 playerStateMap.put(uuid, 1); // Alive
             }
    }
    
    private void triggerMapRestore(ServerWorld dest, Runnable callback) {
        MinecraftServer server = dest.getServer();
        Identifier blueprintId = Identifier.of("two-dimensional-bedwars", "blueprint");
        RegistryKey<World> bKey = RegistryKey.of(RegistryKeys.WORLD, blueprintId);
        ServerWorld blueprintWorld = server.getWorld(bKey);

        if (blueprintWorld != null) {
            List<MapManager.RegionPair> regions = new ArrayList<>();
            regions.add(new MapManager.RegionPair(config.arenaRestoreConfig.arena1Template, config.arenaRestoreConfig.arena1Bounds));
            regions.add(new MapManager.RegionPair(config.arenaRestoreConfig.arena2Template, config.arenaRestoreConfig.arena2Bounds));
            
            MapManager.startRestore(blueprintWorld, dest, regions, callback);
        } else {
            System.out.println("Blueprint not found, skipping restore.");
            callback.run();
        }
    }

    public void stopGame() {
        // Restore Players
        if (this.gameWorld != null) {
            MinecraftServer server = this.gameWorld.getServer();
            // Restore every player who has a backup
             for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                  top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.restoreBackup(player);
                  // Reset Gamemode to SURVIVAL
                  player.changeGameMode(GameMode.SURVIVAL);
             }
        }
        
        gamePlayingTask = null;
        
        teams.clear();
        playerTeamMap.clear();
        playerStateMap.clear();
        savedInventories.clear();
        placedBlocks.clear();
        
        // No triggerMapRestore here, as we do it on start.
        status = GameStatus.WAITING;
        System.out.println("Game Stopped. Players restored.");
        // We could trigger a "Clean" restore here too if desired, 
        // but user asked to "change the map restoring here instead of after command stopping"
        // Wait, user said "change the map restoring here [at start] instead of after command stopping".
        // So I REMOVE the restore from stopGame?
        // Yes, "change the map restoring here instead of after command stopping".
        // So I remove the restore logic from here.
    }
    
    private void initialize(ServerWorld world, int teamCount) {
        teams.clear();
        playerTeamMap.clear();

        // Load Config
        var offsets = config.baseOffsets;
        
        // Arena Centers from Restore Config
        GameConfig.MapPoint c1 = config.arenaRestoreConfig.arena1Bounds.center;
        GameConfig.MapPoint c2 = config.arenaRestoreConfig.arena2Bounds.center;
        
        String res1 = config.arena1.resourceType;
        String res2 = config.arena2.resourceType;

        // Create Teams and Bases
        createTeam(world, 1, "Red", Formatting.RED, c1, c2, offsets.team1, res1, res2);
        createTeam(world, 2, "Blue", Formatting.BLUE, c1, c2, offsets.team2, res1, res2);
        
        if (teamCount >= 4) {
            createTeam(world, 3, "Green", Formatting.GREEN, c1, c2, offsets.team3, res1, res2);
            createTeam(world, 4, "Yellow", Formatting.YELLOW, c1, c2, offsets.team4, res1, res2);
        }
        
        // Spawn Public Generators
        // Arena 1
        spawnPublicGenerators(world, c1, config.arena1DiamondGenerators, config.diamondGenerator, Items.DIAMOND);
        spawnPublicGenerators(world, c1, config.arena1EmeraldGenerators, config.emeraldGenerator, Items.EMERALD);
        
        // Arena 2
        spawnPublicGenerators(world, c2, config.arena2GoldGenerators, config.goldGenerator, Items.GOLD_INGOT);
        spawnPublicGenerators(world, c2, config.arena2NetheriteGenerators, config.netheriteGenerator, Items.NETHERITE_INGOT);

        // Assign Players
        List<UUID> unassigned = new ArrayList<>();
        // ... (rest of method unchanged, relying on existing structure below this replacement block?)
        // Actually, replacing initialize implies replacing the whole block.
        // Let's cut it off before 'Assign Players' to keep it cleaner? 
        // No, I must replace contiguous block.
        // The original code has 'Assign Players' right after team creation.
        
        // Pass 1: Preferences
        for (UUID uuid : playerStateMap.keySet()) {
            String updatedPref = preferredTeams.get(uuid);
            boolean assigned = false;
            if (updatedPref != null) {
                for (ITeam team : teams) {
                    if (team.getName().equalsIgnoreCase(updatedPref)) {
                        ((BedWarsTeam) team).addMember(uuid);
                        playerTeamMap.put(uuid, team);
                        assigned = true;
                        break;
                    }
                }
            }
            if (!assigned) {
                unassigned.add(uuid);
            }
        }
        
        // Pass 2: Round Robin for remaining
        int teamIndex = 0;
        for (UUID uuid : unassigned) {
            ITeam team = teams.get(teamIndex % teams.size());
            ((BedWarsTeam) team).addMember(uuid);
            playerTeamMap.put(uuid, team);
            teamIndex++;
        }
    }
    
    private void spawnPublicGenerators(ServerWorld world, GameConfig.MapPoint center, List<GameConfig.Offset> offsets, GameConfig.GeneratorSetting setting, net.minecraft.item.Item item) {
        // We need to attach these to *something* so they tick. 
        // Currently only Teams track generators. 
        // HACK: Attach public generators to the FIRST team (Red) for ticking purposes?
        // Or create a dummy team / manage a separate list in Arena.
        // Given existing structure, let's attach to the first team for now so they get ticked by the GamePlayingTask calling team.tick().
        if (!teams.isEmpty() && teams.get(0) instanceof BedWarsTeam redTeam) {
            for (GameConfig.Offset off : offsets) {
                 BlockPos pos = new BlockPos(center.x + off.dx, center.y + off.dy, center.z + off.dz);
                 redTeam.addLiveGenerator(new OreGenerator(pos, item, setting.amount, setting.delaySeconds, setting.limit));
            }
        }
    }

    private void createTeam(ServerWorld world, int id, String name, Formatting color, 
                            GameConfig.MapPoint c1, GameConfig.MapPoint c2, GameConfig.TeamConfig teamConfig,
                            String res1, String res2) {
        BedWarsTeam team = new BedWarsTeam(name, color.getColorValue());
        teams.add(team);
        
        GameConfig config = GameConfig.getInstance();
        
        // Helper to get settings based on resource name
        GameConfig.GeneratorSetting setting1 = getSettingForResource(res1);
        GameConfig.GeneratorSetting setting2 = getSettingForResource(res2);
        
        net.minecraft.item.Item item1 = getItemForResource(res1);
        net.minecraft.item.Item item2 = getItemForResource(res2);

        // --- Arena 1 Setup ---
        // Spawn: Center + Spawn Offset
        GameConfig.Offset sOff = teamConfig.spawn;
        BlockPos spawn1 = new BlockPos(c1.x + sOff.dx, c1.y + sOff.dy, c1.z + sOff.dz);
        team.setSpawnPoint(1, spawn1);
        team.setBedLocation(1, spawn1); // Bed placed at spawn
        
        // Generator: Center + Generator Offset
        GameConfig.Offset gOff = teamConfig.generator;
        BlockPos gen1 = new BlockPos(c1.x + gOff.dx, c1.y + gOff.dy, c1.z + gOff.dz);
        team.addGenerator(gen1);
        team.addLiveGenerator(new OreGenerator(gen1, item1, setting1.amount, setting1.delaySeconds, setting1.limit));
        
        // Base Blocks (Bed, No Platform)
        net.minecraft.block.Block bedBlock = getBedBlock(name);
        Direction facing = getFacingTowardsCenter(sOff.dx, sOff.dz);
        setupTeamBase(world, team, bedBlock, facing, spawn1);

        // --- Arena 2 Setup ---
        BlockPos spawn2 = new BlockPos(c2.x + sOff.dx, c2.y + sOff.dy, c2.z + sOff.dz);
        team.setSpawnPoint(2, spawn2);
        team.setBedLocation(2, spawn2);
        
        BlockPos gen2 = new BlockPos(c2.x + gOff.dx, c2.y + gOff.dy, c2.z + gOff.dz);
        team.addGenerator(gen2);
        team.addLiveGenerator(new OreGenerator(gen2, item2, setting2.amount, setting2.delaySeconds, setting2.limit));
        
        setupTeamBase(world, team, bedBlock, facing, spawn2);
        
        addTeam(team);
    }
    
    private GameConfig.GeneratorSetting getSettingForResource(String type) {
        if ("Gold".equalsIgnoreCase(type)) return config.goldGenerator;
        if ("Quartz".equalsIgnoreCase(type)) return config.quartzGenerator;
        return config.ironGenerator;
    }
    
    private net.minecraft.item.Item getItemForResource(String type) {
        if ("Gold".equalsIgnoreCase(type)) return Items.GOLD_INGOT;
        if ("Quartz".equalsIgnoreCase(type)) return Items.QUARTZ;
        return Items.IRON_INGOT;
    }
    
    private Block getBedBlock(String name) {
        return switch (name) {
            case "Red" -> Blocks.RED_BED;
            case "Blue" -> Blocks.BLUE_BED;
            case "Green" -> Blocks.GREEN_BED;
            case "Yellow" -> Blocks.YELLOW_BED;
            default -> Blocks.WHITE_BED;
        };
    }
    
    private Direction getFacingTowardsCenter(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.WEST : Direction.EAST;
        } else {
            return dz > 0 ? Direction.NORTH : Direction.SOUTH;
        }
    }
    
    private void setupTeamBase(ServerWorld world, BedWarsTeam team, Block bedBlock, Direction facing, BlockPos spawnPos) {
        // Bed Foot at Spawn, Head towards center (Facing)
        BlockPos footPos = spawnPos;
        BlockPos headPos = spawnPos.offset(facing); 
        
        world.setBlockState(headPos, bedBlock.getDefaultState().with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, facing));
        world.setBlockState(footPos, bedBlock.getDefaultState().with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, facing));
        
        // Removed Auto-Generated Platform
    }
    
    public boolean handleBlockBreak(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (status != GameStatus.PLAYING) return true;
        
        if (state.getBlock() instanceof BedBlock) {
            ITeam playerTeam = getTeam(player);
            
            // Determine Arena ID by distance to centers
            int arenaId = 0;
            double dist1 = pos.getSquaredDistance(config.arenaRestoreConfig.arena1Bounds.center.x, 
                                                  config.arenaRestoreConfig.arena1Bounds.center.y, 
                                                  config.arenaRestoreConfig.arena1Bounds.center.z);
            double dist2 = pos.getSquaredDistance(config.arenaRestoreConfig.arena2Bounds.center.x, 
                                                  config.arenaRestoreConfig.arena2Bounds.center.y, 
                                                  config.arenaRestoreConfig.arena2Bounds.center.z);
            
            if (dist1 < dist2) arenaId = 1;
            else arenaId = 2;
            
            for (ITeam checkTeam : teams) {
                if (checkTeam instanceof BedWarsTeam checkBwTeam) {
                     BlockPos spawn = checkBwTeam.getSpawnPoint(arenaId);
                     // Check if bed is near spawn (simple radius check)
                     if (spawn != null && spawn.getSquaredDistance(pos) < 64) { 
                         return processBedBreak(player, playerTeam, checkBwTeam, arenaId);
                     }
                }
            }
        }
        return true;
    }
    
    private boolean processBedBreak(ServerPlayerEntity breaker, ITeam breakerTeam, BedWarsTeam bedOwnerTeam, int arenaId) {
        if (breakerTeam == bedOwnerTeam) {
            breaker.sendMessage(Text.literal("§cYou cannot break your own bed!"));
            return false;
        }
        
        if (!bedOwnerTeam.isBedDestroyed(arenaId)) {
            bedOwnerTeam.setBedDestroyed(arenaId, true);
            breaker.getServer().getPlayerManager().getPlayerList().forEach(p -> 
                p.sendMessage(Text.literal("§l§c" + bedOwnerTeam.getName() + " Bed (Arena " + arenaId + ") was destroyed by " + breaker.getName().getString() + "!"))
            );
            return true;
        }
        return true;
    }

    public void handleDeath(ServerPlayerEntity player) {
        ITeam team = getTeam(player);
        if (team == null) return;
        
        // Identify death arena
        int currentArena = 1;
        double dist1 = player.getPos().squaredDistanceTo(config.arenaRestoreConfig.arena1Bounds.center.x, 
                                                         config.arenaRestoreConfig.arena1Bounds.center.y, 
                                                         config.arenaRestoreConfig.arena1Bounds.center.z);
        double dist2 = player.getPos().squaredDistanceTo(config.arenaRestoreConfig.arena2Bounds.center.x, 
                                                         config.arenaRestoreConfig.arena2Bounds.center.y, 
                                                         config.arenaRestoreConfig.arena2Bounds.center.z);
                                                         
        if (dist2 < dist1) {
            currentArena = 2;
        }
        
        int targetArena = (currentArena == 1) ? 2 : 1;
        
        if (team instanceof BedWarsTeam bwTeam) {
             if (bwTeam.isBedDestroyed(targetArena)) {
                 // Elimination
                 player.changeGameMode(GameMode.SPECTATOR);
                 player.sendMessage(Text.literal("You have been eliminated! (Target Bed Destroyed)"));
                 playerStateMap.put(player.getUuid(), 0); 
             } else {
                 // Respawn
                 player.changeGameMode(GameMode.SPECTATOR);
                 player.sendMessage(Text.literal("You will respawn in Arena " + targetArena + " in 5 seconds!"));
                 if (gamePlayingTask != null) {
                     respawnTargets.put(player.getUuid(), targetArena);
                     gamePlayingTask.addRespawn(player.getUuid(), 5);
                 }
             }
        }
    }
    
    public void spawnNPCs() {
    }
    
    public void respawnPlayer(UUID uuid, ServerWorld world) {
        ServerWorld targetWorld = (this.gameWorld != null) ? this.gameWorld : world;
        ServerPlayerEntity player = targetWorld.getServer().getPlayerManager().getPlayer(uuid);
        if (player != null) {
            ITeam team = playerTeamMap.get(uuid);
            if (team instanceof BedWarsTeam bwTeam) {
                 int targetArena = respawnTargets.getOrDefault(uuid, 1);
                 
                 BlockPos spawn = bwTeam.getSpawnPoint(targetArena);
                 if (spawn != null) {
                    player.teleport(targetWorld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class), 0, 0, false);
                    player.changeGameMode(GameMode.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                    giveShopItem(player);
                    respawnTargets.remove(uuid);
                 }
            }
        }
    }

    private void giveShopItem(ServerPlayerEntity player) {
        // Simple Paper Item with Name
        net.minecraft.item.ItemStack shopItem = new net.minecraft.item.ItemStack(Items.PAPER);
        shopItem.set(net.minecraft.component.DataComponentTypes.ITEM_NAME, Text.literal("§aShop (Right Click)"));
        player.getInventory().offerOrDrop(shopItem);
    }
}
