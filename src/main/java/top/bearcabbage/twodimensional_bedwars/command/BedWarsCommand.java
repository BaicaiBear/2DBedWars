package top.bearcabbage.twodimensional_bedwars.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("start")
                .executes(context -> startGame(context, 2))
                .then(CommandManager.argument("teams", IntegerArgumentType.integer(2, 4))
                    .executes(context -> startGame(context, IntegerArgumentType.getInteger(context, "teams")))
                )
            )
            .then(CommandManager.literal("stop")
                .executes(BedWarsCommand::stopGame))
            .then(CommandManager.literal("team")
                .then(CommandManager.argument("color", StringArgumentType.word())
                    .executes(BedWarsCommand::setTeam)))
            .then(CommandManager.literal("reload")
                .executes(BedWarsCommand::reloadConfig))
        );
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        top.bearcabbage.twodimensional_bedwars.config.GameConfig.load();
        context.getSource().sendMessage(Text.literal("§aConfiguration reloaded!"));
        return 1;
    }

    private static int setTeam(CommandContext<ServerCommandSource> context) {
        String teamName = StringArgumentType.getString(context, "color");
        IArena arena = ArenaManager.getInstance().getArena();
        if (arena instanceof Arena impl) {
             try {
                if (impl.setPreferredTeam(context.getSource().getPlayer().getUuid(), teamName)) {
                     context.getSource().sendMessage(Text.literal("Team preference set to " + teamName));
                } else {
                     context.getSource().sendError(Text.literal("Cannot set team now (Game running?)"));
                }
             } catch (Exception e) {
                 // Not a player?
             }
        }
        return 1;
    }

    private static int startGame(CommandContext<ServerCommandSource> context, int teamCount) {
        IArena arena = ArenaManager.getInstance().getArena();
        if (arena == null) {
            context.getSource().sendError(Text.literal("Arena not initialized!"));
            return 0;
        }

        if (arena.getStatus() != IArena.GameStatus.WAITING) {
            context.getSource().sendError(Text.literal("Game is already running or not in waiting state!"));
            return 0;
        }

        if (arena instanceof Arena) {
            ((Arena) arena).startGame(context.getSource().getWorld(), teamCount);
            context.getSource().sendMessage(Text.literal("BedWars game started with " + teamCount + " teams!"));
        } else {
             context.getSource().sendError(Text.literal("Unknown Arena implementation!"));
             return 0;
        }

        return 1;
    }

    private static int stopGame(CommandContext<ServerCommandSource> context) {
        IArena arena = ArenaManager.getInstance().getArena();
        if (arena == null) {
             context.getSource().sendError(Text.literal("Arena not initialized!"));
             return 0;
        }
        
        if (arena instanceof Arena) {
            ((Arena) arena).stopGame();
            context.getSource().sendMessage(Text.literal("BedWars game stopped!"));
        }
        
        return 1;
    }
}
