package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.SignatureTrialCatalog;

import java.util.List;

/**
 * Pure evaluator for the v0.4 Signature Trial objective layer.
 *
 * <p>This class deliberately separates two questions:</p>
 * <ol>
 *     <li>Did the supplied battle telemetry satisfy the objective written in the character wiki?</li>
 *     <li>Is canon complete enough to settle a first-clear reward?</li>
 * </ol>
 *
 * <p>The current v0.4 documents leave every Trial encounter roster unresolved, and P08 has a
 * prerequisite contradiction. Therefore an objective can be {@link ObjectiveState#MET} while
 * {@link Evaluation#settlementEligible()} remains false. That is intentional: runtime code must not
 * fabricate a Trial roster merely to unlock Signature Equipment.</p>
 */
public final class SignatureTrialEvaluator {
    public enum ObjectiveState { MET, NOT_MET, NOT_EVALUABLE }

    public record Evaluation(
            String characterId,
            ObjectiveState objectiveState,
            boolean canonBlocked,
            String detail
    ) {
        public Evaluation {
            if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("Blank Signature Trial character");
            if (objectiveState == null) throw new IllegalArgumentException("Missing Signature Trial objective state");
            detail = detail == null ? "" : detail;
        }

        public boolean objectiveMet() { return objectiveState == ObjectiveState.MET; }
        public boolean settlementEligible() { return objectiveMet() && !canonBlocked; }
    }

    private SignatureTrialEvaluator() {}

    public static Evaluation evaluate(String characterId, BattleState state) {
        if (state == null) throw new IllegalArgumentException("Missing Signature Trial battle state");
        SignatureTrialCatalog.Spec spec = SignatureTrialCatalog.forCharacter(characterId);

        if (spec.canonState() == SignatureTrialCatalog.CanonState.CANON_CONTRADICTION) {
            return blocked(spec, ObjectiveState.NOT_EVALUABLE, spec.unresolvedReason());
        }
        if (state.outcome() != BattleOutcome.ALLY_VICTORY) {
            return blocked(spec, ObjectiveState.NOT_MET, "Signature Trial requires ally victory");
        }

        return switch (characterId) {
            case "P01" -> blocked(spec, ObjectiveState.NOT_EVALUABLE,
                    "Party-size telemetry is available, but the special Elite canonical identity is unresolved");
            case "P02" -> blocked(spec, ObjectiveState.NOT_EVALUABLE,
                    "SPD/action telemetry is available, but the Trial Boss canonical identity is unresolved");
            case "P03" -> blocked(spec, ObjectiveState.NOT_EVALUABLE,
                    "Enemy-action telemetry is available, but the protected NPC canonical identity is unresolved");
            case "P04" -> evaluateP04(spec, state);
            case "P05" -> evaluateP05(spec, state);
            case "P06" -> evaluateP06(spec, state);
            case "P07" -> evaluateP07(spec, state);
            case "P08" -> blocked(spec, ObjectiveState.NOT_EVALUABLE, spec.unresolvedReason());
            default -> throw new IllegalArgumentException("No Signature Trial evaluator for " + characterId);
        };
    }

    private static Evaluation evaluateP04(SignatureTrialCatalog.Spec spec, BattleState state) {
        List<CombatantState> party = regularAllies(state);
        boolean allyDownRecorded = state.events().stream()
                .filter(event -> "DOWN".equals(event.type()))
                .anyMatch(event -> party.stream().anyMatch(member -> member.instanceId().equals(event.targetId())));
        boolean allAlive = !party.isEmpty() && party.stream().noneMatch(CombatantState::downed);
        ObjectiveState result = allyDownRecorded && allAlive ? ObjectiveState.MET : ObjectiveState.NOT_MET;
        return blocked(spec, result,
                "allyDown=" + allyDownRecorded + ", finalPartyAlive=" + allAlive);
    }

