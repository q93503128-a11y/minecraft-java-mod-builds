package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import kr.moonseungjun.titanbreak.combat.ReflexDriveService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.station.StationService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

public final class TitanbreakNetwork {
    public static final String PROTOCOL_VERSION = "titanbreak-0-1-alpha10";

    private TitanbreakNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(StatusPayload.TYPE, StatusPayload.STREAM_CODEC);
        registrar.playToClient(StationOpenPayload.TYPE, StationOpenPayload.STREAM_CODEC);
        registrar.playToServer(DriveTogglePayload.TYPE, DriveTogglePayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            boolean installed = hasReflexDrive(player);
            ReflexDriveService.setRequested(player, payload.enabled() && installed);
            sync(player);
        });
        registrar.playToServer(StationActionPayload.TYPE, StationActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) StationService.handleAction(player, payload.action());
        });
        registrar.playToServer(AugmentAbilityPayload.TYPE, AugmentAbilityPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.ability() == AugmentAbilityPayload.HOOK) StationService.useHook(player);
        });
    }

    public static boolean hasReflexDrive(ServerPlayer player) {
        return TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player).hasInstalled("reflex_drive_i");
    }

    public static void openStation(ServerPlayer player, String station) {
        PacketDistributor.sendToPlayer(player, new StationOpenPayload(station));
    }

    public static void sync(ServerPlayer player) {
        TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
        boolean active = ReflexDriveService.active(player.getUUID());
        String installed = state.installedView().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(entry -> entry.getKey().name() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
        String snapshot = "sanity=" + one(state.sanity())
                + ";heat=" + one(state.heat())
                + ";rd=" + state.researchData()
                + ";adaptLevel=" + state.adaptationLevel()
                + ";adaptXp=" + state.adaptationXp()
                + ";adaptNext=" + TitanPlayerData.xpForNext(state.adaptationLevel())
                + ";apt=" + state.adaptationPoints()
                + ";normalSeen=" + state.normalFirstKillCount()
                + ";eliteSeen=" + state.eliteFirstKillCount()
                + ";bossSeen=" + (state.hasBossFirstKill("the_pursuer") ? 1 : 0)
                + ";installed=" + installed
                + ";surgeryTicks=" + StationService.remainingTicks(player)
                + ";jamTicks=" + AnalysisJammingService.remainingTicks(player)
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
