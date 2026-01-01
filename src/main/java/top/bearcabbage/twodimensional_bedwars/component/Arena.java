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
                } else {
                    // If skipping restore (e.g. on disconnect), clear inventory to prevent item leaks
                    // The backup will restore it on next join.
                    player.getInventory().clear();
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
            } else {
                player.getInventory().clear();
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

            // NPC LookAt Logic (Every 10 ticks)
            if (world.getTime() % 10 == 0 && this.gameWorld != null) {
                // Villagers
                for (net.minecraft.entity.passive.VillagerEntity entity : this.gameWorld.getEntitiesByType(
                        net.minecraft.entity.EntityType.VILLAGER, e -> e.getCommandTags().contains("BedWarsShop"))) {
                    lookAtNearestPlayer(entity);
                }
                // Piglins
                for (net.minecraft.entity.mob.PiglinEntity entity : this.gameWorld.getEntitiesByType(
                        net.minecraft.entity.EntityType.PIGLIN, e -> e.getCommandTags().contains("BedWarsShop"))) {
                    lookAtNearestPlayer(entity);
                }
            }

            // Food & Saturation
            if (this.gameWorld != null) {
                for (ServerPlayerEntity p : this.gameWorld.getPlayers()) {
                    p.getHungerManager().setFoodLevel(20);
                    p.getHungerManager().setSaturationLevel(20.0f);
                }
            }

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

            BlockState feet = world.getBlockState(player.getBlockPos());
            BlockState head = world.getBlockState(player.getBlockPos().up());

            boolean inPortal = feet.getBlock() == net.minecraft.block.Blocks.NETHER_PORTAL ||
                    head.getBlock() == net.minecraft.block.Blocks.NETHER_PORTAL;

            if (inPortal) {
                // Determine portal logic based on cooldown
                // If player has cooldown (just teleported), prevent vanilla logic but SKIP our
                // logic
                // Vanilla logic requires max portal time, setting cooldown resets that timer,
                // blocking it effectively.
                if (player.getPortalCooldown() > 0) {
                    // We just ensure vanilla doesn't trigger by keeping the timer incomplete?
                    // Actually, if we do nothing, cooldown ticks down. Once 0, we can TP again.
                    // To BLOCK vanilla, we rely on the fact that we teleport INSTANTLY when
                    // cooldown is 0.
                    // If cooldown > 0, we just wait.
                    continue;
                }

                // Check for Center Portal (based on proximity to center)
                GameConfig.MapPoint c1 = config.arenaRestoreConfig.arena1Bounds.center;
                GameConfig.MapPoint c2 = config.arenaRestoreConfig.arena2Bounds.center;

                double portalY1 = c1.y + config.centerPortalOffsetY;
                double portalY2 = c2.y + config.centerPortalOffsetY;

                // Allow a small radius around center for the portal
                double d1 = player.getPos().squaredDistanceTo(c1.x, portalY1, c1.z);
                double d2 = player.getPos().squaredDistanceTo(c2.x, portalY2, c2.z);

                // Radius of 5 blocks should cover the portal frame
                if (d1 < 25 || d2 < 25) {
                    // This IS a Center Portal
                    BlockPos targetPos;
                    if (d1 < d2) {
                        // In Arena 1 -> Go to Arena 2 Center
                        targetPos = new BlockPos(c2.x, (int) portalY2, c2.z);
                    } else {
                        // In Arena 2 -> Go to Arena 1 Center
                        targetPos = new BlockPos(c1.x, (int) portalY1, c1.z);
                    }

                    // Instant Teleport, No Bed Check, +1 Y Offset
                    player.teleport(world, targetPos.getX() + 0.5, targetPos.getY() + 1.0,
                            targetPos.getZ() + 0.5,
                            java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
                            player.getYaw(), player.getPitch(), false);
                    player.playSound(net.minecraft.sound.SoundEvents.BLOCK_PORTAL_TRAVEL, 0.5f, 1.0f);
                    // Set Cooldown 5s
                    player.setPortalCooldown(100);
                    return;
                }

                // Standard Base Team Portal Logic
                ITeam team = getTeam(player);
                if (team instanceof BedWarsTeam bwTeam) {
                    // Determine current arena
                    int currentArena = 1;
                    if (d2 < d1)
                        currentArena = 2;

                    int targetArena = (currentArena == 1) ? 2 : 1;

                    if (bwTeam.isBedDestroyed(targetArena)) {
                        player.sendMessage(Text.translatable("two-dimensional-bedwars.arena.teleport_fail_bed"), true);
                        // Prevent Spam
                        player.setPortalCooldown(100);
                    } else {
                        teleportToTeamSpawn(player, targetArena);
                        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        // Set Cooldown 5s
                        player.setPortalCooldown(100);
                    }
                }
            }
        }
    }

    public void setupCenterPortals(ServerWorld world) {
        // Build 5x5 Crying Obsidian Frames at Centers
        // Frame: 5x5. Inner: 3x3.
        // Orientation: Along X axis (facing Z)

        GameConfig.MapPoint c1 = config.arenaRestoreConfig.arena1Bounds.center;
        GameConfig.MapPoint c2 = config.arenaRestoreConfig.arena2Bounds.center;

        buildPortalFrame(world, new BlockPos(c1.x, c1.y + config.centerPortalOffsetY, c1.z));
        buildPortalFrame(world, new BlockPos(c2.x, c2.y + config.centerPortalOffsetY, c2.z));
    }

    private void buildPortalFrame(ServerWorld world, BlockPos center) {
        // Center of the base. Relative to center, let's build axis X.
        // x-2 to x+2 (5 blocks wide). y to y+5 (6 blocks high).
        // Crying Obsidian Frame.

        BlockState frame = net.minecraft.block.Blocks.CRYING_OBSIDIAN.getDefaultState();

        for (int z = -2; z <= 2; z++) {
            for (int h = 0; h <= 5; h++) {
                BlockPos pos = center.add(0, h, z);
                boolean isEdge = (z == -2 || z == 2 || h == 0 || h == 5);
                if (isEdge) {
                    world.setBlockState(pos, frame);
                    // Protect it?
                    getData().recordBlastProof(pos); // Actually map blocks are protected anyway, but ensure it.
                } else {
                    world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    public void setCenterPortalState(ServerWorld world, boolean open) {
        BlockState inner = open
                ? net.minecraft.block.Blocks.NETHER_PORTAL.getDefaultState()
                        .with(net.minecraft.block.NetherPortalBlock.AXIS, net.minecraft.util.math.Direction.Axis.Z)
                : net.minecraft.block.Blocks.AIR.getDefaultState();

        GameConfig.MapPoint c1 = config.arenaRestoreConfig.arena1Bounds.center;
        GameConfig.MapPoint c2 = config.arenaRestoreConfig.arena2Bounds.center;

        int y1 = c1.y + config.centerPortalOffsetY;
        int y2 = c2.y + config.centerPortalOffsetY;

        updatePortalState(world, new BlockPos(c1.x, y1, c1.z), inner);
        updatePortalState(world, new BlockPos(c2.x, y2, c2.z), inner);

        if (open) {
            broadcastToGame(world.getServer(),
                    Text.translatable("two-dimensional-bedwars.event.portal_open").formatted(Formatting.LIGHT_PURPLE));
            // Play Sound
            world.playSound(null, c1.x, y1, c1.z, net.minecraft.sound.SoundEvents.BLOCK_END_PORTAL_SPAWN,
                    net.minecraft.sound.SoundCategory.BLOCKS, 100f, 1f);
            world.playSound(null, c2.x, y2, c2.z, net.minecraft.sound.SoundEvents.BLOCK_END_PORTAL_SPAWN,
                    net.minecraft.sound.SoundCategory.BLOCKS, 100f, 1f);
        } else {
            broadcastToGame(world.getServer(),
                    Text.translatable("two-dimensional-bedwars.event.portal_close").formatted(Formatting.RED));
        }
    }

    private void updatePortalState(ServerWorld world, BlockPos center, BlockState state) {
        for (int z = -1; z <= 1; z++) {
            for (int h = 1; h <= 4; h++) {
                world.setBlockState(center.add(0, h, z), state);
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
            setupCenterPortals(gameWorld); // Place portal frames after restore
            this.mapRestoreComplete = true; // Signals tick -> beginMatch -> initialize
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
            if (player != null && team instanceof BedWarsTeam) {
                // Create BedWarsPlayer
                BedWarsPlayer bwPlayer = new BedWarsPlayer(uuid, team);
                bwPlayer.setState(1); // Alive
                team.addPlayer(bwPlayer); // Add to Team storage

                // Save Backup
                bwPlayer.saveLobbyState(player);

                player.getInventory().clear();

                teleportToTeamSpawn(player, 1); // Start in Arena 1 with offset

                player.changeGameMode(GameMode.SURVIVAL);
                player.changeGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.getHungerManager().setFoodLevel(20);

                // Apply Tools & Give Shop Item
                bwPlayer.applyTools(player);
                // Shop item removed (Line 612 cleared)
            }
        }

        preferredTeams.clear(); // One-time preference usage
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
        // Natural Regeneration Disabled
        world.getGameRules().get(net.minecraft.world.GameRules.NATURAL_REGENERATION).set(false, world.getServer());

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

    /**
     * Attempts to break a bed/anchor at the given position.
     * 
     * @return true if the block was identified as a bed/anchor and processed
     *         (whether broken or denied). false if ignored.
     */
    public boolean attemptBreakBed(ServerPlayerEntity player, BlockPos pos) {
        BlockState state = player.getWorld().getBlockState(pos);
        boolean isBed = state.getBlock() instanceof BedBlock;
        boolean isAnchor = state.getBlock() == net.minecraft.block.Blocks.RESPAWN_ANCHOR;

        if (!isBed && !isAnchor)
            return false;

        ITeam team = getTeam(player);
        if (!(team instanceof BedWarsTeam playerTeam)) {
            return false;
        }

        for (ITeam checkTeam : teams) {
            if (checkTeam instanceof BedWarsTeam checkBwTeam) {
                boolean isTarget = false;
                int targetArena = 0;

                // Check Arena 1
                BlockPos bed1 = checkBwTeam.getBedLocation(1);
                if (bed1 != null && (bed1.equals(pos) || bed1.getSquaredDistance(pos) < 4)) {
                    isTarget = true;
                    targetArena = 1;
                }

                // Check Arena 2
                BlockPos bed2 = checkBwTeam.getBedLocation(2);
                if (!isTarget && bed2 != null && (bed2.equals(pos) || bed2.getSquaredDistance(pos) < 4)) {
                    isTarget = true;
                    targetArena = 2;
                }

                if (isTarget) {
                    // 1. Own Bed Check
                    if (checkTeam == playerTeam) {
                        player.sendMessage(Text.translatable("two-dimensional-bedwars.arena.break_own_bed"), true);
                        return true;
                    }

                    // 2. Anchor Tool Check
                    if (isAnchor) {
                        net.minecraft.item.Item item = player.getMainHandStack().getItem();
                        boolean canBreak = (item == net.minecraft.item.Items.IRON_PICKAXE ||
                                item == net.minecraft.item.Items.DIAMOND_PICKAXE);
                        if (!canBreak) {
                            player.sendMessage(Text.translatable("two-dimensional-bedwars.block.anchor_protection"),
                                    true);
                            return true;
                        }
                    }

                    // 3. Process Break
                    if (isBed) {
                        net.minecraft.util.math.Direction direction = net.minecraft.block.BedBlock
                                .getOppositePartDirection(state);
                        BlockPos otherPos = pos.offset(direction);
                        if (player.getWorld().getBlockState(otherPos)
                                .getBlock() instanceof net.minecraft.block.BedBlock) {
                            player.getWorld().breakBlock(otherPos, false);
                        }
                    }
                    player.getWorld().breakBlock(pos, false);

                    processBedBreak(player, playerTeam, checkBwTeam, targetArena);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean processBedBreak(ServerPlayerEntity breaker, ITeam breakerTeam, BedWarsTeam bedOwnerTeam,
            int arenaId) {
        // Redundant check removed logic - but kept method for Event Broadcast
        if (!bedOwnerTeam.isBedDestroyed(arenaId)) {
            bedOwnerTeam.setBedDestroyed(arenaId, true);

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
            Text teamName = Text.translatable("two-dimensional-bedwars.team." + bedOwnerTeam.getName().toLowerCase())
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

        int otherArena = (currentArena == 1) ? 2 : 1;
        int targetArena = currentArena;
        boolean eliminated = false;

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

            // Respawn Logic:
            // 1. Try Other Arena Bed
            if (!bwTeam.isBedDestroyed(otherArena)) {
                targetArena = otherArena;
            }
            // 2. Try Same Arena Bed
            else if (!bwTeam.isBedDestroyed(currentArena)) {
                targetArena = currentArena;
            }
            // 3. Elimination
            else {
                eliminated = true;
            }

            if (eliminated) {
                // Elimination
                player.sendMessage(Text.translatable("two-dimensional-bedwars.arena.eliminated"));
                player.changeGameMode(GameMode.SPECTATOR);
                if (bwPlayer != null)
                    bwPlayer.setState(0); // Spectator
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
            if (team instanceof BedWarsTeam) {
                int targetArena = respawnTargets.getOrDefault(uuid, 1);

                teleportToTeamSpawn(player, targetArena);

                player.changeGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());

                BedWarsPlayer bwPlayer = team.getPlayer(uuid);
                if (bwPlayer != null) {
                    bwPlayer.applyTools(player);
                }
                // Shop item removed
                respawnTargets.remove(uuid);
            }
        }
    }

    private void teleportToTeamSpawn(ServerPlayerEntity player, int arenaId) {
        ITeam team = playerTeamMap.get(player.getUuid());
        if (team instanceof BedWarsTeam bwTeam) {
            BlockPos spawn = bwTeam.getSpawnPoint(arenaId);
            if (spawn != null) {
                double centerX = (arenaId == 2) ? 400.0 : 0.0;
                double centerZ = 0.0;

                double dirX = centerX - spawn.getX();
                double dirZ = centerZ - spawn.getZ();
                double dist = Math.sqrt(dirX * dirX + dirZ * dirZ);

                double finalX = spawn.getX() + 0.5;
                double finalZ = spawn.getZ() + 0.5;

                // Offset 3 blocks towards center to avoid spawning in bed shell
                if (dist > 1.0) {
                    finalX += (dirX / dist) * 3.0;
                    finalZ += (dirZ / dist) * 3.0;
                }

                ServerWorld world = (gameWorld != null) ? gameWorld : (ServerWorld) player.getWorld();
                player.teleport(world, finalX, spawn.getY(), finalZ,
                        java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
                        player.getYaw(), player.getPitch(),
                        false);
            }
        }
    }

    // Removed Give Shop Item

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

    private void lookAtNearestPlayer(net.minecraft.entity.Entity entity) {
        net.minecraft.entity.player.PlayerEntity nearest = entity.getWorld().getClosestPlayer(entity, 10);
        if (nearest != null) {
            entity.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES,
                    nearest.getEyePos()); // Look at EYES
        }
    }

    public boolean handleBlockBreak(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof BedBlock) {
            boolean result = attemptBreakBed(player, pos);
            return false;
        }

        if (isBlockPlayerPlaced(pos)) {
            placedBlocks.remove(pos);
            return true;
        }
        return false;
    }

}
