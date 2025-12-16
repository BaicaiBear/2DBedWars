package top.bearcabbage.twodimensional_bedwars.screen.button;

public interface IButtonHolder {
    ACButton setButton(int page, int index, ACButton button);
    int getPage();
    ACButton onClick(InventoryEvent event);
}
