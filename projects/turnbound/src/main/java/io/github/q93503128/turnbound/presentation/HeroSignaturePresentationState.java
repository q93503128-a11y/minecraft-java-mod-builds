package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.combat.BattleState;
import io.github.q93503128.turnbound.combat.CombatantState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Small authored body-language state used by the live battle model.
     * Stage 0 is neutral; stages 1~3 increase signature readiness. Overheat is P08-only.
     */
    public record VisualState(int stage, boolean overheat) {
        public VisualState {
            stage = Math.max(0, Math.min(3, stage));
        }

        public String animationKey() { return overheat ? "overheat" : "state_" + stage; }
    }

    public enum RelationKind { DUEL, SIGHTLINE, SANCTUARY, PARTNER_GUARD }

    public record Relation(RelationKind kind, String sourceId, String targetId) {
        public Relation {
            if (kind == null) throw new IllegalArgumentException("Missing relation kind");
            if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("Missing relation source");
            if (targetId == null || targetId.isBlank()) throw new IllegalArgumentException("Missing relation target");
        }

        public String key() { return kind.name() + "|" + targetId; }
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

    public static VisualState visualState(CombatantState combatant) {
        Resource resource = resource(combatant);
        if (resource == null) return null;
        if (combatant.downed()) return new VisualState(0, false);

        int value = Math.min(resource.value(), resource.max());
        return switch (combatant.definition().id()) {
            case "P01" -> new VisualState(Math.min(3, value), false);
            case "P03", "P07" -> new VisualState(
                    value <= 0 ? 0 : value < 50 ? 1 : value < resource.max() ? 2 : 3, false);
            case "P05" -> new VisualState(Math.min(2, value), false);
            case "P06" -> new VisualState(
                    value <= 0 ? 0 : value <= 2 ? 1 : value < resource.max() ? 2 : 3, false);
            case "P08" -> new VisualState(
                    value <= 0 ? 0 : value < 60 ? 1 : value < 80 ? 2 : 3, isRazeOverheated(combatant));
            default -> null;
        };
    }

    private static boolean isRazeOverheated(CombatantState combatant) {
        String self = combatant.instanceId();
        var attack = combatant.status("attack_multiplier", self);
        var speed = combatant.status("speed_multiplier", self);
        var defense = combatant.status("defense_multiplier", self);
        return attack != null && attack.magnitude() > 0
                && speed != null && speed.magnitude() > 0
                && defense != null && defense.magnitude() < 0;
    }

    public static List<Relation> relations(BattleState state) {
        if (state == null) return List.of();
        Map<String, Relation> out = new LinkedHashMap<>();

        for (CombatantState source : state.combatants()) {
            if (source.downed()) continue;
            RelationKind kind = switch (source.definition().id()) {
                case "P01" -> RelationKind.DUEL;
                case "P05" -> RelationKind.SIGHTLINE;
                default -> null;
            };
            if (kind == null) continue;
            String targetId = "P01".equals(source.definition().id()) ? source.ref("focusTarget") : source.ref("sightline");
            CombatantState target = state.find(targetId);
            if (target == null || target.downed()) continue;
            Relation relation = new Relation(kind, source.instanceId(), target.instanceId());
            out.put(relation.key(), relation);
        }

        for (CombatantState target : state.combatants()) {
            if (target.downed()) continue;
            for (var status : target.statusesView().values()) {
                RelationKind kind = switch (status.id()) {
                    case "sanctuary" -> RelationKind.SANCTUARY;
                    case "partner_guard" -> RelationKind.PARTNER_GUARD;
                    default -> null;
                };
                if (kind == null) continue;
                CombatantState source = state.find(status.sourceId());
                String requiredSource = kind == RelationKind.SANCTUARY ? "P04" : "P07";
                if (source == null || source.downed() || !requiredSource.equals(source.definition().id())) continue;
                Relation relation = new Relation(kind, source.instanceId(), target.instanceId());
                out.putIfAbsent(relation.key(), relation);
            }
        }
        return List.copyOf(out.values());
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
