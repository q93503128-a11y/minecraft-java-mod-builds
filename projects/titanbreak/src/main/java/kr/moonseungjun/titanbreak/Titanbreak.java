package kr.moonseungjun.titanbreak;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.titanbreak.augmentation.AugmentationEffectService;
import kr.moonseungjun.titanbreak.combat.AnalysisJammingService;
import kr.moonseungjun.titanbreak.combat.AugmentedMobilityService;
import kr.moonseungjun.titanbreak.combat.HuntRewardService;
import kr.moonseungjun.titanbreak.combat.ReflexDriveService;
import kr.moonseungjun.titanbreak.combat.ReflexFieldService;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import kr.moonseungjun.titanbreak.player.VanillaArmorLockout;
import kr.moonseungjun.titanbreak.registry.ModBlocks;
import kr.moonseungjun.titanbreak.registry.ModEntities;
import kr.moonseungjun.titanbreak.registry.ModItems;
import kr.moonseungjun.titanbreak.station.StationService;
import kr.moonseungjun.titanbreak.world.EncounterDirector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(Titanbreak.MOD_ID)
public final class Titanbreak {
    public static final String MOD_ID = "titanbreak";
    public static final String VERSION = "0.1.0-alpha.13";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final double OVERHEAT_LOCK = 95.0D;
    private static final double OVERHEAT_RESTART = 45.0D;

    public Titanbreak(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        modEventBus.addListener(TitanbreakNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(HuntRewardService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(StationService::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(StationService::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("TITANBREAK {} loaded", VERSION);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TitanPlayerData.get(((ServerLevel) player.level()).getServer()).ensureProfile(player);
        ReflexFieldService.clear(player.getUUID());
        AugmentedMobilityService.clear(player);
        AugmentationEffectService.clear(player);
        AnalysisJammingService.clear(player.getUUID());
        StationService.clear(player.getUUID());
        EncounterDirector.clear(player.getUUID());
        TitanbreakNetwork.sync(player);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ReflexDriveService.clear(player.getUUID());
        ReflexFieldService.clear(player.getUUID());
        AugmentedMobilityService.clear(player);
        AugmentationEffectService.clear(player);
        AnalysisJammingService.clear(player.getUUID());
        StationService.clear(player.getUUID());
        EncounterDirector.clear(player.getUUID());
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ReflexDriveService.clear(player.getUUID());
        ReflexFieldService.clear(player.getUUID());
        AugmentedMobilityService.clear(player);
        AugmentationEffectService.clear(player);
        AnalysisJammingService.clear(player.getUUID());
        StationService.clear(player.getUUID());
        EncounterDirector.clear(player.getUUID());
        TitanPlayerData.get(((ServerLevel) player.level()).getServer()).ensureProfile(player);
        TitanbreakNetwork.sync(player);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        VanillaArmorLockout.tick(player);
        AugmentationEffectService.tick(player, state);
        StationService.tick(player);
        EncounterDirector.tick(player, state);

        boolean installed = TitanbreakNetwork.hasReflexDrive(player);
        if (!installed) ReflexDriveService.setRequested(player, false);

        boolean requested = installed && ReflexDriveService.requested(player.getUUID());
        boolean wasActive = ReflexDriveService.active(player.getUUID());
        boolean active = requested && (wasActive ? state.heat() < OVERHEAT_LOCK : state.heat() < OVERHEAT_RESTART);
        ReflexDriveService.setActive(player, active);
        ReflexFieldService.update(player, active, ReflexDriveService.rating(player.getUUID()), ReflexFieldService.P0_RADIUS);

        if (active) {
            data.setHeat(player, state.heat() + 0.65D);
            data.setSanity(player, state.sanity() - 0.002D);
        } else {
            data.setHeat(player, state.heat() - 0.45D);
        }

        AugmentedMobilityService.clear(player);
        if (player.tickCount % 5 == 0) TitanbreakNetwork.sync(player);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        EncounterDirector.clearAll();
        AnalysisJammingService.clearAll();
        ReflexFieldService.clearAll();
        ReflexDriveService.restore(event.getServer());
    }
}
