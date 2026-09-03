package io.github.q93503128.turnbound.presentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnemyAnimationCoverageTest {
    private static final List<String> REQUIRED_CLIPS = List.of(
            "misc.idle", "misc.turn_ready", "attack.strike", "attack.cast",
            "combat.hit", "misc.death", "field.walk", "field.idle");

    private static final List<String> NORMALS = List.of(
            "e001_rotted_walker", "e002_bone_marksman", "e003_unstable_burster", "e004_road_bandit",
            "e005_field_medic", "e006_moss_boar", "e007_spore_lantern", "e008_root_guard",
            "e009_aqueduct_sentry", "e010_flood_leech", "e011_rusted_support", "e012_ash_hound",
            "e013_cinder_adept", "e014_lava_driller");

    private static final List<String> ELITES = List.of(
            "el01_rot_captain", "el02_briar_stag", "el03_rusted_centurion", "el04_magma_drill_king");

    @Test
    void everyNormalEnemyHasDedicatedEightClipBaseline() throws IOException {
        for (String name : NORMALS) assertAnimationCoverage("enemy", name);
    }

    @Test
    void everyEliteHasDedicatedEightClipBaseline() throws IOException {
        for (String name : ELITES) assertAnimationCoverage("elite", name);
    }

    private static void assertAnimationCoverage(String category, String name) throws IOException {
        String path = "assets/turnbound/geckolib/animations/entity/" + category + "/" + name + ".animation.json";
        try (InputStream stream = EnemyAnimationCoverageTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, () -> name + " must have a dedicated animation file: " + path);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String clip : REQUIRED_CLIPS) {
                assertTrue(json.contains("\"" + clip + "\""),
                        () -> name + " is missing required presentation clip " + clip);
            }
        }
    }
}
