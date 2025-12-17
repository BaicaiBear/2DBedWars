# 2-Dimensional BedWars Mod

> A server-side Fabric mod that reinvents BedWars with a dual-dimension twist.

![Icon](src/main/resources/assets/two-dimensional-bedwars/icon.png)

## 🌌 Overview

**2-Dimensional BedWars** challenges players to defend two bases simultaneously in parallel dimensions.
The game world is split into two halves:
1.  **Overworld Arena (Desert)**: `x < 200`
2.  **Nether Arena (Hell)**: `x >= 200`

### Key Features
*   **Dual Bases**: Every team has a bed in both the Overworld and the Nether. You must protect **both** to survive.
*   **Death Swap**: Dying in one dimension respawns you in the other.
*   **Resource Split**:
    *   **Overworld**: Iron, Diamond, Emerald.
    *   **Nether**: Quartz, Gold, Netherite.
*   **Portal Travel**: Seamlessly travel between your bases using Nether Portals.
*   **Server-Side Only**: No client mod required.

## 📚 Documentation
For detailed gameplay guides and technical stats, please refer to:
*   [Player Guide (English)](PLAYER_GUIDE.md)
*   [玩家指南 (简体中文)](PLAYER_GUIDE_CN.md)

## 🛠️ Installation

### Prerequisites
*   **Minecraft**: Version 1.21.8
*   **Fabric Loader**: Version 0.18.2 or higher
*   **Fabric API**: Latest version compatible with Minecraft 1.21.8
*   **Java**: Version 21 or higher

### Server Installation Steps

1.   Install **Fabric Loader** for your Minecraft server (1.21.8).
2.  Download and place **Fabric API** mod into your server's `mods` folder.
3.  Drop the `two-dimensional-bedwars-x.x.x.jar` into your server's `mods` folder.
4.  Start the server. On first launch, the mod will:
     *   Generate `config/bedwars_config.json` with default settings
     *   Create the custom dimension `two-dimensional-bedwars:arena`
     *   Generate the `dimensions/two-dimensional-bedwars/blueprint` folder

> **Note**: This is a **Server-Side** mod. Players do not need to install it on their clients.

### Troubleshooting

*   **Config Not Generated?** Check server logs for permissions errors. The mod requires write access to the `config` directory.
*   **Dimension Not Loading?** Ensure your `server.properties` has `level-type=minecraft:normal` and restart the server.
*   **Players Can't Join Arena?** Use `/bedwars join` command. The arena dimension is separate from the main world.

## 🎮 Commands

### Player Commands
| Command              | Description                                               |
| :------------------- | :-------------------------------------------------------- |
| `/bedwars join`      | Join the game lobby.                                      |
| `/bedwars start`     | start the game countdown.                                 |
| `/bedwars team <id>` | Select a team (`1=Red`, `2=Blue`, `3=Green`, `4=Yellow`). |
| `/bedwars spectate`  | Join as a spectator.                                      |
| `/bedwars leave`     | Leave the game (requires confirmation if playing).        |

### Admin Commands
*(Requires OP / Permission Level 2)*

| Command           | Description                                  |
| :---------------- | :------------------------------------------- |
| `/bedwars stop`   | Force stop the game and restore map/players. |
| `/bedwars reload` | Reload `bedwars_config.json`.                |

## 📂 Project Structure

*   `top.bearcabbage.twodimensional_bedwars`
    *   **`game`**: Core game loop, logic, and `ArenaManager`.
    *   **`component`**: `Arena`, `BedWarsTeam`, `BedWarsPlayer`, `ShopManager`, and `OreGenerator` implementations.
    *   **`world`**: Custom Dimension (`SplitBiomeSource`, `ArenaChunkGenerator`).
    *   **`mechanic`**: Game mechanics like `GamePlayingTask` (events, respawn), `CustomItemHandler` (special items), and `InternalAdapter`.
    *   **`command`**: Command parsing logic (`BedWarsCommand`).
    *   **`config`**: JSON configuration management (`GameConfig`).
    *   **`screen`**: Shop UI and custom inventory handlers.
    *   **`data`**: Player data persistence using Player Data API.
    *   **`mixin`**: Minecraft behavior modifications (egg entity, server world).

