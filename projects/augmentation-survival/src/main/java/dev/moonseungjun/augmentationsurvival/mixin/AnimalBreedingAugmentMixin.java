package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.ConsumptionAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the bounded extra-offspring roll after vanilla breeding has finalized normally. */
@Mixin(Animal.class)
public abstract class AnimalBreedingAugmentMixin {
    @Unique private ServerPlayer augmentationSurvival$breedingActor;

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
    private void augmentationSurvival$captureBreedingActor(
            ServerLevel level,
            Animal partner,
            AgeableMob offspring,
            CallbackInfo ci
    ) {
        augmentationSurvival$breedingActor = ConsumptionAugmentService.breedingActor(
                (Animal) (Object) this,
                partner
        );
    }

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void augmentationSurvival$spawnTwin(
            ServerLevel level,
            Animal partner,
            AgeableMob offspring,
            CallbackInfo ci
    ) {
        ConsumptionAugmentService.trySpawnTwin(
                level,
                (Animal) (Object) this,
                partner,
                offspring,
                augmentationSurvival$breedingActor
        );
        augmentationSurvival$breedingActor = null;
    }
}
