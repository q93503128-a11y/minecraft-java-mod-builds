package kr.moonseungjun.anomalousclasses;

import kr.moonseungjun.anomalousclasses.command.ClassCommands;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.state.ClassSelectionService;
import kr.moonseungjun.anomalousclasses.state.CombatTracker;
import kr.moonseungjun.anomalousclasses.state.ClassRosterCatalog;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@Mod(AnomalousClasses.MOD_ID)
public final class AnomalousClasses {
    public static final String MOD_ID = "anomalousclasses";

    public AnomalousClasses(IEventBus modEventBus) {
        ClassRosterCatalog.validate();
        ClassNetwork.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ClassSelectionService.onPlayerLogin(event.getEntity());
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CombatTracker.remove(event.getEntity().getUUID());
    }

    private void onLivingDamage(LivingDamageEvent.Post event) {
        CombatTracker.onDamage(event);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ClassCommands.register(event.getDispatcher());
    }
}
