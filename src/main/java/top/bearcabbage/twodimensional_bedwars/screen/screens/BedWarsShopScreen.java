package top.bearcabbage.twodimensional_bedwars.screen.screens;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import java.util.List;
import top.bearcabbage.twodimensional_bedwars.screen.button.ItemBuilder;
import top.bearcabbage.twodimensional_bedwars.config.GameConfig;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.enchantment.Enchantments;

public class BedWarsShopScreen extends AbstractACScreen {

    private final List<GameConfig.ShopEntry> shopEntries;

    public BedWarsShopScreen(List<GameConfig.ShopEntry> shopEntries) {
        super(6);
        this.shopEntries = shopEntries;
    }

    @Override
    public Text getTitleText() {
        return Text.translatable("two-dimensional-bedwars.shop.title");
    }

    @Override
    protected void addButtons(ServerPlayerEntity viewer) {
        top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam team = null;
        top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer = null;

        top.bearcabbage.twodimensional_bedwars.api.IArena arena = top.bearcabbage.twodimensional_bedwars.game.ArenaManager
                .getInstance().getArena();
        if (arena instanceof top.bearcabbage.twodimensional_bedwars.component.Arena arenaImpl) {
            top.bearcabbage.twodimensional_bedwars.api.ITeam t = arenaImpl.getTeam(viewer);
            if (t instanceof top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam bwt) {
                team = bwt;
                bwPlayer = bwt.getPlayer(viewer.getUuid());
            }
        }

        for (GameConfig.ShopEntry entry : shopEntries) {
            // Handle Upgrade Visibility logic
            if (!entry.type.equals("ITEM")) {
                int currentLevel = 0;

                // Team Upgrades
                if (team != null) {
                    if (entry.type.equals("UPGRADE_SHARPNESS"))
                        currentLevel = team.getSharpnessLevel();
                    else if (entry.type.equals("UPGRADE_PROTECTION"))
                        currentLevel = team.getProtectionLevel();
                    else if (entry.type.equals("UPGRADE_HASTE"))
                        currentLevel = team.getHasteLevel();
                    else if (entry.type.equals("UPGRADE_FORGE"))
                        currentLevel = team.getForgeLevel();
                }

                // Player Tool Upgrades
                if (bwPlayer != null) {
                    if (entry.type.equals("TOOL_PICKAXE"))
                        currentLevel = bwPlayer.getPickaxeLevel();
                    else if (entry.type.equals("TOOL_AXE"))
                        currentLevel = bwPlayer.getAxeLevel();
                    else if (entry.type.equals("TOOL_SWORD"))
                        currentLevel = bwPlayer.getSwordLevel();
                }

                // Only show next tier
                if (entry.upgradeTier != currentLevel + 1)
                    continue;
            }

            String[] itemParts = entry.itemId.split(":");
            Identifier itemId = itemParts.length > 1 ? Identifier.of(itemParts[0], itemParts[1])
                    : Identifier.of("minecraft", entry.itemId);

            String[] costParts = entry.costId.split(":");
            Identifier costId = costParts.length > 1 ? Identifier.of(costParts[0], costParts[1])
                    : Identifier.of("minecraft", entry.costId);

            Item item = Registries.ITEM.get(itemId);
            Item cost = Registries.ITEM.get(costId);

            if (item != Items.AIR && cost != Items.AIR) {
                addEntryButton(entry, item, cost, viewer, team, bwPlayer);
            }
        }
    }

    private void addEntryButton(GameConfig.ShopEntry entry, Item item, Item currency, ServerPlayerEntity viewer,
            top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam team,
            top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer) {

        Formatting color = Formatting.WHITE; // Default
        if (currency == Items.IRON_INGOT)
            color = Formatting.GRAY; // Iron = Gray
        else if (currency == Items.GOLD_INGOT)
            color = Formatting.GOLD; // Gold = Gold
        else if (currency == Items.DIAMOND)
            color = Formatting.AQUA; // Diamond = Aqua
        else if (currency == Items.EMERALD)
            color = Formatting.GREEN; // Emerald = Green
        else if (currency == Items.NETHERITE_INGOT)
            color = Formatting.DARK_GRAY; // Netherite = Dark Gray
        else if (currency == Items.QUARTZ)
            color = Formatting.WHITE; // Quartz = White

        Text currencyName = currency.getName().copy().formatted(color);

        Text displayName;
        if (entry.name == null || entry.name.isEmpty()) {
            displayName = item.getName();
        } else {
            // Assume translation key if it contains dots, otherwise literal (legacy basic
            // support)
            if (entry.name.contains(".")) {
                displayName = Text.translatable(entry.name);
            } else {
                displayName = Text.literal(entry.name);
            }
        }

        // Create display stack for the button with all properties applied
        ItemStack displayStack = new ItemStack(item);
        applySpecialProperties(viewer, entry, displayStack);

        setButton(entry.slot, ItemBuilder.start(displayStack) // Use the fully configured stack
                .name(Text.empty().append(displayName).formatted(Formatting.GREEN))
                .tooltip(
                        Text.translatable("two-dimensional-bedwars.shop.price",
                                Text.literal(entry.price + "").formatted(color),
                                currencyName),
                        Text.empty(),
                        Text.translatable("two-dimensional-bedwars.shop.click_to_purchase")
                                .formatted(Formatting.YELLOW))
                .button(event -> {
                    if (entry.type.equals("ITEM")) {
                        buyItem(viewer, createProductStack(viewer, entry, item), new ItemStack(currency, entry.price),
                                entry);
                    } else {
                        handleUpgrade(viewer, team, bwPlayer, entry, new ItemStack(currency, entry.price));
                    }
                }));
    }

