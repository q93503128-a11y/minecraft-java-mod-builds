package dev.moonseungjun.openworldrpg.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ClassPassiveCatalog {
    private static final String RESOURCE =
            "/data/openworld_rpg/progression/class_passives.json";
    private final Map<String, ClassPassiveNodeSpec> byId;

    private ClassPassiveCatalog(Map<String, ClassPassiveNodeSpec> byId) {
        this.byId = Map.copyOf(byId);
        validateShape();
    }

    public static ClassPassiveCatalog bundled() {
        return Holder.INSTANCE;
    }

    public Optional<ClassPassiveNodeSpec> node(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public ClassPassiveNodeSpec require(String id) {
        return node(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown class passive node: " + id)
        );
    }

    public List<ClassPassiveNodeSpec> rootNodes(RootClass rootClass) {
        Objects.requireNonNull(rootClass, "rootClass");
        return byId.values().stream()
                .filter(node -> node.rootClass() == rootClass)
                .filter(ClassPassiveNodeSpec::rootNode)
                .sorted(java.util.Comparator.comparing(ClassPassiveNodeSpec::id))
                .toList();
    }

    public List<ClassPassiveNodeSpec> branchNodes(ClassSpecialization specialization) {
        Objects.requireNonNull(specialization, "specialization");
        return byId.values().stream()
                .filter(node -> node.specialization().equals(Optional.of(specialization)))
                .sorted(java.util.Comparator.comparing(ClassPassiveNodeSpec::id))
                .toList();
    }

    public int size() {
        return byId.size();
    }

    private void validateShape() {
        if (byId.size() != 115) {
            throw new IllegalStateException("Canonical passive catalog must contain 115 root/branch nodes; got " + byId.size());
        }
        for (RootClass rootClass : RootClass.values()) {
            List<ClassPassiveNodeSpec> root = rootNodes(rootClass);
            if (root.size() != 7 || root.stream().mapToInt(ClassPassiveNodeSpec::maxRank).sum() != 17) {
                throw new IllegalStateException("Root passive shape mismatch for " + rootClass);
            }
        }
        for (ClassSpecialization specialization : ClassSpecialization.values()) {
            List<ClassPassiveNodeSpec> branch = branchNodes(specialization);
            if (branch.size() != 8 || branch.stream().mapToInt(ClassPassiveNodeSpec::maxRank).sum() != 18) {
                throw new IllegalStateException("Branch passive shape mismatch for " + specialization);
            }
            if (branch.stream().filter(n -> n.tier() == ClassPassiveTier.I).count() != 2L
                    || branch.stream().filter(n -> n.tier() == ClassPassiveTier.II).count() != 2L
                    || branch.stream().filter(n -> n.tier() == ClassPassiveTier.III).count() != 2L
                    || branch.stream().filter(n -> n.tier() == ClassPassiveTier.CAPSTONE).count() != 2L) {
                throw new IllegalStateException("Branch passive tier shape mismatch for " + specialization);
            }
        }
    }

    private static ClassPassiveCatalog loadBundled() {
        try (InputStream stream = ClassPassiveCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing passive catalog resource: " + RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            int schema = root.get("schema_version").getAsInt();
            if (schema != 1) {
                throw new IllegalStateException("Unsupported passive catalog schema: " + schema);
            }
            JsonArray nodes = root.getAsJsonArray("nodes");
            Map<String, ClassPassiveNodeSpec> parsed = new LinkedHashMap<>();
            for (JsonElement element : nodes) {
                JsonObject object = element.getAsJsonObject();
                String id = object.get("id").getAsString();
                String name = object.get("name").getAsString();
                RootClass rootClass = RootClass.valueOf(
                        object.get("root_class").getAsString().toUpperCase(Locale.ROOT)
                );
                Optional<ClassSpecialization> specialization =
                        object.has("specialization") && !object.get("specialization").isJsonNull()
                                ? Optional.of(
                                        ClassSpecialization.byId(object.get("specialization").getAsString())
                                                .orElseThrow(() -> new IllegalStateException(
                                                        "Unknown specialization in passive catalog: "
                                                                + object.get("specialization").getAsString()
                                                ))
                                )
                                : Optional.empty();
                ClassPassiveTier tier = ClassPassiveTier.valueOf(
                        object.get("tier").getAsString().toUpperCase(Locale.ROOT)
                );
                int maxRank = object.get("max_rank").getAsInt();
                ClassPassiveNodeSpec spec = new ClassPassiveNodeSpec(
                        id, name, rootClass, specialization, tier, maxRank
                );
                if (parsed.putIfAbsent(id, spec) != null) {
                    throw new IllegalStateException("Duplicate passive node id: " + id);
                }
            }
            return new ClassPassiveCatalog(parsed);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load passive catalog.", exception);
        }
    }

    private static final class Holder {
        private static final ClassPassiveCatalog INSTANCE = loadBundled();
    }
}
