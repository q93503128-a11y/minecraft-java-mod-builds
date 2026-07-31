package kr.moonseungjun.villageguardians;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(VillageGuardians.MOD_ID)
public final class VillageGuardians {
    public static final String MOD_ID = "villageguardians";
    public static final Logger LOGGER = LogUtils.getLogger();

    private int maintenanceTicks;

    public VillageGuardians(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Village Guardians governance, RPG, and fortress world core loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        VillageCouncilState.initializeServer(event.getServer());
        VillageRpgSystem.resetTransientState();
        VillageWorldSystem.resetTransientState();
        maintenanceTicks = 0;
        LOGGER.info("Village time, persistent RPG progression, and fortress state initialized");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        VillageCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            VillageCouncilState.registerPlayer(serverPlayer);
            var server = serverPlayer.level().getServer();
            if (server != null) {
                VillageCouncilState.enforceFrozenTime(server);
            }
            VillageWorldSystem.ensureFortifiedVillage(serverPlayer);
            VillageRpgSystem.refreshPlayerPassive(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            VillageWorldSystem.ensureFortifiedVillage(serverPlayer);
            VillageRpgSystem.refreshPlayerPassive(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getEntity() instanceof Mob mob
                && !VillageWorldSystem.isAllowedGameMob(mob)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        VillageRpgSystem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        VillageRpgSystem.handleDeath(event);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        maintenanceTicks++;
        if (maintenanceTicks >= 20) {
            maintenanceTicks = 0;
            VillageCouncilState.enforceFrozenTime(event.getServer());
            VillageRpgSystem.refreshPassives(event.getServer());
        }
    }
}
