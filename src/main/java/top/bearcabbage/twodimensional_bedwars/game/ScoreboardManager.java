package top.bearcabbage.twodimensional_bedwars.game;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;
import top.bearcabbage.twodimensional_bedwars.component.Arena;

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
                Text.translatable("two-dimensional-bedwars.scoreboard.title"),
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
            ((top.bearcabbage.twodimensional_bedwars.mixin.ScoreboardInvoker)server.getScoreboard()).invokeResetScore(net.minecraft.scoreboard.ScoreHolder.fromName(token), objective);
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
            top.bearcabbage.twodimensional_bedwars.mechanic.GamePlayingTask.GameEvent nextEvent = task.getNextEvent();
            Text eventName = Text.translatable("two-dimensional-bedwars.event_name.waiting");
            int time = 0;

            if (nextEvent != null) {
                eventName = nextEvent.name;
                time = nextEvent.timeSeconds - task.getElapsedTime();
            } else {
                eventName = Text.translatable("two-dimensional-bedwars.event_name.ended");
                time = 0;
            }

            String timeStr = String.format("%02d:%02d", time / 60, time % 60);
            updateLine(timerScore,
                    Text.empty().append(Text.literal("§e")).append(eventName).append(Text.literal(": §a" + timeStr)));
        } else {
            updateLine(timerScore, Text.translatable("two-dimensional-bedwars.scoreboard.waiting_ellipsis"));
        }

        // Top Spacer
        updateLine(topSpacer, Text.empty());

        // Teams (at 3+teamCount down to 4)
        int currentScore = 3 + teamCount;
        // int teamIndex = 1; // Unused

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
                    // Only count if physically present and NOT a spectator (respawning players are
                    // spectators)
                    if (spe != null && !spe.isSpectator()) {
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

            // Determine Color and Localized Name
            net.minecraft.util.Formatting color = net.minecraft.util.Formatting.WHITE;
            String teamKey = "two-dimensional-bedwars.team.white";

            if (bwTeam.getName().equalsIgnoreCase("Red")) {
                color = net.minecraft.util.Formatting.RED;
                teamKey = "two-dimensional-bedwars.team.red";
            } else if (bwTeam.getName().equalsIgnoreCase("Blue")) {
                color = net.minecraft.util.Formatting.BLUE;
                teamKey = "two-dimensional-bedwars.team.blue";
            } else if (bwTeam.getName().equalsIgnoreCase("Green")) {
                color = net.minecraft.util.Formatting.GREEN;
                teamKey = "two-dimensional-bedwars.team.green";
            } else if (bwTeam.getName().equalsIgnoreCase("Yellow")) {
                color = net.minecraft.util.Formatting.YELLOW;
                teamKey = "two-dimensional-bedwars.team.yellow";
            }

            // Alias: TEAM 1, TEAM 2...
            // Format: [Color] [TeamName]
            Text alias = Text.translatable(teamKey).formatted(color);

            // Format: TEAM1 [Beds] P:P1/P2 K/D:K/D
            // String line = String.format("%s%-6s \u00A7f[\u00A7l%s%s\u00A7r\u00A7f]
            // \u00A77P:%d/%d \u00A77K/D:%d/%d", colorCode, alias, bed1, bed2, p1, p2,
            // kills, deaths);
            Text line = Text.empty()
                    .append(alias)
                    .append(" ")
                    .append(Text.literal("\u00A7f[\u00A7l"))
                    .append(Text.literal(bed1))
                    .append(Text.literal(bed2))
                    .append(Text.literal("\u00A7r\u00A7f] \u00A77P:"))
                    .append(Text.literal(p1 + "/" + p2))
                    .append(Text.literal(" \u00A77K/D:"))
                    .append(Text.literal(kills + "/" + deaths));

            updateLine(currentScore, line);
            currentScore--;
        }

        // Bottom Spacers
        updateLine(3, Text.empty());
        updateLine(2, Text.empty());
        // Footer
        updateLine(1, Text.translatable("two-dimensional-bedwars.scoreboard.footer"));
    }

    private void updateLine(int score, Text text) {
        ServerScoreboard scoreboard = server.getScoreboard();
        String teamName = lineTeams.get(score);
        if (teamName != null) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.setPrefix(text);
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
