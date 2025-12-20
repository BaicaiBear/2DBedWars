# Multi-Version Support Guide

## Overview

This project uses a flexible build system to support multiple Minecraft versions. The architecture keeps the codebase simple while allowing easy version-specific builds through configuration files.

## How It Works

The build system uses version-specific properties files to configure dependencies for each Minecraft version. When building, you can specify which version to target, and the build system will automatically use the correct dependencies.

### Key Components

1. **`gradle.properties`** - Contains default version configuration (currently 1.21.8)
2. **`versions/` directory** - Contains version-specific property files
3. **`build.gradle`** - Automatically loads version-specific properties when building
4. **Single codebase** - All game logic remains in one place

## Project Structure

```
2DBedWars/
├── versions/
│   ├── 1.21.8.properties    # Minecraft 1.21.8 dependencies
│   ├── 1.21.4.properties    # Minecraft 1.21.4 dependencies (example)
│   └── ...                  # Add more versions as needed
│
├── src/                     # Single shared codebase
│   └── main/
│       ├── java/           # All game logic
│       └── resources/      # Assets, data, fabric.mod.json
│
├── build.gradle            # Build system with multi-version support
└── gradle.properties       # Default version configuration
```

## Adding Support for a New Minecraft Version

### Step 1: Create Version Properties File

Create a new file in the `versions/` directory named `{version}.properties`:

```bash
# Example: Supporting Minecraft 1.21.4
touch versions/1.21.4.properties
```

### Step 2: Configure Version-Specific Dependencies

Edit the properties file with the correct dependencies for that Minecraft version:

```properties
# Minecraft 1.21.4 Version-Specific Properties
# Find these values at https://fabricmc.net/develop

yarn_mappings=1.21.4+build.XX
fabric_version=X.XX.X+1.21.4
loader_version=0.18.2
```

To find the correct values:
1. Visit https://fabricmc.net/develop
2. Select your Minecraft version
3. Copy the Yarn Mappings and Fabric API version numbers

### Step 3: Build for the New Version

```bash
# Build for Minecraft 1.21.4
./gradlew clean build -PmcVersion=1.21.4

# Build for Minecraft 1.21.8 (default)
./gradlew clean build

# Build for all configured versions
./gradlew buildAllVersions
```

The output JAR will be named: `two-dimensional-bedwars-{minecraft-version}-{mod-version}.jar`

### Step 4: Test the Build

```bash
# Run development server with specific version
./gradlew runServer -PmcVersion=1.21.4
```

## Handling Version-Specific Code

### When APIs Are Compatible

If the Minecraft APIs haven't changed between versions (common for minor updates like 1.21.4 → 1.21.8), you don't need to change any code! Just:

1. Create the version properties file
2. Build with the new version flag
3. Test the mod

### When APIs Have Changed

If Minecraft changed APIs between versions, you have two options:

#### Option A: Version Detection (Recommended for Minor Differences)

Add runtime version detection in your code:

```java
import net.fabricmc.loader.api.FabricLoader;

public class VersionAdapter {
    private static final String MC_VERSION = FabricLoader.getInstance()
        .getModContainer("minecraft")
        .get().getMetadata().getVersion().getFriendlyString();
    
    public static boolean is1_21_8() {
        return MC_VERSION.startsWith("1.21.8");
    }
    
    public static void registerCodec() {
        if (is1_21_8()) {
            // 1.21.8 API
            Registry.register(Registries.BIOME_SOURCE, id, codec);
        } else {
            // 1.21.4 API (example)
            Registries.BIOME_SOURCE.register(id, codec);
        }
    }
}
```

#### Option B: Separate Branches (For Major Differences)

For significant API changes across major versions (e.g., 1.20.x vs 1.21.x):

1. Create a Git branch for each major version:
   ```bash
   git branch minecraft-1.20.x
   git branch minecraft-1.21.x
   ```

2. Maintain code separately in each branch
3. Cherry-pick bug fixes and features between branches
4. Document which branch supports which versions

## Building and Distributing Multiple Versions

### Build All Versions at Once

```bash
# This builds JARs for all versions defined in versions/*.properties
./gradlew buildAllVersions
```

