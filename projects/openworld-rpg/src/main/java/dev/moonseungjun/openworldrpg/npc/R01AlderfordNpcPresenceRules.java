package dev.moonseungjun.openworldrpg.npc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Canon-locked non-visual Alderford NPC presence, service relevance and pathfinder budget rules. */
public final class R01AlderfordNpcPresenceRules {
    public static final double SERVICE_RELEVANCE_RADIUS_BLOCKS = 12.0;
    public static final long SERVICE_OVERRIDE_RELEASE_TICKS = 20L * 20L;
    public static final int MAX_CORE_PATHFINDERS = 15;
    public static final int WATCH_GUARD_COUNT = 2;
    public static final int AMBIENT_TOWNSFOLK_COUNT = 4;

    public static final String MARA_VENN = "openworld_rpg:npc/mara_venn";
    public static final String ELIAN_ROOK = "openworld_rpg:npc/elian_rook";
    public static final String DAREN_HOLT = "openworld_rpg:npc/daren_holt";
    public static final String LYSA_FEN = "openworld_rpg:npc/lysa_fen";
    public static final String TOMA_REED = "openworld_rpg:npc/toma_reed";
    public static final String BRIN_HALE = "openworld_rpg:npc/brin_hale";
    public static final String NESSA_BELL = "openworld_rpg:npc/nessa_bell";
    public static final String OREN_QUILL = "openworld_rpg:npc/oren_quill";
    public static final String SERA_WREN = "openworld_rpg:npc/sera_wren";
    public static final String ILYAN_VOSS = "openworld_rpg:npc/ilyan_voss";
    public static final String KEST_ARDEN = "openworld_rpg:npc/kest_arden";

    private static final Map<String, String> PRIMARY_SERVICE = Map.of(
            ELIAN_ROOK, "openworld_rpg:service/alderford/wayfarers_hall",
            DAREN_HOLT, "openworld_rpg:service/alderford/holt_forge",
            LYSA_FEN, "openworld_rpg:service/alderford/greenwater_remedies",
            TOMA_REED, "openworld_rpg:service/alderford/fordside_stables",
            BRIN_HALE, "openworld_rpg:service/alderford/copper_kettle",
            NESSA_BELL, "openworld_rpg:service/alderford/market",
            OREN_QUILL, "openworld_rpg:service/alderford/alderford_vault",
            SERA_WREN, "openworld_rpg:service/alderford/route_board"
    );

    private R01AlderfordNpcPresenceRules() {
    }

    public static Optional<String> primaryServiceId(String npcId) {
        requireNpcId(npcId);
        return Optional.ofNullable(PRIMARY_SERVICE.get(npcId));
    }

    /**
     * Minecraft day time 0 is 06:00; therefore 4k/12k/16k map exactly to 10:00/18:00/22:00.
     */
    public static TimeBand timeBand(long dayTime) {
        long tick = Math.floorMod(dayTime, 24000L);
        if (tick < 4000L) return TimeBand.MORNING;
        if (tick < 12000L) return TimeBand.DAY;
        if (tick < 16000L) return TimeBand.EVENING;
        return TimeBand.NIGHT;
    }

