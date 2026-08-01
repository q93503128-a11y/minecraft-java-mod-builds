package kr.moonseungjun.villageguardians;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(VillageGuardians.MOD_ID)
public final class VillageGuardians {
    public static final String MOD_ID = "villageguardians";
    public static final Logger LOGGER = LogUtils.getLogger();

    private int maintenanceTicks;

    public VillageGuardians(IEventBus modEventBus) {
        modEventBus.addListener(VillageNetwork::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Village Guardians original fortress, RPG HUD, skill tree, travel, and trading loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        VillageCouncilState.initializeServer(event.getServer());
        VillageProgressionSystem.initializeServer(event.getServer());
        VillageRpgSystem.resetTransientState();
        VillageWorldSystem.resetTransientState();
        VillageStructureHud.reset();
        VillageHudSystem.reset();
        VillageHealthDisplaySystem.reset();
        VillageRaidSystem.resetTransientState(event.getServer());
        maintenanceTicks = 0;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VillageCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setGameMode(GameType.ADVENTURE);
            VillageCouncilState.registerPlayer(player);
            VillageProgressionSystem.registerPlayer(player);
            var server = player.level().getServer();
            if (server != null) {
                VillageCouncilState.enforceFrozenTime(server);
            }
            VillageWorldSystem.ensureFortifiedVillage(player);
            VillageStarterKit.grantOnLogin(player);
            VillageRpgSystem.refreshPlayerPassive(player);
            if (VillageProgressionSystem.isGameOver() && server != null) {
                VillageUiService.openGameOverForAll(server);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.setGameMode(GameType.ADVENTURE);
            VillageWorldSystem.ensureFortifiedVillage(player);
            VillageStarterKit.grantCaller(player);
            VillageRpgSystem.refreshPlayerPassive(player);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (VillageWorldSystem.handleCentralBellInteraction(event)) {
            return;
        }
        if (VillageWorldSystem.handleGateInteraction(event)) {
            return;
        }
        if (VillageTownHallInteraction.handle(event)) {
            return;
        }
        VillageProgressionSystem.handleBuildingInteraction(event);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        VillageStarterKit.handleItemInteraction(event);
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getLevel() instanceof ServerLevel level
                && level.getServer() != null
                && level == level.getServer().overworld()
                && event.getEntity() instanceof Mob mob
                && VillageWorldSystem.isInsideVillageArea(mob.blockPosition())
                && !VillageWorldSystem.isAllowedGameMob(mob)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        VillageWorldSystem.recordCombat(event);
        VillageRpgSystem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        VillageRaidSystem.onLivingDeath(event);
        VillageRpgSystem.handleDeath(event);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        VillageRaidSystem.tick(event.getServer());
        VillageStructureHud.tick(event.getServer());
        VillageHudSystem.tick(event.getServer());
        VillageHealthDisplaySystem.tick(event.getServer());
        VillageEquipmentRules.restoreDurability(event.getServer());
        maintenanceTicks++;
        if (maintenanceTicks >= 20) {
            maintenanceTicks = 0;
            VillageCouncilState.enforceFrozenTime(event.getServer());
            VillageRpgSystem.refreshPassives(event.getServer());
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                player.setGameMode(GameType.ADVENTURE);
            }
        }
    }
}
