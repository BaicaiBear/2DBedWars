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

        // Level 0 (Initial)
        // Iron: 3 items / 2s
        List<ForgeResource> l0 = new ArrayList<>();
        l0.add(new ForgeResource("minecraft:iron_ingot", 2.0, 3, 48));
        levels.add(new ForgeLevel(0, l0));

        // Level 1
        // Iron: 5 items / 2s
        List<ForgeResource> l1 = new ArrayList<>();
        l1.add(new ForgeResource("minecraft:iron_ingot", 2.0, 5, 64));
        levels.add(new ForgeLevel(1, l1));

        // Level 2
        // Iron: 5 items / 2s
        // Quartz: 1 item / 5s
        List<ForgeResource> l2 = new ArrayList<>();
        l2.add(new ForgeResource("minecraft:iron_ingot", 2.0, 5, 64));
        l2.add(new ForgeResource("minecraft:quartz", 5.0, 1, 12));
        levels.add(new ForgeLevel(2, l2));

        // Level 3
        // Iron: 5 items / 2s
        // Quartz: 2 items / 5s
        // Emerald: 1 item / 20s
        List<ForgeResource> l3 = new ArrayList<>();
        l3.add(new ForgeResource("minecraft:iron_ingot", 2.0, 5, 64));
        l3.add(new ForgeResource("minecraft:quartz", 5.0, 2, 24));
        l3.add(new ForgeResource("minecraft:emerald", 20.0, 1, 4));
        levels.add(new ForgeLevel(3, l3));

        // Level 4 (Supreme)
        // Iron: 4 items / 1s
        // Quartz: 4 items / 5s
        // Emerald: 1 item / 20s
        List<ForgeResource> l4 = new ArrayList<>();
        l4.add(new ForgeResource("minecraft:iron_ingot", 1.0, 4, 64));
        l4.add(new ForgeResource("minecraft:quartz", 5.0, 4, 48));
        l4.add(new ForgeResource("minecraft:emerald", 20.0, 1, 6));
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
        public String type; // Optional, used for public levels
        public int amount;
        public double delaySeconds;
        public int limit;

        public GeneratorSetting(int amount, double delaySeconds, int limit) {
            this.amount = amount;
            this.delaySeconds = delaySeconds;
            this.limit = limit;
        }

        public GeneratorSetting(String type, int amount, double delaySeconds, int limit) {
            this.type = type;
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

        list.add(new ShopEntry(0, "minecraft:white_wool", 16, "minecraft:iron_ingot", 8, null));
        list.add(new ShopEntry(1, "minecraft:terracotta", 16, "minecraft:iron_ingot", 12, null));
        list.add(new ShopEntry(2, "minecraft:ladder", 16, "minecraft:iron_ingot", 4, null));
        list.add(new ShopEntry(3, "minecraft:oak_planks", 16, "minecraft:iron_ingot", 16, null));

        list.add(new ShopEntry(5, "minecraft:glass", 8, "minecraft:quartz", 12,
                "two-dimensional-bedwars.shop.item.blast_proof_glass", "BLAST_PROOF_GLASS"));
        list.add(new ShopEntry(6, "minecraft:end_stone", 12, "minecraft:quartz", 24, null));
        list.add(new ShopEntry(7, "minecraft:obsidian", 4, "minecraft:netherite_ingot", 4, null));
        list.add(new ShopEntry(8, "minecraft:golden_apple", 1, "minecraft:gold_ingot", 2,
                "two-dimensional-bedwars.shop.item.golden_apple", "GOLDEN_APPLE"));

        list.add(new ShopEntry(9, "minecraft:water_bucket", 1, "minecraft:iron_ingot", 12,
                "two-dimensional-bedwars.shop.item.water_bucket", "WATER_BUCKET"));
        list.add(new ShopEntry(10, "minecraft:sponge", 4, "minecraft:iron_ingot", 48,
                "two-dimensional-bedwars.shop.item.sponge", "SPONGE"));
        list.add(new ShopEntry(11, "minecraft:fire_charge", 1, "minecraft:iron_ingot", 32,
                "two-dimensional-bedwars.shop.item.fireball", "FIREBALL"));
        list.add(new ShopEntry(12, "minecraft:egg", 1, "minecraft:emerald", 1,
                "two-dimensional-bedwars.shop.item.bridge_egg", "BRIDGE_EGG"));

        list.add(new ShopEntry(14, "minecraft:stone_sword", 1, "minecraft:quartz", 10,
                "two-dimensional-bedwars.shop.item.stone_sword", "TOOL_SWORD", 2, "TOOL_SWORD"));
        list.add(new ShopEntry(14, "minecraft:iron_sword", 1, "minecraft:quartz", 64,
                "two-dimensional-bedwars.shop.item.iron_sword", "TOOL_SWORD", 3, "TOOL_SWORD"));
        list.add(new ShopEntry(14, "minecraft:diamond_sword", 1, "minecraft:netherite_ingot", 6,
                "two-dimensional-bedwars.shop.item.diamond_sword", "TOOL_SWORD", 4, "TOOL_SWORD"));
        list.add(new ShopEntry(15, "minecraft:stick", 1, "minecraft:gold_ingot", 4,
                "two-dimensional-bedwars.shop.item.knockback_stick", "KNOCKBACK_STICK"));
        list.add(new ShopEntry(16, "minecraft:shears", 1, "minecraft:quartz", 20, null, "SHEARS"));
        list.add(new ShopEntry(17, "minecraft:cherry_leaves", 16, "minecraft:quartz", 4, null));

        list.add(new ShopEntry(18, "minecraft:wooden_pickaxe", 1, "minecraft:iron_ingot", 10,
                "two-dimensional-bedwars.shop.item.wood_pickaxe_1", "TOOL_PICKAXE", 1, "TOOL_PICKAXE"));
        list.add(new ShopEntry(18, "minecraft:stone_pickaxe", 1, "minecraft:iron_ingot", 10,
                "two-dimensional-bedwars.shop.item.stone_pickaxe_2", "TOOL_PICKAXE", 2, "TOOL_PICKAXE"));
        list.add(new ShopEntry(18, "minecraft:iron_pickaxe", 1, "minecraft:diamond", 2,
                "two-dimensional-bedwars.shop.item.iron_pickaxe_3", "TOOL_PICKAXE", 3, "TOOL_PICKAXE"));
        list.add(new ShopEntry(18, "minecraft:diamond_pickaxe", 1, "minecraft:emerald", 6,
                "two-dimensional-bedwars.shop.item.diamond_pickaxe_4", "TOOL_PICKAXE", 4, "TOOL_PICKAXE"));

        list.add(new ShopEntry(19, "minecraft:wooden_axe", 1, "minecraft:iron_ingot", 10,
                "two-dimensional-bedwars.shop.item.wood_axe_1", "TOOL_AXE", 1, "TOOL_AXE"));
        list.add(new ShopEntry(19, "minecraft:stone_axe", 1, "minecraft:iron_ingot", 10,
                "two-dimensional-bedwars.shop.item.stone_axe_2", "TOOL_AXE", 2, "TOOL_AXE"));
        list.add(new ShopEntry(19, "minecraft:iron_axe", 1, "minecraft:diamond", 2,
                "two-dimensional-bedwars.shop.item.iron_axe_3", "TOOL_AXE", 3, "TOOL_AXE"));
        list.add(new ShopEntry(19, "minecraft:diamond_axe", 1, "minecraft:emerald", 6,
                "two-dimensional-bedwars.shop.item.diamond_axe_4", "TOOL_AXE", 4, "TOOL_AXE"));

        list.add(new ShopEntry(20, "minecraft:potion", 1, "minecraft:emerald", 1,
                "two-dimensional-bedwars.shop.potion.speed", "POTION_SPEED"));
        list.add(new ShopEntry(21, "minecraft:potion", 1, "minecraft:emerald", 1,
                "two-dimensional-bedwars.shop.potion.jump", "POTION_JUMP"));

        list.add(new ShopEntry(23, "minecraft:bow", 1, "minecraft:quartz", 32, null, "BOW_NORMAL"));
        list.add(new ShopEntry(24, "minecraft:bow", 1, "minecraft:gold_ingot", 3, null, "BOW_POWER_1"));
        list.add(new ShopEntry(25, "minecraft:bow", 1, "minecraft:gold_ingot", 6, null, "BOW_POWER_1_PUNCH_1"));
        list.add(new ShopEntry(26, "minecraft:arrow", 6, "minecraft:quartz", 6, null));

        list.add(new ShopEntry(27, "minecraft:chainmail_boots", 1, "minecraft:iron_ingot", 32,
                "two-dimensional-bedwars.shop.upgrade.protection", "ARMOR_CHAINMAIL"));
        list.add(new ShopEntry(28, "minecraft:iron_boots", 1, "minecraft:diamond", 4,
                "two-dimensional-bedwars.shop.upgrade.protection", "ARMOR_IRON"));
        list.add(new ShopEntry(29, "minecraft:diamond_boots", 1, "minecraft:emerald", 10,
                "two-dimensional-bedwars.shop.upgrade.protection", "ARMOR_DIAMOND"));

        list.add(new ShopEntry(32, "minecraft:potion", 1, "minecraft:netherite_ingot", 2,
                "two-dimensional-bedwars.shop.potion.invis", "POTION_INVISIBILITY"));
        list.add(new ShopEntry(33, "minecraft:tnt", 1, "minecraft:gold_ingot", 1,
                "two-dimensional-bedwars.shop.item.tnt", "TNT_TRIGGERED"));
        list.add(new ShopEntry(34, "minecraft:ender_pearl", 1, "minecraft:netherite_ingot", 2, null,
                "ENDER_PEARL"));

        list.add(new ShopEntry(47, "minecraft:furnace", 1, "minecraft:diamond", 4,
                "two-dimensional-bedwars.shop.upgrade.forge", "UPGRADE_FORGE", 1));
        list.add(new ShopEntry(47, "minecraft:blast_furnace", 1, "minecraft:diamond", 8,
                "two-dimensional-bedwars.shop.upgrade.forge", "UPGRADE_FORGE", 2));
        list.add(new ShopEntry(47, "minecraft:ender_chest", 1, "minecraft:netherite_ingot", 4,
                "two-dimensional-bedwars.shop.upgrade.forge", "UPGRADE_FORGE", 3));
        list.add(new ShopEntry(47, "minecraft:emerald_block", 1, "minecraft:gold_ingot", 16,
                "two-dimensional-bedwars.shop.upgrade.forge", "UPGRADE_FORGE", 4));

        list.add(new ShopEntry(48, "minecraft:iron_chestplate", 1, "minecraft:diamond", 5,
                "two-dimensional-bedwars.shop.upgrade.protection", "UPGRADE_PROTECTION", 1));
        list.add(new ShopEntry(48, "minecraft:diamond_chestplate", 1, "minecraft:diamond", 10,
                "two-dimensional-bedwars.shop.upgrade.protection", "UPGRADE_PROTECTION", 2));
        list.add(new ShopEntry(48, "minecraft:netherite_chestplate", 1, "minecraft:diamond", 20,
                "two-dimensional-bedwars.shop.upgrade.protection", "UPGRADE_PROTECTION", 3));
        list.add(new ShopEntry(48, "minecraft:netherite_chestplate", 1, "minecraft:diamond", 30,
                "two-dimensional-bedwars.shop.upgrade.protection", "UPGRADE_PROTECTION", 4));

        list.add(new ShopEntry(50, "minecraft:golden_pickaxe", 1, "minecraft:gold_ingot", 4,
                "two-dimensional-bedwars.shop.upgrade.haste", "UPGRADE_HASTE", 1));
        list.add(new ShopEntry(50, "minecraft:diamond_pickaxe", 1, "minecraft:gold_ingot", 6,
                "two-dimensional-bedwars.shop.upgrade.haste", "UPGRADE_HASTE", 2));

        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8,
                "two-dimensional-bedwars.shop.upgrade.sharpness", "UPGRADE_SHARPNESS", 1));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8,
                "two-dimensional-bedwars.shop.upgrade.sharpness", "UPGRADE_SHARPNESS", 2));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8,
                "two-dimensional-bedwars.shop.upgrade.sharpness", "UPGRADE_SHARPNESS", 3));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8,
                "two-dimensional-bedwars.shop.upgrade.sharpness", "UPGRADE_SHARPNESS", 4));
        list.add(new ShopEntry(51, "minecraft:iron_sword", 1, "minecraft:gold_ingot", 8,
                "two-dimensional-bedwars.shop.upgrade.sharpness", "UPGRADE_SHARPNESS", 5));

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
        if (publicGeneratorLevels == null)
            publicGeneratorLevels = defaultPublicGeneratorLevels();
        if (eventSettings == null)
            eventSettings = new EventSettings();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public EventSettings eventSettings = new EventSettings();

    public static class EventSettings {
        public int diamondIISeconds = 360; // 6 mins
        public int emeraldIISeconds = 720; // 12 mins
        public int diamondIIISeconds = 1080; // 18 mins
        public int emeraldIIISeconds = 1440; // 24 mins
        public int bedDestructionSeconds = 1800; // 30 mins
        public int suddenDeathSeconds = 2160; // 36 mins
        public int gameEndSeconds = 2520; // 42 mins
    }

    // Public Generator Levels (Diamond/Emerald etc upgrades)
    public List<PublicGeneratorLevel> publicGeneratorLevels = defaultPublicGeneratorLevels();

    public static class PublicGeneratorLevel {
        public int level; // 1, 2, 3
        public List<GeneratorSetting> settings; // Settings for each type (Diamond, Emerald, Gold, Netherite)

        public PublicGeneratorLevel(int level, List<GeneratorSetting> settings) {
            this.level = level;
            this.settings = settings;
        }
    }

    private static List<PublicGeneratorLevel> defaultPublicGeneratorLevels() {
        List<PublicGeneratorLevel> levels = new ArrayList<>();

        // Level 1 (Initial)
        // Diamond/Gold: 30s
        // Emerald/Netherite: 55s
        List<GeneratorSetting> l1 = new ArrayList<>();
        l1.add(new GeneratorSetting("Diamond", 1, 30.0, 4));
        l1.add(new GeneratorSetting("Emerald", 1, 55.0, 2));
        l1.add(new GeneratorSetting("Gold", 1, 30.0, 12));
        l1.add(new GeneratorSetting("Netherite", 1, 55.0, 2));
        levels.add(new PublicGeneratorLevel(1, l1));

        // Level 2 (Diamond II / Emerald II)
        // Diamond/Gold: 23s
        // Emerald/Netherite: 40s
        List<GeneratorSetting> l2 = new ArrayList<>();
        l2.add(new GeneratorSetting("Diamond", 1, 23.0, 4));
        l2.add(new GeneratorSetting("Emerald", 1, 40.0, 2));
        l2.add(new GeneratorSetting("Gold", 1, 23.0, 12));
        l2.add(new GeneratorSetting("Netherite", 1, 40.0, 2));
        levels.add(new PublicGeneratorLevel(2, l2));

        // Level 3 (Diamond III / Emerald III)
        // Diamond/Gold: 15s
        // Emerald/Netherite: 30s
        List<GeneratorSetting> l3 = new ArrayList<>();
        l3.add(new GeneratorSetting("Diamond", 1, 15.0, 4));
        l3.add(new GeneratorSetting("Emerald", 1, 30.0, 2));
        l3.add(new GeneratorSetting("Gold", 1, 15.0, 12));
        l3.add(new GeneratorSetting("Netherite", 1, 30.0, 2));
        levels.add(new PublicGeneratorLevel(3, l3));

        return levels;
    }

    // ... existing restore config classes ...

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
