package top.bearcabbage.twodimensional_bedwars.component;

import java.util.UUID;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;

public class BedWarsPlayer {
    private final UUID uuid;
    private final ITeam team;
    
    // Tools: 0=None, 1=Wooden, 2=Stone, 3=Iron, 4=Diamond
    private int swordLevel = 1; // Default Wooden
    private int pickaxeLevel = 0;
    private int axeLevel = 0;
    
    // State: 0=Spectator, 1=Alive
    private int state = 0;
    
    public BedWarsPlayer(UUID uuid, ITeam team) {
        this.uuid = uuid;
        this.team = team;
    }

    public UUID getUuid() { return uuid; }
    public ITeam getTeam() { return team; }
    public int getState() { return state; }
    public void setState(int state) { this.state = state; }

    public void saveLobbyState(ServerPlayerEntity player) {
        top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.saveBackup(player);
    }
    
    public void restoreLobbyState(ServerPlayerEntity player) {
        top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.restoreBackup(player);
    }

    public boolean tryUpgradeSword(ServerPlayerEntity player, int newLevel) {
        if (newLevel > swordLevel) {
            swordLevel = newLevel;
            removeItemsByType(player, "SWORD");
            giveSword(player);
            return true;
        }
        return false;
    }

    public boolean tryUpgradePickaxe(ServerPlayerEntity player, int newLevel) {
        if (newLevel > pickaxeLevel) {
            pickaxeLevel = newLevel;
            removeItemsByType(player, "PICKAXE");
            givePickaxe(player);
            return true;
        }
        return false;
    }

    public boolean tryUpgradeAxe(ServerPlayerEntity player, int newLevel) {
        if (newLevel > axeLevel) {
            axeLevel = newLevel;
            removeItemsByType(player, "_AXE");
            giveAxe(player);
            return true;
        }
        return false;
    }

    public void upgradeSword(ServerPlayerEntity player) { 
        tryUpgradeSword(player, swordLevel + 1);
    }
    
    public void upgradePickaxe(ServerPlayerEntity player) { 
        tryUpgradePickaxe(player, pickaxeLevel + 1);
    }
    
    public void upgradeAxe(ServerPlayerEntity player) { 
        tryUpgradeAxe(player, axeLevel + 1);
    }

    // Apply tools to player inventory (clears existing tools first if needed, or just sets slots)
    public void applyTools(ServerPlayerEntity player) {
        // Remove old tools first to be safe? Or assume empty on respawn.
        // On respawn it's empty.
        giveSword(player);
        givePickaxe(player);
        giveAxe(player);
    }
    
    private void giveSword(ServerPlayerEntity player) {
        ItemStack item = getSwordItem();
        if (item != null) player.getInventory().offerOrDrop(item);
    }
    
    private void givePickaxe(ServerPlayerEntity player) {
        ItemStack item = getPickaxeItem();
        if (item != null) player.getInventory().offerOrDrop(item);
    }
    
    private void giveAxe(ServerPlayerEntity player) {
        ItemStack item = getAxeItem();
        if (item != null) player.getInventory().offerOrDrop(item);
    }
    
    private void removeItemsByType(ServerPlayerEntity player, String suffix) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String id = stack.getItem().toString().toUpperCase(); // e.g. "diamond_sword"
                // getName().getString() might be localized. toString() on Item usually gives "sword", "diamond_sword"?
                // Registry key is reliable.
                // But let's use check:
                if (isToolType(stack, suffix)) {
                    player.getInventory().removeStack(i);
                }
            }
        }
    }
    
    private boolean isToolType(ItemStack stack, String typeSuffix) {
        String name = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath().toUpperCase();
        if ("SWORD".equals(typeSuffix)) return name.endsWith("_SWORD");
        if ("PICKAXE".equals(typeSuffix)) return name.endsWith("_PICKAXE");
        if ("_AXE".equals(typeSuffix)) return name.endsWith("_AXE") && !name.contains("PICKAXE");
        return false;
    }

    private ItemStack getSwordItem() {
         return switch (swordLevel) {
            case 1 -> new ItemStack(Items.WOODEN_SWORD);
            case 2 -> new ItemStack(Items.STONE_SWORD);
            case 3 -> new ItemStack(Items.IRON_SWORD);
            case 4 -> new ItemStack(Items.DIAMOND_SWORD);
            default -> new ItemStack(Items.WOODEN_SWORD); 
        };
    }

    private ItemStack getPickaxeItem() {
        return switch (pickaxeLevel) {
            case 1 -> new ItemStack(Items.WOODEN_PICKAXE);
            case 2 -> new ItemStack(Items.STONE_PICKAXE);
            case 3 -> new ItemStack(Items.IRON_PICKAXE);
            case 4 -> new ItemStack(Items.DIAMOND_PICKAXE);
            default -> null; 
        };
    }

    private ItemStack getAxeItem() {
         return switch (axeLevel) {
            case 1 -> new ItemStack(Items.WOODEN_AXE);
            case 2 -> new ItemStack(Items.STONE_AXE);
            case 3 -> new ItemStack(Items.IRON_AXE);
            case 4 -> new ItemStack(Items.DIAMOND_AXE);
            default -> null; 
        };
    }

    public void handleDeath(ServerPlayerEntity player, ServerWorld world) {
        // 1. Drop Currency
        dropCurrency(player, world);
        
        // 2. Clear Inventory (Remove everything else)
        player.getInventory().clear();
        
        // 3. Downgrade Tools
        downgradeTools();
    }

    private void dropCurrency(ServerPlayerEntity player, ServerWorld world) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isCurrency(stack)) {
                // Drop it
                ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), stack.copy());
                world.spawnEntity(itemEntity);
            }
        }
    }

    private boolean isCurrency(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isOf(Items.IRON_INGOT) || 
               stack.isOf(Items.GOLD_INGOT) || 
               stack.isOf(Items.DIAMOND) || 
               stack.isOf(Items.EMERALD) || 
               stack.isOf(Items.NETHERITE_INGOT) ||
               stack.isOf(Items.QUARTZ);
    }

    private void downgradeTools() {
        // 4->3, 3->2, 2->1, 1->1
        if (swordLevel > 1) swordLevel--;
        if (pickaxeLevel > 1) pickaxeLevel--;
        if (axeLevel > 1) axeLevel--;
        
        // Note: We do NOT go to 0 if level was 1. 1 stays 1.
        // If level was 0 (for Pick/Axe), it stays 0.
    }
}
