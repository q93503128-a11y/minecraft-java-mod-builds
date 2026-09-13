package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.client.EquipmentClientState;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.EquipmentForgeService;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Equipment intent and inventory-backed forge networking. */
final class EquipmentNetwork {
    private EquipmentNetwork() {}

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                EquipmentNetworkPayloads.ActionC2S.TYPE,
                EquipmentNetworkPayloads.ActionC2S.STREAM_CODEC,
                EquipmentNetwork::handleAction);
        registrar.playToClient(
                EquipmentNetworkPayloads.SnapshotS2C.TYPE,
                EquipmentNetworkPayloads.SnapshotS2C.STREAM_CODEC,
                (payload, context) -> EquipmentClientState.accept(payload));
    }

    private static void handleAction(EquipmentNetworkPayloads.ActionC2S payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        final EquipmentNetworkPayloads.DecodedAction decoded;
        try {
            decoded = payload.decode();
        } catch (RuntimeException invalidWire) {
            TurnboundRe.LOGGER.warn("Rejected malformed TURNBOUND equipment command from {}: {}",
                    player.getUUID(), invalidWire.toString());
            return;
        }

        EquipmentForgeService.Result result = switch (decoded.operation()) {
            case "CRAFT" -> TurnboundRe.EQUIPMENT_FORGE.craft(player, decoded.equipmentId(), decoded.expectedLevel());
            case "UPGRADE" -> TurnboundRe.EQUIPMENT_FORGE.upgrade(player, decoded.equipmentId(), decoded.expectedLevel());
            case "EQUIP" -> TurnboundRe.EQUIPMENT_FORGE.equip(
                    player, decoded.characterId(), decoded.equipmentId(), decoded.expectedLevel(), decoded.expectedEquippedId());
            case "UNEQUIP" -> TurnboundRe.EQUIPMENT_FORGE.unequip(
                    player, decoded.characterId(), decoded.equipmentId(), decoded.expectedLevel(), decoded.expectedEquippedId());
            default -> null;
        };
        if (result == null) {
            replyState(context, player, "INVALID_OPERATION", decoded.operation());
            return;
        }
        ProgressionNetwork.replyState(context, player, "", "", result.code(), result.detail());
    }

    static void replyState(IPayloadContext context, ServerPlayer player, String resultCode, String resultDetail) {
        var server = player.level().getServer();
        if (server == null) throw new IllegalStateException("server unavailable for equipment snapshot");
        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        context.reply(EquipmentNetworkPayloads.SnapshotS2C.from(
                progress,
                definitions,
                TurnboundRe.EQUIPMENT_FORGE.materialCounts(player, definitions),
                EquipmentForgeService.forgeAvailable(player),
                resultCode,
                resultDetail));
    }
}
