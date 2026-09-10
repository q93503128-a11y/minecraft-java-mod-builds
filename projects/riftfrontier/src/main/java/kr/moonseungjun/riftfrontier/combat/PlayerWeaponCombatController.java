package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-thread-owned player weapon execution boundary.
 *
 * <p>Only moves authored by the assembled family can start. Module behaviour authorization is sampled
 * from the same immutable {@link AttackExecution} clock, so recovery-only techniques cannot open during
 * TELEGRAPH/ACTIVE or after COMPLETE.</p>
 */
public final class PlayerWeaponCombatController {
    private final PlayerWeaponRuntimeProfile profile;
    private final AttackStateMachine attacks = new AttackStateMachine();

    public PlayerWeaponCombatController(PlayerWeaponRuntimeProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public PlayerWeaponRuntimeProfile profile() {
        return profile;
    }

    public AttackExecution.Snapshot beginMove(ContentId moveId, long gameTick) {
        return attacks.begin(profile.requireMove(moveId), gameTick);
    }

    public AttackStateMachine.Step advance(long gameTick) {
        return attacks.advance(gameTick);
    }

    public boolean isExecuting() {
        return attacks.isExecuting();
    }

    public Optional<AttackExecution> cancel() {
        return attacks.cancel();
    }

    /**
     * Authorizes the first-slice technique semantic without changing attack timing itself.
     * The caller may perform its movement transition only while this returns true.
     */
    public boolean recoveryPivotAuthorized(long gameTick) {
        if (!profile.hasModuleBehaviour(PlayerWeaponRuntimeProfile.RECOVERY_PIVOT)) return false;
        return attacks.currentExecution()
            .map(execution -> execution.sample(gameTick).presentationPhase() == AttackTimeline.Phase.RECOVERY)
            .orElse(false);
    }
}
