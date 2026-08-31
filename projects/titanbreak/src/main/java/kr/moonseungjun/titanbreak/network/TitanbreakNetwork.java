package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import kr.moonseungjun.titanbreak.combat.AugmentAbilityService;
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
    public static final String PROTOCOL_VERSION = "titanbreak-0-1-alpha24";

    private TitanbreakNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(StatusPayload.TYPE, StatusPayload.STREAM_CODEC);
        registrar.playToClient(StationOpenPayload.TYPE, StationOpenPayload.STREAM_CODEC);
        registrar.playToServer(DriveTogglePayload.TYPE, DriveTogglePayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            TitanPlayerData.State state = TitanPlayerData.get(((ServerLevel) player.level()).getServer()).state(player);
            TitanPlayerData.AugmentInstance drive = state.firstInstalledInstance("reflex_drive_i");
            boolean installed = drive != null;
            int rating = ReflexDriveService.ratingForMk(drive == null ? 1 : drive.mk());
            ReflexDriveService.setRequested(player, payload.enabled() && installed, rating);
            sync(player);
        });
        registrar.playToServer(StationActionPayload.TYPE, StationActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) StationService.handleAction(player, payload.action());
        });
        registrar.playToServer(AugmentAbilityPayload.TYPE, AugmentAbilityPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.ability()) {
                case AugmentAbilityPayload.HOOK -> AugmentAbilityService.useHook(player);
                case AugmentAbilityPayload.PHASE_STEP -> AugmentAbilityService.usePhaseStep(player);
                case AugmentAbilityPayload.ARM_RIGHT -> AugmentAbilityService.useArm(player, AugmentationCatalog.Slot.RIGHT_ARM_MAIN);
                case AugmentAbilityPayload.ARM_LEFT -> AugmentAbilityService.useArm(player, AugmentationCatalog.Slot.LEFT_ARM_MAIN);
                default -> { }
            }
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
        String installedMeta = state.installedInstanceView().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(entry -> entry.getKey().name() + ":" + entry.getValue().id() + ":"
                        + entry.getValue().mk() + ":" + entry.getValue().enhancement())
                .collect(Collectors.joining(","));
        String vault = state.vaultView().stream()
                .map(instance -> instance.id() + ":" + instance.mk() + ":" + instance.enhancement())
                .collect(Collectors.joining(","));
        String mastery = AugmentationCatalog.DEFINITIONS.stream()
                .filter(definition -> state.masteryXp(definition.id()) > 0)
                .map(definition -> definition.id() + ":" + state.masteryLevel(definition.id()))
                .collect(Collectors.joining(","));
        TitanPlayerData.AugmentInstance drive = state.firstInstalledInstance("reflex_drive_i");

        AugmentationResourceService.Snapshot resources = AugmentationResourceService.snapshot(state);
        double power = AugmentationResourceService.currentPower(player, state);

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
                + ";installedMeta=" + installedMeta
                + ";vault=" + vault
                + ";vaultCount=" + state.vaultView().size()
                + ";mastery=" + mastery
                + ";driveMk=" + (drive == null ? 0 : drive.mk())
                + ";driveEnh=" + (drive == null ? 0 : drive.enhancement())
                + ";driveMastery=" + state.masteryLevel("reflex_drive_i")
                + ";power=" + one(power)
                + ";powerCap=" + one(resources.powerCapacity())
                + ";heatCap=" + one(resources.heatCapacity())
                + ";neuralCap=" + one(resources.neuralCapacity())
                + ";powerLoad=" + one(resources.powerLoad())
                + ";heatLoad=" + one(resources.heatLoad())
                + ";neuralLoad=" + one(resources.neuralLoad())
                + ";neuralOver=" + (resources.neuralOverloaded() ? 1 : 0)
                + ";surgeryTicks=" + StationService.remainingTicks(player)
                + ";jamTicks=" + AnalysisJammingService.remainingTicks(player)
                + ";requested=" + (ReflexDriveService.requested(player.getUUID()) ? 1 : 0)
                + ";active=" + (active ? 1 : 0)
                + ";rating=" + ReflexDriveService.rating(player.getUUID())
                + ";worldRate=" + one(ReflexDriveService.currentWorldTickRate())
                + ";fieldRate=" + one(ReflexDriveService.BASE_WORLD_RELATIVE_RATE)
                + ";schema=" + state.schemaVersion();
        PacketDistributor.sendToPlayer(player, new StatusPayload(snapshot));
    }

    private static String one(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
