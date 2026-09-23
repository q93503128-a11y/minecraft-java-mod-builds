package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent, server-owned personal R01 state.
 *
 * <p>This is the schema-v1 projection required by R01_VERTICAL_SLICE.md,
 * R01_CONTENT_BIBLE.md and QUEST_WORLD_STATE.md. Physical shared-world encounter state remains
 * outside this record. Stable internal IDs are persisted; player-facing text is never used as
 * identity.</p>
 */
public record R01PlayerState(
        int schemaVersion,
        OpeningState opening,
        QuarryState quarry,
        WorldLoopState worldLoops,
        EconomyState economy,
        TransactionLedger ledger,
        long updatedAtWorldTick
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );
    private static final Codec<Set<Integer>> INT_SET_CODEC = Codec.INT.listOf().xmap(
            Set::copyOf,
            value -> value.stream().sorted().toList()
    );

    public static final Codec<R01PlayerState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("player_state_schema_version")
                            .forGetter(R01PlayerState::schemaVersion),
                    OpeningState.CODEC.fieldOf("opening").forGetter(R01PlayerState::opening),
                    QuarryState.CODEC.fieldOf("quarry").forGetter(R01PlayerState::quarry),
                    WorldLoopState.CODEC.fieldOf("world_loops").forGetter(R01PlayerState::worldLoops),
                    EconomyState.CODEC.fieldOf("economy").forGetter(R01PlayerState::economy),
                    TransactionLedger.CODEC.fieldOf("transaction_ledger").forGetter(R01PlayerState::ledger),
                    Codec.LONG.fieldOf("updated_at_world_tick").forGetter(R01PlayerState::updatedAtWorldTick)
            ).apply(instance, R01PlayerState::new)
    );

    public R01PlayerState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported R01 player-state schema version: " + schemaVersion
            );
        }
        Objects.requireNonNull(opening, "opening");
        Objects.requireNonNull(quarry, "quarry");
        Objects.requireNonNull(worldLoops, "worldLoops");
        Objects.requireNonNull(economy, "economy");
        Objects.requireNonNull(ledger, "ledger");
        if (updatedAtWorldTick < 0L) {
            throw new IllegalArgumentException("updatedAtWorldTick must be non-negative.");
        }
    }

    public static R01PlayerState initial() {
        return new R01PlayerState(
                CURRENT_SCHEMA_VERSION,
                OpeningState.initial(),
                QuarryState.initial(),
                WorldLoopState.initial(),
                EconomyState.initial(),
                TransactionLedger.initial(),
                0L
        );
    }

    public static final String OPENING_LOADOUT_CLAIM_ID =
            "openworld_rpg:r01/opening_loadout";

    public boolean openingLoadoutClaimed() {
        return ledger.rewardClaimIds().contains(OPENING_LOADOUT_CLAIM_ID);
    }

    public R01PlayerState markOpeningLoadoutClaimed(long worldTick) {
        validateTick(worldTick);
        return changed(
                opening,
                quarry,
                worldLoops,
                economy,
                ledger.withRewardClaimId(OPENING_LOADOUT_CLAIM_ID),
                worldTick
        );
    }

    public R01PlayerState markFirstShrineActivated(long worldTick) {
        validateTick(worldTick);
        OpeningState nextOpening = opening
                .withFirstShrineActivated()
                .withMainStage(R01MainStage.ALDERFORD_REACHED);
        TransactionLedger nextLedger = ledger.withDiscoveryFlag(
                "openworld_rpg:r01/alderford_gate_shrine"
        );
        return changed(nextOpening, quarry, worldLoops, economy, nextLedger, worldTick);
    }

    public R01PlayerState markFirstRootClassSelected(long worldTick) {
        validateTick(worldTick);
        OpeningState nextOpening = opening
                .withFirstRootClassSelected()
                .withMainStage(R01MainStage.FIRST_CLASS_SELECTED);
        nextOpening = reconcileQuarryRoadActivation(nextOpening);
        TransactionLedger nextLedger = ledger.withChoiceFlag(
                "openworld_rpg:r01/first_root_class_selected"
        );
        return changed(nextOpening, quarry, worldLoops, economy, nextLedger, worldTick);
    }

    public R01PlayerState markStarterPackageClaimed(long worldTick) {
        validateTick(worldTick);
        OpeningState nextOpening = opening.withStarterPackageClaimed();
        nextOpening = reconcileQuarryRoadActivation(nextOpening);
        TransactionLedger nextLedger = ledger.withRewardClaimId(
                "openworld_rpg:r01/starter_package"
        );
        return changed(nextOpening, quarry, worldLoops, economy, nextLedger, worldTick);
    }

    public R01PlayerState markDodgeHintSeen(long worldTick) {
        validateTick(worldTick);
        return changed(
                opening.withDodgeHintSeen(),
                quarry,
                worldLoops,
                economy,
                ledger,
                worldTick
        );
    }

    public R01PlayerState markDodgeUsedOnce(long worldTick) {
        validateTick(worldTick);
        return changed(
                opening.withDodgeUsedOnce(),
                quarry,
                worldLoops,
                economy,
                ledger,
                worldTick
        );
    }

    public R01PlayerState recordQuarryRoadAction(
            QuarryRoadAction action,
            long worldTick
    ) {
        Objects.requireNonNull(action, "action");
        validateTick(worldTick);

        int nextBits = opening.quarryRoadActionBits() | action.mask();
        if (nextBits == opening.quarryRoadActionBits()) {
            return this;
        }

        OpeningState nextOpening = opening.withQuarryRoadActionBits(nextBits);
        TransactionLedger nextLedger = ledger.withObjectiveProgress(
                "openworld_rpg:r01/dust_on_quarry_road/" + action.id(),
                1
        );

        if (Integer.bitCount(nextBits) >= 3) {
            nextLedger = nextLedger.withCompletedStepId(
                    "openworld_rpg:r01/dust_on_quarry_road/complete"
            );
            nextOpening = nextOpening.withMainStage(
                    quarry.discovered()
                            ? R01MainStage.QUARRY_ENTRANCE_DISCOVERED
                            : R01MainStage.QUARRY_ROAD_COMPLETE
            );
        }

        return changed(nextOpening, quarry, worldLoops, economy, nextLedger, worldTick);
    }

    public R01PlayerState markRegalhartClueSeen(RegalhartClue clue, long worldTick) {
        Objects.requireNonNull(clue, "clue");
        validateTick(worldTick);

        int nextBits = opening.regalhartCluesSeenBits() | clue.mask();
        if (nextBits == opening.regalhartCluesSeenBits()) {
            return this;
        }

        OpeningState nextOpening = opening.withRegalhartCluesSeenBits(nextBits);
        TransactionLedger nextLedger = ledger.withObjectiveProgress(
                "openworld_rpg:r01/crowned_trail/" + clue.id(),
                1
        );
        if (Integer.bitCount(nextBits) >= 2) {
            nextOpening = nextOpening.withRegalhartDiscovered();
            nextLedger = nextLedger.withDiscoveryFlag(
                    "openworld_rpg:r01/regalhart_search_region"
            );
        }
        return changed(nextOpening, quarry, worldLoops, economy, nextLedger, worldTick);
    }

    public R01PlayerState markRegalhartDiscovered(long worldTick) {
        validateTick(worldTick);
        return changed(
                opening.withRegalhartDiscovered(),
                quarry,
                worldLoops,
                economy,
                ledger.withDiscoveryFlag("openworld_rpg:r01/regalhart"),
                worldTick
        );
    }

    public R01PlayerState markQuarryDiscovered(long worldTick) {
        validateTick(worldTick);
        OpeningState nextOpening = opening;
        if (opening.mainStage().isAtLeast(R01MainStage.QUARRY_ROAD_COMPLETE)) {
            nextOpening = opening.withMainStage(R01MainStage.QUARRY_ENTRANCE_DISCOVERED);
        }
        QuarryState nextQuarry = quarry.withDiscovered();
        TransactionLedger nextLedger = ledger.withDiscoveryFlag(
                "openworld_rpg:r01/quarry_entrance"
        );
        return changed(nextOpening, nextQuarry, worldLoops, economy, nextLedger, worldTick);
    }

    public R01PlayerState markQuarryWaystoneDiscovered(long worldTick) {
        validateTick(worldTick);
        return changed(
                opening,
                quarry.withWaystoneDiscovered(),
                worldLoops,
                economy,
                ledger.withDiscoveryFlag("openworld_rpg:r01/quarry_waystone"),
                worldTick
        );
    }

    public R01PlayerState markQuarryWaystoneActivated(long worldTick) {
        validateTick(worldTick);
        if (!quarry.waystoneDiscovered()) {
            throw new IllegalStateException(
                    "Quarry Waystone cannot activate before personal discovery commits."
            );
        }
        return changed(
                opening,
                quarry.withWaystoneActivated(),
                worldLoops,
                economy,
                ledger.withCompletedStepId("openworld_rpg:r01/quarry_waystone/activated"),
                worldTick
        );
    }

    public R01PlayerState beginQuarryRun(long runId, long worldTick) {
        validateTick(worldTick);
        if (runId <= 0L) {
            throw new IllegalArgumentException("runId must be positive.");
        }
        if (!quarry.discovered()
                || !opening.mainStage().isAtLeast(R01MainStage.QUARRY_ROAD_COMPLETE)) {
            throw new IllegalStateException(
                    "R01 main quarry run cannot advance before quarry-road completion and discovery."
            );
        }
        return changed(
                opening.withMainStage(R01MainStage.QUARRY_DUNGEON_ACTIVE),
                quarry.withRun(runId, Optional.of("active")),
                worldLoops,
                economy,
                ledger.withQuestStepId(
                        "openworld_rpg:r01/roots_below_stone",
                        "quarry_dungeon_active"
                ),
                worldTick
        );
    }

    public R01PlayerState markQuarryLiftOpen(long worldTick) {
        validateTick(worldTick);
        return changed(
                opening,
                quarry.withLiftOpen(),
                worldLoops,
                economy,
                ledger.withCompletedStepId("openworld_rpg:r01/quarry_lift_open"),
                worldTick
        );
    }

    public R01PlayerState markQuarryRelayEvidenceSeen(long worldTick) {
        validateTick(worldTick);
        return changed(
                opening,
                quarry.withRelayEvidenceSeen(),
                worldLoops,
                economy,
                ledger.withDiscoveryFlag("openworld_rpg:r01/quarry_relay_evidence"),
                worldTick
        );
    }

    public R01PlayerState markEarthloongFirstClear(long worldTick) {
        validateTick(worldTick);
        if (quarry.firstClear()) {
            return this;
        }

        QuarryState nextQuarry = quarry
                .withFirstClear()
                .withRun(quarry.runId(), Optional.of("cleared"))
                .withRewardChoicePending();
        EconomyState nextEconomy = economy.withPendingRewardClaimId(
                "openworld_rpg:r01/earthloong_first_clear_choice"
        );
        TransactionLedger nextLedger = ledger
                .withFirstClearId("openworld_rpg:r01_quarry")
                .withCompletedStepId("openworld_rpg:r01/earthloong_cleared");

        return changed(
                opening.withMainStage(R01MainStage.EARTHLOONG_CLEARED),
                nextQuarry,
                worldLoops,
                nextEconomy,
                nextLedger,
                worldTick
        );
    }

    private OpeningState reconcileQuarryRoadActivation(OpeningState candidate) {
        if (candidate.firstRootClassSelected()
                && candidate.starterPackageClaimed()
                && !candidate.mainStage().isAtLeast(R01MainStage.QUARRY_ROAD_ACTIVE)) {
            return candidate.withMainStage(R01MainStage.QUARRY_ROAD_ACTIVE);
        }
        return candidate;
    }

    private R01PlayerState changed(
            OpeningState nextOpening,
            QuarryState nextQuarry,
            WorldLoopState nextWorldLoops,
            EconomyState nextEconomy,
            TransactionLedger nextLedger,
            long worldTick
    ) {
        if (opening.equals(nextOpening)
                && quarry.equals(nextQuarry)
                && worldLoops.equals(nextWorldLoops)
                && economy.equals(nextEconomy)
                && ledger.equals(nextLedger)) {
            return this;
        }
        return new R01PlayerState(
                schemaVersion,
                nextOpening,
                nextQuarry,
                nextWorldLoops,
                nextEconomy,
                nextLedger,
                worldTick
        );
    }

    private static void validateTick(long worldTick) {
        if (worldTick < 0L) {
            throw new IllegalArgumentException("worldTick must be non-negative.");
        }
    }

    private static <T> Set<T> immutableSet(Set<T> value, String name) {
        Objects.requireNonNull(value, name);
        return Set.copyOf(value);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> value, String name) {
        Objects.requireNonNull(value, name);
        return Map.copyOf(value);
    }

    private static void requireStableId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be a stable non-blank internal id.");
        }
    }

    public enum QuarryRoadAction {
        LOST_CARGO("lost_cargo", 1 << 0),
        MEADOW_VIPER("meadow_viper", 1 << 1),
        BROKEN_ROAD_MARKER("broken_road_marker", 1 << 2),
        R01_GATHERING_NODE("r01_gathering_node", 1 << 3),
        ROADSIDE_TROUBLE("roadside_trouble", 1 << 4);

        private final String id;
        private final int mask;

        QuarryRoadAction(String id, int mask) {
            this.id = id;
            this.mask = mask;
        }

        public String id() {
            return id;
        }

        public int mask() {
            return mask;
        }
    }

    public enum RegalhartClue {
        ANTLER_SCORING("antler_scoring", 1 << 0),
        HOOF_FURROWS("hoof_furrows", 1 << 1),
        CROWN_MARKER_DAMAGE("crown_marker_damage", 1 << 2);

        private final String id;
        private final int mask;

        RegalhartClue(String id, int mask) {
            this.id = id;
            this.mask = mask;
        }

        public String id() {
            return id;
        }

        public int mask() {
            return mask;
        }
    }

    public record OpeningState(
            R01MainStage mainStage,
            boolean firstShrineActivated,
            boolean firstRootClassSelected,
            boolean starterPackageClaimed,
            int quarryRoadActionBits,
            Map<String, String> optionalContractStates,
            Optional<String> trailStagState,
            boolean trailStagUnlocked,
            int regalhartCluesSeenBits,
            boolean regalhartDiscovered,
            boolean postQuarryBriefingSeen,
            boolean act1WesternRelayLeadKnown,
            boolean act1WhitecrestStationLeadKnown,
            boolean dodgeHintSeen,
            boolean dodgeUsedOnce
    ) {
        public static final Codec<OpeningState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        R01MainStage.CODEC.fieldOf("r01_main_stage").forGetter(OpeningState::mainStage),
                        Codec.BOOL.fieldOf("first_shrine_activated").forGetter(OpeningState::firstShrineActivated),
                        Codec.BOOL.fieldOf("first_root_class_selected").forGetter(OpeningState::firstRootClassSelected),
                        Codec.BOOL.fieldOf("starter_package_claimed").forGetter(OpeningState::starterPackageClaimed),
                        Codec.intRange(0, 0b1_1111).fieldOf("quarry_road_action_bits").forGetter(OpeningState::quarryRoadActionBits),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .fieldOf("optional_contract_states")
                                .forGetter(OpeningState::optionalContractStates),
                        Codec.STRING.optionalFieldOf("trail_stag_state").forGetter(OpeningState::trailStagState),
                        Codec.BOOL.fieldOf("trail_stag_unlocked").forGetter(OpeningState::trailStagUnlocked),
                        Codec.intRange(0, 0b111).fieldOf("regalhart_clues_seen_bits").forGetter(OpeningState::regalhartCluesSeenBits),
                        Codec.BOOL.fieldOf("regalhart_discovered").forGetter(OpeningState::regalhartDiscovered),
                        Codec.BOOL.fieldOf("post_quarry_briefing_seen").forGetter(OpeningState::postQuarryBriefingSeen),
                        Codec.BOOL.fieldOf("act1_western_relay_lead_known").forGetter(OpeningState::act1WesternRelayLeadKnown),
                        Codec.BOOL.fieldOf("act1_whitecrest_station_lead_known").forGetter(OpeningState::act1WhitecrestStationLeadKnown),
                        Codec.BOOL.fieldOf("r01_dodge_hint_seen").forGetter(OpeningState::dodgeHintSeen),
                        Codec.BOOL.fieldOf("r01_dodge_used_once").forGetter(OpeningState::dodgeUsedOnce)
                ).apply(instance, OpeningState::new)
        );

        public OpeningState {
            Objects.requireNonNull(mainStage, "mainStage");
            optionalContractStates = immutableMap(optionalContractStates, "optionalContractStates");
            trailStagState = Objects.requireNonNull(trailStagState, "trailStagState");
            trailStagState.ifPresent(value -> requireStableId(value, "trailStagState"));
            if ((quarryRoadActionBits & ~0b1_1111) != 0) {
                throw new IllegalArgumentException("quarryRoadActionBits contains unknown categories.");
            }
            if ((regalhartCluesSeenBits & ~0b111) != 0) {
                throw new IllegalArgumentException("regalhartCluesSeenBits contains unknown clues.");
            }
        }

        public static OpeningState initial() {
            return new OpeningState(
                    R01MainStage.ARRIVAL_ROAD,
                    false,
                    false,
                    false,
                    0,
                    Map.of(),
                    Optional.empty(),
                    false,
                    0,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }

        public OpeningState withMainStage(R01MainStage stage) {
            R01MainStage next = R01MainStage.furthest(mainStage, Objects.requireNonNull(stage, "stage"));
            if (next == mainStage) return this;
            return copy(next, firstShrineActivated, firstRootClassSelected, starterPackageClaimed,
                    quarryRoadActionBits, regalhartCluesSeenBits, regalhartDiscovered,
                    dodgeHintSeen, dodgeUsedOnce);
        }

        public OpeningState withFirstShrineActivated() {
            if (firstShrineActivated) return this;
            return copy(mainStage, true, firstRootClassSelected, starterPackageClaimed,
                    quarryRoadActionBits, regalhartCluesSeenBits, regalhartDiscovered,
                    dodgeHintSeen, dodgeUsedOnce);
        }

        public OpeningState withFirstRootClassSelected() {
            if (firstRootClassSelected) return this;
            return copy(mainStage, firstShrineActivated, true, starterPackageClaimed,
                    quarryRoadActionBits, regalhartCluesSeenBits, regalhartDiscovered,
                    dodgeHintSeen, dodgeUsedOnce);
        }

        public OpeningState withStarterPackageClaimed() {
            if (starterPackageClaimed) return this;
            return copy(mainStage, firstShrineActivated, firstRootClassSelected, true,
                    quarryRoadActionBits, regalhartCluesSeenBits, regalhartDiscovered,
                    dodgeHintSeen, dodgeUsedOnce);
        }

        public OpeningState withQuarryRoadActionBits(int bits) {
            if (bits == quarryRoadActionBits) return this;
            return copy(mainStage, firstShrineActivated, firstRootClassSelected, starterPackageClaimed,
                    bits, regalhartCluesSeenBits, regalhartDiscovered,
                    dodgeHintSeen, dodgeUsedOnce);
        }

        public OpeningState withRegalhartCluesSeenBits(int bits) {
            if (bits == regalhartCluesSeenBits) return this;
            return copy(mainStage, firstShrineActivated, firstRootClassSelected, starterPackageClaimed,
                    quarryRoadActionBits, bits, regalhartDiscovered,
                    dodgeHintSeen, dodgeUsedOnce);
        }

        public OpeningState withRegalhartDiscovered() {
            if (regalhartDiscovered) return this;
            return copy(mainStage, firstShrineActivated, firstRootClassSelected, starterPackageClaimed,
                    quarryRoadActionBits, regalhartCluesSeenBits, true,
                    dodgeHintSeen, dodgeUsedOnce);
        }

        public OpeningState withDodgeHintSeen() {
            if (dodgeHintSeen) return this;
            return copy(mainStage, firstShrineActivated, firstRootClassSelected, starterPackageClaimed,
                    quarryRoadActionBits, regalhartCluesSeenBits, regalhartDiscovered,
                    true, dodgeUsedOnce);
        }

        public OpeningState withDodgeUsedOnce() {
            if (dodgeUsedOnce) return this;
            return copy(mainStage, firstShrineActivated, firstRootClassSelected, starterPackageClaimed,
                    quarryRoadActionBits, regalhartCluesSeenBits, regalhartDiscovered,
                    dodgeHintSeen, true);
        }

        private OpeningState copy(
                R01MainStage stage,
                boolean shrine,
                boolean rootClass,
                boolean starter,
                int roadBits,
                int clueBits,
                boolean regalhart,
                boolean dodgeHint,
                boolean dodgeUsed
        ) {
            return new OpeningState(
                    stage,
                    shrine,
                    rootClass,
                    starter,
                    roadBits,
                    optionalContractStates,
                    trailStagState,
                    trailStagUnlocked,
                    clueBits,
                    regalhart,
                    postQuarryBriefingSeen,
                    act1WesternRelayLeadKnown,
                    act1WhitecrestStationLeadKnown,
                    dodgeHint,
                    dodgeUsed
            );
        }
    }

    public record QuarryState(
            boolean discovered,
            boolean relayEvidenceSeen,
            boolean firstClear,
            boolean firstClearRewardClaimed,
            boolean rewardChoicePending,
            boolean waystoneDiscovered,
            boolean waystoneActivated,
            long runId,
            Optional<String> runState,
            boolean liftOpen
    ) {
        public static final Codec<QuarryState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("quarry_discovered").forGetter(QuarryState::discovered),
                        Codec.BOOL.fieldOf("quarry_relay_evidence_seen").forGetter(QuarryState::relayEvidenceSeen),
                        Codec.BOOL.fieldOf("quarry_first_clear").forGetter(QuarryState::firstClear),
                        Codec.BOOL.fieldOf("first_clear_reward_claimed").forGetter(QuarryState::firstClearRewardClaimed),
                        Codec.BOOL.fieldOf("reward_choice_pending").forGetter(QuarryState::rewardChoicePending),
                        Codec.BOOL.fieldOf("r01_quarry_waystone_discovered").forGetter(QuarryState::waystoneDiscovered),
                        Codec.BOOL.fieldOf("r01_quarry_waystone_activated").forGetter(QuarryState::waystoneActivated),
                        Codec.LONG.fieldOf("r01_quarry_run_id").forGetter(QuarryState::runId),
                        Codec.STRING.optionalFieldOf("r01_quarry_run_state").forGetter(QuarryState::runState),
                        Codec.BOOL.fieldOf("r01_quarry_lift_open").forGetter(QuarryState::liftOpen)
                ).apply(instance, QuarryState::new)
        );

        public QuarryState {
            if (runId < 0L) {
                throw new IllegalArgumentException("runId must be non-negative.");
            }
            runState = Objects.requireNonNull(runState, "runState");
            runState.ifPresent(value -> requireStableId(value, "runState"));
            if (waystoneActivated && !waystoneDiscovered) {
                throw new IllegalArgumentException("Activated Quarry Waystone must also be discovered.");
            }
            if (firstClearRewardClaimed && !firstClear) {
                throw new IllegalArgumentException("First-clear reward cannot be claimed before first clear.");
            }
            if (rewardChoicePending && !firstClear) {
                throw new IllegalArgumentException("Reward choice cannot be pending before first clear.");
            }
        }

        public static QuarryState initial() {
            return new QuarryState(
                    false, false, false, false, false,
                    false, false, 0L, Optional.empty(), false
            );
        }

        public QuarryState withDiscovered() {
            if (discovered) return this;
            return new QuarryState(true, relayEvidenceSeen, firstClear, firstClearRewardClaimed,
                    rewardChoicePending, waystoneDiscovered, waystoneActivated, runId, runState, liftOpen);
        }

        public QuarryState withWaystoneDiscovered() {
            if (waystoneDiscovered) return this;
            return new QuarryState(discovered, relayEvidenceSeen, firstClear, firstClearRewardClaimed,
                    rewardChoicePending, true, waystoneActivated, runId, runState, liftOpen);
        }

        public QuarryState withWaystoneActivated() {
            if (waystoneActivated) return this;
            return new QuarryState(discovered, relayEvidenceSeen, firstClear, firstClearRewardClaimed,
                    rewardChoicePending, true, true, runId, runState, liftOpen);
        }

        public QuarryState withRun(long nextRunId, Optional<String> nextRunState) {
            Objects.requireNonNull(nextRunState, "nextRunState");
            if (runId == nextRunId && runState.equals(nextRunState)) return this;
            return new QuarryState(discovered, relayEvidenceSeen, firstClear, firstClearRewardClaimed,
                    rewardChoicePending, waystoneDiscovered, waystoneActivated,
                    nextRunId, nextRunState, liftOpen);
        }

        public QuarryState withLiftOpen() {
            if (liftOpen) return this;
            return new QuarryState(discovered, relayEvidenceSeen, firstClear, firstClearRewardClaimed,
                    rewardChoicePending, waystoneDiscovered, waystoneActivated, runId, runState, true);
        }

        public QuarryState withRelayEvidenceSeen() {
            if (relayEvidenceSeen) return this;
            return new QuarryState(discovered, true, firstClear, firstClearRewardClaimed,
                    rewardChoicePending, waystoneDiscovered, waystoneActivated, runId, runState, liftOpen);
        }

        public QuarryState withFirstClear() {
            if (firstClear) return this;
            return new QuarryState(discovered, relayEvidenceSeen, true, firstClearRewardClaimed,
                    rewardChoicePending, waystoneDiscovered, waystoneActivated, runId, runState, liftOpen);
        }

        public QuarryState withRewardChoicePending() {
            if (rewardChoicePending) return this;
            if (!firstClear) {
                throw new IllegalStateException("Cannot pend Earthloong reward choice before first clear.");
            }
            return new QuarryState(discovered, relayEvidenceSeen, firstClear, firstClearRewardClaimed,
                    true, waystoneDiscovered, waystoneActivated, runId, runState, liftOpen);
        }
    }

    public record WorldLoopState(
            RepeatAndCampState repeatAndCamp,
            PropertyProfessionState propertyProfession,
            FishingServiceState fishingService
    ) {
        public static final Codec<WorldLoopState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        RepeatAndCampState.CODEC.fieldOf("repeat_and_camp").forGetter(WorldLoopState::repeatAndCamp),
                        PropertyProfessionState.CODEC.fieldOf("property_and_profession").forGetter(WorldLoopState::propertyProfession),
                        FishingServiceState.CODEC.fieldOf("fishing_and_service").forGetter(WorldLoopState::fishingService)
                ).apply(instance, WorldLoopState::new)
        );

        public WorldLoopState {
            Objects.requireNonNull(repeatAndCamp, "repeatAndCamp");
            Objects.requireNonNull(propertyProfession, "propertyProfession");
            Objects.requireNonNull(fishingService, "fishingService");
        }

        public static WorldLoopState initial() {
            return new WorldLoopState(
                    RepeatAndCampState.initial(),
                    PropertyProfessionState.initial(),
                    FishingServiceState.initial()
            );
        }
    }

    public record RepeatAndCampState(
            long roadsideEventCycle,
            Set<String> roadsideEventParticipation,
            long roadsideEventLastEndActiveTime,
            long regalhartCycle,
            long regalhartLastDefeatActiveTime,
            boolean fieldCampRecipeKnown,
            boolean fieldCampPermanentUnlock
    ) {
        public static final Codec<RepeatAndCampState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("r01_roadside_event_cycle").forGetter(RepeatAndCampState::roadsideEventCycle),
                        STRING_SET_CODEC.fieldOf("r01_roadside_event_participation").forGetter(RepeatAndCampState::roadsideEventParticipation),
                        Codec.LONG.fieldOf("r01_roadside_event_last_end_active_time").forGetter(RepeatAndCampState::roadsideEventLastEndActiveTime),
                        Codec.LONG.fieldOf("r01_regalhart_cycle").forGetter(RepeatAndCampState::regalhartCycle),
                        Codec.LONG.fieldOf("r01_regalhart_last_defeat_active_time").forGetter(RepeatAndCampState::regalhartLastDefeatActiveTime),
                        Codec.BOOL.fieldOf("field_camp_recipe_known").forGetter(RepeatAndCampState::fieldCampRecipeKnown),
                        Codec.BOOL.fieldOf("field_camp_permanent_unlock").forGetter(RepeatAndCampState::fieldCampPermanentUnlock)
                ).apply(instance, RepeatAndCampState::new)
        );

        public RepeatAndCampState {
            if (roadsideEventCycle < 0L || regalhartCycle < 0L) {
                throw new IllegalArgumentException("R01 repeat-cycle indexes must be non-negative.");
            }
            if (roadsideEventLastEndActiveTime < -1L || regalhartLastDefeatActiveTime < -1L) {
                throw new IllegalArgumentException("R01 active-time sentinels must be >= -1.");
            }
            roadsideEventParticipation = immutableSet(
                    roadsideEventParticipation,
                    "roadsideEventParticipation"
            );
        }

        public static RepeatAndCampState initial() {
            return new RepeatAndCampState(0L, Set.of(), -1L, 0L, -1L, false, false);
        }
    }

    public record PropertyProfessionState(
            Set<String> propertyInspectedIds,
            Optional<String> propertyOwnerState,
            Set<String> professionInsightFlags,
            Map<String, Integer> contractReacceptGeneration
    ) {
        public static final Codec<PropertyProfessionState> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                STRING_SET_CODEC.fieldOf("r01_property_inspected").forGetter(PropertyProfessionState::propertyInspectedIds),
                                Codec.STRING.optionalFieldOf("r01_property_owner_state").forGetter(PropertyProfessionState::propertyOwnerState),
                                STRING_SET_CODEC.fieldOf("r01_profession_insight_flags").forGetter(PropertyProfessionState::professionInsightFlags),
                                Codec.unboundedMap(Codec.STRING, Codec.INT)
                                        .fieldOf("r01_contract_reaccept_generation")
                                        .forGetter(PropertyProfessionState::contractReacceptGeneration)
                        ).apply(instance, PropertyProfessionState::new)
                );

        public PropertyProfessionState {
            propertyInspectedIds = immutableSet(propertyInspectedIds, "propertyInspectedIds");
            propertyOwnerState = Objects.requireNonNull(propertyOwnerState, "propertyOwnerState");
            propertyOwnerState.ifPresent(value -> requireStableId(value, "propertyOwnerState"));
            professionInsightFlags = immutableSet(professionInsightFlags, "professionInsightFlags");
            contractReacceptGeneration = immutableMap(
                    contractReacceptGeneration,
                    "contractReacceptGeneration"
            );
            contractReacceptGeneration.values().forEach(
                    value -> requireNonNegative(value, "contractReacceptGeneration")
            );
        }

        public static PropertyProfessionState initial() {
            return new PropertyProfessionState(Set.of(), Optional.empty(), Set.of(), Map.of());
        }
    }

    public record FishingServiceState(
            Map<String, Integer> fishingSpotCycleIndex,
            Map<String, Integer> fishingCatchOrdinal,
            Map<String, String> fishingPendingCandidate,
            Set<String> fishDiscoveryFlags,
            Set<String> serviceFirstUseFlags,
            Set<String> npcAftermathCommentFlags
    ) {
        public static final Codec<FishingServiceState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.unboundedMap(Codec.STRING, Codec.INT)
                                .fieldOf("r01_fishing_spot_cycle_index")
                                .forGetter(FishingServiceState::fishingSpotCycleIndex),
                        Codec.unboundedMap(Codec.STRING, Codec.INT)
                                .fieldOf("r01_fishing_catch_ordinal")
                                .forGetter(FishingServiceState::fishingCatchOrdinal),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .fieldOf("r01_fishing_pending_candidate")
                                .forGetter(FishingServiceState::fishingPendingCandidate),
                        STRING_SET_CODEC.fieldOf("r01_fish_discovery_flags").forGetter(FishingServiceState::fishDiscoveryFlags),
                        STRING_SET_CODEC.fieldOf("r01_service_first_use_flags").forGetter(FishingServiceState::serviceFirstUseFlags),
                        STRING_SET_CODEC.fieldOf("r01_npc_aftermath_comment_flags").forGetter(FishingServiceState::npcAftermathCommentFlags)
                ).apply(instance, FishingServiceState::new)
        );

        public FishingServiceState {
            fishingSpotCycleIndex = immutableMap(fishingSpotCycleIndex, "fishingSpotCycleIndex");
            fishingCatchOrdinal = immutableMap(fishingCatchOrdinal, "fishingCatchOrdinal");
            fishingPendingCandidate = immutableMap(fishingPendingCandidate, "fishingPendingCandidate");
            fishDiscoveryFlags = immutableSet(fishDiscoveryFlags, "fishDiscoveryFlags");
            serviceFirstUseFlags = immutableSet(serviceFirstUseFlags, "serviceFirstUseFlags");
            npcAftermathCommentFlags = immutableSet(
                    npcAftermathCommentFlags,
                    "npcAftermathCommentFlags"
            );
            fishingSpotCycleIndex.values().forEach(
                    value -> requireNonNegative(value, "fishingSpotCycleIndex")
            );
            fishingCatchOrdinal.values().forEach(
                    value -> requireNonNegative(value, "fishingCatchOrdinal")
            );
        }

        public static FishingServiceState initial() {
            return new FishingServiceState(
                    Map.of(), Map.of(), Map.of(), Set.of(), Set.of(), Set.of()
            );
        }
    }

    public record EconomyState(
            long merchantEpochActiveTime,
            long merchantCycleIndex,
            Set<Integer> merchantCycleSoldSlots,
            Set<String> pendingRewardClaimIds
    ) {
        public static final Codec<EconomyState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("r01_merchant_epoch_active_time").forGetter(EconomyState::merchantEpochActiveTime),
                        Codec.LONG.fieldOf("r01_merchant_cycle_index").forGetter(EconomyState::merchantCycleIndex),
                        INT_SET_CODEC.fieldOf("r01_merchant_cycle_sold_slots").forGetter(EconomyState::merchantCycleSoldSlots),
                        STRING_SET_CODEC.fieldOf("r01_pending_reward_claim_ids").forGetter(EconomyState::pendingRewardClaimIds)
                ).apply(instance, EconomyState::new)
        );

        public EconomyState {
            if (merchantEpochActiveTime < 0L || merchantCycleIndex < 0L) {
                throw new IllegalArgumentException("Merchant epoch/cycle must be non-negative.");
            }
            merchantCycleSoldSlots = immutableSet(merchantCycleSoldSlots, "merchantCycleSoldSlots");
            pendingRewardClaimIds = immutableSet(pendingRewardClaimIds, "pendingRewardClaimIds");
            for (int slot : merchantCycleSoldSlots) {
                if (slot < 1 || slot > 5) {
                    throw new IllegalArgumentException("R01 merchant sold slot must be inside 1..5.");
                }
            }
        }

        public static EconomyState initial() {
            return new EconomyState(0L, 0L, Set.of(), Set.of());
        }

        public EconomyState withPendingRewardClaimId(String claimId) {
            requireStableId(claimId, "claimId");
            if (pendingRewardClaimIds.contains(claimId)) return this;
            Set<String> next = new java.util.HashSet<>(pendingRewardClaimIds);
            next.add(claimId);
            return new EconomyState(
                    merchantEpochActiveTime,
                    merchantCycleIndex,
                    merchantCycleSoldSlots,
                    Set.copyOf(next)
            );
        }
    }

    public record TransactionLedger(
            Map<String, String> questStepIds,
            Map<String, Integer> objectiveProgress,
            Set<String> completedStepIds,
            Set<String> choiceFlags,
            Set<String> rewardClaimIds,
            Set<String> firstClearIds,
            Set<String> discoveryFlags
    ) {
        public static final Codec<TransactionLedger> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("quest_step_ids").forGetter(TransactionLedger::questStepIds),
                        Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("objective_progress").forGetter(TransactionLedger::objectiveProgress),
                        STRING_SET_CODEC.fieldOf("completed_step_ids").forGetter(TransactionLedger::completedStepIds),
                        STRING_SET_CODEC.fieldOf("choice_flags").forGetter(TransactionLedger::choiceFlags),
                        STRING_SET_CODEC.fieldOf("reward_claim_ids").forGetter(TransactionLedger::rewardClaimIds),
                        STRING_SET_CODEC.fieldOf("first_clear_ids").forGetter(TransactionLedger::firstClearIds),
                        STRING_SET_CODEC.fieldOf("discovery_flags").forGetter(TransactionLedger::discoveryFlags)
                ).apply(instance, TransactionLedger::new)
        );

        public TransactionLedger {
            questStepIds = immutableMap(questStepIds, "questStepIds");
            objectiveProgress = immutableMap(objectiveProgress, "objectiveProgress");
            completedStepIds = immutableSet(completedStepIds, "completedStepIds");
            choiceFlags = immutableSet(choiceFlags, "choiceFlags");
            rewardClaimIds = immutableSet(rewardClaimIds, "rewardClaimIds");
            firstClearIds = immutableSet(firstClearIds, "firstClearIds");
            discoveryFlags = immutableSet(discoveryFlags, "discoveryFlags");
            objectiveProgress.values().forEach(value -> requireNonNegative(value, "objectiveProgress"));
        }

        public static TransactionLedger initial() {
            return new TransactionLedger(
                    Map.of(), Map.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of()
            );
        }

        public TransactionLedger withQuestStepId(String questId, String stepId) {
            requireStableId(questId, "questId");
            requireStableId(stepId, "stepId");
            if (stepId.equals(questStepIds.get(questId))) return this;
            Map<String, String> next = new java.util.HashMap<>(questStepIds);
            next.put(questId, stepId);
            return new TransactionLedger(next, objectiveProgress, completedStepIds, choiceFlags,
                    rewardClaimIds, firstClearIds, discoveryFlags);
        }

        public TransactionLedger withObjectiveProgress(String objectiveId, int progress) {
            requireStableId(objectiveId, "objectiveId");
            requireNonNegative(progress, "progress");
            Integer current = objectiveProgress.get(objectiveId);
            if (current != null && current >= progress) return this;
            Map<String, Integer> next = new java.util.HashMap<>(objectiveProgress);
            next.put(objectiveId, progress);
            return new TransactionLedger(questStepIds, next, completedStepIds, choiceFlags,
                    rewardClaimIds, firstClearIds, discoveryFlags);
        }

        public TransactionLedger withCompletedStepId(String stepId) {
            return withSetValue(stepId, completedStepIds, SetTarget.COMPLETED_STEP);
        }

        public TransactionLedger withChoiceFlag(String flagId) {
            return withSetValue(flagId, choiceFlags, SetTarget.CHOICE);
        }

        public TransactionLedger withRewardClaimId(String claimId) {
            return withSetValue(claimId, rewardClaimIds, SetTarget.REWARD);
        }

        public TransactionLedger withFirstClearId(String firstClearId) {
            return withSetValue(firstClearId, firstClearIds, SetTarget.FIRST_CLEAR);
        }

        public TransactionLedger withDiscoveryFlag(String discoveryId) {
            return withSetValue(discoveryId, discoveryFlags, SetTarget.DISCOVERY);
        }

        private TransactionLedger withSetValue(
                String id,
                Set<String> source,
                SetTarget target
        ) {
            requireStableId(id, "id");
            if (source.contains(id)) return this;
            Set<String> next = new java.util.HashSet<>(source);
            next.add(id);
            Set<String> immutable = Set.copyOf(next);
            return switch (target) {
                case COMPLETED_STEP -> new TransactionLedger(
                        questStepIds, objectiveProgress, immutable, choiceFlags,
                        rewardClaimIds, firstClearIds, discoveryFlags);
                case CHOICE -> new TransactionLedger(
                        questStepIds, objectiveProgress, completedStepIds, immutable,
                        rewardClaimIds, firstClearIds, discoveryFlags);
                case REWARD -> new TransactionLedger(
                        questStepIds, objectiveProgress, completedStepIds, choiceFlags,
                        immutable, firstClearIds, discoveryFlags);
                case FIRST_CLEAR -> new TransactionLedger(
                        questStepIds, objectiveProgress, completedStepIds, choiceFlags,
                        rewardClaimIds, immutable, discoveryFlags);
                case DISCOVERY -> new TransactionLedger(
                        questStepIds, objectiveProgress, completedStepIds, choiceFlags,
                        rewardClaimIds, firstClearIds, immutable);
            };
        }

        private enum SetTarget {
            COMPLETED_STEP,
            CHOICE,
            REWARD,
            FIRST_CLEAR,
            DISCOVERY
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative.");
        }
    }
}
