package kr.moonseungjun.livingkingdoms.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Authoritative foundation catalog for the first complete kingdom slice.
 *
 * <p>Only the core human origin in Erden is exposed while that kingdom is being completed. Future
 * peoples and realms must not be reintroduced until their geography, society, settlements and
 * simulation are implemented to the same standard.</p>
 */
public final class FoundationCatalog {
    private static final Map<String, SpeciesDefinition> SPECIES = new LinkedHashMap<>();
    private static final Map<String, HomelandDefinition> HOMELANDS = new LinkedHashMap<>();
    private static final Map<String, BackgroundDefinition> BACKGROUNDS = new LinkedHashMap<>();
    private static final Map<String, ResidenceDefinition> RESIDENCES = new LinkedHashMap<>();

    private static boolean bootstrapped;

    private FoundationCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;

        registerHomeland(new HomelandDefinition(
                "erden_kingdom", "에르덴 왕국", 0, 0,
                Set.of("urban", "rural", "river", "agriculture", "trade", "craft",
                        "law", "faith", "military")
        ));
        registerSpecies(new SpeciesDefinition(
                "human", "인간", Set.of("adaptable", "social_flexibility"), Set.of("erden_kingdom")
        ));
        registerBackground(new BackgroundDefinition(
                "common_resident", "평범한 주민", Set.of(),
                Set.of("erden_citizen", "local_contacts", "basic_literacy")
        ));
        registerResidence(new ResidenceDefinition(
                "erden_city_room", "왕도 시민구의 임대방", "erden_kingdom", "urban", 320, 180
        ));

        bootstrapped = true;
    }

    public static Map<String, SpeciesDefinition> species() {
        bootstrap();
        return Collections.unmodifiableMap(SPECIES);
    }

    public static Map<String, HomelandDefinition> homelands() {
        bootstrap();
        return Collections.unmodifiableMap(HOMELANDS);
    }

    public static Map<String, BackgroundDefinition> backgrounds() {
        bootstrap();
        return Collections.unmodifiableMap(BACKGROUNDS);
    }

    public static Map<String, ResidenceDefinition> residences() {
        bootstrap();
        return Collections.unmodifiableMap(RESIDENCES);
    }

    public static ValidationResult validate(OriginSelection selection) {
        bootstrap();
        Objects.requireNonNull(selection, "selection");

        List<String> errors = new ArrayList<>();
        SpeciesDefinition species = SPECIES.get(selection.speciesId());
        HomelandDefinition homeland = HOMELANDS.get(selection.homelandId());
        BackgroundDefinition background = BACKGROUNDS.get(selection.backgroundId());
        ResidenceDefinition residence = RESIDENCES.get(selection.residenceId());

        if (species == null) errors.add("Unknown species: " + selection.speciesId());
        if (homeland == null) errors.add("Unknown homeland: " + selection.homelandId());
        if (background == null) errors.add("Unknown background: " + selection.backgroundId());
        if (residence == null) errors.add("Unknown residence: " + selection.residenceId());

        if (species != null && homeland != null && !species.allowedHomelandIds().contains(homeland.id())) {
            errors.add(species.displayName() + " cannot start in " + homeland.displayName());
        }
        if (residence != null && homeland != null && !residence.homelandId().equals(homeland.id())) {
            errors.add("Residence does not belong to the selected homeland");
        }
        if (background != null && homeland != null && !background.requiredLifestyleTags().isEmpty()
                && Collections.disjoint(background.requiredLifestyleTags(), homeland.lifestyleTags())) {
            errors.add("Background is not supported by the selected homeland");
        }
        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    private static void registerSpecies(SpeciesDefinition definition) {
        putUnique(SPECIES, definition.id(), definition);
    }

    private static void registerHomeland(HomelandDefinition definition) {
        putUnique(HOMELANDS, definition.id(), definition);
    }

    private static void registerBackground(BackgroundDefinition definition) {
        putUnique(BACKGROUNDS, definition.id(), definition);
    }

    private static void registerResidence(ResidenceDefinition definition) {
        HomelandDefinition homeland = HOMELANDS.get(definition.homelandId());
        if (homeland == null) {
            throw new IllegalStateException("Residence references unknown homeland: " + definition.homelandId());
        }
        if (!homeland.lifestyleTags().contains(definition.lifestyleTag())) {
            throw new IllegalStateException("Unsupported residence lifestyle tag: " + definition.lifestyleTag());
        }
        putUnique(RESIDENCES, definition.id(), definition);
    }

    private static <T> void putUnique(Map<String, T> target, String id, T value) {
        if (target.putIfAbsent(id, value) != null) {
            throw new IllegalStateException("Duplicate foundation id: " + id);
        }
    }

    private static String requireId(String id) {
        String value = Objects.requireNonNull(id, "id").trim();
        if (!value.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid foundation id: " + value);
        return value;
    }

    private static String requireText(String value, String field) {
        String text = Objects.requireNonNull(value, field).trim();
        if (text.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return text;
    }

    public record SpeciesDefinition(String id, String displayName, Set<String> traits,
                                    Set<String> allowedHomelandIds) {
        public SpeciesDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            traits = Set.copyOf(traits);
            allowedHomelandIds = Set.copyOf(allowedHomelandIds);
        }
    }

    public record HomelandDefinition(String id, String displayName, int centerX, int centerZ,
                                     Set<String> lifestyleTags) {
        public HomelandDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            lifestyleTags = Set.copyOf(lifestyleTags);
        }
    }

    public record BackgroundDefinition(String id, String displayName,
                                       Set<String> requiredLifestyleTags, Set<String> startingFlags) {
        public BackgroundDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            requiredLifestyleTags = Set.copyOf(requiredLifestyleTags);
            startingFlags = Set.copyOf(startingFlags);
        }
    }

    public record ResidenceDefinition(String id, String displayName, String homelandId,
                                      String lifestyleTag, int spawnX, int spawnZ) {
        public ResidenceDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            homelandId = requireId(homelandId);
            lifestyleTag = requireId(lifestyleTag);
        }
    }

    public record OriginSelection(String speciesId, String homelandId,
                                  String backgroundId, String residenceId) {
        public OriginSelection {
            speciesId = requireId(speciesId);
            homelandId = requireId(homelandId);
            backgroundId = requireId(backgroundId);
            residenceId = requireId(residenceId);
        }
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public ValidationResult {
            errors = List.copyOf(errors);
            if (valid && !errors.isEmpty()) throw new IllegalArgumentException("A valid result cannot contain errors");
        }
    }
}
