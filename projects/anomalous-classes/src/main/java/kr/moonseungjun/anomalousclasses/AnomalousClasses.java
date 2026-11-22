package kr.moonseungjun.anomalousclasses;

import kr.moonseungjun.anomalousclasses.command.ClassCommands;
import kr.moonseungjun.anomalousclasses.command.PactCommands;
import kr.moonseungjun.anomalousclasses.command.SwarmCommands;
import kr.moonseungjun.anomalousclasses.item.ClassFocusItems;
import kr.moonseungjun.anomalousclasses.item.PuppetItems;
import kr.moonseungjun.anomalousclasses.block.PuppetBlocks;
import kr.moonseungjun.anomalousclasses.block.PactBlocks;
import kr.moonseungjun.anomalousclasses.worldgen.PactWorldgen;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntities;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import kr.moonseungjun.anomalousclasses.entity.SwarmEntities;
import kr.moonseungjun.anomalousclasses.entity.SwarmNodeEntity;
import kr.moonseungjun.anomalousclasses.state.SwarmCasterService;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.state.ClassSelectionService;
import kr.moonseungjun.anomalousclasses.state.CombatTracker;
import kr.moonseungjun.anomalousclasses.state.ClassRosterCatalog;
import kr.moonseungjun.anomalousclasses.state.ClassFocusService;
import kr.moonseungjun.anomalousclasses.state.MentalDominatorConfig;
import kr.moonseungjun.anomalousclasses.state.MentalDominatorService;
import kr.moonseungjun.anomalousclasses.state.PuppetService;
import kr.moonseungjun.anomalousclasses.state.PuppetCombatService;
import kr.moonseungjun.anomalousclasses.state.ParasiteService;
import kr.moonseungjun.anomalousclasses.state.ParasiteTraitService;
import kr.moonseungjun.anomalousclasses.state.ParasiteHostCombatService;
import kr.moonseungjun.anomalousclasses.state.ParasiteHostReachService;
import kr.moonseungjun.anomalousclasses.state.ExternalMobAudit;
import kr.moonseungjun.anomalousclasses.state.RpgExperienceService;
import kr.moonseungjun.anomalousclasses.state.BarrierCasterService;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactService;
import kr.moonseungjun.anomalousclasses.state.PactCovenantService;
import kr.moonseungjun.anomalousclasses.state.PactCombatService;
import kr.moonseungjun.anomalousclasses.state.PactTierTwoCombatService;
import kr.moonseungjun.anomalousclasses.state.PactTierThreeCombatService;
import kr.moonseungjun.anomalousclasses.state.PactTierFourCombatService;
import kr.moonseungjun.anomalousclasses.state.PactTrialService;
import kr.moonseungjun.anomalousclasses.state.PactBlessingService;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.EventPriority;

@Mod(AnomalousClasses.MOD_ID)
public final class AnomalousClasses {
    public static final String MOD_ID = "anomalousclasses";

