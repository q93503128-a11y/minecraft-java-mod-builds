package kr.moonseungjun.livingkingdoms.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Noise terrain generator used only by the Living Realm.
 *
 * <p>Terrain, caves, surface rules and biome decoration continue to use the normal noise pipeline,
 * but registry-driven vanilla and third-party structures are not started here. Living Kingdoms
 * settlements are assembled later by the attributed, terrain-integrated realm builder, so disabling
 * this phase cannot remove the authored Erden capital.</p>
 */
public final class StructurelessNoiseChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<StructurelessNoiseChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(StructurelessNoiseChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings")
                            .forGetter(generator -> generator.authoredSettings)
            ).apply(instance, instance.stable(StructurelessNoiseChunkGenerator::new)));

    private final Holder<NoiseGeneratorSettings> authoredSettings;

    public StructurelessNoiseChunkGenerator(BiomeSource biomeSource,
                                             Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.authoredSettings = settings;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void createStructures(RegistryAccess registryAccess,
                                 ChunkGeneratorStructureState structureState,
                                 StructureManager structureManager,
                                 ChunkAccess chunk,
                                 StructureTemplateManager templateManager,
                                 ResourceKey<Level> levelKey) {
        // Deliberately empty. The Living Realm accepts only Living Kingdoms authored settlements.
    }
}
