# Contributing to 2-Dimensional BedWars

Thank you for your interest in contributing to 2-Dimensional BedWars! This document provides guidelines and information for developers who want to contribute to the project.

## 📋 Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Code Style Guidelines](#code-style-guidelines)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)

## 🚀 Getting Started

### Prerequisites

- **JDK 21** or higher
- **Git** for version control
- **IntelliJ IDEA** or **Eclipse** (recommended IDEs)
- Basic knowledge of:
  - Java programming
  - Minecraft modding with Fabric
  - Gradle build system

### First Time Setup

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/2DBedWars.git
   cd 2DBedWars
   ```
3. **Generate IDE project files**:
   ```bash
   ./gradlew idea        # For IntelliJ IDEA
   ./gradlew eclipse     # For Eclipse
   ```
4. **Build the project**:
   ```bash
   ./gradlew build
   ```
5. **Run a test server**:
   ```bash
   ./gradlew runServer
   ```

## 🛠️ Development Setup

### Building the Mod

```bash
# Clean build
./gradlew clean build

# Build without running tests
./gradlew build -x test

# Run the mod in a development server
./gradlew runServer
```

The compiled JAR will be in `build/libs/`.

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with debug output
./gradlew test --info
```

### Debugging

To debug the mod in your IDE:

1. Run: `./gradlew runServer --debug-jvm`
2. Attach your IDE's remote debugger to `localhost:5005`
3. Set breakpoints in your code
4. Connect to the server at `localhost:25565`

### Hot Reloading

For faster development iteration:

1. Use `/bedwars reload` command to reload configuration changes without restarting
2. For code changes, a full server restart is required

## 📁 Project Structure

```
src/main/java/top/bearcabbage/twodimensional_bedwars/
├── api/              # Public interfaces (IArena, ITeam)
├── command/          # Command implementations (/bedwars)
├── component/        # Core game components (Arena, Team, Player, Shop)
├── config/           # Configuration management (GameConfig)
├── data/             # Player data persistence
├── game/             # Game state management (ArenaManager, ScoreboardManager)
├── mechanic/         # Game mechanics (GamePlayingTask, CustomItemHandler)
├── mixin/            # Minecraft behavior modifications
├── screen/           # GUI screens (Shop interface)
└── world/            # Custom dimension (SplitBiomeSource, ArenaChunkGenerator)

src/main/resources/
├── assets/two-dimensional-bedwars/
│   ├── icon.png                    # Mod icon
│   └── lang/                       # Translations (en_us.json, zh_cn.json)
├── data/two-dimensional-bedwars/
│   ├── dimension/                  # Dimension definitions
│   └── worldgen/biome/            # Custom biomes
├── fabric.mod.json                 # Mod metadata
└── two-dimensional-bedwars.mixins.json  # Mixin configuration
```

## 📝 Code Style Guidelines

### Java Conventions

- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Maximum 120 characters
- **Naming Conventions**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Code Organization

1. **Group imports**: Standard library → Minecraft → Fabric → Project classes
2. **Method order**: Public methods first, then private methods
3. **Comments**: Use JavaDoc for public APIs, inline comments for complex logic
4. **Null safety**: Always check for null, use Optional where appropriate

### Example

```java
package top.bearcabbage.twodimensional_bedwars.component;

import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;
import top.bearcabbage.twodimensional_bedwars.api.ITeam;

/**
 * Manages a BedWars team including members, upgrades, and resources.
 */
public class BedWarsTeam implements ITeam {
    private static final int MAX_TEAM_SIZE = 8;
    
    private final String teamName;
    private final List<UUID> members;
    
    /**
     * Creates a new BedWars team.
     * 
     * @param teamName The display name of the team
     */
    public BedWarsTeam(String teamName) {
        this.teamName = teamName;
        this.members = new ArrayList<>();
    }
    
    @Override
    public void addMember(UUID playerId) {
        if (!members.contains(playerId) && members.size() < MAX_TEAM_SIZE) {
            members.add(playerId);
        }
    }
    
    // ... more methods
}
```

## 🧪 Testing

### Manual Testing Workflow

1. Start the test server: `./gradlew runServer`
2. Connect to `localhost:25565`
3. Test your changes:
   ```
   /bedwars join
   /bedwars team 1
   /bedwars start
   ```
4. Check for errors in server console
5. Test edge cases (e.g., all beds destroyed, sudden death)

### Testing Checklist

