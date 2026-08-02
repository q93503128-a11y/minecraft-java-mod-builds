package kr.moonseungjun.livingkingdoms.worldgen;

import com.mojang.serialization.MapCodec;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers data-driven world-generation codecs before the Living Realm datapack is decoded. */
public final class LivingWorldgenTypes {
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, LivingKingdoms.MOD_ID);

    static {
        DENSITY_FUNCTION_TYPES.register(
                "authored_continent",
                () -> AuthoredContinentDensity.CODEC.codec()
        );
    }

    private LivingWorldgenTypes() {
    }

    public static void register(IEventBus modEventBus) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);
    }
}
