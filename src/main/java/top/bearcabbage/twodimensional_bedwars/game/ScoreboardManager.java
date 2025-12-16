package top.bearcabbage.twodimensional_bedwars.game;

import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.Map;
import top.bearcabbage.twodimensional_bedwars.component.Arena;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;

public class ScoreboardManager {
    private static final String OBJECTIVE_NAME = "bedwars_game";
    private final Arena arena;
    private final MinecraftServer server;
    private ScoreboardObjective objective;

    // Map of Line Index -> Team Name (for reusing teams)
    private final Map<Integer, String> lineTeams = new HashMap<>();
    // Unique hidden tokens for score holders
    private static final String[] LINE_TOKENS = {
            "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74", "\u00A75", "\u00A76", "\u00A77", "\u00A78",
            "\u00A79", "\u00A7a", "\u00A7b", "\u00A7c", "\u00A7d", "\u00A7e"
    };

    public ScoreboardManager(Arena arena, MinecraftServer server) {
        this.arena = arena;
        this.server = server;
    }

    public void setup() {
        ServerScoreboard scoreboard = server.getScoreboard();

        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);

        // Clear old objective if exists
        ScoreboardObjective old = scoreboard.getNullableObjective(OBJECTIVE_NAME);
        if (old != null) {
            scoreboard.removeObjective(old);
        }

