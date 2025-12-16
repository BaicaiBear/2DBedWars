package top.bearcabbage.twodimensional_bedwars.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import top.bearcabbage.twodimensional_bedwars.api.IArena;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;
import top.bearcabbage.twodimensional_bedwars.config.GameConfig;
import top.bearcabbage.twodimensional_bedwars.game.ScoreboardManager;
import top.bearcabbage.twodimensional_bedwars.mechanic.GamePlayingTask;

public class Arena implements IArena {
    private GameStatus status;
    private final List<ITeam> teams;
    private ScoreboardManager scoreboardManager;
    private final Map<UUID, ITeam> playerTeamMap;
    // 0 = Spectator, 1 = Alive (State is now in BedWarsPlayer)
    // Players waiting to join (before game starts)
    private final java.util.Set<UUID> waitingPlayers;

    private final Map<UUID, String> preferredTeams;
    private final GameConfig config;
    private final Map<UUID, Integer> respawnTargets;

    private ServerWorld gameWorld;
    private GamePlayingTask gamePlayingTask;

    // Countdown and Restore State
    private int ticksUntilStart = -1;
    private boolean mapRestoreComplete = false;
    private int requestedTeamCount = -1;

    public Arena() {
        this.status = GameStatus.WAITING;
        this.teams = new ArrayList<>();
        this.playerTeamMap = new HashMap<>();
        this.waitingPlayers = new java.util.HashSet<>();
        this.preferredTeams = new HashMap<>();
        this.config = GameConfig.getInstance();
        this.respawnTargets = new HashMap<>();
        this.placedBlocks = new java.util.HashSet<>();
        this.publicGenerators = new ArrayList<>();
    }

    // Generators not belonging to any team
    private final List<OreGenerator> publicGenerators;

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

        public ArenaData(Arena arena) {
            this.arena = arena;
        }

        public boolean isBlockPlayerPlaced(BlockPos pos) {
            return arena.isBlockPlayerPlaced(pos);
        }

        public void recordPlacedBlock(BlockPos pos) {
            arena.recordPlacedBlock(pos);
        }
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

    public List<OreGenerator> getPublicGenerators() {
        return publicGenerators;
    }

    @Override
    public boolean addPlayer(ServerPlayerEntity player) {
        if (status != GameStatus.WAITING && status != GameStatus.STARTING) {
            return false;
        }
        waitingPlayers.add(player.getUuid());
        return true;
    }

    @Override
    public void removePlayer(ServerPlayerEntity player) {
        playerTeamMap.remove(player.getUuid());
        waitingPlayers.remove(player.getUuid());
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
                world.getServer().getPlayerManager().getPlayerList()
                        .forEach(p -> p.sendMessage(Text.literal("§eGame starting in §c" + sec + " §eseconds!"), true));
            }

