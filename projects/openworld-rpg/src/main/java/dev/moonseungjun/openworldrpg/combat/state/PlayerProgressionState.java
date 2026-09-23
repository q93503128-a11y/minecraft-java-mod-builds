package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.progression.ProjectProgressionRules;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent server-owned player progression.
 *
 * <p>Existing combat-level/allocation saves remain readable: newly introduced XP / Class Rank
 * fields have explicit backward-compatible defaults. One-time XP grants also persist stable
 * transaction IDs so reward retries after reconnect cannot duplicate progression.</p>
 */
public record PlayerProgressionState(
        int combatLevel,
        long combatXp,
        Optional<RootClass> activeClass,
        AttributeAllocation warrior,
        AttributeAllocation hunter,
        AttributeAllocation cleric,
        AttributeAllocation mage,
        AttributeAllocation guardian,
        RootClassProgress warriorProgress,
        RootClassProgress hunterProgress,
        RootClassProgress clericProgress,
        RootClassProgress mageProgress,
        RootClassProgress guardianProgress,
        Set<String> appliedCombatXpTransactionIds,
        Set<String> appliedClassXpTransactionIds
) {
    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<PlayerProgressionState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(
                            ProjectCombatRules.MIN_CONTENT_LEVEL,
                            ProjectCombatRules.MAX_CONTENT_LEVEL
                    ).fieldOf("combat_level").forGetter(PlayerProgressionState::combatLevel),
                    Codec.LONG.optionalFieldOf("combat_xp", 0L).forGetter(PlayerProgressionState::combatXp),
                    RootClass.CODEC.optionalFieldOf("active_class").forGetter(PlayerProgressionState::activeClass),
                    AttributeAllocation.CODEC.fieldOf("warrior").forGetter(PlayerProgressionState::warrior),
                    AttributeAllocation.CODEC.fieldOf("hunter").forGetter(PlayerProgressionState::hunter),
                    AttributeAllocation.CODEC.fieldOf("cleric").forGetter(PlayerProgressionState::cleric),
                    AttributeAllocation.CODEC.fieldOf("mage").forGetter(PlayerProgressionState::mage),
                    AttributeAllocation.CODEC.fieldOf("guardian").forGetter(PlayerProgressionState::guardian),
                    RootClassProgress.CODEC
                            .optionalFieldOf("warrior_class_progress", RootClassProgress.initial())
                            .forGetter(PlayerProgressionState::warriorProgress),
                    RootClassProgress.CODEC
                            .optionalFieldOf("hunter_class_progress", RootClassProgress.initial())
                            .forGetter(PlayerProgressionState::hunterProgress),
                    RootClassProgress.CODEC
                            .optionalFieldOf("cleric_class_progress", RootClassProgress.initial())
                            .forGetter(PlayerProgressionState::clericProgress),
                    RootClassProgress.CODEC
                            .optionalFieldOf("mage_class_progress", RootClassProgress.initial())
                            .forGetter(PlayerProgressionState::mageProgress),
                    RootClassProgress.CODEC
                            .optionalFieldOf("guardian_class_progress", RootClassProgress.initial())
                            .forGetter(PlayerProgressionState::guardianProgress),
                    STRING_SET_CODEC
                            .optionalFieldOf("applied_combat_xp_transaction_ids", Set.of())
                            .forGetter(PlayerProgressionState::appliedCombatXpTransactionIds),
                    STRING_SET_CODEC
                            .optionalFieldOf("applied_class_xp_transaction_ids", Set.of())
                            .forGetter(PlayerProgressionState::appliedClassXpTransactionIds)
            ).apply(instance, PlayerProgressionState::new)
    );

    public PlayerProgressionState {
        if (combatLevel < ProjectCombatRules.MIN_CONTENT_LEVEL
                || combatLevel > ProjectCombatRules.MAX_CONTENT_LEVEL) {
            throw new IllegalArgumentException("combatLevel must be inside [1, 80].");
        }
        if (combatXp < 0L) {
            throw new IllegalArgumentException("combatXp cannot be negative.");
        }
        if (combatLevel == ProjectProgressionRules.MAX_COMBAT_LEVEL) {
            if (combatXp != 0L) {
                throw new IllegalArgumentException("Lv 80 cannot store overflow combat XP.");
            }
        } else if (combatXp >= ProjectProgressionRules.combatXpToNext(combatLevel)) {
            throw new IllegalArgumentException(
                    "Current-Lv combat XP must remain below the next-level requirement."
            );
        }

        Objects.requireNonNull(activeClass, "activeClass");
        Objects.requireNonNull(warrior, "warrior");
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(cleric, "cleric");
        Objects.requireNonNull(mage, "mage");
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(warriorProgress, "warriorProgress");
        Objects.requireNonNull(hunterProgress, "hunterProgress");
        Objects.requireNonNull(clericProgress, "clericProgress");
        Objects.requireNonNull(mageProgress, "mageProgress");
        Objects.requireNonNull(guardianProgress, "guardianProgress");

        appliedCombatXpTransactionIds = immutableTransactionIds(
                appliedCombatXpTransactionIds,
                "appliedCombatXpTransactionIds"
        );
        appliedClassXpTransactionIds = immutableTransactionIds(
                appliedClassXpTransactionIds,
                "appliedClassXpTransactionIds"
        );

        int earned = combatLevel - 1;
        for (RootClass rootClass : RootClass.values()) {
            if (allocation(rootClass, warrior, hunter, cleric, mage, guardian).spentPoints() > earned) {
                throw new IllegalArgumentException(
                        "Attribute allocation for " + rootClass
                                + " exceeds earned pool at Lv " + combatLevel + "."
                );
            }
        }
    }

    public static PlayerProgressionState initial() {
        AttributeAllocation empty = AttributeAllocation.unspent();
        RootClassProgress untrained = RootClassProgress.initial();
        return new PlayerProgressionState(
                1,
                0L,
                Optional.empty(),
                empty,
                empty,
                empty,
                empty,
                empty,
                untrained,
                untrained,
                untrained,
                untrained,
                untrained,
                Set.of(),
                Set.of()
        );
    }

    public AttributeAllocation allocation(RootClass rootClass) {
        return allocation(rootClass, warrior, hunter, cleric, mage, guardian);
    }

    public RootClassProgress classProgress(RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        return switch (rootClass) {
            case WARRIOR -> warriorProgress;
            case HUNTER -> hunterProgress;
            case CLERIC -> clericProgress;
            case MAGE -> mageProgress;
            case GUARDIAN -> guardianProgress;
        };
    }

    /**
     * Exact administrative/bootstrap level setter. Changing Lv clears current-Lv XP.
     */
    public PlayerProgressionState withCombatLevel(int level) {
        if (level == combatLevel) {
            return this;
        }
        return copy(
                level,
                0L,
                activeClass,
                warrior,
                hunter,
                cleric,
                mage,
                guardian,
                warriorProgress,
                hunterProgress,
                clericProgress,
                mageProgress,
                guardianProgress,
                appliedCombatXpTransactionIds,
                appliedClassXpTransactionIds
        );
    }

    public PlayerProgressionState withActiveClass(RootClass rootClass) {
        Optional<RootClass> next = Optional.of(Objects.requireNonNull(rootClass, "rootClass"));
        if (activeClass.equals(next)) {
            return this;
        }
        return copy(
                combatLevel,
                combatXp,
                next,
                warrior,
                hunter,
                cleric,
                mage,
                guardian,
                warriorProgress,
                hunterProgress,
                clericProgress,
                mageProgress,
                guardianProgress,
                appliedCombatXpTransactionIds,
                appliedClassXpTransactionIds
        );
    }

    public PlayerProgressionState withAllocation(
            RootClass rootClass,
            AttributeAllocation newAllocation
    ) {
        Objects.requireNonNull(rootClass, "rootClass");
        Objects.requireNonNull(newAllocation, "newAllocation");
        return copy(
                combatLevel,
                combatXp,
                activeClass,
                rootClass == RootClass.WARRIOR ? newAllocation : warrior,
                rootClass == RootClass.HUNTER ? newAllocation : hunter,
                rootClass == RootClass.CLERIC ? newAllocation : cleric,
                rootClass == RootClass.MAGE ? newAllocation : mage,
                rootClass == RootClass.GUARDIAN ? newAllocation : guardian,
                warriorProgress,
                hunterProgress,
                clericProgress,
                mageProgress,
                guardianProgress,
                appliedCombatXpTransactionIds,
                appliedClassXpTransactionIds
        );
    }

    public PlayerProgressionState grantCombatXpOnce(
            String transactionId,
            long amount
    ) {
        requireTransactionId(transactionId);
        if (amount <= 0L) {
            throw new IllegalArgumentException("Combat XP grant must be positive.");
        }
        if (appliedCombatXpTransactionIds.contains(transactionId)) {
            return this;
        }

        int nextLevel = combatLevel;
        long nextXp = combatXp;
        long remaining = amount;

        while (remaining > 0L && nextLevel < ProjectProgressionRules.MAX_COMBAT_LEVEL) {
            long requirement = ProjectProgressionRules.combatXpToNext(nextLevel);
            long needed = requirement - nextXp;
            if (remaining < needed) {
                nextXp = Math.addExact(nextXp, remaining);
                remaining = 0L;
            } else {
                remaining -= needed;
                nextLevel++;
                nextXp = 0L;
            }
        }

        if (nextLevel == ProjectProgressionRules.MAX_COMBAT_LEVEL) {
            nextXp = 0L;
        }

        Set<String> nextTransactions = new HashSet<>(appliedCombatXpTransactionIds);
        nextTransactions.add(transactionId);
        return copy(
                nextLevel,
                nextXp,
                activeClass,
                warrior,
                hunter,
                cleric,
                mage,
                guardian,
                warriorProgress,
                hunterProgress,
                clericProgress,
                mageProgress,
                guardianProgress,
                Set.copyOf(nextTransactions),
                appliedClassXpTransactionIds
        );
    }

    public PlayerProgressionState grantClassXpOnce(
            String transactionId,
            RootClass rootClass,
            long amount
    ) {
        requireTransactionId(transactionId);
        Objects.requireNonNull(rootClass, "rootClass");
        if (amount <= 0L) {
            throw new IllegalArgumentException("Class XP grant must be positive.");
        }
        if (appliedClassXpTransactionIds.contains(transactionId)) {
            return this;
        }

        RootClassProgress nextProgress = classProgress(rootClass).grantXp(amount);
        Set<String> nextTransactions = new HashSet<>(appliedClassXpTransactionIds);
        nextTransactions.add(transactionId);

        return copy(
                combatLevel,
                combatXp,
                activeClass,
                warrior,
                hunter,
                cleric,
                mage,
                guardian,
                rootClass == RootClass.WARRIOR ? nextProgress : warriorProgress,
                rootClass == RootClass.HUNTER ? nextProgress : hunterProgress,
                rootClass == RootClass.CLERIC ? nextProgress : clericProgress,
                rootClass == RootClass.MAGE ? nextProgress : mageProgress,
                rootClass == RootClass.GUARDIAN ? nextProgress : guardianProgress,
                appliedCombatXpTransactionIds,
                Set.copyOf(nextTransactions)
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

    private PlayerProgressionState copy(
            int nextCombatLevel,
            long nextCombatXp,
            Optional<RootClass> nextActiveClass,
            AttributeAllocation nextWarrior,
            AttributeAllocation nextHunter,
            AttributeAllocation nextCleric,
            AttributeAllocation nextMage,
            AttributeAllocation nextGuardian,
            RootClassProgress nextWarriorProgress,
            RootClassProgress nextHunterProgress,
            RootClassProgress nextClericProgress,
            RootClassProgress nextMageProgress,
            RootClassProgress nextGuardianProgress,
            Set<String> nextCombatTransactions,
            Set<String> nextClassTransactions
    ) {
        return new PlayerProgressionState(
                nextCombatLevel,
                nextCombatXp,
                nextActiveClass,
                nextWarrior,
                nextHunter,
                nextCleric,
                nextMage,
                nextGuardian,
                nextWarriorProgress,
                nextHunterProgress,
                nextClericProgress,
                nextMageProgress,
                nextGuardianProgress,
                nextCombatTransactions,
                nextClassTransactions
        );
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

    private static Set<String> immutableTransactionIds(
            Set<String> value,
            String name
    ) {
        Objects.requireNonNull(value, name);
        Set<String> copy = Set.copyOf(value);
        copy.forEach(PlayerProgressionState::requireTransactionId);
        return copy;
    }

    private static void requireTransactionId(String transactionId) {
        if (transactionId == null
                || transactionId.isBlank()
                || !transactionId.contains(":")) {
            throw new IllegalArgumentException(
                    "Progression transaction ID must be a stable namespaced ID."
            );
        }
    }
}
