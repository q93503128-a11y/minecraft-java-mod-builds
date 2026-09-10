package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.WorldEncounterAnchorClientState;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.world.WorldEncounterAnchorResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Server-authoritative preview and confirm path for authored in-world encounter anchors. */
public final class WorldEncounterAnchorNetwork {
    private WorldEncounterAnchorNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                WorldEncounterAnchorPayloads.StartAnchorEncounterC2S.TYPE,
                WorldEncounterAnchorPayloads.StartAnchorEncounterC2S.STREAM_CODEC,
                WorldEncounterAnchorNetwork::handleStart);
        registrar.playToClient(
                WorldEncounterAnchorPayloads.AnchorPreviewS2C.TYPE,
                WorldEncounterAnchorPayloads.AnchorPreviewS2C.STREAM_CODEC,
                (payload, context) -> WorldEncounterAnchorClientState.accept(payload));
        registrar.playToClient(
                WorldEncounterAnchorPayloads.AnchorRejectedS2C.TYPE,
                WorldEncounterAnchorPayloads.AnchorRejectedS2C.STREAM_CODEC,
                (payload, context) -> WorldEncounterAnchorClientState.reject(payload));
    }

    public static void sendPreview(
            ServerPlayer player,
            Entity anchorEntity,
            WorldEncounterAnchorResolver.Resolved resolved
    ) {
        if (player == null || anchorEntity == null || resolved == null) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());

        String code = "";
        if (TurnboundRe.BATTLES.battleForController(player.getUUID()).isPresent()) code = "ALREADY_IN_BATTLE";
        else if (progress.party().isEmpty()) code = "EMPTY_PARTY";

        PacketDistributor.sendToPlayer(player, WorldEncounterAnchorPayloads.AnchorPreviewS2C.from(
                new WorldEncounterAnchorPayloads.PreviewView(
                        anchorEntity.getUUID(),
                        resolved.anchor().locator(),
                        resolved.encounter().id(),
                        resolved.encounter().difficulty(),
                        WorldEncounterAnchorResolver.enemySourceEntities(definitions, resolved.encounter()),
                        WorldEncounterAnchorResolver.rewardKinds(definitions, resolved.encounter()),
                        resolved.anchor().repeatable() && resolved.encounter().repeatable(),
                        progress.party().size(),
                        code,
                        "")));
    }

    private static void handleStart(
            WorldEncounterAnchorPayloads.StartAnchorEncounterC2S payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        final WorldEncounterAnchorPayloads.StartRequest request;
        try {
            request = payload.decode();
        } catch (RuntimeException malformed) {
            context.reply(WorldEncounterAnchorPayloads.AnchorRejectedS2C.of("ANCHOR_UNAVAILABLE", ""));
            return;
        }

        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        String dimensionId = player.level().dimension().identifier().toString();
        WorldEncounterAnchorResolver.Resolved resolved = WorldEncounterAnchorResolver
                .resolve(definitions, request.locator(), dimensionId)
                .orElse(null);
        if (resolved == null) {
            context.reply(WorldEncounterAnchorPayloads.AnchorRejectedS2C.of("ANCHOR_UNAVAILABLE", request.locator()));
            return;
        }
        if (!resolved.encounter().id().equals(request.encounterId())) {
            context.reply(WorldEncounterAnchorPayloads.AnchorRejectedS2C.of("ANCHOR_MISMATCH", request.encounterId()));
            return;
        }

        Entity entity = player.level().getEntity(request.anchorEntityId());
        if (!WorldEncounterAnchorResolver.matchesEntity(entity, request.anchorEntityId(), request.locator())) {
            context.reply(WorldEncounterAnchorPayloads.AnchorRejectedS2C.of("ANCHOR_UNAVAILABLE", request.locator()));
            return;
        }
        if (!WorldEncounterAnchorResolver.withinConfirmRange(player.distanceToSqr(entity))) {
            context.reply(WorldEncounterAnchorPayloads.AnchorRejectedS2C.of("TOO_FAR", request.locator()));
            return;
        }

        EncounterLaunchService.Result result = EncounterLaunchService.tryLaunch(player, request.encounterId());
        if (!result.accepted()) {
            context.reply(WorldEncounterAnchorPayloads.AnchorRejectedS2C.of(result.code(), result.detail()));
            return;
        }
        EncounterLaunchService.replyAccepted(context, result);
    }
}