    public static AmbientSlot ambientSlot(
            String npcId,
            TimeBand band,
            boolean quarryFirstClear
    ) {
        requireNpcId(npcId);
        Objects.requireNonNull(band, "band");

        if (KEST_ARDEN.equals(npcId)) {
            return AmbientSlot.EVENT_OWNED_ONLY;
        }
        if (MARA_VENN.equals(npcId)) {
            return switch (band) {
                case MORNING -> AmbientSlot.MARA_GATE_ROUTE;
                case DAY -> AmbientSlot.WAYFARERS_HALL;
                case EVENING, NIGHT -> AmbientSlot.MARA_EVENING_NIGHT_INTERIOR;
            };
        }
        if (ELIAN_ROOK.equals(npcId)) {
            return switch (band) {
                case MORNING -> AmbientSlot.ELIAN_HALL_MORNING;
                case DAY -> AmbientSlot.ELIAN_HALL_DAY;
                case EVENING -> AmbientSlot.ELIAN_HALL_EVENING;
                case NIGHT -> AmbientSlot.ELIAN_HALL_NIGHT;
            };
        }
        if (DAREN_HOLT.equals(npcId)) {
            return switch (band) {
                case MORNING, DAY -> AmbientSlot.HOLT_FORGE;
                case EVENING -> AmbientSlot.DAREN_EVENING_FORGE_OR_KETTLE;
                case NIGHT -> AmbientSlot.HOLT_FORGE_INTERIOR;
            };
        }
        if (LYSA_FEN.equals(npcId)) {
            return switch (band) {
                case MORNING -> AmbientSlot.LYSA_RIVER_HERB_GARDEN_EDGE;
                case DAY -> AmbientSlot.GREENWATER_REMEDIES;
                case EVENING, NIGHT -> AmbientSlot.GREENWATER_REMEDIES_INTERIOR;
            };
        }
        if (TOMA_REED.equals(npcId)) {
            return switch (band) {
                case MORNING, DAY -> AmbientSlot.STABLE_PADDOCK;
                case EVENING, NIGHT -> AmbientSlot.STABLE_INTERIOR;
            };
        }
        if (BRIN_HALE.equals(npcId)) {
            return AmbientSlot.COPPER_KETTLE;
        }
        if (NESSA_BELL.equals(npcId)) {
            return band == TimeBand.DAY
                    ? AmbientSlot.MARKET_STALL
                    : AmbientSlot.MARKET_STOCK_COVERED;
        }
        if (OREN_QUILL.equals(npcId)) {
            return AmbientSlot.ALDERFORD_VAULT;
        }
        if (SERA_WREN.equals(npcId)) {
            return switch (band) {
                case MORNING -> AmbientSlot.ROUTE_BOARD;
                case DAY -> AmbientSlot.SERA_RIVERWOOD_ROAD_EDGE;
                case EVENING -> AmbientSlot.SERA_EVENING_KETTLE_OR_BOARD;
                case NIGHT -> AmbientSlot.SERA_NIGHT_NO_FORCED_FIELD_PATROL;
            };
        }
        if (ILYAN_VOSS.equals(npcId)) {
            if (!quarryFirstClear) {
                return AmbientSlot.ILYAN_PRE_CLEAR_KETTLE_GUEST;
            }
            return switch (band) {
                case DAY -> AmbientSlot.WAYFARERS_HALL;
                case EVENING -> AmbientSlot.COPPER_KETTLE;
                case MORNING, NIGHT -> AmbientSlot.UNSPECIFIED_POST_CLEAR_AMBIENT;
            };
        }
        throw new IllegalArgumentException("Unknown R01 named NPC: " + npcId);
    }

    public static PresenceUpdate updateNamedPresence(
            PresenceLatch previous,
            PresenceContext context
    ) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(context, "context");
        requireNpcId(context.npcId());
        if (context.serverTick() < 0L) {
            throw new IllegalArgumentException("serverTick must be non-negative.");
        }

        boolean hasService = primaryServiceId(context.npcId()).isPresent();
        boolean relevantNow = hasService
                && context.serviceLegallyAvailable()
                && (context.playerWithinServiceRadius()
                    || context.questTurnInRelevant());

        long releaseAt = previous.releaseAtTick();
        boolean latched = previous.serviceOverrideLatched();
        if (relevantNow) {
            latched = true;
            releaseAt = Math.addExact(
                    context.serverTick(),
                    SERVICE_OVERRIDE_RELEASE_TICKS
            );
        } else if (latched && context.serverTick() >= releaseAt) {
            latched = false;
        }

        PresenceLatch next = new PresenceLatch(latched, releaseAt);

        if (context.storySceneOverride()) {
            return new PresenceUpdate(
                    next,
                    PresenceMode.STORY_OR_EVENT_SCENE,
                    ActivationPriority.REQUIRED_NAMED,
                    ambientSlot(
                            context.npcId(),
                            context.timeBand(),
                            context.quarryFirstClear()
                    )
            );
        }

        if (latched) {
            return new PresenceUpdate(
                    next,
                    PresenceMode.PRIMARY_SERVICE,
                    ActivationPriority.REQUIRED_NAMED,
                    ambientSlot(
                            context.npcId(),
                            context.timeBand(),
                            context.quarryFirstClear()
                    )
            );
        }

