package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import kr.moonseungjun.titanbreak.entity.HollowColossusEntity;
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

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Titanbreak.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Giant>> HOLLOW_COLOSSUS =
            ENTITIES.register("hollow_colossus", () -> EntityType.Builder.<Giant>of(
                            (type, level) -> new HollowColossusEntity(type, level), MobCategory.MONSTER)
                    .sized(3.6F, 12.0F)
                    .eyeHeight(10.44F)
                    .ridingOffset(-3.75F)
                    .clientTrackingRange(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(Titanbreak.MOD_ID, "hollow_colossus"))));

    private ModEntities() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HOLLOW_COLOSSUS.get(), Giant.createAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .build());
    }
}
