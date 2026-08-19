package kr.moonseungjun.arcanecircle;

import com.mojang.logging.LogUtils;
import kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;
import kr.moonseungjun.arcanecircle.magic.ArcaneLightService;
import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.magic.ArcaneVitalityService;
import kr.moonseungjun.arcanecircle.magic.DestructiveMagicService;
import kr.moonseungjun.arcanecircle.magic.HighUtilitySpellService;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.MageGearService;
import kr.moonseungjun.arcanecircle.magic.RpgScaleService;
import kr.moonseungjun.arcanecircle.magic.SpellCastingService;
import kr.moonseungjun.arcanecircle.magic.SpellGameplayService;
import kr.moonseungjun.arcanecircle.magic.SpellKineticsService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.WorldMagicService;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import kr.moonseungjun.arcanecircle.world.ArcaneEconomyService;
import kr.moonseungjun.arcanecircle.world.ArcaneMageService;
import kr.moonseungjun.arcanecircle.world.ArcaneEncounterService;
import kr.moonseungjun.arcanecircle.world.MagicWorldService;
import kr.moonseungjun.arcanecircle.world.NpcMeteorBarrageService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(ArcaneCircle.MOD_ID)
public final class ArcaneCircle {
    public static final String MOD_ID = "arcanecircle";
    public static final String VERSION = "0.12.1-alpha.47";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcaneCircle(IEventBus modEventBus) {
        SpellCatalog.bootstrap();
        ModItems.register(modEventBus);
        modEventBus.addListener(ArcaneNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RpgScaleService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(MageGearService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ArcaneVitalityService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(HighUtilitySpellService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SpellGameplayService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ArcaneMageService::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ArcaneEncounterService::onDeath);
        NeoForge.EVENT_BUS.addListener(ArcaneVitalityService::onHeal);
        NeoForge.EVENT_BUS.addListener(ArcaneMageService::onInteract);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("Arcane Circle {} loaded with {} classic spells, {} fusion formulae, {} spellbooks and {} staves",
                VERSION, SpellCatalog.spells().size(), SpellCatalog.fusions().size(),
                ModItems.spellbooks().size(), ModItems.all().size());
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MagicPlayerData data = MagicPlayerData.get(((ServerLevel) player.level()).getServer());
        boolean firstAwakening = data.ensureProfile(player);
        ArcaneEconomyService.balance(player);
        grantStarterStaffOnce(player, data);
        grantStarterPrimerOnce(player, data);
        MagicWorldService.onLogin(player, firstAwakening);
        if (firstAwakening) {
            player.sendSystemMessage(Component.literal(
                    "§5[구중 마법학] §f마력핵만 각성했습니다. 아직 익힌 주문은 없습니다."));
            player.sendSystemMessage(Component.literal(
                    "§d초심자 마도서§f를 읽어 1써클 기초 주문을 각인하고, 이후 주문서를 수집하세요."));
            player.sendSystemMessage(Component.literal(
                    "§71~5를 눌러 회로를 전개합니다. 시전시간 0초 주문은 짧게 눌렀다 놓으면 즉시 발동하며, 누른 채 다른 주문을 더하면 융합됩니다."));
            player.sendSystemMessage(Component.literal(
                    "§7높은 써클일수록 하위 주문의 마력 소모·시전시간·재사용 대기시간이 크게 감소합니다."));
        }
        ArcaneNetwork.sync(player);
    }

    private void grantStarterStaffOnce(ServerPlayer player, MagicPlayerData data) {
        if (!data.claimStarterStaff(player)) return;
        giveOrDrop(player, new ItemStack(ModItems.NOVICE_STAFF.get()));
        player.sendSystemMessage(Component.literal(
                "§5[마도구 지원] §f견습 마도봉을 지급했습니다. 주 손이나 보조 손에 들면 효과가 적용됩니다."));
    }

    private void grantStarterPrimerOnce(ServerPlayer player, MagicPlayerData data) {
        if (!data.claimStarterPrimer(player)) return;
        giveOrDrop(player, new ItemStack(ModItems.BEGINNER_GRIMOIRE.get()));
        player.sendSystemMessage(Component.literal(
                "§5[마법 교육 지원] §f초심자 마도서를 지급했습니다. 우클릭해 소모하면 1써클 주문 5종을 익힙니다."));
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        boolean stored = player.getInventory().add(stack);
        if (!stored) player.drop(stack, false);
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArcaneLightService.clear(player);
            ArcaneFieldService.clear(player.getUUID());
            HighUtilitySpellService.clear(player);
            SpellGameplayService.clear(player);
            WorldMagicService.stop(player);
            WorldMagicService.clearVisuals(player);
        }
        SpellCastingService.clearSession(event.getEntity().getUUID());
        ArcaneNoticeService.clear(event.getEntity().getUUID());
        MageGearService.clear(event.getEntity().getUUID());
        SpellKineticsService.clear(event.getEntity().getUUID());
    }

    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ArcaneLightService.clear(player);
        ArcaneFieldService.clear(player.getUUID());
        HighUtilitySpellService.clear(player);
        SpellGameplayService.clear(player);
        SpellCastingService.clearSession(player.getUUID());
        SpellKineticsService.clear(player.getUUID());
        WorldMagicService.stop(player);
        WorldMagicService.clearVisuals(player);
        MagicWorldService.onRespawn(player);
        ArcaneNetwork.sync(player);
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ArcaneLightService.clear(player);
        ArcaneFieldService.clear(player.getUUID());
        HighUtilitySpellService.clear(player);
        SpellGameplayService.clear(player);
        SpellCastingService.clearSession(player.getUUID());
        SpellKineticsService.clear(player.getUUID());
        WorldMagicService.stop(player);
        WorldMagicService.clearVisuals(player);
        ArcaneNetwork.sync(player);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        SpellCastingService.tickCharge(player);
        ArcaneLightService.tick(player);
        SpellKineticsService.tick(player);
        MagicWorldService.tick(player);
        ArcaneEncounterService.tick(player);
        MageGearService.tickMovement(player);
        NpcMeteorBarrageService.tick(level);
        DestructiveMagicService.tick(level);
        if (player.tickCount % 10 == 0) MageGearService.tick(player);
        if (player.tickCount % 4 == 0) ArcaneMageService.tickNear(player);
        SpellGameplayService.tick(level);
        HighUtilitySpellService.tick(level);
        // Run field suppression last: Antimagic/Time Stop must win the current server tick.
        ArcaneFieldService.tick(level);
        MagicPlayerData data = MagicPlayerData.get(level.getServer());
        if (player.tickCount % 10 == 0) data.regenerate(player);
        if (player.tickCount % 5 == 0) ArcaneNetwork.sync(player);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        ArcaneLightService.clearAll(event.getServer());
        SpellGameplayService.clearAll();
        HighUtilitySpellService.clearAll();
        ArcaneFieldService.clearAll();
        DestructiveMagicService.clearAll();
        SpellCastingService.clearAllSessions();
        SpellKineticsService.clearAll();
        NpcMeteorBarrageService.clearAll();
        WorldMagicService.clearAll();
        ArcaneNoticeService.clearAll();
    }
}
