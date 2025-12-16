package top.bearcabbage.twodimensional_bedwars.api;

import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import top.bearcabbage.twodimensional_bedwars.component.BedWarsPlayer;

public interface ITeam {
    boolean isBedDestroyed();
    
    void setBedDestroyed(boolean destroyed);

    List<UUID> getMembers();
    
    // New methods for BedWarsPlayer
    void addPlayer(BedWarsPlayer player);
    List<BedWarsPlayer> getPlayers();
    BedWarsPlayer getPlayer(UUID uuid);

    List<BlockPos> getGenerators();

    void spawnNPCs();
    
    void tick(net.minecraft.server.world.ServerWorld world);
    
    String getName();
    
    int getColor(); // Decimal color for text/leather armor
    
    void setBedLocation(int arenaId, BlockPos pos);
    BlockPos getBedLocation(int arenaId);
}
