package kr.moonseungjun.survivalascension.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
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
        player.sendSystemMessage(Component.literal("§2[원정] §f" + expedition.count(player) + "/9 조사 · §7" + stage
                + (expedition.isMasterSurveyComplete(player) ? " §6· 현장 숙련 해방" : "")));
        player.sendSystemMessage(Component.literal(expedition.summary(player)));
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
