package top.bearcabbage.twodimensional_bedwars.component;

import java.util.List;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class OreGenerator {
    private final BlockPos location;
    private final Item itemType;
    private int amount;
    private double delaySeconds;
    private int limit;
    private final String identifier; // e.g. "Diamond", "Sapphire" (custom)

    private int currentTimer;

    public OreGenerator(BlockPos location, Item itemType, int amount, double delaySeconds, int limit) {
        this(location, itemType, amount, delaySeconds, limit, "Unknown");
    }

    public OreGenerator(BlockPos location, Item itemType, int amount, double delaySeconds, int limit,
            String identifier) {
        this.location = location;
        this.itemType = itemType;
        this.amount = amount;
        this.delaySeconds = delaySeconds;
        this.limit = limit;
        this.identifier = identifier;
        this.currentTimer = (int) (delaySeconds * 20);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void updateSettings(int amount, double delaySeconds, int limit) {
        this.amount = amount;
        this.delaySeconds = delaySeconds;
        this.limit = limit;
        // Don't reset timer immediately, just let it tick down.
        // Or if new delay is much shorter, maybe clamp?
        // For simplicity: keep current timer but ensure next reset uses new delay.
    }

    public void tick(ServerWorld world) {
        currentTimer--;

        // Update Hologram logic would go here (visual packet)

        if (currentTimer <= 0) {
            spawnItem(world);
            currentTimer = (int) (delaySeconds * 20); // Reset to configured seconds * 20 ticks
        }
    }

    private void spawnItem(ServerWorld world) {
        // Scan for existing items to enforce limit
        Box checkArea = new Box(location).expand(2.0);
        List<ItemEntity> nearbyItems = world.getEntitiesByClass(ItemEntity.class, checkArea,
                entity -> entity.getStack().getItem() == itemType);

        int currentCount = nearbyItems.stream().mapToInt(entity -> entity.getStack().getCount()).sum();

        if (currentCount < limit) {
            ItemStack stack = new ItemStack(itemType, amount);
            ItemEntity itemEntity = new ItemEntity(world, location.getX() + 0.5, location.getY() + 0.5,
                    location.getZ() + 0.5, stack);
            itemEntity.setVelocity(0, 0, 0);
            itemEntity.setToDefaultPickupDelay();
            itemEntity.age = -32768; // Effectively infinite lifetime (takes ~27 minutes to despawn)
            world.spawnEntity(itemEntity);
        }
    }
}
