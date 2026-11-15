package kr.moonseungjun.anomalousclasses.entity;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PuppetEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AnomalousClasses.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<PuppetEntity>> MARIONETTE =
            ENTITIES.register("marionette", () -> EntityType.Builder
                    .of(PuppetEntity::new, MobCategory.CREATURE)
                    .sized(.68F, 1.5F).clientTrackingRange(10)
                    .build("anomalousclasses:marionette"));

    private PuppetEntities() {}
}
