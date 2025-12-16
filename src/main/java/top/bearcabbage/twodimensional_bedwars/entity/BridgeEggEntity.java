package top.bearcabbage.twodimensional_bedwars.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class BridgeEggEntity extends ThrownItemEntity {

    // Standard constructor with wildcard
    public BridgeEggEntity(EntityType<? extends BridgeEggEntity> entityType, World world) {
        super(entityType, world);
    }

    public BridgeEggEntity(World world, LivingEntity owner, ItemStack stack) {
        super(EntityType.EGG, owner, world, stack);
    }

    public BridgeEggEntity(World world, LivingEntity owner) {
        super(EntityType.EGG, owner, world, new ItemStack(Items.EGG));
    }

    public BridgeEggEntity(World world, double x, double y, double z) {
        super(EntityType.EGG, x, y, z, world, new ItemStack(Items.EGG));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.EGG;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient)
            return;

        if (this.age > 1) {
            net.minecraft.util.math.Vec3d currentPos = this.getPos();
            // Estimate previous pos using velocity (since prevX fields might be
            // inaccessible or named differently in this mapping)
            net.minecraft.util.math.Vec3d velocity = this.getVelocity();
            net.minecraft.util.math.Vec3d prevPos = currentPos.subtract(velocity);

            // Interpolate
            double dist = prevPos.distanceTo(currentPos);
            int steps = (int) Math.ceil(dist * 2); // 2 steps per block roughly

            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                net.minecraft.util.math.Vec3d lerped = prevPos.lerp(currentPos, t);
                BlockPos basePos = BlockPos.ofFloored(lerped);
                BlockPos below = basePos.down();

                // Width logic
                net.minecraft.util.math.Vec3d vel = this.getVelocity();
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

    private void placeBridgeBlock(BlockPos pos) {
        if (this.getWorld().getBlockState(pos).isAir()) {
            net.minecraft.block.Block blockToPlace = Blocks.WHITE_WOOL;
            if (getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity player) {
                top.bearcabbage.twodimensional_bedwars.api.IArena arena = top.bearcabbage.twodimensional_bedwars.game.ArenaManager
                        .getInstance().getArena();
                if (arena instanceof top.bearcabbage.twodimensional_bedwars.component.Arena arenaImpl) {
                    top.bearcabbage.twodimensional_bedwars.api.ITeam team = arenaImpl.getTeam(player);
                    if (team != null) {
                        if (team.getName().contains("Red"))
                            blockToPlace = Blocks.RED_WOOL;
                        else if (team.getName().contains("Blue"))
                            blockToPlace = Blocks.BLUE_WOOL;
                        else if (team.getName().contains("Green"))
                            blockToPlace = Blocks.GREEN_WOOL;
                        else if (team.getName().contains("Yellow"))
                            blockToPlace = Blocks.YELLOW_WOOL;
                    }
                }
            }

            if (top.bearcabbage.twodimensional_bedwars.game.ArenaManager.getInstance()
                    .getArena() instanceof top.bearcabbage.twodimensional_bedwars.component.Arena arena) {
                arena.getData().recordPlacedBlock(pos);
            }

            this.getWorld().setBlockState(pos, blockToPlace.getDefaultState());
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        net.minecraft.entity.Entity owner = this.getOwner();
        if (this.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            entityHitResult.getEntity().damage(serverWorld, this.getDamageSources().thrown(this, owner), 0.0f);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            this.discard();
        }
    }
}
