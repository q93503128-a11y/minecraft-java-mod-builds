package dev.moonseungjun.openworldrpg.entity;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Project-owned entity types whose gameplay authority lives entirely in Openworld RPG. */
public final class OpenworldRpgEntityTypes {
    public static final EntityType<MeadowViperEntity> MEADOW_VIPER = register(
            "meadow_viper",
            EntityType.Builder.<MeadowViperEntity>of(MeadowViperEntity::new, MobCategory.CREATURE)
                    // Review seed derived from the pinned Snake.glb bounds at 0.35 source scale.
                    // Client acceptance of scale/ground contact remains an explicit playtest gate.
                    .sized(0.40F, 1.08F)
                    .clientTrackingRange(8)
    );

    public static final EntityType<TrailStagEntity> TRAIL_STAG = register(
            "trail_stag",
            EntityType.Builder.<TrailStagEntity>of(TrailStagEntity::new, MobCategory.CREATURE)
                    // Collision is a review seed until the exact Stag is accepted in-client.
                    .sized(TrailStagEntity.REVIEW_WIDTH, TrailStagEntity.REVIEW_HEIGHT)
                    .clientTrackingRange(10)
    );

    private OpenworldRpgEntityTypes() {
    }

    private static <T extends Entity> EntityType<T> register(
            String path,
            EntityType.Builder<T> builder
    ) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, path)
        );
        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                key,
                builder.build(key)
        );
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(
                MEADOW_VIPER,
                MeadowViperEntity.createAttributes()
        );
        FabricDefaultAttributeRegistry.register(
                TRAIL_STAG,
                TrailStagEntity.createAttributes()
        );
    }
}
