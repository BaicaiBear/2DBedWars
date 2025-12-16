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
            player.getInventory().remove(item -> item.getItem() == cost.getItem(), cost.getCount(), player.getInventory());
            player.getInventory().offerOrDrop(product.copy());
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage(Text.of("§aPurchased " + product.getName().getString() + "!"), true);
        } else {
            player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Text.of("§cNot enough resources! Need " + cost.getCount() + " " + cost.getName().getString()), true);
        }
    }
}
