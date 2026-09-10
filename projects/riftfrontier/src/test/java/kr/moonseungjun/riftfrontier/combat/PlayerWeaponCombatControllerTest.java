package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.ContentRegistry;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerWeaponCombatControllerTest {
    private static final ContentId MOBILE = ContentId.parse("riftfrontier:test_mobile_pressure");
    private static final ContentId REACH = ContentId.parse("riftfrontier:test_reach_commitment");
    private static final ContentId QUICK = ContentId.parse("riftfrontier:test_quick_entry");
    private static final ContentId FINISHER = ContentId.parse("riftfrontier:test_committed_finisher");
    private static final ContentId REACH_MOVE = ContentId.parse("riftfrontier:test_reach_move");
    private static final ContentId PIVOT = ContentId.parse("riftfrontier:test_recovery_pivot");

    @Test
    void publishedFamilyAndModuleAssembleIntoOneAuthoritativeMoveCapability() {
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry());
        PlayerWeaponRuntimeProfile profile = catalog.playerWeaponProfile(MOBILE, Optional.of(PIVOT));

        assertEquals(MOBILE, profile.family().id());
        assertEquals(PIVOT, profile.module().orElseThrow().id());
        assertEquals(Set.of(QUICK, FINISHER), profile.moves().stream().map(CoreDefinition.AttackPattern::id).collect(java.util.stream.Collectors.toSet()));
        assertTrue(profile.hasModuleBehaviour(PlayerWeaponRuntimeProfile.RECOVERY_PIVOT));
        assertThrows(IllegalArgumentException.class, () -> profile.requireMove(REACH_MOVE));
    }

    @Test
    void controllerUsesAttackPatternClockAndOnlyAuthorizesRecoveryPivotDuringRecovery() {
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry());
        PlayerWeaponCombatController controller = catalog.playerWeaponController(MOBILE, Optional.of(PIVOT));

        AttackExecution.Snapshot started = controller.beginMove(QUICK, 100);
        assertEquals(AttackTimeline.Phase.TELEGRAPH, started.presentationPhase());
        assertFalse(controller.recoveryPivotAuthorized(100));
        assertFalse(controller.recoveryPivotAuthorized(103));
        assertEquals(AttackTimeline.Phase.ACTIVE, controller.advance(104).snapshot().orElseThrow().presentationPhase());
        assertFalse(controller.recoveryPivotAuthorized(104));
        assertEquals(AttackTimeline.Phase.RECOVERY, controller.advance(106).snapshot().orElseThrow().presentationPhase());
        assertTrue(controller.recoveryPivotAuthorized(106));
        assertTrue(controller.recoveryPivotAuthorized(108));
        assertTrue(controller.advance(110).finished());
        assertFalse(controller.recoveryPivotAuthorized(110));
    }

    @Test
    void controllerRejectsMovesOutsideFamilyAndCannotOverlapExecutions() {
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry());
        PlayerWeaponCombatController controller = catalog.playerWeaponController(MOBILE, Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> controller.beginMove(REACH_MOVE, 0));
        controller.beginMove(QUICK, 0);
        assertThrows(IllegalStateException.class, () -> controller.beginMove(FINISHER, 1));
        assertFalse(controller.recoveryPivotAuthorized(6));
    }

    @Test
    void runtimeRechecksModuleCompatibilityEvenIfCallerBypassesContentGraphValidation() {
        ContentRegistry registry = registry();
        ContentId incompatible = ContentId.parse("riftfrontier:test_wrong_module");
        registry.register(new CoreDefinition.WeaponModule(incompatible, Set.of(REACH), "technique", Set.of("recovery_pivot")));
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry);

        IllegalStateException failure = assertThrows(
            IllegalStateException.class,
            () -> catalog.playerWeaponProfile(MOBILE, Optional.of(incompatible))
        );
        assertTrue(failure.getMessage().contains("not compatible"));
    }

    @Test
    void runtimeFailsClosedWhenPublishedFamilyMoveCannotResolve() {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.WeaponFamily(MOBILE, Set.of(QUICK), Set.of("mobile_pressure"), Set.of("technique")));
        CombatRuntimeCatalog catalog = new CombatRuntimeCatalog(registry);
        assertThrows(IllegalStateException.class, () -> catalog.playerWeaponProfile(MOBILE, Optional.empty()));
    }

    private static ContentRegistry registry() {
        ContentRegistry registry = new ContentRegistry();
        registry.register(new CoreDefinition.AttackPattern(QUICK, "close_pressure", 4, 2, 4, Set.of("spacing", "sidestep"), "test_quick"));
        registry.register(new CoreDefinition.AttackPattern(FINISHER, "committed_finish", 8, 3, 8, Set.of("backstep", "interrupt_before_active"), "test_finisher"));
        registry.register(new CoreDefinition.AttackPattern(REACH_MOVE, "reach_commitment", 7, 2, 7, Set.of("close_distance", "sidestep"), "test_reach"));
        registry.register(new CoreDefinition.WeaponFamily(MOBILE, Set.of(QUICK, FINISHER), Set.of("mobile_pressure"), Set.of("technique")));
        registry.register(new CoreDefinition.WeaponFamily(REACH, Set.of(REACH_MOVE), Set.of("reach_commitment"), Set.of("technique")));
        registry.register(new CoreDefinition.WeaponModule(PIVOT, Set.of(MOBILE), "technique", Set.of("recovery_pivot")));
        return registry;
    }
}
