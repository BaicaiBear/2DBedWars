package top.bearcabbage.twodimensional_bedwars.screen.button;

import net.minecraft.item.ItemStack;

public class ACButton {
    public ItemStack stack;
    public ACListener listener;

    public ACButton(ItemStack stack) {
        this.stack = stack;
    }

    public ACButton withListener(ACListener listener) {
        this.listener = listener;
        return this;
    }

    public ACButton withStack(ItemStack stack) {
        this.stack = stack;
        return this;
    }

    public ItemStack getStack() {
        return stack;
    }

    public boolean hasListener() {
        return listener != null;
    }

    public ACListener getListener() {
        return listener == null ? ACListener.none() : listener;
    }
}
