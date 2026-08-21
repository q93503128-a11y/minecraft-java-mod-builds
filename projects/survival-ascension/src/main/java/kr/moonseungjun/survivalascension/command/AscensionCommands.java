package kr.moonseungjun.survivalascension.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.moonseungjun.survivalascension.mining.MiningProgression;
import kr.moonseungjun.survivalascension.progress.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AscensionCommands {
    private AscensionCommands() {}
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ascension")
                .then(Commands.literal("stats").executes(context -> showStats(context.getSource().getPlayerOrException())))
                .then(Commands.literal("mining").then(Commands.literal("setlevel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("level", IntegerArgumentType.integer(0, SkillTuning.MAX_LEVEL)).executes(context -> setLevel(context.getSource().getPlayerOrException(), SkillType.MINING, IntegerArgumentType.getInteger(context, "level"))))))
                .then(Commands.literal("woodcutting").then(Commands.literal("setlevel").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("level", IntegerArgumentType.integer(0, SkillTuning.MAX_LEVEL)).executes(context -> setLevel(context.getSource().getPlayerOrException(), SkillType.WOODCUTTING, IntegerArgumentType.getInteger(context, "level")))))));
    }
    private static int showStats(ServerPlayer player) {
        SkillProgressData data = SkillProgressData.get(player);
        player.sendSystemMessage(Component.literal("§6[Survival Ascension] §f현재 성장"));
        sendSkillLine(player, data, SkillType.MINING);
        sendSkillLine(player, data, SkillType.WOODCUTTING);
        return 1;
    }
    private static void sendSkillLine(ServerPlayer player, SkillProgressData data, SkillType skill) {
        int level = data.level(player, skill);
        long totalXp = data.xp(player, skill), into = SkillTuning.xpIntoLevel(totalXp), next = SkillTuning.xpForNextLevel(level);
        String progress = level >= SkillTuning.MAX_LEVEL ? "MAX" : into + "/" + next;
        String extra = skill == SkillType.MINING
                ? "범위 " + SkillTuning.miningAreaSize(level) + "×" + SkillTuning.miningAreaSize(level) + " | 속도 " + MiningProgression.formatMultiplier(SkillTuning.miningSpeedMultiplier(level))
                : "연쇄 " + SkillTuning.woodcuttingLogLimit(level) + " | 속도 " + MiningProgression.formatMultiplier(SkillTuning.woodcuttingSpeedMultiplier(level));
        player.sendSystemMessage(Component.literal("§e" + skill.koreanName() + " Lv." + level + " §7(" + progress + ") §f| " + extra));
    }
    private static int setLevel(ServerPlayer player, SkillType skill, int level) {
        SkillProgressionService.setLevel(player, skill, level);
        player.sendSystemMessage(Component.literal("§6[테스트] §f" + skill.koreanName() + " 레벨을 §e" + SkillProgressData.get(player).level(player, skill) + "§f로 설정했습니다."));
        return 1;
    }
}
