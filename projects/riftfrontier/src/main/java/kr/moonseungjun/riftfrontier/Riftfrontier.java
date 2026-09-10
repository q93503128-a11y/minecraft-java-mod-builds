package kr.moonseungjun.riftfrontier;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.riftfrontier.combat.PlayerWeaponServerRuntime;
import kr.moonseungjun.riftfrontier.combat.RiftfrontierCombatDataComponents;
import kr.moonseungjun.riftfrontier.content.ContentRuntime;
import kr.moonseungjun.riftfrontier.content.ContentServerReloadListener;
import kr.moonseungjun.riftfrontier.content.bootstrap.CoreContentBootstrap;
import kr.moonseungjun.riftfrontier.diagnostics.RuntimeDiagnosticsCommand;
import kr.moonseungjun.riftfrontier.entity.RiftfrontierEntityTypes;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionGameplayCommand;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionGameplayEvents;
import kr.moonseungjun.riftfrontier.expedition.ExpeditionRestartReconciler;
import kr.moonseungjun.riftfrontier.gametest.CombatAuthorityGameTests;
import kr.moonseungjun.riftfrontier.gametest.CombatGameTests;
import kr.moonseungjun.riftfrontier.gametest.PlayerWeaponGameTests;
import kr.moonseungjun.riftfrontier.gametest.RiftfrontierGameTests;
import kr.moonseungjun.riftfrontier.network.RiftfrontierNetworking;
import kr.moonseungjun.riftfrontier.persistence.RiftfrontierWorldData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(Riftfrontier.MOD_ID)
public final class Riftfrontier {
    public static final String MOD_ID = "riftfrontier";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Riftfrontier(IEventBus modEventBus, ModContainer modContainer) {
        var pack = CoreContentBootstrap.bootstrapAndValidate();
        var report = pack.validation();
        report.warnings().forEach(issue -> LOGGER.warn("[content] {} - {}", issue.source(), issue.message()));

        var snapshot = ContentRuntime.installValidated(pack.registry(), java.util.List.of(pack.packId()));
        LOGGER.info(
            "Riftfrontier core loaded: pack={}, schema={}, definitions={}, generation={}, catalog={}",
            pack.packId(), pack.schemaVersion(), report.definitionCount(), snapshot.generation(), snapshot.fingerprint()
        );

        RiftfrontierCombatDataComponents.register(modEventBus);
        RiftfrontierEntityTypes.register(modEventBus);
        modEventBus.addListener(RiftfrontierEntityTypes::createAttributes);
        RiftfrontierGameTests.register(modEventBus);
        CombatGameTests.register(modEventBus);
        CombatAuthorityGameTests.register(modEventBus);
        PlayerWeaponGameTests.register(modEventBus);
        modEventBus.addListener(RiftfrontierNetworking::registerPayloads);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::addServerReloadListeners);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::registerCommands);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::serverStarted);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::playerWeaponTick);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::playerWeaponLoggedIn);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::playerWeaponLoggedOut);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::playerWeaponChangedDimension);
        NeoForge.EVENT_BUS.addListener(Riftfrontier::playerWeaponClone);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::entityJoinLevel);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::playerClone);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::playerLoggedOut);
        NeoForge.EVENT_BUS.addListener(ExpeditionGameplayEvents::playerLoggedIn);
    }

    private static void addServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(ContentServerReloadListener.ID, new ContentServerReloadListener());
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        RuntimeDiagnosticsCommand.register(event);
        ExpeditionGameplayCommand.register(event);
    }

    private static void serverStarted(ServerStartedEvent event) {
        var snapshot = ContentRuntime.requireCurrent();
        var worldData = RiftfrontierWorldData.get(event.getServer().overworld());
        boolean changed = worldData.synchronizeContentFingerprint(snapshot.fingerprint());
        var reconciled = ExpeditionRestartReconciler.reconcile(event.getServer().overworld());
        if (!reconciled.isEmpty()) {
            LOGGER.warn(
                "Riftfrontier restart reconciliation failed {} non-terminal expedition(s) without refund; sequences={}. Persisted proxy entities will be discarded when loaded and stranded field players will be returned through the login re-entry adapter",
                reconciled.size(),
                reconciled.stream().map(run -> Long.toString(run.sequence())).toList()
            );
        }
        LOGGER.info("Riftfrontier authoritative world root ready: changed={}, {}", changed, worldData.diagnosticSummary());
    }

    private static void playerWeaponTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerWeaponServerRuntime.tickPlayer(player);
    }

    private static void playerWeaponLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerWeaponServerRuntime.clearPlayer(player);
    }

    private static void playerWeaponLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerWeaponServerRuntime.clearPlayer(player);
    }

    private static void playerWeaponChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) PlayerWeaponServerRuntime.clearPlayer(player);
    }

    private static void playerWeaponClone(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide()) PlayerWeaponServerRuntime.clearPlayer(event.getOriginal().getUUID());
    }
}
