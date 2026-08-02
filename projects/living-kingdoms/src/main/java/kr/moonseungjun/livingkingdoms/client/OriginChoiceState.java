package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;

/** Fixed origin state for the first complete Erden kingdom release. */
final class OriginChoiceState {
    OriginChoiceState() {
        FoundationCatalog.bootstrap();
        PlayableOriginCatalog.residences();
    }

    void nextSpecies() {
    }

    void nextHomeland() {
    }

    void nextBackground() {
    }

    void nextResidence() {
    }

    String speciesId() {
        return PlayableOriginCatalog.DEFAULT_SPECIES;
    }

    String homelandId() {
        return PlayableOriginCatalog.DEFAULT_HOMELAND;
    }

    String backgroundId() {
        return PlayableOriginCatalog.DEFAULT_BACKGROUND;
    }

    String residenceId() {
        return PlayableOriginCatalog.DEFAULT_RESIDENCE;
    }

    String speciesName() {
        return FoundationCatalog.species().get(speciesId()).displayName();
    }

    String homelandName() {
        return FoundationCatalog.homelands().get(homelandId()).displayName();
    }

    String backgroundName() {
        return FoundationCatalog.backgrounds().get(backgroundId()).displayName();
    }

    String residenceName(boolean compact) {
        return compact ? "왕도 시민구 임대방"
                : PlayableOriginCatalog.residences().get(residenceId()).displayName();
    }
}
