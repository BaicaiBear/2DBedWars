package top.bearcabbage.twodimensional_bedwars.mechanic;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import java.util.Optional;
import top.bearcabbage.twodimensional_bedwars.game.ArenaManager;
import top.bearcabbage.twodimensional_bedwars.component.Arena;
import top.bearcabbage.twodimensional_bedwars.api.IArena.GameStatus;

public class ArenaExplosionBehavior extends ExplosionBehavior {
    private final ExplosionBehavior fallback;

    public ArenaExplosionBehavior(ExplosionBehavior fallback) {
        this.fallback = fallback;
    }

    @Override
    public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
        // Check standard behavior first (fallback)
        if (fallback != null) {
            if (!fallback.canDestroyBlock(explosion, world, pos, state, power)) {
                return false;
            }
        } else {
            // Fallback Logic: Standard Minecraft Check (roughly)
            // We allow destruction if fallback is null (standard behavior)
            // But we still apply our restrictions below.
        }

        // Custom Arena Logic
        if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
            if (gameArena.getStatus() == GameStatus.PLAYING) {

                // 0. Sudden Death: Allow Creeper Destruction of EVERYTHING
                if (gameArena.isSuddenDeathActive()) {
                    net.minecraft.entity.Entity source = explosion.getEntity(); // Using getEntity() mapping if
                                                                                // available or field access?
                    // Explosion.class usually has generic `Entity getEntity()` or `getCauser()`.
                    // Let's use `explosion.getEntity()`. If fail, we fix.
                    if (source instanceof net.minecraft.entity.mob.CreeperEntity) {
                        return true;
                    }
                }

                // 1. Check if Block is Blast-Proof (Custom Glass)
                if (gameArena.getData().isBlastProof(pos)) {
                    return false; // NEVER destroy blast-proof glass
                }

                // 2. Check if Block is Player Placed or Bed
                // "The original map block should also not be able to be destroyed by tnt"

                boolean isPlayerPlaced = gameArena.getData().isBlockPlayerPlaced(pos);

                // Allow Beds to be destroyed (Requested update)
                if (state.getBlock() instanceof net.minecraft.block.BedBlock) {
                    return true;
                }

                if (isPlayerPlaced) {
                    return true; // Allow destruction of player placed blocks (unless blast proof, handled above)
                }

                return false; // Protect everything else
            }
        }

        return true;
    }
}
