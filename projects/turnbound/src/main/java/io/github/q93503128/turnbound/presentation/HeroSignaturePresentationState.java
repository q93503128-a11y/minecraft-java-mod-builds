package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantState;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure projection of the v1 core-hero signature mechanics into presentation tokens.
 * Combat state stays authoritative; this class never mutates gameplay.
 */
public final class HeroSignaturePresentationState {
    public record Resource(String id, int value, int max) {
        public Resource {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Blank resource id");
            value = Math.max(0, value);
            max = Math.max(1, max);
        }

        public String token() { return "@r:" + id + ":" + value + ":" + max; }
    }

    private HeroSignaturePresentationState() {}

    public static Resource resource(CombatantState combatant) {
        if (combatant == null) return null;
        return switch (combatant.definition().id()) {
            case "P01" -> new Resource("focus", combatant.counter("focus"),
                    combatant.definition().intParam("focusMax", 3));
            case "P03" -> new Resource("guard", combatant.counter("guard"),
                    combatant.definition().intParam("guardMax", 100));
            case "P05" -> new Resource("shot", combatant.counter("shot"),
                    combatant.definition().intParam("shotMax", 2));
            case "P06" -> new Resource("records", combatant.counter("records"),
                    combatant.definition().intParam("recordMax", 5));
            case "P07" -> new Resource("bond", combatant.counter("bond"),
                    combatant.definition().intParam("bondMax", 100));
            case "P08" -> new Resource("fury", combatant.counter("fury"),
                    combatant.definition().intParam("furyMax", 100));
            default -> null;
        };
    }

    public static List<String> tokens(BattleState state, CombatantState combatant) {
        if (combatant == null) return List.of();
        List<String> out = new ArrayList<>();
        Resource resource = resource(combatant);
        if (resource != null) out.add(resource.token());

        if (state != null) {
            String targetId = combatant.instanceId();
            boolean duelTarget = state.combatants().stream().anyMatch(owner ->
                    !owner.downed() && "P01".equals(owner.definition().id()) && targetId.equals(owner.ref("focusTarget")));
            boolean sightlineTarget = state.combatants().stream().anyMatch(owner ->
                    !owner.downed() && "P05".equals(owner.definition().id()) && targetId.equals(owner.ref("sightline")));
            if (duelTarget) out.add("@m:duel");
            if (sightlineTarget) out.add("@m:sightline");
        }
        return List.copyOf(out);
    }
}
