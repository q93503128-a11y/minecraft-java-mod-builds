package kr.moonseungjun.survivalascension.fishing;

import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

// Catch-driven skill entry point; kept isolated from unrelated expedition action counters.
public final class FishingProgression {
    private static final int METER = 1000;
    private static final int HARBOR_BONUS_CATCH_MILLI = 350;
    private static final int HARBOR_PRESERVATION_MILLI = 150;
    private static final double HARBOR_XP_MULTIPLIER = 1.25D;

    private FishingProgression() {}

    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        int oldLevel = SkillProgressData.get(player).level(player, SkillType.FISHING);
        boolean harbor = InfrastructureData.get(player).isComplete(InfrastructureProject.ANGLER_HARBOR);
        int rawXp = xpForCatch(event);
        if (harbor) rawXp = Math.max(1, (int) Math.round(rawXp * HARBOR_XP_MULTIPLIER));
        SkillProgressData.AddXpResult result = SkillProgressionService.award(player, SkillType.FISHING, rawXp);
        applyBonusCatch(player, event, oldLevel, harbor);
        preserveRod(player, event, oldLevel, harbor);
        announceMilestones(player, result);
    }

    private static int xpForCatch(ItemFishedEvent event) {
        int xp = 8;
        for (ItemStack stack : event.getDrops()) {
            if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
                xp += 4;
            } else if (stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.NAME_TAG) || stack.is(Items.NAUTILUS_SHELL)
                    || stack.is(Items.SADDLE) || stack.is(Items.BOW) || stack.is(Items.FISHING_ROD)) {
                xp += 12;
            } else {
                xp += 2;
            }
        }
        return Math.max(8, Math.min(32, xp));
    }

    private static void applyBonusCatch(ServerPlayer player, ItemFishedEvent event, int level, boolean harbor) {
        ItemStack fish = ItemStack.EMPTY;
        for (ItemStack stack : event.getDrops()) {
            if (isFishCatch(stack)) { fish = stack; break; }
        }
        if (fish.isEmpty()) return;

        int gain = (int) Math.round(SkillTuning.fishingBonusCatchChance(level) * METER)
                + (harbor ? HARBOR_BONUS_CATCH_MILLI : 0);
        if (gain <= 0) return;
        SkillProgressData data = SkillProgressData.get(player);
        int meter = data.fishingBonusMilli(player) + gain;
        int extra = meter / METER;
        data.setFishingBonusMilli(player, meter % METER);
        if (extra > 0) fish.grow(extra);
    }

    private static boolean isFishCatch(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH);
    }

    private static void preserveRod(ServerPlayer player, ItemFishedEvent event, int level, boolean harbor) {
        int damage = event.getRodDamage();
        if (damage <= 0) return;
        int gain = (int) Math.round(SkillTuning.fishingRodPreservationChance(level) * METER)
                + (harbor ? HARBOR_PRESERVATION_MILLI : 0);
        gain = Math.max(0, Math.min(METER, gain));
        if (gain <= 0) return;
        SkillProgressData data = SkillProgressData.get(player);
        int meter = data.fishingPreserveMilli(player) + gain;
        if (meter >= METER) {
            event.damageRodBy(Math.max(0, damage - 1));
            meter -= METER;
        }
        data.setFishingPreserveMilli(player, meter);
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 10% 누적 · 물고기 추가 어획 10% 누적"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 25% · 추가 어획 25% · 확률 추첨 대신 누적 보장"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 45% · 추가 어획 50% 누적"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f마모 절약 65% · 추가 어획 75% 누적"));
        if (oldLevel < 100 && newLevel >= 100) player.sendSystemMessage(Component.literal("§3[낚시 숙련 VI] §f마모 절약 80% · 물고기 추가 어획 매회 +1 보장"));
    }
}
