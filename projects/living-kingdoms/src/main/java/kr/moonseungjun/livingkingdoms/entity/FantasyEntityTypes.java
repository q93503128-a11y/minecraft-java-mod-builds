package kr.moonseungjun.livingkingdoms.entity;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registered species owned by Living Kingdoms rather than renamed vanilla mobs. */
public final class FantasyEntityTypes {
    public static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(LivingKingdoms.MOD_ID);

    public static final Supplier<EntityType<SilverHartEntity>> SILVER_HART =
            ENTITY_TYPES.registerEntityType(
                    "silver_hart", SilverHartEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.95F, 1.55F).eyeHeight(1.35F).clientTrackingRange(10));

    public static final Supplier<EntityType<AshHoundEntity>> ASH_HOUND =
            ENTITY_TYPES.registerEntityType(
                    "ash_hound", AshHoundEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(0.75F, 0.95F).eyeHeight(0.78F).clientTrackingRange(10));

    public static final Supplier<EntityType<RiverWispEntity>> RIVER_WISP =
            ENTITY_TYPES.registerEntityType(
                    "river_wisp", RiverWispEntity::new, MobCategory.AMBIENT,
                    builder -> builder.sized(0.55F, 0.65F).eyeHeight(0.45F).clientTrackingRange(12)
                            .updateInterval(2));

    private FantasyEntityTypes() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(FantasyEntityTypes::createAttributes);
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        event.put(SILVER_HART.get(), Goat.createAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .build());
        event.put(ASH_HOUND.get(), Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .build());
        event.put(RIVER_WISP.get(), Allay.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.FLYING_SPEED, 0.16D)
                .build());
    }
}
