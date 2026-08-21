package kr.moonseungjun.survivalascension.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.moonseungjun.survivalascension.mining.MiningProgression;
import kr.moonseungjun.survivalascension.progress.MiningProgressData;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AscensionCommands {
    private AscensionCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("ascension")
                .then(Commands.literal("stats")
                        .executes(context -> showStats(context.getSource().getPlayerOrException())))
                .then(Commands.literal("mining")
                        .then(Commands.literal("setlevel")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, MiningProgressData.MAX_LEVEL))
                                        .executes(context -> setMiningLevel(
                                                context.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "level"))))));
        event.getDispatcher().register(root);
    }

    private static int showStats(ServerPlayer player) {
        MiningProgressData data = MiningProgressData.get(player);
        int level = data.miningLevel(player);
        long totalXp = data.miningXp(player);
        long intoLevel = MiningProgressData.xpIntoLevel(totalXp);
        long next = MiningProgressData.xpForNextLevel(level);
        String progress = level >= MiningProgressData.MAX_LEVEL ? "MAX" : intoLevel + "/" + next;
        player.sendSystemMessage(Component.literal(
                "§6[Survival Ascension] §f채굴 Lv.§e" + level
                        + " §7(" + progress + ") §f| 범위 §b" + MiningProgression.areaSize(level) + "×"
                        + MiningProgression.areaSize(level) + " §f| 속도 §a"
                        + MiningProgression.formatMultiplier(MiningProgression.speedMultiplier(level))));
        return 1;
    }

    private static int setMiningLevel(ServerPlayer player, int level) {
        MiningProgressData data = MiningProgressData.get(player);
        data.setMiningLevel(player, level);
        MiningProgression.refreshMiningSpeed(player);
        player.sendSystemMessage(Component.literal("§6[테스트] §f채굴 레벨을 §e" + level + "§f로 설정했습니다. 범위: §b"
                + MiningProgression.areaSize(level) + "×" + MiningProgression.areaSize(level)
                + "§f, 속도: §a" + MiningProgression.formatMultiplier(MiningProgression.speedMultiplier(level))));
        return 1;
    }
}
