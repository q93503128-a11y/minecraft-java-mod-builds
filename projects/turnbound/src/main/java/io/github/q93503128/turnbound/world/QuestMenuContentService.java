package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.QuestCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Adds canonical current-main and Character Quest summaries plus authored investigation routes to the quest menu. */
public final class QuestMenuContentService {
    private record CharacterQuestText(String owner, String story, String trial) {}

    private static final Map<String, CharacterQuestText> CHARACTER = characterRows();

    private QuestMenuContentService() {}

    /**
     * Q rows deliberately reuse the existing RegionQuestRow wire shape so older UI code can render them.
     * Character quest detailed combat objectives remain unspecified; the physical investigation site is shown separately.
     */
    public static String encode(UUID playerId) {
        var snapshot = CampaignProgressStore.snapshot(playerId);
        StringBuilder out = new StringBuilder();
        QuestCatalog.Quest current = currentMain(snapshot.quests().completed());
        if (current != null) {
            out.append("Q|").append(current.id()).append('|')
                    .append(safe("MAIN · Ch" + current.chapter() + " · " + current.name())).append("|1|")
                    .append(snapshot.quests().completed().contains(current.id()) ? 1 : 0).append('|')
                    .append(safe(mainObjective(current))).append('\n');
        }

        for (QuestCatalog.Quest quest : QuestCatalog.kind(QuestCatalog.Kind.CHARACTER)) {
            CharacterQuestText text = CHARACTER.get(quest.id());
            if (text == null) continue;
            boolean completed = snapshot.quests().completed().contains(quest.id());
            boolean available = available(playerId, quest.owner());
            String status = completed ? "완료" : available ? "해금" : "잠금";
            out.append("Q|").append(quest.id()).append('|')
                    .append(safe("CHARACTER · " + quest.owner() + " · " + quest.name() + " · " + status)).append("|0|")
                    .append(completed ? 1 : 0).append('|')
                    .append(safe(text.story()
                            + " / 조사 위치: " + CharacterQuestRouteCatalog.route(quest.owner())
                            + " / Signature Trial: " + text.trial())).append('\n');
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
        String targets = String.join(", ", quest.targetIds());
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
            default -> quest.objectiveType();
        };
    }

    private static Map<String, CharacterQuestText> characterRows() {
        Map<String, CharacterQuestText> out = new LinkedHashMap<>();
        out.put("CQ_P01", new CharacterQuestText("P01", "과거 카이렌이 지키지 못한 북문 초소의 기록을 찾는다.",
                "B05 / P01 ★6 Lv60 / P01 포함 2인 이하 / 특수 엘리트 격파"));
        out.put("CQ_P02", new CharacterQuestText("P02", "시계탑이 20년 전 대단절 순간의 시간을 반복하는 문제를 해결한다.",
                "P02 ★6 Lv60 / SPD 80 이하 아군 2명 이상 / 아군 행동 22 이하 / Trial Boss"));
        out.put("CQ_P03", new CharacterQuestText("P03", "남문 대피 기록을 조사한다.",
                "P03 ★6 Lv60 / 지정 NPC를 적 행동 10회 동안 생존"));
        out.put("CQ_P04", new CharacterQuestText("P04", "대단절 생존자 명단을 복원한다.",
                "P04 ★6 Lv60 / 아군 사망 1회 이상 / 승리 시 전원 생존"));
        out.put("CQ_P05", new CharacterQuestText("P05", "잘못된 사냥 정보로 민간인이 위험해졌던 과거 사건을 조사한다.",
                "P05 ★6 Lv60 / Follow-up 10회 이상 / 아군 행동 25 이하 / 승리"));
        out.put("CQ_P06", new CharacterQuestText("P06", "대단절 당시 이름 없이 사망 처리된 사람의 신원을 확인한다.",
                "P06 ★6 Lv60 / 자가부활 1회 / 기억 5 이상 / 승리"));
        out.put("CQ_P07", new CharacterQuestText("P07", "계약수 토토가 단순 도구가 아니라 자기 의지를 가진 존재인지 확인한다.",
                "P07 ★6 Lv60 / Toto 사망 후 재소환 / Marion 생존 / 승리"));
        out.put("CQ_P08", new CharacterQuestText("P08", "채석장 사고 당시 사람들을 두고 도망쳤다는 라제의 소문을 조사한다.",
                "P08 ★6 Lv60 / 종료 HP 30% 이하 / HP 1 생존 패시브 발동 / 승리"));
        return Map.copyOf(out);
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\n', ' ');
    }
}
