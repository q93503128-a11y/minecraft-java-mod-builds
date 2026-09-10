package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentPackLoader;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductionPlayerWeaponContentTest {
    private static final String RESOURCE = "/data/riftfrontier/riftfrontier/content/player_combat_01.json";

    @Test
    void productionPackLocksExactlyTwoContrastingFamiliesAndRecoveryPivot() throws Exception {
        var stream = ProductionPlayerWeaponContentTest.class.getResourceAsStream(RESOURCE);
        assertNotNull(stream, "production player combat pack must be packaged");

        ContentPackLoader.LoadedPack pack;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            pack = new ContentPackLoader().load(reader, RESOURCE);
        }
        pack.requireValid();
        assertEquals("riftfrontier:production/player_combat_01", pack.packId());
        assertEquals(6, pack.registry().size(), "first slice must stay at three moves, two families and one module");

        ContentId mobileFamilyId = ContentId.parse("riftfrontier:weapon_family/mobile_pressure");
        ContentId reachFamilyId = ContentId.parse("riftfrontier:weapon_family/reach_commitment");
        ContentId moduleId = ContentId.parse("riftfrontier:weapon_module/recovery_pivot");
        ContentId mobileEntryId = ContentId.parse("riftfrontier:attack/player/mobile_pressure_entry");
        ContentId mobileFinisherId = ContentId.parse("riftfrontier:attack/player/mobile_pressure_finisher");
        ContentId reachStrikeId = ContentId.parse("riftfrontier:attack/player/reach_commitment_strike");

        var mobile = (CoreDefinition.WeaponFamily) pack.registry()
            .find(CoreDefinition.Kind.WEAPON_FAMILY, mobileFamilyId).orElseThrow();
        var reach = (CoreDefinition.WeaponFamily) pack.registry()
            .find(CoreDefinition.Kind.WEAPON_FAMILY, reachFamilyId).orElseThrow();
        var module = (CoreDefinition.WeaponModule) pack.registry()
            .find(CoreDefinition.Kind.WEAPON_MODULE, moduleId).orElseThrow();
        var mobileEntry = (CoreDefinition.AttackPattern) pack.registry()
            .find(CoreDefinition.Kind.ATTACK_PATTERN, mobileEntryId).orElseThrow();
        var mobileFinisher = (CoreDefinition.AttackPattern) pack.registry()
            .find(CoreDefinition.Kind.ATTACK_PATTERN, mobileFinisherId).orElseThrow();
        var reachStrike = (CoreDefinition.AttackPattern) pack.registry()
            .find(CoreDefinition.Kind.ATTACK_PATTERN, reachStrikeId).orElseThrow();

        assertEquals(Set.of(mobileEntryId, mobileFinisherId), mobile.moves());
        assertEquals(Set.of(reachStrikeId), reach.moves());
        assertTrue(mobile.combatRoles().contains("mobile_pressure"));
        assertTrue(reach.combatRoles().contains("reach_commitment"));
        assertEquals(Set.of("technique"), mobile.moduleSockets());
        assertEquals(Set.of("technique"), reach.moduleSockets());
        assertEquals(Set.of(mobileFamilyId, reachFamilyId), module.compatibleFamilies());
        assertEquals("technique", module.socket());
        assertEquals(Set.of("recovery_pivot"), module.behaviourChanges());

        assertTrue(mobileEntry.telegraphTicks() < reachStrike.telegraphTicks(),
            "ordinary mobile entry must remain lower-commitment than the reach payoff");
        assertTrue(mobileEntry.recoveryTicks() < mobileFinisher.recoveryTicks(),
            "mobile family must contain a clearly more punishable committed finisher");
        assertTrue(mobileFinisher.recoveryTicks() > mobileEntry.recoveryTicks());
        assertTrue(reachStrike.recoveryTicks() > mobileEntry.recoveryTicks(),
            "reach commitment must expose a real post-commitment punish window");

        pack.registry().all().forEach(definition ->
            assertFalse(definition.id().path().contains("fixture"), "production pack must not promote fixture IDs"));
    }
}