        // Create new Objective
        this.objective = scoreboard.addObjective(
                OBJECTIVE_NAME,
                ScoreboardCriterion.DUMMY,
                Text.literal("\u00A7e\u00A7l2D BEDWARS"),
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                null);

        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, this.objective);

        initializeLines();
    }

    // Create teams for each line to allow flicker-free updates via Prefix/Suffix
    private void initializeLines() {
        ServerScoreboard scoreboard = server.getScoreboard();

        // We'll prepare lines 9 down to 1
        for (int i = 0; i < 9; i++) {
            String teamName = "bw_line_" + i;
            String token = LINE_TOKENS[i];

            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.addTeam(teamName);
            }

            // Add the token to the team
            scoreboard.addScoreHolderToTeam(token, team);

            // Set the score for this token to secure its position
            net.minecraft.scoreboard.ScoreAccess score = objective.getScoreboard()
                    .getOrCreateScore(net.minecraft.scoreboard.ScoreHolder.fromName(token), objective);
            score.setScore(i + 1);

            // Hide Numbers (1.21+) - TODO: Find correct API for BlankFormat
            // score.setNumberFormat(net.minecraft.scoreboard.number.BlankFormat.INSTANCE);

            lineTeams.put(i + 1, teamName);
        }
    }

    public void update() {
        if (objective == null)
            return;

        // 1. Identify Active Teams
        java.util.List<top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam> activeTeams = new java.util.ArrayList<>();
        for (ITeam t : arena.getTeams()) {
            if (t instanceof top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam bwTeam) {
                activeTeams.add(bwTeam);
            }
        }
        int teamCount = activeTeams.size();

        // 2. Define Positions
        // Footer=1, Spacer=2, Spacer=3
        // Teams=4 to (3+teamCount)
        // Spacer=(4+teamCount)
        // Timer=(5+teamCount)
        int topSpacer = 4 + teamCount;
        int timerScore = 5 + teamCount;

        // 3. Clear Excess Lines (timerScore + 1 up to 15)
        for (int i = timerScore + 1; i <= 15; i++) {
            if (i < 1)
                continue;
            String token = LINE_TOKENS[i - 1];
            net.minecraft.scoreboard.ScoreboardHelper.resetScore(server.getScoreboard(),
                    net.minecraft.scoreboard.ScoreHolder.fromName(token), objective);
        }

        // 4. Ensure Active Lines Have Scores
        for (int i = 1; i <= timerScore; i++) {
            String token = LINE_TOKENS[i - 1];
            net.minecraft.scoreboard.ScoreAccess score = objective.getScoreboard()
                    .getOrCreateScore(net.minecraft.scoreboard.ScoreHolder.fromName(token), objective);
            score.setScore(i);
        }

        // 5. Render Content

        // Timer (at timerScore)
        top.bearcabbage.twodimensional_bedwars.mechanic.GamePlayingTask task = arena.getGamePlayingTask();
        if (task != null) {
            String eventName = "Diamond II";
            int time = task.getBedsDestroyCountdown();

            if (time < 0) {
                time = task.getDragonSpawnCountdown();
                eventName = "Dragon Spawn";
                if (time < 0) {
                    time = task.getGameEndCountdown();
                    eventName = "Game End";
                }
            }

            String timeStr = (time >= 0) ? String.format("%02d:%02d", time / 60, time % 60) : "--:--";
            updateLine(timerScore, "\u00A7e" + eventName + ": \u00A7a" + timeStr);
        } else {
            updateLine(timerScore, "Waiting...");
        }

        // Top Spacer
        updateLine(topSpacer, "");

        // Teams (at 3+teamCount down to 4)
        int currentScore = 3 + teamCount;
        int teamIndex = 1;

        for (top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam bwTeam : activeTeams) {
            int kills = 0;
            int deaths = 0;
            int p1 = 0;
            int p2 = 0;
            for (top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer p : bwTeam.getPlayers()) {
                kills += p.getKills();
                deaths += p.getDeaths();
                if (p.getState() == 1) { // 1 = Alive
                    // Determine Arena by checking player position
                    net.minecraft.server.network.ServerPlayerEntity spe = server.getPlayerManager()
                            .getPlayer(p.getUuid());
                    if (spe != null) {
                        if (spe.getX() > 200) {
                            p2++;
                        } else {
                            p1++;
                        }
                    }
                }
            }

            // Bed Status (Arena 1 | Arena 2)
            String bed1 = bwTeam.isBedDestroyed(1) ? "\u00A7c\u2718" : "\u00A7a\u2714"; // X or Check
            String bed2 = bwTeam.isBedDestroyed(2) ? "\u00A7c\u2718" : "\u00A7a\u2714";

            // Determine Color
            String colorCode = "\u00A7f";
            if (bwTeam.getName().equalsIgnoreCase("Red"))
                colorCode = "\u00A7c";
            else if (bwTeam.getName().equalsIgnoreCase("Blue"))
                colorCode = "\u00A79";
            else if (bwTeam.getName().equalsIgnoreCase("Green"))
                colorCode = "\u00A7a";
            else if (bwTeam.getName().equalsIgnoreCase("Yellow"))
                colorCode = "\u00A7e";

            // Alias: TEAM 1, TEAM 2...
            String alias = "TEAM " + teamIndex++;

            // Format: TEAM1 [Beds] P:P1/P2 K/D:K/D
            String line = String.format("%s%-6s \u00A7f[\u00A7l%s%s\u00A7r\u00A7f] \u00A77P:%d/%d \u00A77K/D:%d/%d",
                    colorCode, alias, bed1, bed2, p1, p2, kills, deaths);

            updateLine(currentScore, line);
            currentScore--;
        }

        // Bottom Spacers
        updateLine(3, "");
        updateLine(2, "");
        // Footer
        updateLine(1, "\u00A7eMirrorTree");
    }

    private void updateLine(int score, String text) {
        ServerScoreboard scoreboard = server.getScoreboard();
        String teamName = lineTeams.get(score);
        if (teamName != null) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.setPrefix(Text.literal(text));
                // Suffix not strictly needed if text fits in prefix (1.20.4+ supports long
                // prefixes)
            }
        }
    }

    public void cleanup() {
        ServerScoreboard scoreboard = server.getScoreboard();
        if (objective != null) {
            scoreboard.removeObjective(objective);
            objective = null;
        }
        // Cleanup teams
        for (String teamName : lineTeams.values()) {
            Team t = scoreboard.getTeam(teamName);
            if (t != null) {
                scoreboard.removeTeam(t);
            }
        }
    }
}
