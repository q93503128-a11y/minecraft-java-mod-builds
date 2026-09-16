package kr.moonseungjun.earthtostars.fabric.content;

import kr.moonseungjun.earthtostars.fabric.EarthToStarsFabric;
import kr.moonseungjun.earthtostars.fabric.entity.LaunchCraftEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class EarthToStarsFabricEntities {
    public static final ResourceKey<EntityType<?>> LAUNCH_CRAFT_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            EarthToStarsFabric.id("launch_craft")
    );

    public static final EntityType<LaunchCraftEntity> LAUNCH_CRAFT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            LAUNCH_CRAFT_KEY,
            EntityType.Builder.<LaunchCraftEntity>of(LaunchCraftEntity::new, MobCategory.MISC)
                    .sized(5.4F, 2.3F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build(LAUNCH_CRAFT_KEY)
    );

    private EarthToStarsFabricEntities() {
    }

    public static void initialize() {
        // Class initialization performs registration.
    }
}
