package dev.moonseungjun.openworldrpg.progression.r01;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Canon-locked non-spatial encounter composition for the three pre-boss Quarry rooms.
 *
 * <p>Anchor ids are semantic authored slots only. Spatial closure later binds each id to a real
 * Azari coordinate/volume; this class intentionally owns no coordinates and no donor registry ids.</p>
 */
public final class R01QuarryRoomEncounterRules {
    public static final String UPPER_GALLERY_DEEPER_THRESHOLD =
            "openworld_rpg:r01/quarry/upper_gallery/deeper_gallery_threshold";

    private static final List<String> UPPER_FIRST_ANCHORS = List.of(
            "openworld_rpg:r01/quarry/upper_gallery/entry_pair_left",
            "openworld_rpg:r01/quarry/upper_gallery/entry_pair_right"
    );
    private static final List<String> UPPER_SECOND_ANCHORS = List.of(
            "openworld_rpg:r01/quarry/upper_gallery/deeper_1",
            "openworld_rpg:r01/quarry/upper_gallery/deeper_2",
            "openworld_rpg:r01/quarry/upper_gallery/deeper_3",
            "openworld_rpg:r01/quarry/upper_gallery/deeper_4",
            "openworld_rpg:r01/quarry/upper_gallery/deeper_5"
    );
    private static final List<String> HOIST_ANCHORS = List.of(
            "openworld_rpg:r01/quarry/collapsed_hoist/wall_1",
            "openworld_rpg:r01/quarry/collapsed_hoist/wall_2",
            "openworld_rpg:r01/quarry/collapsed_hoist/ceiling_1",
            "openworld_rpg:r01/quarry/collapsed_hoist/added_1",
            "openworld_rpg:r01/quarry/collapsed_hoist/added_2",
            "openworld_rpg:r01/quarry/collapsed_hoist/added_3"
    );
    private static final String ROOT_NATURE_SPIRIT_ANCHOR =
            "openworld_rpg:r01/quarry/root_breached/nature_spirit_elite";

    private R01QuarryRoomEncounterRules() {
    }

    public static SpawnPlan initialPlan(RoomId room, int engagedPlayers) {
        Objects.requireNonNull(room, "room");
        validatePartySize(engagedPlayers);
        return switch (room) {
            case UPPER_GALLERY -> new SpawnPlan(
                    room,
                    Wave.FIRST,
                    ActorRole.CAVE_CENTIPEDE,
                    UPPER_FIRST_ANCHORS,
                    1.0,
                    1.0
            );
            case COLLAPSED_HOIST -> new SpawnPlan(
                    room,
                    Wave.SINGLE,
                    ActorRole.CAVE_CENTIPEDE,
                    HOIST_ANCHORS.subList(0, collapsedHoistCount(engagedPlayers)),
                    1.0,
                    1.0
            );
            case ROOT_BREACHED -> new SpawnPlan(
                    room,
                    Wave.SINGLE,
                    ActorRole.NATURE_SPIRIT,
                    List.of(ROOT_NATURE_SPIRIT_ANCHOR),
                    authoredEliteHpScale(engagedPlayers),
                    authoredElitePoiseScale(engagedPlayers)
            );
        };
    }

    public static SpawnPlan upperGallerySecondWave(int engagedPlayers) {
        validatePartySize(engagedPlayers);
        int count = upperGallerySecondWaveCount(engagedPlayers);
        return new SpawnPlan(
                RoomId.UPPER_GALLERY,
                Wave.SECOND,
                ActorRole.CAVE_CENTIPEDE,
                UPPER_SECOND_ANCHORS.subList(0, count),
                1.0,
                1.0
        );
    }

    public static int upperGallerySecondWaveCount(int engagedPlayers) {
        validatePartySize(engagedPlayers);
        return 2 + (engagedPlayers - 1);
    }

    public static int collapsedHoistCount(int engagedPlayers) {
        validatePartySize(engagedPlayers);
        return 3 + (engagedPlayers - 1);
    }

    public static double authoredEliteHpScale(int engagedPlayers) {
        validatePartySize(engagedPlayers);
        return 1.0 + 0.65 * (engagedPlayers - 1);
    }

    public static double authoredElitePoiseScale(int engagedPlayers) {
        validatePartySize(engagedPlayers);
        return 1.0 + 0.40 * (engagedPlayers - 1);
    }

    private static void validatePartySize(int engagedPlayers) {
        if (engagedPlayers < 1 || engagedPlayers > 4) {
            throw new IllegalArgumentException(
                    "R01 Quarry authored room party size must be inside 1..4."
            );
        }
    }

    public enum RoomId {
        UPPER_GALLERY,
        COLLAPSED_HOIST,
        ROOT_BREACHED;

        public static final Codec<RoomId> CODEC = Codec.STRING.comapFlatMap(
                value -> {
                    try {
                        return DataResult.success(
                                RoomId.valueOf(value.trim().toUpperCase(Locale.ROOT))
                        );
                    } catch (IllegalArgumentException exception) {
                        return DataResult.error(
                                () -> "Unknown R01 Quarry room id: " + value
                        );
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT)
        );
    }

    public enum Wave {
        SINGLE,
        FIRST,
        SECOND
    }

    public enum ActorRole {
        CAVE_CENTIPEDE,
        NATURE_SPIRIT
    }

    public record SpawnPlan(
            RoomId room,
            Wave wave,
            ActorRole actorRole,
            List<String> anchorIds,
            double hpScale,
            double poiseScale
    ) {
        public SpawnPlan {
            Objects.requireNonNull(room, "room");
            Objects.requireNonNull(wave, "wave");
            Objects.requireNonNull(actorRole, "actorRole");
            anchorIds = List.copyOf(Objects.requireNonNull(anchorIds, "anchorIds"));
            if (anchorIds.isEmpty()) {
                throw new IllegalArgumentException("Spawn plan requires at least one anchor.");
            }
            for (String anchorId : anchorIds) {
                if (anchorId == null
                        || anchorId.isBlank()
                        || !anchorId.startsWith("openworld_rpg:")) {
                    throw new IllegalArgumentException("Invalid Quarry authored anchor id.");
                }
            }
            if (!Double.isFinite(hpScale)
                    || hpScale <= 0.0
                    || !Double.isFinite(poiseScale)
                    || poiseScale <= 0.0) {
                throw new IllegalArgumentException("Invalid Quarry encounter scaling.");
            }
        }

        public int actorCount() {
            return anchorIds.size();
        }
    }
}