Before submitting:
- [ ] Test with multiple players (at least 2 teams)
- [ ] Verify both arenas work correctly
- [ ] Test respawn mechanics in both dimensions
- [ ] Verify shop purchases work
- [ ] Test team upgrades apply correctly
- [ ] Check special items (Bridge Egg, Fireball, TNT)
- [ ] Test game events at different timestamps
- [ ] Verify config reload works
- [ ] Check for console errors/warnings

## 📤 Submitting Changes

### Pull Request Process

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** with clear, focused commits:
   ```bash
   git add .
   git commit -m "Add detailed description of changes"
   ```

3. **Keep commits atomic**: One logical change per commit

4. **Write descriptive commit messages**:
   ```
   Add support for custom team colors
   
   - Implement RGB color configuration in GameConfig
   - Update team selection UI to show custom colors
   - Add validation for color hex codes
   - Update documentation
   ```

5. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Open a Pull Request** on GitHub:
   - Use a clear title describing the change
   - Reference related issues (e.g., "Fixes #123")
   - Describe what you changed and why
   - List any breaking changes
   - Include screenshots for UI changes

### PR Review Process

- Maintainers will review your PR
- Address any requested changes
- Once approved, your PR will be merged
- Your contribution will be credited in release notes

## 🐛 Reporting Bugs

### Before Reporting

1. **Check existing issues** - your bug may already be reported
2. **Reproduce the bug** - ensure it's consistent
3. **Test with latest version** - bug may be fixed

### Bug Report Template

```markdown
**Describe the bug**
Clear description of what the bug is.

**To Reproduce**
1. Join the game as team Red
2. Purchase Iron Armor
3. Die and respawn
4. Armor disappears

**Expected behavior**
Armor should persist after respawn if bed is alive.

**Environment:**
- Mod Version: v1.0.0
- Minecraft Version: 1.21.8
- Fabric Loader: 0.18.2
- Server or Client: Server

**Additional context**
Server logs, screenshots, etc.
```

## 💡 Feature Requests

We welcome feature suggestions! When requesting a feature:

1. **Check existing issues** - feature may be planned or discussed
2. **Describe the feature** - what it does and why it's useful
3. **Provide use cases** - how players/admins would use it
4. **Consider implementation** - is it technically feasible?

### Feature Request Template

```markdown
**Feature Description**
Brief description of the suggested feature.

**Problem it Solves**
What problem or limitation does this address?

**Proposed Solution**
How would this feature work?

**Alternatives Considered**
Other approaches you've thought about.

**Additional Context**
Mockups, examples from other games, etc.
```

## 🔧 Common Development Tasks

### Adding a New Shop Item

1. Add item configuration in `GameConfig.defaultShop()`:
   ```java
   list.add(new ShopEntry(slot, "minecraft:item_id", count, 
       "minecraft:currency", price, "translation.key", "SPECIAL_TYPE"));
   ```

2. Add translation in `assets/two-dimensional-bedwars/lang/en_us.json`:
   ```json
   "two-dimensional-bedwars.shop.item.my_item": "My Item Name"
   ```

3. If special behavior needed, update `CustomItemHandler.java`

4. Update shop documentation in `PLAYER_GUIDE.md`

### Adding a Game Event

1. Register event in `GamePlayingTask.registerEvents()`:
   ```java
   events.add(new GameEvent(
       Text.translatable("event.name"),
       timeInSeconds,
       (world) -> {
           // Event logic here
           arena.broadcastToGame(world.getServer(), 
               Text.translatable("event.message"));
       }
   ));
   ```

2. Add translations for event name and message

3. Update event timeline in `PLAYER_GUIDE.md`

### Adding a Team Upgrade

1. Add upgrade tier in shop config with type `UPGRADE_*`
2. Implement upgrade logic in `BedWarsTeam.applyTeamUpgrades()`
3. Add translation strings
4. Document in `PLAYER_GUIDE.md`

## 📚 Additional Resources

- [Fabric Wiki](https://fabricmc.net/wiki/start) - Fabric modding documentation
- [Minecraft Wiki](https://minecraft.wiki/) - Minecraft mechanics reference
- [Yarn Mappings](https://github.com/FabricMC/yarn) - Minecraft code mappings
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/) - Java language reference

## 📜 License

By contributing to this project, you agree that your contributions will be licensed under the GPL-3.0-only license.

## 🤝 Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what's best for the project
- Help others learn and grow

---

**Thank you for contributing to 2-Dimensional BedWars! Your efforts help make this mod better for everyone.** 🎮✨
