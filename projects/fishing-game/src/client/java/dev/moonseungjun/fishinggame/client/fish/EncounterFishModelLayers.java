package dev.moonseungjun.fishinggame.client.fish;

import dev.moonseungjun.fishinggame.FishingGameMod;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class EncounterFishModelLayers {
    public static final ModelLayerLocation SMALL = new ModelLayerLocation(FishingGameMod.id("encounter_small_fish"), "main");
    public static final ModelLayerLocation TALL = new ModelLayerLocation(FishingGameMod.id("encounter_tall_fish"), "main");
    public static final ModelLayerLocation FAT = new ModelLayerLocation(FishingGameMod.id("encounter_fat_fish"), "main");
    public static final ModelLayerLocation LONG = new ModelLayerLocation(FishingGameMod.id("encounter_long_fish"), "main");
    public static final ModelLayerLocation ANGLER = new ModelLayerLocation(FishingGameMod.id("encounter_angler_fish"), "main");
    public static final ModelLayerLocation CYPRINID = new ModelLayerLocation(FishingGameMod.id("encounter_cyprinid_fish"), "main");
    public static final ModelLayerLocation PELAGIC = new ModelLayerLocation(FishingGameMod.id("encounter_pelagic_fish"), "main");
    public static final ModelLayerLocation BREAM = new ModelLayerLocation(FishingGameMod.id("encounter_bream_fish"), "main");
    public static final ModelLayerLocation CATFISH = new ModelLayerLocation(FishingGameMod.id("encounter_catfish_fish"), "main");

    private EncounterFishModelLayers() {
    }
}
