package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import kr.moonseungjun.riftfrontier.content.CoreDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Immutable server-side capability assembled from one validated weapon family and an optional module.
 *
 * <p>The family owns the legal move set, {@link CoreDefinition.AttackPattern} remains the only attack
 * clock, and a module may only expose behaviour semantics through a socket declared by the family.
 * This runtime boundary deliberately contains no damage/range/resource-cost or presentation values.</p>
 */
public final class PlayerWeaponRuntimeProfile {
    public static final String RECOVERY_PIVOT = "recovery_pivot";

    private final CoreDefinition.WeaponFamily family;
    private final Optional<CoreDefinition.WeaponModule> module;
    private final Map<ContentId, CoreDefinition.AttackPattern> moves;

    private PlayerWeaponRuntimeProfile(
        CoreDefinition.WeaponFamily family,
        Optional<CoreDefinition.WeaponModule> module,
        Map<ContentId, CoreDefinition.AttackPattern> moves
    ) {
        this.family = Objects.requireNonNull(family, "family");
        this.module = Objects.requireNonNull(module, "module");
        this.moves = Map.copyOf(Objects.requireNonNull(moves, "moves"));
        if (this.moves.isEmpty()) {
            throw new IllegalArgumentException("weapon runtime profile requires at least one move");
        }
        if (!this.moves.keySet().equals(family.moves())) {
            throw new IllegalArgumentException("resolved move set must exactly match weapon family moves");
        }
    }

    public static PlayerWeaponRuntimeProfile assemble(
        CoreDefinition.WeaponFamily family,
        Optional<CoreDefinition.WeaponModule> module,
        Function<ContentId, CoreDefinition.AttackPattern> patternResolver
    ) {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(patternResolver, "patternResolver");

        module.ifPresent(candidate -> {
            if (!candidate.compatibleFamilies().contains(family.id())) {
                throw new IllegalStateException("Weapon module " + candidate.id() + " is not compatible with family " + family.id());
            }
            if (!family.moduleSockets().contains(candidate.socket())) {
                throw new IllegalStateException("Weapon module " + candidate.id() + " requires undeclared socket " + candidate.socket());
            }
        });

        Map<ContentId, CoreDefinition.AttackPattern> resolved = new LinkedHashMap<>();
        family.moves().stream().sorted().forEach(moveId -> {
            CoreDefinition.AttackPattern pattern = Objects.requireNonNull(patternResolver.apply(moveId), "resolved attack pattern");
            if (!pattern.id().equals(moveId)) {
                throw new IllegalStateException("Attack-pattern resolver returned mismatched id for " + moveId + ": " + pattern.id());
            }
            resolved.put(moveId, pattern);
        });
        return new PlayerWeaponRuntimeProfile(family, module, resolved);
    }

    public CoreDefinition.WeaponFamily family() {
        return family;
    }

    public Optional<CoreDefinition.WeaponModule> module() {
        return module;
    }

    public List<CoreDefinition.AttackPattern> moves() {
        return moves.values().stream().sorted((left, right) -> left.id().compareTo(right.id())).toList();
    }

    public CoreDefinition.AttackPattern requireMove(ContentId moveId) {
        Objects.requireNonNull(moveId, "moveId");
        CoreDefinition.AttackPattern pattern = moves.get(moveId);
        if (pattern == null) {
            throw new IllegalArgumentException("Move " + moveId + " is not authored for weapon family " + family.id());
        }
        return pattern;
    }

    public boolean hasModuleBehaviour(String behaviour) {
        String normalized = Objects.requireNonNull(behaviour, "behaviour").trim();
        if (normalized.isEmpty()) return false;
        return module.map(candidate -> candidate.behaviourChanges().contains(normalized)).orElse(false);
    }
}
