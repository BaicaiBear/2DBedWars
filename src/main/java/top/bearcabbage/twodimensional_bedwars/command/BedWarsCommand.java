package top.bearcabbage.twodimensional_bedwars.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import top.bearcabbage.twodimensional_bedwars.game.ArenaManager;
import top.bearcabbage.twodimensional_bedwars.api.IArena;
import top.bearcabbage.twodimensional_bedwars.component.Arena;

public class BedWarsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bedwars")
                .then(CommandManager.literal("start")
                        .executes(context -> startGame(context)))
                .then(CommandManager.literal("stop")
                        .executes(BedWarsCommand::stopGame))
                .then(CommandManager.literal("team")
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1, 4))
                                .executes(BedWarsCommand::setTeam)))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(BedWarsCommand::reloadConfig)));
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        top.bearcabbage.twodimensional_bedwars.config.GameConfig.load();
        context.getSource().sendMessage(Text.literal("§aConfiguration reloaded!"));
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

        IArena arena = ArenaManager.getInstance().getArena();
        if (arena instanceof Arena impl) {
            try {
                if (impl.setPreferredTeam(context.getSource().getPlayer().getUuid(), teamName)) {
                    context.getSource().sendMessage(Text.literal("Selected Team " + id + " (" + teamName + ")"));
                } else {
                    context.getSource().sendError(Text.literal("Cannot set team now (Game running?)"));
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
            context.getSource().sendMessage(Text.literal("BedWars game starting..."));
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Failed to start game! (Already running or not waiting)"));
            return 0;
        }
    }

    private static int stopGame(CommandContext<ServerCommandSource> context) {
        boolean success = ArenaManager.getInstance().stopGame();
        if (success) {
            context.getSource().sendMessage(Text.literal("BedWars game stopped!"));
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Failed to stop game! (No arena?)"));
            return 0;
        }
    }
}
