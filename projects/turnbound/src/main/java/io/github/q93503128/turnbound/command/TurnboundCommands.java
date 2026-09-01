package io.github.q93503128.turnbound.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.q93503128.turnbound.combat.P0Scenario;
import io.github.q93503128.turnbound.progression.GachaService;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import io.github.q93503128.turnbound.world.CampaignPersistence;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import io.github.q93503128.turnbound.world.FieldSessionManager;
import io.github.q93503128.turnbound.world.MetaNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TurnboundCommands {
    private TurnboundCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("turnbound")
                .then(Commands.literal("field").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    return FieldSessionManager.enter(player) ? Command.SINGLE_SUCCESS : 0;
                }))
                .then(Commands.literal("status").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    FieldSessionManager.sendStatus(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("profile").executes(context -> profile(context.getSource())))
                .then(Commands.literal("archive")
                        .then(Commands.literal("single").executes(context -> summon(context.getSource(), 1, false)))
                        .then(Commands.literal("ten").executes(context -> summon(context.getSource(), 10, false)))
                        .then(Commands.literal("starter").executes(context -> summon(context.getSource(), 10, true))))
                .then(Commands.literal("test")
                        .then(Commands.literal("give")
                                .then(currencyNode("gold", PlayerProfile.Currency.GOLD))
                                .then(currencyNode("crystal", PlayerProfile.Currency.SUMMON_CRYSTAL))
                                .then(currencyNode("essence", PlayerProfile.Currency.STAR_ESSENCE))
                                .then(currencyNode("core", PlayerProfile.Currency.AWAKENING_CORE))))
                .then(Commands.literal("p0").executes(context -> {
                    String result = P0Scenario.runAutoDiagnostic(160);
                    context.getSource().sendSuccess(() -> Component.literal("TURNBOUND P0: " + result), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("battle").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    BattleSessionManager.start(player);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("leave").executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    BattleSessionManager.end(player);
                    return Command.SINGLE_SUCCESS;
                })));
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
        String text = "Gold " + snapshot.gold()
                + " | Crystal " + snapshot.summonCrystal()
                + " | Essence " + snapshot.starEssence()
                + " | Core " + snapshot.awakeningCore()
                + " | ★5 pity " + snapshot.fiveStarPity() + "/80"
                + " | Starter " + (snapshot.starterArchiveUsed() ? "사용 완료" : snapshot.starterArchiveUnlocked() ? "사용 가능" : "잠김")
                + " | 보유 " + snapshot.ownedCharacters().size();
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
        source.sendSuccess(() -> Component.literal("TURNBOUND TEST · " + currencyLabel(currency)
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
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static String summarize(GachaService.BatchResult result, boolean starter) {
        StringBuilder out = new StringBuilder(starter ? "Starter Archive: " : "Standard Archive: ");
        for (int i = 0; i < result.pulls().size(); i++) {
            GachaService.PullResult pull = result.pulls().get(i);
            if (i > 0) out.append(" / ");
            out.append('★').append(pull.nativeStars()).append(' ').append(pull.characterId());
            if (pull.newlyOwned()) out.append(" 신규");
            else out.append(" +Essence ").append(pull.starEssenceGranted());
        }
        out.append(" | -Crystal ").append(result.crystalSpent());
        return out.toString();
    }
}
