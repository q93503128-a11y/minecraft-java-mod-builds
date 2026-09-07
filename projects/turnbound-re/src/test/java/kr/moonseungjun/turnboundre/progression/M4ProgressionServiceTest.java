package kr.moonseungjun.turnboundre.progression;

import kr.moonseungjun.turnboundre.data.CharacterDefinition;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.data.ProgressionDefinition;
import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class M4ProgressionServiceTest {
    private static final String ZOMBIE = "turnbound_re:zombie";
    private static final String WITCH = "turnbound_re:witch";
    private static final String ENDERMAN = "turnbound_re:enderman";
    private static final String GOLEM = "turnbound_re:iron_golem";

    @Test
    void unlockLevelAndAscendSpendResourcesAtomically() throws IOException {
        Fixture f = fixture();
        PlayerProgress fresh = PlayerProgress.fresh(f.tuning);
        PlayerProgress funded = f.service.grantCurrency(fresh, 10_000, 10_000);
        funded = f.service.grantShards(funded, ZOMBIE, 100);

        ProgressionService.Result unlocked = f.service.unlock(funded, ZOMBIE);
        assertTrue(unlocked.accepted());
        assertEquals(1, unlocked.state().characters().get(ZOMBIE).level());
        assertEquals(2, unlocked.state().characters().get(ZOMBIE).originStar());
        assertEquals(2, unlocked.state().characters().get(ZOMBIE).currentStar());
        assertEquals(90, unlocked.state().shards().get(ZOMBIE));

        ProgressionRules.Cost levelCost = ProgressionRules.levelUpCost(f.tuning, 2, 1);
        ProgressionService.Result leveled = f.service.levelUp(unlocked.state(), ZOMBIE);
        assertTrue(leveled.accepted());
        assertEquals(2, leveled.state().characters().get(ZOMBIE).level());
        assertEquals(unlocked.state().coin() - levelCost.coin(), leveled.state().coin());
        assertEquals(unlocked.state().essence() - levelCost.essence(), leveled.state().essence());

        PlayerProgress ready = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA, 10_000, 10_000, Map.of(ZOMBIE, 100),
                Map.of(ZOMBIE, new CharacterProgress(ZOMBIE, 2, 2, 30)), List.of(), f.tuning.partyCapacity());
        ProgressionRules.Cost ascendCost = ProgressionRules.ascensionCost(f.tuning, 3);
        ProgressionService.Result ascended = f.service.ascend(ready, ZOMBIE);
        assertTrue(ascended.accepted());
        assertEquals(2, ascended.state().characters().get(ZOMBIE).originStar());
        assertEquals(3, ascended.state().characters().get(ZOMBIE).currentStar());
        assertEquals(30, ascended.state().characters().get(ZOMBIE).level());
        assertEquals(100 - ascendCost.shards(), ascended.state().shards().get(ZOMBIE));
        assertEquals(10_000 - ascendCost.coin(), ascended.state().coin());
    }

    @Test
    void rejectedOperationsReturnExactUnchangedState() throws IOException {
        Fixture f = fixture();
        PlayerProgress noMoney = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA, 0, 0, Map.of(ZOMBIE, 100),
                Map.of(ZOMBIE, new CharacterProgress(ZOMBIE, 2, 2, 1)), List.of(), f.tuning.partyCapacity());
        ProgressionService.Result noCoin = f.service.levelUp(noMoney, ZOMBIE);
        assertEquals(ProgressionService.ResultCode.INSUFFICIENT_COIN, noCoin.code());
        assertSame(noMoney, noCoin.state());

        PlayerProgress notAtCap = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA, 10_000, 10_000, Map.of(ZOMBIE, 100),
                Map.of(ZOMBIE, new CharacterProgress(ZOMBIE, 2, 2, 29)), List.of(), f.tuning.partyCapacity());
        ProgressionService.Result rejected = f.service.ascend(notAtCap, ZOMBIE);
        assertEquals(ProgressionService.ResultCode.NOT_AT_LEVEL_CAP, rejected.code());
        assertSame(notAtCap, rejected.state());
    }

    @Test
    void partyHasFourSlotAndSquadCostGatesWithoutPartialMutation() throws IOException {
        Fixture f = fixture();
        Map<String, CharacterProgress> owned = new LinkedHashMap<>();
        for (String id : List.of(ZOMBIE, WITCH, ENDERMAN, GOLEM)) {
            CharacterDefinition def = f.registry.characters().get(id);
            owned.put(id, new CharacterProgress(id, def.originStar(), def.originStar(), 1));
        }
        PlayerProgress state = new PlayerProgress(
                PlayerProgress.CURRENT_SCHEMA, 0, 0, Map.of(), owned, List.of(), f.tuning.partyCapacity());

        ProgressionService.Result valid = f.service.setParty(state, List.of(ENDERMAN, GOLEM, ZOMBIE));
        assertTrue(valid.accepted());
        assertEquals(List.of(ENDERMAN, GOLEM, ZOMBIE), valid.state().party());

        ProgressionService.Result tooExpensive = f.service.setParty(state, List.of(ENDERMAN, GOLEM, WITCH));
        assertEquals(ProgressionService.ResultCode.PARTY_COST_EXCEEDED, tooExpensive.code());
        assertSame(state, tooExpensive.state());
        assertTrue(tooExpensive.state().party().isEmpty());

        ProgressionService.Result duplicate = f.service.setParty(state, List.of(ZOMBIE, ZOMBIE));
        assertEquals(ProgressionService.ResultCode.INVALID_PARTY, duplicate.code());
        assertSame(state, duplicate.state());
    }

    private static Fixture fixture() throws IOException {
        DefinitionRegistry registry = ProductionDefinitionFixture.load().registry();
        ProgressionDefinition tuning = registry.progressions().get("turnbound_re:default_progression");
        assertNotNull(tuning);
        return new Fixture(registry, tuning, new ProgressionService(registry, tuning));
    }

    private record Fixture(DefinitionRegistry registry, ProgressionDefinition tuning, ProgressionService service) {}
}
