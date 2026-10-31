package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.combat.ShieldCompressionDurabilityContext;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;

/** Regressions discovered by the 2026-10-30 second live-play pass. */
public final class LegendaryLiveFixBatchDGameTests implements CustomTestMethodInvoker {
    private static final String RETURN_TO_SENDER_ID =
            AugmentationSurvival.MOD_ID + ":return_to_sender";
    private static final float DURABILITY_CONSUMING_HIT = 6.0F;

    @GameTest(maxTicks = 60)
    public void manuallyClearedActiveStaysUnequippedAfterNewAcquisition(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        List<String> supported = ActiveAbilityRouter.supportedIds();
        require(supported.size() >= 3, "active router no longer has three active augments");
        String first = supported.get(0);
        String second = supported.get(1);
        String third = supported.get(2);

        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial()
                        .withAugmentLevel(first, 1)
                        .withAugmentLevel(second, 1)
        );
        require(ActiveAbilityRouter.clearSlot(player, 0), "manual active clear failed");
        AugmentationPlayerState afterClear = AugmentationPlayerStateService.state(player);
        require(ActiveAbilityRouter.resolvedSlots(afterClear).get(0).isBlank(),
                "manually cleared active immediately reappeared");

        AugmentationPlayerStateService.replace(player, afterClear.withAugmentLevel(third, 1));
        List<String> resolved = ActiveAbilityRouter.resolvedSlots(AugmentationPlayerStateService.state(player));
        require(resolved.get(0).isBlank(), "manual blank slot was auto-filled after a new acquisition");
        require(!resolved.contains(first), "manually unequipped active silently moved to another slot");
        require(!resolved.contains(third), "manual layout was silently changed by a newly acquired active");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 80)
    public void returnToSenderShockwaveBypassesExistingTargetHurtIFrames(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setYHeadRot(0.0F);
        AugmentationPlayerStateService.replace(
                player,
                AugmentationPlayerState.initial().withAugmentLevel(RETURN_TO_SENDER_ID, 3)
        );
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        LivingEntity attacker = helper.spawnMob(EntityTypes.HUSK, 3, 2, 7).withNoFreeWill().spawn();

        GameTestPlayers.runAfterDelay(helper, 2, () -> {
            require(!player.hasInfiniteMaterials(), "survival test player unexpectedly has infinite materials");
            blockHit(level, player, attacker, DURABILITY_CONSUMING_HIT);
            blockHit(level, player, attacker, DURABILITY_CONSUMING_HIT);
            attacker.setHealth(attacker.getMaxHealth());
            attacker.invulnerableTime = 10;
            float before = attacker.getHealth();
            blockHit(level, player, attacker, DURABILITY_CONSUMING_HIT);
            float dealt = before - attacker.getHealth();
            require(Math.abs(dealt - 18.0F) <= 0.05F,
                    "돌려보내기 was swallowed by an existing target hurt i-frame: dealt=" + dealt);
            require(attacker.invulnerableTime >= 10,
                    "돌려보내기 did not restore the target's pre-existing hurt i-frame");
            GameTestPlayers.succeed(helper);
        });
    }

    private static void blockHit(ServerLevel level, ServerPlayer player, LivingEntity attacker, float damage) {
        ItemStack shield = player.getOffhandItem();
        BlocksAttacks blocksAttacks = shield.get(DataComponents.BLOCKS_ATTACKS);
        require(blocksAttacks != null, "test shield is missing vanilla blocks-attacks component");
        DamageSource source = player.damageSources().mobAttack(attacker);
        float blocked = blocksAttacks.resolveBlockedDamage(source, damage, 0.0D);
        require(blocked >= 3.0F,
                "shield component did not resolve durability-consuming blocked damage: blocked=" + blocked);

        int durabilityBefore = shield.getDamageValue();
        ShieldCompressionDurabilityContext.begin(player, source, shield);
        try {
            blocksAttacks.hurtBlockingItem(level, shield, player, InteractionHand.OFF_HAND, blocked);
        } finally {
            ShieldCompressionDurabilityContext.end(player);
        }
        require(shield.getDamageValue() > durabilityBefore,
                "vanilla shield durability boundary did not consume durability");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
