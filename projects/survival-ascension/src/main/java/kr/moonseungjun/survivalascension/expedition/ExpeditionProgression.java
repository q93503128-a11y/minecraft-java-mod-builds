package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillType;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ExpeditionProgression {
    private static final Map<UUID, VoyageState> OCEAN_VOYAGE = new HashMap<>();

    private ExpeditionProgression() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        if (player.tickCount % 20 != 0 || !(player.level() instanceof ServerLevel level)) return;

        int worldStage = WorldAscensionData.get(level.getServer()).stage();
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        ExpeditionRegion current = matchingRegion(biome, worldStage);
        if (current != null) ensureDiscovered(player, current);
        trackOceanVoyage(player, current);
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ExpeditionData data = ExpeditionData.get(player);
        int discovered = data.count(player);
        int completed = data.countCompleted(player);
        if (discovered <= 0) return;
        player.sendSystemMessage(Component.literal("§2[원정 기록] §f발견 §e" + discovered + "/9 §7· 완수 §a" + completed + "/9"
                + (data.isMasterSurveyComplete(player) ? " §6· 현장 숙련 해방" : "")));
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        OCEAN_VOYAGE.remove(event.getEntity().getUUID());
    }

    public static boolean hasFieldMastery(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        return WorldAscensionData.get(level.getServer()).stage() >= 2 && ExpeditionData.get(player).isMasterSurveyComplete(player);
    }

    public static void recordSkillAction(ServerPlayer player, SkillType skill, int amount) {
        recordAction(player, ExpeditionAction.fromSkill(skill), amount);
    }

    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {
        if (amount <= 0 || player.isCreative() || player.isSpectator() || !(player.level() instanceof ServerLevel level)) return;
        int worldStage = WorldAscensionData.get(level.getServer()).stage();
        ExpeditionRegion region = matchingRegion(level.getBiome(player.blockPosition()), worldStage);
        if (region == null) return;
        ensureDiscovered(player, region);
        addObjectiveProgress(player, region, action, amount);
    }

    private static void trackOceanVoyage(ServerPlayer player, ExpeditionRegion current) {
        UUID uuid = player.getUUID();
        Vec3 pos = player.position();
        ResourceKey<Level> dimension = player.level().dimension();
        VoyageState old = OCEAN_VOYAGE.get(uuid);
        OCEAN_VOYAGE.put(uuid, new VoyageState(dimension, pos.x, pos.z));
        if (current != ExpeditionRegion.OCEAN) return;
        if (!(player.isPassenger() || player.isSwimming() || player.isInWater())) return;
        if (old == null || !old.dimension.equals(dimension)) return;
        double dx = pos.x - old.x;
        double dz = pos.z - old.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.25D || distance > 24.0D) return;
        addObjectiveProgress(player, ExpeditionRegion.OCEAN, ExpeditionAction.OCEAN_VOYAGE,
                Math.max(1, (int) Math.floor(distance)));
    }

    private static ExpeditionRegion matchingRegion(Holder<Biome> biome, int worldStage) {
        for (ExpeditionRegion region : ExpeditionRegion.values()) {
            if (worldStage < region.requiredWorldStage()) continue;
            if (region.matches(biome)) return region;
        }
        return null;
    }

    private static void ensureDiscovered(ServerPlayer player, ExpeditionRegion region) {
        ExpeditionData data = ExpeditionData.get(player);
        int option = player.level().getRandom().nextInt(ExpeditionDirective.optionCount(region));
        if (!data.discover(player, region, option)) return;
        ExpeditionDirective directive = data.directive(player, region);
        player.sendSystemMessage(Component.literal("§2[원정 발견] §f" + region.koreanName()
                + " §7· 현장 지령: §e" + directive.koreanName()));
        player.sendSystemMessage(Component.literal("§7" + data.directiveSummary(player, region)
                + " §8· 모든 항목을 이 지역 안에서 수행해야 합니다."));
    }

    private static void addObjectiveProgress(ServerPlayer player, ExpeditionRegion region, ExpeditionAction action, int amount) {
        ExpeditionData data = ExpeditionData.get(player);
        if (data.isComplete(player, region)) return;
        ExpeditionData.ProgressResult result = data.addProgress(player, region, action, amount);
        if (result.target() <= 0 || result.newProgress() == result.oldProgress()) return;

        if (result.regionCompletedNow()) {
            boolean rewardGranted = data.claimRegionReward(player, region);
            if (rewardGranted) SkillProgressionService.award(player, region.rewardSkill(), region.skillXp());
            String rewardText = rewardGranted
                    ? " §7· " + region.rewardSkill().koreanName() + " 숙련 XP +" + region.skillXp()
                    : " §7· 기존 0.23 발견 보상 승계";
            player.sendSystemMessage(Component.literal("§a[원정 완수] §f" + region.koreanName() + " · §e"
                    + data.directive(player, region).koreanName() + rewardText + " §7· 완수 " + data.countCompleted(player) + "/9"));
            checkMilestones(player, data, WorldAscensionData.get(((ServerLevel) player.level()).getServer()).stage());
            return;
        }

        if (result.taskCompletedNow()) {
            player.sendSystemMessage(Component.literal("§2[지령 항목 완료] §f" + region.koreanName() + " · "
                    + action.koreanName() + " §a" + result.newProgress() + "/" + result.target()
                    + " §7· 남은 지령: " + data.directiveSummary(player, region)));
            return;
        }

        int oldQuarter = result.oldProgress() * 4 / result.target();
        int newQuarter = result.newProgress() * 4 / result.target();
        if (newQuarter > oldQuarter) {
            player.sendSystemMessage(Component.literal("§2[원정 진행] §f" + region.koreanName() + " · " + action.koreanName()
                    + " §e" + result.newProgress() + "/" + result.target()), true);
        }
    }

    private static void checkMilestones(ServerPlayer player, ExpeditionData data, int worldStage) {
        if (data.countStageZeroCompleted(player) >= 4 && data.claimMilestone(player, ExpeditionData.MILESTONE_OVERWORLD)) {
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 4));
            giveOrDrop(player, new ItemStack(Items.EMERALD, 16));
            giveOrDrop(player, new ItemStack(Items.AMETHYST_SHARD, 32));
            player.sendSystemMessage(Component.literal("§a[대륙 원정 완주] §f초기 5개 원정권 중 4개 현장 지령 완료"
                    + " §7· 다이아4 · 에메랄드16 · 자수정32"));
        }

        if (worldStage >= 1 && data.countCompleted(player) >= 7
                && data.isComplete(player, ExpeditionRegion.DEEP) && data.isComplete(player, ExpeditionRegion.NETHER)
                && data.claimMilestone(player, ExpeditionData.MILESTONE_LEGENDARY)) {
            giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, 2));
            giveOrDrop(player, new ItemStack(Items.DIAMOND, 16));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 32));
            player.sendSystemMessage(Component.literal("§b[전설 원정 완주] §f심층권·네더권 포함 7개 현장 지령 완료"
                    + " §7· 네더라이트 파편2 · 다이아16 · 메아리32"));
        }

        if (worldStage >= 2 && data.isMasterSurveyComplete(player)
                && data.claimMilestone(player, ExpeditionData.MILESTONE_MASTER)) {
            giveOrDrop(player, AscensionAffixes.createEliteDrop(player.level().getRandom(), 3));
            giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, 4));
            giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 64));
            giveOrDrop(player, new ItemStack(Items.DRAGON_BREATH, 16));
            player.giveExperiencePoints(500);
            player.sendSystemMessage(Component.literal("§6[대원정 완주] §f9개 원정권 현장 지령 완료 · §e현장 숙련 해방!"));
            player.sendSystemMessage(Component.literal("§7Lv.100에서 채석장 터널·벌목·농사·전투 충격파·건축·공중 돌진이 원정 완주형 최종 체급으로 확장됩니다."));
            player.sendSystemMessage(Component.literal("§7보상 · 신화 III 1개 · 네더라이트 파편4 · 메아리64 · 드래곤의 숨결16 · 경험치500"));
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record VoyageState(ResourceKey<Level> dimension, double x, double z) {}
}
