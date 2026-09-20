package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.BattleEvent;
import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;

/**
 * Minimal contextual teaching for the first real Capital Valley battle.
 *
 * <p>The route design calls for Basic -> Turn Order -> Active/CD learning without turning the field route into
 * a modal tutorial. This helper only derives a short presentation cue from authoritative battle history.</p>
 */
final class BattleContextualGuidance {
    static final String FIRST_COMMON = "CV_FIRST_COMMON";

    record Cue(String text) {
        Cue { text = text == null ? "" : text.trim(); }
        static Cue none() { return new Cue(""); }
        boolean visible() { return !text.isBlank(); }
    }

    private BattleContextualGuidance() {}

    static Cue resolve(String encounterId, BattleState state) {
        if (!FIRST_COMMON.equals(encounterId) || state == null || state.outcome() != BattleOutcome.RUNNING) {
            return Cue.none();
        }

        int allyActions = 0;
        for (BattleEvent event : state.events()) {
            if (!"ACTION".equals(event.type())) continue;
            CombatantState source = state.find(event.sourceId());
            if (source == null || source.side() != CombatantSide.ALLY || source.definition().summon()) continue;
            allyActions++;
        }

        return switch (allyActions) {
            case 0 -> new Cue("적을 선택하고 기본 공격을 사용하세요");
            case 1 -> new Cue("위 초상화 순서가 다음 행동 순서입니다");
            case 2 -> new Cue("액티브 스킬을 사용하세요 · CD는 내 행동마다 감소");
            default -> Cue.none();
        };
    }
}