Output files:
```
build/libs/
├── two-dimensional-bedwars-1.21.8-0.8.0.jar
├── two-dimensional-bedwars-1.21.4-0.8.0.jar
└── ...
```

### Build Specific Version

```bash
# Clean and build for a specific version
./gradlew clean build -PmcVersion=1.21.4

# Just build (reuses previous compile)
./gradlew build -PmcVersion=1.21.4
```

### Development Workflow

```bash
# Test 1.21.8 (default)
./gradlew runServer

# Test 1.21.4
./gradlew runServer -PmcVersion=1.21.4

# Quick rebuild during development
./gradlew build --continuous -PmcVersion=1.21.8
```

## Porting Changes Between Versions

When you update the mod, follow this workflow:

1. **Make changes to the shared codebase** (`src/`)
2. **Test with all supported versions**:
   ```bash
   ./gradlew clean build -PmcVersion=1.21.8
   ./gradlew clean build -PmcVersion=1.21.4
   # etc.
   ```
3. **If builds fail**, add version detection code or update version-specific branches
4. **Commit** once all versions build successfully

### Example: Adding a New Feature

```bash
# 1. Implement feature in src/
vi src/main/java/...

# 2. Test with all versions
./gradlew clean build -PmcVersion=1.21.8
./gradlew runServer -PmcVersion=1.21.8
# Test in-game...

./gradlew clean build -PmcVersion=1.21.4
./gradlew runServer -PmcVersion=1.21.4
# Test in-game...

# 3. If all versions work, commit
git add src/ versions/
git commit -m "Add new feature - tested on 1.21.4 and 1.21.8"
```

## Continuous Integration / Automation

To build all versions in CI:

### GitHub Actions Example

```yaml
name: Build All Versions
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        minecraft: [1.21.4, 1.21.8]
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      
      - name: Build ${{ matrix.minecraft }}
        run: ./gradlew build -PmcVersion=${{ matrix.minecraft }}
      
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: mod-${{ matrix.minecraft }}
          path: build/libs/*.jar
```

## Best Practices

1. **Test all supported versions** before releasing
2. **Keep version properties files updated** when Fabric releases new mappings
3. **Document API differences** in code comments when using version detection
4. **Use semantic versioning** for mod releases (e.g., 0.8.0)
5. **Tag releases** with supported versions (e.g., `v0.8.0-mc1.21.4+1.21.8`)

## Troubleshooting

### Build Fails: "Could not find fabric-api:X.X.X+1.21.4"

**Solution**: The version number is incorrect. Visit https://fabricmc.net/develop and get the correct Fabric API version for your Minecraft version.

### Runtime Error: "Incompatible mod set!"

**Solution**: The `fabric.mod.json` dependency constraint is too strict. It should use `~` (tilde) for flexible matching:
```json
"minecraft": "~1.21.4"
```

### Code Compiles but Crashes at Runtime

**Solution**: Minecraft APIs changed between versions. Use version detection (Option A above) or create separate branches (Option B).

### "Cannot find properties file for version X.X.X"

**Solution**: Create `versions/X.X.X.properties` with the correct dependencies, or build without `-PmcVersion` to use defaults.

## Version Support Matrix

| Version  | Status      | Properties File        | Notes                          |
|----------|-------------|------------------------|--------------------------------|
| 1.21.8   | ✅ Supported | `versions/1.21.8.properties` | Primary development version    |

To add more versions, follow the steps above and update this table.

## Migrating Existing Mods to This System

If you have an existing single-version mod:

1. **Create `versions/` directory** with your current version's properties
2. **Update `build.gradle`** to use the multi-version build script
3. **Change `fabric.mod.json`** to use `${minecraft_version}` variable
4. **Test** that your current version still builds: `./gradlew clean build`
5. **Add new versions** by creating additional properties files

No code changes needed - just configuration!

## Further Reading

- [Fabric Wiki: Version-Agnostic Mods](https://fabricmc.net/wiki/tutorial:version_agnostic_mods)
- [Fabric API Versions](https://fabricmc.net/develop)
- [Yarn Mappings](https://github.com/FabricMC/yarn)
