package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import java.util.Objects;
import java.util.Optional;

/**
 * Persistent server-owned combat progression slice needed to produce combat snapshots.
 *
 * <p>Class Rank/XP/passives remain separate future progression data. This state owns only the
 * global combat Lv plus the per-root-class Attribute allocations already closed by combat canon.</p>
 */
public record PlayerProgressionState(
        int combatLevel,
        Optional<RootClass> activeClass,
        AttributeAllocation warrior,
        AttributeAllocation hunter,
        AttributeAllocation cleric,
        AttributeAllocation mage,
        AttributeAllocation guardian
) {
    public static final Codec<PlayerProgressionState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(
                            ProjectCombatRules.MIN_CONTENT_LEVEL,
                            ProjectCombatRules.MAX_CONTENT_LEVEL
                    ).fieldOf("combat_level").forGetter(PlayerProgressionState::combatLevel),
                    RootClass.CODEC.optionalFieldOf("active_class").forGetter(PlayerProgressionState::activeClass),
                    AttributeAllocation.CODEC.fieldOf("warrior").forGetter(PlayerProgressionState::warrior),
                    AttributeAllocation.CODEC.fieldOf("hunter").forGetter(PlayerProgressionState::hunter),
                    AttributeAllocation.CODEC.fieldOf("cleric").forGetter(PlayerProgressionState::cleric),
                    AttributeAllocation.CODEC.fieldOf("mage").forGetter(PlayerProgressionState::mage),
                    AttributeAllocation.CODEC.fieldOf("guardian").forGetter(PlayerProgressionState::guardian)
            ).apply(instance, PlayerProgressionState::new)
    );

    public PlayerProgressionState {
        if (combatLevel < ProjectCombatRules.MIN_CONTENT_LEVEL
                || combatLevel > ProjectCombatRules.MAX_CONTENT_LEVEL) {
            throw new IllegalArgumentException("combatLevel must be inside [1, 80].");
        }
        Objects.requireNonNull(activeClass, "activeClass");
        Objects.requireNonNull(warrior, "warrior");
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(cleric, "cleric");
        Objects.requireNonNull(mage, "mage");
        Objects.requireNonNull(guardian, "guardian");

        int earned = combatLevel - 1;
        for (RootClass rootClass : RootClass.values()) {
            if (allocation(rootClass, warrior, hunter, cleric, mage, guardian).spentPoints() > earned) {
                throw new IllegalArgumentException(
                        "Attribute allocation for " + rootClass + " exceeds earned pool at Lv " + combatLevel + "."
                );
            }
        }
    }

    public static PlayerProgressionState initial() {
        AttributeAllocation empty = AttributeAllocation.unspent();
        return new PlayerProgressionState(
                1,
                Optional.empty(),
                empty,
                empty,
                empty,
                empty,
                empty
        );
    }

    public AttributeAllocation allocation(RootClass rootClass) {
        return allocation(rootClass, warrior, hunter, cleric, mage, guardian);
    }

    public PlayerProgressionState withCombatLevel(int level) {
        return new PlayerProgressionState(
                level,
                activeClass,
                warrior,
                hunter,
                cleric,
                mage,
                guardian
        );
    }

    public PlayerProgressionState withActiveClass(RootClass rootClass) {
        return new PlayerProgressionState(
                combatLevel,
                Optional.of(Objects.requireNonNull(rootClass, "rootClass")),
                warrior,
                hunter,
                cleric,
                mage,
                guardian
        );
    }

    public PlayerProgressionState withAllocation(
            RootClass rootClass,
            AttributeAllocation newAllocation
    ) {
        Objects.requireNonNull(rootClass, "rootClass");
        Objects.requireNonNull(newAllocation, "newAllocation");
        return new PlayerProgressionState(
                combatLevel,
                activeClass,
                rootClass == RootClass.WARRIOR ? newAllocation : warrior,
                rootClass == RootClass.HUNTER ? newAllocation : hunter,
                rootClass == RootClass.CLERIC ? newAllocation : cleric,
                rootClass == RootClass.MAGE ? newAllocation : mage,
                rootClass == RootClass.GUARDIAN ? newAllocation : guardian
        );
    }

    public Optional<PlayerCombatBuildState> buildWith(EquipmentCombatState equipment) {
        Objects.requireNonNull(equipment, "equipment");
        return activeClass.map(rootClass -> new PlayerCombatBuildState(
                combatLevel,
                rootClass,
                allocation(rootClass),
                equipment
        ));
    }

    private static AttributeAllocation allocation(
            RootClass rootClass,
            AttributeAllocation warrior,
            AttributeAllocation hunter,
            AttributeAllocation cleric,
            AttributeAllocation mage,
            AttributeAllocation guardian
    ) {
        return switch (rootClass) {
            case WARRIOR -> warrior;
            case HUNTER -> hunter;
            case CLERIC -> cleric;
            case MAGE -> mage;
            case GUARDIAN -> guardian;
        };
    }
}
