package top.bearcabbage.twodimensional_bedwars.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class GameConfig {
    private static GameConfig INSTANCE;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bedwars_config.json");

    public ArenaDetail arena1 = new ArenaDetail("Iron");
    public ArenaDetail arena2 = new ArenaDetail("Quartz");
    public BaseOffsets baseOffsets = new BaseOffsets();
    public RestoreConfig arenaRestoreConfig = new RestoreConfig();

    public GeneratorSetting ironGenerator = new GeneratorSetting(1, 0.5, 48);
    public GeneratorSetting goldGenerator = new GeneratorSetting(1, 2.0, 12);
    public GeneratorSetting diamondGenerator = new GeneratorSetting(1, 15.0, 4);
    public GeneratorSetting emeraldGenerator = new GeneratorSetting(1, 30.0, 2);
    public GeneratorSetting quartzGenerator = new GeneratorSetting(1, 0.5, 64);
    public GeneratorSetting netheriteGenerator = new GeneratorSetting(1, 45.0, 2);

    // Base Generator Progression (Levels 0-4)
    public List<ForgeLevel> forgeLevels = defaultForgeLevels();

    private static List<ForgeLevel> defaultForgeLevels() {
        List<ForgeLevel> levels = new ArrayList<>();

        // Level 0 (Start)
        List<ForgeResource> l0 = new ArrayList<>();
        l0.add(new ForgeResource("minecraft:iron_ingot", 1.0, 1, 48));
        l0.add(new ForgeResource("minecraft:gold_ingot", 4.0, 1, 12));
        levels.add(new ForgeLevel(0, l0));

        // Level 1 (+50% Resources)
        List<ForgeResource> l1 = new ArrayList<>();
        l1.add(new ForgeResource("minecraft:iron_ingot", 0.7, 1, 64));
        l1.add(new ForgeResource("minecraft:gold_ingot", 2.7, 1, 16));
        levels.add(new ForgeLevel(1, l1));

        // Level 2 (+100% Resources)
        List<ForgeResource> l2 = new ArrayList<>();
        l2.add(new ForgeResource("minecraft:iron_ingot", 0.5, 1, 64));
        l2.add(new ForgeResource("minecraft:gold_ingot", 2.0, 1, 24));
        levels.add(new ForgeLevel(2, l2));

        // Level 3 (Emeralds)
        List<ForgeResource> l3 = new ArrayList<>();
        l3.add(new ForgeResource("minecraft:iron_ingot", 0.35, 1, 64));
        l3.add(new ForgeResource("minecraft:gold_ingot", 1.3, 1, 32));
        l3.add(new ForgeResource("minecraft:emerald", 10.0, 1, 4));
        levels.add(new ForgeLevel(3, l3));

        // Level 4 (Supreme)
        List<ForgeResource> l4 = new ArrayList<>();
        l4.add(new ForgeResource("minecraft:iron_ingot", 0.25, 1, 64));
        l4.add(new ForgeResource("minecraft:gold_ingot", 1.0, 1, 48));
        l4.add(new ForgeResource("minecraft:emerald", 8.0, 1, 6));
        levels.add(new ForgeLevel(4, l4));

        return levels;
    }

    // Public Generators
    public List<Offset> arena1DiamondGenerators = defaultDiamondOffsets();
    public List<Offset> arena1EmeraldGenerators = defaultEmeraldOffsets();

    public List<Offset> arena2GoldGenerators = defaultGoldOffsets();
    public List<Offset> arena2NetheriteGenerators = defaultNetheriteOffsets();

    public List<ShopEntry> shop = defaultShop();

    public static class ForgeLevel {
        public int level;
        public List<ForgeResource> resources;

        public ForgeLevel(int level, List<ForgeResource> resources) {
            this.level = level;
            this.resources = resources;
        }
    }

    public static class ForgeResource {
        public String material;
        public double delay;
        public int amount;
        public int limit;

        public ForgeResource(String material, double delay, int amount, int limit) {
            this.material = material;
            this.delay = delay;
            this.amount = amount;
            this.limit = limit;
        }
    }

    private static List<Offset> defaultDiamondOffsets() {
        List<Offset> list = new ArrayList<>();
        list.add(new Offset(31, -18, 29));
        list.add(new Offset(29, -18, -31));
        list.add(new Offset(-31, -18, 29));
        list.add(new Offset(-29, -18, 31));
        return list;
    }

    private static List<Offset> defaultEmeraldOffsets() {
        List<Offset> list = new ArrayList<>();
        list.add(new Offset(-7, -14, -11));
        list.add(new Offset(8, -14, 12));
        return list;
    }

    private static List<Offset> defaultGoldOffsets() {
        List<Offset> list = new ArrayList<>();
        list.add(new Offset(31, -18, 29));
        list.add(new Offset(29, -18, -31));
        list.add(new Offset(-31, -18, 29));
        list.add(new Offset(-29, -18, 31));
        return list;
    }

    private static List<Offset> defaultNetheriteOffsets() {
        List<Offset> list = new ArrayList<>();
        list.add(new Offset(-7, -14, -11));
        list.add(new Offset(8, -14, 12));
        return list;
    }

    public static class ArenaDetail {
        public String resourceType; // "Iron", "Gold", "Quartz"

        public ArenaDetail(String resourceType) {
            this.resourceType = resourceType;
        }
    }

    public static class BaseOffsets {
        public TeamConfig team1 = new TeamConfig(new Offset(66, -18, 0), new Offset(82, -20, 0));
        public TeamConfig team2 = new TeamConfig(new Offset(-66, -18, 0), new Offset(-82, -20, 0));
        public TeamConfig team3 = new TeamConfig(new Offset(0, -18, 66), new Offset(0, -20, 82));
        public TeamConfig team4 = new TeamConfig(new Offset(0, -18, -66), new Offset(0, -20, -82));
    }

    public static class TeamConfig {
        public Offset spawn;
        public Offset generator;

        public TeamConfig(Offset spawn, Offset generator) {
            this.spawn = spawn;
            this.generator = generator;
        }
    }

    public static class Offset {
        public int dx;
        public int dy;
        public int dz;

        public Offset(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    public static class GeneratorSetting {
        public int amount;
        public double delaySeconds;
        public int limit;

        public GeneratorSetting(int amount, double delaySeconds, int limit) {
            this.amount = amount;
            this.delaySeconds = delaySeconds;
            this.limit = limit;
        }
    }

    public static class ShopEntry {
        public int slot;
        public String itemId;
        public int count;
        public String costId;
        public int price;
        public String name;
        public String type; // ITEM, UPGRADE_SHARPNESS, UPGRADE_PROTECTION, UPGRADE_HASTE, UPGRADE_FORGE,
                            // TOOL_PICKAXE, TOOL_AXE
        public int upgradeTier;
        public String specialType; // Extended function

        public ShopEntry(int slot, String itemId, int count, String costId, int price, String name) {
            this(slot, itemId, count, costId, price, name, "ITEM", 0, null);
        }

        public ShopEntry(int slot, String itemId, int count, String costId, int price, String name,
                String specialType) {
            this(slot, itemId, count, costId, price, name, "ITEM", 0, specialType);
        }

        public ShopEntry(int slot, String itemId, int count, String costId, int price, String name, String type,
                int upgradeTier) {
            this(slot, itemId, count, costId, price, name, type, upgradeTier, null);
        }

        public ShopEntry(int slot, String itemId, int count, String costId, int price, String name, String type,
                int upgradeTier, String specialType) {
            this.slot = slot;
            this.itemId = itemId;
            this.count = count;
            this.costId = costId;
            this.price = price;
            this.name = name;
            this.type = type;
            this.upgradeTier = upgradeTier;
            this.specialType = specialType;
        }
    }

    public static class BaseGeneratorRecipe {
        public int level;
        public String resourceType;
        public double delaySeconds;
        public int amount;
        public int limit;

        public BaseGeneratorRecipe(int level, String resourceType, double delaySeconds, int amount, int limit) {
            this.level = level;
            this.resourceType = resourceType;
            this.delaySeconds = delaySeconds;
            this.amount = amount;
            this.limit = limit;
        }
    }

    private static List<ShopEntry> defaultShop() {
        List<ShopEntry> list = new ArrayList<>();

        list.add(new ShopEntry(0, "minecraft:white_wool", 16, "minecraft:iron_ingot", 8, "Wool"));
        list.add(new ShopEntry(1, "minecraft:terracotta", 16, "minecraft:iron_ingot", 12, "Hardened Clay"));
        list.add(new ShopEntry(2, "minecraft:ladder", 16, "minecraft:iron_ingot", 4, "Ladder"));
        list.add(new ShopEntry(3, "minecraft:oak_planks", 16, "minecraft:iron_ingot", 16, "Wood Planks"));

        list.add(new ShopEntry(5, "minecraft:glass", 8, "minecraft:quartz", 12, "Blast-proof Glass",
                "BLAST_PROOF_GLASS"));
        list.add(new ShopEntry(6, "minecraft:end_stone", 12, "minecraft:quartz", 24, "End Stone"));
        list.add(new ShopEntry(7, "minecraft:obsidian", 4, "minecraft:netherite_ingot", 4, "Obsidian"));
        list.add(new ShopEntry(8, "minecraft:golden_apple", 1, "minecraft:gold_ingot", 2, "Golden Apple",
                "GOLDEN_APPLE"));

        list.add(new ShopEntry(9, "minecraft:water_bucket", 1, "minecraft:iron_ingot", 12, "Water Bucket",
                "WATER_BUCKET"));
        list.add(new ShopEntry(10, "minecraft:sponge", 4, "minecraft:iron_ingot", 48, "Sponge", "SPONGE"));
        list.add(new ShopEntry(11, "minecraft:fire_charge", 1, "minecraft:iron_ingot", 32, "Fireball", "FIREBALL"));
        list.add(new ShopEntry(12, "minecraft:egg", 1, "minecraft:emerald", 1, "Bridge Egg", "BRIDGE_EGG"));

        list.add(new ShopEntry(14, "minecraft:stone_sword", 1, "minecraft:quartz", 10, "Stone Sword"));
        list.add(new ShopEntry(15, "minecraft:iron_sword", 1, "minecraft:quartz", 64, "Iron Sword"));
        list.add(new ShopEntry(16, "minecraft:diamond_sword", 1, "minecraft:netherite_ingot", 6, "Diamond Sword"));
        list.add(new ShopEntry(17, "minecraft:stick", 1, "minecraft:gold_ingot", 4, "Knockback Stick",
                "KNOCKBACK_STICK"));

        list.add(new ShopEntry(18, "minecraft:wooden_pickaxe", 1, "minecraft:iron_ingot", 10, "Wood Pickaxe (Eff I)",
                "TOOL_PICKAXE", 1, "TOOL_PICKAXE"));
        list.add(new ShopEntry(18, "minecraft:stone_pickaxe", 1, "minecraft:iron_ingot", 10, "Stone Pickaxe (Eff I)",
                "TOOL_PICKAXE", 2, "TOOL_PICKAXE"));
        list.add(new ShopEntry(18, "minecraft:iron_pickaxe", 1, "minecraft:diamond", 2, "Iron Pickaxe (Eff II)",
                "TOOL_PICKAXE", 3, "TOOL_PICKAXE"));
        list.add(new ShopEntry(18, "minecraft:diamond_pickaxe", 1, "minecraft:emerald", 6, "Diamond Pickaxe (Eff III)",
                "TOOL_PICKAXE", 4, "TOOL_PICKAXE"));

        list.add(new ShopEntry(19, "minecraft:wooden_axe", 1, "minecraft:iron_ingot", 10, "Wood Axe (Eff I)",
                "TOOL_AXE", 1, "TOOL_AXE"));
        list.add(new ShopEntry(19, "minecraft:stone_axe", 1, "minecraft:iron_ingot", 10, "Stone Axe (Eff II)",
                "TOOL_AXE", 2, "TOOL_AXE"));
        list.add(new ShopEntry(19, "minecraft:iron_axe", 1, "minecraft:diamond", 2, "Iron Axe (Eff II)", "TOOL_AXE", 3,
                "TOOL_AXE"));
        list.add(new ShopEntry(19, "minecraft:diamond_axe", 1, "minecraft:emerald", 6, "Diamond Axe (Eff III)",
                "TOOL_AXE", 4, "TOOL_AXE"));

        list.add(
                new ShopEntry(20, "minecraft:potion", 1, "minecraft:emerald", 1, "Speed Potion (60s)", "POTION_SPEED"));
        list.add(new ShopEntry(21, "minecraft:potion", 1, "minecraft:emerald", 1, "Jump Boost Potion (60s)",
                "POTION_JUMP"));

        list.add(new ShopEntry(23, "minecraft:bow", 1, "minecraft:quartz", 32, "Bow", "BOW_NORMAL"));
        list.add(new ShopEntry(24, "minecraft:bow", 1, "minecraft:gold_ingot", 3, "Bow (Power I)", "BOW_POWER_1"));
        list.add(new ShopEntry(25, "minecraft:bow", 1, "minecraft:gold_ingot", 6, "Bow (Power I, Punch I)",
                "BOW_POWER_1_PUNCH_1"));
        list.add(new ShopEntry(26, "minecraft:arrow", 6, "minecraft:quartz", 6, "Arrows"));

        list.add(new ShopEntry(27, "minecraft:chainmail_boots", 1, "minecraft:iron_ingot", 32, "Chainmail Armor",
                "ARMOR_CHAINMAIL"));
        list.add(new ShopEntry(28, "minecraft:iron_boots", 1, "minecraft:diamond", 4, "Iron Armor", "ARMOR_IRON"));
        list.add(new ShopEntry(29, "minecraft:diamond_boots", 1, "minecraft:emerald", 10, "Diamond Armor",
                "ARMOR_DIAMOND"));

        list.add(new ShopEntry(32, "minecraft:potion", 1, "minecraft:netherite_ingot", 2, "Invisibility Potion (30s)",
                "POTION_INVISIBILITY"));
        list.add(new ShopEntry(33, "minecraft:tnt", 1, "minecraft:gold_ingot", 1, "Triggered TNT", "TNT_TRIGGERED"));
        list.add(new ShopEntry(34, "minecraft:ender_pearl", 1, "minecraft:netherite_ingot", 2, "Ender Pearl",
                "ENDER_PEARL"));

        list.add(new ShopEntry(47, "minecraft:furnace", 1, "minecraft:diamond", 4, "Forge Upgrade I", "UPGRADE_FORGE",
                1));
        list.add(new ShopEntry(47, "minecraft:blast_furnace", 1, "minecraft:diamond", 8, "Forge Upgrade II",
                "UPGRADE_FORGE", 2));
        list.add(new ShopEntry(47, "minecraft:ender_chest", 1, "minecraft:netherite_ingot", 4, "Forge Upgrade III",
                "UPGRADE_FORGE", 3));
        list.add(new ShopEntry(47, "minecraft:emerald_block", 1, "minecraft:gold_ingot", 16, "Forge Upgrade IV",
                "UPGRADE_FORGE", 4));

        list.add(new ShopEntry(48, "minecraft:iron_chestplate", 1, "minecraft:diamond", 5, "Reinforced Armor I",
                "UPGRADE_PROTECTION", 1));
        list.add(new ShopEntry(48, "minecraft:diamond_chestplate", 1, "minecraft:diamond", 10, "Reinforced Armor II",
                "UPGRADE_PROTECTION", 2));
        list.add(new ShopEntry(48, "minecraft:netherite_chestplate", 1, "minecraft:diamond", 20, "Reinforced Armor III",
                "UPGRADE_PROTECTION", 3));
        list.add(new ShopEntry(48, "minecraft:netherite_chestplate", 1, "minecraft:diamond", 30, "Reinforced Armor IV",
                "UPGRADE_PROTECTION", 4));

        list.add(new ShopEntry(50, "minecraft:golden_pickaxe", 1, "minecraft:gold_ingot", 4, "Maniac Miner I",
                "UPGRADE_HASTE", 1));
        list.add(new ShopEntry(50, "minecraft:diamond_pickaxe", 1, "minecraft:gold_ingot", 6, "Maniac Miner II",
                "UPGRADE_HASTE", 2));

        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8, "Sharpened Swords I",
                "UPGRADE_SHARPNESS", 1));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8, "Sharpened Swords II",
                "UPGRADE_SHARPNESS", 2));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8, "Sharpened Swords III",
                "UPGRADE_SHARPNESS", 3));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8, "Sharpened Swords IV",
                "UPGRADE_SHARPNESS", 4));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8, "Sharpened Swords V",
                "UPGRADE_SHARPNESS", 5));

        return list;
    }

    public static GameConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            INSTANCE = new GameConfig();
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            GameConfig loaded = gson.fromJson(reader, GameConfig.class);
            if (loaded != null) {
                INSTANCE = loaded;
                INSTANCE.ensureDefaults();
            } else {
                INSTANCE = new GameConfig();
            }
            save();
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            e.printStackTrace();
            if (INSTANCE == null)
                INSTANCE = new GameConfig();
        }
    }

    private void ensureDefaults() {
        if (arena1 == null)
            arena1 = new ArenaDetail("Iron");
        if (arena2 == null)
            arena2 = new ArenaDetail("Quartz");
        if (baseOffsets == null)
            baseOffsets = new BaseOffsets();
        if (arenaRestoreConfig == null)
            arenaRestoreConfig = new RestoreConfig();

        if (ironGenerator == null)
            ironGenerator = new GeneratorSetting(1, 0.5, 48);
        if (goldGenerator == null)
            goldGenerator = new GeneratorSetting(1, 1.0, 16);
        if (diamondGenerator == null)
            diamondGenerator = new GeneratorSetting(1, 1.0, 16);
        if (emeraldGenerator == null)
            emeraldGenerator = new GeneratorSetting(1, 10.0, 5);
        if (quartzGenerator == null)
            quartzGenerator = new GeneratorSetting(1, 0.5, 64);
        if (netheriteGenerator == null)
            netheriteGenerator = new GeneratorSetting(1, 10.0, 5);

        if (arena1DiamondGenerators == null)
            arena1DiamondGenerators = defaultDiamondOffsets();
        if (arena1EmeraldGenerators == null)
            arena1EmeraldGenerators = defaultEmeraldOffsets();
        if (arena2GoldGenerators == null)
            arena2GoldGenerators = defaultGoldOffsets();
        if (arena2NetheriteGenerators == null)
            arena2NetheriteGenerators = defaultNetheriteOffsets();

        if (shop == null)
            shop = defaultShop();

        if (forgeLevels == null)
            forgeLevels = defaultForgeLevels();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class RestoreConfig {
        public MapRegion arena1Bounds = new MapRegion(0, 85, 0, 100, 45, 100);
        public MapRegion arena2Bounds = new MapRegion(400, 85, 0, 100, 45, 100);
        public MapRegion arena1Template = new MapRegion(0, 85, 0, 100, 45, 100);
        public MapRegion arena2Template = new MapRegion(400, 85, -33, 100, 45, 100);
    }

    public static class MapRegion {
        public MapPoint center;
        public MapPoint radius;

        public MapRegion(int cx, int cy, int cz, int rx, int ry, int rz) {
            this.center = new MapPoint(cx, cy, cz);
            this.radius = new MapPoint(rx, ry, rz);
        }

        public MapPoint getMinPt() {
            return new MapPoint(center.x - radius.x, center.y - radius.y, center.z - radius.z);
        }

        public MapPoint getMaxPt() {
            return new MapPoint(center.x + radius.x, center.y + radius.y, center.z + radius.z);
        }
    }

    public static class MapPoint {
        public int x, y, z;

        public MapPoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
