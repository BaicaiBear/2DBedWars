package top.bearcabbage.twodimensional_bedwars.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import top.bearcabbage.twodimensional_bedwars.screen.screens.AbstractACScreen;

public class CustomScreenHandler extends GenericContainerScreenHandler {
    public CustomScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, AbstractACScreen inventory, PlayerEntity viewer, int rows) {
        super(type, syncId, playerInventory, inventory, rows);
        if (viewer instanceof ServerPlayerEntity spe) {
            inventory.init(spe);
        }
    }
    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
        // If the click is outside the inventory (e.g. drop) or in player inv, let super handle it?
        // Actually, we want to block interactions with the shop inventory slots completely except for our buttons.
        
        // Slots 0 to (rows*9 - 1) are the container.
        // Slots after that are player inventory.
        
        int inventorySize = getInventory().size();
        
        if (slotIndex >= 0 && slotIndex < inventorySize) {
            // This is a click in our custom GUI
            if (player instanceof ServerPlayerEntity spe && getInventory() instanceof AbstractACScreen screen) {
                // Execute Button Logic
                top.bearcabbage.twodimensional_bedwars.screen.button.InventoryEvent event = 
                    new top.bearcabbage.twodimensional_bedwars.screen.button.InventoryEvent(slotIndex, button, actionType, screen, spe);
                
                screen.onClick(event);
                
                // Cancel default behavior (pickup) by NOT calling super.onSlotClick
                // We do usually want to update the tree changes though if needed, but for a "button" GUI, 
                // preventing the item from moving is key.
                return;
            }
        }
        
        // Ensure shift-clicking from player inventory into the shop is also blocked/handled if necessary.
        // For now, allow default behavior for player inventory slots (picking up their own items),
        // but verify shift-click (QUICK_MOVE) doesn't move items INTO the shop.
        if (actionType == net.minecraft.screen.slot.SlotActionType.QUICK_MOVE) {
            // If checking a player slot, and it tries to move to shop -> Cancel.
            if (slotIndex >= inventorySize) {
                 // It's a player slot. Super will try to move it to container.
                 // We should probably block this to keep the shop clean.
                 return;
            }
        }
        
        super.onSlotClick(slotIndex, button, actionType, player);
    }
}
