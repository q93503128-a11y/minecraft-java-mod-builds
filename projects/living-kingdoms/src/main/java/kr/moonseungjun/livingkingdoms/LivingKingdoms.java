package kr.moonseungjun.livingkingdoms;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.livingkingdoms.crime.CrimeManager;
import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.network.LivingKingdomsNetwork;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.skill.SkillCrimeHooks;
import kr.moonseungjun.livingkingdoms.skill.SkillProgressionManager;
import kr.moonseungjun.livingkingdoms.world.LivingRealmWorldManager;
import kr.moonseungjun.livingkingdoms.world.StarterNpcManager;
import kr.moonseungjun.livingkingdoms.world.StarterRealmDiagnostics;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(LivingKingdoms.MOD_ID)
public final class LivingKingdoms {
    public static final String MOD_ID = "livingkingdoms";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingKingdoms(IEventBus modEventBus) {
        FoundationCatalog.bootstrap();
        modEventBus.addListener(LivingKingdomsNetwork::register);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);

        LOGGER.info(
                "Living Kingdoms loaded: {} species, {} homelands, {} backgrounds, {} residences",
                FoundationCatalog.species().size(),
                FoundationCatalog.homelands().size(),
                FoundationCatalog.backgrounds().size(),
                FoundationCatalog.residences().size()
        );
    }

    private void onServerStarting(ServerStartingEvent event) {
        OriginProfileManager.initialize(event.getServer());
        StarterRealmDiagnostics.runIfRequested(event.getServer());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OriginProfileManager.requestSelection(player);
            OriginProfileManager.profile(player.getUUID()).ifPresent(profile -> {
                LivingRealmWorldManager.ensureForPlayer(player, profile);
                StarterNpcManager.ensureForPlayer(player, profile);
                SkillProgressionManager.state(player);
            });
        }
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OriginProfileManager.profile(player.getUUID()).ifPresent(profile -> {
                LivingRealmWorldManager.placePlayer(player, profile);
                StarterNpcManager.ensureForPlayer(player, profile);
                SkillProgressionManager.state(player);
            });
        }
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (OriginProfileManager.requiresSelection(player.getUUID())) {
            if (player.level().getGameTime() % 40L == 0L) OriginProfileManager.requestSelection(player);
            return;
        }

        if (player.level().getGameTime() % 200L == 0L) {
            OriginProfileManager.profile(player.getUUID()).ifPresent(profile -> {
                LivingRealmWorldManager.ensureForPlayer(player, profile);
                StarterNpcManager.ensureForPlayer(player, profile);
            });
        }
        SkillProgressionManager.tick(player);
        SkillCrimeHooks.tick(player);
        CrimeManager.tickPlayer(player);
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
        CrimeManager.handleDeath(event);
    }

    private void onBlockBreak(BlockDropsEvent event) {
        SkillProgressionManager.modifyDrops(event);
        CrimeManager.handleBlockBreak(event);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        StarterNpcManager.handleInteraction(event);
    }
}
