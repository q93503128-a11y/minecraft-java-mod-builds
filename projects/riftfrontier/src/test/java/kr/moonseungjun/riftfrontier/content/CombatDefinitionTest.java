package kr.moonseungjun.riftfrontier.content;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

final class CombatDefinitionTest {
    private final ContentDocumentCodec codec = new ContentDocumentCodec();

    @Test
    void decodesAttackPatternCadenceAndCounterplay() {
        CoreDefinition.AttackPattern pattern = (CoreDefinition.AttackPattern) codec.decode("""
            {"kind":"attack_pattern","id":"riftfrontier:boss_sweep","delivery":"arc_melee","telegraph_ticks":18,"active_ticks":6,"recovery_ticks":14,"counterplay":["backstep","guard"],"presentation_cue":"full_body_windup"}
            """);
        assertEquals(38, pattern.totalTicks());
        assertEquals(Set.of("backstep", "guard"), pattern.counterplay());
        assertEquals(CoreDefinition.Kind.ATTACK_PATTERN, pattern.kind());
    }

    @Test
    void bossProfileRequiresResolvableAttackPatterns() {
        ContentRegistry registry = new ContentRegistry();
        ContentId bossId = ContentId.parse("riftfrontier:region_01_apex");
        ContentId patternId = ContentId.parse("riftfrontier:boss_sweep");
        registry.register(new CoreDefinition.BossProfile(bossId, 2, Set.of(patternId), "keep_center_lane_open"));
        ContentValidator.Report report = new ContentValidator().validate(registry);
        assertTrue(report.hasErrors());
        assertEquals(1, report.byCode(ContentValidator.Code.MISSING_REFERENCE).size());

        registry.register(new CoreDefinition.AttackPattern(patternId, "arc_melee", 18, 6, 14, Set.of("backstep"), "full_body_windup"));
        assertFalse(new ContentValidator().validate(registry).hasErrors());
    }

    @Test
    void attackPatternRejectsMissingCounterplayAndInvalidTiming() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode("""
            {"kind":"attack_pattern","id":"riftfrontier:bad","delivery":"slam","telegraph_ticks":0,"active_ticks":4,"recovery_ticks":8,"counterplay":["dodge"],"presentation_cue":"raise_arm"}
            """));

        ContentRegistry registry = new ContentRegistry();
        ContentId id = ContentId.parse("riftfrontier:no_counterplay");
        registry.register(new CoreDefinition.AttackPattern(id, "slam", 10, 4, 8, Set.of(), "raise_arm"));
        ContentValidator.Report report = new ContentValidator().validate(registry);
        assertEquals(1, report.byCode(ContentValidator.Code.EMPTY_ATTACK_COUNTERPLAY).size());
    }

    @Test
    void weaponFamilyAndModuleDecodeAsComposableCombatSemantics() {
        CoreDefinition.WeaponFamily family = (CoreDefinition.WeaponFamily) codec.decode("""
            {"kind":"weapon_family","id":"riftfrontier:fixture/weapon_family/mobile_blade","moves":["riftfrontier:fixture/move/commit_slash"],"combat_roles":["mobile_commitment"],"module_sockets":["technique"]}
            """);
        CoreDefinition.WeaponModule module = (CoreDefinition.WeaponModule) codec.decode("""
            {"kind":"weapon_module","id":"riftfrontier:fixture/module/tempo_shift","compatible_families":["riftfrontier:fixture/weapon_family/mobile_blade"],"socket":"technique","behaviour_changes":["reposition_after_commitment"]}
            """);

        assertEquals(CoreDefinition.Kind.WEAPON_FAMILY, family.kind());
        assertEquals(Set.of(ContentId.parse("riftfrontier:fixture/move/commit_slash")), family.moves());
        assertEquals(Set.of("mobile_commitment"), family.combatRoles());
        assertEquals(CoreDefinition.Kind.WEAPON_MODULE, module.kind());
        assertEquals("technique", module.socket());
        assertEquals(Set.of("reposition_after_commitment"), module.behaviourChanges());
    }

    @Test
    void weaponCompositionRequiresResolvedMovesFamiliesAndDeclaredSockets() {
        ContentId moveId = ContentId.parse("riftfrontier:fixture/move/commit_slash");
        ContentId familyId = ContentId.parse("riftfrontier:fixture/weapon_family/mobile_blade");
        ContentId moduleId = ContentId.parse("riftfrontier:fixture/module/tempo_shift");
        ContentRegistry registry = new ContentRegistry();

        registry.register(new CoreDefinition.WeaponFamily(
            familyId,
            Set.of(moveId),
            Set.of("mobile_commitment"),
            Set.of("technique")
        ));
        registry.register(new CoreDefinition.WeaponModule(
            moduleId,
            Set.of(familyId),
            "technique",
            Set.of("reposition_after_commitment")
        ));

        ContentValidator.Report unresolved = new ContentValidator().validate(registry);
        assertEquals(1, unresolved.byCode(ContentValidator.Code.MISSING_REFERENCE).size(), "weapon move must resolve to an attack pattern");

        registry.register(new CoreDefinition.AttackPattern(
            moveId, "arc_melee", 4, 2, 3, Set.of("space_out"), "fixture_windup"
        ));
        assertFalse(new ContentValidator().validate(registry).hasErrors());

        ContentRegistry badSocketRegistry = new ContentRegistry();
        badSocketRegistry.register(new CoreDefinition.AttackPattern(
            moveId, "arc_melee", 4, 2, 3, Set.of("space_out"), "fixture_windup"
        ));
        badSocketRegistry.register(new CoreDefinition.WeaponFamily(
            familyId,
            Set.of(moveId),
            Set.of("mobile_commitment"),
            Set.of("technique")
        ));
        badSocketRegistry.register(new CoreDefinition.WeaponModule(
            moduleId,
            Set.of(familyId),
            "utility",
            Set.of("reposition_after_commitment")
        ));
        ContentValidator.Report badSocket = new ContentValidator().validate(badSocketRegistry);
        assertEquals(1, badSocket.byCode(ContentValidator.Code.INCOMPATIBLE_WEAPON_MODULE_SOCKET).size());
    }

    @Test
    void weaponCompositionRejectsEmptySemanticComponents() {
        ContentRegistry registry = new ContentRegistry();
        ContentId familyId = ContentId.parse("riftfrontier:fixture/weapon_family/empty");
        ContentId moduleId = ContentId.parse("riftfrontier:fixture/module/empty");
        registry.register(new CoreDefinition.WeaponFamily(familyId, Set.of(), Set.of(), Set.of()));
        registry.register(new CoreDefinition.WeaponModule(moduleId, Set.of(), "", Set.of()));

        ContentValidator.Report report = new ContentValidator().validate(registry);
        assertEquals(1, report.byCode(ContentValidator.Code.NO_WEAPON_MOVES).size());
        assertEquals(1, report.byCode(ContentValidator.Code.NO_WEAPON_ROLES).size());
        assertEquals(1, report.byCode(ContentValidator.Code.NO_WEAPON_MODULE_SOCKETS).size());
        assertEquals(1, report.byCode(ContentValidator.Code.NO_WEAPON_MODULE_FAMILIES).size());
        assertEquals(1, report.byCode(ContentValidator.Code.INVALID_WEAPON_MODULE_SOCKET).size());
        assertEquals(1, report.byCode(ContentValidator.Code.EMPTY_WEAPON_MODULE_CHANGES).size());
    }
}
