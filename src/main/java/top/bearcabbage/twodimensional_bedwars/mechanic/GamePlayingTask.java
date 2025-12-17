package top.bearcabbage.twodimensional_bedwars.mechanic;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;
import top.bearcabbage.twodimensional_bedwars.component.Arena;
import top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam;
import top.bearcabbage.twodimensional_bedwars.component.OreGenerator;
import net.minecraft.util.Formatting;

public class GamePlayingTask {
    private final Arena arena;

    // Events
    public static class GameEvent {
        public Text name;
        public int timeSeconds;
        public java.util.function.Consumer<ServerWorld> action;

        public GameEvent(Text name, int timeSeconds, java.util.function.Consumer<ServerWorld> action) {
            this.name = name;
            this.timeSeconds = timeSeconds;
            this.action = action;
        }
    }

    private final List<GameEvent> events = new ArrayList<>();
    private int elapsedTime = 0; // In Seconds

    private final Map<UUID, Integer> respawnTimers = new HashMap<>();
    private int tickCounter = 0;

    public GamePlayingTask(Arena arena) {
        this.arena = arena;
        registerEvents();
    }

    private void registerEvents() {
        top.bearcabbage.twodimensional_bedwars.config.GameConfig config = top.bearcabbage.twodimensional_bedwars.config.GameConfig
                .getInstance();

        // Helper to apply level settings for specific types
        // types: list of identifiers to match e.g. ["Diamond", "Gold"]
        java.util.function.BiConsumer<Integer, List<String>> upgradeGenerators = (level, types) -> {
            config.publicGeneratorLevels.stream().filter(l -> l.level == level).findFirst().ifPresent(lvl -> {
                for (top.bearcabbage.twodimensional_bedwars.config.GameConfig.GeneratorSetting setting : lvl.settings) {
                    // Check if this setting is for one of the requested types
                    if (setting.type != null && types.stream().anyMatch(t -> t.equalsIgnoreCase(setting.type))) {
                        for (OreGenerator gen : arena.getPublicGenerators()) {
                            if (gen.getIdentifier().equalsIgnoreCase(setting.type)) {
                                gen.updateSettings(setting.amount, setting.delaySeconds, setting.limit);
                            }
                        }
                    }
                }
            });
        };

        // | 进程 | 描述 | 时间 | 倒计时 |
        // |钻石点II级|钻石点生成钻石速度加快|开局6分钟后|6：00|
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.diamond_ii"),
                config.eventSettings.diamondIISeconds, (world) -> {
                    upgradeGenerators.accept(2, List.of("Diamond", "Gold"));
                    arena.broadcastToGame(world.getServer(),
                            Text.translatable("two-dimensional-bedwars.event.diamond_ii").formatted(Formatting.AQUA));
                }));

        // |绿宝石点II级|绿宝石点生成绿宝石速度加快|开局12分钟后|6：00|
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.emerald_ii"),
                config.eventSettings.emeraldIISeconds, (world) -> {
                    upgradeGenerators.accept(2, List.of("Emerald", "Netherite"));
                    arena.broadcastToGame(world.getServer(),
                            Text.translatable("two-dimensional-bedwars.event.emerald_ii").formatted(Formatting.GREEN));
                }));

        // |钻石点III级|钻石点生成钻石速度为最快|开局18分钟后|6：00|
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.diamond_iii"),
                config.eventSettings.diamondIIISeconds, (world) -> {
                    upgradeGenerators.accept(3, List.of("Diamond", "Gold"));
                    arena.broadcastToGame(world.getServer(),
                            Text.translatable("two-dimensional-bedwars.event.diamond_iii").formatted(Formatting.AQUA));
                }));

        // |绿宝石点III级|绿宝石点生成绿宝石速度为最快|开局24分钟后|6：00|
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.emerald_iii"),
                config.eventSettings.emeraldIIISeconds, (world) -> {
                    upgradeGenerators.accept(3, List.of("Emerald", "Netherite"));
                    arena.broadcastToGame(world.getServer(),
                            Text.translatable("two-dimensional-bedwars.event.emerald_iii").formatted(Formatting.GREEN));
                }));

        // |床自毁|所有队伍未被摧毁的床自动摧毁|开局30分钟后|6：00|
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.bed_destruction"),
                config.eventSettings.bedDestructionSeconds, (world) -> {
                    for (ITeam team : arena.getTeams()) {
                        if (team instanceof BedWarsTeam bwTeam) {
                            // Destroy both beds if not destroyed
                            checkAndBreakBed(world, bwTeam, 1);
                            checkAndBreakBed(world, bwTeam, 2);
                        }
                    }
                    arena.broadcastToGame(world.getServer(),
                            Text.translatable("two-dimensional-bedwars.event.bed_destruction").formatted(Formatting.RED,
                                    Formatting.BOLD));
                }));

        // |末影龙出没|末影龙开始在地图中游荡并攻击玩家|开局36分钟后|6：00|
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.sudden_death"),
                config.eventSettings.suddenDeathSeconds, (world) -> {
                    arena.broadcastToGame(world.getServer(),
                            Text.translatable("two-dimensional-bedwars.event.sudden_death").formatted(Formatting.RED,
                                    Formatting.BOLD));
                    suddenDeathActive = true;
                }));

        // |游戏结束|游戏结束，所有存活的玩家不会判定为胜利。|开局42分钟后|6：00|
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.game_end"),
                config.eventSettings.gameEndSeconds, (world) -> {
                    arena.broadcastToGame(world.getServer(),
                            Text.translatable("two-dimensional-bedwars.event.game_over").formatted(Formatting.RED,
                                    Formatting.BOLD));
                    determineWinnerAtGameEnd(world);
                }));

        // Sort events by time just to be safe
        events.sort((e1, e2) -> Integer.compare(e1.timeSeconds, e2.timeSeconds));
    }

    // Better broadcast strategy: pass World to the event execution or store last
    // world
    // For now, let's keep the runnable simple and do logic in tick if needed,
    // OR just use the players list from arena which we can access.

    private boolean suddenDeathActive = false;
    private boolean gameWinning = false;

    public boolean isSuddenDeathActive() {
        return suddenDeathActive;
    }

    public void run(ServerWorld world) {
        // Run every tick (20Hz)

        // 1. Generators
        for (ITeam team : arena.getTeams()) {
            if (team instanceof BedWarsTeam bwTeam) {
                for (OreGenerator generator : bwTeam.getLiveGenerators()) {
                    generator.tick(world);
                }
            }
        }
        for (OreGenerator generator : arena.getPublicGenerators()) {
            generator.tick(world);
        }

        // 2. Bed Integrity Check (Every 10 Ticks)
        if (tickCounter % 10 == 0) {
            for (ITeam team : arena.getTeams()) {
                if (team instanceof BedWarsTeam bwTeam) {
                    checkBedIntegrity(world, bwTeam, 1);
                    checkBedIntegrity(world, bwTeam, 2);
                }
            }
        }

        // 3. One Second Logic
        tickCounter++;
        if (tickCounter < 20) {
            return;
        }
        tickCounter = 0;
        elapsedTime++;

        // 4. Check Events
        for (GameEvent event : events) {
            if (event.timeSeconds == elapsedTime) {
                event.action.accept(world);
                // Broadcast handled safely in action now or explicitly here if needed
            }
        }

        // 5. Respawn Timers
        Iterator<Map.Entry<UUID, Integer>> iterator = respawnTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int timeLeft = entry.getValue() - 1;
            if (timeLeft <= 0) {
                arena.respawnPlayer(entry.getKey(), world);
                iterator.remove();
            } else {
                entry.setValue(timeLeft);
            }
        }

        // 6. Sudden Death Creepers
        if (suddenDeathActive) {
            spawnSuddenDeathCreepers(world);
        }

        // 7. Win Condition (Every 1s)
        if (!gameWinning) {
            checkWinCondition(world);
        }
    }

    private void checkWinCondition(ServerWorld world) {
        if (arena.getTeams().size() < 2)
            return;

        List<ITeam> aliveTeams = new ArrayList<>();
        for (ITeam team : arena.getTeams()) {
            if (team instanceof BedWarsTeam bwTeam) {
                boolean bedsGone = bwTeam.isBedDestroyed(1) && bwTeam.isBedDestroyed(2);
                boolean hasAlivePlayers = false;
                for (top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer p : bwTeam.getPlayers()) {
                    if (p.getState() == 1) {
                        hasAlivePlayers = true;
                        break;
                    }
                }

                if (!bedsGone || hasAlivePlayers) {
                    aliveTeams.add(bwTeam);
                }
            }
        }

        if (aliveTeams.size() == 1) {
            gameWinning = true;
            BedWarsTeam winner = (BedWarsTeam) aliveTeams.get(0);
            triggerWin(world, winner);
        }
    }

    private void determineWinnerAtGameEnd(ServerWorld world) {
        if (arena.getTeams().size() < 2) {
            arena.stopGame();
            return;
        }

        List<BedWarsTeam> aliveTeams = new ArrayList<>();
        for (ITeam team : arena.getTeams()) {
            if (team instanceof BedWarsTeam bwTeam) {
                boolean bedsGone = bwTeam.isBedDestroyed(1) && bwTeam.isBedDestroyed(2);
                boolean hasAlivePlayers = false;
                for (top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer p : bwTeam.getPlayers()) {
                    if (p.getState() == 1) {
                        hasAlivePlayers = true;
                        break;
                    }
                }

                if (!bedsGone || hasAlivePlayers) {
                    aliveTeams.add(bwTeam);
                }
            }
        }

        // If only one team alive, they win
        if (aliveTeams.size() == 1) {
            gameWinning = true;
            triggerWin(world, aliveTeams.get(0));
            return;
        }

        // If no teams alive or less than 2 teams, end game without winner
        if (aliveTeams.size() < 2) {
            arena.broadcastToGame(world.getServer(),
                    Text.translatable("two-dimensional-bedwars.event.no_winner").formatted(Formatting.GRAY));
            arena.stopGame();
            return;
        }

        // Multiple teams alive - determine winner by tiebreakers
        BedWarsTeam winner = determineWinnerByTiebreakers(aliveTeams);
        
        if (winner != null) {
            gameWinning = true;
            triggerWin(world, winner);
        } else {
            // No winner - draw
            arena.broadcastToGame(world.getServer(),
                    Text.translatable("two-dimensional-bedwars.event.draw").formatted(Formatting.GRAY, Formatting.BOLD));
            arena.stopGame();
        }
    }

    private BedWarsTeam determineWinnerByTiebreakers(List<BedWarsTeam> teams) {
        // Defensive copy to avoid mutation of input parameter
        List<BedWarsTeam> teamsCopy = new ArrayList<>(teams);
        
        // Calculate statistics once for all teams to avoid redundant iterations
        Map<BedWarsTeam, TeamStats> teamStatsMap = new HashMap<>();
        for (BedWarsTeam team : teamsCopy) {
            teamStatsMap.put(team, calculateTeamStats(team));
        }
        
        // 1. Count alive players for each team
        int maxAlivePlayers = teamStatsMap.values().stream()
                .mapToInt(stats -> stats.alivePlayers)
                .max()
                .orElse(0);
        
        List<BedWarsTeam> topTeams = new ArrayList<>();
        for (BedWarsTeam team : teamsCopy) {
            if (teamStatsMap.get(team).alivePlayers == maxAlivePlayers) {
                topTeams.add(team);
            }
        }

        // If only one team has max alive players, they win
        if (topTeams.size() == 1) {
            return topTeams.get(0);
        }

        // 2. Compare by K/D ratio
        double maxKdRatio = topTeams.stream()
                .mapToDouble(team -> teamStatsMap.get(team).kdRatio)
                .max()
                .orElse(0.0);
        
        List<BedWarsTeam> topKdTeams = new ArrayList<>();
        for (BedWarsTeam team : topTeams) {
            if (Math.abs(teamStatsMap.get(team).kdRatio - maxKdRatio) < 0.0001) { // Compare doubles with epsilon
                topKdTeams.add(team);
            }
        }

        // If only one team has max K/D ratio, they win
        if (topKdTeams.size() == 1) {
            return topKdTeams.get(0);
        }

        // 3. Compare by total kills
        int maxKills = topKdTeams.stream()
                .mapToInt(team -> teamStatsMap.get(team).totalKills)
                .max()
                .orElse(0);
        
        List<BedWarsTeam> topKillTeams = new ArrayList<>();
        for (BedWarsTeam team : topKdTeams) {
            if (teamStatsMap.get(team).totalKills == maxKills) {
                topKillTeams.add(team);
            }
        }

        // If only one team has max kills, they win
        if (topKillTeams.size() == 1) {
            return topKillTeams.get(0);
        }

        // 4. All tiebreakers same - no winner (draw)
        return null;
    }
    
    private TeamStats calculateTeamStats(BedWarsTeam team) {
        int alivePlayers = 0;
        int totalKills = 0;
        int totalDeaths = 0;
        
        for (top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer p : team.getPlayers()) {
            if (p.getState() == 1) {
                alivePlayers++;
            }
            totalKills += p.getKills();
            totalDeaths += p.getDeaths();
        }
        
        // Calculate K/D ratio (avoid division by zero)
        double kdRatio = (totalDeaths == 0) ? totalKills : (double) totalKills / totalDeaths;
        
        return new TeamStats(alivePlayers, totalKills, totalDeaths, kdRatio);
    }
    
    private static class TeamStats {
        final int alivePlayers;
        final int totalKills;
        final int totalDeaths;
        final double kdRatio;
        
        TeamStats(int alivePlayers, int totalKills, int totalDeaths, double kdRatio) {
            this.alivePlayers = alivePlayers;
            this.totalKills = totalKills;
            this.totalDeaths = totalDeaths;
            this.kdRatio = kdRatio;
        }
    }

    private void triggerWin(ServerWorld world, BedWarsTeam winner) {
        arena.broadcastToGame(world.getServer(),
                Text.translatable("two-dimensional-bedwars.event.victory", winner.getName()).formatted(Formatting.GOLD,
                        Formatting.BOLD));

        Text title = Text.translatable("two-dimensional-bedwars.title.victory").formatted(Formatting.GOLD,
                Formatting.BOLD);
        Text subtitle = Text.translatable("two-dimensional-bedwars.subtitle.victory", winner.getName())
                .formatted(Formatting.YELLOW);

        for (UUID uuid : arena.getParticipantUUIDs()) {
            net.minecraft.server.network.ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(uuid);
            if (p != null) {
                ITeam pTeam = arena.getTeam(p);
                if (pTeam == winner) {
                    p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(title));
                    p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(subtitle));

                    // Celebration: Spectator & Teleport Up
                    p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                    p.teleport(world, p.getX(), 80.0, p.getZ(),
                            java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
                            p.getYaw(), p.getPitch(), false);
                } else {
                    Text defeatTitle = Text.translatable("two-dimensional-bedwars.title.defeat");
                    Text defeatSubtitle = Text.translatable("two-dimensional-bedwars.subtitle.defeat",
                            winner.getName());
                    p.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(defeatTitle));
                    p.networkHandler
                            .sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(defeatSubtitle));
                }
            }
        }

        spawnFireworks(world, winner.getSpawnPoint(1));
        spawnFireworks(world, winner.getSpawnPoint(2));

        // Schedule Stop in 10s
        events.add(new GameEvent(Text.translatable("two-dimensional-bedwars.event_name.game_end"), elapsedTime + 10,
                (w) -> {
                    arena.stopGame();
                }));
    }

    private void spawnFireworks(ServerWorld world, net.minecraft.util.math.BlockPos pos) {
        if (pos == null)
            return;
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.item.Items.FIREWORK_ROCKET);
        // Simple Flight 1
        net.minecraft.component.type.FireworksComponent fireworks = new net.minecraft.component.type.FireworksComponent(
                (byte) 1, List.of());
        stack.set(net.minecraft.component.DataComponentTypes.FIREWORKS, fireworks);

        net.minecraft.entity.projectile.FireworkRocketEntity rocket = new net.minecraft.entity.projectile.FireworkRocketEntity(
                world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stack);
        world.spawnEntity(rocket);
    }

    private void spawnSuddenDeathCreepers(ServerWorld world) {
        // Frequency increased by 100x as requested.
        for (int i = 0; i < 100; i++) {
            // Attempt to spawn in both arenas occasionally
            if (world.random.nextFloat() < 0.7f) {
                spawnCreeperInArena(world, 0, 0); // Arena 1 center
            }
            if (world.random.nextFloat() < 0.7f) {
                spawnCreeperInArena(world, 400, 0); // Arena 2 center
            }
        }
    }

    private void spawnCreeperInArena(ServerWorld world, int centerX, int centerZ) {
        // Random pos within 150 blocks of center (Diameter 300)
        double x = centerX + (world.random.nextDouble() - 0.5) * 300;
        double z = centerZ + (world.random.nextDouble() - 0.5) * 300;

        // Find surface?
        // Raycast down from Y=120
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);

        // Check bounds (Y should be reasonable, > 0)
        if (y < 0 || y > 200)
            return;

        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos((int) x, y, (int) z);
        if (!world.getBlockState(pos.down()).isAir()) {
            net.minecraft.entity.mob.CreeperEntity creeper = new net.minecraft.entity.mob.CreeperEntity(
                    net.minecraft.entity.EntityType.CREEPER, world);
            creeper.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360, 0);

            // Ignite
            creeper.ignite();

            // Charge using mechanic
            net.minecraft.entity.LightningEntity lightning = new net.minecraft.entity.LightningEntity(
                    net.minecraft.entity.EntityType.LIGHTNING_BOLT, world);
            if (lightning != null) {
                lightning.refreshPositionAndAngles(x, y, z, 0, 0);
                creeper.onStruckByLightning(world, lightning);
                lightning.discard();
            }
            creeper.ignite();

            world.spawnEntity(creeper);
        }
    }

    public GameEvent getNextEvent() {
        for (GameEvent event : events) {
            if (event.timeSeconds > elapsedTime) {
                return event;
            }
        }
        return null;
    }

    public int getElapsedTime() {
        return elapsedTime;
    }

    public void addRespawn(UUID uuid, int seconds) {
        respawnTimers.put(uuid, seconds);
    }

    private void checkBedIntegrity(ServerWorld world, BedWarsTeam team, int arenaId) {
        if (team.isBedDestroyed(arenaId))
            return;

        net.minecraft.util.math.BlockPos bedPos = team.getBedLocation(arenaId);
        if (bedPos == null)
            return;

        if (!(world.getBlockState(bedPos).getBlock() instanceof net.minecraft.block.BedBlock)) {
            team.setBedDestroyed(arenaId, true);
            Text arenaName = arenaId == 1 ? Text.translatable("two-dimensional-bedwars.arena.overworld")
                    : Text.translatable("two-dimensional-bedwars.arena.nether");
            world.getServer().getPlayerManager().getPlayerList()
                    .forEach(p -> p.sendMessage(
                            Text.translatable("two-dimensional-bedwars.event.bed_destroyed", team.getName(),
                                    arenaName)));
        }
    }

    private void checkAndBreakBed(ServerWorld world, BedWarsTeam team, int arenaId) {
        if (!team.isBedDestroyed(arenaId)) {
            team.setBedDestroyed(arenaId, true);
            // Physically break the bed
            net.minecraft.util.math.BlockPos bedPos = team.getBedLocation(arenaId);
            if (bedPos != null) {
                // Break block (drop=false, entity=null)
                // Only break if it is actually a bed to be polite, though event implies
                // destruction.
                net.minecraft.block.BlockState state = world.getBlockState(bedPos);
                if (state.getBlock() instanceof net.minecraft.block.BedBlock) {
                    world.breakBlock(bedPos, false);
                }
            }
        }
    }
}
