package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.BattleCommand;
import kr.moonseungjun.turnboundre.battle.BattleDefinitionContext;
import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.battle.BattleInstance;
import kr.moonseungjun.turnboundre.battle.BattleManager;
import kr.moonseungjun.turnboundre.battle.BattleParticipant;
import kr.moonseungjun.turnboundre.battle.BattleState;
import kr.moonseungjun.turnboundre.battle.BattleTeam;
import kr.moonseungjun.turnboundre.battle.EnemyIntent;
import kr.moonseungjun.turnboundre.battle.EntityParticipantBinding;
import kr.moonseungjun.turnboundre.battle.ParticipantCombatState;
import kr.moonseungjun.turnboundre.data.ActionDefinition;
import kr.moonseungjun.turnboundre.data.CharacterDefinition;
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
import java.util.function.Function;

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

    /**
     * A server-published action affordance for the current player actor.
     * eligibleTargetIds is authoritative presentation data for selection only; the strict server gate revalidates submission.
     */
    public record SnapshotAction(
            String id,
            String slot,
            String kind,
            int energyCost,
            int hpPower,
            int poisePower,
            String damageTag,
            String targetTeam,
            String targetShape,
            int targetCount,
            boolean usable,
            String disabledReason,
            List<String> eligibleTargetIds
    ) {
        public SnapshotAction {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("action id must not be blank");
            if (slot == null || slot.isBlank()) throw new IllegalArgumentException("action slot must not be blank");
            if (kind == null || kind.isBlank()) throw new IllegalArgumentException("action kind must not be blank");
            if (energyCost < 0) throw new IllegalArgumentException("action energyCost must be >= 0");
            if (targetTeam == null || targetTeam.isBlank()) throw new IllegalArgumentException("action targetTeam must not be blank");
            if (targetShape == null || targetShape.isBlank()) throw new IllegalArgumentException("action targetShape must not be blank");
            if (targetCount <= 0) throw new IllegalArgumentException("action targetCount must be > 0");
            if (damageTag == null) damageTag = "";
            if (disabledReason == null) disabledReason = "";
            eligibleTargetIds = eligibleTargetIds == null ? List.of() : List.copyOf(eligibleTargetIds);
            for (String targetId : eligibleTargetIds) {
                if (targetId == null || targetId.isBlank()) {
                    throw new IllegalArgumentException("eligibleTargetIds must not contain blanks");
                }
            }
            if (eligibleTargetIds.stream().distinct().count() != eligibleTargetIds.size()) {
                throw new IllegalArgumentException("eligibleTargetIds must not contain duplicates");
            }
            if (usable && !disabledReason.isBlank()) throw new IllegalArgumentException("usable action must not carry disabledReason");
            if (!usable && disabledReason.isBlank()) throw new IllegalArgumentException("disabled action requires disabledReason");
            if (usable && eligibleTargetIds.size() < targetCount) {
                throw new IllegalArgumentException("usable action requires enough eligible targets");
            }
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
            int participantOrdinal,
            String characterId,
            List<SnapshotStatus> statuses,
            SnapshotIntent intent,
            UUID entityId
    ) {
        public SnapshotParticipant(
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
                int participantOrdinal,
                String characterId,
                List<SnapshotStatus> statuses,
                SnapshotIntent intent
        ) {
            this(id, hp, maxHp, poise, poiseMax, energy, guard, exposed, poiseGuard, alive,
                    team, participantOrdinal, characterId, statuses, intent, null);
        }

        public SnapshotParticipant {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("participant id must not be blank");
            if (team == null || team.isBlank()) throw new IllegalArgumentException("participant team must not be blank");
            if (participantOrdinal < 0) throw new IllegalArgumentException("participantOrdinal must be >= 0");
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
            List<SnapshotParticipant> participants,
            List<SnapshotAction> availableActions
    ) {
        public DecodedSnapshot {
            participants = participants == null ? List.of() : List.copyOf(participants);
            availableActions = availableActions == null ? List.of() : List.copyOf(availableActions);
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

        public BattleCommandC2S { wire = checkedWire(wire); }

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
            return encodeSnapshot(battle, null, ignored -> null);
        }

        /** Production presentation path without world bindings, retained for deterministic core tests. */
        public static BattleSnapshotS2C from(BattleInstance battle, BattleDefinitionContext definitions) {
            return encodeSnapshot(battle, definitions, ignored -> null);
        }

        /** Production/server path: includes immutable definitions plus authoritative participant -> entity bindings. */
        public static BattleSnapshotS2C from(
                BattleInstance battle,
                BattleDefinitionContext definitions,
                BattleManager battles
        ) {
            if (battle == null) throw new IllegalArgumentException("battle must not be null");
            if (battles == null) throw new IllegalArgumentException("battles must not be null");
            return encodeSnapshot(
                    battle,
                    definitions,
                    participantId -> battles.binding(battle.battleId(), participantId)
                            .map(EntityParticipantBinding::entityId)
                            .orElse(null));
        }

        private static BattleSnapshotS2C encodeSnapshot(
                BattleInstance battle,
                BattleDefinitionContext definitions,
                Function<String, UUID> entityIds
        ) {
            if (battle == null) throw new IllegalArgumentException("battle must not be null");
            if (entityIds == null) throw new IllegalArgumentException("entityIds must not be null");
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
                UUID entityId = entityIds.apply(id);

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
                        Integer.toString(participant.participantOrdinal()),
                        characterId,
                        packList(statusRows),
                        packIntent(intent),
                        entityId == null ? "" : entityId.toString()));
            }

            List<String> actionRows = snapshotActions(battle, definitions).stream()
                    .map(action -> join(
                            action.id(), action.slot(), action.kind(), Integer.toString(action.energyCost()),
                            Integer.toString(action.hpPower()), Integer.toString(action.poisePower()), action.damageTag(),
                            action.targetTeam(), action.targetShape(), Integer.toString(action.targetCount()),
                            Boolean.toString(action.usable()), action.disabledReason(), packList(action.eligibleTargetIds())))
                    .toList();

            return new BattleSnapshotS2C(join(
                    battle.battleId().toString(), Long.toString(battle.revision()), battle.state().name(),
                    Integer.toString(battle.cycle()), battle.currentActorId(), packList(participantRows), packList(actionRows)));
        }

        public DecodedSnapshot decode() {
            List<String> p = split(wire, 7);
            List<SnapshotParticipant> participants = new ArrayList<>();
            for (String row : unpackList(p.get(5))) {
                List<String> r = split(row, 16);
                List<SnapshotStatus> statuses = new ArrayList<>();
                for (String statusRow : unpackList(r.get(13))) {
                    List<String> status = split(statusRow, 3);
                    statuses.add(new SnapshotStatus(status.get(0), Integer.parseInt(status.get(1)), Integer.parseInt(status.get(2))));
                }
                participants.add(new SnapshotParticipant(
                        r.get(0), Integer.parseInt(r.get(1)), Integer.parseInt(r.get(2)), Integer.parseInt(r.get(3)),
                        Integer.parseInt(r.get(4)), Integer.parseInt(r.get(5)), Boolean.parseBoolean(r.get(6)),
                        Boolean.parseBoolean(r.get(7)), Boolean.parseBoolean(r.get(8)), Boolean.parseBoolean(r.get(9)),
                        r.get(10), Integer.parseInt(r.get(11)), r.get(12), List.copyOf(statuses), unpackIntent(r.get(14)),
                        r.get(15).isBlank() ? null : UUID.fromString(r.get(15))));
            }

            List<SnapshotAction> actions = new ArrayList<>();
            for (String row : unpackList(p.get(6))) {
                List<String> r = split(row, 13);
                actions.add(new SnapshotAction(
                        r.get(0), r.get(1), r.get(2), Integer.parseInt(r.get(3)), Integer.parseInt(r.get(4)),
                        Integer.parseInt(r.get(5)), r.get(6), r.get(7), r.get(8), Integer.parseInt(r.get(9)),
                        Boolean.parseBoolean(r.get(10)), r.get(11), unpackList(r.get(12))));
            }

            return new DecodedSnapshot(
                    UUID.fromString(p.get(0)), Long.parseLong(p.get(1)), p.get(2), Integer.parseInt(p.get(3)), p.get(4),
                    List.copyOf(participants), List.copyOf(actions));
        }

        private static SnapshotIntent snapshotIntent(BattleInstance battle, BattleParticipant participant, ParticipantCombatState state) {
            if (participant.team() != BattleTeam.ENEMY || !state.alive() || battle.state() == BattleState.ENCOUNTER_OPEN) return null;
            EnemyIntent intent = battle.enemyIntent(participant.id());
            return new SnapshotIntent(
                    intent.actionId(), intent.type().name(), intent.targeting().name(), intent.risk().name(),
                    intent.breakCancelable(), intent.breakDowngradeAction());
        }

        private static List<SnapshotAction> snapshotActions(BattleInstance battle, BattleDefinitionContext definitions) {
            if (definitions == null || battle.state() != BattleState.AWAIT_COMMAND) return List.of();

            String actorId = battle.currentActorId();
            BattleParticipant actor = battle.participant(actorId);
            ParticipantCombatState actorState = battle.combatState(actorId);
            if (actor.team() != BattleTeam.PLAYER || !actorState.alive()) return List.of();

            String characterId = definitions.characterId(actorId);
            if (characterId == null || characterId.isBlank()) {
                throw new IllegalStateException("battle definition snapshot has no character id for current actor " + actorId);
            }
            CharacterDefinition character = definitions.definitions().characters().get(characterId);
            if (character == null) throw new IllegalStateException("battle definition snapshot has no CharacterDefinition " + characterId);

            List<SnapshotAction> actions = new ArrayList<>();
            addDataAction(actions, battle, actor, actorState, definitions, character.basicAction(), "BASIC");
            for (int i = 0; i < character.skills().size(); i++) {
                addDataAction(actions, battle, actor, actorState, definitions, character.skills().get(i), "SKILL_" + (i + 1));
            }
            actions.add(new SnapshotAction(
                    "guard", "GUARD", "GUARD", 0, 0, 0, "", "SELF", "SINGLE", 1, true, "",
                    List.of(actor.id())));
            addDataAction(actions, battle, actor, actorState, definitions, character.burst(), "BURST");
            return List.copyOf(actions);
        }

        private static void addDataAction(
                List<SnapshotAction> out,
                BattleInstance battle,
                BattleParticipant actor,
                ParticipantCombatState actorState,
                BattleDefinitionContext definitions,
                String actionId,
                String slot
        ) {
            ActionDefinition action = definitions.definitions().actions().get(actionId);
            if (action == null) throw new IllegalStateException("battle definition snapshot has no ActionDefinition " + actionId);

            List<String> eligibleTargetIds = eligibleTargetIds(battle, actor, action.targeting().team());
            String disabledReason = "";
            if (actorState.energy() < action.energyCost()) {
                disabledReason = "ENERGY";
            } else if (eligibleTargetIds.size() < action.targeting().count()) {
                disabledReason = "TARGETS";
            }

            out.add(new SnapshotAction(
                    action.id(), slot, action.kind(), action.energyCost(), action.hpPower(), action.poisePower(),
                    action.damageTag(), action.targeting().team(), action.targeting().shape(), action.targeting().count(),
                    disabledReason.isEmpty(), disabledReason, eligibleTargetIds));
        }

        private static List<String> eligibleTargetIds(BattleInstance battle, BattleParticipant actor, String targetTeam) {
            List<BattleParticipant> targets = new ArrayList<>();
            for (String participantId : battle.actorOrder()) {
                if (!battle.combatState(participantId).alive()) continue;
                BattleParticipant target = battle.participant(participantId);
                boolean allowed = switch (targetTeam) {
                    case "SELF" -> target.id().equals(actor.id());
                    case "ALLY" -> target.team() == actor.team();
                    case "ENEMY" -> target.team() != actor.team();
                    case "ANY" -> true;
                    default -> throw new IllegalStateException("validated action has unsupported target team " + targetTeam);
                };
                if (allowed) targets.add(target);
            }
            targets.sort(Comparator.comparingInt(BattleParticipant::participantOrdinal));
            return targets.stream().map(BattleParticipant::id).toList();
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
            for (BattleEvent event : events) rows.add(join(Long.toString(event.revision()), event.type(), event.actorId(), event.detail()));
            return new BattleEventsS2C(join(battleId.toString(), Long.toString(resultingRevision), Integer.toString(fromIndex), packList(rows)));
        }

        public static BattleEventsS2C rejection(UUID battleId, long revision, String reason) {
            return new BattleEventsS2C(join(battleId.toString(), Long.toString(revision), "-1",
                    packList(List.of(join(Long.toString(revision), "COMMAND_REJECTED", "", reason)))));
        }

        public DecodedEvents decode() {
            List<String> p = split(wire, 4);
            List<BattleEvent> events = new ArrayList<>();
            for (String row : unpackList(p.get(3))) {
                List<String> r = split(row, 4);
                events.add(new BattleEvent(Long.parseLong(r.get(0)), r.get(1), r.get(2), r.get(3)));
            }
            return new DecodedEvents(UUID.fromString(p.get(0)), Long.parseLong(p.get(1)), Integer.parseInt(p.get(2)), List.copyOf(events));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static String packIntent(SnapshotIntent intent) {
        if (intent == null) return "";
        return join(intent.actionId(), intent.type(), intent.targeting(), intent.risk(),
                Boolean.toString(intent.breakCancelable()), intent.breakDowngradeAction());
    }

    private static SnapshotIntent unpackIntent(String packed) {
        if (packed == null || packed.isEmpty()) return null;
        List<String> values = split(packed, 6);
        return new SnapshotIntent(values.get(0), values.get(1), values.get(2), values.get(3),
                Boolean.parseBoolean(values.get(4)), values.get(5));
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
