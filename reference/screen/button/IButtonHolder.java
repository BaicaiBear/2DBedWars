package me.alpestrine.c.reward.screen.button;

public interface IButtonHolder {
    ACButton setButton(int page, int index, ACButton button);
    int getPage();
    ACButton onClick(InventoryEvent event);
}
