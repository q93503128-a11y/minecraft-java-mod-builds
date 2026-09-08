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
                    .sized(2.4F, 1.35F)
                    .eyeHeight(0.85F)
                    .attach(EntityAttachment.PASSENGER, 0.0F, -0.50F, 0.12F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
    );

    private EarthToStarsEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
