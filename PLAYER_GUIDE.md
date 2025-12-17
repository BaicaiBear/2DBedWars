# 2-Dimensional BedWars: Player Guide

Welcome to **2-Dimensional BedWars**, a unique twist on the classic minigame where you must defend bases in two parallel dimensions simultaneously!

## 🔑 Key Differences from Traditional BedWars

*   **Dual Arenas**: You have bases in TWO dimensions (Overworld & Nether)
*   **Death Swap**: Dying swaps you to the other dimension
*   **Split Resources**: Different resources in each arena
*   **Both Beds Matter**: Teams aren't eliminated until BOTH beds are destroyed
*   **Portal Travel**: Instant teleportation between your bases

## 🌌 The Concept: Split Worlds
The game takes place in a world split into two distinct arenas. You are not just fighting on one front; you are fighting on two.

-   **Arena 1 (The Overworld)**: A desert-themed arena located at coordinates around `X=0, Z=0`. Produces Iron, Diamond, and Emerald resources.
-   **Arena 2 (The Nether)**: A hellish landscape located at coordinates around `X=400, Z=0`. Produces Quartz, Gold, and Netherite resources.

**Every team has a base and a bed in BOTH arenas.** To win, you must destroy the beds of your enemies in **both dimensions**.

### The Four Teams

*   **Red Team**: Spawns at positive X coordinates
*   **Blue Team**: Spawns at negative X coordinates  
*   **Green Team**: Spawns at positive Z coordinates
*   **Yellow Team**: Spawns at negative Z coordinates

Each team has symmetrical spawns in both arenas, with Nether Portals connecting their bases.

## 🎮 Getting Started

### Joining a Game

1.   **Join the lobby**: Use `/bedwars join` to enter the waiting area
2.   **Select your team**: Use `/bedwars team <id>` where:
     *   `1` = Red Team
     *   `2` = Blue Team
     *   `3` = Green Team
     *   `4` = Yellow Team
3.   **Wait for start**: Any player can use `/bedwars start` to begin the countdown
4.   **Game begins**: After map restoration, you'll be teleported to your base

### Pre-Game Rules

*   Teams can have **1-8 players** each
*   At least **one team** must have a player to start
*   You can **change teams** before the game starts
*   Once the game begins, team selection is **locked**
*   The lobby is in a different dimension - you'll be teleported to the arena

## ⚔️ Gameplay Mechanics

### 1. Death Swap & Respawn System
Using a custom respawn mechanic, **death is a portal**.
-   If you die in the **Overworld**, you will respawn in the **Nether**.
-   If you die in the **Nether**, you will respawn in the **Overworld**.

**Respawn Rules:**
*   **5-second cooldown**: After death, you wait 5 seconds before respawning
*   **Target bed must exist**: If your bed in the target dimension is destroyed, you cannot respawn there
*   **Both beds destroyed**: If both of your team's beds are destroyed, your next death is permanent (you become a spectator)
*   **Team elimination**: A team is eliminated when both beds are destroyed AND all players are dead

This mechanic allows strategic dimension switching but requires defending both bases!

### 2. Portal Travel
Vanilla Nether Portals are your gateways. Walking into a portal will instantly teleport you between your team's Overworld base and Nether base. 

**Portal Mechanics:**
*   **Instant Travel**: No loading screen - immediate teleportation
*   **Directional**: Portals only connect between your team's two bases
*   **Safe Escape**: Use portals to flee from attackers or reposition quickly
*   **Resource Collection**: Travel to gather resources from both dimensions
*   **Bed Requirement**: Cannot teleport to a dimension if your bed there is destroyed
*   **Tactical Uses**: Coordinate multi-front attacks or defend under pressure

**Pro Tip**: Keep your portals accessible and defended - they're essential for map mobility!

### Map Restoration System

Before each game starts, the arenas are automatically restored:

*   **Automatic Cleanup**: All player-placed blocks are removed
*   **Original Terrain**: Map resets to its original state from the blueprint dimension
*   **Async Process**: Restoration happens in chunks to avoid lag
*   **Progress Indicator**: You'll see "Waiting for Map Restoration..." before game start
*   **Countdown**: Once restored, a countdown begins before the game starts

This ensures every game starts with a fresh, clean arena!

### Scoreboard System

During the game, a sidebar scoreboard displays important information:

*   **Game Title**: "2D BEDWARS" at the top
*   **Next Event**: Shows the upcoming game event and countdown timer
*   **Team Status**: For each team:
    *   Team name and color
    *   Bed status: ✓ (intact) or ✗ (destroyed) for each arena
    *   Number of alive players
