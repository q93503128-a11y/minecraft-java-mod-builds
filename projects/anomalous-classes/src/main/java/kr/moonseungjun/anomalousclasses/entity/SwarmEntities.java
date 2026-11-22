package kr.moonseungjun.anomalousclasses.entity;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One living collision proxy per occupied role, never one mob per organism. */
public final class SwarmEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AnomalousClasses.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<SwarmNodeEntity>> NODE =
            ENTITIES.register("swarm_node", () -> EntityType.Builder
                    .of(SwarmNodeEntity::new, MobCategory.CREATURE)
                    .sized(.8F, .45F).noSave().noSummon().clientTrackingRange(8)
                    .build("anomalousclasses:swarm_node"));

    private SwarmEntities() {}
}
