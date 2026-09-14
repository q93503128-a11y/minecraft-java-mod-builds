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
    public static final DeferredHolder<EntityType<?>, EntityType<StarterZombieVisualEntity>> STARTER_ZOMBIE =
            DeferredHolder.create(Registries.ENTITY_TYPE, STARTER_ZOMBIE_ID);

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
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STARTER_ZOMBIE.get(), Monster.createMonsterAttributes().build());
    }
}
