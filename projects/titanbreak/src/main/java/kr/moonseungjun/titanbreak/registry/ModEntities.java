package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.entity.BulwarkEntity;
import kr.moonseungjun.titanbreak.entity.ChronoHoundEntity;
import kr.moonseungjun.titanbreak.entity.GliderEntity;
import kr.moonseungjun.titanbreak.entity.HollowColossusEntity;
import kr.moonseungjun.titanbreak.entity.HowlerEntity;
import kr.moonseungjun.titanbreak.entity.JammerEntity;
import kr.moonseungjun.titanbreak.entity.NeedlerEntity;
import kr.moonseungjun.titanbreak.entity.NullEyeEntity;
import kr.moonseungjun.titanbreak.entity.PursuerEntity;
import kr.moonseungjun.titanbreak.entity.RipperEntity;
import kr.moonseungjun.titanbreak.entity.SkitterEntity;
import kr.moonseungjun.titanbreak.entity.SpitterEntity;
import kr.moonseungjun.titanbreak.entity.VoltaicEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
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
                    .sized(0.8F, 0.8F).eyeHeight(0.72F).clientTrackingRange(20)
                    .build(key("hollow_colossus")));

    public static final DeferredHolder<EntityType<?>, EntityType<RipperEntity>> RIPPER =
            ENTITIES.register("ripper", () -> EntityType.Builder.<RipperEntity>of(
                            RipperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(10)
                    .build(key("ripper")));

    public static final DeferredHolder<EntityType<?>, EntityType<SpitterEntity>> SPITTER =
            ENTITIES.register("spitter", () -> EntityType.Builder.<SpitterEntity>of(
                            SpitterEntity::new, MobCategory.MONSTER)
                    .sized(0.72F, 1.65F).eyeHeight(1.38F).clientTrackingRange(12)
                    .build(key("spitter")));

    public static final DeferredHolder<EntityType<?>, EntityType<SkitterEntity>> SKITTER =
            ENTITIES.register("skitter", () -> EntityType.Builder.<SkitterEntity>of(
                            SkitterEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 0.9F).eyeHeight(0.65F).clientTrackingRange(10)
                    .build(key("skitter")));

    public static final DeferredHolder<EntityType<?>, EntityType<GliderEntity>> GLIDER =
            ENTITIES.register("glider", () -> EntityType.Builder.<GliderEntity>of(
                            GliderEntity::new, MobCategory.MONSTER)
                    .sized(1.35F, 0.82F).eyeHeight(0.58F).clientTrackingRange(16)
                    .build(key("glider")));

    public static final DeferredHolder<EntityType<?>, EntityType<BulwarkEntity>> BULWARK =
            ENTITIES.register("bulwark", () -> EntityType.Builder.<BulwarkEntity>of(
                            BulwarkEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.2F).eyeHeight(1.92F).clientTrackingRange(10)
                    .build(key("bulwark")));

    public static final DeferredHolder<EntityType<?>, EntityType<NeedlerEntity>> NEEDLER =
            ENTITIES.register("needler", () -> EntityType.Builder.<NeedlerEntity>of(
                            NeedlerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(12)
                    .build(key("needler")));

    public static final DeferredHolder<EntityType<?>, EntityType<HowlerEntity>> HOWLER =
            ENTITIES.register("howler", () -> EntityType.Builder.<HowlerEntity>of(
                            HowlerEntity::new, MobCategory.MONSTER)
                    .sized(0.72F, 2.05F).eyeHeight(1.8F).clientTrackingRange(12)
                    .build(key("howler")));

    public static final DeferredHolder<EntityType<?>, EntityType<JammerEntity>> JAMMER =
            ENTITIES.register("jammer", () -> EntityType.Builder.<JammerEntity>of(
                            JammerEntity::new, MobCategory.MONSTER)
                    .sized(0.70F, 2.10F).eyeHeight(1.84F).clientTrackingRange(14)
                    .build(key("jammer")));

    public static final DeferredHolder<EntityType<?>, EntityType<VoltaicEntity>> VOLTAIC =
            ENTITIES.register("voltaic", () -> EntityType.Builder.<VoltaicEntity>of(
                            VoltaicEntity::new, MobCategory.MONSTER)
                    .sized(0.82F, 2.00F).eyeHeight(1.74F).clientTrackingRange(14)
                    .build(key("voltaic")));

    public static final DeferredHolder<EntityType<?>, EntityType<ChronoHoundEntity>> CHRONO_HOUND =
            ENTITIES.register("chrono_hound", () -> EntityType.Builder.<ChronoHoundEntity>of(
                            ChronoHoundEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.4F).eyeHeight(1.1F).clientTrackingRange(14)
                    .build(key("chrono_hound")));

    public static final DeferredHolder<EntityType<?>, EntityType<NullEyeEntity>> NULL_EYE =
            ENTITIES.register("null_eye", () -> EntityType.Builder.<NullEyeEntity>of(
                            NullEyeEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.15F).eyeHeight(1.9F).clientTrackingRange(14)
                    .build(key("null_eye")));

    public static final DeferredHolder<EntityType<?>, EntityType<PursuerEntity>> THE_PURSUER =
            ENTITIES.register("the_pursuer", () -> EntityType.Builder.<PursuerEntity>of(
                            PursuerEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F).eyeHeight(0.9F).clientTrackingRange(40)
                    .build(key("the_pursuer")));

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

        event.put(SPITTER.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(110.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(18.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .build());

        event.put(SKITTER.get(), Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(100.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(12.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.43D)
                .build());

        event.put(GLIDER.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(130.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(16.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .build());

        event.put(BULWARK.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(260.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(24.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                .build());

        event.put(NEEDLER.get(), Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(90.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(32.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .build());

        event.put(HOWLER.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(150.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(10.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.21D)
                .build());

        event.put(JAMMER.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(140.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(14.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .build());

        event.put(VOLTAIC.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(160.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(20.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .build());

        event.put(CHRONO_HOUND.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(520.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(36.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .build());

        event.put(NULL_EYE.get(), Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, CombatScale.toInternal(430.0D))
                .add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(22.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .build());

        event.put(THE_PURSUER.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, 900.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .build());
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path));
    }
}
