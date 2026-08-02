package kr.moonseungjun.arcanecircle.network;

import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCastingService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;
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
    public static final String PROTOCOL_VERSION = "ninefold-arcana-9";
    private static final Set<String> PAGES = Set.of("atlas", "recipes", "staffs", "core", "academy", "sync");

    private ArcaneNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(GrimoireSnapshotPayload.TYPE, GrimoireSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(RequestGrimoirePayload.TYPE, RequestGrimoirePayload.STREAM_CODEC, ArcaneNetwork::handleRequest);
        registrar.playToServer(BeginCastPayload.TYPE, BeginCastPayload.STREAM_CODEC, ArcaneNetwork::handleBeginCast);
        registrar.playToServer(ReleaseCastPayload.TYPE, ReleaseCastPayload.STREAM_CODEC, ArcaneNetwork::handleReleaseCast);
        registrar.playToServer(QueueFusionPayload.TYPE, QueueFusionPayload.STREAM_CODEC, ArcaneNetwork::handleQueueFusion);
        registrar.playToServer(CommitFusionPayload.TYPE, CommitFusionPayload.STREAM_CODEC, ArcaneNetwork::handleCommitFusion);
        registrar.playToServer(EquipSpellPayload.TYPE, EquipSpellPayload.STREAM_CODEC, ArcaneNetwork::handleEquip);
        registrar.playToServer(PurchaseAcademyItemPayload.TYPE, PurchaseAcademyItemPayload.STREAM_CODEC,
                ArcaneNetwork::handlePurchase);
        registrar.playToServer(ChooseTraditionPayload.TYPE, ChooseTraditionPayload.STREAM_CODEC,
                ArcaneNetwork::handleTradition);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot(player, "sync"));
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

    private static void handlePurchase(PurchaseAcademyItemPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        kr.moonseungjun.arcanecircle.world.ArcaneEconomyService.purchase(player, payload.offerId());
        context.reply(snapshot(player, "academy"));
    }

    private static void handleTradition(ChooseTraditionPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        kr.moonseungjun.arcanecircle.world.ArcaneEconomyService.chooseTradition(player, payload.traditionId());
        context.reply(snapshot(player, "academy"));
    }

    private static void handleEquip(EquipSpellPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        boolean selected = data(player).selectSpell(player, payload.slot(), payload.spellId());
        if (!selected) {
            player.sendSystemMessage(Component.literal("§c[마도서] §f현재 써클에서 사용할 수 없거나 아직 각인되지 않은 주문입니다."));
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
                + ";cooldowns=" + magicData.cooldownSnapshot(player)
                + ";staff_id=" + staff.id()
                + ";staff=" + staff.displayName()
                + ";staff_summary=" + staff.summary()
                + ";staff_mana=" + staff.maxManaBonus()
                + ";staff_cost=" + permille(staff.manaCostMultiplier())
                + ";staff_power=" + permille(staff.powerMultiplier())
                + ";staff_range=" + permille(staff.rangeMultiplier())
                + ";staff_cooldown=" + permille(staff.cooldownMultiplier())
                + ";staff_regen=" + permille(staff.regenMultiplier())
                + ";" + "marks=" + kr.moonseungjun.arcanecircle.world.ArcaneEconomyService.balance(player)
                + ";" + "tradition=" + kr.moonseungjun.arcanecircle.world.ArcaneWorldData
                        .get(((ServerLevel) player.level()).getServer()).tradition(player).name()
                + ";known=" + known
                + ";mastery=" + mastery
                + ";spell_count=" + SpellCatalog.spells().size();
        return new GrimoireSnapshotPayload(page, snapshot);
    }

    private static int permille(double value) {
        return (int) Math.round(value * 1000.0);
    }
}
