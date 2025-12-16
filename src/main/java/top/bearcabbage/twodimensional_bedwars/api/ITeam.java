package top.bearcabbage.twodimensional_bedwars.api;

import java.util.List;
import java.util.UUID;

import net.minecraft.util.math.BlockPos;

public interface ITeam {
    boolean isBedDestroyed();
    
    void setBedDestroyed(boolean destroyed);

    List<UUID> getMembers();

    List<BlockPos> getGenerators();

    void spawnNPCs();
    
    String getName();
    
    int getColor(); // Decimal color for text/leather armor
    
    void setBedLocation(int arenaId, BlockPos pos);
    BlockPos getBedLocation(int arenaId);
}
