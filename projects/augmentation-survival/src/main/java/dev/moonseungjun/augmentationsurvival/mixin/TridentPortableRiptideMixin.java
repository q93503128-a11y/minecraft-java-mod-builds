package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.AdvancedUtilityAugmentService;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies the water condition and a server-side vanilla-equivalent fallback for dry portable Riptide. */
@Mixin(TridentItem.class)
public abstract class TridentPortableRiptideMixin {
    private static final ThreadLocal<ServerPlayer> AUGMENTATION_SURVIVAL$PLAYER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> AUGMENTATION_SURVIVAL$PORTABLE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "use", at = @At("HEAD"))
    private void augmentationSurvival$captureUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            AUGMENTATION_SURVIVAL$PLAYER.set(serverPlayer);
            AUGMENTATION_SURVIVAL$PORTABLE.set(
                    AdvancedUtilityAugmentService.canUsePortableRiptide(
                            serverPlayer,
                            player.getItemInHand(hand)
                    )
            );
        } else {
            clearContext();
        }
    }

    @ModifyExpressionValue(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowPortableUse(boolean original) {
        return original || Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get());
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void augmentationSurvival$clearUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        clearContext();
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void augmentationSurvival$captureRelease(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof ServerPlayer player) {
            AUGMENTATION_SURVIVAL$PLAYER.set(player);
            AUGMENTATION_SURVIVAL$PORTABLE.set(
                    AdvancedUtilityAugmentService.canUsePortableRiptide(player, stack)
            );
        } else {
            clearContext();
        }
    }

    @ModifyExpressionValue(
            method = "releaseUsing",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"
            )
    )
    private boolean augmentationSurvival$allowPortableRelease(boolean original) {
        return original || Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get());
    }

    @Inject(method = "releaseUsing", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$finishPortableRiptide(
            ItemStack stack,
            Level level,
            LivingEntity entity,
            int remainingUseTicks,
            CallbackInfoReturnable<Boolean> cir
    ) {
        try {
            ServerPlayer player = AUGMENTATION_SURVIVAL$PLAYER.get();
            boolean portable = player != null && Boolean.TRUE.equals(AUGMENTATION_SURVIVAL$PORTABLE.get());
            if (!portable) {
                return;
            }
            if (Boolean.TRUE.equals(cir.getReturnValue())) {
                AdvancedUtilityAugmentService.consumePortableRiptideWater(player);
                return;
            }
            if (!(level instanceof ServerLevel)
                    || player.isPassenger()
                    || stack.nextDamageWillBreak()
                    || stack.getUseDuration(player) - remainingUseTicks < TridentItem.THROW_THRESHOLD_TIME) {
                return;
            }
            float riptideStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
            if (!(riptideStrength > 0.0F)) {
                return;
            }

            stack.hurtWithoutBreaking(1, player);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));

            float yRot = player.getYRot();
            float xRot = player.getXRot();
            float xd = -Mth.sin(yRot * (float) (Math.PI / 180.0))
                    * Mth.cos(xRot * (float) (Math.PI / 180.0));
            float yd = -Mth.sin(xRot * (float) (Math.PI / 180.0));
            float zd = Mth.cos(yRot * (float) (Math.PI / 180.0))
                    * Mth.cos(xRot * (float) (Math.PI / 180.0));
            float distance = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            xd *= riptideStrength / distance;
            yd *= riptideStrength / distance;
            zd *= riptideStrength / distance;
            player.push(xd, yd, zd);
            player.startAutoSpinAttack(20, 8.0F, stack);
            if (player.onGround()) {
                player.move(MoverType.SELF, new Vec3(0.0D, 1.1999999D, 0.0D));
            }

            Holder<SoundEvent> sound = EnchantmentHelper
                    .pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
                    .orElse(SoundEvents.TRIDENT_THROW);
            level.playSound(null, player, sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            AdvancedUtilityAugmentService.consumePortableRiptideWater(player);
            cir.setReturnValue(true);
        } finally {
            clearContext();
        }
    }

    private static void clearContext() {
        AUGMENTATION_SURVIVAL$PLAYER.remove();
        AUGMENTATION_SURVIVAL$PORTABLE.remove();
    }
}
