package dev.moonseungjun.fishinggame.entity;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishVisualFamily;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.fish.AbstractFish;

public final class FishingEntities {
    public static final EntityType<EncounterFishEntity> SMALL_FISH = register("encounter_small_fish", 0.55f, 0.35f);
    public static final EntityType<EncounterFishEntity> TALL_FISH = register("encounter_tall_fish", 0.55f, 0.72f);
    public static final EntityType<EncounterFishEntity> FAT_FISH = register("encounter_fat_fish", 0.85f, 0.58f);
    public static final EntityType<EncounterFishEntity> LONG_FISH = register("encounter_long_fish", 1.15f, 0.48f);
    public static final EntityType<EncounterFishEntity> ANGLER_FISH = register("encounter_angler_fish", 1.05f, 0.82f);
    public static final EntityType<EncounterFishEntity> CYPRINID_FISH = register("encounter_cyprinid_fish", 0.80f, 0.62f);
    public static final EntityType<EncounterFishEntity> PELAGIC_FISH = register("encounter_pelagic_fish", 0.95f, 0.48f);
    public static final EntityType<EncounterFishEntity> BREAM_FISH = register("encounter_bream_fish", 0.70f, 0.78f);
    public static final EntityType<EncounterFishEntity> CATFISH_FISH = register("encounter_catfish_fish", 1.00f, 0.53f);

    private FishingEntities() {
    }

    public static void initialize() {
    }

    public static EntityType<EncounterFishEntity> typeFor(FishVisualFamily family) {
        return switch (family) {
            case SMALL -> SMALL_FISH;
            case TALL -> TALL_FISH;
            case FAT -> FAT_FISH;
            case LONG -> LONG_FISH;
            case ANGLER -> ANGLER_FISH;
            case CYPRINID -> CYPRINID_FISH;
            case PELAGIC -> PELAGIC_FISH;
            case BREAM -> BREAM_FISH;
            case CATFISH -> CATFISH_FISH;
        };
    }

    private static EntityType<EncounterFishEntity> register(String path, float width, float height) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, FishingGameMod.id(path));
        EntityType<EncounterFishEntity> type = EntityType.Builder.of(EncounterFishEntity::new, MobCategory.MISC)
                .sized(width, height)
                .clientTrackingRange(10)
                .updateInterval(1)
                .noLootTable()
                .noSave()
                .build(key);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
        FabricDefaultAttributeRegistry.register(type, AbstractFish.createAttributes());
        return type;
    }
}