*   **Your Stats**: Personal kill/death statistics

The scoreboard updates in real-time to keep you informed of the match status!

### 3. Resource Split
Resources are distributed between the dimensions to force cross-dimensional play. You cannot camp in one world forever if you want the best gear.

#### **Overworld (Arena 1)**
-   **Iron**: Basic currency for blocks and tools.
-   **Diamond**: Currency for Team Upgrades.
-   **Emerald**: High-tier currency for powerful items.

#### **The Nether (Arena 2)**
-   **Quartz**: Alternate currency found in the Nether (replaces Iron).
-   **Gold**: Currency for better equipment.
-   **Netherite**: Ultra-rare currency for the strongest gear.

## 🏆 How to Win
1.  Protect your **Red/Blue/Green/Yellow** beds in both dimensions.
2.  Gather resources from both the Overworld and Nether.
3.  Travel between worlds to outmaneuver your opponents.
4.  Destroy both beds of an enemy team to eliminate them!

## 📊 Technical Details

### Team Forge Levels
Upgrade your team's generator to produce resources faster.

| Level | Iron (Overworld) | Quartz (Nether) | Emerald (Overworld) | Cost        |
| :---- | :--------------- | :-------------- | :------------------ | :---------- |
| **0** | 3 / 2s           | -               | -                   | *Free*      |
| **1** | 5 / 2s           | -               | -                   | 4 Diamonds  |
| **2** | 5 / 2s           | 1 / 5s          | -                   | 8 Diamonds  |
| **3** | 5 / 2s           | 2 / 5s          | 1 / 20s             | 4 Netherite |
| **4** | 4 / 1s           | 4 / 5s          | 1 / 20s             | 16 Gold     |

### Public Generator Rates
Resources spawn at public locations in the middle of each arena.

| Tier    | Diamond / Gold | Emerald / Netherite |
| :------ | :------------- | :------------------ |
| **I**   | 30s            | 55s                 |
| **II**  | 23s            | 40s                 |
| **III** | 15s            | 30s                 |

### ⏳ Game Events Timeline

Events occur at specific times during the match to speed up gameplay and force action.

| Time     | Event                  | Effect                                                        |
| :------- | :--------------------- | :------------------------------------------------------------ |
| **0:00** | **Game Start**         | Forge at Level 0: Iron 3/2s                                   |
| **6:00** | **Diamond II**         | Diamond & Gold generators upgrade to 23s spawn rate           |
| **12:00**| **Emerald II**         | Emerald & Netherite generators upgrade to 40s spawn rate      |
| **18:00**| **Diamond III**        | Diamond & Gold generators upgrade to 15s spawn rate (MAX)     |
| **24:00**| **Emerald III**        | Emerald & Netherite generators upgrade to 30s spawn rate (MAX)|
| **30:00**| **Bed Destruction**    | 🔥 ALL remaining beds are destroyed automatically!            |
| **36:00**| **Sudden Death**       | ⚡ Charged creepers spawn continuously in both arenas!        |
| **42:00**| **Game End (Draw)**    | Game ends with no winner if teams still remain                |

**Strategy Notes:**
*   Rush for early kills before Diamond II (first 6 minutes)
*   Collect emeralds between 12-18 minutes for powerful items
*   After 30 minutes, **every death is permanent** - play cautiously!
*   During Sudden Death (36m+), charged creepers are extremely dangerous

## 🛒 Complete Shop Guide

### 🧱 Building Blocks

| Item                    | Cost         | Notes                                                  |
| :---------------------- | :----------- | :----------------------------------------------------- |
| **Wool** (16)           | 8 Iron       | Quick and cheap building material                      |
| **Terracotta** (16)     | 12 Iron      | More durable than wool                                 |
| **Ladder** (16)         | 4 Iron       | Essential for vertical movement                        |
| **Wood Planks** (16)    | 16 Iron      | Balanced cost and durability                           |
| **Cherry Leaves** (16)  | 4 Quartz     | Camouflage blocks available in Nether                  |
| **Blast-Proof Glass** (8) | 12 Quartz | **Special**: 100% immune to ALL explosions (including Sudden Death!) |
| **End Stone** (12)      | 24 Quartz    | Strong defense block                                   |
| **Obsidian** (4)        | 4 Netherite  | Nearly indestructible, excellent bed defense           |

### ⚔️ Weapons & Combat

