package kr.moonseungjun.survivalascension.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.moonseungjun.survivalascension.apex.ApexHuntData;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import kr.moonseungjun.survivalascension.expedition.ExpeditionRegion;
import kr.moonseungjun.survivalascension.production.FieldDepotData;
import kr.moonseungjun.survivalascension.production.FieldDepotService;
import kr.moonseungjun.survivalascension.production.ProductionData;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AscensionCommands {
    private AscensionCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ascension")
                .then(Commands.literal("stats").executes(context -> showStats(context.getSource().getPlayerOrException())))
                .then(skillSetLevelNode("mining", SkillType.MINING))
                .then(skillSetLevelNode("woodcutting", SkillType.WOODCUTTING))
                .then(skillSetLevelNode("harvesting", SkillType.HARVESTING))
                .then(skillSetLevelNode("combat", SkillType.COMBAT))
                .then(skillSetLevelNode("construction", SkillType.CONSTRUCTION))
                .then(skillSetLevelNode("mobility", SkillType.MOBILITY)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> skillSetLevelNode(String literal, SkillType skill) {
        return Commands.literal(literal)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, SkillTuning.MAX_LEVEL))
                                .executes(context -> setLevel(context.getSource().getPlayerOrException(), skill,
                                        IntegerArgumentType.getInteger(context, "level")))));
    }

    private static int showStats(ServerPlayer player) {
        SkillProgressData data = SkillProgressData.get(player);
        player.sendSystemMessage(Component.literal("§6[Survival Ascension] §f숙련 현황"));
        for (SkillType skill : SkillType.values()) sendSkillLine(player, data, skill);

        ExpeditionData expedition = ExpeditionData.get(player);
        String stage = WorldAscensionData.get(((ServerLevel) player.level()).getServer()).stageName();
        int incidentsResolved = 0;
        for (ExpeditionRegion region : ExpeditionRegion.values()) if (expedition.incidentResolved(player, region)) incidentsResolved++;
        player.sendSystemMessage(Component.literal("§2[원정] §f발견 §e" + expedition.count(player) + "/9 §7· 완수 §a"
                + expedition.countCompleted(player) + "/9 §7· 사건 해결 §6" + incidentsResolved + "/9 §7· " + stage
                + (expedition.isMasterSurveyComplete(player) ? " §6· 현장 숙련 해방" : "")));
        player.sendSystemMessage(Component.literal(expedition.summary(player)));
        for (ExpeditionRegion region : ExpeditionRegion.values()) {
            if (!expedition.isDiscovered(player, region) || expedition.isComplete(player, region)) continue;
            player.sendSystemMessage(Component.literal("§e" + region.koreanName() + " §7· §f"
                    + expedition.directiveSummary(player, region)
                    + (expedition.incidentResolved(player, region) ? " §6· 사건 해결 완료" : "")));
        }

        ApexHuntData apex = ApexHuntData.get(player);
        player.sendSystemMessage(Component.literal("§4[정점 사냥] §f최초 격파 §e" + apex.uniqueDefeated(player)
                + "/9 §7· 총 승리 §f" + apex.victories(player)
                + (apex.masteryClaimed(player) ? " §6· 9종 완주 보상 수령" : "")));

        ProductionData production = ProductionData.get(player);
        FieldDepotData depots = FieldDepotData.get(player);
        player.sendSystemMessage(Component.literal("§3[산업 생산망] §f누적 사이클 §b" + production.cycles(player)
                + " §7· 현장 보급권 §e" + production.supplyCharges(player) + "/" + ProductionData.MAX_SUPPLY_CHARGES
                + " §7· 물류 거점 §f" + depots.count(player) + "/" + FieldDepotData.MAX_DEPOTS_PER_PLAYER
                + " §7(현재 활성 §a" + FieldDepotService.activeDepotCount(player) + "§7)"));
        return 1;
    }

    private static void sendSkillLine(ServerPlayer player, SkillProgressData data, SkillType skill) {
        int level = data.level(player, skill);
        long xp = data.xp(player, skill);
        long into = SkillTuning.xpIntoLevel(xp);
        long next = SkillTuning.xpForNextLevel(level);
        String progress = level >= SkillTuning.MAX_LEVEL ? "MAX" : into + "/" + next + " XP";
        String extra = switch (skill) {
            case MINING -> "굴착 " + SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level)
                    + " | 광맥 " + (SkillTuning.miningVeinLimit(level) <= 1 ? "잠김" : "최대 " + SkillTuning.miningVeinLimit(level))
                    + " | 속도 " + fmt(SkillTuning.miningSpeedMultiplier(level));
            case WOODCUTTING -> "연결 로그 " + SkillTuning.woodcuttingLogLimit(level) + " | 속도 " + fmt(SkillTuning.woodcuttingSpeedMultiplier(level));
            case HARVESTING -> "범위 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level)
                    + " | 속도 " + fmt(SkillTuning.harvestingSpeedMultiplier(level));
            case COMBAT -> "피해 " + fmt(SkillTuning.combatDamageMultiplier(level))
                    + " | 파급 " + (SkillTuning.combatCleaveTargetLimit(level) <= 0 ? "잠김" : SkillTuning.combatCleaveTargetLimit(level) + "체");
            case CONSTRUCTION -> "선 " + SkillTuning.constructionLineLength(level)
                    + " | 면 " + SkillTuning.constructionPlaneSize(level) + "×" + SkillTuning.constructionPlaneSize(level);
            case MOBILITY -> "이속 " + fmt(SkillTuning.mobilitySpeedMultiplier(level))
                    + " | 단차 " + String.format(java.util.Locale.ROOT, "%.2f", SkillTuning.mobilityStepHeight(level))
                    + " | 안전낙하 " + String.format(java.util.Locale.ROOT, "%.0f", SkillTuning.mobilitySafeFallDistance(level))
                    + " | R " + (level < 30 ? "잠김" : SkillTuning.mobilityDashCooldownTicks(level) / 20.0D + "초");
        };
        player.sendSystemMessage(Component.literal("§e" + skill.koreanName() + " §fLv." + level + " §7(" + progress + ") §8- §f" + extra));
    }

    private static String fmt(double value) { return String.format(java.util.Locale.ROOT, "%.2f×", value); }

    private static int setLevel(ServerPlayer player, SkillType skill, int level) {
        SkillProgressionService.setLevel(player, skill, level);
        player.sendSystemMessage(Component.literal("§6[Survival Ascension] §f" + skill.koreanName() + " 레벨을 §e" + level + "§f로 설정했습니다."));
        return 1;
    }
}
