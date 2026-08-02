package kr.moonseungjun.livingkingdoms.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** The single origin exposed while the complete Erden kingdom slice is being built. */
public final class PlayableOriginCatalog {
    public static final String DEFAULT_SPECIES = "human";
    public static final String DEFAULT_HOMELAND = "erden_kingdom";
    public static final String DEFAULT_BACKGROUND = "common_resident";
    public static final String DEFAULT_RESIDENCE = "erden_city_room";

    public static final Set<String> SPECIES = Set.of(DEFAULT_SPECIES);
    public static final Set<String> HOMELANDS = Set.of(DEFAULT_HOMELAND);
    public static final Set<String> BACKGROUNDS = Set.of(DEFAULT_BACKGROUND);

    private static final Map<String, ResidenceOption> RESIDENCES = new LinkedHashMap<>();

    static {
        register(new ResidenceOption(
                DEFAULT_RESIDENCE,
                "왕도 시민구의 임대방",
                DEFAULT_HOMELAND,
                "erden_capital_citizen_quarter",
                320, 72, 180
        ));
    }

    private PlayableOriginCatalog() {
    }

    public static Map<String, ResidenceOption> residences() {
        return Collections.unmodifiableMap(RESIDENCES);
    }

    public static List<ResidenceOption> residencesFor(String homelandId) {
        return RESIDENCES.values().stream()
                .filter(option -> option.homelandId().equals(homelandId))
                .toList();
    }

    public static ValidationResult validate(String speciesId, String homelandId,
                                            String backgroundId, String residenceId) {
        FoundationCatalog.bootstrap();
        List<String> errors = new ArrayList<>();

        if (!DEFAULT_SPECIES.equals(speciesId)) errors.add("현재는 인간만 시작할 수 있습니다.");
        if (!DEFAULT_HOMELAND.equals(homelandId)) errors.add("현재는 에르덴 왕국만 시작할 수 있습니다.");
        if (!DEFAULT_BACKGROUND.equals(backgroundId)) errors.add("현재는 평범한 주민 배경만 지원합니다.");
        if (!DEFAULT_RESIDENCE.equals(residenceId)) errors.add("현재는 왕도 시민구 거주지만 지원합니다.");

        FoundationCatalog.OriginSelection selection = new FoundationCatalog.OriginSelection(
                speciesId, homelandId, backgroundId, residenceId
        );
        FoundationCatalog.ValidationResult foundation = FoundationCatalog.validate(selection);
        errors.addAll(foundation.errors());
        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    private static void register(ResidenceOption option) {
        if (RESIDENCES.putIfAbsent(option.id(), option) != null) {
            throw new IllegalStateException("Duplicate playable residence: " + option.id());
        }
    }

    public record ResidenceOption(String id, String displayName, String homelandId,
                                  String regionId, int spawnX, int spawnY, int spawnZ) {
        public ResidenceOption {
            id = requireId(id, "id");
            displayName = Objects.requireNonNull(displayName, "displayName").trim();
            homelandId = requireId(homelandId, "homelandId");
            regionId = requireId(regionId, "regionId");
            if (displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
            if (spawnY < 66 || spawnY > 300) throw new IllegalArgumentException("Unsafe playable spawn Y: " + spawnY);
        }
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public ValidationResult {
            errors = List.copyOf(errors);
            if (valid && !errors.isEmpty()) throw new IllegalArgumentException("Valid origin result cannot contain errors");
        }
    }

    private static String requireId(String value, String field) {
        String id = Objects.requireNonNull(value, field).trim();
        if (!id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid " + field + ": " + id);
        return id;
    }
}
