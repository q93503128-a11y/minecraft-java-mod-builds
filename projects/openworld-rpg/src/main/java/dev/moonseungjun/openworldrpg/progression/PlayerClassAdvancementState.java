package dev.moonseungjun.openworldrpg.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.Optional;

public record PlayerClassAdvancementState(
        int schemaVersion,
        RootClassSpecializationState warrior,
        RootClassSpecializationState hunter,
        RootClassSpecializationState cleric,
        RootClassSpecializationState mage,
        RootClassSpecializationState guardian,
        long branchSwitchSequence,
        Optional<PendingBranchSwitch> pendingBranchSwitch
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<PlayerClassAdvancementState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .optionalFieldOf(
                                    "class_advancement_schema_version",
                                    CURRENT_SCHEMA_VERSION
                            )
                            .forGetter(PlayerClassAdvancementState::schemaVersion),
                    RootClassSpecializationState.CODEC
                            .optionalFieldOf(
                                    "warrior",
                                    RootClassSpecializationState.initial()
                            )
                            .forGetter(PlayerClassAdvancementState::warrior),
                    RootClassSpecializationState.CODEC
                            .optionalFieldOf(
                                    "hunter",
                                    RootClassSpecializationState.initial()
                            )
                            .forGetter(PlayerClassAdvancementState::hunter),
                    RootClassSpecializationState.CODEC
                            .optionalFieldOf(
                                    "cleric",
                                    RootClassSpecializationState.initial()
                            )
                            .forGetter(PlayerClassAdvancementState::cleric),
                    RootClassSpecializationState.CODEC
                            .optionalFieldOf(
                                    "mage",
                                    RootClassSpecializationState.initial()
                            )
                            .forGetter(PlayerClassAdvancementState::mage),
                    RootClassSpecializationState.CODEC
                            .optionalFieldOf(
                                    "guardian",
                                    RootClassSpecializationState.initial()
                            )
                            .forGetter(PlayerClassAdvancementState::guardian),
                    Codec.LONG.optionalFieldOf("branch_switch_sequence", 0L)
                            .forGetter(PlayerClassAdvancementState::branchSwitchSequence),
                    PendingBranchSwitch.CODEC
                            .optionalFieldOf("pending_branch_switch")
                            .forGetter(PlayerClassAdvancementState::pendingBranchSwitch)
            ).apply(instance, PlayerClassAdvancementState::new));

    public PlayerClassAdvancementState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported class-advancement schema version: "
                            + schemaVersion
            );
        }
        Objects.requireNonNull(warrior, "warrior");
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(cleric, "cleric");
        Objects.requireNonNull(mage, "mage");
        Objects.requireNonNull(guardian, "guardian");
        if (branchSwitchSequence < 0L) {
            throw new IllegalArgumentException(
                    "Branch-switch sequence must be non-negative."
            );
        }
        pendingBranchSwitch = Objects.requireNonNull(
                pendingBranchSwitch,
                "pendingBranchSwitch"
        );

        validateRoot(RootClass.WARRIOR, warrior);
        validateRoot(RootClass.HUNTER, hunter);
        validateRoot(RootClass.CLERIC, cleric);
        validateRoot(RootClass.MAGE, mage);
        validateRoot(RootClass.GUARDIAN, guardian);

        PendingBranchSwitch pending = pendingBranchSwitch.orElse(null);
        if (pending != null) {
            if (pending.sequence() != branchSwitchSequence) {
                throw new IllegalArgumentException(
                        "Pending branch-switch sequence mismatch."
                );
            }
            RootClassSpecializationState root = rootState(
                    pending.rootClass(),
                    warrior,
                    hunter,
                    cleric,
                    mage,
                    guardian
            );
            if (!root.active().equals(Optional.of(pending.source()))) {
                throw new IllegalArgumentException(
                        "Pending branch-switch source must be the active specialization."
                );
            }
            if (!root.isUnlocked(pending.target())) {
                throw new IllegalArgumentException(
                        "Pending branch-switch target must already be unlocked."
                );
            }
        }
    }

    public static PlayerClassAdvancementState initial() {
        RootClassSpecializationState empty =
                RootClassSpecializationState.initial();
        return new PlayerClassAdvancementState(
                CURRENT_SCHEMA_VERSION,
                empty,
                empty,
                empty,
                empty,
                empty,
                0L,
                Optional.empty()
        );
    }

    public RootClassSpecializationState rootState(RootClass rootClass) {
        return rootState(
                rootClass,
                warrior,
                hunter,
                cleric,
                mage,
                guardian
        );
    }

    public PlayerClassAdvancementState unlockSpecialization(
            ClassSpecialization specialization
    ) {
        Objects.requireNonNull(specialization, "specialization");
        RootClass rootClass = specialization.rootClass();
        RootClassSpecializationState current = rootState(rootClass);
        RootClassSpecializationState next = current.unlock(specialization);
        if (current.equals(next)) {
            return this;
        }
        return withRootState(rootClass, next, branchSwitchSequence, pendingBranchSwitch);
    }

    public PlayerClassAdvancementState prepareBranchSwitch(
            ClassSpecialization target,
            long goldCost
    ) {
        Objects.requireNonNull(target, "target");
        if (goldCost <= 0L) {
            throw new IllegalArgumentException(
                    "Branch-switch Gold cost must be positive."
            );
        }
        if (pendingBranchSwitch.isPresent()) {
            throw new IllegalStateException(
                    "Another branch-switch transaction is already pending."
            );
        }

        RootClass rootClass = target.rootClass();
        RootClassSpecializationState root = rootState(rootClass);
        ClassSpecialization source = root.active().orElseThrow(
                () -> new IllegalStateException(
                        "Cannot switch branch before the first specialization is active."
                )
        );
        if (source == target) {
            throw new IllegalArgumentException(
                    "Branch switch requires a different target specialization."
            );
        }
        if (!root.isUnlocked(target)) {
            throw new IllegalStateException(
                    "Branch switch target must already be unlocked."
            );
        }

        long sequence = Math.addExact(branchSwitchSequence, 1L);
        PendingBranchSwitch pending = new PendingBranchSwitch(
                "openworld_rpg:branch_switch/" + sequence,
                sequence,
                rootClass,
                source,
                target,
                goldCost
        );
        return new PlayerClassAdvancementState(
                schemaVersion,
                warrior,
                hunter,
                cleric,
                mage,
                guardian,
                sequence,
                Optional.of(pending)
        );
    }

    public PlayerClassAdvancementState commitBranchSwitch(
            String transactionId
    ) {
        PendingBranchSwitch pending = requirePending(transactionId);
        RootClassSpecializationState root = rootState(
                pending.rootClass()
        ).activate(pending.target());
        return withRootState(
                pending.rootClass(),
                root,
                pending.sequence(),
                Optional.empty()
        );
    }

    public PlayerClassAdvancementState cancelBranchSwitch(
            String transactionId
    ) {
        PendingBranchSwitch pending = requirePending(transactionId);
        return new PlayerClassAdvancementState(
                schemaVersion,
                warrior,
                hunter,
                cleric,
                mage,
                guardian,
                pending.sequence(),
                Optional.empty()
        );
    }

    private PendingBranchSwitch requirePending(String transactionId) {
        requireStableId(transactionId);
        PendingBranchSwitch pending = pendingBranchSwitch.orElseThrow(
                () -> new IllegalStateException(
                        "No branch-switch transaction is pending."
                )
        );
        if (!pending.transactionId().equals(transactionId)) {
            throw new IllegalStateException(
                    "Cannot mutate a different branch-switch transaction."
            );
        }
        return pending;
    }

    private PlayerClassAdvancementState withRootState(
            RootClass rootClass,
            RootClassSpecializationState next,
            long nextSequence,
            Optional<PendingBranchSwitch> nextPending
    ) {
        return new PlayerClassAdvancementState(
                schemaVersion,
                rootClass == RootClass.WARRIOR ? next : warrior,
                rootClass == RootClass.HUNTER ? next : hunter,
                rootClass == RootClass.CLERIC ? next : cleric,
                rootClass == RootClass.MAGE ? next : mage,
                rootClass == RootClass.GUARDIAN ? next : guardian,
                nextSequence,
                nextPending
        );
    }

    private static RootClassSpecializationState rootState(
            RootClass rootClass,
            RootClassSpecializationState warrior,
            RootClassSpecializationState hunter,
            RootClassSpecializationState cleric,
            RootClassSpecializationState mage,
            RootClassSpecializationState guardian
    ) {
        Objects.requireNonNull(rootClass, "rootClass");
        return switch (rootClass) {
            case WARRIOR -> warrior;
            case HUNTER -> hunter;
            case CLERIC -> cleric;
            case MAGE -> mage;
            case GUARDIAN -> guardian;
        };
    }

    private static void validateRoot(
            RootClass rootClass,
            RootClassSpecializationState state
    ) {
        for (ClassSpecialization specialization : state.unlocked()) {
            if (specialization.rootClass() != rootClass) {
                throw new IllegalArgumentException(
                        "Specialization " + specialization.id()
                                + " is stored under the wrong root class."
                );
            }
        }
        state.active().ifPresent(specialization -> {
            if (specialization.rootClass() != rootClass) {
                throw new IllegalArgumentException(
                        "Active specialization is stored under the wrong root class."
                );
            }
        });
    }

    public record PendingBranchSwitch(
            String transactionId,
            long sequence,
            RootClass rootClass,
            ClassSpecialization source,
            ClassSpecialization target,
            long goldCost
    ) {
        public static final Codec<PendingBranchSwitch> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingBranchSwitch::transactionId),
                        Codec.LONG.fieldOf("sequence")
                                .forGetter(PendingBranchSwitch::sequence),
                        RootClass.CODEC.fieldOf("root_class")
                                .forGetter(PendingBranchSwitch::rootClass),
                        ClassSpecialization.CODEC.fieldOf("source")
                                .forGetter(PendingBranchSwitch::source),
                        ClassSpecialization.CODEC.fieldOf("target")
                                .forGetter(PendingBranchSwitch::target),
                        Codec.LONG.fieldOf("gold_cost")
                                .forGetter(PendingBranchSwitch::goldCost)
                ).apply(instance, PendingBranchSwitch::new));

        public PendingBranchSwitch {
            requireStableId(transactionId);
            if (sequence <= 0L) {
                throw new IllegalArgumentException(
                        "Branch-switch sequence must be positive."
                );
            }
            Objects.requireNonNull(rootClass, "rootClass");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(target, "target");
            if (source.rootClass() != rootClass
                    || target.rootClass() != rootClass
                    || source == target) {
                throw new IllegalArgumentException(
                        "Branch-switch source and target must be different specializations of one root class."
                );
            }
            if (goldCost <= 0L) {
                throw new IllegalArgumentException(
                        "Pending branch-switch Gold cost must be positive."
                );
            }
        }

        public String debitTransactionId() {
            return transactionId + "/gold";
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Branch-switch transaction id must be namespaced."
            );
        }
    }
}
