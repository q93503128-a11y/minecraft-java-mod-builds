package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.QuestCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Adds player-facing main and character quest summaries plus authored investigation routes to the quest menu. */
public final class QuestMenuContentService {
    private record CharacterQuestText(String owner, String story, String trial) {}

    private static final Map<String, CharacterQuestText> CHARACTER = characterRows();

    private QuestMenuContentService() {}

    /** Q rows use display titles on the wire. Canonical quest IDs remain server-side progression keys. */
    public static String encode(UUID playerId) {
        var snapshot = CampaignProgressStore.snapshot(playerId);
        StringBuilder out = new StringBuilder();
        QuestCatalog.Quest current = currentMain(snapshot.quests().completed());
        if (current != null) {
            out.append("Q|").append(safe(current.name())).append('|')
                    .append(safe("메인 · 제" + current.chapter() + "장")).append("|1|")
                    .append(snapshot.quests().completed().contains(current.id()) ? 1 : 0).append('|')
                    .append(safe(mainObjective(current))).append('\n');
        }

        for (QuestCatalog.Quest quest : QuestCatalog.kind(QuestCatalog.Kind.CHARACTER)) {
            CharacterQuestText text = CHARACTER.get(quest.id());
            if (text == null) continue;
            boolean completed = snapshot.quests().completed().contains(quest.id());
            boolean available = available(playerId, quest.owner());
            String status = completed ? "완료" : available ? "해금" : "잠금";
            String ownerName = characterName(quest.owner());
            out.append("Q|").append(safe(quest.name())).append('|')
                    .append(safe("인연 · " + ownerName + " · " + status)).append("|0|")
                    .append(completed ? 1 : 0).append('|')
                    .append(safe(text.story()
                            + " / 조사 위치: " + CharacterQuestRouteCatalog.route(quest.owner())
                            + " / 전용 장비 시험: " + text.trial())).append('\n');
        }
        return out.toString();
    }

    static boolean available(UUID playerId, String owner) {
        var snapshot = CampaignProgressStore.snapshot(playerId);
        if (!snapshot.profile().ownedCharacters().contains(owner)) return false;
        int level = CampaignProgressStore.character(playerId, owner).level();
        return switch (owner) {
            case "P01" -> CampaignContentUnlocks.chapter2Complete(playerId) && level >= 20;
            case "P02" -> CampaignContentUnlocks.chapter3Complete(playerId) && level >= 20;
            case "P08" -> CampaignContentUnlocks.chapter4Complete(playerId);
            case "P03", "P04", "P05", "P06", "P07" -> CampaignContentUnlocks.characterQuestStageOne(playerId);
            default -> false;
        };
    }

    private static QuestCatalog.Quest currentMain(Set<String> completed) {
        List<QuestCatalog.Quest> main = QuestCatalog.kind(QuestCatalog.Kind.MAIN);
        for (QuestCatalog.Quest quest : main) {
            if (completed.contains(quest.id())) continue;
            if (quest.prerequisites().stream().allMatch(completed::contains)) return quest;
        }
        return main.isEmpty() ? null : main.getLast();
    }

    private static String mainObjective(QuestCatalog.Quest quest) {
        String targets = quest.targetIds().stream().map(QuestMenuContentService::targetLabel)
                .reduce((left, right) -> left + ", " + right).orElse("-");
        return switch (quest.objectiveType()) {
            case "INTERACT" -> "상호작용 · " + targets;
            case "PARTY_CONFIRM" -> "파티 확인 · " + targets;
            case "BATTLE_WINS" -> "전투 승리 · " + targets;
            case "BATTLE_WIN" -> "대상 포함 전투 승리 · " + targets;
            case "BOSS_WIN" -> "보스 승리 · " + targets;
            case "INTERACT_COUNT" -> "상호작용 " + quest.requiredCount() + "회 · " + targets;
            case "BATTLE_WINS_WITH" -> "대상 포함 전투 " + quest.requiredCount() + "승 · " + targets;
            case "KILL_AND_LOOT" -> "처치/회수 각 " + quest.requiredCount() + "회 · " + targets;
            case "INVENTORY_FLAGS" -> "Relay 기록 제출 · " + targets;
            case "BOSS_AND_INTERACT" -> "보스 격파 후 Relay 재연결 · " + targets;
            default -> "임무 목표 확인";
        };
    }