    private static Evaluation evaluateP05(SignatureTrialCatalog.Spec spec, BattleState state) {
        CombatantState lynette = hero(state, "P05");
        if (lynette == null) return blocked(spec, ObjectiveState.NOT_MET, "P05 is not present in the Trial party");
        long followUps = state.events().stream().filter(event ->
                "REACTION_DAMAGE".equals(event.type())
                        && lynette.instanceId().equals(event.sourceId())
                        && "P05_FOLLOW_UP".equals(event.detail())).count();
        long actions = state.events().stream().filter(event -> "ACTION".equals(event.type())).count();
        ObjectiveState result = followUps >= 10 && actions <= 25 ? ObjectiveState.MET : ObjectiveState.NOT_MET;
        return blocked(spec, result, "followUps=" + followUps + ", actions=" + actions);
    }

    private static Evaluation evaluateP06(SignatureTrialCatalog.Spec spec, BattleState state) {
        CombatantState morwen = hero(state, "P06");
        if (morwen == null) return blocked(spec, ObjectiveState.NOT_MET, "P06 is not present in the Trial party");
        boolean selfRevived = state.events().stream().anyMatch(event ->
                "SELF_REVIVE".equals(event.type())
                        && morwen.instanceId().equals(event.sourceId())
                        && morwen.instanceId().equals(event.targetId()));
        int memory = morwen.counter("memory");
        ObjectiveState result = selfRevived && memory >= 5 ? ObjectiveState.MET : ObjectiveState.NOT_MET;
        return blocked(spec, result, "selfRevived=" + selfRevived + ", memory=" + memory);
    }

    private static Evaluation evaluateP07(SignatureTrialCatalog.Spec spec, BattleState state) {
        CombatantState marion = hero(state, "P07");
        if (marion == null) return blocked(spec, ObjectiveState.NOT_MET, "P07 is not present in the Trial party");

        int summonDownIndex = -1;
        List<BattleEvent> events = state.events();
        for (int i = 0; i < events.size(); i++) {
            BattleEvent event = events.get(i);
            if ("SUMMON_DOWN".equals(event.type()) && marion.instanceId().equals(event.targetId())) {
                summonDownIndex = i;
                break;
            }
        }
        boolean resummonedAfterDeath = false;
        if (summonDownIndex >= 0) {
            for (int i = summonDownIndex + 1; i < events.size(); i++) {
                BattleEvent event = events.get(i);
                if ("ACTION".equals(event.type())
                        && marion.instanceId().equals(event.sourceId())
                        && "p07_summon_toto".equals(event.detail())) {
                    resummonedAfterDeath = true;
                    break;
                }
            }
        }
        boolean marionAlive = !marion.downed();
        ObjectiveState result = summonDownIndex >= 0 && resummonedAfterDeath && marionAlive
                ? ObjectiveState.MET : ObjectiveState.NOT_MET;
        return blocked(spec, result,
                "summonDied=" + (summonDownIndex >= 0)
                        + ", resummoned=" + resummonedAfterDeath
                        + ", marionAlive=" + marionAlive);
    }

    private static Evaluation blocked(SignatureTrialCatalog.Spec spec, ObjectiveState state, String detail) {
        // Current v0.4 has no fully authored Signature Trial encounter. RULES_READY_ROSTER_GAP and
        // CANON_CONTRADICTION are both hard settlement blocks until the canon itself is revised.
        boolean canonBlocked = switch (spec.canonState()) {
            case RULES_READY_ROSTER_GAP, CANON_CONTRADICTION -> true;
        };
        String reason = detail == null || detail.isBlank() ? spec.unresolvedReason() : detail;
        return new Evaluation(spec.characterId(), state, canonBlocked, reason);
    }

    private static List<CombatantState> regularAllies(BattleState state) {
        return state.combatants().stream()
                .filter(unit -> unit.side() == CombatantSide.ALLY && !unit.definition().summon())
                .toList();
    }

    private static CombatantState hero(BattleState state, String definitionId) {
        return state.combatants().stream()
                .filter(unit -> unit.side() == CombatantSide.ALLY && definitionId.equals(unit.definition().id()))
                .findFirst().orElse(null);
    }
}
