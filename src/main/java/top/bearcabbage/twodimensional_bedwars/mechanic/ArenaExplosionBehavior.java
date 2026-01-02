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
                // BUT protect from friendly fireballs
                if (state.getBlock() instanceof net.minecraft.block.BedBlock || state.isOf(net.minecraft.block.Blocks.RESPAWN_ANCHOR)) {
                    if (shouldProtectBed(gameArena, explosion, pos, world)) {
                        return false;
                    }
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

    private boolean shouldProtectBed(Arena arena, Explosion explosion, BlockPos pos, BlockView world) {
         net.minecraft.entity.Entity source = explosion.getEntity();
         if (source instanceof net.minecraft.entity.projectile.FireballEntity fireball 
             && fireball.getOwner() instanceof net.minecraft.entity.player.PlayerEntity player) {
             
             if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                 top.bearcabbage.twodimensional_bedwars.api.ITeam team = arena.getTeam(serverPlayer);
                 if (team instanceof top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam bwTeam) {
                     return isTeamBedOrAnchor(pos, bwTeam, world);
                 }
             }
         }
         return false;
    }

    private boolean isTeamBedOrAnchor(BlockPos pos, top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam playerTeam, BlockView world) {
        if (isLocationMatch(pos, playerTeam.getBedLocation(1), world)) return true;
        if (isLocationMatch(pos, playerTeam.getBedLocation(2), world)) return true;
        return false;
    }

    private boolean isLocationMatch(BlockPos target, BlockPos teamBedPos, BlockView world) {
        if (teamBedPos == null) return false;
        if (target.equals(teamBedPos)) return true;
        
        BlockState state = world.getBlockState(teamBedPos);
        if (state.getBlock() instanceof net.minecraft.block.BedBlock) {
            net.minecraft.util.math.Direction facing = state.get(net.minecraft.block.BedBlock.FACING);
            // Foot or Head? We just check the other part.
            // Actually, we need to know WHICH part teamBedPos is to know where the other part is.
             net.minecraft.block.enums.BedPart part = state.get(net.minecraft.block.BedBlock.PART);
            
            BlockPos otherPartPos;
            if (part == net.minecraft.block.enums.BedPart.FOOT) {
                otherPartPos = teamBedPos.offset(facing); 
            } else {
                otherPartPos = teamBedPos.offset(facing.getOpposite());
            }
            
            if (target.equals(otherPartPos)) return true;
        }
        return false;
    }


}
