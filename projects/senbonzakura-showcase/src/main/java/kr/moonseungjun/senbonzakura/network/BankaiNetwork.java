package kr.moonseungjun.senbonzakura.network;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Locale;
import java.util.UUID;

public final class BankaiNetwork {
    public static final String PROTOCOL_VERSION = "senbonzakura-showcase-v1";

    private BankaiNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(BankaiVisualPayload.TYPE, BankaiVisualPayload.STREAM_CODEC);
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