| Item                  | Cost         | Notes                                          |
| :-------------------- | :----------- | :--------------------------------------------- |
| **Stone Sword**       | 10 Quartz    | Tier 2 weapon (upgrades your wooden sword)     |
| **Iron Sword**        | 64 Quartz    | Tier 3 weapon (upgrades previous sword)        |
| **Diamond Sword**     | 6 Netherite  | Tier 4 weapon (upgrades previous sword)        |
| **Knockback Stick**   | 4 Gold       | Stick with Knockback enchantment for pushing   |
| **Bow**               | 32 Quartz    | Basic ranged weapon                            |
| **Bow (Power I)**     | 3 Gold       | Enhanced damage bow                            |
| **Bow (Pow I, Pun I)**| 6 Gold       | Power + Punch combo bow                        |
| **Arrow** (6)         | 6 Quartz     | Ammunition for bows                            |

**Note**: You start with a wooden sword. Purchasing swords auto-upgrades your current sword!

### 🛡️ Armor (Must Repurchase)

| Item                | Cost        | Effect                                            |
| :------------------ | :---------- | :------------------------------------------------ |
| **Chainmail Armor** | 32 Iron     | Basic protection set                              |
| **Iron Armor**      | 4 Diamond   | Better protection                                 |
| **Diamond Armor**   | 10 Emerald  | Best armor set                                    |

**Important**: Armor **vanishes on death**! You must repurchase it after respawning.

### 🛠️ Tools (Progressive Upgrades)

**Pickaxes** (Progressive tiers - each unlocks the next):
*   **Wood Pickaxe (Efficiency I)** - Tier 1: 10 Iron
*   **Stone Pickaxe (Efficiency I)** - Tier 2: 10 Iron (auto-upgrade previous)
*   **Iron Pickaxe (Efficiency II)** - Tier 3: 2 Diamond (auto-upgrade previous)
*   **Diamond Pickaxe (Efficiency III)** - Tier 4: 6 Emerald (auto-upgrade previous)

**Axes** (Progressive tiers):
*   **Wood Axe (Efficiency I)** - Tier 1: 10 Iron
*   **Stone Axe (Efficiency II)** - Tier 2: 10 Iron (auto-upgrade previous)
*   **Iron Axe (Efficiency II)** - Tier 3: 2 Diamond (auto-upgrade previous)
*   **Diamond Axe (Efficiency III)** - Tier 4: 6 Emerald (auto-upgrade previous)

**Other Tools**:
*   **Shears**: 20 Quartz - Break blocks faster (leaves, wool, etc.)

**Important**: Tools automatically upgrade when you purchase a higher tier! You don't need to keep the old tool.

### 🧪 Potions & Consumables

| Item                         | Cost        | Effect                               |
| :--------------------------- | :---------- | :----------------------------------- |
| **Golden Apple**             | 2 Gold      | Instant healing + absorption         |
| **Speed Potion** (45s)       | 1 Emerald   | Speed II for fast movement           |
| **Jump Boost Potion** (45s)  | 1 Emerald   | Jump Boost IV for high jumps         |
| **Invisibility Potion** (30s)| 2 Netherite | Turn invisible for stealth attacks   |

### 🧨 Special Items

| Item                 | Cost        | Special Mechanics                                           |
| :------------------- | :---------- | :---------------------------------------------------------- |
| **Bridge Egg**       | 1 Emerald   | Throws egg that builds a **colored wool bridge** (matches your team) in its path      |
| **Fireball**         | 32 Iron     | Launches a ghast-like explosive fireball                    |
| **TNT**              | 1 Gold      | Auto-ignites when placed (4-second fuse)                    |
| **Ender Pearl**      | 2 Netherite | Teleport to where it lands                                  |
| **Water Bucket**     | 12 Iron     | Reusable water source for MLG saves                         |
| **Sponge** (4)       | 48 Iron     | Remove water quickly                                        |

### 💎 Team Upgrades (Shared)

**Forge Upgrade** (Increases base generator production):
*   **Level 1**: 4 Diamond - Iron: 5/2s
*   **Level 2**: 8 Diamond - Iron: 5/2s, Quartz: 1/5s
*   **Level 3**: 4 Netherite - Iron: 5/2s, Quartz: 2/5s, Emerald: 1/20s
*   **Level 4**: 16 Gold - Iron: 4/1s, Quartz: 4/5s, Emerald: 1/20s (MAX)

**Protection** (Team armor enchantment):
*   **Level 1**: 5 Diamond - Protection I on all armor
*   **Level 2**: 10 Diamond - Protection II
*   **Level 3**: 20 Diamond - Protection III  
*   **Level 4**: 30 Diamond - Protection IV (MAX)

**Sharpness** (Team weapon enchantment):
*   **Levels 1-5**: 8 Gold each - Adds Sharpness I-V to all swords

