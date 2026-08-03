package kr.moonseungjun.villageguardians;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VillageSkillEffectEntities {
    private static final DeferredRegister.Entities ENTITIES =
            DeferredRegister.createEntities(VillageGuardians.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<VillageSkillEffectEntity>> SKILL_EFFECT =
            ENTITIES.registerEntityType(
                    "skill_effect",
                    VillageSkillEffectEntity::new,
                    MobCategory.MISC,
                    builder -> builder
                            .sized(24.0f, 16.0f)
                            .noSave()
                            .noSummon()
                            .clientTrackingRange(128)
                            .updateInterval(1));

    private VillageSkillEffectEntities() {}

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
