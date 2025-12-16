package top.bearcabbage.twodimensional_bedwars.component;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ShopManager {
    // Defines a simple shop item structure
    public static class ShopItem {
        public final ItemStack itemStack;
        public final Item costCurrency;
        public final int costAmount;

        public ShopItem(ItemStack itemStack, Item costCurrency, int costAmount) {
            this.itemStack = itemStack;
            this.costCurrency = costCurrency;
            this.costAmount = costAmount;
        }
    }

    private final Map<String, ShopItem> shopItems = new HashMap<>();

    public ShopManager() {
        // Load default shop items (hardcoded for now as per spec implies config loading, but we need skeleton first)
    }

    public void handleTransaction(PlayerEntity player, ShopItem shopItem) {
        // Validation: Check balance
        if (hasEnoughCurrency(player, shopItem.costCurrency, shopItem.costAmount)) {
            removeCurrency(player, shopItem.costCurrency, shopItem.costAmount);
            giveItem(player, shopItem.itemStack);
        } else {
            // Send message: "You don't have enough resources!"
        }
    }

    private boolean hasEnoughCurrency(PlayerEntity player, Item currency, int amount) {
        // Inventory scan logic
        return player.getInventory().count(currency) >= amount;
    }

    private void removeCurrency(PlayerEntity player, Item currency, int amount) {
        // Remove logic
        // player.getInventory().remove(stack -> stack.getItem() == currency, amount, ...);
        // Note: Fabric/Mixin might be needed for exact inventory manipulation helpers or use standard looping.
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == currency) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
    }

    private void giveItem(PlayerEntity player, ItemStack item) {
        player.getInventory().insertStack(item.copy());
    }
}
