package top.bearcabbage.twodimensional_bedwars.screen.button;

@FunctionalInterface
public interface ACListener {
    void onClick(InventoryEvent event);
    static ACListener none() {
        return event -> {};
    }
}
