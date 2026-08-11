package kr.moonseungjun.livingkingdoms;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.livingkingdoms.crime.CrimeManager;
import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.network.LivingKingdomsNetwork;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.skill.SkillCrimeHooks;
import kr.moonseungjun.livingkingdoms.skill.SkillProgressionManager;
import kr.moonseungjun.livingkingdoms.world.ErdenAuthoritativeEconomyManager;
import kr.moonseungjun.livingkingdoms.world.ErdenAuthoredRoadNormalizer;
import kr.moonseungjun.livingkingdoms.world.ErdenCapitalStreamingBuilder;
import kr.moonseungjun.livingkingdoms.world.ErdenCitadelInteriorManager;
import kr.moonseungjun.livingkingdoms.world.ErdenEntranceThresholdManager;
import kr.moonseungjun.livingkingdoms.world.ErdenEntryTraversalAudit;
import kr.moonseungjun.livingkingdoms.world.ErdenKingdomExteriorBuilder;
import kr.moonseungjun.livingkingdoms.world.ErdenKingdomExteriorInventoryManager;
import kr.moonseungjun.livingkingdoms.world.ErdenCargoEscrowAudit;
import kr.moonseungjun.livingkingdoms.world.ErdenCargoEscrowManager;
import kr.moonseungjun.livingkingdoms.world.ErdenDiagnosticDebrisSettler;
import kr.moonseungjun.livingkingdoms.world.ErdenExteriorTicketReaper;
import kr.moonseungjun.livingkingdoms.world.ErdenExteriorLifecycleManager;
import kr.moonseungjun.livingkingdoms.world.ErdenExteriorEstateManager;
import kr.moonseungjun.livingkingdoms.world.ErdenExteriorWorkforceManager;
import kr.moonseungjun.livingkingdoms.world.ErdenLandmarkInteriorManager;
import kr.moonseungjun.livingkingdoms.world.ErdenLivingEconomyManager;
import kr.moonseungjun.livingkingdoms.world.ErdenPopulationCiChunkRetainer;
import kr.moonseungjun.livingkingdoms.world.ErdenPopulationManager;
import kr.moonseungjun.livingkingdoms.world.ErdenTransportManager;
import kr.moonseungjun.livingkingdoms.world.ErdenUrbanInteriorBuilder;
import kr.moonseungjun.livingkingdoms.world.ErdenUrbanMicroInfillManager;
import kr.moonseungjun.livingkingdoms.world.ErdenUrbanLifeManager;
import kr.moonseungjun.livingkingdoms.world.FantasyWorldRules;
import kr.moonseungjun.livingkingdoms.world.LivingRealmWorldManager;
import kr.moonseungjun.livingkingdoms.world.RealmBuildCoordinator;
import kr.moonseungjun.livingkingdoms.world.RealmSitePlanner;
import kr.moonseungjun.livingkingdoms.world.RegionalEcologyManager;
import kr.moonseungjun.livingkingdoms.world.SelectionStagingManager;
import kr.moonseungjun.livingkingdoms.world.StarterNpcManager;
import kr.moonseungjun.livingkingdoms.world.StarterRealmDiagnostics;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import kr.moonseungjun.livingkingdoms.worldgen.LivingWorldgenTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(LivingKingdoms.MOD_ID)
public final class LivingKingdoms {
    public static final String MOD_ID = "livingkingdoms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingKingdoms(IEventBus modEventBus) {
        LivingWorldgenTypes.register(modEventBus);
        FoundationCatalog.bootstrap();
        modEventBus.addListener(LivingKingdomsNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreakAttempt);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onWorkstationInteraction);
        LOGGER.info("Living Kingdoms loaded: {} species, {} homelands, {} backgrounds, {} residences",
                FoundationCatalog.species().size(), FoundationCatalog.homelands().size(),
                FoundationCatalog.backgrounds().size(), FoundationCatalog.residences().size());
    }

    private void onServerStarting(ServerStartingEvent event) {
        OriginProfileManager.initialize(event.getServer());
        StarterRealmDiagnostics.runIfRequested(event.getServer());
    }

    private void onServerTick(ServerTickEvent.Post event) {
        RealmBuildCoordinator.onServerTick(event);
        ErdenCapitalStreamingBuilder.onServerTick(event);
        ErdenKingdomExteriorBuilder.onServerTick(event);
        ErdenExteriorTicketReaper.onServerTick(event);
        ErdenExteriorLifecycleManager.onServerTick(event);
        ErdenExteriorWorkforceManager.onServerTick(event);
        ErdenExteriorEstateManager.onServerTick(event);
        ErdenUrbanInteriorBuilder.onServerTick(event);
        ErdenCitadelInteriorManager.onServerTick(event);
        ErdenLandmarkInteriorManager.onServerTick(event);
        ErdenUrbanMicroInfillManager.onServerTick(event);
        ErdenUrbanLifeManager.onServerTick(event);
        ErdenEntranceThresholdManager.onServerTick(event);
        ErdenEntryTraversalAudit.onServerTick(event);
        ErdenPopulationCiChunkRetainer.onServerTick(event);
        ErdenPopulationManager.onServerTick(event);
        ErdenKingdomExteriorInventoryManager.captureBeforeSupply(event);
        ErdenAuthoritativeEconomyManager.onServerTick(event);
        ErdenKingdomExteriorInventoryManager.materializeAfterSupply(event);
        ErdenLivingEconomyManager.onServerTick(event);
        ErdenTransportManager.onServerTick(event);
        ErdenCargoEscrowManager.onServerTick(event);
        ErdenCargoEscrowAudit.onServerTick(event);
        ErdenDiagnosticDebrisSettler.onServerTick(event);
        ErdenAuthoredRoadNormalizer.onServerTick(event);
        StarterRealmDiagnostics.onServerTick(event);
        RegionalEcologyManager.onServerTick(event);
    }

    private void onChunkLoad(ChunkEvent.Load event) {
        ErdenCapitalStreamingBuilder.onChunkLoad(event);
        ErdenKingdomExteriorBuilder.onChunkLoad(event);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (OriginProfileManager.requiresSelection(player.getUUID())) {
            SelectionStagingManager.ensure(player);
            OriginProfileManager.requestSelection(player);
            return;
        }
        OriginProfileManager.profile(player.getUUID()).ifPresent(profile -> {
            LivingRealmWorldManager.requestPlacement(player, profile);
            SkillProgressionManager.state(player);
            RealmEconomyManager.account(player);
            RealmEconomyManager.sync(player);
        });
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (OriginProfileManager.requiresSelection(player.getUUID())) {
            SelectionStagingManager.ensure(player);
            OriginProfileManager.requestSelection(player);
            return;
        }
        OriginProfileManager.profile(player.getUUID()).ifPresent(profile -> {
            LivingRealmWorldManager.requestPlacement(player, profile);
            SkillProgressionManager.state(player);
            RealmEconomyManager.account(player);
            RealmEconomyManager.sync(player);
        });
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long gameTime = player.level().getGameTime();
        if (OriginProfileManager.requiresSelection(player.getUUID())) {
            if (gameTime % 20L == 0L) SelectionStagingManager.ensure(player);
            if (gameTime % 40L == 0L) OriginProfileManager.requestSelection(player);
            return;
        }
        OriginProfileManager.profile(player.getUUID()).ifPresent(profile -> {
            ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
            if (realm != null && !RealmSitePlanner.isBuilt(realm, profile.homelandId()) && gameTime % 20L == 0L) {
                SelectionStagingManager.ensure(player);
            }
        });
        FantasyWorldRules.tick(player);
        SkillProgressionManager.tick(player);
        SkillCrimeHooks.tick(player);
        CrimeManager.tickPlayer(player);
        RealmEconomyManager.tick(player);
    }

    private void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && OriginProfileManager.requiresSelection(player.getUUID())) {
            event.setAmount(0.0F);
            return;
        }
        SkillProgressionManager.modifyDamage(event);
        CrimeManager.handleDamage(event);
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (FantasyWorldRules.handleDefeat(event)) return;
        if (event.getEntity() instanceof Villager villager
                && villager.level() instanceof ServerLevel level) {
            ErdenPopulationManager.markDeadIfResident(level, villager);
            ErdenExteriorWorkforceManager.markDeadIfWorker(level, villager);
            ErdenExteriorLifecycleManager.markDeadIfLifecycleResident(level, villager);
        }
        CrimeManager.handleDeath(event);
    }

    private void onBlockBreakAttempt(BreakBlockEvent event) {
        FantasyWorldRules.handleBlockBreak(event);
    }

    private void onBlockBreak(BlockDropsEvent event) {
        SkillProgressionManager.modifyDrops(event);
        CrimeManager.handleBlockBreak(event);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ErdenTransportManager.handleInteraction(event);
        StarterNpcManager.handleInteraction(event);
        ErdenPopulationManager.handleInteraction(event);
        ErdenExteriorLifecycleManager.handleInteraction(event);
        ErdenExteriorWorkforceManager.handleInteraction(event);
        ErdenLivingEconomyManager.handleInteraction(event);
    }

    private void onWorkstationInteraction(PlayerInteractEvent.RightClickBlock event) {
        ErdenKingdomExteriorInventoryManager.onInteraction(event);
        ErdenAuthoritativeEconomyManager.handleInteraction(event);
        FantasyWorldRules.handleWorkstation(event);
    }
}
