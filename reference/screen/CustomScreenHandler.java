package me.alpestrine.c.reward.screen;

import me.alpestrine.c.reward.screen.screens.AbstractACScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

public class CustomScreenHandler extends GenericContainerScreenHandler {
    public CustomScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, AbstractACScreen inventory, PlayerEntity viewer, int rows) {
        super(type, syncId, playerInventory, inventory, rows);
        if (viewer instanceof ServerPlayerEntity spe) {
            inventory.init(spe);
        }
    }
}
