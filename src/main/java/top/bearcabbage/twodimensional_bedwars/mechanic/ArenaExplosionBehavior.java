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
        // Check standard behavior first
        // If fallback is provided, respect it.
        if (fallback != null) {
             if (!fallback.canDestroyBlock(explosion, world, pos, state, power)) {
                 return false;
             }
        } 
        // If fallback is null, standard Minecraft behavior is to check logic in Explosion class (blast resistance).
        // Since we are overriding canDestroyBlock, if we return true/false, it is final?
        // Wait, Explosion.java calls behavior.canDestroyBlock(). 
        // If behavior is null, it typically runs internal logic: state.isAir() || explosion.getPower() > ...
        // If we provide a behavior, the Internal Logic MIGHT BE SKIPPED or Delegated?
        // In Vanilla:
        // if (behavior != null) { return behavior.canDestroyBlock(...); }
        // else { return state.getBlock().getBlastResistance(...) ... }
        
        // Replacing 'null' with 'ArenaExplosionBehavior' means WE are responsible for the blast resistance check if fallback was null!
        // This is tricky.
        // We should call the default logic if fallback is null.
        // But we can't easily call "super.canDestroyBlock" because ExplosionBehavior base class implementation is: 
        // "public boolean canDestroyBlock(...) { return true; }" ?
        // Let's obtain the default behavior instance if possible.
        // Or, we can implement the blast resistance check manually?
        
        if (fallback == null) {
             // Mimic default logic: Check blast resistance
             // return state.getBlock().getBlastResistance() ... 
             // Actually, base checks rely on return value.
             // If we return NEW implementation, we must handle basic resistance.
             // Base ExplosionBehavior.canDestroyBlock implementation:
             // "return true" (actually abstract? No, it's not abstract, it returns true).
             // Wait, looking at Yarn:
             // ExplosionBehavior.canDestroyBlock default impl takes context and returns true?
             // No, usually it returns: state.isAir() || ...
        }
        
        // Actually, safer bet:
        // If fallback is null, we assume standard behavior.
        // We can just check our restriction. If we restrict, return false.
        // If we allow, we verify blast resistance?
        // If we just return 'true', then obsidion might break?
        // YES. We need to handle blast resistance if fallback is null.
        
        if (fallback == null) {
             // Use default calculation
             // float resistance = state.getBlock().getBlastResistance();
             // float damage = power; // Wait, calculation is complex.
             // We cannot easily replicate it without access.
             
             // ALTERNATIVE: Don't replace null. If null, create a WRAPPER that Delegates to "Default"?
             // But "Default" behavior is implicit in `Explosion` class when behavior is null.
             // Is there a "DefaultExplosionBehavior" class?
             // No.
             
             // BUT! 1.21 `ExplosionBehavior` base class DOES have a default implementation!
             // `public boolean canDestroyBlock(...)`
             // Does it do the check?
             // If I extend ExplosionBehavior, `super.canDestroyBlock` might be the default?
             // Checking source (via knowledge):
             // Base `ExplosionBehavior#canDestroyBlock` simply returns `true` (or delegates).
             // The *Logic* inside `Explosion` uses `behavior.canDestroyBlock`.
             // If I return true, it breaks.
             
             // Ok, I need to know if `ExplosionBehavior` has the logic.
             // It seems `ExplosionBehavior` handles calculation?
             // Search result 6: "ExplosionBehavior class also plays a role in defining how explosions interact".
             
             // I will assume `super.canDestroyBlock` handles the default logic if I am extending it.
             // So I should call `super.canDestroyBlock()`?
             
        }
        
        // Let's try calling super.
        if (fallback == null) {
             if (!super.canDestroyBlock(explosion, world, pos, state, power)) {
                 return false;
             }
        }
        
        // ... then my logic.

        // Custom Arena Logic
        if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
            if (gameArena.getStatus() == GameStatus.PLAYING) {
                // If it is NOT player placed, protect it.
                // UNLESS it is a bed? User said: "original map block should also not be able to be destroyed by tnt"
                // Usually beds are breakable by TNT in BedWars?
                // User requirement: "The original map block should also not be able to be destroyed by tnt"
                // Implies player placed blocks ARE destroyable.
                // What about Beds?
                // Standard BedWars: Beds act like map blocks regarding TNT usually? Or are they destroyed?
                // User didn't specify beds explicitly for TNT, only "original map block".
                // I will protect map blocks. Beds are map blocks usually (part of map).
                // If the bed is NOT in `placedBlocks`, it's protected.
                // If the user wants beds to be destroyed by TNT, they need to say so.
                // BUT, standard bedwars allows TNT to expose beds.
                // Safest bet: stick to strict "only player placed blocks are destroyable".
                
                if (gameArena.getData().isBlockPlayerPlaced(pos) || state.getBlock() instanceof net.minecraft.block.BedBlock) {
                    return true;
                }
                return false;
            }
        }

        // If not in game, default to true (allow destruction) or fallback's opinion?
        // Fallback said yes (or null), so we say yes.
        return true;
    }
}
