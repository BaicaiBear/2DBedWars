package top.bearcabbage.twodimensional_bedwars.screen.button;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

public class InventoryEvent {
    public final int index, button;
    public final SlotActionType action;
    public final ItemStack clickedStack;
    public final Inventory inventory;
    public final ServerPlayerEntity player;

    public InventoryEvent(int index, int button, SlotActionType action, Inventory inventory, ServerPlayerEntity player) {
        this.index = index;
        this.button = button;
        this.action = action;
        this.inventory = inventory;
        this.player = player;
        this.clickedStack = index == ScreenHandler.EMPTY_SPACE_SLOT_INDEX ? player.currentScreenHandler.getCursorStack() : inventory.getStack(index);
    }

    @Override
    public String toString() {
        return "InventoryEvent{" + index +
                ", " + button +
                ", " + action +
                ", " + clickedStack +
                ", " + inventory.getClass().getSimpleName() +
                '}';
    }
}
