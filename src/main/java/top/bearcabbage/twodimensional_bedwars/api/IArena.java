package top.bearcabbage.twodimensional_bedwars.api;

import java.util.List;

import net.minecraft.server.network.ServerPlayerEntity;

public interface IArena {
    enum GameStatus {
        WAITING,
        STARTING,
        PLAYING,
        ENDING,
        RESTORING
    }
    
    GameStatus getStatus();
    
    List<ITeam> getTeams();
    
    boolean addPlayer(ServerPlayerEntity player);
    void removePlayer(ServerPlayerEntity player);
    
    ITeam getTeam(ServerPlayerEntity player);
    void spawnNPCs();
    
    void startGame(net.minecraft.server.world.ServerWorld world, int teamCount);
    void stopGame();
}
