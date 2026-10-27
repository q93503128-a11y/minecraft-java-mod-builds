package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.presentation.PassiveProcReadabilityService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.FocusedAimAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileTuning;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shot-action boundary shared by arrow saving, focused aim and scatter-shot projectiles. */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemAugmentMixin {
    private static final String ARROW_SAVER_ID = id("arrow_saver");
    private static final String SCATTER_SHOT_ID = id("scatter_shot");

    @Shadow
    protected abstract Projectile createProjectile(
            net.minecraft.world.level.Level level,
            LivingEntity shooter,
            ItemStack weapon,
            ItemStack projectile,
            boolean isCrit
    );

    @Shadow
    protected abstract void shootProjectile(
            LivingEntity shooter,
            Projectile projectileEntity,
            int index,
            float power,
            float uncertainty,
            float angle,
            @Nullable LivingEntity targetOverride
    );

    @Inject(method = "useAmmo", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$saveRootAmmo(
            ItemStack weapon,
            ItemStack projectile,
            LivingEntity holder,
            boolean forceInfinite,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (forceInfinite
                || holder.hasInfiniteMaterials()
                || !(holder instanceof ServerPlayer player)
                || !(weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem)
                || !projectile.is(ItemTags.ARROWS)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, ARROW_SAVER_ID);
        if (level <= 0) {
            return;
        }
        double chance = ProjectileTuning.current().arrowSaverChancePerLevel() * level;
        if (player.getRandom().nextDouble() >= chance) {
            return;
        }

        ItemStack copy = projectile.copyWithCount(1);
        copy.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
        cir.setReturnValue(copy);
        PassiveProcReadabilityService.presentArrowSaved(player);
    }

    @Inject(method = "shoot", at = @At("HEAD"))
    private void augmentationSurvival$spawnScatterProjectiles(
            ServerLevel level,
            LivingEntity shooter,
            InteractionHand hand,
            ItemStack weapon,
            List<ItemStack> projectiles,
            float power,
            float uncertainty,
            boolean isCrit,
            @Nullable LivingEntity targetOverride,
            CallbackInfo ci
    ) {
        if (!(shooter instanceof ServerPlayer player)) {
            return;
        }
        ProjectileWeaponItem self = (ProjectileWeaponItem) (Object) this;
        if (!(self instanceof BowItem || self instanceof CrossbowItem)) {
            return;
        }
        if (self instanceof BowItem && power < 2.99F) {
            return;
        }
        if (self instanceof BowItem) {
            FocusedAimAugmentService.prepareBowShot(player);
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int levelValue = boundedLevel(state, SCATTER_SHOT_ID);
        if (levelValue <= 0) {
            return;
        }

        ItemStack template = ItemStack.EMPTY;
        for (ItemStack projectile : projectiles) {
            if (!projectile.isEmpty() && projectile.is(ItemTags.ARROWS)) {
                template = projectile;
                break;
            }
        }
        if (template.isEmpty()) {
            return;
        }

        int count = levelValue;
        double maxSpread = ProjectileTuning.current().scatterMaxSpreadDegrees(levelValue);
        for (int index = 0; index < count; index++) {
            ItemStack auxiliaryAmmo = template.copyWithCount(1);
            auxiliaryAmmo.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            Projectile projectile = createProjectile(level, shooter, weapon, auxiliaryAmmo, isCrit);
            if (!(projectile instanceof AbstractArrow arrow)) {
                continue;
            }
            ProjectileAugmentService.markDerivedProjectile(arrow, player);
            float angle = (float) scatterAngle(player, index, count, maxSpread);
            int shotIndex = index + projectiles.size();
            Projectile.spawnProjectile(
                    projectile,
                    level,
                    auxiliaryAmmo,
                    entity -> shootProjectile(
                            shooter,
                            entity,
                            shotIndex,
                            power,
                            uncertainty,
                            angle,
                            targetOverride
                    )
            );
        }
    }

    private static double scatterAngle(
            ServerPlayer player,
            int index,
            int count,
            double maxSpread
    ) {
        if (count <= 1) {
            return player.getRandom().nextBoolean() ? maxSpread : -maxSpread;
        }
        return -maxSpread + (2.0D * maxSpread * index / (count - 1.0D));
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        if (level <= 0) {
            return 0;
        }
        return Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