    private static String targetLabel(String id) {
        if (id == null || id.isBlank()) return "지정 목표";
        String authored = switch (id) {
            case "Director Iven" -> "총괄관 아이븐";
            case "SOUTH_GATE" -> "남문";
            case "TUTORIAL_1" -> "전투 훈련 1";
            case "TUTORIAL_2" -> "전투 훈련 2";
            case "TUTORIAL_3" -> "전투 훈련 3";
            case "SPORE_LANTERN" -> "포자등불";
            case "AQUEDUCT_VALVE" -> "수로 압력 밸브";
            case "CORE_FRAGMENT" -> "Relay 핵 파편";
            case "RELAY_FRAGMENT_MEADOW" -> "남문 초원 Relay 조각";
            case "RELAY_FRAGMENT_AQUEDUCT" -> "붕괴 수로 Relay 조각";
            case "RELAY_FRAGMENT_QUARRY" -> "잿불 채석장 Relay 조각";
            case "SERAK_RECORD" -> "세라크 기록";
            case "RELAY_CONSOLE" -> "Relay 제어 콘솔";
            default -> null;
        };
        if (authored != null) return authored;
        try {
            return CanonicalData.definition(id).name();
        } catch (RuntimeException ignored) {
            return "지정 목표";
        }
    }

    private static String characterName(String id) {
        try {
            return CanonicalData.definition(id).name();
        } catch (RuntimeException ignored) {
            return "동료";
        }
    }

    private static Map<String, CharacterQuestText> characterRows() {
        Map<String, CharacterQuestText> out = new LinkedHashMap<>();
        out.put("CQ_P01", new CharacterQuestText("P01", "과거 카이렌이 지키지 못한 북문 초소의 기록을 찾는다.",
                "메인 스토리 완료 / 카이렌 ★6 Lv.60 / 카이렌 포함 2인 이하 / 특수 정예 격파"));
        out.put("CQ_P02", new CharacterQuestText("P02", "시계탑이 20년 전 대단절 순간의 시간을 반복하는 문제를 해결한다.",
                "루메아 ★6 Lv.60 / SPD 80 이하 아군 2명 이상 / 아군 행동 22 이하 / 시험 보스 격파"));
        out.put("CQ_P03", new CharacterQuestText("P03", "남문 대피 기록을 조사한다.",
                "브람 ★6 Lv.60 / 지정 인물을 적 행동 10회 동안 생존"));
        out.put("CQ_P04", new CharacterQuestText("P04", "대단절 생존자 명단을 복원한다.",
                "엘리시아 ★6 Lv.60 / 아군 사망 1회 이상 / 승리 시 전원 생존"));
        out.put("CQ_P05", new CharacterQuestText("P05", "잘못된 사냥 정보로 민간인이 위험해졌던 과거 사건을 조사한다.",
                "리네트 ★6 Lv.60 / 추가타 10회 이상 / 아군 행동 25 이하 / 승리"));
        out.put("CQ_P06", new CharacterQuestText("P06", "대단절 당시 이름 없이 사망 처리된 사람의 신원을 확인한다.",
                "모르웬 ★6 Lv.60 / 자가부활 1회 / 기억 5 이상 / 승리"));
        out.put("CQ_P07", new CharacterQuestText("P07", "계약수 토토가 단순 도구가 아니라 자기 의지를 가진 존재인지 확인한다.",
                "마리온 ★6 Lv.60 / 토토 사망 후 재소환 / 마리온 생존 / 승리"));
        out.put("CQ_P08", new CharacterQuestText("P08", "채석장 사고 당시 사람들을 두고 도망쳤다는 라제의 소문을 조사한다.",
                "라제 ★6 Lv.60 / 종료 HP 30% 이하 / HP 1 생존 능력 발동 / 승리"));
        return Map.copyOf(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ');
    }
}
