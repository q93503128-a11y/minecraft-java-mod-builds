package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registration boundary for temporary server-authoritative combat clones. */
public final class CloneEntityTypes {
    private static final ResourceKey<EntityType<?>> COMBAT_CLONE_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "combat_clone")
    );

    public static final EntityType<CombatCloneEntity> COMBAT_CLONE = register(
            COMBAT_CLONE_KEY,
            EntityType.Builder.<CombatCloneEntity>of(CombatCloneEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F),
            CombatCloneEntity.createAttributes()
    );

    private CloneEntityTypes() {
    }

    public static void initialize() {
        // Static initialization performs registration.
    }

    private static <T extends Entity> EntityType<T> register(
            ResourceKey<EntityType<?>> key,
            EntityType.Builder<T> builder
    ) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    private static <T extends net.minecraft.world.entity.LivingEntity> EntityType<T> register(
            ResourceKey<EntityType<?>> key,
            EntityType.Builder<T> builder,
            net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes
    ) {
        EntityType<T> type = register(key, builder);
        FabricDefaultAttributeRegistry.register(type, attributes);
        return type;
    }
}
