package kr.moonseungjun.turnboundre.presentation;

import kr.moonseungjun.turnboundre.TurnboundRe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registers client-facing visual entity types without changing gameplay character source entities. */
public final class TurnboundPresentationEntities {
    public static final Identifier STARTER_ZOMBIE_ID = Identifier.fromNamespaceAndPath(
            TurnboundRe.MOD_ID, "starter_zombie_visual");
    public static final Identifier BLAZE_ID = Identifier.fromNamespaceAndPath(
            TurnboundRe.MOD_ID, "blaze_visual");
    public static final Identifier WITCH_ID = Identifier.fromNamespaceAndPath(
            TurnboundRe.MOD_ID, "witch_visual");
    public static final Identifier IRON_GOLEM_ID = Identifier.fromNamespaceAndPath(
            TurnboundRe.MOD_ID, "iron_golem_visual");
    public static final Identifier CREEPER_ID = Identifier.fromNamespaceAndPath(
            TurnboundRe.MOD_ID, "creeper_visual");
    public static final Identifier SPIDER_ID = Identifier.fromNamespaceAndPath(
            TurnboundRe.MOD_ID, "spider_visual");

    public static final DeferredHolder<EntityType<?>, EntityType<StarterZombieVisualEntity>> STARTER_ZOMBIE =
            DeferredHolder.create(Registries.ENTITY_TYPE, STARTER_ZOMBIE_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<BlazeVisualEntity>> BLAZE =
            DeferredHolder.create(Registries.ENTITY_TYPE, BLAZE_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<WitchVisualEntity>> WITCH =
            DeferredHolder.create(Registries.ENTITY_TYPE, WITCH_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<IronGolemVisualEntity>> IRON_GOLEM =
            DeferredHolder.create(Registries.ENTITY_TYPE, IRON_GOLEM_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<CreeperVisualEntity>> CREEPER =
            DeferredHolder.create(Registries.ENTITY_TYPE, CREEPER_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<SpiderVisualEntity>> SPIDER =
            DeferredHolder.create(Registries.ENTITY_TYPE, SPIDER_ID);

    private TurnboundPresentationEntities() {}

    public static void register(IEventBus modBus) {
        if (modBus == null) throw new IllegalArgumentException("modBus required");
        modBus.addListener(TurnboundPresentationEntities::registerEntityTypes);
        modBus.addListener(TurnboundPresentationEntities::registerAttributes);
    }

    private static void registerEntityTypes(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.ENTITY_TYPE)) return;
        event.register(Registries.ENTITY_TYPE, STARTER_ZOMBIE_ID, () -> EntityType.Builder
                .of(StarterZombieVisualEntity::new, MobCategory.MISC)
                .sized(0.78F, 2.05F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, STARTER_ZOMBIE_ID)));
        event.register(Registries.ENTITY_TYPE, BLAZE_ID, () -> EntityType.Builder
                .of(BlazeVisualEntity::new, MobCategory.MISC)
                .sized(0.82F, 1.95F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, BLAZE_ID)));
        event.register(Registries.ENTITY_TYPE, WITCH_ID, () -> EntityType.Builder
                .of(WitchVisualEntity::new, MobCategory.MISC)
                .sized(0.72F, 1.95F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, WITCH_ID)));
        event.register(Registries.ENTITY_TYPE, IRON_GOLEM_ID, () -> EntityType.Builder
                .of(IronGolemVisualEntity::new, MobCategory.MISC)
                .sized(1.45F, 2.75F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, IRON_GOLEM_ID)));
        event.register(Registries.ENTITY_TYPE, CREEPER_ID, () -> EntityType.Builder
                .of(CreeperVisualEntity::new, MobCategory.MISC)
                .sized(0.60F, 1.70F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, CREEPER_ID)));
        event.register(Registries.ENTITY_TYPE, SPIDER_ID, () -> EntityType.Builder
                .of(SpiderVisualEntity::new, MobCategory.MISC)
                .sized(1.40F, 0.90F)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, SPIDER_ID)));
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STARTER_ZOMBIE.get(), Monster.createMonsterAttributes().build());
        event.put(BLAZE.get(), Monster.createMonsterAttributes().build());
        event.put(WITCH.get(), Monster.createMonsterAttributes().build());
        event.put(IRON_GOLEM.get(), Monster.createMonsterAttributes().build());
        event.put(CREEPER.get(), Monster.createMonsterAttributes().build());
        event.put(SPIDER.get(), Monster.createMonsterAttributes().build());
    }
}
