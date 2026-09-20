package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KyrenFocusPresentationEventTest {
    @Test
    void focusMutationPublishesAResourceEventWithoutChangingCombatAuthority() {
        CombatantState kyren = new CombatantState("kyren", CanonicalData.definition("P01"), CombatantSide.ALLY, 0);
        CombatantState enemy = new CombatantState("enemy", CanonicalData.definition("E001"), CombatantSide.ENEMY, 0);
        BattleState state = new BattleState(List.of(kyren, enemy));
        BattleEngine engine = new BattleEngine(state);

        kyren.setGauge(1000);
        engine.nextReady();
        engine.useSkill(kyren.instanceId(), "p01_chase_slash", enemy.instanceId());

        assertTrue(state.events().stream().anyMatch(event ->
                "RESOURCE".equals(event.type())
                        && kyren.instanceId().equals(event.sourceId())
                        && "P01_FOCUS".equals(event.detail())
                        && event.value() == 1));
    }
}
