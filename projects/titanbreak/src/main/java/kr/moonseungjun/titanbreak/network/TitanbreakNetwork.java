package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.combat.ReflexDriveService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Locale;

public final class TitanbreakNetwork {
    public static final String PROTOCOL_VERSION = "titanbreak-0-1-alpha6";

    private TitanbreakNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(StatusPayload.TYPE, StatusPayload.STREAM_CODEC);
        registrar.playToServer(DriveTogglePayload.TYPE, DriveTogglePayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean installed = hasP0ReflexDrive(player);
            ReflexDriveService.setRequested(player, payload.enabled() && installed);
            sync(player);
        });
    }

    public static boolean hasP0ReflexDrive(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.REFLEX_DRIVE_I.get())
                || player.getOffhandItem().is(ModItems.REFLEX_DRIVE_I.get());
    }

    public static void sync(ServerPlayer player) {
        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        boolean active = ReflexDriveService.active(player.getUUID());
        String snapshot = "sanity=" + one(state.sanity())
                + ";heat=" + one(state.heat())
                + ";requested=" + (ReflexDriveService.requested(player.getUUID()) ? 1 : 0)
                + ";active=" + (active ? 1 : 0)
                + ";rating=" + ReflexDriveService.rating(player.getUUID())
                + ";worldRate=" + one(ReflexDriveService.currentWorldTickRate())
                + ";fieldRate=" + one(ReflexDriveService.P0_WORLD_RELATIVE_RATE)
                + ";schema=" + state.schemaVersion();
        PacketDistributor.sendToPlayer(player, new StatusPayload(snapshot));
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
