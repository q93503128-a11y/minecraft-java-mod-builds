package kr.moonseungjun.arcanecircle.network;

import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCastingService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Set;
import java.util.stream.Collectors;

public final class ArcaneNetwork {
    public static final String PROTOCOL_VERSION = "ninefold-arcana-2";
    private static final Set<String> PAGES = Set.of("spells", "fusion", "circle", "sync");

    private ArcaneNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(GrimoireSnapshotPayload.TYPE, GrimoireSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(RequestGrimoirePayload.TYPE, RequestGrimoirePayload.STREAM_CODEC, ArcaneNetwork::handleRequest);
        registrar.playToServer(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC, ArcaneNetwork::handleCast);
        registrar.playToServer(SelectSlotPayload.TYPE, SelectSlotPayload.STREAM_CODEC, ArcaneNetwork::handleSelect);
        registrar.playToServer(EquipSpellPayload.TYPE, EquipSpellPayload.STREAM_CODEC, ArcaneNetwork::handleEquip);
        registrar.playToServer(FuseSpellPayload.TYPE, FuseSpellPayload.STREAM_CODEC, ArcaneNetwork::handleFuse);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot(player, "sync"));
    }

    private static void handleRequest(RequestGrimoirePayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        String page = PAGES.contains(payload.page()) && !"sync".equals(payload.page()) ? payload.page() : "spells";
        context.reply(snapshot(player, page));
    }

    private static void handleCast(CastSpellPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        SpellCastingService.cast(player, payload.slot());
        context.reply(snapshot(player, "sync"));
    }

    private static void handleSelect(SelectSlotPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        MagicPlayerData.get(player.getServer()).select(player, payload.slot());
        context.reply(snapshot(player, "sync"));
    }

    private static void handleEquip(EquipSpellPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        boolean equipped = MagicPlayerData.get(player.getServer()).equip(player, payload.slot(), payload.spellId());
        if (!equipped) player.sendSystemMessage(Component.literal("§c[마도서] §f해당 주문을 슬롯에 장착할 수 없습니다."));
        context.reply(snapshot(player, "spells"));
    }

    private static void handleFuse(FuseSpellPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        MagicPlayerData.FusionResult result = MagicPlayerData.get(player.getServer()).fuse(player, payload.resultId());
        player.sendSystemMessage(Component.literal((result.accepted() ? "§d[융합 연구] §f" : "§c[융합 실패] §f")
                + result.message()));
        context.reply(snapshot(player, "fusion"));
    }

    private static ServerPlayer requirePlayer(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) return player;
        context.disconnect(Component.literal("잘못된 마도서 요청입니다."));
        return null;
    }

    public static GrimoireSnapshotPayload snapshot(ServerPlayer player, String page) {
        MagicPlayerData.MageState state = MagicPlayerData.get(player.getServer()).state(player);
        String known = state.known().stream().sorted().collect(Collectors.joining("|"));
        String slots = String.join("|", state.slots());
        String data = "circle=" + state.circle()
                + ";mana=" + (int) state.mana()
                + ";max=" + state.maxMana()
                + ";insight=" + state.insight()
                + ";next=" + state.nextCircleInsight()
                + ";selected=" + state.selected()
                + ";known=" + known
                + ";slots=" + slots
                + ";spell_count=" + SpellCatalog.spells().size();
        return new GrimoireSnapshotPayload(page, data);
    }
}
