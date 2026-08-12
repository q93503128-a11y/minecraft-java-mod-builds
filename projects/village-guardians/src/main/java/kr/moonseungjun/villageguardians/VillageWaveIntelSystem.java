package kr.moonseungjun.villageguardians;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VillageWaveIntelSystem {
    private VillageWaveIntelSystem() {}

    public static List<WavePreview> previews(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        int day = VillageCouncilState.currentDay();
        int players = VillageProgressionSystem.previewRaidPlayerCount(server);
        int maximum = VillageRaidSystem.previewMaxWaves(day);
        List<WavePreview> result = new ArrayList<>();
        for (int wave = 1; wave <= maximum; wave++) {
            VillageWaveTrait trait = VillageWaveTrait.select(day, wave);
            int count = VillageRaidSystem.previewWaveCount(day, wave, players, trait);
            int bosses = VillageRaidSystem.previewBossCount(day, wave, maximum, count);
            Map<VillageEnemyArchetypeSystem.Archetype, Integer> roster = new LinkedHashMap<>();
            List<String> bossLines = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                boolean boss = index < bosses;
                VillageEnemyArchetypeSystem.Archetype archetype =
                        VillageEnemyArchetypeSystem.previewArchetype(day, wave, index, boss, trait);
                roster.merge(archetype, 1, Integer::sum);
                if (boss) bossLines.add(archetype.displayName() + " · "
                        + VillageBossAspectSystem.previewText(day, wave, index));
            }
            List<String> lines = new ArrayList<>();
            List<String> siege = new ArrayList<>();
            roster.forEach((type, amount) -> {
                lines.add(type.displayName() + " ×" + amount + " · " + VillageEnemyArchetypeSystem.combatRole(type));
                if (VillageEnemyArchetypeSystem.structureDamageMultiplier(type) >= 1.20f
                        || VillageEnemyArchetypeSystem.prefersTower(type)) {
                    siege.add(type.displayName() + " ×" + amount);
                }
            });
            String direction = VillageAttackPlanSystem.scoutLine(day, wave, count);
            String elite = VillageEnemyEliteSystem.scoutSummary(day, count);
            String bossDoctrine = bosses <= 0 ? "없음" : VillageSiegeBossSystem.previewBossMechanic(day);
            String detail = "예상 총 " + count + "명" + (bosses > 0 ? " · 보스 " + bosses + "명" : "")
                    + "\n" + direction
                    + "\n공성 병과: " + (siege.isEmpty() ? "뚜렷한 전담 병과 없음" : String.join(" · ", siege))
                    + "\n정예: " + elite
                    + "\n보스 전투 구조: " + bossDoctrine
                    + "\n특성: " + trait.description() + "\n대응: " + trait.counterHint()
                    + (bossLines.isEmpty() ? "" : "\n보스 변이:\n- " + String.join("\n- ", bossLines))
                    + "\n병력:\n- " + String.join("\n- ", lines);
            result.add(new WavePreview(wave, maximum, trait, count, bosses, detail));
        }
        return List.copyOf(result);
    }

    public static String report(ServerPlayer player) {
        if (VillageCouncilState.currentPhase() != VillageTimePhase.DAY) {
            return "현재 야간 습격이 진행 중입니다.\n" + VillageRaidSystem.status()
                    + "\n전체 다음 밤 편성표는 낮 정비 시간에 확인할 수 있습니다.";
        }
        int players = VillageProgressionSystem.previewRaidPlayerCount(player.level().getServer());
        return "제 " + VillageCouncilState.currentDay() + "일 밤 예정 편성 · 기준 수호자 "
                + players + "명\n주공·별동대·전장 상황·웨이브 특성·병과·수량은 낮에 미리 공개됩니다."
                + " 추가 수호자 1명당 적 수는 약 +30%이며 공격 방향 자체는 인원수로 늘어나지 않습니다.";
    }

    public static String report() {
        return "낮 정비 시간에 성벽 또는 병영 단말기에서 주공·별동대와 웨이브별 편성을 확인하세요.";
    }

    public record WavePreview(int wave, int maximumWaves, VillageWaveTrait trait,
                              int count, int bossCount, String detail) {
        public String title() { return "웨이브 " + wave + "/" + maximumWaves + " · " + trait.displayName(); }
    }
}
