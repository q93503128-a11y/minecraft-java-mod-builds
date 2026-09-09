package kr.moonseungjun.riftfrontier.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationPublicationSlotTest {
    @Test
    void newerUpdateClearsCurrentAndRejectsOlderAsyncCompletion() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket first = slot.beginUpdate();
        assertTrue(slot.isCurrent(first));
        assertTrue(slot.publish(first, "first"));
        assertEquals("first", slot.current().orElseThrow());

        GenerationPublicationSlot.Ticket second = slot.beginUpdate();
        assertFalse(slot.isCurrent(first));
        assertTrue(slot.isCurrent(second));
        assertTrue(slot.current().isEmpty());
        assertFalse(slot.publish(first, "stale"));
        assertTrue(slot.publish(second, "second"));
        assertEquals("second", slot.current().orElseThrow());
    }

    @Test
    void invalidateClearsPublishedValueAndMakesInflightTicketStale() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket ticket = slot.beginUpdate();
        assertTrue(slot.isCurrent(ticket));
        assertTrue(slot.publish(ticket, "ready"));
        long publishedGeneration = slot.generation();

        slot.invalidate();

        assertFalse(slot.isCurrent(ticket));
        assertTrue(slot.current().isEmpty());
        assertEquals(publishedGeneration + 1L, slot.generation());
        assertFalse(slot.publish(ticket, "resurrected"));
    }

    @Test
    void ticketCannotCrossPublicationSlots() {
        GenerationPublicationSlot<String> first = new GenerationPublicationSlot<>();
        GenerationPublicationSlot<String> second = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket foreign = first.beginUpdate();

        assertThrows(IllegalArgumentException.class, () -> second.isCurrent(foreign));
        assertThrows(IllegalArgumentException.class, () -> second.publish(foreign, "wrong-slot"));
        assertTrue(second.current().isEmpty());
    }

    @Test
    void sameCurrentTicketCanAtomicallyReplaceBeforeInvalidation() {
        GenerationPublicationSlot<String> slot = new GenerationPublicationSlot<>();
        GenerationPublicationSlot.Ticket ticket = slot.beginUpdate();

        assertTrue(slot.publish(ticket, "prepared-a"));
        assertTrue(slot.isCurrent(ticket));
        assertTrue(slot.publish(ticket, "prepared-b"));
        assertEquals("prepared-b", slot.current().orElseThrow());
    }
}