            if (ticksUntilStart <= 0) {
                if (mapRestoreComplete) {
                    beginMatch(world);
                } else {
                    if (Math.abs(ticksUntilStart) % 40 == 0) {
                        // Notify waiting
                        world.getServer().getPlayerManager().getPlayerList()
                                .forEach(p -> p.sendMessage(Text.literal("§7Waiting for Map Restoration..."), true));
                    }
                }
            }
        }

        else if (status == GameStatus.PLAYING) {
            if (gamePlayingTask == null) {
                gamePlayingTask = new GamePlayingTask(this);
            }
            gamePlayingTask.run(this.gameWorld != null ? this.gameWorld : world);

            // Tick Teams (Upgrades & Generators)
            for (ITeam team : teams) {
                team.tick(this.gameWorld != null ? this.gameWorld : world);
            }

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

                checkPortals(this.gameWorld);

                if (scoreboardManager != null) {
                    scoreboardManager.update();
                }
            }
        }

    }

    private void checkPortals(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator())
                continue;

            // strict collision check or just block at feet/head?
            // getBlockStateAtPos checks integer position.
            // Better: bounding box check or simple block check at pos and pos+1.
            BlockState feet = world.getBlockState(player.getBlockPos());
            BlockState head = world.getBlockState(player.getBlockPos().up());

            boolean inPortal = feet.getBlock() == net.minecraft.block.Blocks.NETHER_PORTAL ||
                    head.getBlock() == net.minecraft.block.Blocks.NETHER_PORTAL;

            if (inPortal) {
                // Prevent vanilla portal logic (teleport to Nether)
                player.setPortalCooldown(20);

                ITeam team = getTeam(player);
                if (team instanceof BedWarsTeam bwTeam) {
                    // Determine current arena
                    int currentArena = 1;
                    double dist1 = player.getPos().squaredDistanceTo(config.arenaRestoreConfig.arena1Bounds.center.x,
                            config.arenaRestoreConfig.arena1Bounds.center.y,
                            config.arenaRestoreConfig.arena1Bounds.center.z);
                    double dist2 = player.getPos().squaredDistanceTo(config.arenaRestoreConfig.arena2Bounds.center.x,
                            config.arenaRestoreConfig.arena2Bounds.center.y,
                            config.arenaRestoreConfig.arena2Bounds.center.z);
                    if (dist2 < dist1)
                        currentArena = 2;

                    int targetArena = (currentArena == 1) ? 2 : 1;

                    // Check Target Bed Status
                    // "If the bed on another side is safe" -> logic: can we go TO that side?
                    // Usually "Bed Destroyed" means you can't respawn there.
                    // If bed is missing in target arena, does it mean "base lost"?
                    // User said: "if the bed is broken, send a failure notice".
                    // This implies players use portals to TRAVEL between their bases.

                    if (bwTeam.isBedDestroyed(targetArena)) {
                        player.sendMessage(Text.literal("§cCannot teleport! Target Bed is Destroyed!"), true);
                    } else {
                        // Teleport
                        BlockPos targetSpawn = bwTeam.getSpawnPoint(targetArena);
                        if (targetSpawn != null) {
                            player.teleport(world, targetSpawn.getX() + 0.5, targetSpawn.getY(),
                                    targetSpawn.getZ() + 0.5,
                                    java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
                                    player.getYaw(), player.getPitch(), false);
                            player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        }
                    }
                }
            }
        }
    }

    // Called by command
    public void startGame(ServerWorld world, int teamCount) {
        if (status != GameStatus.WAITING)
            return;

        System.out.println("Initiating Game Start Sequence...");
        this.requestedTeamCount = teamCount;
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
            if (!waitingPlayers.contains(player.getUuid())) {
                addPlayer(player);
            }
        }
    }

    // Internal Method to Start Match
    private void beginMatch(ServerWorld world) {
        // Clear Item Entities in Arena
        if (this.gameWorld != null) {
            GameConfig.MapPoint min1 = config.arenaRestoreConfig.arena1Bounds.getMinPt();
            GameConfig.MapPoint max2 = config.arenaRestoreConfig.arena2Bounds.getMaxPt();

            this.gameWorld.getEntitiesByType(
                    net.minecraft.util.TypeFilter.instanceOf(net.minecraft.entity.ItemEntity.class),
                    new net.minecraft.util.math.Box(
                            min1.x - 100, -64, min1.z - 100,
                            max2.x + 100, 320, max2.z + 100),
                    e -> true).forEach(net.minecraft.entity.Entity::discard);
        }

        initialize(this.gameWorld, this.requestedTeamCount); // assume 4 teams max or pass dynamically?
        // Actually, teamCount was passed to startGame. We need to store it or assume
        // max.
        // Codebase seemed to pass it. I'll default to 4 for now or save it.
        // The old startGame used teamCount.

        status = GameStatus.PLAYING;
        // gameWorld.getGameRules().get(GameRules.DO_IMMEDIATE_RESPAWN).set(true,
        // world.getServer());
        gamePlayingTask = new GamePlayingTask(this);
        respawnTargets.clear();

        // Setup Scoreboard
        if (this.scoreboardManager != null) {
            this.scoreboardManager.cleanup();
        }
        this.scoreboardManager = new ScoreboardManager(this, world.getServer());
        this.scoreboardManager.setup();

        System.out.println("Starting Game with " + playerTeamMap.size() + " players assigned.");

        for (Map.Entry<UUID, ITeam> entry : playerTeamMap.entrySet()) {
            UUID uuid = entry.getKey();
            ITeam team = entry.getValue();
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && team instanceof BedWarsTeam bwTeam) {
                // Create BedWarsPlayer
                BedWarsPlayer bwPlayer = new BedWarsPlayer(uuid, team);
                bwPlayer.setState(1); // Alive
                team.addPlayer(bwPlayer); // Add to Team storage

                // Save Backup
                bwPlayer.saveLobbyState(player);

                player.getInventory().clear();

                BlockPos spawn = bwTeam.getSpawnPoint(1); // Start in Arena 1
                player.teleport(gameWorld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class), 0, 0,
                        false);
                player.changeGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.getHungerManager().setFoodLevel(20);

                // Apply Tools & Give Shop Item
                bwPlayer.applyTools(player);
                giveShopItem(player);
            }
        }

        preferredTeams.clear(); // One-time preference usage
        // Clear waiting players as they are now ingame (or keep them?)
        // waitingPlayers.clear(); // Keep them to know who joined initially?
        // Logic doesn't use them during game.
        waitingPlayers.clear();
    }

    private void triggerMapRestore(ServerWorld dest, Runnable callback) {
        MinecraftServer server = dest.getServer();
        Identifier blueprintId = Identifier.of("two-dimensional-bedwars", "blueprint");
        RegistryKey<World> bKey = RegistryKey.of(RegistryKeys.WORLD, blueprintId);
        ServerWorld blueprintWorld = server.getWorld(bKey);

        if (blueprintWorld != null) {
            List<MapManager.RegionPair> regions = new ArrayList<>();
            regions.add(new MapManager.RegionPair(config.arenaRestoreConfig.arena1Template,
                    config.arenaRestoreConfig.arena1Bounds));
            regions.add(new MapManager.RegionPair(config.arenaRestoreConfig.arena2Template,
                    config.arenaRestoreConfig.arena2Bounds));

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
            // Restore players via Teams
            for (ITeam team : teams) {
                if (team instanceof BedWarsTeam bwTeam) {
                    for (BedWarsPlayer bwPlayer : bwTeam.getPlayers()) {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(bwPlayer.getUuid());
                        if (player != null) {
                            bwPlayer.restoreLobbyState(player);
                            // Reset Gamemode to SURVIVAL
                            player.changeGameMode(GameMode.SURVIVAL);
                        }
                    }
                }
            }

            // Fallback: Check for any players with backups who might not be in a team?
            // (e.g. if they weren't assigned properly or something)
            // But for OOC refactoring, we rely on Team structure.
        }

        gamePlayingTask = null;

        teams.clear();
        playerTeamMap.clear();
        waitingPlayers.clear();
        placedBlocks.clear();

        // No triggerMapRestore here, as we do it on start.
        status = GameStatus.WAITING;
        if (scoreboardManager != null) {
            scoreboardManager.cleanup();
            scoreboardManager = null;
        }
        System.out.println("Game Stopped. Players restored.");
        // We could trigger a "Clean" restore here too if desired,
        // but user asked to "change the map restoring here instead of after command
        // stopping"
        // Wait, user said "change the map restoring here instead of after command
        // stopping".
        // So I REMOVE the restore from stopGame?
        // Yes, "change the map restoring here instead of after command stopping".
        // So I remove the restore logic from here.
    }

    public void initialize(ServerWorld world, int requestedTeamCount) {
        // Disable Mob Spawning
        world.getGameRules().get(net.minecraft.world.GameRules.DO_MOB_SPAWNING).set(false, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_TRADER_SPAWNING).set(false, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_PATROL_SPAWNING).set(false, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_WARDEN_SPAWNING).set(false, world.getServer());

        this.gameWorld = world;
        teams.clear();
        playerTeamMap.clear();

        // Load Config
        var offsets = config.baseOffsets;

        // Arena Centers from Restore Config
        GameConfig.MapPoint c1 = config.arenaRestoreConfig.arena1Bounds.center;
        GameConfig.MapPoint c2 = config.arenaRestoreConfig.arena2Bounds.center;

        String res1 = config.arena1.resourceType;
        String res2 = config.arena2.resourceType;

        // Determine active team count (2 or 4)
        // Default to 2 unless players selected Team 3/4 or command requested more
        int activeTeams = 2;

        // Check preferences
        for (String pref : preferredTeams.values()) {
            if ("Green".equalsIgnoreCase(pref) || "Yellow".equalsIgnoreCase(pref)) {
                activeTeams = 4;
                break;
            }
        }

        // Allow command override (if > 2)
        if (requestedTeamCount > 2) {
            activeTeams = 4;
        }

        // Create Teams and Bases
        teams.add(BedWarsTeam.createTeam(world, 1, "Red", Formatting.RED.getColorValue(), c1, c2, offsets.team1, res1,
                res2));
        teams.add(BedWarsTeam.createTeam(world, 2, "Blue", Formatting.BLUE.getColorValue(), c1, c2, offsets.team2, res1,
                res2));

        if (activeTeams >= 4) {
            teams.add(BedWarsTeam.createTeam(world, 3, "Green", Formatting.GREEN.getColorValue(), c1, c2, offsets.team3,
                    res1, res2));
            teams.add(BedWarsTeam.createTeam(world, 4, "Yellow", Formatting.YELLOW.getColorValue(), c1, c2,
                    offsets.team4, res1, res2));
        }

        // Spawn Public Generators
        // Arena 1
        spawnPublicGenerators(world, c1, config.arena1DiamondGenerators, config.diamondGenerator, Items.DIAMOND);
        spawnPublicGenerators(world, c1, config.arena1EmeraldGenerators, config.emeraldGenerator, Items.EMERALD);

        // Arena 2
        spawnPublicGenerators(world, c2, config.arena2GoldGenerators, config.goldGenerator, Items.GOLD_INGOT);
        spawnPublicGenerators(world, c2, config.arena2NetheriteGenerators, config.netheriteGenerator,
                Items.NETHERITE_INGOT);

        // Assign Players
        List<UUID> unassigned = new ArrayList<>();
        // ... (rest of method unchanged, relying on existing structure below this
        // replacement block?)
        // Actually, replacing initialize implies replacing the whole block.
        // Let's cut it off before 'Assign Players' to keep it cleaner?
        // No, I must replace contiguous block.
        // The original code has 'Assign Players' right after team creation.

        // Pass 1: Preferences
        for (UUID uuid : waitingPlayers) {
            String updatedPref = preferredTeams.get(uuid);
            if (updatedPref == null)
                continue; // Skip players who didn't select a team

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

    private void spawnPublicGenerators(ServerWorld world, GameConfig.MapPoint center, List<GameConfig.Offset> offsets,
            GameConfig.GeneratorSetting setting, net.minecraft.item.Item item) {
        for (GameConfig.Offset off : offsets) {
            BlockPos pos = new BlockPos(center.x + off.dx, center.y + off.dy, center.z + off.dz);
            publicGenerators.add(new OreGenerator(pos, item, setting.amount, setting.delaySeconds, setting.limit));
        }
    }

    // Old createTeam and helper methods removed - moved to BedWarsTeam factory.

    public boolean handleBlockBreak(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (status != GameStatus.PLAYING)
            return true;

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

            if (dist1 < dist2)
                arenaId = 1;
            else
                arenaId = 2;

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

    private boolean processBedBreak(ServerPlayerEntity breaker, ITeam breakerTeam, BedWarsTeam bedOwnerTeam,
            int arenaId) {
        if (breakerTeam == bedOwnerTeam) {
            breaker.sendMessage(Text.literal("§cYou cannot break your own bed!"));
            return false;
        }

        if (!bedOwnerTeam.isBedDestroyed(arenaId)) {
            bedOwnerTeam.setBedDestroyed(arenaId, true);
            breaker.getServer().getPlayerManager().getPlayerList()
                    .forEach(p -> p.sendMessage(Text.literal("§l§c" + bedOwnerTeam.getName() + " Bed (Arena " + arenaId
                            + ") was destroyed by " + breaker.getName().getString() + "!")));
            return true;
        }
        return true;
    }

    public void handleDeath(ServerPlayerEntity player, net.minecraft.entity.damage.DamageSource source) {
        ITeam team = getTeam(player);
        if (team == null)
            return;

        // Stats: Kill
        if (source != null && source.getAttacker() instanceof ServerPlayerEntity attacker) {
            ITeam attackerTeam = getTeam(attacker);
            if (attackerTeam != null && attackerTeam != team) {
                BedWarsPlayer attackerBwPlayer = attackerTeam.getPlayer(attacker.getUuid());
                if (attackerBwPlayer != null) {
                    attackerBwPlayer.addKill();
                }
            }
        }

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
            BedWarsPlayer bwPlayer = team.getPlayer(player.getUuid());
            if (bwPlayer != null) {
                bwPlayer.addDeath(); // Increment Death
                bwPlayer.handleDeath(player, this.gameWorld);
            }

            if (bwTeam.isBedDestroyed(targetArena)) {
                // Elimination
                player.sendMessage(Text.literal("You have been eliminated! (Target Bed Destroyed)"));
                player.changeGameMode(GameMode.SPECTATOR);
                if (bwPlayer != null)
                    bwPlayer.setState(0); // Spectator
                // playerStateMap.put(player.getUuid(), 0); // Removed
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
                    player.teleport(targetWorld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                            java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class), 0, 0,
                            false);
                    player.changeGameMode(GameMode.SURVIVAL);
                    player.setHealth(player.getMaxHealth());

                    BedWarsPlayer bwPlayer = team.getPlayer(uuid);
                    if (bwPlayer != null) {
                        bwPlayer.applyTools(player);
                    }
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

    public boolean handleEnderChest(ServerPlayerEntity player) {
        ITeam team = getTeam(player);
        if (team instanceof BedWarsTeam bwTeam) {
            net.minecraft.inventory.SimpleInventory chest = bwTeam.getEnderChest();
            player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> net.minecraft.screen.GenericContainerScreenHandler.createGeneric9x3(syncId, inv,
                            chest),
                    Text.literal("Team Ender Chest")));
            return true;
        }
        return false;
    }

    public GamePlayingTask getGamePlayingTask() {
        return gamePlayingTask;
    }
}
