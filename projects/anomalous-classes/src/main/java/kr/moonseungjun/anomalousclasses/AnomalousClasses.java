package kr.moonseungjun.anomalousclasses;

import kr.moonseungjun.anomalousclasses.command.ClassCommands;
import kr.moonseungjun.anomalousclasses.item.ClassFocusItems;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.state.ClassSelectionService;
import kr.moonseungjun.anomalousclasses.state.CombatTracker;
import kr.moonseungjun.anomalousclasses.state.ClassRosterCatalog;
import kr.moonseungjun.anomalousclasses.state.ClassFocusService;
import kr.moonseungjun.anomalousclasses.state.MentalDominatorConfig;
import kr.moonseungjun.anomalousclasses.state.MentalDominatorService;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(AnomalousClasses.MOD_ID)
public final class AnomalousClasses {
    public static final String MOD_ID = "anomalousclasses";

    public AnomalousClasses(IEventBus modEventBus, ModContainer modContainer) {
        ClassRosterCatalog.validate();
        ClassFocusItems.ITEMS.register(modEventBus);
        ClassNetwork.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, MentalDominatorConfig.SPEC, "anomalousclasses-server.toml");
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onMobDespawn);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::tick);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ClassSelectionService.onPlayerLogin(event.getEntity());
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            MentalDominatorService.onPlayerRespawn(player);
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CombatTracker.remove(event.getEntity().getUUID());
        MentalDominatorService.onPlayerLogout(event.getEntity());
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ClassSelectionService.onPlayerRespawn(player);
            ClassFocusService.onPlayerRespawn(player);
            MentalDominatorService.onPlayerRespawn(player);
        }
    }

    private void onLivingDamage(LivingDamageEvent.Post event) {
        CombatTracker.onDamage(event);
        MentalDominatorService.onLivingDamagePost(event);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ClassCommands.register(event.getDispatcher());
    }
}