    private ItemStack createProductStack(ServerPlayerEntity player, GameConfig.ShopEntry entry, Item item) {
        ItemStack stack = new ItemStack(item, entry.count);
        applySpecialProperties(player, entry, stack);
        return stack;
    }

    private void applySpecialProperties(ServerPlayerEntity player, GameConfig.ShopEntry entry, ItemStack stack) {
        // Add Enchants for display for Tool Upgrades
        if (entry.type.equals("TOOL_PICKAXE") || entry.type.equals("TOOL_AXE")) {
            net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup = player.getWorld()
                    .getRegistryManager();
            int tier = entry.upgradeTier;
            int eff = (tier == 1 || tier == 2) ? 1 : (tier == 3 ? 2 : (tier == 4 ? 3 : 0));
            if (eff > 0) {
                registryLookup.getOptional(RegistryKeys.ENCHANTMENT)
                        .orElseThrow()
                        .getOptional(Enchantments.EFFICIENCY)
                        .ifPresent(e -> stack.addEnchantment(e, eff));
            }
        }

        if (entry.specialType != null && !entry.specialType.isEmpty()) {
            net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
            nbt.putString("bedwars:item_type", entry.specialType);
            stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                    net.minecraft.component.type.NbtComponent.of(nbt));

            net.minecraft.registry.RegistryWrapper.WrapperLookup registryLookup = player.getWorld()
                    .getRegistryManager();

            if (entry.specialType.startsWith("POTION_")) {
                if (entry.specialType.equals("POTION_SPEED")) {
                    stack.set(net.minecraft.component.DataComponentTypes.POTION_CONTENTS,
                            new net.minecraft.component.type.PotionContentsComponent(
                                    java.util.Optional.of(net.minecraft.potion.Potions.SWIFTNESS),
                                    java.util.Optional.empty(), java.util.List.of(), java.util.Optional.empty()));
                    stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                            net.minecraft.text.Text.translatable("two-dimensional-bedwars.shop.potion.speed"));
                } else if (entry.specialType.equals("POTION_JUMP")) {
                    stack.set(net.minecraft.component.DataComponentTypes.POTION_CONTENTS,
                            new net.minecraft.component.type.PotionContentsComponent(
                                    java.util.Optional.of(net.minecraft.potion.Potions.LEAPING),
                                    java.util.Optional.empty(), java.util.List.of(), java.util.Optional.empty()));
                    stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                            net.minecraft.text.Text.translatable("two-dimensional-bedwars.shop.potion.jump"));
                } else if (entry.specialType.equals("POTION_INVISIBILITY")) {
                    stack.set(net.minecraft.component.DataComponentTypes.POTION_CONTENTS,
                            new net.minecraft.component.type.PotionContentsComponent(
                                    java.util.Optional.of(net.minecraft.potion.Potions.INVISIBILITY),
                                    java.util.Optional.empty(), java.util.List.of(), java.util.Optional.empty()));
                    stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME,
                            net.minecraft.text.Text.translatable("two-dimensional-bedwars.shop.potion.invis"));
                }
            } else if (entry.specialType.equals("KNOCKBACK_STICK")) {
                registryLookup.getOptional(RegistryKeys.ENCHANTMENT)
                        .orElseThrow()
                        .getOptional(Enchantments.KNOCKBACK)
                        .ifPresent(e -> stack.addEnchantment(e, 2));
            } else if (entry.specialType.equals("BOW_POWER_1")) {
                registryLookup.getOptional(RegistryKeys.ENCHANTMENT)
                        .orElseThrow()
                        .getOptional(Enchantments.POWER)
                        .ifPresent(e -> stack.addEnchantment(e, 1));
            } else if (entry.specialType.equals("BOW_POWER_1_PUNCH_1")) {
                registryLookup.getOptional(RegistryKeys.ENCHANTMENT)
                        .orElseThrow()
                        .getOptional(Enchantments.POWER)
                        .ifPresent(e -> stack.addEnchantment(e, 1));
                registryLookup.getOptional(RegistryKeys.ENCHANTMENT)
                        .orElseThrow()
                        .getOptional(Enchantments.PUNCH)
                        .ifPresent(e -> stack.addEnchantment(e, 1));
            }
        }
    }

    private void handleUpgrade(ServerPlayerEntity player,
            top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam team,
            top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer, GameConfig.ShopEntry entry,
            ItemStack cost) {

        int totalCurrency = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == cost.getItem()) {
                totalCurrency += s.getCount();
            }
        }

        if (totalCurrency < cost.getCount()) {
            player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.not_enough_resources"), true);
            return;
        }

        int currentLevel = 0;
        boolean isPlayerUpgrade = false;

        if (entry.type.startsWith("TOOL_")) {
            isPlayerUpgrade = true;
            if (bwPlayer != null) {
                if (entry.type.equals("TOOL_PICKAXE"))
                    currentLevel = bwPlayer.getPickaxeLevel();
                else if (entry.type.equals("TOOL_AXE"))
                    currentLevel = bwPlayer.getAxeLevel();
                else if (entry.type.equals("TOOL_SWORD"))
                    currentLevel = bwPlayer.getSwordLevel();
            }
        } else {
            if (team != null) {
                if (entry.type.equals("UPGRADE_SHARPNESS"))
                    currentLevel = team.getSharpnessLevel();
                else if (entry.type.equals("UPGRADE_PROTECTION"))
                    currentLevel = team.getProtectionLevel();
                else if (entry.type.equals("UPGRADE_HASTE"))
                    currentLevel = team.getHasteLevel();
                else if (entry.type.equals("UPGRADE_FORGE"))
                    currentLevel = team.getForgeLevel();
            }
        }

        if (entry.upgradeTier <= currentLevel) {
            player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.already_purchased"), true);
            return;
        }
        if (entry.upgradeTier > currentLevel + 1) {
            player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.unlock_previous"), true);
            return;
        }

        player.getInventory().remove(item -> item.getItem() == cost.getItem(), cost.getCount(), player.getInventory());
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        if (isPlayerUpgrade) {
            if (bwPlayer != null) {
                if (entry.type.equals("TOOL_PICKAXE"))
                    bwPlayer.tryUpgradePickaxe(player, entry.upgradeTier);
                else if (entry.type.equals("TOOL_AXE"))
                    bwPlayer.tryUpgradeAxe(player, entry.upgradeTier);
                else if (entry.type.equals("TOOL_SWORD"))
                    bwPlayer.tryUpgradeSword(player, entry.upgradeTier);
                Text itemNameText = (entry.name.contains(".")) ? Text.translatable(entry.name)
                        : Text.literal(entry.name);
                player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.upgraded_tool", itemNameText), true);
            }
        } else {
            if (team != null) {
                if (entry.type.equals("UPGRADE_SHARPNESS"))
                    team.setSharpnessLevel(entry.upgradeTier);
                else if (entry.type.equals("UPGRADE_PROTECTION"))
                    team.setProtectionLevel(entry.upgradeTier);
                else if (entry.type.equals("UPGRADE_HASTE"))
                    team.setHasteLevel(entry.upgradeTier);
                else if (entry.type.equals("UPGRADE_FORGE"))
                    team.setForgeLevel(entry.upgradeTier);

                Text itemNameText = (entry.name.contains(".")) ? Text.translatable(entry.name)
                        : Text.literal(entry.name);
                player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.upgrade_unlocked", itemNameText),
                        false);
            }
        }
        init(player);
    }

    private void buyItem(ServerPlayerEntity player, ItemStack product, ItemStack cost, GameConfig.ShopEntry entry) {
        int totalCurrency = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == cost.getItem()) {
                totalCurrency += s.getCount();
            }
        }

        if (totalCurrency >= cost.getCount()) {
            boolean handled = false;

            top.bearcabbage.twodimensional_bedwars.api.IArena arena = top.bearcabbage.twodimensional_bedwars.game.ArenaManager
                    .getInstance().getArena();
            if (arena instanceof top.bearcabbage.twodimensional_bedwars.component.Arena arenaImpl) {
                top.bearcabbage.twodimensional_bedwars.api.ITeam team = arenaImpl.getTeam(player);
                if (team instanceof top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam bwTeam) {
                    top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer = bwTeam
                            .getPlayer(player.getUuid());
                    if (bwPlayer != null) {
                        // Check for Tool Upgrades (Sword, Pickaxe, Axe)
                        if (handleToolUpgradePurchase(bwPlayer, player, product, entry)) {
                            handled = true;
                        } else {
                            // Fallback to existing logic
                            handled = tryHandleToolOrArmorPurchase(bwPlayer, player, product, entry);
                        }
                    }
                }
            }

            if (handled) {
                player.getInventory().remove(item -> item.getItem() == cost.getItem(), cost.getCount(),
                        player.getInventory());
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.equipped", product.getName()), true);
                init(player);
            } else {
                player.getInventory().remove(item -> item.getItem() == cost.getItem(), cost.getCount(),
                        player.getInventory());
                player.getInventory().offerOrDrop(product.copy());
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.purchased", product.getName()),
                        true);
                init(player);

                if (entry.specialType != null && entry.specialType.equals("SHEARS")) {
                    top.bearcabbage.twodimensional_bedwars.api.IArena arena2 = top.bearcabbage.twodimensional_bedwars.game.ArenaManager
                            .getInstance().getArena();
                    if (arena2 instanceof top.bearcabbage.twodimensional_bedwars.component.Arena arenaImpl) {
                        top.bearcabbage.twodimensional_bedwars.api.ITeam team = arenaImpl.getTeam(player);
                        if (team instanceof top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam bwTeam) {
                            top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer = bwTeam
                                    .getPlayer(player.getUuid());
                            if (bwPlayer != null)
                                bwPlayer.setHasShears(true);
                        }
                    }
                }
            }
        } else {
            player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(
                    Text.translatable("two-dimensional-bedwars.shop.need_resources", cost.getCount(), cost.getName()),
                    true);
        }
    }

    private boolean handleToolUpgradePurchase(
            top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer, ServerPlayerEntity player,
            ItemStack product, GameConfig.ShopEntry entry) {

        String itemId = net.minecraft.registry.Registries.ITEM.getId(product.getItem()).getPath().toUpperCase();

        // Define Tool Levels
        int level = 0;
        if (itemId.contains("WOODEN"))
            level = 1;
        else if (itemId.contains("STONE"))
            level = 2;
        else if (itemId.contains("IRON"))
            level = 3;
        else if (itemId.contains("DIAMOND"))
            level = 4;

        if (level == 0)
            return false; // Not a tiered tool execution

        if (itemId.endsWith("_SWORD")) {
            if (bwPlayer.tryUpgradeSword(player, level))
                return true;
            // If return false, it means current level >= new level.
            // We return true here to "consume" the action but maybe prevent purchase?
            // Or if we return false, it will fall through to normal buy (which duplicates
            // items).
            // We want to Block purchase if downgrade.
            player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.already_purchased"), true);
            return true;
        }

        if (itemId.endsWith("_PICKAXE")) {
            if (bwPlayer.tryUpgradePickaxe(player, level))
                return true;
            player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.already_purchased"), true);
            return true;
        }

        // Axe (Check for _AXE and NOT Pickaxe)
        if (itemId.endsWith("_AXE") && !itemId.contains("PICKAXE")) {
            if (bwPlayer.tryUpgradeAxe(player, level))
                return true;
            player.sendMessage(Text.translatable("two-dimensional-bedwars.shop.already_purchased"), true);
            return true;
        }

        return false;
    }

    private boolean tryHandleToolOrArmorPurchase(
            top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer, ServerPlayerEntity player,
            ItemStack product, GameConfig.ShopEntry entry) {
        if (entry.specialType != null) {
            // These are unnecessary if switching to Upgrade logic for Tools, but existing
            // shop entries might still use ITEM interaction for first buy?
            // Actually, if I changed Pickaxe to TOOL_PICKAXE type, it goes to
            // handleUpgrade, not buyItem.
            // But ARMOR types are still ITEM type in config (checked logic: only startsWith
            // TOOL_ is player upgrade logic).
            // Actually, my config for Armor still says "ITEM" (implicit).
            // Line 288-294 in GameConfig: new ShopEntry(..., "ARMOR_CHAINMAIL") -> Calls
            // constructor setting type="ITEM".
            // So Armor goes to buyItem.

            if (entry.specialType.equals("ARMOR_CHAINMAIL")) {
                if (bwPlayer.getArmorLevel() < 1) {
                    bwPlayer.tryUpgradeArmor(player, 1);
                    return true;
                }
                return false;
            }
            if (entry.specialType.equals("ARMOR_IRON")) {
                if (bwPlayer.getArmorLevel() < 2) {
                    bwPlayer.tryUpgradeArmor(player, 2);
                    return true;
                }
                return false;
            }
            if (entry.specialType.equals("ARMOR_DIAMOND")) {
                if (bwPlayer.getArmorLevel() < 3) {
                    bwPlayer.tryUpgradeArmor(player, 3);
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}
