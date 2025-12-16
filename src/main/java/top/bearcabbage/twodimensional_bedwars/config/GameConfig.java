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

    public List<ShopEntry> ironShop = defaultIronShop();
    public List<ShopEntry> goldShop = defaultGoldShop();

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

    private static List<Offset> defaultOffsets(int count, int x, int y, int z) {
        // Deprecated helper, keeping if needed or removing
        List<Offset> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int mX = (i % 2 == 0) ? x : -x;
            int mZ = (i < 2) ? z : -z;
            list.add(new Offset(mX, y, mZ));
        }
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
        public String type; // ITEM, UPGRADE_SHARPNESS, UPGRADE_PROTECTION, UPGRADE_HASTE, UPGRADE_FORGE
        public int upgradeTier;

        public ShopEntry(int slot, String itemId, int count, String costId, int price, String name) {
            this(slot, itemId, count, costId, price, name, "ITEM", 0);
        }

        public ShopEntry(int slot, String itemId, int count, String costId, int price, String name, String type,
                int upgradeTier) {
            this.slot = slot;
            this.itemId = itemId;
            this.count = count;
            this.costId = costId;
            this.price = price;
            this.name = name;
            this.type = type;
            this.upgradeTier = upgradeTier;
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

    private static List<ShopEntry> defaultIronShop() {
        List<ShopEntry> list = new ArrayList<>();
        list.add(new ShopEntry(10, "minecraft:white_wool", 16, "minecraft:iron_ingot", 4, "White Wool"));
        list.add(new ShopEntry(11, "minecraft:oak_planks", 16, "minecraft:gold_ingot", 4, "Oak Planks"));
        list.add(new ShopEntry(13, "minecraft:stone_sword", 1, "minecraft:iron_ingot", 10, "Stone Sword"));
        list.add(new ShopEntry(14, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 7, "Iron Sword"));
        list.add(new ShopEntry(16, "minecraft:golden_apple", 1, "minecraft:gold_ingot", 3, "Golden Apple"));
        return list;
    }

    private static List<ShopEntry> defaultGoldShop() {
        List<ShopEntry> list = new ArrayList<>();
        // Items
        list.add(new ShopEntry(10, "minecraft:white_wool", 64, "minecraft:iron_ingot", 8, "Mega Wool"));
        list.add(new ShopEntry(11, "minecraft:obsidian", 4, "minecraft:gold_ingot", 8, "Obsidian"));
        list.add(new ShopEntry(13, "minecraft:diamond_sword", 1, "minecraft:gold_ingot", 5, "Diamond Sword"));

        // Upgrades (Diamond Currency)
        // Sharpness
        list.add(new ShopEntry(19, "minecraft:iron_sword", 1, "minecraft:diamond", 4, "Sharpness I",
                "UPGRADE_SHARPNESS", 1));

        // Protection
        list.add(new ShopEntry(20, "minecraft:iron_chestplate", 1, "minecraft:diamond", 2, "Protection I",
                "UPGRADE_PROTECTION", 1));
        list.add(new ShopEntry(20, "minecraft:diamond_chestplate", 1, "minecraft:diamond", 4, "Protection II",
                "UPGRADE_PROTECTION", 2));
        list.add(new ShopEntry(20, "minecraft:netherite_chestplate", 1, "minecraft:diamond", 8, "Protection III",
                "UPGRADE_PROTECTION", 3));

        // Haste
        list.add(new ShopEntry(21, "minecraft:golden_pickaxe", 1, "minecraft:diamond", 4, "Haste I", "UPGRADE_HASTE",
                1));
        list.add(new ShopEntry(21, "minecraft:diamond_pickaxe", 1, "minecraft:diamond", 6, "Haste II", "UPGRADE_HASTE",
                2));

        // Forge
        list.add(new ShopEntry(22, "minecraft:furnace", 1, "minecraft:diamond", 4, "Forge +50%", "UPGRADE_FORGE", 1));
        list.add(new ShopEntry(22, "minecraft:blast_furnace", 1, "minecraft:diamond", 8, "Forge +100%", "UPGRADE_FORGE",
                2));
        list.add(new ShopEntry(22, "minecraft:emerald_block", 1, "minecraft:diamond", 12, "Forge Emeralds",
                "UPGRADE_FORGE", 3));

        return list;
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
                // Fix potential nulls from Gson not calling constructor
                INSTANCE.ensureDefaults();
            } else {
                INSTANCE = new GameConfig(); // Empty file case
            }
            save(); // Write back missing/defaults
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            e.printStackTrace();
            if (INSTANCE == null)
                INSTANCE = new GameConfig(); // Keep memory safe
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

        if (ironShop == null)
            ironShop = defaultIronShop();
        if (goldShop == null)
            goldShop = defaultGoldShop();

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
}
