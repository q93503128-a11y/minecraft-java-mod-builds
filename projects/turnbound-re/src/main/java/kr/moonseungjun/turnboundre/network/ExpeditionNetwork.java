package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.EnemyTurnService;
import kr.moonseungjun.turnboundre.client.ExpeditionJournalClientState;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Server-authoritative Expedition Journal request/start flow. */
public final class ExpeditionNetwork {
    private ExpeditionNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                ExpeditionNetworkPayloads.RequestJournalC2S.TYPE,
                ExpeditionNetworkPayloads.RequestJournalC2S.STREAM_CODEC,
                ExpeditionNetwork::handleRequest);
        registrar.playToServer(
                ExpeditionNetworkPayloads.StartEncounterC2S.TYPE,
                ExpeditionNetworkPayloads.StartEncounterC2S.STREAM_CODEC,
                ExpeditionNetwork::handleStart);
        registrar.playToClient(
                ExpeditionNetworkPayloads.JournalSnapshotS2C.TYPE,
                ExpeditionNetworkPayloads.JournalSnapshotS2C.STREAM_CODEC,
                (payload, context) -> ExpeditionJournalClientState.accept(payload));
    }

    private static void handleRequest(ExpeditionNetworkPayloads.RequestJournalC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        replyJournal(context, player, "", "");
    }

    private static void handleStart(ExpeditionNetworkPayloads.StartEncounterC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        final String encounterId;
        try {
            encounterId = payload.encounterId();
        } catch (RuntimeException malformed) {
            replyJournal(context, player, "INVALID_ENCOUNTER", "");
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        if (TurnboundRe.BATTLES.battleForController(player.getUUID()).isPresent()) {
            replyJournal(context, player, "ALREADY_IN_BATTLE", "");
            return;
        }

        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        if (!definitions.encounters().containsKey(encounterId)) {
            replyJournal(context, player, "INVALID_ENCOUNTER", encounterId);
            return;
        }
        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        if (progress.party().isEmpty()) {
            replyJournal(context, player, "EMPTY_PARTY", "");
            return;
        }

        List<CharacterProgress> party = new ArrayList<>(progress.party().size());
        for (String characterId : progress.party()) {
            CharacterProgress character = progress.characters().get(characterId);
            if (character == null) {
                replyJournal(context, player, "INVALID_PARTY", characterId);
                return;
            }
            party.add(character);
        }

        UUID battleId = UUID.randomUUID();
        long battleSeed = player.level().getGameTime()
                ^ player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 21)
                ^ Integer.toUnsignedLong(encounterId.hashCode());
        var launch = TurnboundRe.AUTHORED_ENCOUNTERS.openVirtual(
                encounterId, player.getUUID(), party, battleId, battleSeed);
        EnemyTurnService.resolveUntilPlayerOrTerminal(TurnboundRe.BATTLES, launch.battle());

        // Reuse the canonical character visual catalog so virtual battle participants can be rendered
        // without duplicating sourceEntity metadata into the dynamic battle snapshot contract.
        context.reply(CharacterPresentationNetworkPayloads.CatalogS2C.from(definitions));
        context.reply(BattleNetworkPayloads.BattleSnapshotS2C.from(
                launch.battle(), launch.definitionContext(), TurnboundRe.BATTLES));
    }

    private static void replyJournal(
            IPayloadContext context,
            ServerPlayer player,
            String resultCode,
            String resultDetail
    ) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        List<ExpeditionNetworkPayloads.EncounterView> encounters = definitions.encounters().values().stream()
                .sorted(Comparator.comparingInt(kr.moonseungjun.turnboundre.data.EncounterDefinition::difficulty)
                        .thenComparing(kr.moonseungjun.turnboundre.data.EncounterDefinition::id))
                .map(encounter -> new ExpeditionNetworkPayloads.EncounterView(
                        encounter.id(), encounter.difficulty(), encounter.enemies().size(), encounter.repeatable()))
                .toList();
        context.reply(ExpeditionNetworkPayloads.JournalSnapshotS2C.of(
                new ExpeditionNetworkPayloads.JournalView(progress.party(), encounters, resultCode, resultDetail)));
    }
}
