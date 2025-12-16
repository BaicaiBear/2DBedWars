package top.bearcabbage.twodimensional_bedwars.game;

import top.bearcabbage.twodimensional_bedwars.api.IArena;

public class ArenaManager {
    private static ArenaManager instance;
    private IArena arena;

    private ArenaManager() {}

    public static ArenaManager getInstance() {
        if (instance == null) {
            instance = new ArenaManager();
        }
        return instance;
    }

    public void registerArena(IArena arena) {
        this.arena = arena;
    }

    public IArena getArena() {
        return arena;
    }
}
