package top.bearcabbage.twodimensional_bedwars.mixin;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;
import top.bearcabbage.twodimensional_bedwars.component.Arena;
import top.bearcabbage.twodimensional_bedwars.game.ArenaManager;

@Mixin(ThrownEntity.class)
public abstract class EggEntityMixin extends ProjectileEntity {

    public EggEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void tickBridgeEgg(CallbackInfo ci) {
        if (this.getWorld().isClient)
            return;

        // Ensure this logic only runs for EggEntity instances
        if (!((Object) this instanceof EggEntity))
            return;

        // Simplified Logic: All eggs in "arena" dimension are Bridge Eggs
        net.minecraft.util.Identifier dimId = this.getWorld().getRegistryKey().getValue();

        if (dimId.getPath().equals("arena")) {
            if (this.age > 10) {
                Vec3d currentPos = this.getPos();
                Vec3d velocity = this.getVelocity();
                Vec3d prevPos = currentPos.subtract(velocity);

                double dist = prevPos.distanceTo(currentPos);
                int steps = (int) Math.ceil(dist * 2);

                for (int i = 0; i <= steps; i++) {
                    double t = (double) i / steps;
                    Vec3d lerped = prevPos.lerp(currentPos, t);
                    BlockPos basePos = BlockPos.ofFloored(lerped);
                    BlockPos below = basePos.down();

                    Vec3d vel = this.getVelocity();
                    BlockPos offsetPos;
                    if (Math.abs(vel.x) > Math.abs(vel.z)) {
                        offsetPos = below.south();
                    } else {
                        offsetPos = below.east();
                    }

                    placeBridgeBlock(below);
                    placeBridgeBlock(offsetPos);
                }
            }
        }
    }

    private void placeBridgeBlock(BlockPos pos) {
        if (this.getWorld().getBlockState(pos).isAir()) {
            net.minecraft.block.Block blockToPlace = Blocks.WHITE_WOOL;

            net.minecraft.entity.Entity owner = this.getOwner();
            if (owner instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                if (ArenaManager.getInstance().getArena() instanceof Arena arena) {
                    ITeam team = arena.getTeam(player);
                    if (team != null) {
                        String teamName = team.getName();
                        // top.bearcabbage.twodimensional_bedwars.TwoDimensionalBedWars.LOGGER.info("[BridgeEgg]
                        // Owner: " + player.getName().getString() + " Team: " + teamName);
                        if (teamName.contains("Red")) {
                            blockToPlace = Blocks.RED_WOOL;
                        } else if (teamName.contains("Blue")) {
                            blockToPlace = Blocks.BLUE_WOOL;
                        } else if (teamName.contains("Green")) {
                            blockToPlace = Blocks.GREEN_WOOL;
                        } else if (teamName.contains("Yellow")) {
                            blockToPlace = Blocks.YELLOW_WOOL;
                        }
                    }
                }
            }

            if (ArenaManager.getInstance().getArena() instanceof Arena arena) {
                arena.getData().recordPlacedBlock(pos);
            }

            this.getWorld().setBlockState(pos, blockToPlace.getDefaultState());
        }
    }
}
