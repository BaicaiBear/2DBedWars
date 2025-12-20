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

import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.playerdata.api.storage.NbtDataStorage;
import eu.pb4.playerdata.api.storage.PlayerDataStorage;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import top.bearcabbage.twodimensional_bedwars.world.SplitBiomeSource;
import top.bearcabbage.twodimensional_bedwars.world.ArenaChunkGenerator;
import top.bearcabbage.twodimensional_bedwars.game.ArenaManager;
import top.bearcabbage.twodimensional_bedwars.command.BedWarsCommand;

import top.bearcabbage.twodimensional_bedwars.component.Arena;
import top.bearcabbage.twodimensional_bedwars.api.IArena.GameStatus;
import top.bearcabbage.twodimensional_bedwars.mechanic.CustomItemHandler;

import net.minecraft.nbt.NbtCompound;

public class TwoDimensionalBedWars implements ModInitializer {
    public static final String MOD_ID = "two-dimensional-bedwars";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final PlayerDataStorage<NbtCompound> BACKUP_STORAGE = new NbtDataStorage("bedwars_backup");

    @Override
    public void onInitialize() {
        LOGGER.info("Starting BedWars Engine initialization...");

        Registry.register(Registries.BIOME_SOURCE, Identifier.of(MOD_ID, "split"), SplitBiomeSource.CODEC);
        Registry.register(Registries.CHUNK_GENERATOR, Identifier.of(MOD_ID, "arena"), ArenaChunkGenerator.CODEC);

        PlayerDataApi.register(BACKUP_STORAGE);

        CustomItemHandler.init();

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey().getValue().getNamespace().equals(MOD_ID) &&
                    world.getRegistryKey().getValue().getPath().equals("arena")) {
                world.setTimeOfDay(6000);
                world.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
            }
        });

        // Safe Breakdown on Server Stop
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                if (gameArena.getStatus() == GameStatus.PLAYING) {
                    LOGGER.info("Server stopping: Forcing game end and cleanup...");
                    gameArena.stopGame();
                }
            }
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
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
                            try {
                                java.nio.file.Files.deleteIfExists(destPath.resolve("level.dat"));
                                java.nio.file.Files.deleteIfExists(destPath.resolve("level.dat_old"));
                                java.nio.file.Files.deleteIfExists(destPath.resolve("session.lock"));
                                LOGGER.info("Cleaned up potential conflicting world data in blueprint dimension.");
                            } catch (java.io.IOException e) {
                                LOGGER.warn("Failed to clean up blueprint directory", e);
                            }
                        }

                        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files
                                .walk(sourcePath)) {
                            stream.forEach(source -> {
                                String relative = sourcePath.relativize(source).toString();
                                if (!relative.startsWith("region"))
                                    return;
                                java.nio.file.Path destination = destPath.resolve(relative);
                                try {
                                    if (java.nio.file.Files.isDirectory(source)) {
                                        if (!java.nio.file.Files.exists(destination))
                                            java.nio.file.Files.createDirectories(destination);
                                    } else {
                                        java.nio.file.Files.copy(source, destination,
                                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

        ArenaManager.getInstance().registerArena(new Arena());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BedWarsCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                gameArena.tick(server.getOverworld());
            }
        });

        ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                if (gameArena.getStatus() == GameStatus.PLAYING) {
                    gameArena.handleDeath(player, damageSource);
                    player.setHealth(player.getMaxHealth());
                    player.getHungerManager().setFoodLevel(20);
                    player.getInventory().clear();
                    player.teleport((ServerWorld) player.getWorld(), player.getX(), 100.0, player.getZ(),
                            EnumSet.noneOf(PositionFlag.class), player.getYaw(), player.getPitch(), false);
                    return false;
                }
            }
            return true;
        });

        // Instant Break Logic (Re-added)
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT
                .register((player, world, hand, pos, direction) -> {
                    if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                        if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                            if (gameArena.getStatus() == GameStatus.PLAYING) {
                                if (gameArena.attemptBreakBed(serverPlayer, pos)) {
                                    return ActionResult.SUCCESS; // Handled (Instant Break)
                                }
                            }
                        }
                    }
                    return ActionResult.PASS;
                });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player.isCreative())
                return true;

            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // 1. If Game is NOT playing, allow everything.
                    if (gameArena.getStatus() != GameStatus.PLAYING) {
                        return true;
                    }

                    // 2. If Player is NOT in the Game World, allow everything.
                    // (Assuming lobby is a different world, or at least consistent with gameWorld
                    // tracking)
                    if (gameArena.getGameWorld() != null && player.getWorld() != gameArena.getGameWorld()) {
                        return true;
                    }

                    // 3. Game is PLAYING and Player is in Game World.
                    // Apply Restrictions.
                    if (gameArena.getData().isBlockPlayerPlaced(pos)
                            || state.getBlock() instanceof net.minecraft.block.BedBlock) {
                        return gameArena.handleBlockBreak(serverPlayer, pos, state);
                    } else {
                        serverPlayer.sendMessage(
                                Text.translatable("two-dimensional-bedwars.block.break_map_prevention"), true);
                        return false;
                    }
                }
            }
            return true;
        });

        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                    if (gameArena.getStatus() == GameStatus.PLAYING) {
                        net.minecraft.block.BlockState state = world.getBlockState(hitResult.getBlockPos());

                        if (state.getBlock() == net.minecraft.block.Blocks.CRAFTING_TABLE) {
                            player.sendMessage(Text.translatable("two-dimensional-bedwars.block.crafting_disabled"),
                                    true);
                            return ActionResult.FAIL;
                        }

                        if (state.getBlock() == net.minecraft.block.Blocks.ENDER_CHEST) {
                            if (gameArena.handleEnderChest(serverPlayer))
                                return ActionResult.SUCCESS;
                        }

                        if (state.getBlock() instanceof net.minecraft.block.BedBlock) {
                            if (!serverPlayer.isSneaking()) {
                                serverPlayer.sendMessage(
                                        Text.translatable("two-dimensional-bedwars.block.sleep_disabled"), true);
                                return ActionResult.FAIL;
                            }
                        }

                        net.minecraft.item.ItemStack stack = player.getStackInHand(hand);
                        if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
                            net.minecraft.util.math.BlockPos targetPos = hitResult.getBlockPos()
                                    .offset(hitResult.getSide());
                            String special = CustomItemHandler.getSpecialType(stack);
                            if (special != null && special.equals("BLAST_PROOF_GLASS")) {
                                gameArena.getData().recordBlastProof(targetPos);
                            } else {
                                gameArena.getData().recordPlacedBlock(targetPos);
                            }
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });

        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                net.minecraft.item.ItemStack stack = player.getStackInHand(hand);
                if (stack.getItem() == net.minecraft.item.Items.PAPER &&
                        stack.getName().getString().contains("Shop")) {

                    if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                        if (gameArena.getStatus() != GameStatus.PLAYING)
                            return ActionResult.PASS;

                        java.util.List<top.bearcabbage.twodimensional_bedwars.config.GameConfig.ShopEntry> shopList = top.bearcabbage.twodimensional_bedwars.config.GameConfig
                                .getInstance().shop;

                        serverPlayer.openHandledScreen(
                                new top.bearcabbage.twodimensional_bedwars.screen.screens.BedWarsShopScreen(shopList));
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT
                .register((player, world, hand, entity, hitResult) -> {
                    if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                            && hand == net.minecraft.util.Hand.MAIN_HAND) {
                        if (entity.getCommandTags().contains("BedWarsShop")) {
                            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                                if (gameArena.getStatus() != GameStatus.PLAYING)
                                    return ActionResult.PASS;

                                // Check if player is a participant
                                if (!gameArena.getParticipantUUIDs().contains(player.getUuid())) {
                                    return ActionResult.PASS;
                                }

                                java.util.List<top.bearcabbage.twodimensional_bedwars.config.GameConfig.ShopEntry> shopList = top.bearcabbage.twodimensional_bedwars.config.GameConfig
                                        .getInstance().shop;

                                serverPlayer.openHandledScreen(
                                        new top.bearcabbage.twodimensional_bedwars.screen.screens.BedWarsShopScreen(
                                                shopList));
                                return ActionResult.SUCCESS;
                            }
                        }
                    }
                    return ActionResult.PASS;
                });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.hasBackup(player)) {
                boolean restored = top.bearcabbage.twodimensional_bedwars.data.BedWarsPlayerData.restoreBackup(player);
                if (restored) {
                    player.sendMessage(Text.translatable("two-dimensional-bedwars.backup.restored"), false);

                    // Fix: If player successfully restored but was still tracked as "In Game",
                    // we must remove them from the Arena logic WITHOUT triggering another
                    // restore/wipe.
                    if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                        if (gameArena.getTeam(player) != null) {
                            gameArena.leavePlayer(player, true); // true = Skip Restore
                        }
                    }
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (ArenaManager.getInstance().getArena() instanceof Arena gameArena) {
                // Determine if player needs to be removed from Arena
                // (Waiting, Spectator, or Playing)
                // Arena's safe leavePlayer handles all cases or ignores if unknown.
                gameArena.leavePlayer(player);
            }
        });

        LOGGER.info("BedWars Engine initialized!");
    }
}