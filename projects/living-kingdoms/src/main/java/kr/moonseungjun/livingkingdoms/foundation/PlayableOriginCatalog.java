package kr.moonseungjun.livingkingdoms.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The intentionally small origin set exposed by the next playable test.
 * The wider FoundationCatalog remains available for later world expansion.
 */
public final class PlayableOriginCatalog {
    public static final Set<String> SPECIES = Set.of("human", "elf", "dwarf");
    public static final Set<String> HOMELANDS = Set.of("erden_kingdom", "silvana_forest", "kardum_league");
    public static final Set<String> BACKGROUNDS = Set.of(
            "common_resident", "fisher_family", "wanderer", "scholar_student"
    );

    private static final Map<String, ResidenceOption> RESIDENCES = new LinkedHashMap<>();

    static {
        register(new ResidenceOption(
                "erden_city_room", "에르덴 변경도시의 임대방", "erden_kingdom", "erden_city", 12, 70, 10
        ));
        register(new ResidenceOption(
                "erden_farm_home", "로엔 들판의 가족 주택", "erden_kingdom", "erden_fields", 112, 69, 74
        ));
        register(new ResidenceOption(
                "river_fishing_hut", "은빛강 어촌의 작은 집", "erden_kingdom", "erden_river", -104, 68, 92
        ));
        register(new ResidenceOption(
                "forest_camp", "왕국 북로의 방랑자 야영지", "erden_kingdom", "erden_road", 86, 69, -112
        ));

        register(new ResidenceOption(
                "silvana_tree_home", "실바나 수관 주거지", "silvana_forest", "silvana_canopy", 1210, 82, 8
        ));
        register(new ResidenceOption(
                "silvana_moonwell_lodge", "달샘 숲지기의 숙소", "silvana_forest", "silvana_moonwell", 1290, 70, 84
        ));

        register(new ResidenceOption(
                "kardum_worker_quarters", "카르둠 작업자 숙소", "kardum_league", "kardum_hall", -1210, 72, 8
        ));
        register(new ResidenceOption(
                "kardum_gate_lodge", "산문 경비대의 객실", "kardum_league", "kardum_gate", -1128, 76, 92
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

    public static ValidationResult validate(
            String speciesId,
            String homelandId,
            String backgroundId,
            String residenceId
    ) {
        FoundationCatalog.bootstrap();
        List<String> errors = new ArrayList<>();

        if (!SPECIES.contains(speciesId) || !FoundationCatalog.species().containsKey(speciesId)) {
            errors.add("현재 선택할 수 없는 종족입니다.");
        }
        if (!HOMELANDS.contains(homelandId) || !FoundationCatalog.homelands().containsKey(homelandId)) {
            errors.add("현재 선택할 수 없는 출신 세력입니다.");
        }
        if (!BACKGROUNDS.contains(backgroundId) || !FoundationCatalog.backgrounds().containsKey(backgroundId)) {
            errors.add("현재 선택할 수 없는 사회적 배경입니다.");
        }

        FoundationCatalog.SpeciesDefinition species = FoundationCatalog.species().get(speciesId);
        if (species != null && !species.allowedHomelandIds().contains(homelandId)) {
            errors.add("해당 종족은 아직 이 출신 세력에서 시작할 수 없습니다.");
        }

        ResidenceOption residence = RESIDENCES.get(residenceId);
        if (residence == null || !residence.homelandId().equals(homelandId)) {
            errors.add("선택한 거주지가 출신 세력과 맞지 않습니다.");
        }

        FoundationCatalog.BackgroundDefinition background = FoundationCatalog.backgrounds().get(backgroundId);
        FoundationCatalog.HomelandDefinition homeland = FoundationCatalog.homelands().get(homelandId);
        if (background != null && homeland != null && !background.requiredLifestyleTags().isEmpty()
                && Collections.disjoint(background.requiredLifestyleTags(), homeland.lifestyleTags())) {
            errors.add("이 배경은 해당 출신 지역에서 아직 지원되지 않습니다.");
        }

        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    private static void register(ResidenceOption option) {
        if (RESIDENCES.putIfAbsent(option.id(), option) != null) {
            throw new IllegalStateException("Duplicate playable residence: " + option.id());
        }
    }

    public record ResidenceOption(
            String id,
            String displayName,
            String homelandId,
            String regionId,
            int spawnX,
            int spawnY,
            int spawnZ
    ) {
        public ResidenceOption {
            id = requireId(id, "id");
            displayName = Objects.requireNonNull(displayName, "displayName").trim();
            homelandId = requireId(homelandId, "homelandId");
            regionId = requireId(regionId, "regionId");
            if (displayName.isBlank()) {
                throw new IllegalArgumentException("displayName must not be blank");
            }
            if (spawnY < 66 || spawnY > 300) {
                throw new IllegalArgumentException("Unsafe playable spawn Y: " + spawnY);
            }
        }
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public ValidationResult {
            errors = List.copyOf(errors);
            if (valid && !errors.isEmpty()) {
                throw new IllegalArgumentException("Valid origin result cannot contain errors");
            }
        }
    }

    private static String requireId(String value, String field) {
        String id = Objects.requireNonNull(value, field).trim();
        if (!id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid " + field + ": " + id);
        }
        return id;
    }
}
