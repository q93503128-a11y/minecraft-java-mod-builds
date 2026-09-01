package kr.moonseungjun.titanbreak.network;

import kr.moonseungjun.titanbreak.augmentation.AugmentIntegrityService;
import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import kr.moonseungjun.titanbreak.combat.AugmentAbilityService;
import kr.moonseungjun.titanbreak.combat.CombatAutopilotService;
import kr.moonseungjun.titanbreak.combat.LegAugmentationService;
import kr.moonseungjun.titanbreak.combat.NeuralCombatAssistService;
import kr.moonseungjun.titanbreak.combat.NullSuppressionService;
import kr.moonseungjun.titanbreak.combat.OverdriveCirculationService;
import kr.moonseungjun.titanbreak.combat.ReflexDriveService;
import kr.moonseungjun.titanbreak.combat.SpineAugmentationService;
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
    public static final String PROTOCOL_VERSION = "titanbreak-0-1-alpha34";

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
        registrar.playToServer(CombatAssistIntentPayload.TYPE, CombatAssistIntentPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            NeuralCombatAssistService.setRequested(player, payload.active());
            sync(player);
        });
        registrar.playToServer(StationActionPayload.TYPE, StationActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) StationService.handleAction(player, payload.action());
        });
        registrar.playToServer(AugmentAbilityPayload.TYPE, AugmentAbilityPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (abilitySuppressed(player, payload.ability())) {
                sync(player);
                return;
            }
            switch (payload.ability()) {
                case AugmentAbilityPayload.HOOK -> AugmentAbilityService.useHook(player);
                case AugmentAbilityPayload.PHASE_STEP -> {
                    SpineAugmentationService.notePhaseIntent(player);
                    AugmentAbilityService.usePhaseStep(player);
                }
                case AugmentAbilityPayload.ARM_RIGHT -> useArmWithIntegrity(player, AugmentationCatalog.Slot.RIGHT_ARM_MAIN);
                case AugmentAbilityPayload.ARM_LEFT -> useArmWithIntegrity(player, AugmentationCatalog.Slot.LEFT_ARM_MAIN);
                case AugmentAbilityPayload.LEG_JUMP -> LegAugmentationService.useMobilityJump(player);
                case AugmentAbilityPayload.OVERDRIVE -> OverdriveCirculationService.activate(player);
                case AugmentAbilityPayload.COMBAT_AUTOPILOT -> CombatAutopilotService.activate(player);
                default -> { }
            }
        });
    }

    private static boolean abilitySuppressed(ServerPlayer player, int ability) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        return switch (ability) {
            case AugmentAbilityPayload.HOOK ->
                    NullSuppressionService.isSuppressed(player, "wire_hook_arm");
            case AugmentAbilityPayload.PHASE_STEP ->
                    NullSuppressionService.isSuppressed(player, "phase_step_spine");
            case AugmentAbilityPayload.ARM_RIGHT ->
                    slotSuppressed(player, state, AugmentationCatalog.Slot.RIGHT_ARM_MAIN);
            case AugmentAbilityPayload.ARM_LEFT ->
                    slotSuppressed(player, state, AugmentationCatalog.Slot.LEFT_ARM_MAIN);
            case AugmentAbilityPayload.LEG_JUMP ->
                    NullSuppressionService.isSuppressed(player, "jump_booster_legs")
                            || NullSuppressionService.isSuppressed(player, "propulsion_legs");
            case AugmentAbilityPayload.OVERDRIVE ->
                    NullSuppressionService.isSuppressed(player, "overdrive_circulation");
            case AugmentAbilityPayload.COMBAT_AUTOPILOT ->
                    NullSuppressionService.isSuppressed(player, "combat_autopilot");
            default -> false;
        };
    }

    private static boolean slotSuppressed(ServerPlayer player, TitanPlayerData.State state, AugmentationCatalog.Slot slot) {
        String augmentId = state.installed(slot);
        return augmentId != null && NullSuppressionService.isSuppressed(player, augmentId);
    }

    private static void useArmWithIntegrity(ServerPlayer player, AugmentationCatalog.Slot slot) {
        if (!(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        TitanPlayerData.AugmentInstance instance = state.installedInstance(slot);
        double heatBefore = state.heat();
        int masteryBefore = instance == null ? 0 : state.masteryXp(instance.id());

        AugmentAbilityService.useArm(player, slot);

        if (instance == null || !"photon_emitter_arm".equals(instance.id()) || instance.enhancement() < 10
                || heatBefore < 55.0D || state.masteryXp(instance.id()) <= masteryBefore) return;

        int stressSteps = heatBefore >= 80.0D ? 2 : 1;
        if (AugmentIntegrityService.stress(player, state, instance, stressSteps)) sync(player);
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
                + ";integrityDamaged=" + AugmentIntegrityService.damagedCount(state)
                + ";integrityWorst=" + AugmentIntegrityService.worstRank(state)
                + ";assistActive=" + (NeuralCombatAssistService.active(player.getUUID()) ? 1 : 0)
                + ";autopilotTicks=" + CombatAutopilotService.remainingTicks(player)
                + ";overdriveTicks=" + OverdriveCirculationService.remainingTicks(player)
                + ";surgeryTicks=" + StationService.remainingTicks(player)
                + ";jamTicks=" + AnalysisJammingService.remainingTicks(player)
                + ";nullSuppressionTicks=" + NullSuppressionService.remainingTicks(player)
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
