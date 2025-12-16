package top.bearcabbage.twodimensional_bedwars;

import java.util.EnumSet;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.bearcabbage.twodimensional_bedwars.game.ArenaManager;
import top.bearcabbage.twodimensional_bedwars.command.BedWarsCommand;

import top.bearcabbage.twodimensional_bedwars.component.Arena;
import top.bearcabbage.twodimensional_bedwars.api.IArena.GameStatus;

public class TwoDimensionalBedWars implements ModInitializer {
    public static final String MOD_ID = "two-dimensional-bedwars";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Starting BedWars Engine initialization...");
		
        // Register Server Starting Event for Map Import
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
                // Find map_template in Mod Resources
                var container = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(MOD_ID);
                if (container.isPresent()) {
                    java.nio.file.Path sourcePath = container.get().findPath("map_template").orElse(null);
                    
                    if (sourcePath != null) {
                        java.nio.file.Path levelPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
                        java.nio.file.Path destPath = levelPath.resolve("dimensions/two-dimensional-bedwars/blueprint");
                        
                        LOGGER.info("Importing Map from Mod Resources ({}) to {}", sourcePath, destPath);
                        
                        if (!java.nio.file.Files.exists(destPath)) {
                             java.nio.file.Files.createDirectories(destPath);
                        } else {
                            // Clean up potential conflicting files from previous runs
                            try {
                                java.nio.file.Files.deleteIfExists(destPath.resolve("level.dat"));
                                java.nio.file.Files.deleteIfExists(destPath.resolve("level.dat_old"));
                                java.nio.file.Files.deleteIfExists(destPath.resolve("session.lock"));
                                LOGGER.info("Cleaned up potential conflicting world data in blueprint dimension.");
                            } catch (java.io.IOException e) {
                                LOGGER.warn("Failed to clean up blueprint directory", e);
                            }
                        }
                        
                        // Recursive Copy (Works for both Directory and JAR FileSystem)
                        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(sourcePath)) {
                            stream.forEach(source -> {
                                // Only Copy Region Files (and maybe data if needed, but definitely not level.dat)
                                String relative = sourcePath.relativize(source).toString();
                                
                                // Simple filter: Only allow 'region/' directory content
                                if (!relative.startsWith("region")) {
                                    return; 
                                }

                                java.nio.file.Path destination = destPath.resolve(relative);
                                
                                try {
                                    if (java.nio.file.Files.isDirectory(source)) {
                                        if (!java.nio.file.Files.exists(destination)) {
                                            java.nio.file.Files.createDirectories(destination);
                                        }
                                    } else {
                                        java.nio.file.Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    }
                                } catch (java.io.IOException e) {
                                    LOGGER.error("Failed to copy file: " + source, e);
                                }
                            });
                        }
                        LOGGER.info("Map Import Complete.");
                    } else {
                        LOGGER.warn("map_template not found in Mod Resources!");
                    }
                }

            } catch (Exception e) {
                LOGGER.error("Failed to import map", e);
            }
        });

		// Initialize the ArenaManager and register the single arena instance
        ArenaManager.getInstance().registerArena(new Arena());
		
        // Register Command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BedWarsCommand.register(dispatcher);
        });

        // Register Server Tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                // Determine sensible world to pass
                gameArena.tick(server.getOverworld());
            }
        });
        
        // Register Respawn Event
        // Register Fake Death Event (ALLOW_DEATH)
        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                // If game is running, handle death manually
                if (gameArena.getStatus() == GameStatus.PLAYING) {
                    gameArena.handleDeath(player);
                    
                    // Reset health effectively "respawning" them as spectator immediately
                    player.setHealth(player.getMaxHealth());
                    player.getHungerManager().setFoodLevel(20);
                    player.getInventory().clear();
                    
                    // Teleport to spectator height (Y=100) above current position
                    // We stay in the same world
                    player.teleport((ServerWorld) player.getWorld(), player.getX(), 100.0, player.getZ(), EnumSet.noneOf(PositionFlag.class), player.getYaw(), player.getPitch(), false);
                    
                    // Return false to cancel the actual death process
                    return false;
                }
            }
            return true; // Allow normal death if not in game
        });
        
        // Register Join Event
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                gameArena.addPlayer(handler.player);
            }
        });
        
        // Register Block Break Event
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // "Disable block breaking... except for beds and player placed block"
                    if (gameArena.getStatus() == GameStatus.PLAYING) {
                         // Allow if Bed (handled in handleBlockBreak) OR if block is in placed blocks list
                         if (gameArena.getData().isBlockPlayerPlaced(pos) || state.getBlock() instanceof net.minecraft.block.BedBlock) {
                             return gameArena.handleBlockBreak(serverPlayer, pos, state);
                         } else {
                             serverPlayer.sendMessage(Text.literal("§cYou cannot break map blocks!"), true);
                             return false;
                         }
                    }
                }
            }
            return true;
        });
        
        // Register Use Block (Place & Interact)
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
             if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                 if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                     if (gameArena.getStatus() == GameStatus.PLAYING) {
                         net.minecraft.block.BlockState state = world.getBlockState(hitResult.getBlockPos());
                         
                         // Disable Crafting Table Interaction
                         if (state.getBlock() == net.minecraft.block.Blocks.CRAFTING_TABLE) {
                             player.sendMessage(Text.literal("§cCrafting is disabled!"), true);
                             return ActionResult.FAIL;
                         }
                         
                         // Track Placed Blocks
                         net.minecraft.item.ItemStack stack = player.getStackInHand(hand);
                         if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
                             net.minecraft.util.math.BlockPos targetPos = hitResult.getBlockPos().offset(hitResult.getSide());
                             gameArena.getData().recordPlacedBlock(targetPos);
                         }
                     }
                 }
             }
             return ActionResult.PASS;
        });



        // Register Use Item Event (Shop)
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                net.minecraft.item.ItemStack stack = player.getStackInHand(hand);
                if (stack.getItem() == net.minecraft.item.Items.PAPER && 
                    stack.getName().getString().contains("Shop")) {
                    
                    // Access Control: Must be in playing state
                    // TODO: Ideally check if player is actually participating in the game
                    if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                         if (gameArena.getStatus() != GameStatus.PLAYING) {
                             return ActionResult.PASS;
                         }
                         
                         // Context-Aware Shop Opening
                         // Arena 1 (Iron) center roughly 0,0
                         // Arena 2 (Gold) center roughly 400,0
                         // Threshold: X > 200 = Gold Shop
                         
                         java.util.List<top.bearcabbage.twodimensional_bedwars.config.GameConfig.ShopEntry> shopList;
                         if (player.getX() > 200) {
                             shopList = top.bearcabbage.twodimensional_bedwars.config.GameConfig.getInstance().goldShop;
                         } else {
                             shopList = top.bearcabbage.twodimensional_bedwars.config.GameConfig.getInstance().ironShop;
                         }
                         
                         serverPlayer.openHandledScreen(new top.bearcabbage.twodimensional_bedwars.screen.screens.BedWarsShopScreen(shopList));
                         return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        // Restore player backup on join (Crash Recovery)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.hasBackup(player)) {
               top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.restoreBackup(player);
               player.sendMessage(Text.of("§c[BedWars] Backup restored due to unexpected disconnect/restart."), false);
            }
        });
		LOGGER.info("BedWars Engine initialized!");
	}
}