package kr.moonseungjun.anomalousclasses.worldgen;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The worldgen feature selects exactly one type per shared placement region. */
public final class PactWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, AnomalousClasses.MOD_ID);
    public static final DeferredHolder<Feature<?>, PactRuinFeature> RUINS =
            FEATURES.register("pact_ruins", PactRuinFeature::new);
    private PactWorldgen() {}
}
