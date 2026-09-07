package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleState;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.battle.EnemyIntent;
import kr.moonseungjun.turnboundre.battle.ParticipantCombatState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative battle wire contracts.
 * Presentation fields are snapshots only; clients must never recompute battle truth from them.
 */
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

    public record SnapshotStatus(String id, int stacks, int remaining) {
        public SnapshotStatus {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("status id must not be blank");
            if (stacks <= 0) throw new IllegalArgumentException("status stacks must be > 0");
            if (remaining == 0 || remaining < -1) throw new IllegalArgumentException("status remaining must be -1 or > 0");
        }
    }

    public record SnapshotIntent(
            String actionId,
            String type,
            String targeting,
            String risk,
            boolean breakCancelable,
            String breakDowngradeAction
    ) {
        public SnapshotIntent {
            if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("intent actionId must not be blank");
            if (type == null || type.isBlank()) throw new IllegalArgumentException("intent type must not be blank");
            if (targeting == null || targeting.isBlank()) throw new IllegalArgumentException("intent targeting must not be blank");
            if (risk == null || risk.isBlank()) throw new IllegalArgumentException("intent risk must not be blank");
            if (breakDowngradeAction == null) breakDowngradeAction = "";
        }
    }

    public record SnapshotParticipant(
            String id,
            int hp,
            int maxHp,
            int poise,
            int poiseMax,
            int energy,
            boolean guard,
            boolean exposed,
            boolean poiseGuard,
            boolean alive,
            String team,
            String characterId,
            List<SnapshotStatus> statuses,
            SnapshotIntent intent
    ) {
        public SnapshotParticipant {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("participant id must not be blank");
            if (team == null || team.isBlank()) throw new IllegalArgumentException("participant team must not be blank");
            if (characterId == null) characterId = "";
            statuses = statuses == null ? List.of() : List.copyOf(statuses);
        }
    }

    public record DecodedSnapshot(
            UUID battleId,
            long revision,
            String state,
            int cycle,
            String currentActorId,
            List<SnapshotParticipant> participants
    ) {
        public DecodedSnapshot {
            participants = participants == null ? List.of() : List.copyOf(participants);
        }
    }

    public record DecodedEvents(
            UUID battleId,
            long resultingRevision,
            int fromIndex,
            List<BattleEvent> events
    ) {}

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

        /** Core/unit-test path when no immutable data-definition context exists. */
        public static BattleSnapshotS2C from(BattleInstance battle) {
            return from(battle, null);
        }

        /** Production path: includes immutable character identity captured when the battle opened. */
        public static BattleSnapshotS2C from(BattleInstance battle, BattleDefinitionContext definitions) {
            if (battle == null) throw new IllegalArgumentException("battle must not be null");
            List<String> participantRows = new ArrayList<>();
            for (String id : battle.actorOrder()) {
                BattleParticipant participant = battle.participant(id);
                ParticipantCombatState state = battle.combatState(id);
                String characterId = "";
                if (definitions != null) {
                    characterId = definitions.characterId(id);
                    if (characterId == null || characterId.isBlank()) {
                        throw new IllegalStateException("battle definition snapshot has no character id for participant " + id);
                    }
                }

                List<String> statusRows = state.statuses().activeIds().stream()
                        .sorted(Comparator.naturalOrder())
                        .map(statusId -> join(
                                statusId,
                                Integer.toString(state.statuses().stacks(statusId)),
                                Integer.toString(state.statuses().remaining(statusId))))
                        .toList();

                SnapshotIntent intent = snapshotIntent(battle, participant, state);
                participantRows.add(join(
                        id,
                        Integer.toString(state.hp()),
                        Integer.toString(state.maxHp()),
                        Integer.toString(state.poise()),
                        Integer.toString(state.poiseMax()),
                        Integer.toString(state.energy()),
                        Boolean.toString(state.guard()),
                        Boolean.toString(state.exposed()),
                        Boolean.toString(state.poiseGuard()),
                        Boolean.toString(state.alive()),
                        participant.team().name(),
                        characterId,
                        packList(statusRows),
                        packIntent(intent)));
            }
            return new BattleSnapshotS2C(join(
                    battle.battleId().toString(),
                    Long.toString(battle.revision()),
                    battle.state().name(),
                    Integer.toString(battle.cycle()),
                    battle.currentActorId(),
                    packList(participantRows)));
        }

        public DecodedSnapshot decode() {
            List<String> p = split(wire, 6);
            List<SnapshotParticipant> participants = new ArrayList<>();
            for (String row : unpackList(p.get(5))) {
                List<String> r = split(row, 14);
                List<SnapshotStatus> statuses = new ArrayList<>();
                for (String statusRow : unpackList(r.get(12))) {
                    List<String> status = split(statusRow, 3);
                    statuses.add(new SnapshotStatus(
                            status.get(0),
                            Integer.parseInt(status.get(1)),
                            Integer.parseInt(status.get(2))));
                }
                participants.add(new SnapshotParticipant(
                        r.get(0),
                        Integer.parseInt(r.get(1)),
                        Integer.parseInt(r.get(2)),
                        Integer.parseInt(r.get(3)),
                        Integer.parseInt(r.get(4)),
                        Integer.parseInt(r.get(5)),
                        Boolean.parseBoolean(r.get(6)),
                        Boolean.parseBoolean(r.get(7)),
                        Boolean.parseBoolean(r.get(8)),
                        Boolean.parseBoolean(r.get(9)),
                        r.get(10),
                        r.get(11),
                        List.copyOf(statuses),
                        unpackIntent(r.get(13))));
            }
            return new DecodedSnapshot(
                    UUID.fromString(p.get(0)),
                    Long.parseLong(p.get(1)),
                    p.get(2),
                    Integer.parseInt(p.get(3)),
                    p.get(4),
                    List.copyOf(participants));
        }

        private static SnapshotIntent snapshotIntent(
                BattleInstance battle,
                BattleParticipant participant,
                ParticipantCombatState state
        ) {
            if (participant.team() != BattleTeam.ENEMY || !state.alive() || battle.state() == BattleState.ENCOUNTER_OPEN) {
                return null;
            }
            EnemyIntent intent = battle.enemyIntent(participant.id());
            return new SnapshotIntent(
                    intent.actionId(),
                    intent.type().name(),
                    intent.targeting().name(),
                    intent.risk().name(),
                    intent.breakCancelable(),
                    intent.breakDowngradeAction());
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

        public DecodedEvents decode() {
            List<String> p = split(wire, 4);
            List<BattleEvent> events = new ArrayList<>();
            for (String row : unpackList(p.get(3))) {
                List<String> r = split(row, 4);
                events.add(new BattleEvent(Long.parseLong(r.get(0)), r.get(1), r.get(2), r.get(3)));
            }
            return new DecodedEvents(
                    UUID.fromString(p.get(0)),
                    Long.parseLong(p.get(1)),
                    Integer.parseInt(p.get(2)),
                    List.copyOf(events));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static String packIntent(SnapshotIntent intent) {
        if (intent == null) return "";
        return join(
                intent.actionId(),
                intent.type(),
                intent.targeting(),
                intent.risk(),
                Boolean.toString(intent.breakCancelable()),
                intent.breakDowngradeAction());
    }

    private static SnapshotIntent unpackIntent(String packed) {
        if (packed == null || packed.isEmpty()) return null;
        List<String> values = split(packed, 6);
        return new SnapshotIntent(
                values.get(0),
                values.get(1),
                values.get(2),
                values.get(3),
                Boolean.parseBoolean(values.get(4)),
                values.get(5));
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
