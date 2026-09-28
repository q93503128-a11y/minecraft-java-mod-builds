package dev.moonseungjun.openworldrpg.world.structure;

import com.google.gson.annotations.SerializedName;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingData;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data-driven R01 Alderford structure/service/property binding contract.
 *
 * <p>This layer deliberately separates terrain candidates from authored visual compositions.
 * A structure can name its fixed external asset families and deterministic composition identity
 * without claiming an exact prefab before the creator archive and Minecraft visual review have
 * accepted that composition. Service interaction points are semantic composition sockets rather
 * than copied terrain-center coordinates.</p>
 */
public record R01StructureBindingData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        @SerializedName("map_build") String mapBuild,
        @SerializedName("source_status") String sourceStatus,
        List<StructureBinding> structures,
        List<ServiceBinding> services,
        List<PropertyBinding> properties
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CANONICAL_ID =
            "openworld_rpg:r01/alderford_structure_bindings";
    public static final String CANONICAL_MAP_BUILD = "AzariNEW4252026";
    public static final String CANONICAL_SOURCE_STATUS =
            "family_bound_composition_gated";

    public static final String MEDIEVAL_VILLAGE_FAMILY =
            "openworld_rpg:asset_family/quaternius_medieval_village_standard";
    public static final String FANTASY_PROPS_FAMILY =
            "openworld_rpg:asset_family/quaternius_fantasy_props_standard";
    public static final String KAYKIT_RPG_TOOLS_FAMILY =
            "openworld_rpg:asset_family/kaykit_rpg_tools";

    private static final Set<String> ALLOWED_STRUCTURE_KINDS = Set.of(
            "gate",
            "shrine",
            "service",
            "market",
            "board",
            "housing"
    );
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "candidate",
            "client_verified",
            "production"
    );
    private static final Set<String> ALLOWED_COMPOSITION_STATES = Set.of(
            "visual_review_gated",
            "accepted"
    );
    private static final Set<String> ALLOWED_ASSET_FAMILIES = Set.of(
            MEDIEVAL_VILLAGE_FAMILY,
            FANTASY_PROPS_FAMILY,
            KAYKIT_RPG_TOOLS_FAMILY
    );
    private static final Set<String> REQUIRED_STRUCTURE_IDS = Set.of(
            "openworld_rpg:r01/alderford/gate_watch",
            "openworld_rpg:r01/alderford/gate_shrine",
            "openworld_rpg:r01/alderford/copper_kettle",
            "openworld_rpg:r01/alderford/wayfarers_hall",
            "openworld_rpg:r01/alderford/holt_forge",
            "openworld_rpg:r01/alderford/alderford_vault",
            "openworld_rpg:r01/alderford/greenwater_remedies",
            "openworld_rpg:r01/alderford/fordside_stables",
            "openworld_rpg:r01/alderford/market",
            "openworld_rpg:r01/alderford/route_board",
            "openworld_rpg:r01/alderford/housing/gate_cottage",
            "openworld_rpg:r01/alderford/housing/paddock_cottage",
            "openworld_rpg:r01/alderford/housing/riverside_cottage",
            "openworld_rpg:r01/alderford/housing/quarry_road_cottage",
            "openworld_rpg:r01/alderford/housing/market_house"
    );
    private static final Set<String> REQUIRED_SERVICE_IDS = Set.of(
            "openworld_rpg:service/alderford/gate_shrine",
            "openworld_rpg:service/alderford/copper_kettle",
            "openworld_rpg:service/alderford/wayfarers_hall",
            "openworld_rpg:service/alderford/holt_forge",
            "openworld_rpg:service/alderford/alderford_vault",
            "openworld_rpg:service/alderford/greenwater_remedies",
            "openworld_rpg:service/alderford/fordside_stables",
            "openworld_rpg:service/alderford/market",
            "openworld_rpg:service/alderford/route_board"
    );
    private static final Set<String> REQUIRED_PROPERTY_IDS = Set.of(
            "openworld_rpg:property/alderford/gate_cottage",
            "openworld_rpg:property/alderford/paddock_cottage",
            "openworld_rpg:property/alderford/riverside_cottage",
            "openworld_rpg:property/alderford/quarry_road_cottage",
            "openworld_rpg:property/alderford/market_house"
    );

    public R01StructureBindingData {
        structures = List.copyOf(Objects.requireNonNull(structures, "structures"));
        services = List.copyOf(Objects.requireNonNull(services, "services"));
        properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
    }

    public Optional<StructureBinding> structure(String structureId) {
        Objects.requireNonNull(structureId, "structureId");
        return structures.stream()
                .filter(structure -> structure.id().equals(structureId))
                .findFirst();
    }

    public Optional<StructureBinding> productionStructure(String structureId) {
        return structure(structureId)
                .filter(StructureBinding::productionReady);
    }

    public Optional<ServiceBinding> service(String serviceId) {
        Objects.requireNonNull(serviceId, "serviceId");
        return services.stream()
                .filter(service -> service.id().equals(serviceId))
                .findFirst();
    }

    public Optional<ServiceBinding> productionService(String serviceId) {
        return service(serviceId).filter(service -> {
            if (!service.production()) {
                return false;
            }
            return structure(service.structureId())
                    .filter(StructureBinding::productionReady)
                    .isPresent();
        });
    }

    public Optional<PropertyBinding> property(String propertyId) {
        Objects.requireNonNull(propertyId, "propertyId");
        return properties.stream()
                .filter(property -> property.id().equals(propertyId))
                .findFirst();
    }

    public Optional<PropertyBinding> productionProperty(String propertyId) {
        return property(propertyId).filter(property -> {
            if (!property.production()) {
                return false;
            }
            return structure(property.structureId())
                    .filter(StructureBinding::productionReady)
                    .isPresent();
        });
    }

    public boolean productionReady() {
        return !structures.isEmpty()
                && !services.isEmpty()
                && !properties.isEmpty()
                && structures.stream().allMatch(StructureBinding::productionReady)
                && services.stream().allMatch(ServiceBinding::production)
                && properties.stream().allMatch(PropertyBinding::production);
    }

    public record StructureBinding(
            String id,
            String status,
            String kind,
            @SerializedName("spatial_anchor_id") String spatialAnchorId,
            @SerializedName("architecture_family_id") String architectureFamilyId,
            @SerializedName("prop_family_ids") List<String> propFamilyIds,
            CompositionBinding composition,
            String role
    ) {
        public StructureBinding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(spatialAnchorId, "spatialAnchorId");
            propFamilyIds = List.copyOf(
                    Objects.requireNonNull(propFamilyIds, "propFamilyIds")
            );
            Objects.requireNonNull(composition, "composition");
            Objects.requireNonNull(role, "role");
        }

        public boolean production() {
            return "production".equals(status);
        }

        public boolean productionReady() {
            return production() && composition.accepted();
        }
    }

    public record CompositionBinding(
            String id,
            String state,
            @SerializedName("selection_mode") String selectionMode,
            @SerializedName("exact_prefab_id") String exactPrefabId
    ) {
        public CompositionBinding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(selectionMode, "selectionMode");
        }

        public boolean accepted() {
            return "accepted".equals(state);
        }
    }

    public record ServiceBinding(
            String id,
            String status,
            @SerializedName("structure_id") String structureId,
            @SerializedName("interaction_socket_id") String interactionSocketId,
            String role
    ) {
        public ServiceBinding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(structureId, "structureId");
            Objects.requireNonNull(interactionSocketId, "interactionSocketId");
            Objects.requireNonNull(role, "role");
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public record PropertyBinding(
            String id,
            String status,
            @SerializedName("display_name") String displayName,
            @SerializedName("structure_id") String structureId,
            String tier,
            @SerializedName("purchase_price") int purchasePrice,
            @SerializedName("sale_credit_rate") double saleCreditRate,
            @SerializedName("storage_capacity") int storageCapacity
    ) {
        public PropertyBinding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(structureId, "structureId");
            Objects.requireNonNull(tier, "tier");
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public static void validate(R01StructureBindingData data) {
        Objects.requireNonNull(data, "data");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported R01 structure binding schema: " + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 structure binding id: " + data.id()
            );
        }
        if (!CANONICAL_MAP_BUILD.equals(data.mapBuild())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 structure map build: " + data.mapBuild()
            );
        }
        if (!CANONICAL_SOURCE_STATUS.equals(data.sourceStatus())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 structure source status: " + data.sourceStatus()
            );
        }

        validateUniqueIds(
                data.structures().stream().map(StructureBinding::id).toList(),
                "structure"
        );
        validateUniqueIds(
                data.services().stream().map(ServiceBinding::id).toList(),
                "service"
        );
        validateUniqueIds(
                data.properties().stream().map(PropertyBinding::id).toList(),
                "property"
        );
        validateRequiredIds(
                data.structures().stream().map(StructureBinding::id).collect(Collectors.toSet()),
                REQUIRED_STRUCTURE_IDS,
                "structure"
        );
        validateRequiredIds(
                data.services().stream().map(ServiceBinding::id).collect(Collectors.toSet()),
                REQUIRED_SERVICE_IDS,
                "service"
        );
        validateRequiredIds(
                data.properties().stream().map(PropertyBinding::id).collect(Collectors.toSet()),
                REQUIRED_PROPERTY_IDS,
                "property"
        );

        Map<String, StructureBinding> structuresById = data.structures().stream()
                .collect(Collectors.toUnmodifiableMap(
                        StructureBinding::id,
                        Function.identity()
                ));

        Set<String> compositionIds = new HashSet<>();
        for (StructureBinding structure : data.structures()) {
            requireNamespacedId(structure.id(), "structure");
            requireStatus(structure.status(), "structure " + structure.id());
            if (!ALLOWED_STRUCTURE_KINDS.contains(structure.kind())) {
                throw new IllegalArgumentException(
                        "Unknown R01 structure kind for " + structure.id()
                                + ": " + structure.kind()
                );
            }
            requireNamespacedId(structure.spatialAnchorId(), "spatial anchor reference");
            if (structure.architectureFamilyId() != null) {
                requireAllowedAssetFamily(
                        structure.architectureFamilyId(),
                        "architecture family for " + structure.id()
                );
            } else if (!"board".equals(structure.kind())) {
                throw new IllegalArgumentException(
                        "R01 structure requires an architecture family: " + structure.id()
                );
            }
            for (String familyId : structure.propFamilyIds()) {
                requireAllowedAssetFamily(
                        familyId,
                        "prop family for " + structure.id()
                );
            }
            if (structure.role().isBlank()) {
                throw new IllegalArgumentException(
                        "R01 structure role is blank: " + structure.id()
                );
            }

            CompositionBinding composition = structure.composition();
            requireNamespacedId(composition.id(), "composition");
            if (!compositionIds.add(composition.id())) {
                throw new IllegalArgumentException(
                        "Duplicate R01 composition id: " + composition.id()
                );
            }
            if (!ALLOWED_COMPOSITION_STATES.contains(composition.state())) {
                throw new IllegalArgumentException(
                        "Unknown R01 composition state for " + structure.id()
                                + ": " + composition.state()
                );
            }
            if (!"authored_fixed".equals(composition.selectionMode())) {
                throw new IllegalArgumentException(
                        "R01 structure composition must use authored_fixed selection: "
                                + structure.id()
                );
            }
            if (composition.accepted()) {
                requireNamespacedId(composition.exactPrefabId(), "exact prefab");
            } else if (composition.exactPrefabId() != null) {
                throw new IllegalArgumentException(
                        "Visual-review-gated R01 composition cannot claim an exact prefab: "
                                + structure.id()
                );
            }
            if (structure.production() && !composition.accepted()) {
                throw new IllegalArgumentException(
                        "Production R01 structure requires an accepted composition: "
                                + structure.id()
                );
            }
        }

        Set<String> socketIds = new HashSet<>();
        for (ServiceBinding service : data.services()) {
            requireNamespacedId(service.id(), "service");
            requireStatus(service.status(), "service " + service.id());
            requireNamespacedId(service.structureId(), "service structure reference");
            requireNamespacedId(service.interactionSocketId(), "service interaction socket");
            if (!socketIds.add(service.interactionSocketId())) {
                throw new IllegalArgumentException(
                        "Duplicate R01 service interaction socket: "
                                + service.interactionSocketId()
                );
            }
            StructureBinding structure = structuresById.get(service.structureId());
            if (structure == null) {
                throw new IllegalArgumentException(
                        "R01 service references missing structure: " + service.id()
                );
            }
            if ("housing".equals(structure.kind())) {
                throw new IllegalArgumentException(
                        "R01 service cannot use a housing shell as its service structure: "
                                + service.id()
                );
            }
            if (service.role().isBlank()) {
                throw new IllegalArgumentException(
                        "R01 service role is blank: " + service.id()
                );
            }
            if (service.production() && !structure.productionReady()) {
                throw new IllegalArgumentException(
                        "Production R01 service requires a production-ready structure: "
                                + service.id()
                );
            }
        }

        Map<String, PropertyCanon> propertyCanon = canonicalProperties();
        for (PropertyBinding property : data.properties()) {
            requireNamespacedId(property.id(), "property");
            requireStatus(property.status(), "property " + property.id());
            requireNamespacedId(property.structureId(), "property structure reference");
            StructureBinding structure = structuresById.get(property.structureId());
            if (structure == null || !"housing".equals(structure.kind())) {
                throw new IllegalArgumentException(
                        "R01 property must reference a housing structure: " + property.id()
                );
            }
            PropertyCanon canon = propertyCanon.get(property.id());
            if (canon == null) {
                throw new IllegalArgumentException(
                        "Unexpected R01 Alderford property: " + property.id()
                );
            }
            if (!canon.displayName().equals(property.displayName())
                    || !canon.tier().equals(property.tier())
                    || canon.purchasePrice() != property.purchasePrice()
                    || canon.storageCapacity() != property.storageCapacity()) {
                throw new IllegalArgumentException(
                        "R01 Alderford property canon mismatch: " + property.id()
                );
            }
            if (Math.abs(property.saleCreditRate() - 0.80) > 0.000001) {
                throw new IllegalArgumentException(
                        "R01 property sale credit must remain 80%: " + property.id()
                );
            }
            if (property.production() && !structure.productionReady()) {
                throw new IllegalArgumentException(
                        "Production R01 property requires a production-ready shell: "
                                + property.id()
                );
            }
        }

        if (data.productionReady()) {
            throw new IllegalArgumentException(
                    "Bundled Alderford bindings must stay gated until exact prefab and spatial "
                            + "production acceptance are complete."
            );
        }
    }

    public static void validateAgainstSpatial(
            R01StructureBindingData data,
            R01SpatialBindingData spatial
    ) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(spatial, "spatial");
        for (StructureBinding structure : data.structures()) {
            R01SpatialBindingData.Anchor anchor = spatial.anchor(structure.spatialAnchorId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "R01 structure references missing spatial anchor: "
                                    + structure.id() + " -> " + structure.spatialAnchorId()
                    ));
            if (structure.productionReady() && !anchor.production()) {
                throw new IllegalArgumentException(
                        "Production-ready R01 structure cannot bind a non-production spatial anchor: "
                                + structure.id() + " -> " + structure.spatialAnchorId()
                );
            }
        }
    }

    private static Map<String, PropertyCanon> canonicalProperties() {
        return Map.of(
                "openworld_rpg:property/alderford/gate_cottage",
                new PropertyCanon("Gate Cottage", "small_cottage", 2400, 54),
                "openworld_rpg:property/alderford/paddock_cottage",
                new PropertyCanon("Paddock Cottage", "small_cottage", 2400, 54),
                "openworld_rpg:property/alderford/riverside_cottage",
                new PropertyCanon("Riverside Cottage", "small_cottage", 2400, 54),
                "openworld_rpg:property/alderford/quarry_road_cottage",
                new PropertyCanon("Quarry-Road Cottage", "small_cottage", 2400, 54),
                "openworld_rpg:property/alderford/market_house",
                new PropertyCanon("Market House", "town_house", 9000, 72)
        );
    }

    private static void validateUniqueIds(List<String> ids, String kind) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            requireNamespacedId(id, kind);
            if (!seen.add(id)) {
                throw new IllegalArgumentException(
                        "Duplicate R01 " + kind + " id: " + id
                );
            }
        }
    }

    private static void validateRequiredIds(
            Set<String> actual,
            Set<String> required,
            String kind
    ) {
        if (!actual.equals(required)) {
            Set<String> missing = new HashSet<>(required);
            missing.removeAll(actual);
            Set<String> unexpected = new HashSet<>(actual);
            unexpected.removeAll(required);
            throw new IllegalArgumentException(
                    "R01 Alderford " + kind + " set mismatch; missing=" + missing
                            + ", unexpected=" + unexpected
            );
        }
    }

    private static void requireStatus(String status, String subject) {
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Unknown R01 binding status for " + subject + ": " + status
            );
        }
    }

    private static void requireAllowedAssetFamily(String familyId, String subject) {
        requireNamespacedId(familyId, "asset family");
        if (!ALLOWED_ASSET_FAMILIES.contains(familyId)) {
            throw new IllegalArgumentException(
                    "Unapproved R01 asset family for " + subject + ": " + familyId
            );
        }
    }

    private static void requireNamespacedId(String id, String kind) {
        if (id == null
                || id.isBlank()
                || id.indexOf(':') <= 0
                || id.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Invalid namespaced R01 " + kind + " id: " + id
            );
        }
    }

    private record PropertyCanon(
            String displayName,
            String tier,
            int purchasePrice,
            int storageCapacity
    ) {
    }
}
