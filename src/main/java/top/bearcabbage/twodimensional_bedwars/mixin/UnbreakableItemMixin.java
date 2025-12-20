package top.bearcabbage.twodimensional_bedwars.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class UnbreakableItemMixin {

    @Inject(method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private void onDamage(int amount, net.minecraft.server.world.ServerWorld world, net.minecraft.server.network.ServerPlayerEntity player, java.util.function.Consumer<net.minecraft.item.Item> breakCallback, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (player != null) {
            String dimId = world.getRegistryKey().getValue().toString();
            // Check for "arena" dimension
            if (dimId.contains("two-dimensional-bedwars") && dimId.contains("arena")) {
                // Prevent damage
                ci.cancel(); 
            }
        }
    }
}