    public AnomalousClasses(IEventBus modEventBus, ModContainer modContainer) {
        ClassRosterCatalog.validate();
        PactCatalog.all();
        ClassFocusItems.ITEMS.register(modEventBus);
        PuppetBlocks.BLOCKS.register(modEventBus);
        PactBlocks.BLOCKS.register(modEventBus);
        PactWorldgen.FEATURES.register(modEventBus);
        PuppetItems.ITEMS.register(modEventBus);
        PuppetEntities.ENTITIES.register(modEventBus);
        SwarmEntities.ENTITIES.register(modEventBus);
        modEventBus.addListener(this::onEntityAttributes);
        ClassNetwork.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, MentalDominatorConfig.SPEC, "anomalousclasses-server.toml");
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(RpgExperienceService::onLogout);
        NeoForge.EVENT_BUS.addListener(BarrierCasterService::onLogout);
        NeoForge.EVENT_BUS.addListener(PactCombatService::onLogout);
        NeoForge.EVENT_BUS.addListener(SwarmCasterService::onLogout);
        NeoForge.EVENT_BUS.addListener(SwarmCasterService::onDimension);
        NeoForge.EVENT_BUS.addListener(SwarmCasterService::onRespawn);
        NeoForge.EVENT_BUS.addListener(SwarmCasterService::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(SwarmCasterService::onProjectile);
        NeoForge.EVENT_BUS.addListener(SwarmCasterService::tick);
        NeoForge.EVENT_BUS.addListener(PactBlessingService::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(RpgExperienceService::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RpgExperienceService::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(RpgExperienceService::onAdvancementEarned);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RpgExperienceService::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PactCovenantService::onBreak);
        NeoForge.EVENT_BUS.addListener(BarrierCasterService::onIncoming);
        NeoForge.EVENT_BUS.addListener(BarrierCasterService::onProjectile);
        NeoForge.EVENT_BUS.addListener(BarrierCasterService::onDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PactTierFourCombatService::onDeath);
        NeoForge.EVENT_BUS.addListener(PactCombatService::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(PactCombatService::onDeath);
        NeoForge.EVENT_BUS.addListener(PactCombatService::onDamagePre);
        NeoForge.EVENT_BUS.addListener(PactTierTwoCombatService::onDamagePre);
        NeoForge.EVENT_BUS.addListener(PactTierThreeCombatService::onDamagePre);
        NeoForge.EVENT_BUS.addListener(PactTierFourCombatService::onDamagePre);
        NeoForge.EVENT_BUS.addListener(PactTierTwoCombatService::onProjectile);
        NeoForge.EVENT_BUS.addListener(PactCombatService::onDamagePost);
        NeoForge.EVENT_BUS.addListener(PactTierTwoCombatService::onDamagePost);
        NeoForge.EVENT_BUS.addListener(PactTierThreeCombatService::onDamagePost);
        NeoForge.EVENT_BUS.addListener(PactTierFourCombatService::onDamagePost);
        NeoForge.EVENT_BUS.addListener(BarrierCasterService::onChangedDimension);
        NeoForge.EVENT_BUS.addListener(PactCombatService::onDimension);
        NeoForge.EVENT_BUS.addListener(BarrierCasterService::tick);
        NeoForge.EVENT_BUS.addListener(PactCombatService::tick);
        NeoForge.EVENT_BUS.addListener(PactTierTwoCombatService::tick);
        NeoForge.EVENT_BUS.addListener(PactTierThreeCombatService::tick);
        NeoForge.EVENT_BUS.addListener(PactTierFourCombatService::tick);
        NeoForge.EVENT_BUS.addListener(PactTrialService::tick);
        NeoForge.EVENT_BUS.addListener(PactTrialService::onJoin);
        NeoForge.EVENT_BUS.addListener(PactTrialService::onDeath);
        NeoForge.EVENT_BUS.addListener(PactTrialService::onDamagePost);
        NeoForge.EVENT_BUS.addListener(PactTrialService::onDrops);
        NeoForge.EVENT_BUS.addListener(PactTrialService::onExperience);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onLivingDamagePre);
        NeoForge.EVENT_BUS.addListener(PuppetCombatService::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(ParasiteService::onDamage);
        NeoForge.EVENT_BUS.addListener(ParasiteService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ParasiteService::onDeath);
        NeoForge.EVENT_BUS.addListener(ParasiteService::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(ParasiteService::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(ParasiteHostCombatService::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(ParasiteHostReachService::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(ParasiteHostReachService::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ParasiteHostReachService::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(ParasiteHostReachService::onEntityInteractSpecific);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(ParasiteService::tick);
        NeoForge.EVENT_BUS.addListener(ExternalMobAudit::onServerStarted);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RpgExperienceService::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onMobDespawn);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(PuppetService::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(MentalDominatorService::tick);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(PuppetEntities.MARIONETTE.get(), PuppetEntity.createAttributes().build());
        event.put(SwarmEntities.NODE.get(), SwarmNodeEntity.createAttributes().build());
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ClassSelectionService.onPlayerLogin(event.getEntity());
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            MentalDominatorService.onPlayerRespawn(player);
            ParasiteService.onLogin(player);
            ParasiteTraitService.reset(player);
            PactBlessingService.apply(player);
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CombatTracker.remove(event.getEntity().getUUID());
        MentalDominatorService.onPlayerLogout(event.getEntity());
        ParasiteService.onLogout(event.getEntity());
        ParasiteTraitService.logout(event.getEntity().getUUID());
        ParasiteHostCombatService.logout(event.getEntity().getUUID());
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            PactService.expire(player);
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ClassSelectionService.onPlayerRespawn(player);
            ClassFocusService.onPlayerRespawn(player);
            MentalDominatorService.onPlayerRespawn(player);
            ParasiteService.onRespawn(player);
            ParasiteTraitService.reset(player);
            PactBlessingService.apply(player);
        }
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
            ParasiteService.onChangedDimension(player);
    }

    private void onLivingDamage(LivingDamageEvent.Post event) {
        CombatTracker.onDamage(event);
        MentalDominatorService.onLivingDamagePost(event);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ClassCommands.register(event.getDispatcher());
        PactCommands.register(event.getDispatcher());
        SwarmCommands.register(event.getDispatcher());
    }
}
