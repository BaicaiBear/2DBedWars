package top.bearcabbage.twodimensional_bedwars.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import top.bearcabbage.twodimensional_bedwars.mechanic.ArenaExplosionBehavior;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    // Target createExplosion. 
    // We modify the 'ExplosionBehavior' argument.
    // It is usually index 2 or 3 depending on overload?
    // Using argsOnly=true and type matching is safer.
    
    @ModifyVariable(method = "createExplosion", at = @At("HEAD"), argsOnly = true)
    private ExplosionBehavior injectArenaExplosionBehavior(ExplosionBehavior behavior) {
        // Wrap the provided behavior (or null) with our Arena behavior
        // If behavior is null, Vanilla uses a default. We pass null to fallback, 
        // and our class should handle null fallback by assuming 'true'?
        // Wait, default behavior isn't "always true", it's "based on blast resistance".
        // If fallback is null, we should use default behavior check?
        // Actually, ExplosionBehavior has a default instance? No.
        // If I pass null to my wrapper, I need to mimic default?
        // Or I can return a new ArenaExplosionBehavior(behavior).
        
        // BETTER: Only wrap if we are in an arena game?
        // Yes, the behavior class checks that.
        
        return new ArenaExplosionBehavior(behavior);
    }
}
