package io.github.q93503128.turnbound.combat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class FieldEncounterClaimsTest {
    private static final FieldEncounterClaims.Tuning TEST_TUNING =
            new FieldEncounterClaims.Tuning(10, 1.5, 20, 3, 8);

    @Test void encounterMustBeVisibleWarnThenPursueThenContact() {
        var engine = new FieldEncounterClaims();
        var actor = UUID.randomUUID();
        var player = UUID.randomUUID();
        engine.register(actor, TEST_TUNING);
        assertEquals(FieldEncounterClaims.Phase.IDLE, engine.view(actor).phase());
        assertFalse(engine.observe(actor, player, true, false, 25, 0).warned());
        var warning = engine.observe(actor, player, true, true, 25, 1);
        assertTrue(warning.warned());
        assertEquals(FieldEncounterClaims.Phase.WARNING, warning.phase());
        assertFalse(engine.observe(actor, player, true, true, 1, 2).contactClaim());
        assertEquals(FieldEncounterClaims.Phase.WARNING, engine.view(actor).phase());
        assertEquals(FieldEncounterClaims.Phase.PURSUIT,
                engine.observe(actor, player, true, true, 25, 4).phase());
        var contact = engine.observe(actor, player, true, true, 1, 5);
        assertTrue(contact.contactClaim());
        assertEquals(FieldEncounterClaims.Phase.CLAIMED, contact.phase());
        assertFalse(engine.observe(actor, player, true, true, 1, 6).contactClaim());
        assertEquals(player, engine.view(actor).target());
    }

    @Test void losesPursuitWhenDisengagedOrTargetChangedAndCannotRecontactImmediately() {
        var engine = new FieldEncounterClaims();
        var actor = UUID.randomUUID();
        var player = UUID.randomUUID();
        var other = UUID.randomUUID();
        engine.register(actor, TEST_TUNING);
        engine.observe(actor, player, true, true, 4, 0);
        assertEquals(FieldEncounterClaims.Phase.IDLE,
                engine.observe(actor, other, true, true, 4, 1).phase());
        engine.observe(actor, player, true, true, 4, 2);
        assertEquals(FieldEncounterClaims.Phase.IDLE,
                engine.observe(actor, player, true, true, 500, 3).phase());
        engine.observe(actor, player, true, true, 4, 4);
        engine.observe(actor, player, true, true, 4, 7);
        assertTrue(engine.observe(actor, player, true, true, 1, 8).contactClaim());
        engine.release(actor, 9);
        for(int tick=10;tick<17;tick++)
            assertEquals(FieldEncounterClaims.Phase.COOLDOWN,
                    engine.observe(actor, player, true, true, 1, tick).phase());
        assertEquals(FieldEncounterClaims.Phase.IDLE,
                engine.observe(actor, player, true, true, 1, 17).phase());
        assertTrue(engine.observe(actor, player, true, true, 1, 18).warned());
    }

    @Test void onlyOneActorCanClaimAPlayerAndRemovingActorReleasesOwner() {
        var engine=new FieldEncounterClaims();
        var a=UUID.randomUUID();var b=UUID.randomUUID();var owner=UUID.randomUUID();
        engine.register(a,TEST_TUNING);
        engine.register(b,TEST_TUNING);
        engine.observe(a,owner,true,true,4,0);
        engine.observe(b,owner,true,true,4,0);
        engine.observe(a,owner,true,true,4,4);
        engine.observe(b,owner,true,true,4,4);
        assertTrue(engine.observe(a,owner,true,true,1,5).contactClaim());
        assertEquals(FieldEncounterClaims.Phase.IDLE,
                engine.observe(b,owner,true,true,1,5).phase(),
                "Second representative must lose claim eligibility");
        engine.remove(a);
        assertEquals(1,engine.size());
        assertTrue(engine.observe(b,owner,true,true,1,6).warned());
    }

    @Test void invalidOrUnregisteredRepresentativesRefused() {
        var engine=new FieldEncounterClaims();
        var a=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () ->
            new FieldEncounterClaims.Tuning(1,2,3,0,10));
        assertThrows(IllegalArgumentException.class, () ->
            new FieldEncounterClaims.Tuning(10,1,8,1,10));
        assertThrows(IllegalArgumentException.class, () ->
            engine.observe(a,a,true,true,1,0));
        engine.register(a,TEST_TUNING);
        assertThrows(IllegalStateException.class,()->engine.register(a,TEST_TUNING));
        assertThrows(IllegalArgumentException.class, () ->
            engine.observe(a,a,true,true,Double.NaN,0));
        assertThrows(IllegalArgumentException.class, () ->
            engine.observe(a,a,true,true,1,-1));
    }
}
