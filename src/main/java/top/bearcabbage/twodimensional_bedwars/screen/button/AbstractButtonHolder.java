package top.bearcabbage.twodimensional_bedwars.screen.button;

import java.util.HashMap;

public abstract class AbstractButtonHolder implements IButtonHolder {
    protected HashMap<Integer, HashMap<Integer, ACButton>> buttons;

    public AbstractButtonHolder() {
        buttons = new HashMap<>();
    }

    @Override
    public ACButton setButton(int page, int index, ACButton button) {
        return button == null ? buttonsForPage(page).remove(index) : buttonsForPage(page).put(index, button);
    }

    protected HashMap<Integer, ACButton> buttonsForPage(int page) {
        return buttons.computeIfAbsent(page, p -> new HashMap<>());
    }

    @Override
    public ACButton onClick(InventoryEvent event) {
        ACButton ac = buttonsForPage(getPage()).get(event.index);
        if (ac != null && ac.hasListener()) {
            try {
                ac.listener.onClick(event);
            } catch (Exception e) {
                System.err.println("Error while applying listener: " + ac.stack.getItem().getName().getString() + " inventory event: " + event);
                e.printStackTrace(System.err);
            }
        }
        return ac;
    }
}
