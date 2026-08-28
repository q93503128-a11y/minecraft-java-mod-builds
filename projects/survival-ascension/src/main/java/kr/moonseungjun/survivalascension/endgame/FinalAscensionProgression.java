package kr.moonseungjun.survivalascension.endgame;

import kr.moonseungjun.survivalascension.apex.ApexHuntData;
import kr.moonseungjun.survivalascension.expedition.ExpeditionData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical gate for Survival Ascension's final encounter.
 *
 * The requirements intentionally reuse the existing persistent authorities instead of creating
 * shadow progression: world stage 2 is the Ender Dragon milestone, ExpeditionData owns the nine
 * regional directives, ApexHuntData owns the nine first-victory bits, and InfrastructureData owns
 * the physical Ascension Nexus commissioning state.
 */
public final class FinalAscensionProgression {
    public static final int REQUIRED_EXPEDITIONS = 9;
    public static final int REQUIRED_APEX = 9;

    private FinalAscensionProgression() {}

    public static Requirements requirements(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return new Requirements(false, 0, 0, false);
        }
        boolean dragonCleared = WorldAscensionData.get(level.getServer()).stage() >= 2;
        ExpeditionData expeditions = ExpeditionData.get(player);
        ApexHuntData apex = ApexHuntData.get(player);
        boolean nexusComplete = InfrastructureData.get(player).isComplete(InfrastructureProject.ASCENSION_NEXUS);
        return new Requirements(
                dragonCleared,
                expeditions.countCompleted(player),
                apex.uniqueDefeated(player),
                nexusComplete
        );
    }

    public static boolean isReady(ServerPlayer player) {
        return requirements(player).ready();
    }

    public static void sendStatus(ServerPlayer player) {
        Requirements req = requirements(player);
        player.sendSystemMessage(Component.literal("§6[최후의 승천 준비] §f" + req.completedRequirements() + "/4"));
        player.sendSystemMessage(Component.literal(check(req.enderDragonCleared())
                + " §f엔더 드래곤 격파 §7· 월드 종말 단계"));
        player.sendSystemMessage(Component.literal(check(req.expeditionsComplete())
                + " §f9개 원정권 완주 §7· §e" + Math.min(REQUIRED_EXPEDITIONS, req.expeditionsCompleted()) + "/9"));
        player.sendSystemMessage(Component.literal(check(req.apexComplete())
                + " §f9개 지역 정점 최초 격파 §7· §e" + Math.min(REQUIRED_APEX, req.apexDefeated()) + "/9"));
        player.sendSystemMessage(Component.literal(check(req.ascensionNexusComplete())
                + " §f승천 중추 완공"));

        if (req.ready()) {
            player.sendSystemMessage(Component.literal("§6[최후의 승천 준비 완료] §f모든 종결 조건을 충족했습니다. §7승천 중추가 마지막 결전을 받아들일 수 있는 상태입니다."));
        } else {
            player.sendSystemMessage(Component.literal("§7남은 조건 · " + String.join(" · ", req.missingRequirements())));
        }
    }

    private static String check(boolean done) {
        return done ? "§a[완료]" : "§c[미완료]";
    }

    public record Requirements(
            boolean enderDragonCleared,
            int expeditionsCompleted,
            int apexDefeated,
            boolean ascensionNexusComplete
    ) {
        public boolean expeditionsComplete() { return expeditionsCompleted >= REQUIRED_EXPEDITIONS; }
        public boolean apexComplete() { return apexDefeated >= REQUIRED_APEX; }
        public boolean ready() {
            return enderDragonCleared && expeditionsComplete() && apexComplete() && ascensionNexusComplete;
        }
        public int completedRequirements() {
            int count = 0;
            if (enderDragonCleared) count++;
            if (expeditionsComplete()) count++;
            if (apexComplete()) count++;
            if (ascensionNexusComplete) count++;
            return count;
        }
        public List<String> missingRequirements() {
            List<String> missing = new ArrayList<>();
            if (!enderDragonCleared) missing.add("엔더 드래곤");
            if (!expeditionsComplete()) missing.add("원정 " + Math.min(REQUIRED_EXPEDITIONS, expeditionsCompleted) + "/9");
            if (!apexComplete()) missing.add("정점 " + Math.min(REQUIRED_APEX, apexDefeated) + "/9");
            if (!ascensionNexusComplete) missing.add("승천 중추");
            return List.copyOf(missing);
        }
    }
}
