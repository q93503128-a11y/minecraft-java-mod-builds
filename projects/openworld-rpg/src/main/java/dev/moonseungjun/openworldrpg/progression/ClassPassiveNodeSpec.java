package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;

public record ClassPassiveNodeSpec(
        String id,
        String name,
        RootClass rootClass,
        Optional<ClassSpecialization> specialization,
        ClassPassiveTier tier,
        int maxRank
) {
    public ClassPassiveNodeSpec {
        if (id == null || !id.startsWith("openworld_rpg:passive/") || id.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Passive node requires a stable Openworld RPG id.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Passive node canonical name cannot be blank.");
        }
        Objects.requireNonNull(rootClass, "rootClass");
        specialization = Objects.requireNonNull(specialization, "specialization");
        Objects.requireNonNull(tier, "tier");
        if (maxRank < 1 || maxRank > 3) {
            throw new IllegalArgumentException("Passive node max rank must be inside [1, 3].");
        }
        if (specialization.isEmpty() && tier != ClassPassiveTier.ROOT) {
            throw new IllegalArgumentException("Root passive nodes must use ROOT tier.");
        }
        if (specialization.isPresent()) {
            if (tier == ClassPassiveTier.ROOT) {
                throw new IllegalArgumentException("Branch passive node cannot use ROOT tier.");
            }
            if (specialization.orElseThrow().rootClass() != rootClass) {
                throw new IllegalArgumentException("Passive specialization/root mismatch.");
            }
        }
    }

    public boolean rootNode() {
        return specialization.isEmpty();
    }
}
