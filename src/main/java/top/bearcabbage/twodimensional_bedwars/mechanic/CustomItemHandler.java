package top.bearcabbage.twodimensional_bedwars.mechanic;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import top.bearcabbage.twodimensional_bedwars.game.ArenaManager;
import top.bearcabbage.twodimensional_bedwars.component.Arena;

public class CustomItemHandler {

    public static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack stack = player.getStackInHand(hand);
                String specialType = getSpecialType(stack);

                if (specialType != null) {
                    if ("FIREBALL".equals(specialType)) {
                        return handleFireball(serverPlayer, world, stack);
                    }
                    if ("BRIDGE_EGG".equals(specialType)) {
                        return handleBridgeEgg(serverPlayer, world, stack);
                    }
                }
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                ItemStack stack = player.getStackInHand(hand);
                String specialType = getSpecialType(stack);

                if ("TNT_TRIGGERED".equals(specialType)) {
                    net.minecraft.util.math.BlockPos target = hitResult.getBlockPos().offset(hitResult.getSide());
                    if (!world.getBlockState(target).isAir())
                        return ActionResult.PASS;

                    TntEntity tnt = new TntEntity(world, target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                            serverPlayer);
                    tnt.setFuse(80);
                    world.spawnEntity(tnt);
                    world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.ENTITY_TNT_PRIMED,
                            SoundCategory.BLOCKS, 1.0f, 1.0f);

                    if (!player.isCreative())
                        stack.decrement(1);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    public static String getSpecialType(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return null;
        net.minecraft.component.type.NbtComponent nbtComponent = stack
                .get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            net.minecraft.nbt.NbtCompound nbt = nbtComponent.copyNbt();
            if (nbt.contains("bedwars:item_type")) {
                // Try to handle Optional<String> - compiler says it returns Optional
                Object val = nbt.getString("bedwars:item_type");
                if (val instanceof java.util.Optional opt) {
                    return (String) opt.orElse(null);
                }
                // If it was already string
                return (String) val;
            }
        }
        return null;
    }

    private static ActionResult handleFireball(ServerPlayerEntity player, World world, ItemStack stack) {
        Vec3d look = player.getRotationVector();
        FireballEntity fireball = new FireballEntity(world, player, look, 1);
        fireball.setPosition(player.getX(), player.getEyeY() + look.y, player.getZ());
        world.spawnEntity(fireball);

        if (!player.isCreative()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    private static ActionResult handleBridgeEgg(ServerPlayerEntity player, World world, ItemStack stack) {
        top.bearcabbage.twodimensional_bedwars.entity.BridgeEggEntity egg = new top.bearcabbage.twodimensional_bedwars.entity.BridgeEggEntity(
                world, player);
        egg.setItem(stack);
        egg.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, 1.5f, 1.0f);
        world.spawnEntity(egg);

        if (!player.isCreative()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
