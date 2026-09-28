package dev.moonseungjun.openworldrpg.world.spatial;

import com.google.gson.annotations.SerializedName;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules.RoomId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Runtime-binding contract between the authored R01 Quarry encounter roles and final Azari
 * production geometry.
 *
 * <p>The Pass-3 review shells are evidence/provenance only. They are never exposed as runtime room
 * bounds. Final room volumes, encounter spawn sockets and the Upper Gallery second-wave trigger
 * must be authored separately and promoted through the ordinary spatial production gate.</p>
 */
public record R01QuarrySpatialBindingData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        @SerializedName("map_build") String mapBuild,
        List<RoomBinding> rooms,
        StageBinding relay,
        @SerializedName("earthloong_arena") StageBinding earthloongArena
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CANONICAL_ID =
            "openworld_rpg:r01/quarry_runtime_bindings";
    public static final String CANONICAL_MAP_BUILD = "AzariNEW4252026";

    public static final String UPPER_REVIEW =
            "openworld_rpg:r01/quarry/upper_gallery_review";
    public static final String UPPER_RUNTIME =
            "openworld_rpg:r01/quarry/upper_gallery/runtime_volume";
    public static final String HOIST_REVIEW =
            "openworld_rpg:r01/quarry/collapsed_hoist_review";
    public static final String HOIST_RUNTIME =
            "openworld_rpg:r01/quarry/collapsed_hoist/runtime_volume";
    public static final String ROOT_REVIEW =
            "openworld_rpg:r01/quarry/root_breached_review";
    public static final String ROOT_RUNTIME =
            "openworld_rpg:r01/quarry/root_breached/runtime_volume";
    public static final String RELAY_REVIEW =
            "openworld_rpg:r01/quarry/relay_gallery_review";
    public static final String RELAY_RUNTIME =
            "openworld_rpg:r01/quarry/relay_gallery/runtime_volume";
    public static final String EARTHLOONG_REVIEW =
            "openworld_rpg:r01/quarry/earthloong_chamber_review";
    public static final String EARTHLOONG_RUNTIME =
            "openworld_rpg:r01/quarry/earthloong/runtime_arena";

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "candidate",
            "client_verified",
            "production"
    );

    public R01QuarrySpatialBindingData {
        rooms = List.copyOf(Objects.requireNonNull(rooms, "rooms"));
        Objects.requireNonNull(relay, "relay");
        Objects.requireNonNull(earthloongArena, "earthloongArena");
    }

    public Optional<RoomBinding> room(RoomId room) {
        Objects.requireNonNull(room, "room");
        return rooms.stream()
                .filter(binding -> binding.room() == room)
                .findFirst();
    }

    public Optional<ProductionRoomBinding> productionRoom(
            RoomId room,
            R01SpatialBindingData spatial
    ) {
        Objects.requireNonNull(room, "room");
        Objects.requireNonNull(spatial, "spatial");
        RoomBinding binding = room(room).orElseThrow();
        if (!binding.production()) {
            return Optional.empty();
        }

        var runtimeVolume = spatial.productionVolume(binding.runtimeVolumeId());
        if (runtimeVolume.isEmpty()) {
            return Optional.empty();
        }

        List<R01SpatialBindingData.Anchor> sockets = new ArrayList<>();
        for (String socketId : binding.spawnSocketIds()) {
            var anchor = spatial.productionAnchor(socketId);
            if (anchor.isEmpty()) {
                return Optional.empty();
            }
            sockets.add(anchor.orElseThrow());
        }

        Optional<R01SpatialBindingData.Volume> secondWaveTrigger = Optional.empty();
        if (binding.secondWaveTriggerVolumeId() != null) {
            secondWaveTrigger =
                    spatial.productionVolume(binding.secondWaveTriggerVolumeId());
            if (secondWaveTrigger.isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of(new ProductionRoomBinding(
                binding,
                runtimeVolume.orElseThrow(),
                sockets,
                secondWaveTrigger
        ));
    }

    public Optional<ProductionStageBinding> productionRelay(
            R01SpatialBindingData spatial
    ) {
        return productionStage(relay, spatial);
    }

    public Optional<ProductionStageBinding> productionEarthloongArena(
            R01SpatialBindingData spatial
    ) {
        return productionStage(earthloongArena, spatial);
    }

    public boolean productionReady(R01SpatialBindingData spatial) {
        Objects.requireNonNull(spatial, "spatial");
        for (RoomId room : RoomId.values()) {
            if (productionRoom(room, spatial).isEmpty()) {
                return false;
            }
        }
        return productionRelay(spatial).isPresent()
                && productionEarthloongArena(spatial).isPresent();
    }

    private static Optional<ProductionStageBinding> productionStage(
            StageBinding stage,
            R01SpatialBindingData spatial
    ) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(spatial, "spatial");
        if (!stage.production()) {
            return Optional.empty();
        }
        return spatial.productionVolume(stage.runtimeVolumeId())
                .map(volume -> new ProductionStageBinding(stage, volume));
    }

    public record RoomBinding(
            String id,
            String status,
            @SerializedName("room") String roomId,
            @SerializedName("source_review_volume_id") String sourceReviewVolumeId,
            @SerializedName("runtime_volume_id") String runtimeVolumeId,
            @SerializedName("spawn_socket_ids") List<String> spawnSocketIds,
            @SerializedName("second_wave_trigger_volume_id")
            String secondWaveTriggerVolumeId
    ) {
        public RoomBinding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(roomId, "roomId");
            Objects.requireNonNull(sourceReviewVolumeId, "sourceReviewVolumeId");
            Objects.requireNonNull(runtimeVolumeId, "runtimeVolumeId");
            spawnSocketIds = List.copyOf(
                    Objects.requireNonNull(spawnSocketIds, "spawnSocketIds")
            );
        }

        public RoomId room() {
            return RoomId.valueOf(roomId.trim().toUpperCase(Locale.ROOT));
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public record StageBinding(
            String id,
            String status,
            @SerializedName("source_review_volume_id") String sourceReviewVolumeId,
            @SerializedName("runtime_volume_id") String runtimeVolumeId,
            String role
    ) {
        public StageBinding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(sourceReviewVolumeId, "sourceReviewVolumeId");
            Objects.requireNonNull(runtimeVolumeId, "runtimeVolumeId");
            Objects.requireNonNull(role, "role");
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public record ProductionRoomBinding(
            RoomBinding contract,
            R01SpatialBindingData.Volume runtimeVolume,
            List<R01SpatialBindingData.Anchor> spawnSockets,
            Optional<R01SpatialBindingData.Volume> secondWaveTrigger
    ) {
        public ProductionRoomBinding {
            Objects.requireNonNull(contract, "contract");
            Objects.requireNonNull(runtimeVolume, "runtimeVolume");
            spawnSockets = List.copyOf(
                    Objects.requireNonNull(spawnSockets, "spawnSockets")
            );
            secondWaveTrigger = Objects.requireNonNull(
                    secondWaveTrigger,
                    "secondWaveTrigger"
            );
        }
    }

    public record ProductionStageBinding(
            StageBinding contract,
            R01SpatialBindingData.Volume runtimeVolume
    ) {
        public ProductionStageBinding {
            Objects.requireNonNull(contract, "contract");
            Objects.requireNonNull(runtimeVolume, "runtimeVolume");
        }
    }

    public static void validate(R01QuarrySpatialBindingData data) {
        Objects.requireNonNull(data, "data");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported R01 Quarry spatial-binding schema: "
                            + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 Quarry spatial-binding id: " + data.id()
            );
        }
        if (!CANONICAL_MAP_BUILD.equals(data.mapBuild())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 Quarry map build: " + data.mapBuild()
            );
        }
        if (data.rooms().size() != RoomId.values().length) {
            throw new IllegalArgumentException(
                    "R01 Quarry requires exactly three pre-boss room bindings."
            );
        }

        Set<RoomId> seenRooms = new HashSet<>();
        Set<String> seenIds = new HashSet<>();
        for (RoomBinding binding : data.rooms()) {
            requireNamespacedId(binding.id(), "room binding");
            requireStatus(binding.status(), binding.id());
            RoomId room;
            try {
                room = binding.room();
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unknown R01 Quarry room binding: " + binding.roomId(),
                        exception
                );
            }
            if (!seenRooms.add(room)) {
                throw new IllegalArgumentException(
                        "Duplicate R01 Quarry room binding: " + room
                );
            }
            if (!seenIds.add(binding.id())) {
                throw new IllegalArgumentException(
                        "Duplicate R01 Quarry binding id: " + binding.id()
                );
            }

            String expectedReview = expectedReviewVolume(room);
            String expectedRuntime = expectedRuntimeVolume(room);
            if (!expectedReview.equals(binding.sourceReviewVolumeId())) {
                throw new IllegalArgumentException(
                        "R01 Quarry source review volume mismatch for " + room
                );
            }
            if (!expectedRuntime.equals(binding.runtimeVolumeId())) {
                throw new IllegalArgumentException(
                        "R01 Quarry runtime volume id mismatch for " + room
                );
            }
            if (binding.sourceReviewVolumeId().equals(binding.runtimeVolumeId())) {
                throw new IllegalArgumentException(
                        "R01 Quarry review shell cannot be reused as final runtime volume: "
                                + binding.id()
                );
            }

            Set<String> actualSockets = new HashSet<>(binding.spawnSocketIds());
            if (actualSockets.size() != binding.spawnSocketIds().size()) {
                throw new IllegalArgumentException(
                        "Duplicate R01 Quarry spawn socket id in " + binding.id()
                );
            }
            Set<String> expectedSockets = expectedSpawnSockets(room);
            if (!actualSockets.equals(expectedSockets)) {
                throw new IllegalArgumentException(
                        "R01 Quarry spawn-socket contract mismatch for " + room
                                + "; expected=" + expectedSockets
                                + ", actual=" + actualSockets
                );
            }
            binding.spawnSocketIds().forEach(
                    id -> requireNamespacedId(id, "spawn socket")
            );

            if (room == RoomId.UPPER_GALLERY) {
                if (!R01QuarryRoomEncounterRules.UPPER_GALLERY_DEEPER_THRESHOLD
                        .equals(binding.secondWaveTriggerVolumeId())) {
                    throw new IllegalArgumentException(
                            "Upper Gallery must preserve the authored deeper-gallery threshold id."
                    );
                }
            } else if (binding.secondWaveTriggerVolumeId() != null) {
                throw new IllegalArgumentException(
                        "Only Upper Gallery owns a second-wave trigger volume."
                );
            }
        }

        if (!seenRooms.equals(Set.of(RoomId.values()))) {
            throw new IllegalArgumentException(
                    "R01 Quarry room binding set is incomplete: " + seenRooms
            );
        }

        validateStage(
                data.relay(),
                "openworld_rpg:r01/quarry/relay_gallery",
                RELAY_REVIEW,
                RELAY_RUNTIME
        );
        validateStage(
                data.earthloongArena(),
                "openworld_rpg:r01/quarry/earthloong_arena",
                EARTHLOONG_REVIEW,
                EARTHLOONG_RUNTIME
        );
    }

    public static void validateAgainstSpatial(
            R01QuarrySpatialBindingData data,
            R01SpatialBindingData spatial
    ) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(spatial, "spatial");

        for (RoomBinding binding : data.rooms()) {
            var review = spatial.volume(binding.sourceReviewVolumeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Missing R01 Quarry source review volume: "
                                    + binding.sourceReviewVolumeId()
                    ));
            if (!"natural_seam".equals(review.reviewMode())) {
                throw new IllegalArgumentException(
                        "Unexpected R01 Quarry review mode for " + binding.id()
                );
            }
            validateProductionRoom(binding, spatial);
        }

        validateStageAgainstSpatial(data.relay(), spatial, "transition_probe");
        validateStageAgainstSpatial(
                data.earthloongArena(),
                spatial,
                "solid_carve_probe"
        );
    }

    private static void validateProductionRoom(
            RoomBinding binding,
            R01SpatialBindingData spatial
    ) {
        if (!binding.production()) {
            return;
        }
        if (spatial.productionVolume(binding.runtimeVolumeId()).isEmpty()) {
            throw new IllegalArgumentException(
                    "Production R01 Quarry room requires a production runtime volume: "
                            + binding.id()
            );
        }
        for (String socketId : binding.spawnSocketIds()) {
            if (spatial.productionAnchor(socketId).isEmpty()) {
                throw new IllegalArgumentException(
                        "Production R01 Quarry room requires production spawn socket: "
                                + socketId
                );
            }
        }
        if (binding.secondWaveTriggerVolumeId() != null
                && spatial.productionVolume(
                        binding.secondWaveTriggerVolumeId()
                ).isEmpty()) {
            throw new IllegalArgumentException(
                    "Production Upper Gallery requires a production second-wave trigger volume."
            );
        }
    }

    private static void validateStageAgainstSpatial(
            StageBinding stage,
            R01SpatialBindingData spatial,
            String expectedReviewMode
    ) {
        var review = spatial.volume(stage.sourceReviewVolumeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing R01 Quarry source review volume: "
                                + stage.sourceReviewVolumeId()
                ));
        if (!expectedReviewMode.equals(review.reviewMode())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 Quarry stage review mode: " + stage.id()
            );
        }
        if (stage.production()
                && spatial.productionVolume(stage.runtimeVolumeId()).isEmpty()) {
            throw new IllegalArgumentException(
                    "Production R01 Quarry stage requires production runtime volume: "
                            + stage.id()
            );
        }
    }

    private static void validateStage(
            StageBinding stage,
            String expectedId,
            String expectedReview,
            String expectedRuntime
    ) {
        requireNamespacedId(stage.id(), "stage binding");
        requireStatus(stage.status(), stage.id());
        if (!expectedId.equals(stage.id())
                || !expectedReview.equals(stage.sourceReviewVolumeId())
                || !expectedRuntime.equals(stage.runtimeVolumeId())) {
            throw new IllegalArgumentException(
                    "R01 Quarry stage contract mismatch: " + stage.id()
            );
        }
        if (stage.sourceReviewVolumeId().equals(stage.runtimeVolumeId())) {
            throw new IllegalArgumentException(
                    "R01 Quarry review shell cannot be reused as final runtime volume: "
                            + stage.id()
            );
        }
        if (stage.role().isBlank()) {
            throw new IllegalArgumentException(
                    "R01 Quarry stage role is blank: " + stage.id()
            );
        }
    }

    private static String expectedReviewVolume(RoomId room) {
        return switch (room) {
            case UPPER_GALLERY -> UPPER_REVIEW;
            case COLLAPSED_HOIST -> HOIST_REVIEW;
            case ROOT_BREACHED -> ROOT_REVIEW;
        };
    }

    private static String expectedRuntimeVolume(RoomId room) {
        return switch (room) {
            case UPPER_GALLERY -> UPPER_RUNTIME;
            case COLLAPSED_HOIST -> HOIST_RUNTIME;
            case ROOT_BREACHED -> ROOT_RUNTIME;
        };
    }

    private static Set<String> expectedSpawnSockets(RoomId room) {
        Set<String> ids = new HashSet<>(
                R01QuarryRoomEncounterRules.initialPlan(room, 4).anchorIds()
        );
        if (room == RoomId.UPPER_GALLERY) {
            ids.addAll(
                    R01QuarryRoomEncounterRules
                            .upperGallerySecondWave(4)
                            .anchorIds()
            );
        }
        return Set.copyOf(ids);
    }

    private static void requireStatus(String status, String subject) {
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Unknown R01 Quarry binding status for " + subject
                            + ": " + status
            );
        }
    }

    private static void requireNamespacedId(String id, String kind) {
        if (id == null
                || id.isBlank()
                || id.indexOf(':') <= 0
                || id.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Invalid namespaced R01 Quarry " + kind + " id: " + id
            );
        }
    }
}
