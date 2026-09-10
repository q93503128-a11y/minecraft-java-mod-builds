package kr.moonseungjun.turnboundre.network;

import kr.moonseungjun.turnboundre.TurnboundRe;
import kr.moonseungjun.turnboundre.battle.AuthoredEncounterLauncher;
import kr.moonseungjun.turnboundre.battle.EnemyTurnService;
import kr.moonseungjun.turnboundre.data.DefinitionRegistry;
import kr.moonseungjun.turnboundre.progression.CharacterProgress;
import kr.moonseungjun.turnboundre.progression.PlayerProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared server-authoritative authored encounter launch path for menus and world anchors. */
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

    static Result tryLaunch(ServerPlayer player, String encounterId) {
        if (player == null) return Result.rejected("INVALID_PLAYER", "");
        if (encounterId == null || encounterId.isBlank()) return Result.rejected("INVALID_ENCOUNTER", "");
        MinecraftServer server = player.level().getServer();
        if (server == null) return Result.rejected("SERVER_UNAVAILABLE", "");
        if (TurnboundRe.BATTLES.battleForController(player.getUUID()).isPresent()) {
            return Result.rejected("ALREADY_IN_BATTLE", "");
        }

        DefinitionRegistry definitions = TurnboundRe.DEFINITIONS.snapshot().registry();
        if (!definitions.encounters().containsKey(encounterId)) {
            return Result.rejected("INVALID_ENCOUNTER", encounterId);
        }

        PlayerProgress progress = TurnboundRe.PROGRESS.getOrCreate(server, player.getUUID());
        if (progress.party().isEmpty()) return Result.rejected("EMPTY_PARTY", "");

        List<CharacterProgress> party = new ArrayList<>(progress.party().size());
        for (String characterId : progress.party()) {
            CharacterProgress character = progress.characters().get(characterId);
            if (character == null) return Result.rejected("INVALID_PARTY", characterId);
            party.add(character);
        }

        UUID battleId = UUID.randomUUID();
        long battleSeed = player.level().getGameTime()
                ^ player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 21)
                ^ Integer.toUnsignedLong(encounterId.hashCode());
        AuthoredEncounterLauncher.Launch launch = TurnboundRe.AUTHORED_ENCOUNTERS.openVirtual(
                encounterId, player.getUUID(), party, battleId, battleSeed);
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
