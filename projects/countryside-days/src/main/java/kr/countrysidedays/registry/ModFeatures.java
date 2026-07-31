package kr.countrysidedays.registry;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.worldgen.CountrysideChunkFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            BuiltInRegistries.FEATURE,
            CountrysideDays.MOD_ID
    );

    public static final DeferredHolder<Feature<?>, CountrysideChunkFeature> COUNTRYSIDE_CHUNK =
            FEATURES.register(
                    "countryside_chunk",
                    () -> new CountrysideChunkFeature(NoneFeatureConfiguration.CODEC)
            );

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
