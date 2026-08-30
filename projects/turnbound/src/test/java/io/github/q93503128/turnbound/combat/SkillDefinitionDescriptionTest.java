package io.github.q93503128.turnbound.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillDefinitionDescriptionTest {
    @Test
    void legacyConstructorStillProducesPlayerFacingDescription() {
        SkillDefinition skill = new SkillDefinition(
                "test", "시험 공격", TargetRule.ENEMY_SINGLE, 2,
                List.of(SkillEffect.damage(1.25)));
        assertFalse(skill.description().isBlank());
        assertTrue(skill.description().contains("125%"));
    }
}
