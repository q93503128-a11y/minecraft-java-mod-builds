package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.combat.CombatScale;
import kr.moonseungjun.titanbreak.entity.ApexStalkerEntity;
import kr.moonseungjun.titanbreak.entity.BulwarkEntity;
import kr.moonseungjun.titanbreak.entity.BurrowerEntity;
import kr.moonseungjun.titanbreak.entity.BurstlingEntity;
import kr.moonseungjun.titanbreak.entity.ChronoHoundEntity;
import kr.moonseungjun.titanbreak.entity.CinderEntity;
import kr.moonseungjun.titanbreak.entity.CrusherEntity;
import kr.moonseungjun.titanbreak.entity.GliderEntity;
import kr.moonseungjun.titanbreak.entity.HarvesterEntity;
import kr.moonseungjun.titanbreak.entity.HollowColossusEntity;
import kr.moonseungjun.titanbreak.entity.HowlerEntity;
import kr.moonseungjun.titanbreak.entity.IronMawEntity;
import kr.moonseungjun.titanbreak.entity.JammerEntity;
import kr.moonseungjun.titanbreak.entity.NeedlerEntity;
import kr.moonseungjun.titanbreak.entity.NullEyeEntity;
import kr.moonseungjun.titanbreak.entity.PhaseLurkerEntity;
import kr.moonseungjun.titanbreak.entity.PursuerEntity;
import kr.moonseungjun.titanbreak.entity.RegrowerEntity;
import kr.moonseungjun.titanbreak.entity.RevenantEntity;
import kr.moonseungjun.titanbreak.entity.RipperEntity;
import kr.moonseungjun.titanbreak.entity.ShockChoirEntity;
import kr.moonseungjun.titanbreak.entity.SiegebackEntity;
import kr.moonseungjun.titanbreak.entity.SiphonEntity;
import kr.moonseungjun.titanbreak.entity.SkitterEntity;
import kr.moonseungjun.titanbreak.entity.SpitterEntity;
import kr.moonseungjun.titanbreak.entity.StalkerEntity;
import kr.moonseungjun.titanbreak.entity.VoltaicEntity;
import kr.moonseungjun.titanbreak.entity.WardenNodeEntity;
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
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Titanbreak.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Giant>> HOLLOW_COLOSSUS =
            ENTITIES.register("hollow_colossus", () -> EntityType.Builder.<Giant>of((type, level) -> new HollowColossusEntity(type, level), MobCategory.MONSTER)
                    .sized(0.8F, 0.8F).eyeHeight(0.72F).clientTrackingRange(20).build(key("hollow_colossus")));

    public static final DeferredHolder<EntityType<?>, EntityType<RipperEntity>> RIPPER = entity("ripper", RipperEntity::new, 0.60F, 1.95F, 1.74F, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<SpitterEntity>> SPITTER = entity("spitter", SpitterEntity::new, 0.72F, 1.65F, 1.38F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<SkitterEntity>> SKITTER = ENTITIES.register("skitter", () -> EntityType.Builder.<SkitterEntity>of(SkitterEntity::new, MobCategory.MONSTER).sized(1.4F, 0.9F).eyeHeight(0.65F).clientTrackingRange(10).build(key("skitter")));
    public static final DeferredHolder<EntityType<?>, EntityType<GliderEntity>> GLIDER = entity("glider", GliderEntity::new, 1.35F, 0.82F, 0.58F, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<BulwarkEntity>> BULWARK = entity("bulwark", BulwarkEntity::new, 0.90F, 2.20F, 1.92F, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<NeedlerEntity>> NEEDLER = ENTITIES.register("needler", () -> EntityType.Builder.<NeedlerEntity>of(NeedlerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.99F).eyeHeight(1.74F).clientTrackingRange(12).build(key("needler")));
    public static final DeferredHolder<EntityType<?>, EntityType<HowlerEntity>> HOWLER = entity("howler", HowlerEntity::new, 0.72F, 2.05F, 1.80F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<JammerEntity>> JAMMER = entity("jammer", JammerEntity::new, 0.70F, 2.10F, 1.84F, 14);
    public static final DeferredHolder<EntityType<?>, EntityType<VoltaicEntity>> VOLTAIC = entity("voltaic", VoltaicEntity::new, 0.82F, 2.00F, 1.74F, 14);
    public static final DeferredHolder<EntityType<?>, EntityType<CinderEntity>> CINDER = entity("cinder", CinderEntity::new, 0.78F, 1.95F, 1.70F, 14);
    public static final DeferredHolder<EntityType<?>, EntityType<RegrowerEntity>> REGROWER = entity("regrower", RegrowerEntity::new, 0.80F, 2.10F, 1.82F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<BurrowerEntity>> BURROWER = entity("burrower", BurrowerEntity::new, 0.84F, 1.55F, 1.25F, 14);
    public static final DeferredHolder<EntityType<?>, EntityType<CrusherEntity>> CRUSHER = entity("crusher", CrusherEntity::new, 1.08F, 2.45F, 2.05F, 14);
    public static final DeferredHolder<EntityType<?>, EntityType<StalkerEntity>> STALKER = entity("stalker", StalkerEntity::new, 0.68F, 1.92F, 1.70F, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<BurstlingEntity>> BURSTLING = entity("burstling", BurstlingEntity::new, 0.74F, 1.20F, 0.96F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<SiphonEntity>> SIPHON = entity("siphon", SiphonEntity::new, 0.78F, 2.08F, 1.78F, 14);

    public static final DeferredHolder<EntityType<?>, EntityType<ChronoHoundEntity>> CHRONO_HOUND = entity("chrono_hound", ChronoHoundEntity::new, 0.90F, 1.40F, 1.10F, 14);
    public static final DeferredHolder<EntityType<?>, EntityType<NullEyeEntity>> NULL_EYE = entity("null_eye", NullEyeEntity::new, 0.80F, 2.15F, 1.90F, 14);
    public static final DeferredHolder<EntityType<?>, EntityType<IronMawEntity>> IRON_MAW = entity("iron_maw", IronMawEntity::new, 1.18F, 2.55F, 2.10F, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<RevenantEntity>> REVENANT = entity("revenant", RevenantEntity::new, 0.86F, 2.20F, 1.90F, 16);
    public static final DeferredHolder<EntityType<?>, EntityType<ApexStalkerEntity>> APEX_STALKER = entity("apex_stalker", ApexStalkerEntity::new, 0.72F, 2.02F, 1.78F, 18);
    public static final DeferredHolder<EntityType<?>, EntityType<ShockChoirEntity>> SHOCK_CHOIR = entity("shock_choir", ShockChoirEntity::new, 0.92F, 2.28F, 1.96F, 18);
    public static final DeferredHolder<EntityType<?>, EntityType<SiegebackEntity>> SIEGEBACK = entity("siegeback", SiegebackEntity::new, 1.32F, 2.72F, 2.20F, 18);
    public static final DeferredHolder<EntityType<?>, EntityType<PhaseLurkerEntity>> PHASE_LURKER = entity("phase_lurker", PhaseLurkerEntity::new, 0.70F, 2.02F, 1.78F, 20);
    public static final DeferredHolder<EntityType<?>, EntityType<WardenNodeEntity>> WARDEN_NODE = entity("warden_node", WardenNodeEntity::new, 0.90F, 2.30F, 2.00F, 20);
    public static final DeferredHolder<EntityType<?>, EntityType<HarvesterEntity>> HARVESTER = entity("harvester", HarvesterEntity::new, 1.00F, 2.42F, 2.04F, 18);

    public static final DeferredHolder<EntityType<?>, EntityType<PursuerEntity>> THE_PURSUER =
            ENTITIES.register("the_pursuer", () -> EntityType.Builder.<PursuerEntity>of(PursuerEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F).eyeHeight(0.9F).clientTrackingRange(40).build(key("the_pursuer")));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HOLLOW_COLOSSUS.get(), Giant.createAttributes().add(Attributes.MAX_HEALTH, 400.0D).add(Attributes.MOVEMENT_SPEED, 0.16D).add(Attributes.ATTACK_DAMAGE, 0.0D).build());
        event.put(RIPPER.get(), attrs(120, 14, 0.31D));
        event.put(SPITTER.get(), attrs(110, 18, 0.20D));
        event.put(SKITTER.get(), Spider.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(100.0D)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(12.0D)).add(Attributes.MOVEMENT_SPEED, 0.43D).build());
        event.put(GLIDER.get(), attrs(130, 16, 0.28D));
        event.put(BULWARK.get(), Zombie.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(260.0D)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(24.0D)).add(Attributes.MOVEMENT_SPEED, 0.15D).add(Attributes.KNOCKBACK_RESISTANCE, 0.45D).build());
        event.put(NEEDLER.get(), Skeleton.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(90.0D)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(32.0D)).add(Attributes.MOVEMENT_SPEED, 0.23D).build());
        event.put(HOWLER.get(), attrs(150, 10, 0.21D));
        event.put(JAMMER.get(), attrs(140, 14, 0.18D));
        event.put(VOLTAIC.get(), attrs(160, 20, 0.23D));
        event.put(CINDER.get(), attrs(170, 22, 0.21D));
        event.put(REGROWER.get(), attrs(190, 16, 0.18D));
        event.put(BURROWER.get(), attrs(180, 28, 0.26D));
        event.put(CRUSHER.get(), Zombie.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(340.0D)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(42.0D)).add(Attributes.MOVEMENT_SPEED, 0.17D).add(Attributes.KNOCKBACK_RESISTANCE, 0.65D).build());
        event.put(STALKER.get(), attrs(150, 22, 0.29D));
        event.put(BURSTLING.get(), attrs(75, 0, 0.30D));
        event.put(SIPHON.get(), attrs(200, 12, 0.22D));
        event.put(CHRONO_HOUND.get(), attrs(520, 36, 0.38D));
        event.put(NULL_EYE.get(), attrs(430, 22, 0.20D));
        event.put(IRON_MAW.get(), Zombie.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(780.0D)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(32.0D)).add(Attributes.MOVEMENT_SPEED, 0.16D).add(Attributes.KNOCKBACK_RESISTANCE, 0.82D).build());
        event.put(REVENANT.get(), attrs(650, 26, 0.21D));
        event.put(APEX_STALKER.get(), attrs(480, 32, 0.33D));
        event.put(SHOCK_CHOIR.get(), attrs(550, 20, 0.20D));
        event.put(SIEGEBACK.get(), Zombie.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(980.0D)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(38.0D)).add(Attributes.MOVEMENT_SPEED, 0.14D).add(Attributes.KNOCKBACK_RESISTANCE, 0.92D).build());
        event.put(PHASE_LURKER.get(), attrs(460, 30, 0.31D));
        event.put(WARDEN_NODE.get(), attrs(620, 22, 0.20D));
        event.put(HARVESTER.get(), Zombie.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(700.0D)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(26.0D)).add(Attributes.MOVEMENT_SPEED, 0.18D).add(Attributes.KNOCKBACK_RESISTANCE, 0.35D).build());
        event.put(THE_PURSUER.get(), Giant.createAttributes().add(Attributes.MAX_HEALTH, 900.0D).add(Attributes.MOVEMENT_SPEED, 0.42D).add(Attributes.ATTACK_DAMAGE, 0.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).build());
    }

    private static net.minecraft.world.entity.ai.attributes.AttributeSupplier attrs(double hp, double damage, double speed) {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, CombatScale.toInternal(hp)).add(Attributes.ATTACK_DAMAGE, CombatScale.toInternal(damage)).add(Attributes.MOVEMENT_SPEED, speed).build();
    }

    private static <T extends Zombie> DeferredHolder<EntityType<?>, EntityType<T>> entity(String id, EntityType.EntityFactory<T> factory,
                                                                                          float width, float height, float eyeHeight, int tracking) {
        return ENTITIES.register(id, () -> EntityType.Builder.<T>of(factory, MobCategory.MONSTER)
                .sized(width, height).eyeHeight(eyeHeight).clientTrackingRange(tracking).build(key(id)));
    }

    private static ResourceKey<EntityType<?>> key(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, path));
    }
}
