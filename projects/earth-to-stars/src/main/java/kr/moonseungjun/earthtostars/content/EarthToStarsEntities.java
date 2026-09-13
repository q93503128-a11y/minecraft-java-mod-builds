package kr.moonseungjun.earthtostars.content;

import kr.moonseungjun.earthtostars.EarthToStars;
import kr.moonseungjun.earthtostars.ship.runtime.minecraft.ShipExteriorEntity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class EarthToStarsEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(EarthToStars.MOD_ID);

    public static final Supplier<EntityType<ShipExteriorEntity>> SHIP_EXTERIOR = ENTITY_TYPES.registerEntityType(
            "ship_exterior",
            ShipExteriorEntity::new,
            MobCategory.MISC,
            builder -> builder
                    .sized(5.4F, 2.3F)
                    .eyeHeight(1.25F)
                    .attach(EntityAttachment.PASSENGER, 0.0F, 1.20F, 0.35F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSummon()
    );

    private EarthToStarsEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