**Haste** (Mining speed boost):
*   **Level 1**: 4 Gold - Haste I effect
*   **Level 2**: 6 Gold - Haste II effect (MAX)

**Team Upgrades are permanent for the entire team once purchased!**

## ❓ Frequently Asked Questions

### Q: Can I break my own bed?
**A:** No! The game prevents you from breaking your team's beds. Only enemies can destroy them.

### Q: What happens to my items when I die?
**A:** 
*   **Currency drops**: All resources (iron, gold, diamond, emerald, quartz, netherite) drop on the ground
*   **Inventory cleared**: All other items vanish (blocks, consumables, arrows, special items)
*   **Armor vanishes**: All armor is removed - you must repurchase armor after respawning
*   **Tools downgrade**: Sword, pickaxe, and axe each downgrade by 1 tier (e.g., iron sword → stone sword)
*   **Shears persist**: If you owned shears, you keep them on respawn
*   **Minimum tier**: Tools won't downgrade below tier 1 (wooden pickaxe/axe, wooden sword)

### Q: Can I change teams after the game starts?
**A:** No. Team selection is locked once the game begins. Choose carefully!

### Q: How do I access the Team Ender Chest?
**A:** The game provides a team ender chest accessible by all team members. Items placed inside are shared across the entire team.

### Q: What's the fastest way to get Netherite?
**A:** Visit the Nether arena's central Netherite generators. They spawn every 55s (Tier I), 40s (Tier II), or 30s (Tier III).

### Q: Can I place crafting tables or furnaces?
**A:** No, crafting is **completely disabled** in-game. The crafting grid won't work. All items must be purchased from the shop.

### Q: Can I sleep in beds during the game?
**A:** No, you cannot sleep in beds or set spawn points. Respawn points are fixed at your team's bases.

### Q: Does the Forge upgrade affect public generators?
**A:** No. Forge upgrades only affect your team's base generators. Public generators upgrade automatically based on game events.

### Q: Can I break map blocks?
**A:** No. You can only break blocks that players have placed. Original map blocks are protected.

### Q: What happens during Sudden Death?
**A:** After 36 minutes, **charged creepers** continuously spawn in both arenas. These are powered creepers with massive explosion radius - extremely dangerous! During Sudden Death, creepers can destroy **ALL blocks**, even map blocks that are normally protected!

### Q: How does explosion protection work?
**A:** 
*   **Map blocks**: Cannot be destroyed by TNT or fireballs (protected)
*   **Player-placed blocks**: Can be destroyed by explosions
*   **Blast-Proof Glass**: NEVER destroyed by any explosion (special)
*   **Beds**: Can be destroyed by explosions
*   **Sudden Death Exception**: During Sudden Death, charged creepers can destroy everything including map blocks!

### Q: How do team upgrades work?
**A:** Team upgrades (Sharpness, Protection, Haste, Forge) are **permanent** once purchased and apply to **all team members** instantly. Multiple teammates can contribute to buying them.

### Q: Can I rejoin if I leave the game?
**A:** If you leave during an active match, you'll need to confirm the command twice. Leaving may result in team elimination if you're the last member. You **cannot rejoin** a game in progress - you can only spectate.

### Q: How many players can be on each team?
**A:** Each team can have up to **8 players**. The game supports 4 teams (Red, Blue, Green, Yellow) for a maximum of 32 players total.

### Q: What's the minimum number of players to start?
**A:** Each team that's participating must have **at least 1 player**. You can play with as few as 2 players (1 vs 1) or as many as 32 players (8v8v8v8).

### Q: What's the best resource priority?
**A:** 
1.   **Iron/Quartz**: Building blocks, basic tools, and daily purchases
2.   **Diamond**: Team forge upgrades and protection upgrades (highest priority!)
3.   **Gold**: Combat items (TNT, Golden Apples, Sharpness upgrades)
4.   **Emerald**: Special items (Bridge Egg, Speed Potions, Diamond Armor)
5.   **Netherite**: Ultimate items (Diamond Sword, Ender Pearl, Invisibility)

### Q: How many generators are there?
**A:** 
*   **Team Generators**: Each team has 2 generators (one in each arena) that produce Iron/Quartz based on Forge level
*   **Diamond Generators**: 4 generators in Overworld arena (shared)
*   **Gold Generators**: 4 generators in Nether arena (shared)
*   **Emerald Generators**: 2 generators in Overworld arena (shared)
*   **Netherite Generators**: 2 generators in Nether arena (shared)

---

**Good luck in the arenas! May your beds stay intact and your enemies' beds crumble! 🛏️⚔️**

