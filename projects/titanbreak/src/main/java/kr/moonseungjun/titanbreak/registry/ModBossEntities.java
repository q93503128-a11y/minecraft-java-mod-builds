package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.entity.BastionWalkerEntity;
import kr.moonseungjun.titanbreak.entity.GravemarchColossusEntity;
import kr.moonseungjun.titanbreak.entity.HundredEyedWatcherEntity;
import kr.moonseungjun.titanbreak.entity.RegnantFleshEntity;
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
                    .sized(1.0F, 1.0F).eyeHeight(0.9F).clientTrackingRange(64)
                    .build(key("gravemarch_colossus")));

    public static final DeferredHolder<EntityType<?>, EntityType<BastionWalkerEntity>> BASTION_WALKER =
            ENTITIES.register("bastion_walker", () -> EntityType.Builder
                    .<BastionWalkerEntity>of(BastionWalkerEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F).eyeHeight(0.9F).clientTrackingRange(96)
                    .build(key("bastion_walker")));

    public static final DeferredHolder<EntityType<?>, EntityType<RegnantFleshEntity>> REGNANT_FLESH =
            ENTITIES.register("regnant_flesh", () -> EntityType.Builder
                    .<RegnantFleshEntity>of(RegnantFleshEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F).eyeHeight(0.9F).clientTrackingRange(80)
                    .build(key("regnant_flesh")));

    public static final DeferredHolder<EntityType<?>, EntityType<HundredEyedWatcherEntity>> HUNDRED_EYED_WATCHER =
            ENTITIES.register("hundred_eyed_watcher", () -> EntityType.Builder
                    .<HundredEyedWatcherEntity>of(HundredEyedWatcherEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F).eyeHeight(0.9F).clientTrackingRange(88)
                    .build(key("hundred_eyed_watcher")));

    private ModBossEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModBossEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GRAVEMARCH_COLOSSUS.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(GravemarchColossusEntity.CANONICAL_VISIBLE_MAX_HEALTH))
                .add(Attributes.MOVEMENT_SPEED, 0.10D).add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 24.0D).add(Attributes.ARMOR_TOUGHNESS, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).build());
        event.put(BASTION_WALKER.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(BastionWalkerEntity.CANONICAL_VISIBLE_MAX_HEALTH))
                .add(Attributes.MOVEMENT_SPEED, 0.055D).add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 30.0D).add(Attributes.ARMOR_TOUGHNESS, 14.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).build());
        event.put(REGNANT_FLESH.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(RegnantFleshEntity.CANONICAL_VISIBLE_MAX_HEALTH))
                .add(Attributes.MOVEMENT_SPEED, 0.095D).add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 18.0D).add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.90D).build());
        event.put(HUNDRED_EYED_WATCHER.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(HundredEyedWatcherEntity.CANONICAL_VISIBLE_MAX_HEALTH))
                .add(Attributes.MOVEMENT_SPEED, 0.115D).add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 15.0D).add(Attributes.ARMOR_TOUGHNESS, 7.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.86D).build());
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path));
    }
}
