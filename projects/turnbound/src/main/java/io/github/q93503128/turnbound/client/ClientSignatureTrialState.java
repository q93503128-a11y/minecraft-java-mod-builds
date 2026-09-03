package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.AwakeningRouteRules;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only client projection of server-authored Signature Trial/Awakening state. */
public final class ClientSignatureTrialState {
    public record TrialRow(
            String characterId,
            String title,
            boolean owned,
            boolean endgameUnlocked,
            int level,
            int currentStar,
            boolean characterQuestComplete,
            boolean firstClearClaimed,
            boolean encounterCanonReady,
            boolean signatureGranted,
            boolean signaturePending,
            long awakeningCore,
            boolean awakened,
            boolean awakeningReady,
            String objective,
            String blockReason
    ) {
        public boolean progressionReady() {
            if (AwakeningRouteRules.canonGap(characterId)) return true;
            return owned && endgameUnlocked && level == 60 && currentStar == 6 && characterQuestComplete && !firstClearClaimed;
        }

        public boolean canEnter() {
            return !AwakeningRouteRules.canonGap(characterId) && progressionReady() && encounterCanonReady;
        }
    }

    private static volatile Map<String, TrialRow> rows = Map.of();
    private static volatile long revision;

    private ClientSignatureTrialState() { }

    public static TrialRow forCharacter(String characterId) {
        TrialRow row = rows.get(characterId);
        if (row != null) return row;
        if (!AwakeningRouteRules.canonGap(characterId)) return null;

        var snapshot = ClientMetaState.snapshot();
        var character = snapshot.characters().stream()
                .filter(value -> value.id().equals(characterId))
                .findFirst().orElse(null);
        if (character == null) return null;

        return new TrialRow(
                characterId,
                "없음 · 소재형 각성 경로 미정",
                character.owned(),
                snapshot.riftUnlocked(),
                character.level(),
                character.star(),
                true,
                false,
                false,
                false,
                false,
                snapshot.core(),
                character.awakened(),
                false,
                "전용 장비/Signature Trial 없음 · 별도 각성 조건 확정 필요",
                AwakeningRouteRules.blockReason(characterId));
    }

    public static Map<String, TrialRow> rows() { return rows; }
    public static long revision() { return revision; }

    public static void update(String raw) {
        Map<String, TrialRow> next = new LinkedHashMap<>();
        if (raw != null && !raw.isBlank()) {
            for (String line : raw.split("\n")) {
                if (!line.startsWith("T|")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 17) continue;
                try {
                    TrialRow row = new TrialRow(
                            p[1], p[2], flag(p[3]), flag(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]),
                            flag(p[7]), flag(p[8]), flag(p[9]), flag(p[10]), flag(p[11]), Long.parseLong(p[12]),
                            flag(p[13]), flag(p[14]), p[15], p[16]);
                    next.put(row.characterId(), row);
                } catch (RuntimeException ignored) { }
            }
        }
        rows = Map.copyOf(next);
        revision++;
    }

    private static boolean flag(String value) { return "1".equals(value); }
}
