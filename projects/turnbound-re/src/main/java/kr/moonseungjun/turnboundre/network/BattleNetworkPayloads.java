package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.ParticipantCombatState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Wire contracts only. Production presentation is intentionally absent. */
public final class BattleNetworkPayloads {
    private static final int MAX_WIRE_CHARS = 24_000;
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private BattleNetworkPayloads() {}

    public record DecodedCommand(UUID battleId, long expectedRevision, String actorId, String actionId,
                                 String commandId, List<String> targetIds) {
        public BattleCommand toCore() {
            return new BattleCommand(expectedRevision, actorId, actionId, commandId, targetIds);
        }
    }

    public record BattleCommandC2S(String wire) implements CustomPacketPayload {
        public static final Type<BattleCommandC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BattleCommandC2S> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BattleCommandC2S::wire, BattleCommandC2S::new);

        public BattleCommandC2S {
            wire = checkedWire(wire);
        }

        public static BattleCommandC2S of(UUID battleId, BattleCommand command) {
            return new BattleCommandC2S(join(
                    battleId.toString(), Long.toString(command.expectedRevision()), command.actorId(), command.actionId(),
                    command.commandId(), packList(command.targetIds())));
        }

        public DecodedCommand decode() {
            List<String> p = split(wire, 6);
            return new DecodedCommand(UUID.fromString(p.get(0)), Long.parseLong(p.get(1)), p.get(2), p.get(3), p.get(4), unpackList(p.get(5)));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BattleSnapshotS2C(String wire) implements CustomPacketPayload {
        public static final Type<BattleSnapshotS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BattleSnapshotS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BattleSnapshotS2C::wire, BattleSnapshotS2C::new);

        public BattleSnapshotS2C { wire = checkedWire(wire); }

        public static BattleSnapshotS2C from(BattleInstance battle) {
            List<String> participantRows = new ArrayList<>();
            for (String id : battle.actorOrder()) {
                ParticipantCombatState state = battle.combatState(id);
                participantRows.add(join(id, Integer.toString(state.hp()), Integer.toString(state.maxHp()),
                        Integer.toString(state.poise()), Integer.toString(state.poiseMax()), Integer.toString(state.energy()),
                        Boolean.toString(state.guard()), Boolean.toString(state.exposed()), Boolean.toString(state.poiseGuard()),
                        Boolean.toString(state.alive())));
            }
            return new BattleSnapshotS2C(join(battle.battleId().toString(), Long.toString(battle.revision()), battle.state().name(),
                    Integer.toString(battle.cycle()), battle.currentActorId(), packList(participantRows)));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record BattleEventsS2C(String wire) implements CustomPacketPayload {
        public static final Type<BattleEventsS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurnboundRe.MOD_ID, "battle_events"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BattleEventsS2C> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BattleEventsS2C::wire, BattleEventsS2C::new);

        public BattleEventsS2C { wire = checkedWire(wire); }

        public static BattleEventsS2C from(UUID battleId, long resultingRevision, int fromIndex, List<BattleEvent> events) {
            List<String> rows = new ArrayList<>();
            for (BattleEvent event : events) {
                rows.add(join(Long.toString(event.revision()), event.type(), event.actorId(), event.detail()));
            }
            return new BattleEventsS2C(join(battleId.toString(), Long.toString(resultingRevision), Integer.toString(fromIndex), packList(rows)));
        }

        public static BattleEventsS2C rejection(UUID battleId, long revision, String reason) {
            return new BattleEventsS2C(join(battleId.toString(), Long.toString(revision), "-1", packList(List.of(join(Long.toString(revision), "COMMAND_REJECTED", "", reason)))));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static String join(String... values) {
        List<String> encoded = new ArrayList<>(values.length);
        for (String value : values) encoded.add(enc(value == null ? "" : value));
        return String.join(".", encoded);
    }

    static List<String> split(String wire, int expected) {
        String[] pieces = wire.split("\\.", -1);
        if (pieces.length != expected) throw new IllegalArgumentException("invalid wire field count: " + pieces.length + " expected=" + expected);
        List<String> out = new ArrayList<>(pieces.length);
        for (String piece : pieces) out.add(dec(piece));
        return List.copyOf(out);
    }

    static String packList(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        List<String> encoded = new ArrayList<>(values.size());
        for (String value : values) encoded.add(enc(value));
        return String.join(",", encoded);
    }

    static List<String> unpackList(String packed) {
        if (packed == null || packed.isEmpty()) return List.of();
        String[] pieces = packed.split(",", -1);
        List<String> out = new ArrayList<>(pieces.length);
        for (String piece : pieces) out.add(dec(piece));
        return List.copyOf(out);
    }

    private static String enc(String value) { return B64E.encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String value) { return new String(B64D.decode(value), StandardCharsets.UTF_8); }

    private static String checkedWire(String wire) {
        if (wire == null || wire.isBlank()) throw new IllegalArgumentException("wire must not be blank");
        if (wire.length() > MAX_WIRE_CHARS) throw new IllegalArgumentException("wire too large");
        return wire;
    }
}
