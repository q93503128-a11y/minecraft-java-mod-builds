package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.entity.HollowColossusEntity;
import kr.moonseungjun.titanbreak.entity.RipperEntity;
import kr.moonseungjun.titanbreak.entity.SkitterEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Titanbreak.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Giant>> HOLLOW_COLOSSUS =
            ENTITIES.register("hollow_colossus", () -> EntityType.Builder.<Giant>of(
                            (type, level) -> new HollowColossusEntity(type, level), MobCategory.MONSTER)
                    .sized(0.8F, 0.8F)
                    .eyeHeight(0.72F)
                    .clientTrackingRange(20)
                    .build(key("hollow_colossus")));

    public static final DeferredHolder<EntityType<?>, EntityType<Zombie>> RIPPER =
            ENTITIES.register("ripper", () -> EntityType.Builder.<Zombie>of(
                            (type, level) -> new RipperEntity(type, level), MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(10)
                    .build(key("ripper")));

    public static final DeferredHolder<EntityType<?>, EntityType<Spider>> SKITTER =
            ENTITIES.register("skitter", () -> EntityType.Builder.<Spider>of(
                            (type, level) -> new SkitterEntity(type, level), MobCategory.MONSTER)
                    .sized(1.4F, 0.9F)
                    .eyeHeight(0.65F)
                    .clientTrackingRange(10)
                    .build(key("skitter")));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HOLLOW_COLOSSUS.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .build());

        event.put(RIPPER.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(120.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(14.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .build());

        event.put(SKITTER.get(), Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(100.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(12.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.43D)
                .build());
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path));
    }
}
