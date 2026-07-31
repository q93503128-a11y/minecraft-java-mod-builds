package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class OriginChoiceState {
    private static final List<String> SPECIES = List.of("human", "elf", "dwarf");
    private static final List<String> HOMELANDS = List.of("erden_kingdom", "silvana_forest", "kardum_league");
    private static final List<String> BACKGROUNDS = List.of(
            "common_resident", "fisher_family", "wanderer", "scholar_student"
    );

    private int speciesIndex;
    private int homelandIndex;
    private int backgroundIndex;
    private int residenceIndex;

    OriginChoiceState() {
        FoundationCatalog.bootstrap();
        PlayableOriginCatalog.residences();
        normalize();
    }

    void nextSpecies() {
        speciesIndex = (speciesIndex + 1) % SPECIES.size();
        homelandIndex = 0;
        backgroundIndex = 0;
        residenceIndex = 0;
        normalize();
    }

    void nextHomeland() {
        homelandIndex = (homelandIndex + 1) % homelands().size();
        backgroundIndex = 0;
        residenceIndex = 0;
        normalize();
    }

    void nextBackground() {
        backgroundIndex = (backgroundIndex + 1) % backgrounds().size();
        normalize();
    }

    void nextResidence() {
        residenceIndex = (residenceIndex + 1) % residences().size();
        normalize();
    }

    String speciesId() {
        return SPECIES.get(Math.floorMod(speciesIndex, SPECIES.size()));
    }

    String homelandId() {
        List<String> values = homelands();
        return values.get(Math.floorMod(homelandIndex, values.size()));
    }

    String backgroundId() {
        List<String> values = backgrounds();
        return values.get(Math.floorMod(backgroundIndex, values.size()));
    }

    String residenceId() {
        List<String> values = residences();
        return values.get(Math.floorMod(residenceIndex, values.size()));
    }

    String speciesName() {
        FoundationCatalog.SpeciesDefinition value = FoundationCatalog.species().get(speciesId());
        return value == null ? speciesId() : value.displayName();
    }

    String homelandName() {
        FoundationCatalog.HomelandDefinition value = FoundationCatalog.homelands().get(homelandId());
        return value == null ? homelandId() : value.displayName();
    }

    String backgroundName() {
        FoundationCatalog.BackgroundDefinition value = FoundationCatalog.backgrounds().get(backgroundId());
        return value == null ? backgroundId() : value.displayName();
    }

    String residenceName(boolean compact) {
        if (compact) {
            return switch (residenceId()) {
                case "erden_city_room" -> "변경도시 임대방";
                case "erden_farm_home" -> "로엔 들판 주택";
                case "river_fishing_hut" -> "은빛강 어촌집";
                case "forest_camp" -> "북로 야영지";
                case "silvana_tree_home" -> "실바나 수관 주거지";
                case "silvana_moonwell_lodge" -> "달샘 숲지기 숙소";
                case "kardum_worker_quarters" -> "카르둠 작업자 숙소";
                case "kardum_gate_lodge" -> "산문 경비대 객실";
                default -> fullResidenceName();
            };
        }
        return fullResidenceName();
    }

    private String fullResidenceName() {
        PlayableOriginCatalog.ResidenceOption value = PlayableOriginCatalog.residences().get(residenceId());
        return value == null ? residenceId() : value.displayName();
    }

    private List<String> homelands() {
        FoundationCatalog.SpeciesDefinition species = FoundationCatalog.species().get(speciesId());
        List<String> result = new ArrayList<>();
        for (String id : HOMELANDS) {
            if (species != null && species.allowedHomelandIds().contains(id)) result.add(id);
        }
        return result.isEmpty() ? List.of("erden_kingdom") : result;
    }

    private List<String> backgrounds() {
        FoundationCatalog.HomelandDefinition homeland = FoundationCatalog.homelands().get(homelandId());
        List<String> result = new ArrayList<>();
        for (String id : BACKGROUNDS) {
            FoundationCatalog.BackgroundDefinition background = FoundationCatalog.backgrounds().get(id);
            if (background != null && homeland != null
                    && (background.requiredLifestyleTags().isEmpty()
                    || !Collections.disjoint(background.requiredLifestyleTags(), homeland.lifestyleTags()))) {
                result.add(id);
            }
        }
        return result.isEmpty() ? List.of("common_resident") : result;
    }

    private List<String> residences() {
        List<String> result = PlayableOriginCatalog.residencesFor(homelandId()).stream()
                .map(PlayableOriginCatalog.ResidenceOption::id)
                .toList();
        return result.isEmpty() ? List.of("erden_city_room") : result;
    }

    private void normalize() {
        speciesIndex = Math.floorMod(speciesIndex, SPECIES.size());
        homelandIndex = Math.floorMod(homelandIndex, homelands().size());
        backgroundIndex = Math.floorMod(backgroundIndex, backgrounds().size());
        residenceIndex = Math.floorMod(residenceIndex, residences().size());
    }
}
