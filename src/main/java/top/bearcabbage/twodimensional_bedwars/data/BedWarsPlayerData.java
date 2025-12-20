package top.bearcabbage.twodimensional_bedwars.data;

import top.bearcabbage.twodimensional_bedwars.TwoDimensionalBedWars;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.network.packet.s2c.play.PositionFlag;

public class BedWarsPlayerData {

    public static void saveBackup(ServerPlayerEntity player) {
        NbtCompound data = new NbtCompound();

        // Mark as Not Restored (Persistent Backup)
        data.putBoolean("Restored", false);

        // Save Location & Dimension
        NbtCompound loc = new NbtCompound();
        loc.putDouble("x", player.getX());
        loc.putDouble("y", player.getY());
        loc.putDouble("z", player.getZ());
        loc.putFloat("yaw", player.getYaw());
        loc.putFloat("pitch", player.getPitch());
        loc.putString("dim", player.getWorld().getRegistryKey().getValue().toString());
        data.put("Location", loc);

        // Save Inventory using CODEC
        NbtList invList = new NbtList();
        for (int i = 0; i < player.getInventory().size(); ++i) {
            net.minecraft.item.ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound itemWrapper = new NbtCompound();
                itemWrapper.putByte("Slot", (byte) i);

                // Encode Item using Codec
                net.minecraft.item.ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
                        .resultOrPartial(e -> System.err.println("Failed to encode item: " + e))
                        .ifPresent(tag -> itemWrapper.put("Item", tag));

                invList.add(itemWrapper);
            }
        }
        data.put("Inventory", invList);

        // Save XP & Health
        data.putFloat("Health", player.getHealth());
        data.putInt("Food", player.getHungerManager().getFoodLevel());
        data.putInt("XpLevel", player.experienceLevel);
        data.putFloat("XpProgress", player.experienceProgress);

        // Use instance save
        TwoDimensionalBedWars.BACKUP_STORAGE.save(player, data);
    }

    public static boolean hasBackup(ServerPlayerEntity player) {
        NbtCompound data = TwoDimensionalBedWars.BACKUP_STORAGE.load(player);
        return data != null && !data.getBoolean("Restored").orElse(false);
    }

    public static boolean restoreBackup(ServerPlayerEntity player) {
        NbtCompound data = TwoDimensionalBedWars.BACKUP_STORAGE.load(player);
        if (data != null) {
            // Check for previous restoration
            if (data.getBoolean("Restored").orElse(false)) {
                TwoDimensionalBedWars.LOGGER.warn("Restoring backup for player {} that was ALREADY marked as restored!",
                        player.getName().getString());
                // We proceed anyway as failsafe
            }
            // Restore Inventory using CODEC
            if (data.contains("Inventory")) {
                NbtElement elem = data.get("Inventory");
                if (elem instanceof NbtList list) {
                    player.getInventory().clear();
                    for (int i = 0; i < list.size(); ++i) {
                        // getCompound returns Optional<NbtCompound>?
                        NbtCompound itemWrapper = list.getCompound(i).orElse(new NbtCompound());

                        int slot = 0;
                        if (itemWrapper.contains("Slot")) {
                            // If getByte returns Optional<Byte> or Byte?
                            // Error 1093: "itemWrapper.getByte("Slot")" -> Optional<Byte>.
                            // So:
                            slot = itemWrapper.getByte("Slot").orElse((byte) 0);
                        }

                        int safeSlot = slot & 255;

                        if (itemWrapper.contains("Item")) {
                            net.minecraft.item.ItemStack.CODEC.parse(NbtOps.INSTANCE, itemWrapper.get("Item"))
                                    .result()
                                    .ifPresent(stack -> {
                                        if (safeSlot < player.getInventory().size()) {
                                            player.getInventory().setStack(safeSlot, stack);
                                        }
                                    });
                        }
                    }
                }
            }

            // Restore Vitals - WITH OR_ELSE
            if (data.contains("Health"))
                player.setHealth(data.getFloat("Health").orElse(20f));
            if (data.contains("Food"))
                player.getHungerManager().setFoodLevel(data.getInt("Food").orElse(20));
            if (data.contains("XpLevel"))
                player.experienceLevel = data.getInt("XpLevel").orElse(0);
            if (data.contains("XpProgress"))
                player.experienceProgress = data.getFloat("XpProgress").orElse(0.0f);

            // Restore Location
            if (data.contains("Location")) {
                NbtCompound loc = data.getCompound("Location").orElse(new NbtCompound());
                String dimStr = loc.getString("dim").orElse("minecraft:overworld");

                ServerWorld targetWorld = player.getServer().getWorld(
                        RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimStr)));
                if (targetWorld == null)
                    targetWorld = player.getServer().getOverworld();

                player.teleport(targetWorld,
                        loc.getDouble("x").orElse(0d),
                        loc.getDouble("y").orElse(0d),
                        loc.getDouble("z").orElse(0d),
                        java.util.EnumSet.noneOf(PositionFlag.class),
                        loc.getFloat("yaw").orElse(0f),
                        loc.getFloat("pitch").orElse(0f),
                        false);
            }

            // Clean up
            // Mark as Restored (Do NOT delete file)
            try {
                data.putBoolean("Restored", true);
                TwoDimensionalBedWars.BACKUP_STORAGE.save(player, data);
            } catch (Exception e) {
                TwoDimensionalBedWars.LOGGER
                        .error("Failed to update backup status for player " + player.getName().getString(), e);
            }
            return true;
        }
        return false;
    }
}
