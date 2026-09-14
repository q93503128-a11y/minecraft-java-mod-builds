package kr.moonseungjun.riftfrontier.client.render;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** API-free regression coverage for the material preparation provenance discipline. */
class Region01BossMaterialPreparationTest {
    @Test
    void staleGenerationIsRejectedBeforeTextureRead() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket oldReload = slot.beginUpdate();
        slot.beginUpdate();
        AtomicBoolean read = new AtomicBoolean();

        assertThrows(StaleMaterial.class, () -> prepare(slot, oldReload, new Object(), sha256(new byte[] {1}), owner -> {
            read.set(true);
            return new byte[] {1};
        }));
        assertFalse(read.get());
    }

    @Test
    void exactTextureOwnerAndHashProducePreparedMaterial() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();
        Object owner = new Object();
        byte[] texture = new byte[] {4, 8, 15, 16, 23, 42};

        Prepared prepared = prepare(slot, reload, owner, sha256(texture), actualOwner -> {
            assertTrue(actualOwner == owner);
            return texture;
        });

        assertEquals(sha256(texture), prepared.sha256());
        assertTrue(slot.isCurrent(reload));
    }

    @Test
    void hashMismatchFailsClosedBeforePublication() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();
        AtomicBoolean published = new AtomicBoolean();

        assertThrows(IntegrityFailure.class, () -> {
            prepare(slot, reload, new Object(), sha256(new byte[] {1, 2, 3}), owner -> new byte[] {9, 9, 9});
            published.set(true);
        });
        assertFalse(published.get());
    }

    @Test
    void reloadStartedDuringTextureIoRejectsOldBytes() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket reload = slot.beginUpdate();
        AtomicBoolean hashReached = new AtomicBoolean();

        assertThrows(StaleMaterial.class, () -> prepare(
            slot,
            reload,
            new Object(),
            sha256(new byte[] {7}),
            owner -> {
                slot.beginUpdate();
                return new byte[] {7};
            },
            hashReached
        ));
        assertFalse(hashReached.get(), "stale texture bytes must be rejected before integrity/publication work");
    }

    private static Prepared prepare(
        GenerationPublicationSlot<String> slot,
        GenerationPublicationSlot.Ticket reload,
        Object resourceOwner,
        String expectedSha256,
        Reader reader
    ) {
        return prepare(slot, reload, resourceOwner, expectedSha256, reader, new AtomicBoolean());
    }

    private static Prepared prepare(
        GenerationPublicationSlot<String> slot,
        GenerationPublicationSlot.Ticket reload,
        Object resourceOwner,
        String expectedSha256,
        Reader reader,
        AtomicBoolean hashReached
    ) {
        requireCurrent(slot, reload);
        byte[] bytes = reader.read(resourceOwner);
        requireCurrent(slot, reload);
        hashReached.set(true);
        String actual = sha256(bytes);
        if (!actual.equals(expectedSha256)) throw new IntegrityFailure();
        requireCurrent(slot, reload);
        return new Prepared(actual);
    }

    private static void requireCurrent(
        GenerationPublicationSlot<String> slot,
        GenerationPublicationSlot.Ticket reload
    ) {
        if (!slot.isCurrent(reload)) throw new StaleMaterial();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    @FunctionalInterface
    private interface Reader {
        byte[] read(Object owner);
    }

    private record Prepared(String sha256) { }
    private static final class StaleMaterial extends IllegalStateException { }
    private static final class IntegrityFailure extends IllegalArgumentException { }
}
