package top.bearcabbage.twodimensional_bedwars.mixin;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Scoreboard.class)
public interface ScoreboardInvoker {
    @Invoker("resetScore")
    void invokeResetScore(ScoreHolder holder, ScoreboardObjective objective);
}
