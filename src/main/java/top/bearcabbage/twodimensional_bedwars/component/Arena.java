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
        this.blastProofBlocks = new java.util.HashSet<>(); // NEW
        this.publicGenerators = new ArrayList<>();
    }

    private void setChunksForced(ServerWorld world, boolean forced) {
        // Arena 1: (0,0) radius ~120 blocks -> -8 to 8 chunks
        for (int cx = -8; cx <= 8; cx++) {
            for (int cz = -8; cz <= 8; cz++) {
                world.setChunkForced(cx, cz, forced);
            }
        }
        // Arena 2: (400,0) -> Chunk 25. Radius ~120 blocks -> 17 to 33
        for (int cx = 17; cx <= 33; cx++) {
            for (int cz = -8; cz <= 8; cz++) {
                world.setChunkForced(cx, cz, forced);
            }
        }
    }

    // Generators not belonging to any team
    private final List<OreGenerator> publicGenerators;

    private final java.util.Set<BlockPos> placedBlocks;
    private final java.util.Set<BlockPos> blastProofBlocks; // NEW

    public void recordPlacedBlock(BlockPos pos) {
        placedBlocks.add(pos);
    }

    public void recordBlastProof(BlockPos pos) {
        blastProofBlocks.add(pos);
        recordPlacedBlock(pos);
    }

    public boolean isBlockPlayerPlaced(BlockPos pos) {
        return placedBlocks.contains(pos);
    }

    public boolean isBlastProof(BlockPos pos) {
        return blastProofBlocks.contains(pos);
    }

    // API Accessor for TwoDimensionalBedWars to use
    public ArenaData getData() {
        return new ArenaData(this);
    }

    // Inner class or Interface to expose data without making everything public
    // Helper for broadcasting to all relevant players (Participants + Spectators)
    // Better implementation:
    // We need a server reference.
    // if (this.gameWorld != null)
    // else if (!teams.isEmpty()) {
    // try to find via team members
    // }

    // Actually, we can just use the server reference from a playerContext or pass
    // it.
    // But since we are inside Arena, we might not have a permanent server ref if
    // gameWorld is null.
    // However, `tick` provides `world`.
    public void broadcastToGame(MinecraftServer server, Text message) {
        java.util.Set<UUID> targets = new java.util.HashSet<>();
        targets.addAll(getParticipantUUIDs());
        targets.addAll(spectators);

        for (UUID uuid : targets) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
            if (p != null) {
                p.sendMessage(message, false);
            }
        }
    }

    // Action Bar Version
    public void broadcastActionbarToGame(MinecraftServer server, Text message) {
        java.util.Set<UUID> targets = new java.util.HashSet<>();
        targets.addAll(getParticipantUUIDs());
        targets.addAll(spectators);

        for (UUID uuid : targets) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
            if (p != null) {
                p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket(message));
            }
        }
    }

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

        public void recordBlastProof(BlockPos pos) {
            arena.recordBlastProof(pos);
        }

        public boolean isBlastProof(BlockPos pos) {
            return arena.isBlastProof(pos);
        }
    }

    public boolean setPreferredTeam(UUID uuid, String teamName) {
        if (status == GameStatus.WAITING || status == GameStatus.STARTING) {
            preferredTeams.put(uuid, teamName);
            waitingPlayers.add(uuid); // Join Game
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

    public enum JoinResult {
        SUCCESS,
        GAME_RUNNING,
        ALREADY_JOINED
    }

    public JoinResult joinPlayer(ServerPlayerEntity player) {
        if (status != GameStatus.WAITING && status != GameStatus.STARTING) {
            return JoinResult.GAME_RUNNING;
        }
        if (waitingPlayers.contains(player.getUuid()) || playerTeamMap.containsKey(player.getUuid())) {
            return JoinResult.ALREADY_JOINED;
        }
        waitingPlayers.add(player.getUuid());
        return JoinResult.SUCCESS;
    }

    @Override
    public boolean addPlayer(ServerPlayerEntity player) {
        // Deprecated/Legacy support wrapper
        return joinPlayer(player) == JoinResult.SUCCESS;
    }

    private final java.util.Set<UUID> spectators = new java.util.HashSet<>();

    public boolean joinSpectator(ServerPlayerEntity player) {
        if (waitingPlayers.contains(player.getUuid()) || playerTeamMap.containsKey(player.getUuid())
                || spectators.contains(player.getUuid())) {
            return false;
        }

        // Save Backup
        top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.saveBackup(player);
        player.getInventory().clear();

        spectators.add(player.getUuid());

        // Teleport to Arena 1 Center
        GameConfig.MapPoint center = config.arenaRestoreConfig.arena1Bounds.center;
        player.teleport(gameWorld, center.x + 0.5, center.y + 10, center.z + 0.5,
                java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class), 0, 0, false);
        player.fallDistance = 0;
        player.changeGameMode(GameMode.SPECTATOR);

        return true;
    }

    public void leavePlayer(ServerPlayerEntity player) {
        leavePlayer(player, false);
    }

    public void leavePlayer(ServerPlayerEntity player, boolean skipRestore) {
        UUID uuid = player.getUuid();

        // 1. Check Team (Active Player)
        ITeam team = playerTeamMap.get(uuid);
        if (team instanceof BedWarsTeam bwTeam) {
            BedWarsPlayer bwPlayer = bwTeam.getPlayer(uuid);
            if (bwPlayer != null) {
                if (!skipRestore) {
                    // Try to restore. If fail, wipe inventory to prevent game item leak.
                    if (!bwPlayer.restoreLobbyState(player)) {
                        restoreToSpawn(player);
                    }
                }
            }
            bwTeam.removeMember(uuid);

            // Check if team is empty and game is playing
            if (status == GameStatus.PLAYING && bwTeam.getMembers().isEmpty()) {
                boolean wasAlive = !bwTeam.isBedDestroyed();
                // Destroy Beds
                bwTeam.setBedDestroyed(1, true);
                bwTeam.setBedDestroyed(2, true);

                // Broadcast elimination if they were still in game (beds not destroyed)
                if (wasAlive) {
                    Text msg = Text.translatable("two-dimensional-bedwars.event.team_eliminated_quit",
                            bwTeam.getName());
                    player.getServer().getPlayerManager().getPlayerList().forEach(p -> p.sendMessage(msg, false));
                }
            }
            cleanupPlayer(player);
            playerTeamMap.remove(uuid);

            // Check if ALL players have left (Empty Game)
            if (status == GameStatus.PLAYING && getParticipantUUIDs().isEmpty()) {
                broadcastToGame(player.getServer(), Text.translatable("two-dimensional-bedwars.event.game_end"));
                stopGame();
            }
        }
        // 2. Check Spectator
        else if (spectators.contains(uuid)) {
            if (!skipRestore) {
                boolean restored = top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.restoreBackup(player);
                if (!restored) {
                    restoreToSpawn(player); // Fallback wipe
                    player.sendMessage(Text.translatable("two-dimensional-bedwars.backup.restore_fail"), false);
                }
            }
            cleanupPlayer(player);
            spectators.remove(uuid);
        }
        // 3. Check Waiting
        else if (waitingPlayers.contains(uuid)) {
            // Just remove from list. No backup needed (didn't convert yet). No wipe needed.
            waitingPlayers.remove(uuid);
            // Optional: Teleport to spawn if they are in the arena world?
            // If they joined, they might be in the arena world.
            // If we don't TP them, they stay there.
            // Current `joinPlayer` adds them to list but doesn't TP them until start.
            // But if they are in the world, we should probably send them back to spawn?
            // Assuming waiting players are in the lobby/overworld until start.
        }

        // 4. Preferred Teams cleanup
        preferredTeams.remove(uuid);

        // Broadcast Leave (Arena Only)
        Text leaveMsg = Text.translatable("two-dimensional-bedwars.command.leave.broadcast", player.getDisplayName())
                .formatted(Formatting.YELLOW);
        broadcastToGame(player.getServer(), leaveMsg);
    }

    private void cleanupPlayer(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SURVIVAL);
        player.clearStatusEffects();
        player.fallDistance = 0;

        // Clear Scoreboard
        player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket(
                net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR, null));
    }

    @Override
    public void removePlayer(ServerPlayerEntity player) {
        leavePlayer(player);
    }

    private void restoreToSpawn(ServerPlayerEntity player) {
        ServerWorld overworld = player.getServer().getOverworld();
        BlockPos spawn = overworld.getSpawnPos();
        player.teleport(overworld, spawn.getX(), spawn.getY(), spawn.getZ(),
                java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
                0, 0, false);
        player.fallDistance = 0; // Reset Fall Distance
        // Safety: Clear inventory to prevent item leaking if backup failed
        player.getInventory().clear();
    }

    @Override
    public ITeam getTeam(ServerPlayerEntity player) {
        return playerTeamMap.get(player.getUuid());
    }

    public void addTeam(ITeam team) {
        teams.add(team);
    }

    public java.util.Set<UUID> getParticipantUUIDs() {
        return (status == GameStatus.PLAYING) ? playerTeamMap.keySet() : waitingPlayers;
    }

    public void tick(ServerWorld world) {
        // top.bearcabbage.twodimensional_bedwars.TwoDimensionalBedWars.LOGGER.info("Arena
        // Tick: " + System.identityHashCode(this) + " Status: " + status);
        if (status == GameStatus.STARTING) {
            ticksUntilStart--;

            // Notification (Action Bar)
            if (ticksUntilStart % 20 == 0 && ticksUntilStart > 0) {
                int sec = ticksUntilStart / 20;
                Text msg = Text.translatable("two-dimensional-bedwars.arena.starting_in",
                        Text.literal(String.valueOf(sec)).formatted(Formatting.RED))
                        .formatted(Formatting.YELLOW);

                broadcastActionbarToGame(world.getServer(), msg);
            }

            if (ticksUntilStart <= 0) {
                if (mapRestoreComplete) {
                    // Broadcast Teleporting
                    Text msg = Text.translatable("two-dimensional-bedwars.arena.map_restored")
                            .formatted(Formatting.GREEN);
                    broadcastToGame(world.getServer(), msg);

                    beginMatch(world);
                } else {
                    if (Math.abs(ticksUntilStart) % 40 == 0) {
                        // Notify waiting (Action Bar)
                        Text msg = Text.translatable("two-dimensional-bedwars.arena.waiting_map")
                                .formatted(Formatting.GRAY);
                        broadcastActionbarToGame(world.getServer(), msg);
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
                        player.sendMessage(Text.translatable("two-dimensional-bedwars.arena.teleport_fail_bed"), true);
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

        triggerMapRestore(targetGameWorld, () -> {
            System.out.println("Map Restore Complete callback received.");
            this.mapRestoreComplete = true;
        });

        // Removed: Logic that auto-added all online players.
        // Players must now explicitly join via /bedwars join or /bedwars team.
    }

    // Internal Method to Start Match
    private void beginMatch(ServerWorld world) {
        // Clear ALL Entities in Arena (Mobs, Items, Projectiles, etc.) except Players
        if (this.gameWorld != null) {
            GameConfig.MapPoint min1 = config.arenaRestoreConfig.arena1Bounds.getMinPt();
            GameConfig.MapPoint max2 = config.arenaRestoreConfig.arena2Bounds.getMaxPt();

            this.gameWorld.getEntitiesByType(
                    net.minecraft.util.TypeFilter.instanceOf(net.minecraft.entity.Entity.class),
                    new net.minecraft.util.math.Box(
                            min1.x - 100, -64, min1.z - 100,
                            max2.x + 100, 320, max2.z + 100),
                    e -> !(e instanceof net.minecraft.entity.player.PlayerEntity))
                    .forEach(net.minecraft.entity.Entity::discard);
        }

        initialize(this.gameWorld, this.requestedTeamCount);

        // Validate Team Balance
        boolean balanced = true;
        for (ITeam team : teams) {
            if (team instanceof BedWarsTeam bwTeam) {
                if (bwTeam.getMembers().isEmpty()) {
                    balanced = false;
                    break;
                }
            }
        }

        if (!balanced) {
            if (this.gameWorld != null) {
                this.gameWorld.getServer().getPlayerManager().getPlayerList()
                        .forEach(p -> p.sendMessage(
                                Text.translatable("two-dimensional-bedwars.arena.start_fail_players"),
                                false));
            }

            this.teams.clear();
            this.playerTeamMap.clear();
            this.preferredTeams.clear();
            this.publicGenerators.clear();
            this.status = GameStatus.WAITING;
            return;
        }

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
            setChunksForced(this.gameWorld, false);
            MinecraftServer server = this.gameWorld.getServer();
            // Restore players via Teams
            for (ITeam team : teams) {
                if (team instanceof BedWarsTeam bwTeam) {
                    for (BedWarsPlayer bwPlayer : bwTeam.getPlayers()) {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(bwPlayer.getUuid());
                        if (player != null) {
                            if (!bwPlayer.restoreLobbyState(player)) {
                                restoreToSpawn(player);
                                player.sendMessage(Text.translatable("two-dimensional-bedwars.backup.restore_fail"),
                                        false);
                            }
                            // Reset Gamemode to SURVIVAL
                            player.changeGameMode(GameMode.SURVIVAL);
                            player.clearStatusEffects();
                            player.fallDistance = 0;
                            player.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
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
        blastProofBlocks.clear(); // NEW

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
        world.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
        world.getGameRules().get(net.minecraft.world.GameRules.DO_WEATHER_CYCLE).set(false, world.getServer());
        world.setTimeOfDay(6000);
        world.setWeather(0, 0, false, false);

        this.gameWorld = world;
        setChunksForced(world, true);
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
        spawnPublicGenerators(world, c1, config.arena1DiamondGenerators, config.diamondGenerator, Items.DIAMOND,
                "Diamond");
        spawnPublicGenerators(world, c1, config.arena1EmeraldGenerators, config.emeraldGenerator, Items.EMERALD,
                "Emerald");

        // Arena 2
        spawnPublicGenerators(world, c2, config.arena2GoldGenerators, config.goldGenerator, Items.GOLD_INGOT, "Gold");
        spawnPublicGenerators(world, c2, config.arena2NetheriteGenerators, config.netheriteGenerator,
                Items.NETHERITE_INGOT, "Netherite");

        // Pass 1: Preferences
        List<UUID> unassigned = new ArrayList<>();
        for (UUID uuid : waitingPlayers) {
            String updatedPref = preferredTeams.get(uuid);
            // If pref is null, treat as unassigned

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

        // Pass 2: Balanced assignment for remaining
        for (UUID uuid : unassigned) {
            // Find team with lowest member count
            ITeam minTeam = teams.get(0);
            int minSize = minTeam.getMembers().size();

            for (int i = 1; i < teams.size(); i++) {
                ITeam t = teams.get(i);
                int size = t.getMembers().size();
                if (size < minSize) {
                    minTeam = t;
                    minSize = size;
                }
            }

            ((BedWarsTeam) minTeam).addMember(uuid);
            playerTeamMap.put(uuid, minTeam);
        }
    }

    private void spawnPublicGenerators(ServerWorld world, GameConfig.MapPoint center, List<GameConfig.Offset> offsets,
            GameConfig.GeneratorSetting setting, net.minecraft.item.Item item, String type) {
        for (GameConfig.Offset off : offsets) {
            BlockPos pos = new BlockPos(center.x + off.dx, center.y + off.dy, center.z + off.dz);
            publicGenerators
                    .add(new OreGenerator(pos, item, setting.amount, setting.delaySeconds, setting.limit, type));
        }
    }

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
            breaker.sendMessage(Text.translatable("two-dimensional-bedwars.arena.break_own_bed"));
            return false;
        }

        if (!bedOwnerTeam.isBedDestroyed(arenaId)) {
            bedOwnerTeam.setBedDestroyed(arenaId, true);

            Text teamName = Text.translatable("two-dimensional-bedwars.team." + bedOwnerTeam.getName().toLowerCase())
                    .formatted(bedOwnerTeam.getColor() == Formatting.RED.getColorValue() ? Formatting.RED
                            : bedOwnerTeam.getColor() == Formatting.BLUE.getColorValue() ? Formatting.BLUE
                                    : bedOwnerTeam.getColor() == Formatting.GREEN.getColorValue() ? Formatting.GREEN
                                            : bedOwnerTeam.getColor() == Formatting.YELLOW.getColorValue()
                                                    ? Formatting.YELLOW
                                                    : Formatting.WHITE);
            // Wait, BedWarsTeam stores color as int. Formatting.RED.getColorValue() is int.
            // But Text.formatted(Formatting) takes Formatting enum.
            // I should use a helper to get Formatting from int or just switch case on name
            // since I assume standard colors.

            Formatting teamColor = Formatting.WHITE;
            if (bedOwnerTeam.getName().equalsIgnoreCase("Red"))
                teamColor = Formatting.RED;
            else if (bedOwnerTeam.getName().equalsIgnoreCase("Blue"))
                teamColor = Formatting.BLUE;
            else if (bedOwnerTeam.getName().equalsIgnoreCase("Green"))
                teamColor = Formatting.GREEN;
            else if (bedOwnerTeam.getName().equalsIgnoreCase("Yellow"))
                teamColor = Formatting.YELLOW;

            // Re-construct teamName with correct formatting
            teamName = Text.translatable("two-dimensional-bedwars.team." + bedOwnerTeam.getName().toLowerCase())
                    .formatted(teamColor);

            Text msg = Text.translatable("two-dimensional-bedwars.event.bed_destroy",
                    teamName,
                    breaker.getDisplayName())
                    .formatted(Formatting.WHITE);

            broadcastToGame(breaker.getServer(), msg);
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

                // Broadcast Death Message (Arena Only)
                Text deathMsg = source.getDeathMessage(player);
                if (deathMsg != null) {
                    broadcastToGame(player.getServer(), deathMsg);
                }
            }

            if (bwTeam.isBedDestroyed(targetArena)) {
                // Elimination
                player.sendMessage(Text.translatable("two-dimensional-bedwars.arena.eliminated"));
                player.changeGameMode(GameMode.SPECTATOR);
                if (bwPlayer != null)
                    bwPlayer.setState(0); // Spectator
                // playerStateMap.put(player.getUuid(), 0); // Removed
            } else {
                player.changeGameMode(GameMode.SPECTATOR);
                Text arenaName = targetArena == 1 ? Text.translatable("two-dimensional-bedwars.arena.overworld")
                        : Text.translatable("two-dimensional-bedwars.arena.nether");
                player.sendMessage(Text.translatable("two-dimensional-bedwars.arena.respawn_in", arenaName));
                if (gamePlayingTask != null) {
                    respawnTargets.put(player.getUuid(), targetArena); // Keep integer for logic
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
        shopItem.set(net.minecraft.component.DataComponentTypes.ITEM_NAME,
                Text.translatable("two-dimensional-bedwars.item.shop"));
        player.getInventory().offerOrDrop(shopItem);
    }

    public boolean handleEnderChest(ServerPlayerEntity player) {
        ITeam team = getTeam(player);
        if (team instanceof BedWarsTeam bwTeam) {
            net.minecraft.inventory.SimpleInventory chest = bwTeam.getEnderChest();
            player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> net.minecraft.screen.GenericContainerScreenHandler.createGeneric9x3(syncId, inv,
                            chest),
                    Text.translatable("two-dimensional-bedwars.container.team_chest")));
            return true;
        }
        return false;
    }

    public ServerWorld getGameWorld() {
        return this.gameWorld;
    }

    public GamePlayingTask getGamePlayingTask() {
        return gamePlayingTask;
    }

    public boolean isSuddenDeathActive() {
        return gamePlayingTask != null && gamePlayingTask.isSuddenDeathActive();
    }
}
