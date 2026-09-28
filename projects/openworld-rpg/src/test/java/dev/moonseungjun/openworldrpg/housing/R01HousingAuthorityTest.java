package dev.moonseungjun.openworldrpg.housing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.moonseungjun.openworldrpg.economy.PlayerCurrencyState;
import dev.moonseungjun.openworldrpg.inventory.ProjectBackpackState;
import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import dev.moonseungjun.openworldrpg.world.structure.R01StructureBindingLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01HousingAuthorityTest {
    private static final String PLAYER_A =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa").toString();
    private static final String PLAYER_B =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb").toString();

    private static final String GATE =
            "openworld_rpg:property/alderford/gate_cottage";
    private static final String PADDOCK =
            "openworld_rpg:property/alderford/paddock_cottage";
    private static final String MARKET =
            "openworld_rpg:property/alderford/market_house";

    @Test
    void bundledAlderfordRosterIsExactlyFourSmallPlusMarketAndStillGated() {
        var data = R01StructureBindingLoader.loadBundled();

        assertEquals(5, data.properties().size());
        assertEquals(
                4,
                data.properties().stream()
                        .filter(value -> "small_cottage".equals(value.tier()))
                        .count()
        );
        assertEquals(
                1,
                data.properties().stream()
                        .filter(value -> "town_house".equals(value.tier()))
                        .count()
        );
        for (var property : data.properties()) {
            assertFalse(property.production());
            assertTrue(data.productionProperty(property.id()).isEmpty());
        }

        var gate = data.property(GATE).orElseThrow();
        var market = data.property(MARKET).orElseThrow();
        assertEquals(2400, gate.purchasePrice());
        assertEquals(54, gate.storageCapacity());
        assertEquals(0.80, gate.saleCreditRate(), 0.000001);
        assertEquals(9000, market.purchasePrice());
        assertEquals(72, market.storageCapacity());
    }

    @Test
    void oneReservationBlocksAnotherPlayerAndTransferIsIdempotent() {
        var pending = R01HousingPlayerState.initial()
                .prepareMove(
                        PLAYER_A,
                        GATE,
                        54,
                        2400,
                        0,
                        2400
                )
                .pendingMove().orElseThrow();

        var state = R01HousingWorldState.initial();

        var first = state.reserve(
                GATE,
                PLAYER_A,
                pending.transactionId()
        );
        assertTrue(first.success());
        state = first.state();

        var second = state.reserve(
                GATE,
                PLAYER_B,
                "openworld_rpg:housing/b/1"
        );
        assertFalse(second.success());
        assertEquals(
                R01HousingWorldState.ReserveStatus.RESERVED,
                second.status()
        );

        state = state.transferReserved(pending, PLAYER_A);
        assertEquals(PLAYER_A, state.owner(GATE).orElseThrow());
        assertTrue(state.reservation(GATE).isEmpty());

        var repeated = state.transferReserved(pending, PLAYER_A);
        assertEquals(state, repeated);
    }

    @Test
    void movingReleasesOldPropertyAndClaimsNewPropertyAtomicallyInWorldState() {
        var personal = R01HousingPlayerState.initial()
                .prepareMove(PLAYER_A, GATE, 54, 2400, 0, 2400);
        var firstPending = personal.pendingMove().orElseThrow();

        var world = R01HousingWorldState.initial()
                .reserve(GATE, PLAYER_A, firstPending.transactionId())
                .state()
                .transferReserved(firstPending, PLAYER_A);
        personal = personal.commitPending(firstPending.transactionId());

        personal = personal.prepareMove(
                PLAYER_A,
                MARKET,
                72,
                9000,
                1920,
                7080
        );
        var move = personal.pendingMove().orElseThrow();

        world = world.reserve(
                MARKET,
                PLAYER_A,
                move.transactionId()
        ).state();
        world = world.transferReserved(move, PLAYER_A);

        assertTrue(world.owner(GATE).isEmpty());
        assertEquals(PLAYER_A, world.owner(MARKET).orElseThrow());
        assertEquals(7080L, move.requiredGold());
        assertEquals(0L, move.refundGold());
    }

    @Test
    void movingFromMarketHouseToSmallCottageProducesTradeInRefund() {
        var personal = new R01HousingPlayerState(
                1,
                Optional.of(MARKET),
                Optional.of(R01HomeStorageState.empty(72)),
                4L,
                Optional.empty()
        ).prepareMove(
                PLAYER_A,
                PADDOCK,
                54,
                2400,
                7200,
                -4800
        );

        var pending = personal.pendingMove().orElseThrow();
        assertEquals(0L, pending.requiredGold());
        assertEquals(4800L, pending.refundGold());
    }

    @Test
    void downsizingMovesExcessStacksIntoTemporaryMovingSection() {
        var item = ProjectInventoryItem.ordinary(
                "openworld_rpg:test_furnishing",
                1,
                1,
                0L
        );
        List<ProjectBackpackState.SlotEntry> occupied = new ArrayList<>();
        for (int slot = 0; slot < 60; slot++) {
            occupied.add(new ProjectBackpackState.SlotEntry(slot, item));
        }

        var storage = new R01HomeStorageState(72, occupied, List.of())
                .resizeForMove(54);

        assertEquals(54, storage.capacity());
        assertEquals(54, storage.usedSlots());
        assertEquals(6, storage.moving().size());
    }

    @Test
    void goldDebitIsIdempotentAndOldCurrencySaveDecodes() {
        PlayerCurrencyState state = PlayerCurrencyState.initial()
                .creditOnce("openworld_rpg:test/seed_gold", 10_000L);

        var first = state.debitOnce("openworld_rpg:housing/test/debit", 2_400L);
        assertTrue(first.success());
        assertTrue(first.newlyApplied());
        assertEquals(7_600L, first.state().gold());

        var repeated = first.state()
                .debitOnce("openworld_rpg:housing/test/debit", 2_400L);
        assertTrue(repeated.success());
        assertFalse(repeated.newlyApplied());
        assertEquals(7_600L, repeated.state().gold());

        var legacy = JsonParser.parseString(
                """
                {
                  "currency_schema_version": 1,
                  "gold": 123,
                  "applied_credit_transaction_ids": []
                }
                """
        );
        var decoded = PlayerCurrencyState.CODEC
                .parse(JsonOps.INSTANCE, legacy)
                .getOrThrow();

        assertEquals(123L, decoded.gold());
        assertTrue(decoded.appliedDebitTransactionIds().isEmpty());
    }
}
