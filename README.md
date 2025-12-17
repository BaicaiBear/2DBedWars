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

1.   Install **Fabric Loader** for your server.
2.  Drop the `two-dimensional-bedwars-x.x.x.jar` into your server's `mods` folder.
3.  (Optional) Start the server. The mod will automatically generate the `dimensions/two-dimensional-bedwars/blueprint` folder from its internal template.

> **Note**: This is a **Server-Side** mod. Players do not need to install it on their clients.

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
    *   **`component`**: `Arena`, `BedWarsTeam`, and `BedWarsPlayer` implementations.
    *   **`world`**: Custom Dimension (`SplitBiomeSource`, `ArenaChunkGenerator`).
    *   **`mechanic`**: Game mechanics like `GamePlayingTask` and `CustomItemHandler`.
    *   **`command`**: Command parsing logic.
    *   **`config`**: JSON configuration management.

## 📝 License
This project is licensed under [GPL-3.0-only](LICENSE).
