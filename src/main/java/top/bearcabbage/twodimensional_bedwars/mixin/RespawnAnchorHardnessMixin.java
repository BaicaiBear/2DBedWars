package top.bearcabbage.twodimensional_bedwars.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class RespawnAnchorHardnessMixin {

    @Shadow
    public abstract net.minecraft.block.Block getBlock();

    @Inject(method = "getHardness", at = @At("HEAD"), cancellable = true)
    private void onGetHardness(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (this.getBlock() == Blocks.RESPAWN_ANCHOR) {
            String dimId = "";
            if (world instanceof World w) {
                 dimId = w.getRegistryKey().getValue().toString();
            }
            
            // Check for "arena" dimension
            // Typically "two-dimensional-bedwars:arena"
            if (dimId.contains("two-dimensional-bedwars") && dimId.contains("arena")) {
                cir.setReturnValue(0.2f); // Same as Bed
            }
        }
    }
}
