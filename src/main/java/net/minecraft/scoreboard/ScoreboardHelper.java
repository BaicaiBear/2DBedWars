package net.minecraft.scoreboard;

public class ScoreboardHelper {
    public static void resetScore(ServerScoreboard scoreboard, ScoreHolder holder, ScoreboardObjective objective) {
        scoreboard.resetScore(holder, objective);
    }
}
