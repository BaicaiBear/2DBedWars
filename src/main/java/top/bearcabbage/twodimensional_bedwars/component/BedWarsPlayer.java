package top.bearcabbage.twodimensional_bedwars.component;

import java.util.UUID;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.enchantment.Enchantments;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;

public class BedWarsPlayer {
    private final UUID uuid;
    private final ITeam team;

    // Tools
    private int swordLevel = 1;
    private int pickaxeLevel = 0;
    private int axeLevel = 0;
    private int state = 0;
    private int kills = 0;
    private int deaths = 0;
    private int armorLevel = 0;
    private boolean hasShears = false;

    public BedWarsPlayer(UUID uuid, ITeam team) {
        this.uuid = uuid;
        this.team = team;
    }

    public UUID getUuid() {
        return uuid;
    }

    public ITeam getTeam() {
        return team;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getKills() {
        return kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    // Tool Getters
    public int getSwordLevel() {
        return swordLevel;
    }

    public int getPickaxeLevel() {
        return pickaxeLevel;
    }

    public int getAxeLevel() {
        return axeLevel;
    }

    public void saveLobbyState(ServerPlayerEntity player) {
        top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.saveBackup(player);
    }

    public boolean restoreLobbyState(ServerPlayerEntity player) {
        return top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.restoreBackup(player);
    }

    public void setArmorLevel(int level) {
        if (level > this.armorLevel)
            this.armorLevel = level;
    }

    public int getArmorLevel() {
        return armorLevel;
    }

    public void tryUpgradeArmor(ServerPlayerEntity player, int level) {
        if (level > this.armorLevel) {
            this.armorLevel = level;
            giveArmor(player);
        }
    }

    public void setHasShears(boolean has) {
        this.hasShears = has;
    }

    public boolean hasShears() {
        return hasShears;
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
            givePickaxe(player, (ServerWorld) player.getWorld());
            return true;
        }
        return false;
    }

    public boolean tryUpgradeAxe(ServerPlayerEntity player, int newLevel) {
        if (newLevel > axeLevel) {
            axeLevel = newLevel;
            removeItemsByType(player, "_AXE");
            giveAxe(player, (ServerWorld) player.getWorld());
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

    public void applyTools(ServerPlayerEntity player) {
        giveSword(player);
        givePickaxe(player, (ServerWorld) player.getWorld());
        giveAxe(player, (ServerWorld) player.getWorld());
        giveArmor(player);
        if (hasShears) {
            if (!player.getInventory().contains(new ItemStack(Items.SHEARS))) {
                player.getInventory().offerOrDrop(new ItemStack(Items.SHEARS));
            }
        }
    }

    private void giveSword(ServerPlayerEntity player) {
        ItemStack item = getSwordItem();
        if (item != null)
            player.getInventory().offerOrDrop(item);
    }

    private void givePickaxe(ServerPlayerEntity player, ServerWorld world) {
        ItemStack item = getPickaxeItem(world);
        if (item != null)
            player.getInventory().offerOrDrop(item);
    }

    private void giveAxe(ServerPlayerEntity player, ServerWorld world) {
        ItemStack item = getAxeItem(world);
        if (item != null)
            player.getInventory().offerOrDrop(item);
    }

    public void giveArmor(ServerPlayerEntity player) {
        ItemStack boots = ItemStack.EMPTY;
        ItemStack leggings = ItemStack.EMPTY;
        ItemStack chestplace = ItemStack.EMPTY;
        ItemStack helmet = ItemStack.EMPTY;

        switch (armorLevel) {
            case 1 -> {
                boots = new ItemStack(Items.CHAINMAIL_BOOTS);
                leggings = new ItemStack(Items.CHAINMAIL_LEGGINGS);
                chestplace = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                helmet = new ItemStack(Items.CHAINMAIL_HELMET);
            }
            case 2 -> {
                boots = new ItemStack(Items.IRON_BOOTS);
                leggings = new ItemStack(Items.IRON_LEGGINGS);
                chestplace = new ItemStack(Items.IRON_CHESTPLATE);
                helmet = new ItemStack(Items.IRON_HELMET);
            }
            case 3 -> {
                boots = new ItemStack(Items.DIAMOND_BOOTS);
                leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
                chestplace = new ItemStack(Items.DIAMOND_CHESTPLATE);
                helmet = new ItemStack(Items.DIAMOND_HELMET);
            }
        }

        if (!boots.isEmpty())
            player.equipStack(EquipmentSlot.FEET, boots);
        if (!leggings.isEmpty())
            player.equipStack(EquipmentSlot.LEGS, leggings);
        if (!chestplace.isEmpty())
            player.equipStack(EquipmentSlot.CHEST, chestplace);
        if (!helmet.isEmpty())
            player.equipStack(EquipmentSlot.HEAD, helmet);
    }

    private void removeItemsByType(ServerPlayerEntity player, String suffix) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && isToolType(stack, suffix)) {
                player.getInventory().removeStack(i);
            }
        }
    }

