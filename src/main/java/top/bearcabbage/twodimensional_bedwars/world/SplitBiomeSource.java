package top.bearcabbage.twodimensional_bedwars.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.stream.Stream;

public class SplitBiomeSource extends BiomeSource {
    public static final MapCodec<SplitBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.REGISTRY_CODEC.fieldOf("desert").forGetter(s -> s.desert),
                    Biome.REGISTRY_CODEC.fieldOf("nether_wastes").forGetter(s -> s.netherWastes)
            ).apply(instance, SplitBiomeSource::new)
    );

    private final RegistryEntry<Biome> desert;
    private final RegistryEntry<Biome> netherWastes;

    public SplitBiomeSource(RegistryEntry<Biome> desert, RegistryEntry<Biome> netherWastes) {
        this.desert = desert;
        this.netherWastes = netherWastes;
    }

    @Override
    protected MapCodec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    public Stream<RegistryEntry<Biome>> biomeStream() {
        return Stream.of(desert, netherWastes);
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        // x, y, z are in quart coordinates (blocks / 4)
        // We need block coordinates to check x < 200
        int blockX = x * 4;
        
        if (blockX < 200) {
            return desert;
        } else {
            return netherWastes;
        }
    }
}
