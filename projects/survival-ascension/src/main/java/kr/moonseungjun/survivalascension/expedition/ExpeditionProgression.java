package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ExpeditionProgression {
    private ExpeditionProgression() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % 20 != 0 || !(player.level() instanceof ServerLevel level)) return;

        int worldStage = WorldAscensionData.get(level.getServer()).stage();
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        ExpeditionData data = ExpeditionData.get(player);
        for (ExpeditionRegion region : ExpeditionRegion.values()) {
            if (worldStage < region.requiredWorldStage() || !region.matches(biome)) continue;
            if (data.discover(player, region)) {
                SkillProgressionService.award(player, region.rewardSkill(), region.skillXp());
                player.sendSystemMessage(Component.literal("§2[원정 발견] §f" + region.koreanName()
                        + " §7· " + region.rewardSkill().koreanName() + " 숙련 XP +" + region.skillXp()
                        + " §7· 조사 " + data.count(player) + "/" + ExpeditionRegion.values().length));
                checkMilestones(player, data, worldStage);
            }
            break;
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ExpeditionData data = ExpeditionData.get(player);
        int count = data.count(player);
        if (count <= 0) return;
        player.sendSystemMessage(Component.literal("§2[원정 기록] §f조사 지역 §a" + count + "/" + ExpeditionRegion.values().length
                + (data.isMasterSurveyComplete(player) ? " §6· 현장 숙련 해방" : "")));
    }

    public static boolean hasFieldMastery(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        return WorldAscensionData.get(level.getServer()).stage() >= 2 && ExpeditionData.get(player).isMasterSurveyComplete(player);
    }

    private static void checkMilestones(ServerPlayer player, ExpeditionData data, int worldStage) {
        if (data.countStageZero(player) >= 4 && data.claimMilestone(player, ExpeditionData.MILESTONE_OVERWORLD)) {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 4));
            giveOrDrop(player, new ItemStack(Items.EMERALD, 16));
            giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, 32));
            player.sendSystemMessage(Component.literal("§a[대륙 조사 완주] §f초기 5개 원정권 중 4개 조사 완료"
                    + " §7· 다이아4 · 에메랄드16 · 자수정32"));
        }

        if (worldStage >= 1 && data.count(player) >= 7
                && data.has(player, ExpeditionRegion.DEEP) && data.has(player, ExpeditionRegion.NETHER)
                && data.claimMilestone(player, ExpeditionData.MILESTONE_LEGENDARY)) {
            giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, 2));
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 16));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 32));
            player.sendSystemMessage(Component.literal("§b[전설 원정 완주] §f심층권·네더권 포함 7개 지역 조사 완료"
                    + " §7· 네더라이트 파편2 · 다이아16 · 메아리32"));
        }

        if (worldStage >= 2 && data.isMasterSurveyComplete(player)
                && data.claimMilestone(player, ExpeditionData.MILESTONE_MASTER)) {
            giveOrDrop(player, AscensionAffixes.createEliteDrop(player.level().getRandom(), 3));
            giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, 4));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 64));
            giveOrDrop(player, new ItemStack(Items.DRAGON_BREATH, 16));
            player.giveExperiencePoints(500);
            player.sendSystemMessage(Component.literal("§6[대원정 완주] §f9개 원정권 조사 완료 · §e현장 숙련 해방!"));
            player.sendSystemMessage(Component.literal("§7Lv.100에서 채석장 터널·벌목·농사·전투 충격파·건축·공중 돌진이 원정 완주형 최종 체급으로 확장됩니다."));
            player.sendSystemMessage(Component.literal("§7보상 · 신화 III 1개 · 네더라이트 파편4 · 메아리64 · 드래곤의 숨결16 · 경험치500"));
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