        AmbientSlot ambient = ambientSlot(
                context.npcId(),
                context.timeBand(),
                context.quarryFirstClear()
        );
        if (ambient == AmbientSlot.EVENT_OWNED_ONLY) {
            return new PresenceUpdate(
                    next,
                    PresenceMode.EVENT_OWNED_ONLY,
                    ActivationPriority.INACTIVE,
                    ambient
            );
        }
        return new PresenceUpdate(
                next,
                PresenceMode.AMBIENT_SCHEDULE,
                ActivationPriority.NAMED_AMBIENT,
                ambient
        );
    }

    /**
     * Picks active humanoid pathfinders using the exact R01 relevance order.
     * Stable input order is preserved inside the same priority bucket.
     */
    public static List<PathfinderCandidate> selectCorePathfinders(
            List<PathfinderCandidate> candidates
    ) {
        Objects.requireNonNull(candidates, "candidates");
        List<IndexedCandidate> eligible = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            PathfinderCandidate candidate = Objects.requireNonNull(
                    candidates.get(index),
                    "candidate"
            );
            if (candidate.priority() == ActivationPriority.INACTIVE) {
                continue;
            }
            eligible.add(new IndexedCandidate(index, candidate));
        }
        eligible.sort(
                Comparator
                        .comparingInt((IndexedCandidate value) ->
                                value.candidate().priority().rank())
                        .thenComparingInt(IndexedCandidate::index)
        );
        return eligible.stream()
                .limit(MAX_CORE_PATHFINDERS)
                .map(IndexedCandidate::candidate)
                .toList();
    }

    public static boolean insideServiceRelevanceRadius(
            double squaredDistanceBlocks
    ) {
        if (!Double.isFinite(squaredDistanceBlocks)
                || squaredDistanceBlocks < 0.0) {
            return false;
        }
        return squaredDistanceBlocks
                <= SERVICE_RELEVANCE_RADIUS_BLOCKS
                * SERVICE_RELEVANCE_RADIUS_BLOCKS;
    }

    private static void requireNpcId(String npcId) {
        if (npcId == null
                || npcId.isBlank()
                || npcId.indexOf(':') <= 0) {
            throw new IllegalArgumentException(
                    "Expected stable R01 named NPC id."
            );
        }
    }

    public enum TimeBand {
        MORNING,
        DAY,
        EVENING,
        NIGHT
    }

    /**
     * These are semantic authored slots, not production world coordinates.
     * Ambiguous canon remains ambiguous rather than picking a location here.
     */
    public enum AmbientSlot {
        MARA_GATE_ROUTE,
        MARA_EVENING_NIGHT_INTERIOR,
        WAYFARERS_HALL,
        ELIAN_HALL_MORNING,
        ELIAN_HALL_DAY,
        ELIAN_HALL_EVENING,
        ELIAN_HALL_NIGHT,
        HOLT_FORGE,
        DAREN_EVENING_FORGE_OR_KETTLE,
        HOLT_FORGE_INTERIOR,
        LYSA_RIVER_HERB_GARDEN_EDGE,
        GREENWATER_REMEDIES,
        GREENWATER_REMEDIES_INTERIOR,
        STABLE_PADDOCK,
        STABLE_INTERIOR,
        COPPER_KETTLE,
        MARKET_STALL,
        MARKET_STOCK_COVERED,
        ALDERFORD_VAULT,
        ROUTE_BOARD,
        SERA_RIVERWOOD_ROAD_EDGE,
        SERA_EVENING_KETTLE_OR_BOARD,
        SERA_NIGHT_NO_FORCED_FIELD_PATROL,
        ILYAN_PRE_CLEAR_KETTLE_GUEST,
        UNSPECIFIED_POST_CLEAR_AMBIENT,
        EVENT_OWNED_ONLY
    }

    public enum PresenceMode {
        PRIMARY_SERVICE,
        STORY_OR_EVENT_SCENE,
        AMBIENT_SCHEDULE,
        EVENT_OWNED_ONLY
    }

    public enum ActivationPriority {
        REQUIRED_NAMED(0),
        WATCH_GUARD(1),
        NAMED_AMBIENT(2),
        UNNAMED_TOWNSFOLK(3),
        INACTIVE(4);

        private final int rank;

        ActivationPriority(int rank) {
            this.rank = rank;
        }

        public int rank() {
            return rank;
        }
    }

    public record PresenceLatch(
            boolean serviceOverrideLatched,
            long releaseAtTick
    ) {
        public PresenceLatch {
            if (releaseAtTick < 0L) {
                throw new IllegalArgumentException(
                        "releaseAtTick must be non-negative."
                );
            }
        }

        public static PresenceLatch initial() {
            return new PresenceLatch(false, 0L);
        }
    }

    public record PresenceContext(
            String npcId,
            long serverTick,
            TimeBand timeBand,
            boolean quarryFirstClear,
            boolean serviceLegallyAvailable,
            boolean playerWithinServiceRadius,
            boolean questTurnInRelevant,
            boolean storySceneOverride
    ) {
        public PresenceContext {
            requireNpcId(npcId);
            Objects.requireNonNull(timeBand, "timeBand");
        }
    }

    public record PresenceUpdate(
            PresenceLatch state,
            PresenceMode mode,
            ActivationPriority priority,
            AmbientSlot ambientFallback
    ) {
        public PresenceUpdate {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(priority, "priority");
            Objects.requireNonNull(ambientFallback, "ambientFallback");
        }
    }

    public record PathfinderCandidate(
            String actorId,
            ActivationPriority priority
    ) {
        public PathfinderCandidate {
            if (actorId == null || actorId.isBlank()) {
                throw new IllegalArgumentException("actorId must not be blank.");
            }
            Objects.requireNonNull(priority, "priority");
        }
    }

    private record IndexedCandidate(
            int index,
            PathfinderCandidate candidate
    ) {
    }
}
