package kr.moonseungjun.arcanecircle.network;

import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.SpellCastingService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Set;
import java.util.stream.Collectors;

public final class ArcaneNetwork {
    public static final String PROTOCOL_VERSION = "ninefold-arcana-3";
    private static final Set<String> PAGES = Set.of("atlas", "mastery", "core", "sync");

    private ArcaneNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(GrimoireSnapshotPayload.TYPE, GrimoireSnapshotPayload.STREAM_CODEC);
        registrar.playToServer(RequestGrimoirePayload.TYPE, RequestGrimoirePayload.STREAM_CODEC, ArcaneNetwork::handleRequest);
        registrar.playToServer(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC, ArcaneNetwork::handleCast);
        registrar.playToServer(EquipSpellPayload.TYPE, EquipSpellPayload.STREAM_CODEC, ArcaneNetwork::handleEquip);
        registrar.playToServer(SelectSlotPayload.TYPE, SelectSlotPayload.STREAM_CODEC, ArcaneNetwork::handleCycle);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, snapshot(player, "sync"));
    }

    private static void handleRequest(RequestGrimoirePayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        String page = PAGES.contains(payload.page()) && !"sync".equals(payload.page()) ? payload.page() : "atlas";
        context.reply(snapshot(player, page));
    }

    private static void handleCast(CastSpellPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        SpellCastingService.cast(player, payload.slot() == 1);
        context.reply(snapshot(player, "sync"));
    }

    private static void handleEquip(EquipSpellPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        boolean selected = data(player).selectSpell(player, payload.slot(), payload.spellId());
        if (!selected) {
            player.sendSystemMessage(Component.literal("§c[마도서] §f현재 써클에서 사용할 수 없는 주문입니다."));
        }
        context.reply(snapshot(player, "sync"));
    }

    private static void handleCycle(SelectSlotPayload payload, IPayloadContext context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) return;
        int command = Math.floorMod(payload.slot(), 4);
        int socket = command >= 2 ? 1 : 0;
        int delta = command == 0 || command == 2 ? -1 : 1;
        String id = data(player).cycleSpell(player, socket, delta);
        SpellDefinition spell = SpellCatalog.spell(id).orElse(null);
        if (spell != null) {
            player.sendOverlayMessage(Component.literal((socket == 0 ? "§d[주력] §f" : "§b[직조] §f") + spell.name()));
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
        MagicPlayerData.MageState state = data(player).state(player);
        String known = state.known().stream().sorted().collect(Collectors.joining("|"));
        String mastery = SpellCatalog.fusions().stream()
                .map(formula -> formula.result() + ":" + state.mastery(formula.result()))
                .collect(Collectors.joining("|"));
        String fusion = SpellCatalog.fusionFor(state.focus(), state.weave())
                .map(SpellCatalog.FusionFormula::result).orElse("");
        String snapshot = "circle=" + state.circle()
                + ";mana=" + (int) state.mana()
                + ";max=" + state.maxMana()
                + ";insight=" + state.insight()
                + ";next=" + state.nextCircleInsight()
                + ";focus=" + state.focus()
                + ";weave=" + state.weave()
                + ";fusion=" + fusion
                + ";known=" + known
                + ";mastery=" + mastery
                + ";spell_count=" + SpellCatalog.spells().size();
        return new GrimoireSnapshotPayload(page, snapshot);
    }
}
