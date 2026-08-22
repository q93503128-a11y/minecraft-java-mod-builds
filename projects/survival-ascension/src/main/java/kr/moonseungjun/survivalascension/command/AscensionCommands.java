package kr.moonseungjun.survivalascension.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AscensionCommands {
    private AscensionCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ascension")
                .then(Commands.literal("stats").executes(context -> showStats(context.getSource())))
                .then(skillSetLevelNode("mining", SkillType.MINING))
                .then(skillSetLevelNode("woodcutting", SkillType.WOODCUTTING))
                .then(skillSetLevelNode("harvesting", SkillType.HARVESTING)));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> skillSetLevelNode(String literal, SkillType skill) {
        return Commands.literal(literal)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, SkillTuning.MAX_LEVEL))
                                .executes(context -> setLevel(
                                        context.getSource(), skill, IntegerArgumentType.getInteger(context, "level")))));
    }

    private static int showStats(CommandSourceStack source) {
        ServerPlayer player = source.getPlayerOrException();
        SkillProgressData data = SkillProgressData.get(player);
        source.sendSuccess(() -> Component.literal("§6[Survival Ascension] §f숙련 현황"), false);
        sendSkillLine(source, data, player, SkillType.MINING);
        sendSkillLine(source, data, player, SkillType.WOODCUTTING);
        sendSkillLine(source, data, player, SkillType.HARVESTING);
        return 1;
    }

    private static void sendSkillLine(CommandSourceStack source, SkillProgressData data, ServerPlayer player, SkillType skill) {
        int level = data.level(player, skill);
        long xp = data.xp(player, skill);
        long into = SkillTuning.xpIntoLevel(xp);
        long next = SkillTuning.xpForNextLevel(level);
        String progress = level >= SkillTuning.MAX_LEVEL ? "MAX" : into + "/" + next + " XP";
        String extra = switch (skill) {
            case MINING -> "범위 " + SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level)
                    + " | 속도 " + String.format(java.util.Locale.ROOT, "%.2f×", SkillTuning.miningSpeedMultiplier(level));
            case WOODCUTTING -> "연결 로그 " + SkillTuning.woodcuttingLogLimit(level)
                    + " | 속도 " + String.format(java.util.Locale.ROOT, "%.2f×", SkillTuning.woodcuttingSpeedMultiplier(level));
            case HARVESTING -> "범위 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level)
                    + " | 속도 " + String.format(java.util.Locale.ROOT, "%.2f×", SkillTuning.harvestingSpeedMultiplier(level));
            default -> "준비 중";
        };
        source.sendSuccess(() -> Component.literal("§e" + skill.koreanName() + " §fLv." + level + " §7(" + progress + ") §8- §f" + extra), false);
    }

    private static int setLevel(CommandSourceStack source, SkillType skill, int level) {
        ServerPlayer player = source.getPlayerOrException();
        SkillProgressionService.setLevel(player, skill, level);
        source.sendSuccess(() -> Component.literal("§6[Survival Ascension] §f" + skill.koreanName() + " 레벨을 §e" + level + "§f로 설정했습니다."), false);
        return 1;
    }
}
