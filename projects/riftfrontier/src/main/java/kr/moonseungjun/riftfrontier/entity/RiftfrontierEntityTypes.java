package kr.moonseungjun.riftfrontier.entity;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Stable registry boundary for Riftfrontier-owned entity identities. */
public final class RiftfrontierEntityTypes {
    private static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Riftfrontier.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<Region01BossEntity>> REGION_01_BOSS =
        ENTITY_TYPES.registerEntityType("region_01_boss", Region01BossEntity::new, MobCategory.MONSTER);
    /** Review-only; never part of natural expedition composition before human acceptance. */
    public static final DeferredHolder<EntityType<?>, EntityType<Region01HunterFieldReviewEntity>> REGION_01_HUNTER_FIELD_REVIEW =
        ENTITY_TYPES.registerEntityType("region_01_hunter_field_review", Region01HunterFieldReviewEntity::new, MobCategory.MONSTER);

    private RiftfrontierEntityTypes() {}
    public static void register(IEventBus modEventBus) { ENTITY_TYPES.register(modEventBus); }
    public static void createAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier bossAttributes = LivingEntity.createLivingAttributes().build();
        event.put(REGION_01_BOSS.get(), bossAttributes);
        event.put(REGION_01_HUNTER_FIELD_REVIEW.get(), Monster.createMonsterAttributes().build());
    }
}
