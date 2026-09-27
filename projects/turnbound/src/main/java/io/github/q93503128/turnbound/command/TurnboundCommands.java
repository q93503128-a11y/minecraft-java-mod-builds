package io.github.q93503128.turnbound.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.progression.GachaService;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import io.github.q93503128.turnbound.session.MultiplayerPartyService;
import io.github.q93503128.turnbound.world.CampaignPersistence;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import io.github.q93503128.turnbound.world.DrehmalWorldBinding;
import io.github.q93503128.turnbound.world.DrehmalRouteSurveyPlan;
import io.github.q93503128.turnbound.world.DrehmalRouteSurveyService;
import io.github.q93503128.turnbound.world.ExternalWorldBootstrap;
import io.github.q93503128.turnbound.world.FieldSessionManager;
import io.github.q93503128.turnbound.world.MetaNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TurnboundCommands {
    private TurnboundCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("turnbound")
                .then(Commands.literal("status").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    if (ExternalWorldBootstrap.active(player)) {
                        context.getSource().sendSuccess(() -> Component.literal(
                                "Drehmal 연결 활성 · " + DrehmalWorldBinding.status(player.level().getServer())), false);
                    } else if (io.github.q93503128.turnbound.world.WorldSessionRouter.active(player)) {
                        FieldSessionManager.sendStatus(player);
                    } else {
                        context.getSource().sendSuccess(() -> Component.literal(
                                "TURNBOUND 월드 런타임 비활성 · " + DrehmalWorldBinding.status(player.level().getServer())), false);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("world")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("status").executes(context -> worldStatus(context.getSource())))
                        .then(Commands.literal("bind_drehmal").executes(context -> bindDrehmal(context.getSource()))))
                .then(Commands.literal("survey")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("route").executes(context -> surveyRoute(context.getSource())))
                        .then(Commands.literal("here").executes(context -> surveyHere(context.getSource()))))
                .then(Commands.literal("profile").executes(context -> profile(context.getSource())))
                .then(Commands.literal("party")
                        .then(Commands.literal("status").executes(context -> partyStatus(context.getSource())))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> partyInvite(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("accept").executes(context -> partyAccept(context.getSource())))
                        .then(Commands.literal("decline").executes(context -> partyDecline(context.getSource())))
                        .then(Commands.literal("leave").executes(context -> partyLeave(context.getSource()))))
                .then(Commands.literal("archive")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("single").executes(context -> summon(context.getSource(), 1, false)))
                        .then(Commands.literal("ten").executes(context -> summon(context.getSource(), 10, false)))
                        .then(Commands.literal("starter").executes(context -> summon(context.getSource(), 10, true))))
                .then(Commands.literal("grant")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(currencyNode("gold", PlayerProfile.Currency.GOLD))
                        .then(currencyNode("crystal", PlayerProfile.Currency.SUMMON_CRYSTAL))
                        .then(currencyNode("essence", PlayerProfile.Currency.STAR_ESSENCE))
                        .then(currencyNode("core", PlayerProfile.Currency.AWAKENING_CORE)))
                .then(Commands.literal("battle")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            BattleSessionManager.start(player);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.literal("capital_valley").executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            BattleSessionManager.startEncounter(player, "CV_FIRST_COMMON", true, true);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("drabyel_road").executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            BattleSessionManager.startEncounter(player, "CV_DRABYEL_ROAD", true, true);
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(Commands.literal("warning_cave").executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            BattleSessionManager.startEncounter(player, "CV_WARNING_CAVE_ELITE", true, true);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(Commands.literal("leave")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            BattleSessionManager.end(player);
                            return Command.SINGLE_SUCCESS;
                        })));
    }

    private static int surveyRoute(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Capital Valley 26.2 조사 시드"), false);
        for (String line : DrehmalRouteSurveyPlan.routeSeedLines()) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int surveyHere(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var inspection = DrehmalRouteSurveyService.inspect(player);
        source.sendSuccess(() -> Component.literal("현재 위치 조사 · " + inspection.summary()), false);
        source.sendSuccess(() -> Component.literal("site position: " + inspection.positionJson()), false);
        source.sendSuccess(() -> Component.literal("arena candidate: " + inspection.arenaJson()), false);
        if (!inspection.siteGeometryPass()) {
            source.sendSuccess(() -> Component.literal(
                    "이 위치는 site 후보로 바로 확정하지 말고 주변의 평탄하고 절벽이 없는 지점을 다시 확인하십시오."), false);
        }
        if (!inspection.fourPlayerArenaOpen()) {
            source.sendSuccess(() -> Component.literal(
                    "이 위치는 최대 4인 공유전투 formation/camera 검사에 실패했습니다. battle footprint 후보로 사용하지 마십시오."), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int worldStatus(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var server = player.level().getServer();
        boolean bound = DrehmalWorldBinding.isBound(server);
        source.sendSuccess(() -> Component.literal(bound
                ? "세계 연결이 완료되어 있습니다."
                : "Drehmal 세계 연결이 필요합니다."), false);
        return bound ? Command.SINGLE_SUCCESS : 0;
    }

    private static int bindDrehmal(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        try {
            DrehmalWorldBinding.bindManual(player);
            ExternalWorldBootstrap.initialize(player);
            source.sendSuccess(() -> Component.literal("Drehmal 세계 연결을 완료했습니다."), false);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("세계 연결을 완료하지 못했습니다. " + bindFailureText(exception)));
            return 0;
        }
    }

    private static String bindFailureText(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null) return "현재 세계 상태를 확인해 주세요.";
        if (message.contains("stand near")) return "New Drabyel 근처에서 다시 시도해 주세요.";
        if (message.contains("dimension")) return "오버월드에서 다시 시도해 주세요.";
        if (message.contains("different TURNBOUND profile")) return "이미 다른 세계 연결 정보가 존재합니다.";
        if (message.contains("write TURNBOUND")) return "세계 연결 정보를 저장하지 못했습니다.";
        if (message.contains("server unavailable")) return "현재 세계에 연결할 수 없습니다.";
        return "설치된 Drehmal 세계와 위치를 확인해 주세요.";
    }

    private static int partyStatus(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var snapshot = MultiplayerPartyService.snapshot(player.getUUID());
        var server = player.level().getServer();
        String names = snapshot.members().stream().map(id -> {
            var member = server == null ? null : server.getPlayerList().getPlayer(id);
            return member == null ? id.toString().substring(0, 8) : member.getName().getString();
        }).reduce((a,b) -> a + ", " + b).orElse(player.getName().getString());
        source.sendSuccess(() -> Component.literal("플레이어 파티 " + snapshot.members().size() + "/" + MultiplayerPartyService.MAX_PLAYERS
                + " · " + names), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int partyInvite(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        try {
            MultiplayerPartyService.invite(player, target);
            source.sendSuccess(() -> Component.literal(target.getName().getString() + "에게 파티 초대를 보냈습니다."), false);
            target.sendSystemMessage(Component.literal(player.getName().getString()
                    + "의 플레이어 파티 초대 · /turnbound party accept 또는 /turnbound party decline"));
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal(ex.getMessage() == null ? "파티 초대를 보낼 수 없습니다." : ex.getMessage()));
            return 0;
        }
    }

    private static int partyAccept(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        try {
            var snapshot = MultiplayerPartyService.accept(player);
            source.sendSuccess(() -> Component.literal("플레이어 파티에 참가했습니다. " + snapshot.members().size()
                    + "/" + MultiplayerPartyService.MAX_PLAYERS), false);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException ex) {
            source.sendFailure(Component.literal(ex.getMessage() == null ? "파티에 참가할 수 없습니다." : ex.getMessage()));
            return 0;
        }
    }

    private static int partyDecline(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        MultiplayerPartyService.decline(player);
        source.sendSuccess(() -> Component.literal("파티 초대를 거절했습니다."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int partyLeave(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (BattleSessionManager.exists(player)) {
            source.sendFailure(Component.literal("전투 중에는 플레이어 파티를 변경할 수 없습니다."));
            return 0;
        }
        MultiplayerPartyService.leave(player);
        source.sendSuccess(() -> Component.literal("플레이어 파티에서 나왔습니다."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> currencyNode(
            String literal, PlayerProfile.Currency currency) {
        return Commands.literal(literal)
                .then(Commands.argument("amount", LongArgumentType.longArg(1L))
                        .executes(context -> grantCurrency(context.getSource(), currency,
                                LongArgumentType.getLong(context, "amount"))));
    }

    private static int profile(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var snapshot = CampaignProgressStore.snapshot(player.getUUID()).profile();
        String text = "골드 " + snapshot.gold()
                + " | 소환 수정 " + snapshot.summonCrystal()
                + " | 별의 정수 " + snapshot.starEssence()
                + " | 각성 코어 " + snapshot.awakeningCore()
                + " | ★5 천장 " + snapshot.fiveStarPity() + "/80"
                + " | 초기 소환 " + (snapshot.starterArchiveUsed() ? "완료" : snapshot.starterArchiveUnlocked() ? "이용 가능" : "잠김")
                + " | 보유 캐릭터 " + snapshot.ownedCharacters().size();
        source.sendSuccess(() -> Component.literal(text), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int grantCurrency(CommandSourceStack source, PlayerProfile.Currency currency, long amount) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var state = CampaignProgressStore.snapshot(player.getUUID());
        var profile = state.profile();
        long gold = profile.gold();
        long crystal = profile.summonCrystal();
        long essence = profile.starEssence();
        long core = profile.awakeningCore();
        try {
            switch (currency) {
                case GOLD -> gold = Math.addExact(gold, amount);
                case SUMMON_CRYSTAL -> crystal = Math.addExact(crystal, amount);
                case STAR_ESSENCE -> essence = Math.addExact(essence, amount);
                case AWAKENING_CORE -> core = Math.addExact(core, amount);
            }
        } catch (ArithmeticException overflow) {
            source.sendFailure(Component.literal("재화 수치가 너무 큽니다."));
            return 0;
        }

        PlayerProfile.Snapshot updatedProfile = new PlayerProfile.Snapshot(
                gold, crystal, essence, core,
                profile.ownedCharacters(), profile.fiveStarPity(),
                profile.starterArchiveUnlocked(), profile.starterArchiveUsed(),
                profile.summonHistory(), profile.partyPresets());
        CampaignProgressStore.restore(player.getUUID(), new CampaignProgressStore.Snapshot(
                updatedProfile, state.characters(), state.growth(), state.equipment(), state.quests(),
                state.activeParty(), state.clearedEncounters(), state.orphanedCharacterIds(), state.orphanedEquipmentIds()));
        CampaignPersistence.save(player);
        MetaNetwork.sync(player);
        long total = CampaignProgressStore.currency(player.getUUID(), currency);
        source.sendSuccess(() -> Component.literal(currencyLabel(currency)
                + " +" + amount + " → " + total), false);
        return Command.SINGLE_SUCCESS;
    }

    private static String currencyLabel(PlayerProfile.Currency currency) {
        return switch (currency) {
            case GOLD -> "Gold";
            case SUMMON_CRYSTAL -> "Crystal";
            case STAR_ESSENCE -> "Essence";
            case AWAKENING_CORE -> "Core";
        };
    }

    private static int summon(CommandSourceStack source, int count, boolean starter) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        try {
            GachaService.BatchResult result = starter
                    ? CampaignProgressStore.summonStarter(player.getUUID())
                    : CampaignProgressStore.summonStandard(player.getUUID(), count);
            CampaignPersistence.saveIfDirty(player);
            source.sendSuccess(() -> Component.literal(summarize(result, starter)), false);
            return Command.SINGLE_SUCCESS;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            source.sendFailure(Component.literal(summonFailureText(ex)));
            return 0;
        }
    }

    private static String summonFailureText(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null) return "현재 소환을 진행할 수 없습니다.";
        if (message.contains("Not enough Summon Crystal")) return "소환 수정이 부족합니다.";
        if (message.contains("Starter Archive is not available")) return "초기 소환을 이용할 수 없습니다.";
        return "현재 소환을 진행할 수 없습니다.";
    }

    private static String summarize(GachaService.BatchResult result, boolean starter) {
        StringBuilder out = new StringBuilder(starter ? "초기 소환: " : "소환 결과: ");
        for (int i = 0; i < result.pulls().size(); i++) {
            GachaService.PullResult pull = result.pulls().get(i);
            if (i > 0) out.append(" / ");
            String name;
            try {
                name = CanonicalData.definition(pull.characterId()).name();
            } catch (RuntimeException ignored) {
                name = "알 수 없는 인물";
            }
            out.append('★').append(pull.nativeStars()).append(' ').append(name);
            if (pull.newlyOwned()) out.append(" 신규");
            else out.append(" +Essence ").append(pull.starEssenceGranted());
        }
        out.append(" | Crystal -").append(result.crystalSpent());
        return out.toString();
    }
}
