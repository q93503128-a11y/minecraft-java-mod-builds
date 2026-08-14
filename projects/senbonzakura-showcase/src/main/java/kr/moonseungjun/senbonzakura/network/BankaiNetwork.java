package kr.moonseungjun.senbonzakura.network;

import kr.moonseungjun.senbonzakura.bankai.BankaiService;
import kr.moonseungjun.senbonzakura.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Locale;
import java.util.UUID;

public final class BankaiNetwork {
    public static final String PROTOCOL_VERSION = "senbonzakura-showcase-v6";

    private BankaiNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playBidirectional(BankaiVisualPayload.TYPE, BankaiVisualPayload.STREAM_CODEC,
                BankaiNetwork::handleServerPayload);
    }

    private static void handleServerPayload(BankaiVisualPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!"action=request".equals(payload.state())) return;

        boolean holding = player.getMainHandItem().getItem() == ModItems.SENBONZAKURA.get()
                || player.getOffhandItem().getItem() == ModItems.SENBONZAKURA.get();
        if (!holding) {
            player.sendSystemMessage(Component.literal("[천본앵] 참백도 · 천본앵을 손에 들어야 합니다."));
            return;
        }
        BankaiService.activate(player);
    }

    public static void broadcastStart(UUID caster, Vec3 origin, Vec3 facing, int durationTicks) {
        String state = String.format(Locale.ROOT,
                "action=start;caster=%s;x=%.4f;y=%.4f;z=%.4f;dx=%.5f;dy=%.5f;dz=%.5f;duration=%d",
                caster, origin.x, origin.y, origin.z, facing.x, facing.y, facing.z, durationTicks);
        PacketDistributor.sendToAllPlayers(new BankaiVisualPayload(state));
    }

    public static void broadcastStop(UUID caster) {
        PacketDistributor.sendToAllPlayers(new BankaiVisualPayload("action=stop;caster=" + caster));
    }
}
