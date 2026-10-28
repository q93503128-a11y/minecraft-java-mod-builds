package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import java.util.Set;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
public interface ReactiveCombatReflectedAccessor {
    @Accessor("REFLECTED_PROJECTILES")
    static Set<Projectile> augmentationSurvival$getReflectedProjectiles() {
        throw new AssertionError();
    }
}
