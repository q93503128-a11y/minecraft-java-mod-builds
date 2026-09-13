package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.AuthoredEncounterLauncher;
import kr.moonseungjun.turnboundre.battle.EnemyTurnService;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.EquipmentProgress;
import kr.moonseungjun.turnboundre.progression.EquipmentRules;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import kr.moonseungjun.turnboundre.world.BattlePreparationService;
import kr.moonseungjun.turnboundre.world.WorldEncounterAnchorAccessPolicy;
import kr.moonseungjun.turnboundre.world.WorldEncounterAnchorResolver;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative authored encounter launch path for validated in-world anchors. */
final class EncounterLaunchService {
    record Result(
            boolean accepted,
            String code,
            String detail,
            AuthoredEncounterLauncher.Launch launch,
            DefinitionRegistry definitions
    ) {
        Result {
            code = code == null ? "" : code;
            detail = detail == null ? "" : detail;
        }

        static Result rejected(String code, String detail) {
            return new Result(false, code, detail, null, null);
        }

        static Result accepted(AuthoredEncounterLauncher.Launch launch, DefinitionRegistry definitions) {
            return new Result(true, "", "", launch, definitions);
        }
    }

    private EncounterLaunchService() {}

    /** Source-compatible path for server-owned callers that do not have a client preview token. */
    static Result tryLaunchFromAnchor(ServerPlayer player, WorldEncounterAnchorResolver.Resolved resolved) {
        String currentPreparation = BattlePreparationService.preview(player).id();
        return tryLaunchFromAnchor(player, resolved, currentPreparation);
    }

    static Result tryLaunchFromAnchor(
            ServerPlayer player,
            WorldEncounterAnchorResolver.Resolved resolved,
            String expectedPreparationId
    ) {
        if (resolved == null) return Result.rejected("ANCHOR_UNAVAILABLE", "");
        if (player == null) return Result.rejected("INVALID_PLAYER", "");

        MinecraftServer server = player.level().getServer();
        if (server == null) return Result.rejected("SERVER_UNAVAILABLE", "");
        if (TurnboundRe.BATTLES.battleForController(player.getUUID()).isPresent()) {
            return Result.rejected("ALREADY_IN_BATTLE", "");
        }

        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        String encounterId = resolved.encounter().id();
        if (!definitions.encounters().containsKey(encounterId)) {
            return Result.rejected("INVALID_ENCOUNTER", encounterId);
        }

        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        if (WorldEncounterAnchorAccessPolicy.cleared(progress, resolved)) {
            return Result.rejected("ANCHOR_CLEARED", resolved.anchor().locator());
        }
        if (progress.party().isEmpty()) return Result.rejected("EMPTY_PARTY", "");

        List<CharacterProgress> party = new ArrayList<>(progress.party().size());
        Map<String, EquipmentProgress> equipmentByCharacter = new LinkedHashMap<>();
        for (String characterId : progress.party()) {
            CharacterProgress character = progress.characters().get(characterId);
            if (character == null) return Result.rejected("INVALID_PARTY", characterId);
            party.add(character);

            String equipmentId = progress.equippedEquipment().get(characterId);
            if (equipmentId == null) continue;
            EquipmentProgress equipment = progress.equipment().get(equipmentId);
            if (equipment == null || !EquipmentRules.valid(definitions, equipment)) {
                return Result.rejected("INVALID_EQUIPMENT", characterId);
            }
            equipmentByCharacter.put(characterId, equipment);
        }

        BattlePreparationService.Selection preparation = BattlePreparationService.preview(player);
        if (!BattlePreparationService.matchesExpected(expectedPreparationId, preparation)) {
            return Result.rejected("PREPARATION_CHANGED", preparation.id());
        }

        UUID battleId = UUID.randomUUID();
        long battleSeed = player.level().getGameTime()
                ^ player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 21)
                ^ Integer.toUnsignedLong(encounterId.hashCode());
        AuthoredEncounterLauncher.Launch launch = TurnboundRe.AUTHORED_ENCOUNTERS.openVirtualFromAnchor(
                encounterId,
                player.getUUID(),
                party,
                battleId,
                battleSeed,
                resolved.anchor().locator(),
                WorldEncounterAnchorAccessPolicy.repeatable(resolved),
                preparation.bonus(),
                equipmentByCharacter);

        // Consume only after battle construction/registration succeeded. If the selected offhand changed
        // unexpectedly on the same server thread, tear the battle back down rather than granting a free bonus.
        if (!BattlePreparationService.consumeOffhand(player, preparation)) {
            TurnboundRe.BATTLES.cleanup(battleId);
            return Result.rejected("PREPARATION_CHANGED", BattlePreparationService.preview(player).id());
        }

        EnemyTurnService.resolveUntilPlayerOrTerminal(TurnboundRe.BATTLES, launch.battle());
        return Result.accepted(launch, definitions);
    }

    static void replyAccepted(IPayloadContext context, Result result) {
        if (context == null || result == null || !result.accepted() || result.launch() == null || result.definitions() == null) {
            throw new IllegalArgumentException("accepted launch result required");
        }
        context.reply(CharacterPresentationNetworkPayloads.CatalogS2C.from(result.definitions()));
        context.reply(BattleNetworkPayloads.BattleSnapshotS2C.from(
                result.launch().battle(), result.launch().definitionContext(), TurnboundRe.BATTLES));
    }
}
