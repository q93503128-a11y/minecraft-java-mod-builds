package io.github.q93503128.turnbound.content;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

final class RebuildContentCatalogTest {
    @Test void productionManifestIsEmptyAndSchemaValid() {
        assertDoesNotThrow(RebuildContentCatalog::requireEmptyProductionCatalog);
    }

    @Test void unexpectedStoryOrRosterIsRejectedRatherThanSilentlyLoaded() {
        String valid = """
            {"schemaVersion":1,"characters":[],"enemies":[],"quests":[],
             "equipment":[],"encounters":[],"regions":[]}
            """;
        assertDoesNotThrow(() -> RebuildContentCatalog.requireEmpty(new StringReader(valid)));
        assertThrows(IllegalStateException.class,
                () -> RebuildContentCatalog.requireEmpty(new StringReader(valid.replace(
                        "\"characters\":[]", "\"characters\":[{\"id\":\"legacy\"}]"))));
        assertThrows(IllegalStateException.class,
                () -> RebuildContentCatalog.requireEmpty(new StringReader(valid.replace(
                        "\"schemaVersion\":1", "\"schemaVersion\":99"))));
        assertThrows(IllegalStateException.class,
                () -> RebuildContentCatalog.requireEmpty(new StringReader(valid.replace(
                        "\"quests\":[]", "\"quests\":null"))));
    }
}
