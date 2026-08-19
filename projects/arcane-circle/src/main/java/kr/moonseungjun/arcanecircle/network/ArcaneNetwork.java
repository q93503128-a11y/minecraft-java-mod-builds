package kr.moonseungjun.arcanecircle.network;

import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.magic.ArcaneVitalityService;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.MageGearService;
import kr.moonseungjun.arcanecircle.magic.SpellCastingService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellGameplayService;
import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;
import kr.moonseungjun.arcanecircle.world.ArcaneQuestData;
import kr.moonseungjun.arcanecircle.world.ArcaneEncounterData;
import kr.moonseungjun.arcanecircle.world.ArcaneEncounterService;
import kr.moonseungjun.arcanecircle.world.FactionProfile;
import kr.moonseungjun.arcanecircle.world.MageSociety;
import kr.moonseungjun.arcanecircle.world.MagicTradition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ArcaneNetwork {
    public static final String PROTOCOL_VERSION = "ninefold-arcana-12-1-alpha47";
    private static final Set<String> PAGES = Set.of(
            "atlas", "recipes", "staffs", "core", "academy", "quests", "sync");

    private ArcaneNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(GrimoireSnapshotPayload.TYPE, GrimoireSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(WorldMagicPayload.TYPE, WorldMagicPayload.STREAM_CODEC);
        registrar.playToServer(RequestGrimoirePayload.TYPE, RequestGrimoirePayload.STREAM_CODEC, ArcaneNetwork::handleRequest);
        registrar.playToServer(BeginCastPayload.TYPE, BeginCastPayload.STREAM_CODEC, ArcaneNetwork::handleBeginCast);
        registrar.playToServer(ReleaseCastPayload.TYPE, ReleaseCastPayload.STREAM_CODEC, ArcaneNetwork::handleReleaseCast);
        registrar.playToServer(QueueFusionPayload.TYPE, QueueFusionPayload.STREAM_CODEC, ArcaneNetwork::handleQueueFusion);
        registrar.playToServer(CommitFusionPayload.TYPE, CommitFusionPayload.STREAM_CODEC, ArcaneNetwork::handleCommitFusion);
        registrar.playToServer(UseArcaneAbilityPayload.TYPE, UseArcaneAbilityPayload.STREAM_CODEC, ArcaneNetwork::handleArcaneAbility);
        registrar.playToServer(EquipSpellPayload.TYPE, EquipSpellPayload.STREAM_CODEC, ArcaneNetwork::handleEquip);
        registrar.playToServer(PurchaseAcademyItemPayload.TYPE, PurchaseAcademyItemPayload.STREAM_CODEC,
                ArcaneNetwork::handlePurchase);
        registrar.playToServer(ChooseTraditionPayload.TYPE, ChooseTraditionPayload.STREAM_CODEC,
                ArcaneNetwork::handleTradition);
        registrar.playToServer(QuestActionPayload.TYPE, QuestActionPayload.STREAM_CODEC, ArcaneNetwork::handleQuest);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot(player, "sync"));
    }

    public static void openPage(ServerPlayer player, String page) {
        String requested = PAGES.contains(page) && !"sync".equals(page) ? page : "academy";
        PacketDistributor.sendToPlayer(player, snapshot(player, requested));
    }

    private static void handleRequest(RequestGrimoirePayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        String requested = "mastery".equals(payload.page()) ? "recipes" : payload.page();
        String page = PAGES.contains(requested) && !"sync".equals(requested) ? requested : "atlas";
        context.reply(snapshot(player, page));
    }

    private static void handleBeginCast(BeginCastPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        SpellCastingService.beginSlotCharge(player, payload.slot());
        context.reply(snapshot(player, "sync"));
    }

    private static void handleReleaseCast(ReleaseCastPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        SpellCastingService.releaseSlotCharge(player, payload.slot());
        context.reply(snapshot(player, "sync"));
    }

    private static void handleQueueFusion(QueueFusionPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        SpellCastingService.queueFusionSlot(player, payload.slot());
        context.reply(snapshot(player, "sync"));
    }

    private static void handleCommitFusion(CommitFusionPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        if (payload.action() == 0) SpellCastingService.commitFusion(player);
        else {
            SpellCastingService.clearFusion(player, true);
            SpellCastingService.cancelCharge(player, true);
        }
        context.reply(snapshot(player, "sync"));
    }

    private static void handleArcaneAbility(UseArcaneAbilityPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        if (payload.action() != 0) {
            ArcaneNoticeService.push(player, Component.literal("§c[권능] §f알 수 없는 보조 권능 요청입니다."));
        } else {
            SpellGameplayService.useMaintainedAuthority(player);
        }
        context.reply(snapshot(player, "sync"));
    }

    private static void handlePurchase(PurchaseAcademyItemPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        ArcaneEconomyService.purchase(player, payload.offerId());
        context.reply(snapshot(player, "academy"));
    }

    private static void handleTradition(ChooseTraditionPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        ArcaneEconomyService.chooseTradition(player, payload.traditionId());
        context.reply(snapshot(player, "academy"));
    }

    private static void handleQuest(QuestActionPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        ArcaneQuestData quests = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());
        String action = payload.action() == null ? "" : payload.action();
        if ("accept".equals(action)) quests.acceptOffer(player);
        else if ("reject".equals(action)) quests.rejectOffer(player);
        else if (action.startsWith("claim:")) {
            try { quests.claim(player, Integer.parseInt(action.substring("claim:".length()))); }
            catch (NumberFormatException ignored) {
                ArcaneNoticeService.push(player, Component.literal("§c[의뢰] 잘못된 보상 요청입니다."));
            }
        }
        context.reply(snapshot(player, "quests"));
    }

    private static void handleEquip(EquipSpellPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        boolean selected = data(player).selectSpell(player, payload.slot(), payload.spellId());
        if (!selected) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§c[마도서] §f현재 써클에서 사용할 수 없거나 아직 각인되지 않은 주문입니다."));
        }
        context.reply(snapshot(player, "sync"));
    }

    private static MagicPlayerData data(ServerPlayer player) {
        return MagicPlayerData.get(((ServerLevel) player.level()).getServer());
    }

    private static ServerPlayer requirePlayer(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) return player;
        context.disconnect(Component.literal("잘못된 마도서 요청입니다."));
        return null;
    }

    public static GrimoireSnapshotPayload snapshot(ServerPlayer player, String page) {
        MagicPlayerData magicData = data(player);
        MagicPlayerData.MageState state = magicData.state(player);
        MagicPlayerData.EffectiveStats stats = magicData.effectiveStats(player);
        StaffProfile staff = stats.staff();
        MageGearService.GearStats gear = MageGearService.stats(player);
        ArcaneQuestData questData = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());
        List<ArcaneQuestData.QuestStatus> quests = questData.statuses(player);
        ArcaneQuestData.QuestStatus offered = questData.offerStatus(player);
        ArcaneQuestData.QuestStatus legacyQuest = quests.isEmpty() ? ArcaneQuestData.QuestStatus.NONE : quests.getFirst();
        String known = state.known().stream().sorted().collect(Collectors.joining("|"));
        String mastery = SpellCatalog.spells().values().stream()
                .map(spell -> spell.id() + ":" + state.mastery(spell.id()))
                .collect(Collectors.joining("|"));
        String slots = String.join("|", state.slots());
        List<String> queue = SpellCastingService.pendingFusion(player);
        String queued = String.join("|", queue);
        String result = SpellCatalog.fusionFor(queue).map(SpellCatalog.FusionFormula::result).orElse("");
        String candidates = SpellCatalog.candidatesFor(queue).stream()
                .map(SpellCatalog.FusionFormula::result)
                .collect(Collectors.joining("|"));
        String snapshot = "circle=" + state.circle()
                + ";mana=" + (int) state.mana()
                + ";max=" + stats.maxMana()
                + ";health=" + ArcaneVitalityService.effectiveHealth(player)
                + ";health_max=" + ArcaneVitalityService.effectiveMaxHealth(player)
                + ";absorption=" + ArcaneVitalityService.effectiveAbsorption(player)
                + ";regen_milli=" + (int) Math.round(stats.regenPerHalfSecond() * 2000.0)
                + ";insight=" + state.insight()
                + ";next=" + state.nextCircleInsight()
                + ";slots=" + slots
                + ";charging=" + SpellCastingService.chargingSpell(player)
                + ";charging_slot=" + SpellCastingService.chargingSlot(player)
                + ";charge_ticks=" + SpellCastingService.chargingTicks(player)
                + ";charge_required=" + SpellCastingService.chargingRequiredTicks(player)
                + ";queue=" + queued
                + ";queue_result=" + result
                + ";queue_candidates=" + candidates
                + ";queue_extend=" + (SpellCatalog.canExtend(queue) ? 1 : 0)
                + ";fusion_charging=" + SpellCastingService.fusionChargingSpell(player)
                + ";fusion_charge_ticks=" + SpellCastingService.fusionChargingTicks(player)
                + ";fusion_charge_required=" + SpellCastingService.fusionChargingRequiredTicks(player)
                + ";cooldowns=" + magicData.cooldownSnapshot(player)
                + ";staff_id=" + staff.id()
                + ";staff=" + staff.displayName()
                + ";staff_summary=" + staff.summary()
                + ";staff_mana=" + staff.maxManaBonus()
                + ";staff_mana_pct=" + permille(staff.maxManaMultiplier())
                + ";staff_cost=" + permille(staff.manaCostMultiplier())
                + ";staff_power=" + permille(staff.powerMultiplier())
                + ";staff_range=" + permille(staff.rangeMultiplier())
                + ";staff_cooldown=" + permille(staff.cooldownMultiplier())
                + ";staff_regen=" + permille(staff.regenMultiplier())
                + ";gear_hat=" + MageGearService.hatName(player)
                + ";gear_robe=" + MageGearService.robeName(player)
                + ";gear_boots=" + MageGearService.bootsName(player)
                + ";gear_mana=" + gear.maxManaBonus()
                + ";gear_mana_pct=" + permille(gear.maxManaMultiplier())
                + ";gear_health=" + gear.healthBonus()
                + ";gear_health_pct=" + permille(gear.healthMultiplier())
                + ";gear_regen=" + permille(gear.regenMultiplier())
                + ";marks=" + ArcaneEconomyService.balance(player)
                + ";tradition=" + kr.moonseungjun.arcanecircle.world.ArcaneWorldData
                        .get(((ServerLevel) player.level()).getServer()).tradition(player).name()
                + ";known=" + known
                + ";mastery=" + mastery
                + ";notice_seq=" + ArcaneNoticeService.sequence(player)
                + ";notice_ttl=" + ArcaneNoticeService.ttl(player)
                + ";notice=" + ArcaneNoticeService.text(player)
                + ";quest_id=" + legacyQuest.id()
                + ";quest_target=" + legacyQuest.target()
                + ";quest_progress=" + legacyQuest.progress()
                + ";quest_circle=" + legacyQuest.circle()
                + ";quest_reward=" + legacyQuest.reward()
                + ";quest_desc=" + legacyQuest.description()
                + ";" + questSnapshot(offered, quests)
                + ";" + factionSnapshot(player)
                + ";zones=" + ArcaneEncounterService.zoneSummary(player)
                + ";spell_count=" + SpellCatalog.spells().size();
        return new GrimoireSnapshotPayload(page, snapshot);
    }

    private static String questSnapshot(ArcaneQuestData.QuestStatus offered,
                                        List<ArcaneQuestData.QuestStatus> quests) {
        StringBuilder result = new StringBuilder();
        result.append("quest_count=").append(Math.min(ArcaneQuestData.MAX_ACTIVE, quests.size()));
        appendQuest(result, "quest_offer", offered);
        for (int index = 0; index < ArcaneQuestData.MAX_ACTIVE; index++) {
            ArcaneQuestData.QuestStatus quest = index < quests.size()
                    ? quests.get(index) : ArcaneQuestData.QuestStatus.NONE;
            appendQuest(result, "quest_" + index, quest);
        }
        return result.toString();
    }

    private static void appendQuest(StringBuilder result, String prefix, ArcaneQuestData.QuestStatus quest) {
        result.append(';').append(prefix).append("_id=").append(quest.id())
                .append(';').append(prefix).append("_target=").append(quest.target())
                .append(';').append(prefix).append("_progress=").append(quest.progress())
                .append(';').append(prefix).append("_circle=").append(quest.circle())
                .append(';').append(prefix).append("_difficulty=").append(quest.difficulty())
                .append(';').append(prefix).append("_difficulty_name=").append(quest.difficultyName())
                .append(';').append(prefix).append("_reward=").append(quest.reward())
                .append(';').append(prefix).append("_desc=").append(quest.description())
                .append(';').append(prefix).append("_affiliation=").append(quest.affiliation().name());
    }

    private static String factionSnapshot(ServerPlayer player) {
        ArcaneEncounterData data = ArcaneEncounterData.get(((ServerLevel) player.level()).getServer());
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (FactionProfile.Entry entry : FactionProfile.entries()) {
            if (!first) result.append(';');
            first = false;
            MagicTradition tradition = entry.tradition();
            ArcaneEncounterData.Champion champion = data.champion(tradition);
            String prefix = "faction_" + tradition.name().toLowerCase();
            result.append(prefix).append("_representative=").append(entry.representativeName())
                    .append(';').append(prefix).append("_representative_circle=").append(entry.representativeCircle())
                    .append(';').append(prefix).append("_headquarters=").append(entry.headquarters())
                    .append(';').append(prefix).append("_champion=").append(champion.name())
                    .append(';').append(prefix).append("_champion_circle=").append(champion.circle())
                    .append(';').append(prefix).append("_allied=")
                    .append(FactionProfile.namesFor(tradition, MageSociety.Relation.ALLIED))
                    .append(';').append(prefix).append("_friendly=")
                    .append(FactionProfile.namesFor(tradition, MageSociety.Relation.FRIENDLY))
                    .append(';').append(prefix).append("_neutral=")
                    .append(FactionProfile.namesFor(tradition, MageSociety.Relation.NEUTRAL))
                    .append(';').append(prefix).append("_hostile=")
                    .append(FactionProfile.namesFor(tradition, MageSociety.Relation.HOSTILE));
        }
        return result.toString();
    }

    private static int permille(double value) {
        return (int) Math.round(value * 1000.0);
    }
}
