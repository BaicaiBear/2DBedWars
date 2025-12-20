# Version Porting Checklist

Use this checklist when porting the mod to a new Minecraft version.

## Pre-Porting

- [ ] Check Fabric release notes for API changes in target version
- [ ] Verify target version is released and stable
- [ ] Check if Yarn mappings are available for target version
- [ ] Check if Fabric API is available for target version

## Configuration Steps

- [ ] Create `versions/{version}.properties` file
- [ ] Find correct `yarn_mappings` version at https://fabricmc.net/develop
- [ ] Find correct `fabric_version` at https://fabricmc.net/develop  
- [ ] Set appropriate `loader_version` (usually same as current)
- [ ] Update version support matrix in MULTIVERSION_GUIDE.md

## Build Testing

- [ ] Clean build directory: `./gradlew clean`
- [ ] Build for new version: `./gradlew build -PmcVersion={version}`
- [ ] Check for compilation errors
- [ ] Review any deprecation warnings
- [ ] Verify JAR is created in `build/libs/`

## Runtime Testing

- [ ] Start test server: `./gradlew runServer -PmcVersion={version}`
- [ ] Verify mod loads without errors
- [ ] Test basic game functionality:
  - [ ] `/bedwars join` command works
  - [ ] Team selection works
  - [ ] Game can start
  - [ ] Respawn mechanics work
  - [ ] Shop NPCs work
  - [ ] Resource generators work
  - [ ] Bed breaking works
  - [ ] Death/respawn cycle works
  - [ ] Game end works properly
  - [ ] Player data restoration works

## API Compatibility Checks

If runtime testing reveals issues, check these common API changes:

- [ ] Registry API changes (check `Registry.register()` calls)
- [ ] Item/Block component changes (NBT → DataComponents in 1.20.5+)
- [ ] Text API changes (formatting, translations)
- [ ] World generation API changes (biomes, chunk generators)
- [ ] Entity AI changes
- [ ] Networking changes (packets, screen handlers)

## Code Adaptation (if needed)

- [ ] Add version detection code for incompatible APIs
- [ ] Test both old and new versions still work
- [ ] Document API differences in code comments
- [ ] Update BUILD_IMPROVEMENTS.md if significant changes made

## Documentation

- [ ] Update README.md installation section
- [ ] Update MULTIVERSION_GUIDE.md version matrix
- [ ] Add any version-specific notes or warnings
- [ ] Update release notes

## Release Preparation

- [ ] Test on clean Minecraft server installation
- [ ] Verify no client-side mod requirement
- [ ] Check file size is reasonable
- [ ] Test with other common server mods (if applicable)
- [ ] Create GitHub release with version-specific JARs

## Example Workflow

```bash
# 1. Create version file
echo "yarn_mappings=1.21.4+build.3
fabric_version=0.111.0+1.21.4  
loader_version=0.18.2" > versions/1.21.4.properties

# 2. Build
./gradlew clean build -PmcVersion=1.21.4

# 3. Test
./gradlew runServer -PmcVersion=1.21.4

# 4. In-game testing...
# /bedwars join
# /bedwars team 1
# /bedwars start
# [Play through a full game]

# 5. If successful, commit
git add versions/1.21.4.properties MULTIVERSION_GUIDE.md
git commit -m "Add support for Minecraft 1.21.4"
```

## Common Issues

### Compilation Errors

**Problem**: Method not found / Cannot resolve symbol
**Solution**: Check Fabric API changelog for method renames or relocations. Add version-specific code paths if needed.

### Runtime Crash on Startup

**Problem**: ClassNotFoundException / NoSuchMethodError
**Solution**: Dependency versions might be wrong. Double-check yarn_mappings and fabric_version in properties file.

### Mod Doesn't Load

**Problem**: "Incompatible mod set" error
**Solution**: Check `fabric.mod.json` minecraft dependency - should use `~` for flexible matching.

### Features Don't Work

**Problem**: Game mechanics fail at runtime
**Solution**: Minecraft APIs changed. Add version detection and implement version-specific handlers.

## Getting Help

- Fabric Discord: https://discord.gg/v6v4pMv
- Fabric Wiki: https://fabricmc.net/wiki/
- Yarn Mappings: https://github.com/FabricMC/yarn
- This Project's Issues: https://github.com/BaicaiBear/2DBedWars/issues
