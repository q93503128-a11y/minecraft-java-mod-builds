package io.github.q93503128.turnbound.session;
import io.github.q93503128.turnbound.combat.P0Scenario; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class BattleSnapshotCodecTest { @Test void p0HasNineUnitsAndEightPreviewSlots(){var s=P0Scenario.create();assertEquals(9,s.combatants().size());assertEquals(8,s.timelinePreview(8).size());} }
