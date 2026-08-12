package kr.moonseungjun.villageguardians;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;
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
        VillageSkillEffectEntities.register(modEventBus);
        modEventBus.addListener(VillageNetwork::registerPayloads);
        NeoForge.EVENT_BUS.register(this);
        LOGGER.info("Village Guardians siege phase 2, RPG, relic and defense systems loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        VillageCouncilState.initializeServer(event.getServer());
        VillageProgressionSystem.initializeServer(event.getServer());
        VillageSkillTreeSystem.initializeServer(event.getServer());
        VillageRoleSkillSystem.initializeServer(event.getServer());
        VillageTowerSpecializationSystem.initializeServer(event.getServer());
        VillageDefenseResearchSystem.initializeServer(event.getServer());
        VillageRelicSystem.initializeServer(event.getServer());
        VillageMercenarySystem.initializeServer(event.getServer());
        VillageSkillTestSystem.initializeServer(event.getServer());
        VillageSiegePersistence.initializeServer(event.getServer());
        VillagePlacedTurretSystem.initializeServer(event.getServer());
        VillageRoleAbilitySystem.reset();
        VillageRpgSystem.resetTransientState();
        VillageWorldSystem.resetTransientState();
        VillageDefenseSystem.reset();
        VillageTowerResearchBonusSystem.reset();
        VillageGatePrioritySystem.reset();
        VillageRespawnSystem.reset();
        VillageStructureHud.reset();
        VillageHudSystem.reset();
        VillageHealthDisplaySystem.reset();
        VillageAttackPlanSystem.reset();
        VillageEnemyEliteSystem.reset();
        VillageSiegeBossSystem.reset();
        VillageMercenaryDeploymentSystem.reset();
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
            if (!VillageRespawnSystem.isDowned(player)) player.setGameMode(GameType.ADVENTURE);
            VillageCouncilState.registerPlayer(player);
            VillageProgressionSystem.registerPlayer(player);
            var server = player.level().getServer();
            if (server != null) VillageCouncilState.enforceFrozenTime(server);
            VillageWorldSystem.ensureFortifiedVillage(player);
            if (player.level() instanceof ServerLevel level) VillageSiegeSegmentSystem.restoreAllVisuals(level);
            VillageSkillTestSystem.recoverStrandedAfterRestart(player);
            VillageStarterKit.grantOnLogin(player);
            VillageRpgSystem.refreshPlayerPassive(player);
            VillageRespawnSystem.onLogin(player);
            VillageRelicSystem.openChoice(player);
            if (VillageProgressionSystem.isGameOver() && server != null) VillageUiService.openGameOverForAll(server);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var server = event.getEntity().level().getServer();
        if (server != null) VillageCouncilState.onPlayerListChanged(server);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!VillageRespawnSystem.isDowned(player)) player.setGameMode(GameType.ADVENTURE);
            VillageWorldSystem.ensureFortifiedVillage(player);
            VillageStarterKit.grantCaller(player);
            VillageRpgSystem.refreshPlayerPassive(player);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (VillageSkillTestSystem.handleManagementBox(event)) return;
        if (VillagePlacedTurretSystem.handlePlacementClick(event)) return;
        if (VillageWorldSystem.handleCentralBellInteraction(event)) return;
        if (VillageWorldSystem.handleGateInteraction(event)) return;
        if (VillageDoorSystem.handle(event)) return;
        if (VillageTownHallInteraction.handle(event)) return;
        if (VillageBuildingInteractionRouter.handle(event)) return;
        VillageProgressionSystem.handleBuildingInteraction(event);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        VillageStarterKit.handleItemInteraction(event);
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        VillageRoleAbilitySystem.handleEntityJoin(event);
        if (event.getLevel().isClientSide()
                || !(event.getLevel() instanceof ServerLevel level)
                || level.getServer() == null
                || level != level.getServer().overworld()
                || !(event.getEntity() instanceof Mob mob)) return;
        if (VillageMercenarySystem.recognize(mob)) return;
        if (VillageSkillTestSystem.recognize(mob)) return;
        if (VillageDefenseSystem.recognizeDefenseMob(mob)) return;
        if (VillageRaidSystem.shouldDiscardStaleRaidEnemy(mob)) {
            event.setCanceled(true);
            mob.discard();
            return;
        }
        if (VillageRaidSystem.isRaidEnemy(mob)) VillageAttackPlanSystem.onRaidEnemyJoin(level, mob);
        if (VillageWorldSystem.isAllowedGameMob(mob)) return;
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        double radius = VillageWorldSystem.BATTLEFIELD_RADIUS + 96.0;
        if (mob.blockPosition().distSqr(center) > radius * radius) return;
        if (!mob.isPersistenceRequired()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        VillageWorldSystem.recordCombat(event);
        VillageRpgSystem.handleIncomingDamage(event);
        VillageRoleAbilitySystem.handleIncomingDamage(event);
    }

    @SubscribeEvent
    public void onFinalDamage(LivingDamageEvent.Pre event) {
        VillageRespawnSystem.handleFinalDamage(event);
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        VillageRaidLootSystem.handleDrops(event);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        boolean raidEnemy = VillageRaidSystem.isRaidEnemy(event.getEntity());
        boolean boss = event.getEntity() instanceof Mob mob && VillageRaidSystem.isBossEnemy(mob);
        var server = event.getEntity().level().getServer();
        if (raidEnemy && server != null) {
            VillageMercenarySystem.awardKillExperience(server, event.getEntity().position());
            if (boss) VillageRelicSystem.offerToParty(server);
        }
        if (event.getEntity() instanceof Mob mob) VillageMercenarySystem.handleDeath(mob);
        VillageRaidSystem.onLivingDeath(event);
        VillageRoleAbilitySystem.handleDeath(event);
        VillageRpgSystem.handleDeath(event);
    }

    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        VillageRoleAbilitySystem.handleArrowLoose(event);
    }

    @SubscribeEvent
    public void onKnockback(LivingKnockBackEvent event) {
        VillageRoleAbilitySystem.handleKnockback(event);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        VillageRoleAbilitySystem.tick(event.getServer());
        VillageRaidSystem.tick(event.getServer());
        VillageAttackPlanSystem.tick(event.getServer());
        VillageEnemyEliteSystem.tick(event.getServer());
        VillageSiegeBossSystem.tick(event.getServer());
        VillagePlacedTurretSystem.tick(event.getServer());
        VillageTowerResearchBonusSystem.tick(event.getServer());
        VillageGatePrioritySystem.tick(event.getServer());
        // v0.18.9: legacy fixed corner firing is retired; visible towers remain fortress architecture.
        VillageMercenarySystem.tick(event.getServer());
        VillageMercenaryDeploymentSystem.tick(event.getServer());
        VillageRespawnSystem.tick(event.getServer());
        VillageStructureHud.tick(event.getServer());
        VillageHudSystem.tick(event.getServer());
        VillageHealthDisplaySystem.tick(event.getServer());
        VillageEquipmentRules.restoreDurability(event.getServer());
        maintenanceTicks++;
        if (maintenanceTicks >= 20) {
            maintenanceTicks = 0;
            VillageCouncilState.enforceFrozenTime(event.getServer());
            VillageGlobalMobPurgeSystem.purge(event.getServer());
            VillageRpgSystem.refreshPassives(event.getServer());
            VillageProgressionSystem.tickInfirmary(event.getServer());
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                if (!VillageRespawnSystem.isDowned(player)) player.setGameMode(GameType.ADVENTURE);
            }
        }
    }
}
