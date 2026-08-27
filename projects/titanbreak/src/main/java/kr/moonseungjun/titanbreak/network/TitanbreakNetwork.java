package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Locale;

public final class TitanbreakNetwork {
    public static final String PROTOCOL_VERSION = "titanbreak-0-1-alpha1";

    private TitanbreakNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(StatusPayload.TYPE, StatusPayload.STREAM_CODEC);
    }

    public static void sync(ServerPlayer player) {
        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        boolean active = ReflexFieldService.active(player.getUUID());
        String snapshot = "sanity=" + one(state.sanity())
                + ";heat=" + one(state.heat())
                + ";active=" + (active ? 1 : 0)
                + ";rating=" + ReflexFieldService.rating(player.getUUID())
                + ";radius=" + one(ReflexFieldService.radius(player.getUUID()))
                + ";schema=" + state.schemaVersion();
        PacketDistributor.sendToPlayer(player, new StatusPayload(snapshot));
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
