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
import java.util.Optional;
import top.bearcabbage.twodimensional_bedwars.screen.button.ItemBuilder;
import top.bearcabbage.twodimensional_bedwars.config.GameConfig;

public class BedWarsShopScreen extends AbstractACScreen {

    private final List<GameConfig.ShopEntry> shopEntries;

    public BedWarsShopScreen(List<GameConfig.ShopEntry> shopEntries) {
        super(6); // 6 Rows
        this.shopEntries = shopEntries;
    }

    @Override
    public String getName() {
        return "§lBedWars Item Shop";
    }

    @Override
    protected void addButtons(ServerPlayerEntity viewer) {
        for (GameConfig.ShopEntry entry : shopEntries) {
            String[] itemParts = entry.itemId.split(":");
            Identifier itemId = itemParts.length > 1 ? Identifier.of(itemParts[0], itemParts[1]) : Identifier.of("minecraft", entry.itemId);
            
            String[] costParts = entry.costId.split(":");
            Identifier costId = costParts.length > 1 ? Identifier.of(costParts[0], costParts[1]) : Identifier.of("minecraft", entry.costId);

            Item item = Registries.ITEM.get(itemId);
            Item cost = Registries.ITEM.get(costId);
            
            if (item != Items.AIR && cost != Items.AIR) {
                addItem(entry.slot, item, entry.count, entry.price, cost, entry.name, viewer);
            }
        }
    }
    
    private void addItem(int slot, Item item, int count, int price, Item currency, String name, ServerPlayerEntity viewer) {
        String currencyName = currency.getName().getString();
        Formatting color = currency == Items.IRON_INGOT ? Formatting.WHITE : Formatting.GOLD;
        
        String displayName = name != null && !name.isEmpty() ? name : item.getName().getString();

        setButton(slot, ItemBuilder.start(item)
            .name("§a" + displayName)
            .tooltip(
                "§7Price: " + color + price + " " + currencyName,
                "",
                "§eClick to purchase!"
            )
            .button(event -> {
                buyItem(viewer, new ItemStack(item, count), new ItemStack(currency, price));
            })
        );
    }
    
    private void buyItem(ServerPlayerEntity player, ItemStack product, ItemStack cost) {
        int totalCurrency = 0;
        for(int i=0; i<player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.getItem() == cost.getItem()) {
                totalCurrency += s.getCount();
            }
        }
        
        if (totalCurrency >= cost.getCount()) {
            boolean handled = false;
            
            // Check if Tool Upgrade
            top.bearcabbage.twodimensional_bedwars.api.IArena arena = top.bearcabbage.twodimensional_bedwars.game.ArenaManager.getInstance().getArena();
            if (arena instanceof top.bearcabbage.twodimensional_bedwars.component.Arena arenaImpl) {
                top.bearcabbage.twodimensional_bedwars.api.ITeam team = arenaImpl.getTeam(player);
                if (team instanceof top.bearcabbage.twodimensional_bedwars.component.BedWarsTeam bwTeam) {
                    top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer = bwTeam.getPlayer(player.getUuid());
                    if (bwPlayer != null) {
                        handled = tryHandleToolPurchase(bwPlayer, player, product);
                    }
                }
            }
            
            if (handled) {
                // Remove currency
                player.getInventory().remove(item -> item.getItem() == cost.getItem(), cost.getCount(), player.getInventory());
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage(Text.of("§aUpgraded " + product.getName().getString() + "!"), true);
            } else {
                // Normal Purchase
                player.getInventory().remove(item -> item.getItem() == cost.getItem(), cost.getCount(), player.getInventory());
                player.getInventory().offerOrDrop(product.copy());
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.sendMessage(Text.of("§aPurchased " + product.getName().getString() + "!"), true);
            }
        } else {
            player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Text.of("§cNot enough resources! Need " + cost.getCount() + " " + cost.getName().getString()), true);
        }
    }
    
    private boolean tryHandleToolPurchase(top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer bwPlayer, ServerPlayerEntity player, ItemStack product) {
        Item item = product.getItem();
        if (item == Items.WOODEN_SWORD) return bwPlayer.tryUpgradeSword(player, 1);
        if (item == Items.STONE_SWORD) return bwPlayer.tryUpgradeSword(player, 2);
        if (item == Items.IRON_SWORD) return bwPlayer.tryUpgradeSword(player, 3);
        if (item == Items.DIAMOND_SWORD) return bwPlayer.tryUpgradeSword(player, 4);
        
        if (item == Items.WOODEN_PICKAXE) return bwPlayer.tryUpgradePickaxe(player, 1);
        if (item == Items.STONE_PICKAXE) return bwPlayer.tryUpgradePickaxe(player, 2);
        if (item == Items.IRON_PICKAXE) return bwPlayer.tryUpgradePickaxe(player, 3);
        if (item == Items.DIAMOND_PICKAXE) return bwPlayer.tryUpgradePickaxe(player, 4);
        
        if (item == Items.WOODEN_AXE) return bwPlayer.tryUpgradeAxe(player, 1);
        if (item == Items.STONE_AXE) return bwPlayer.tryUpgradeAxe(player, 2);
        if (item == Items.IRON_AXE) return bwPlayer.tryUpgradeAxe(player, 3);
        if (item == Items.DIAMOND_AXE) return bwPlayer.tryUpgradeAxe(player, 4);
        
        return false;
    }
}
