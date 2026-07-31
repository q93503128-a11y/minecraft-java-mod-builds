package kr.moonseungjun.livingkingdoms.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FoundationCatalog {
    private static final Map<String, SpeciesDefinition> SPECIES = new LinkedHashMap<>();
    private static final Map<String, HomelandDefinition> HOMELANDS = new LinkedHashMap<>();
    private static final Map<String, BackgroundDefinition> BACKGROUNDS = new LinkedHashMap<>();
    private static final Map<String, ResidenceDefinition> RESIDENCES = new LinkedHashMap<>();

    private static boolean bootstrapped;

    private FoundationCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }

        registerHomeland(new HomelandDefinition(
                "erden_kingdom", "에르덴 왕국", 0, 0,
                Set.of("urban", "rural", "military", "academic", "trade", "fishing", "craft", "survival")
        ));
        registerHomeland(new HomelandDefinition(
                "silvana_forest", "실바나 대삼림", -9000, -1500,
                Set.of("forest", "hunting", "herbalism", "arcane", "communal")
        ));
        registerHomeland(new HomelandDefinition(
                "kardum_league", "카르둠 산악연맹", -2500, -9000,
                Set.of("mining", "smithing", "engineering", "trade", "fortress")
        ));
        registerHomeland(new HomelandDefinition(
                "red_steppe", "붉은 초원 부족권", 9500, -1000,
                Set.of("tribal", "nomadic", "hunting", "herding", "warrior")
        ));
        registerHomeland(new HomelandDefinition(
                "velas_free_city", "벨라스 자유도시권", 1500, 7500,
                Set.of("urban", "harbor", "trade", "fishing", "mercenary", "criminal")
        ));
        registerHomeland(new HomelandDefinition(
                "sahar_theocracy", "사하르 신정상업국", 9000, 9000,
                Set.of("desert", "trade", "religious", "astronomy", "craft")
        ));
        registerHomeland(new HomelandDefinition(
                "grey_crown_ruins", "회색 왕관 폐허지", 8500, -7500,
                Set.of("ruins", "survival", "monster_hunting", "archaeology", "exile")
        ));
        registerHomeland(new HomelandDefinition(
                "northern_dragonlands", "북부 용골지대", 0, -15000,
                Set.of("frontier", "survival", "hunting", "mercenary", "hermit")
        ));
        registerHomeland(new HomelandDefinition(
                "western_archipelago", "서해 군도", -14000, 7000,
                Set.of("island", "fishing", "sailing", "trade", "piracy")
        ));

        registerSpecies(new SpeciesDefinition(
                "human", "인간", Set.of("adaptable", "social_flexibility"), Set.copyOf(HOMELANDS.keySet())
        ));
        registerSpecies(new SpeciesDefinition(
                "elf", "엘프", Set.of("long_lived", "keen_senses", "arcane_affinity"),
                Set.of("silvana_forest", "velas_free_city", "erden_kingdom", "grey_crown_ruins")
        ));
        registerSpecies(new SpeciesDefinition(
                "dwarf", "드워프", Set.of("sturdy", "darkvision", "craft_tradition"),
                Set.of("kardum_league", "erden_kingdom", "velas_free_city", "northern_dragonlands")
        ));
        registerSpecies(new SpeciesDefinition(
                "orc", "오크", Set.of("powerful_build", "hardy", "clan_bond"),
                Set.of("red_steppe", "erden_kingdom", "velas_free_city", "grey_crown_ruins", "northern_dragonlands")
        ));
        registerSpecies(new SpeciesDefinition(
                "beastkin", "수인", Set.of("bestial_trait", "keen_senses", "environmental_adaptation"),
                Set.of("red_steppe", "silvana_forest", "velas_free_city", "western_archipelago", "erden_kingdom")
        ));
        registerSpecies(new SpeciesDefinition(
                "halfling", "소인족", Set.of("nimble", "community_bond", "fortunate"),
                Set.of("erden_kingdom", "velas_free_city", "western_archipelago", "silvana_forest")
        ));
        registerSpecies(new SpeciesDefinition(
                "mixed_heritage", "혼혈·복합 혈통", Set.of("custom_heritage", "social_complexity"),
                Set.copyOf(HOMELANDS.keySet())
        ));

        registerBackground(new BackgroundDefinition(
                "common_resident", "평범한 주민", Set.of(), Set.of("citizen", "local_contacts")
        ));
        registerBackground(new BackgroundDefinition(
                "fisher_family", "어부 집안", Set.of("fishing"), Set.of("fishing_basics", "boat_familiarity")
        ));
        registerBackground(new BackgroundDefinition(
                "wanderer", "방랑자", Set.of(), Set.of("campcraft", "road_knowledge", "low_property")
        ));
        registerBackground(new BackgroundDefinition(
                "tribal_member", "부족 생활민", Set.of("tribal"), Set.of("tribal_membership", "survival_basics")
        ));
        registerBackground(new BackgroundDefinition(
                "artisan_apprentice", "장인 견습", Set.of("smithing", "engineering", "craft"),
                Set.of("apprentice_contract", "tool_familiarity")
        ));
        registerBackground(new BackgroundDefinition(
                "scholar_student", "학문 수련생", Set.of("academic", "astronomy", "archaeology"),
                Set.of("literacy", "research_notes", "student_debt")
        ));
        registerBackground(new BackgroundDefinition(
                "militia_recruit", "민병대 신병", Set.of("military", "warrior", "mercenary"),
                Set.of("basic_weapon_training", "service_obligation")
        ));
        registerBackground(new BackgroundDefinition(
                "merchant_house", "상인 가문", Set.of("trade"), Set.of("trade_contacts", "starting_credit")
        ));
        registerBackground(new BackgroundDefinition(
                "exile", "추방자", Set.of("exile", "frontier", "criminal"),
                Set.of("no_citizenship", "survival_basics", "hidden_past")
        ));

        registerResidence(new ResidenceDefinition(
                "erden_city_room", "에르덴 지방도시의 임대방", "erden_kingdom", "urban", 250, 120
        ));
        registerResidence(new ResidenceDefinition(
                "erden_farm_home", "로엔 농촌의 가족 주택", "erden_kingdom", "rural", -420, 780
        ));
        registerResidence(new ResidenceDefinition(
                "river_fishing_hut", "강변 어촌의 작은 집", "erden_kingdom", "fishing", 980, 1460
        ));
        registerResidence(new ResidenceDefinition(
                "forest_camp", "숲길의 방랑자 야영지", "erden_kingdom", "survival", -1800, 900
        ));
        registerResidence(new ResidenceDefinition(
                "red_steppe_clan_tent", "붉은 초원 씨족 천막", "red_steppe", "tribal", 9520, -960
        ));
        registerResidence(new ResidenceDefinition(
                "silvana_tree_home", "실바나 수관 주거지", "silvana_forest", "forest", -8960, -1460
        ));
        registerResidence(new ResidenceDefinition(
                "kardum_worker_quarters", "카르둠 작업자 숙소", "kardum_league", "mining", -2460, -8960
        ));
        registerResidence(new ResidenceDefinition(
                "velas_dock_lodging", "벨라스 부두 하숙집", "velas_free_city", "harbor", 1540, 7540
        ));
        registerResidence(new ResidenceDefinition(
                "ruin_shelter", "폐허지 임시 은신처", "grey_crown_ruins", "ruins", 8540, -7460
        ));
        registerResidence(new ResidenceDefinition(
                "northern_cabin", "북부 개척민 통나무집", "northern_dragonlands", "frontier", 40, -14960
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

        if (species == null) {
            errors.add("Unknown species: " + selection.speciesId());
        }
        if (homeland == null) {
            errors.add("Unknown homeland: " + selection.homelandId());
        }
        if (background == null) {
            errors.add("Unknown background: " + selection.backgroundId());
        }
        if (residence == null) {
            errors.add("Unknown residence: " + selection.residenceId());
        }

        if (species != null && homeland != null && !species.allowedHomelandIds().contains(homeland.id())) {
            errors.add(species.displayName() + " cannot currently start in " + homeland.displayName());
        }

        if (residence != null && homeland != null && !residence.homelandId().equals(homeland.id())) {
            errors.add("Residence does not belong to the selected homeland");
        }

        if (background != null && homeland != null && !background.requiredLifestyleTags().isEmpty()
                && Collections.disjoint(background.requiredLifestyleTags(), homeland.lifestyleTags())) {
            errors.add("Background is not supported by the selected homeland's current starting regions");
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
            throw new IllegalStateException(
                    "Residence lifestyle tag " + definition.lifestyleTag()
                            + " is not supported by homeland " + definition.homelandId()
            );
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
        if (!value.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid foundation id: " + value);
        }
        return value;
    }

    private static String requireText(String value, String field) {
        String text = Objects.requireNonNull(value, field).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return text;
    }

    public record SpeciesDefinition(
            String id,
            String displayName,
            Set<String> traits,
            Set<String> allowedHomelandIds
    ) {
        public SpeciesDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            traits = Set.copyOf(traits);
            allowedHomelandIds = Set.copyOf(allowedHomelandIds);
        }
    }

    public record HomelandDefinition(
            String id,
            String displayName,
            int centerX,
            int centerZ,
            Set<String> lifestyleTags
    ) {
        public HomelandDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            lifestyleTags = Set.copyOf(lifestyleTags);
        }
    }

    public record BackgroundDefinition(
            String id,
            String displayName,
            Set<String> requiredLifestyleTags,
            Set<String> startingFlags
    ) {
        public BackgroundDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            requiredLifestyleTags = Set.copyOf(requiredLifestyleTags);
            startingFlags = Set.copyOf(startingFlags);
        }
    }

    public record ResidenceDefinition(
            String id,
            String displayName,
            String homelandId,
            String lifestyleTag,
            int spawnX,
            int spawnZ
    ) {
        public ResidenceDefinition {
            id = requireId(id);
            displayName = requireText(displayName, "displayName");
            homelandId = requireId(homelandId);
            lifestyleTag = requireId(lifestyleTag);
        }
    }

    public record OriginSelection(
            String speciesId,
            String homelandId,
            String backgroundId,
            String residenceId
    ) {
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
            if (valid && !errors.isEmpty()) {
                throw new IllegalArgumentException("A valid result cannot contain errors");
            }
        }
    }
}