    private boolean isToolType(ItemStack stack, String typeSuffix) {
        String name = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath().toUpperCase();
        if ("SWORD".equals(typeSuffix) && stack.getItem() == Items.STICK)
            return false;

        if ("SWORD".equals(typeSuffix))
            return name.endsWith("_SWORD");
        if ("PICKAXE".equals(typeSuffix))
            return name.endsWith("_PICKAXE");
        if ("_AXE".equals(typeSuffix))
            return name.endsWith("_AXE") && !name.contains("PICKAXE");
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

    private ItemStack getPickaxeItem(ServerWorld world) {
        ItemStack stack = switch (pickaxeLevel) {
            case 1 -> new ItemStack(Items.WOODEN_PICKAXE);
            case 2 -> new ItemStack(Items.STONE_PICKAXE);
            case 3 -> new ItemStack(Items.IRON_PICKAXE);
            case 4 -> new ItemStack(Items.DIAMOND_PICKAXE);
            default -> ItemStack.EMPTY;
        };

        if (!stack.isEmpty()) {
            int eff = (pickaxeLevel == 1 || pickaxeLevel == 2) ? 1
                    : (pickaxeLevel == 3 ? 2 : (pickaxeLevel == 4 ? 3 : 0));
            if (eff > 0) {
                world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT)
                        .orElseThrow()
                        .getOptional(Enchantments.EFFICIENCY)
                        .ifPresent(e -> stack.addEnchantment(e, eff));
            }
        }
        return stack.isEmpty() ? null : stack;
    }

    private ItemStack getAxeItem(ServerWorld world) {
        ItemStack stack = switch (axeLevel) {
            case 1 -> new ItemStack(Items.WOODEN_AXE);
            case 2 -> new ItemStack(Items.STONE_AXE);
            case 3 -> new ItemStack(Items.IRON_AXE);
            case 4 -> new ItemStack(Items.DIAMOND_AXE);
            default -> ItemStack.EMPTY;
        };

        if (!stack.isEmpty()) {
            int eff = (axeLevel == 1) ? 1 : (axeLevel == 2 || axeLevel == 3 ? 2 : (axeLevel == 4 ? 3 : 0));
            if (eff > 0) {
                world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT)
                        .orElseThrow()
                        .getOptional(Enchantments.EFFICIENCY)
                        .ifPresent(e -> stack.addEnchantment(e, eff));
            }
        }
        return stack.isEmpty() ? null : stack;
    }

    public void handleDeath(ServerPlayerEntity player, ServerWorld world) {
        dropCurrency(player, world);
        player.getInventory().clear();
        downgradeTools();
    }

    private void dropCurrency(ServerPlayerEntity player, ServerWorld world) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isCurrency(stack)) {
                ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(),
                        stack.copy());
                world.spawnEntity(itemEntity);
            }
        }
    }

    private boolean isCurrency(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        return stack.isOf(Items.IRON_INGOT) || stack.isOf(Items.GOLD_INGOT) || stack.isOf(Items.DIAMOND)
                || stack.isOf(Items.EMERALD) || stack.isOf(Items.NETHERITE_INGOT) || stack.isOf(Items.QUARTZ);
    }

    private void downgradeTools() {
        if (swordLevel > 1)
            swordLevel--;
        if (pickaxeLevel > 1)
            pickaxeLevel--;
        if (axeLevel > 1)
            axeLevel--;

        // Armor vanishes on death
        armorLevel = 0;
    }
}
