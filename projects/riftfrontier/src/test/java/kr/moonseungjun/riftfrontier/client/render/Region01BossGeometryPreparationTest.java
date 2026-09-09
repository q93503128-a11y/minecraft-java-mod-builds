package kr.moonseungjun.riftfrontier.client.render;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API-free regression coverage for the generation discipline used by Region01BossGeometryPreparation.
 *
 * <p>The plain JVM test source set intentionally does not carry Minecraft/NeoForge classes. Production client API
 * compatibility is therefore verified by clean/client compilation and client smoke, while these tests exercise the
 * race-sensitive rule itself: preparation may touch only the resource owner captured by its current generation and
 * must re-check the generation after I/O before any importer/consumer step can run.</p>
 */
class Region01BossGeometryPreparationTest {
    @Test
    void staleGenerationIsRejectedBeforeResourceRead() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket oldReload = slot.beginUpdate();
        slot.beginUpdate();
        AtomicBoolean read = new AtomicBoolean();

        assertThrows(StalePreparation.class, () -> prepare(slot, oldReload, new Object(), manager -> {
            read.set(true);
            return new byte[] {1};
        }));
        assertFalse(read.get(), "stale reload must fail before touching resource bytes");
    }

    @Test
    void currentGenerationReadsOnlyItsCapturedResourceOwner() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();
        Object resources = new Object();
        AtomicBoolean imported = new AtomicBoolean();

        byte[] bytes = prepare(slot, reload, resources, manager -> {
            assertSame(resources, manager, "preparation must retain the exact resource snapshot owner");
            return new byte[] {1, 2, 3, 4};
        });
        if (slot.isCurrent(reload)) imported.set(true);

        assertTrue(imported.get());
        assertTrue(bytes.length == 4);
    }

    @Test
    void newerReloadStartedDuringIoRejectsBytesBeforeImporterStep() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();
        Object resources = new Object();
        AtomicBoolean read = new AtomicBoolean();
        AtomicBoolean importerReached = new AtomicBoolean();

        assertThrows(StalePreparation.class, () -> {
            byte[] bytes = prepare(slot, reload, resources, manager -> {
                assertSame(resources, manager);
                read.set(true);
                slot.beginUpdate();
                return new byte[] {9, 9, 9};
            });
            importerReached.set(bytes.length > 0);
        });

        assertTrue(read.get());
        assertFalse(importerReached.get(), "stale bytes must be rejected before the importer can run");
    }

    private static byte[] prepare(
        GenerationPublicationSlot<String> slot,
        GenerationPublicationSlot.Ticket reload,
        Object resourceOwner,
        Reader reader
    ) {
        requireCurrent(slot, reload);
        byte[] bytes = reader.read(resourceOwner);
        requireCurrent(slot, reload);
        return bytes;
    }

    private static void requireCurrent(
        GenerationPublicationSlot<String> slot,
        GenerationPublicationSlot.Ticket reload
    ) {
        if (!slot.isCurrent(reload)) throw new StalePreparation();
    }

    @FunctionalInterface
    private interface Reader {
        byte[] read(Object resourceOwner);
    }

    private static final class StalePreparation extends IllegalStateException { }
}
