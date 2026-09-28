package dev.moonseungjun.openworldrpg.world.structure;

import com.google.gson.annotations.SerializedName;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingData;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Production runtime geometry required beyond an accepted Alderford visual shell.
 *
 * <p>A service shell is not live until its composition-owned interaction socket is a production
 * spatial anchor. A property shell is not purchasable until its protection/interior/furnishing
 * volumes and authored critical clearances are production spatial volumes.</p>
 */
public record R01AlderfordRuntimeBindingData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        @SerializedName("map_build") String mapBuild,
        List<ServiceRuntimeBinding> services,
        List<PropertyRuntimeBinding> properties
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CANONICAL_ID =
            "openworld_rpg:r01/alderford_runtime_bindings";
    public static final String CANONICAL_MAP_BUILD = "AzariNEW4252026";

    private static final Set<String> ALLOWED_STATUS = Set.of(
            "candidate",
            "client_verified",
            "production"
    );

    public R01AlderfordRuntimeBindingData {
        services = List.copyOf(Objects.requireNonNull(services, "services"));
        properties = List.copyOf(
                Objects.requireNonNull(properties, "properties")
        );
    }

    public Optional<ServiceRuntimeBinding> service(String serviceId) {
        Objects.requireNonNull(serviceId, "serviceId");
        return services.stream()
                .filter(value -> value.serviceId().equals(serviceId))
                .findFirst();
    }

    public Optional<PropertyRuntimeBinding> property(String propertyId) {
        Objects.requireNonNull(propertyId, "propertyId");
        return properties.stream()
                .filter(value -> value.propertyId().equals(propertyId))
                .findFirst();
    }

    public Optional<ProductionService> productionService(
            String serviceId,
            R01StructureBindingData structures,
            R01SpatialBindingData spatial
    ) {
        Objects.requireNonNull(structures, "structures");
        Objects.requireNonNull(spatial, "spatial");
        ServiceRuntimeBinding runtime = service(serviceId).orElse(null);
        if (runtime == null || !runtime.production()) {
            return Optional.empty();
        }
        var structure = structures.productionService(serviceId);
        if (structure.isEmpty()) {
            return Optional.empty();
        }
        var socket = spatial.productionAnchor(runtime.socketAnchorId());
        if (socket.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                new ProductionService(
                        structure.orElseThrow(),
                        runtime,
                        socket.orElseThrow()
                )
        );
    }

    public Optional<ProductionProperty> productionProperty(
            String propertyId,
            R01StructureBindingData structures,
            R01SpatialBindingData spatial
    ) {
        Objects.requireNonNull(structures, "structures");
        Objects.requireNonNull(spatial, "spatial");
        PropertyRuntimeBinding runtime = property(propertyId).orElse(null);
        if (runtime == null || !runtime.production()) {
            return Optional.empty();
        }
        var property = structures.productionProperty(propertyId);
        if (property.isEmpty()) {
            return Optional.empty();
        }

        var protectedVolume =
                spatial.productionVolume(runtime.protectedVolumeId());
        var interiorVolume =
                spatial.productionVolume(runtime.interiorVolumeId());
        var furnishingVolume =
                spatial.productionVolume(runtime.furnishingVolumeId());
        if (protectedVolume.isEmpty()
                || interiorVolume.isEmpty()
                || furnishingVolume.isEmpty()
                || runtime.clearanceVolumeIds().isEmpty()) {
            return Optional.empty();
        }

        for (String clearanceId : runtime.clearanceVolumeIds()) {
            if (spatial.productionVolume(clearanceId).isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of(
                new ProductionProperty(
                        property.orElseThrow(),
                        runtime,
                        protectedVolume.orElseThrow(),
                        interiorVolume.orElseThrow(),
                        furnishingVolume.orElseThrow()
                )
        );
    }

    public boolean productionReady(
            R01StructureBindingData structures,
            R01SpatialBindingData spatial
    ) {
        for (ServiceRuntimeBinding service : services) {
            if (productionService(
                    service.serviceId(),
                    structures,
                    spatial
            ).isEmpty()) {
                return false;
            }
        }
        for (PropertyRuntimeBinding property : properties) {
            if (productionProperty(
                    property.propertyId(),
                    structures,
                    spatial
            ).isEmpty()) {
                return false;
            }
        }
        return !services.isEmpty() && !properties.isEmpty();
    }

    public record ServiceRuntimeBinding(
            @SerializedName("service_id") String serviceId,
            String status,
            @SerializedName("socket_anchor_id") String socketAnchorId
    ) {
        public ServiceRuntimeBinding {
            Objects.requireNonNull(serviceId, "serviceId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(socketAnchorId, "socketAnchorId");
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public record PropertyRuntimeBinding(
            @SerializedName("property_id") String propertyId,
            String status,
            @SerializedName("protected_volume_id") String protectedVolumeId,
            @SerializedName("interior_volume_id") String interiorVolumeId,
            @SerializedName("furnishing_volume_id") String furnishingVolumeId,
            @SerializedName("clearance_volume_ids") List<String> clearanceVolumeIds
    ) {
        public PropertyRuntimeBinding {
            Objects.requireNonNull(propertyId, "propertyId");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(protectedVolumeId, "protectedVolumeId");
            Objects.requireNonNull(interiorVolumeId, "interiorVolumeId");
            Objects.requireNonNull(furnishingVolumeId, "furnishingVolumeId");
            clearanceVolumeIds = List.copyOf(
                    Objects.requireNonNull(
                            clearanceVolumeIds,
                            "clearanceVolumeIds"
                    )
            );
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public record ProductionService(
            R01StructureBindingData.ServiceBinding service,
            ServiceRuntimeBinding runtime,
            R01SpatialBindingData.Anchor interactionAnchor
    ) {
        public ProductionService {
            Objects.requireNonNull(service, "service");
            Objects.requireNonNull(runtime, "runtime");
            Objects.requireNonNull(interactionAnchor, "interactionAnchor");
        }
    }

    public record ProductionProperty(
            R01StructureBindingData.PropertyBinding property,
            PropertyRuntimeBinding runtime,
            R01SpatialBindingData.Volume protectedVolume,
            R01SpatialBindingData.Volume interiorVolume,
            R01SpatialBindingData.Volume furnishingVolume
    ) {
        public ProductionProperty {
            Objects.requireNonNull(property, "property");
            Objects.requireNonNull(runtime, "runtime");
            Objects.requireNonNull(protectedVolume, "protectedVolume");
            Objects.requireNonNull(interiorVolume, "interiorVolume");
            Objects.requireNonNull(furnishingVolume, "furnishingVolume");
        }
    }

    public static void validate(
            R01AlderfordRuntimeBindingData data,
            R01StructureBindingData structures
    ) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(structures, "structures");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported Alderford runtime-binding schema: "
                            + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected Alderford runtime-binding id: " + data.id()
            );
        }
        if (!CANONICAL_MAP_BUILD.equals(data.mapBuild())) {
            throw new IllegalArgumentException(
                    "Unexpected Alderford runtime map build: " + data.mapBuild()
            );
        }
        if (data.services().size() != structures.services().size()) {
            throw new IllegalArgumentException(
                    "Alderford runtime service set must cover every R01 service."
            );
        }
        if (data.properties().size() != structures.properties().size()) {
            throw new IllegalArgumentException(
                    "Alderford runtime property set must cover every R01 property."
            );
        }

        Set<String> seenServices = new HashSet<>();
        for (ServiceRuntimeBinding runtime : data.services()) {
            requireStatus(runtime.status(), runtime.serviceId());
            requireNamespacedId(runtime.serviceId(), "service");
            requireNamespacedId(runtime.socketAnchorId(), "service socket anchor");
            if (!seenServices.add(runtime.serviceId())) {
                throw new IllegalArgumentException(
                        "Duplicate Alderford runtime service: "
                                + runtime.serviceId()
                );
            }
            R01StructureBindingData.ServiceBinding service =
                    structures.service(runtime.serviceId()).orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Runtime service missing from structure binding: "
                                            + runtime.serviceId()
                            )
                    );
            if (!service.interactionSocketId().equals(
                    runtime.socketAnchorId()
            )) {
                throw new IllegalArgumentException(
                        "Runtime service socket must preserve composition socket id: "
                                + runtime.serviceId()
                );
            }
        }

        Set<String> seenProperties = new HashSet<>();
        for (PropertyRuntimeBinding runtime : data.properties()) {
            requireStatus(runtime.status(), runtime.propertyId());
            requireNamespacedId(runtime.propertyId(), "property");
            if (!seenProperties.add(runtime.propertyId())) {
                throw new IllegalArgumentException(
                        "Duplicate Alderford runtime property: "
                                + runtime.propertyId()
                );
            }
            structures.property(runtime.propertyId()).orElseThrow(
                    () -> new IllegalArgumentException(
                            "Runtime property missing from structure binding: "
                                    + runtime.propertyId()
                    )
            );
            requireNamespacedId(
                    runtime.protectedVolumeId(),
                    "property protected volume"
            );
            requireNamespacedId(
                    runtime.interiorVolumeId(),
                    "property interior volume"
            );
            requireNamespacedId(
                    runtime.furnishingVolumeId(),
                    "property furnishing volume"
            );

            Set<String> uniqueClearances = new HashSet<>();
            for (String clearanceId : runtime.clearanceVolumeIds()) {
                requireNamespacedId(
                        clearanceId,
                        "property clearance volume"
                );
                if (!uniqueClearances.add(clearanceId)) {
                    throw new IllegalArgumentException(
                            "Duplicate property clearance volume: "
                                    + clearanceId
                    );
                }
            }
            if (runtime.production()
                    && runtime.clearanceVolumeIds().isEmpty()) {
                throw new IllegalArgumentException(
                        "Production property requires authored doorway/critical "
                                + "clearance volumes: " + runtime.propertyId()
                );
            }
        }

        Set<String> expectedServices = structures.services().stream()
                .map(R01StructureBindingData.ServiceBinding::id)
                .collect(java.util.stream.Collectors.toSet());
        if (!seenServices.equals(expectedServices)) {
            throw new IllegalArgumentException(
                    "Alderford runtime service ids do not match structure canon."
            );
        }
        Set<String> expectedProperties = structures.properties().stream()
                .map(R01StructureBindingData.PropertyBinding::id)
                .collect(java.util.stream.Collectors.toSet());
        if (!seenProperties.equals(expectedProperties)) {
            throw new IllegalArgumentException(
                    "Alderford runtime property ids do not match structure canon."
            );
        }
    }

    private static void requireStatus(String status, String subject) {
        if (!ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException(
                    "Unknown Alderford runtime status for "
                            + subject + ": " + status
            );
        }
    }

    private static void requireNamespacedId(String id, String kind) {
        if (id == null
                || id.isBlank()
                || id.indexOf(':') <= 0
                || id.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Invalid namespaced Alderford " + kind + " id: " + id
            );
        }
    }
}
