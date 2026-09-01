package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.entity.GravemarchColossusEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Giant;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBossEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Titanbreak.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<GravemarchColossusEntity>> GRAVEMARCH_COLOSSUS =
            ENTITIES.register("gravemarch_colossus", () -> EntityType.Builder
                    .<GravemarchColossusEntity>of(GravemarchColossusEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F)
                    .eyeHeight(0.9F)
                    .clientTrackingRange(64)
                    .build(key("gravemarch_colossus")));

    private ModBossEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModBossEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GRAVEMARCH_COLOSSUS.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(GravemarchColossusEntity.CANONICAL_VISIBLE_MAX_HEALTH))
                .add(Attributes.MOVEMENT_SPEED, 0.10D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 24.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .build());
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path));
    }
}
