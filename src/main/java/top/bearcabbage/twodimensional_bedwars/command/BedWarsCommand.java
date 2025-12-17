package top.bearcabbage.twodimensional_bedwars.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import top.bearcabbage.twodimensional_bedwars.game.ArenaManager;
import top.bearcabbage.twodimensional_bedwars.api.IArena;
import top.bearcabbage.twodimensional_bedwars.component.Arena;

public class BedWarsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bedwars")
                .then(CommandManager.literal("start")
                        .executes(context -> startGame(context)))
                .then(CommandManager.literal("join")
                        .executes(BedWarsCommand::joinGame))
                .then(CommandManager.literal("team")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1, 4))
                                .executes(BedWarsCommand::setTeam)))
                .then(CommandManager.literal("spectate")
                        .executes(BedWarsCommand::spectate))
                .then(CommandManager.literal("leave")
                        .executes(BedWarsCommand::leaveGame))
                .then(CommandManager.literal("stop")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(BedWarsCommand::stopGame))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(BedWarsCommand::reloadConfig)));
    }

    private static void broadcast(org.slf4j.Logger logger, net.minecraft.server.MinecraftServer server, Text message) {
        server.getPlayerManager().getPlayerList().forEach(p -> p.sendMessage(message, false));
    }

    private static int spectate(CommandContext<ServerCommandSource> context) {
        IArena arena = ArenaManager.getInstance().getArena();
        if (arena == null || arena.getStatus() != IArena.GameStatus.PLAYING) {
            context.getSource().sendError(Text.translatable("two-dimensional-bedwars.command.spectate.fail"));
            return 0;
        }

        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null)
            return 0;

        if (arena instanceof Arena impl) {
            if (impl.joinSpectator(player)) {
                context.getSource().sendMessage(Text.translatable("two-dimensional-bedwars.command.spectate.success"));
                return 1;
            } else {
                context.getSource().sendError(Text.translatable("two-dimensional-bedwars.command.spectate.fail"));
                return 0;
            }
        }
        return 0;
    }

    private static int joinGame(CommandContext<ServerCommandSource> context) {
        IArena arena = ArenaManager.getInstance().getArena();
        if (arena == null)
            return 0;
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null)
            return 0;

        if (arena instanceof Arena impl) {
            Arena.JoinResult result = impl.joinPlayer(player);
            switch (result) {
                case SUCCESS -> {
                    broadcast(null, context.getSource().getServer(),
                            Text.translatable("two-dimensional-bedwars.command.broadcast.join",
                                    player.getDisplayName()));
                    // Send success message to player too?
                    context.getSource().sendMessage(Text.translatable("two-dimensional-bedwars.command.join_success"));
                    return 1;
                }
                case GAME_RUNNING -> {
                    context.getSource()
                            .sendError(Text.translatable("two-dimensional-bedwars.command.join_fail_running"));
                    return 0;
                }
                case ALREADY_JOINED -> {
                    context.getSource()
                            .sendError(Text.translatable("two-dimensional-bedwars.command.join_fail_already"));
                    return 0;
                }
            }
        }

        // Fallback for interface only
        if (arena.addPlayer(player)) {
            broadcast(null, context.getSource().getServer(),
                    Text.translatable("two-dimensional-bedwars.command.broadcast.join", player.getDisplayName()));
            return 1;
        } else {
            context.getSource().sendError(Text.translatable("two-dimensional-bedwars.command.join_fail"));
            return 0;
        }
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        top.bearcabbage.twodimensional_bedwars.config.GameConfig.load();
        context.getSource().sendMessage(Text.translatable("two-dimensional-bedwars.command.reload_success"));
        return 1;
    }

    private static int setTeam(CommandContext<ServerCommandSource> context) {
        int id = IntegerArgumentType.getInteger(context, "id");
        // Map ID to Name
        String teamName;
        switch (id) {
            case 1:
                teamName = "Red";
                break; // Team 1
            case 2:
                teamName = "Blue";
                break; // Team 2
            case 3:
                teamName = "Green";
                break; // Team 3
            case 4:
                teamName = "Yellow";
                break; // Team 4
            default:
                teamName = "Red";
        }

        // Localized Team Name for Broadcast
        Text localizedTeamName = switch (teamName) {
            case "Red" -> Text.translatable("two-dimensional-bedwars.team.red");
            case "Blue" -> Text.translatable("two-dimensional-bedwars.team.blue");
            case "Green" -> Text.translatable("two-dimensional-bedwars.team.green");
            case "Yellow" -> Text.translatable("two-dimensional-bedwars.team.yellow");
            default -> Text.literal(teamName);
        };

        IArena arena = ArenaManager.getInstance().getArena();
        if (arena instanceof Arena impl) {
            try {
                if (impl.setPreferredTeam(context.getSource().getPlayer().getUuid(), teamName)) {
                    broadcast(null, context.getSource().getServer(),
                            Text.translatable("two-dimensional-bedwars.command.broadcast.team",
                                    context.getSource().getPlayer().getDisplayName(), localizedTeamName));
                    context.getSource().sendMessage(
                            Text.translatable("two-dimensional-bedwars.command.team_selected", localizedTeamName, id));
                } else {
                    context.getSource().sendError(Text.translatable("two-dimensional-bedwars.command.team_fail"));
                }
            } catch (Exception e) {
                // Not a player?
            }
        }
        return 1;
    }

    private static int startGame(CommandContext<ServerCommandSource> context) {
        // -1 indicates auto-detection of team count based on players
        boolean success = ArenaManager.getInstance().startGame(context.getSource().getWorld(), -1);
        if (success) {
            broadcast(null, context.getSource().getServer(),
                    Text.translatable("two-dimensional-bedwars.command.start_success"));
            return 1;
        } else {
            context.getSource().sendError(Text.translatable("two-dimensional-bedwars.command.start_fail"));
            return 0;
        }
    }

    private static int stopGame(CommandContext<ServerCommandSource> context) {
        boolean success = ArenaManager.getInstance().stopGame();
        if (success) {
            broadcast(null, context.getSource().getServer(),
                    Text.translatable("two-dimensional-bedwars.command.stop_success"));
            return 1;
        } else {
            context.getSource().sendError(Text.translatable("two-dimensional-bedwars.command.stop_fail"));
            return 0;
        }
    }

    private static final java.util.Map<java.util.UUID, Long> confirmLeave = new java.util.HashMap<>();

    private static int leaveGame(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null)
            return 0;

        java.util.UUID uuid = player.getUuid();
        IArena arena = ArenaManager.getInstance().getArena();

        // 1. Spectator or Not in Game? Leave immediately (TP to Spawn)
        boolean isParticipant = false;
        if (arena != null) {
            isParticipant = arena.getParticipantUUIDs().contains(uuid);
        }

        if (arena == null || !isParticipant) {
            if (arena instanceof Arena impl) {
                impl.leavePlayer(player);
            } else if (arena != null) {
                arena.removePlayer(player);
            } else {
                // Fallback if no arena info
                player.teleport(context.getSource().getServer().getOverworld(),
                        context.getSource().getServer().getOverworld().getSpawnPos().getX(),
                        context.getSource().getServer().getOverworld().getSpawnPos().getY(),
                        context.getSource().getServer().getOverworld().getSpawnPos().getZ(),
                        java.util.EnumSet.noneOf(net.minecraft.network.packet.s2c.play.PositionFlag.class),
                        0, 0, false);
                player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                player.clearStatusEffects();
            }
            context.getSource().sendMessage(Text.translatable("two-dimensional-bedwars.command.leave.spectator"));
            return 1;
        }

        // 2. Participant (In Playing or Waiting)
        if (confirmLeave.containsKey(uuid)) {
            long lastTime = confirmLeave.get(uuid);
            if (System.currentTimeMillis() - lastTime < 10000) { // 10 seconds
                // Confirmed
                confirmLeave.remove(uuid);
                if (arena instanceof Arena impl) {
                    impl.leavePlayer(player);
                } else {
                    arena.removePlayer(player);
                }
                context.getSource().sendMessage(Text.translatable("two-dimensional-bedwars.command.leave.success"));
                return 1;
            }
        }

        // First attempt or timeout
        confirmLeave.put(uuid, System.currentTimeMillis());
        context.getSource().sendMessage(Text.translatable("two-dimensional-bedwars.command.leave.confirm"));
        return 1;
    }
}