## ⚙️ Configuration

The mod is highly configurable through `config/bedwars_config.json`:

### Key Configuration Sections

*   **Arena Layout**: Configure spawn points, generator locations, and bed positions for each team
*   **Resource Generators**: Adjust spawn rates, amounts, and limits for all resource types (Iron, Gold, Diamond, Emerald, Quartz, Netherite)
*   **Team Forge Levels**: Customize the 5 forge upgrade tiers and their resource production
*   **Public Generators**: Configure Diamond/Emerald (Overworld) and Gold/Netherite (Nether) generator locations and upgrade tiers
*   **Shop Items**: Fully customizable shop with prices, special item types, and upgrade tiers
*   **Game Events**: Adjust timing for Diamond II/III, Emerald II/III, Bed Destruction, Sudden Death, and Game End
*   **Map Restoration**: Define arena boundaries and template regions for automatic map restoration

### Example: Adjusting Resource Rates

```json
{
  "ironGenerator": {
    "amount": 1,
    "delaySeconds": 0.5,
    "limit": 48
  }
}
```

See the auto-generated config file for full documentation of all settings.

## 🎯 Game Architecture

### Game Loop (GamePlayingTask)

The game runs on a tick-based system (20 ticks per second):

1.   **Generator Ticking**: All team and public generators spawn resources based on configured delays
2.   **Bed Integrity Check**: Every 10 ticks, verifies beds are intact and updates team status
3.   **Event System**: Time-based events (Diamond II, Emerald II, etc.) trigger at specified intervals
4.   **Respawn Timers**: Manages 5-second respawn cooldowns for eliminated players
5.   **Win Condition Check**: Evaluates remaining teams and declares victory
6.   **Sudden Death**: Spawns charged creepers across both arenas (after 36 minutes)

### Death & Respawn Mechanics

*   **Death in Overworld** → Respawn in Nether (if bed exists)
*   **Death in Nether** → Respawn in Overworld (if bed exists)
*   **Both Beds Destroyed** → Permanent elimination on next death
*   **5-Second Respawn Timer**: Players wait before respawning in the alternate dimension

### Special Items System

Custom items are identified by NBT tags (`bedwars:item_type`):

*   **FIREBALL**: Launches a ghast-like fireball projectile
*   **BRIDGE_EGG**: Builds a bridge of blocks along its flight path
*   **TNT_TRIGGERED**: Auto-ignites when placed (80-tick fuse)
*   **BLAST_PROOF_GLASS**: Glass immune to explosions
*   **WATER_BUCKET**: Reusable water source
*   **SPONGE**: Water removal tool

See `CustomItemHandler.java` for implementation details.

## 🔧 Building from Source

### Prerequisites

*   JDK 21 or higher
*   Gradle (wrapper included)

### Build Commands

```bash
# Build the mod
./gradlew build

# Run a test server
./gradlew runServer

# Generate IDE project files
./gradlew idea        # For IntelliJ IDEA
./gradlew eclipse     # For Eclipse
```

The compiled JAR will be in `build/libs/two-dimensional-bedwars-x.x.x.jar`.

## 🧪 Development & Testing

### Running a Development Server

```bash
./gradlew runServer
```

The server will start with the mod loaded. Connect via `localhost:25565`.

### Debugging

1.   Set up your IDE with the Fabric development environment
2.  Use `./gradlew runServer --debug-jvm` for remote debugging
3.  Attach your IDE debugger to port 5005

### Testing Commands

```bash
# Quick test workflow
/bedwars join          # Join the lobby
/bedwars team 1        # Select red team
/bedwars start         # Start the game
/bedwars stop          # Force stop (OP required)
/bedwars reload        # Reload config (OP required)
```

## 📝 License
This project is licensed under [GPL-3.0-only](LICENSE).
