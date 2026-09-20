package io.github.q93503128.turnbound.presentation;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroAudioResourceContractTest {
    @Test
    void soundsJsonAndPackagedOggsCoverAllEightCoreHeroTimbres() throws Exception {
        String sounds;
        try (InputStream in = HeroAudioResourceContractTest.class.getClassLoader()
                .getResourceAsStream("assets/turnbound/sounds.json")) {
            assertNotNull(in);
            sounds = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        for (String hero : List.of("kyren","lumea","bram","elysia","lynette","morwen","marion","raze")) {
            assertTrue(sounds.contains("\"sfx.hero." + hero + "\""), hero);
            assertTrue(sounds.contains("turnbound:sfx/heroes/" + hero), hero);
            try (InputStream ogg = HeroAudioResourceContractTest.class.getClassLoader()
                    .getResourceAsStream("assets/turnbound/sounds/sfx/heroes/" + hero + ".ogg")) {
                assertNotNull(ogg, hero + " OGG must be packaged");
                assertTrue(ogg.readNBytes(4).length == 4, hero + " OGG must not be empty");
            }
        }
    }
}
