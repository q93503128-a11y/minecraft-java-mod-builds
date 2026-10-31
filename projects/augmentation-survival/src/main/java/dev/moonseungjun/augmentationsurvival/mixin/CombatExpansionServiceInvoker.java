package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow bridge for the repaired Lightning proc without duplicating the common damage pipeline. */
@Mixin(value = CombatExpansionService.class, remap = false)
public interface CombatExpansionServiceInvoker {
    @Accessor("LIGHTNING_HITS")
    static Map<UUID, Integer> augmentationSurvival$lightningHits() {
        throw new AssertionError();
    }

    @Invoker("applyDerivedDamage")
    static void augmentationSurvival$applyDerivedDamage(
            ServerPlayer attacker,
            LivingEntity target,
            float amount,
            CombatProvenance provenance
    ) {
        throw new AssertionError();
    }
}
