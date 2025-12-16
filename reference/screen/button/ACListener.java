package me.alpestrine.c.reward.screen.button;

@FunctionalInterface
public interface ACListener {
    void onClick(InventoryEvent event);
    static ACListener none() {
        return event -> {};
    }
}
