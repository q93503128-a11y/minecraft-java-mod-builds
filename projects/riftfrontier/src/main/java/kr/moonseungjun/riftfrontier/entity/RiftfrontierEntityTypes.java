package kr.moonseungjun.riftfrontier.entity;

import kr.moonseungjun.riftfrontier.Riftfrontier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Stable registry boundary for Riftfrontier-owned entity identities. */
public final class RiftfrontierEntityTypes {
    private static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Riftfrontier.MOD_ID);

    /**
     * Region 01 boss actor identity. It is intentionally not added to natural spawning or expedition encounter
     * composition until its accepted presentation, physical scale/hitbox and combat authoring gates are complete.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<Region01BossEntity>> REGION_01_BOSS =
        ENTITY_TYPES.registerEntityType("region_01_boss", Region01BossEntity::new, MobCategory.MONSTER);

    private RiftfrontierEntityTypes() {}

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    public static void createAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = LivingEntity.createLivingAttributes().build();
        event.put(REGION_01_BOSS.get(), attributes);
    }
}
