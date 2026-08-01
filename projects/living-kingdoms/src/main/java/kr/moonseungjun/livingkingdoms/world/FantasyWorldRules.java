package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Realm-wide rules that deliberately replace Minecraft's survival loop.
 *
 * <p>The authored continent is a civic fantasy world, not a wilderness sandbox. Ordinary players
 * live under local law, obtain goods from professions and facilities, and recover from defeat
 * through institutions rather than a vanilla death-and-item-loss loop. Creative and spectator
 * players are left untouched so world builders and diagnostics remain usable.</p>
 */
public final class FantasyWorldRules {
    private static final Set<Block> VANILLA_WORKSTATIONS = Set.of(
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.BLAST_FURNACE,
            Blocks.SMOKER,
            Blocks.SMITHING_TABLE,
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.GRINDSTONE,
            Blocks.STONECUTTER,
            Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE,
            Blocks.LOOM,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.CRAFTER
    );

    private static final Set<String> VANILLA_PRODUCTION_MENUS = Set.of(
            "CraftingMenu",
            "FurnaceMenu",
            "BlastFurnaceMenu",
            "SmokerMenu",
            "SmithingMenu",
            "AnvilMenu",
            "GrindstoneMenu",
            "StonecutterMenu",
            "BrewingStandMenu",
            "EnchantmentMenu",
            "LoomMenu",
            "CartographyTableMenu",
            "CrafterMenu"
    );

    private static final Map<UUID, Long> LAST_RULE_MESSAGE = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 40L;

    private FantasyWorldRules() {
    }

    public static void tick(ServerPlayer player) {
        if (!insideRealm(player)) return;

        // Hunger is not a combat timer here. Meals, inns and feasts become social/economic content
        // instead of a bar that forces the player to eat a stack of bread every few minutes.
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);

        // The personal 2x2 grid is server-authoritatively disabled. Materials remain in the input
        // slots, so an accidental attempt does not consume them.
        if (player.inventoryMenu.slots.size() > 0) {
            var result = player.inventoryMenu.getSlot(0);
            if (result.hasItem()) {
                result.set(ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
                notifyRule(player, "물건은 손바닥 위에서 조합하지 않습니다. 허가된 공방이나 장인을 이용하십시오.");
            }
        }

        // Defense in depth: close a vanilla production menu if another mod or command opened it.
        if (player.containerMenu != player.inventoryMenu
                && VANILLA_PRODUCTION_MENUS.contains(player.containerMenu.getClass().getSimpleName())) {
            player.closeContainer();
            notifyRule(player, "이 설비의 원래 조합법은 폐지되었습니다. 지역 공방의 작업 주문을 이용하십시오.");
        }
    }

    public static void handleWorkstation(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !insideRealm(player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!VANILLA_WORKSTATIONS.contains(event.getLevel().getBlockState(event.getPos()).getBlock())) return;

        event.setCanceled(true);
        notifyRule(player, "개인 제작은 허용되지 않습니다. 길드·공방·연금술원에 작업을 의뢰하십시오.");
    }

    /** City fabric is protected by law; gathering outside surveyed civic limits remains possible. */
    public static void handleBlockBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !insideRealm(player)) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!(player.level() instanceof ServerLevel realm) || !insideCivicZone(realm, event.getPos())) return;
        event.setCanceled(true);
        event.setNotifyClient(true);
        notifyRule(player, "도시 건축물과 공공 기반시설은 시민 재산입니다. 채집은 성외 허가 구역에서 하십시오.");
    }

    /**
     * Converts lethal damage into institutional recovery. Returns true when vanilla death was
     * cancelled and callers must not run death penalties.
     */
    public static boolean handleDefeat(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !insideRealm(player)) return false;
        if (player.isCreative() || player.isSpectator()) return false;

        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        if (realm == null || profile == null) return false;

        event.setCanceled(true);
        BlockPos recovery = RealmSitePlanner.residencePosition(
                realm, profile.homelandId(), profile.residenceId()
        );
        player.removeAllEffects();
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.30F));
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.teleportTo(
                realm,
                recovery.getX() + 0.5D,
                recovery.getY() + 0.2D,
                recovery.getZ() + 0.5D,
                Set.<Relative>of(),
                player.getYRot(),
                player.getXRot(),
                true
        );
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 45, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 20, 0, false, false, true));
        player.sendSystemMessage(
                Component.literal("전투불능 상태에서 구조되었습니다. 소지품은 보존되며 회복에는 시간이 필요합니다.")
        );
        return true;
    }

    private static boolean insideCivicZone(ServerLevel realm, BlockPos position) {
        for (String homeland : Set.of("erden_kingdom", "silvana_forest", "kardum_league")) {
            RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(realm, homeland);
            if (site == null || !site.built()) continue;
            int radius = "erden_kingdom".equals(homeland) ? 235 : 155;
            long dx = position.getX() - site.centerX();
            long dz = position.getZ() - site.centerZ();
            if (dx * dx + dz * dz <= (long) radius * radius) return true;
        }
        return false;
    }

    private static boolean insideRealm(ServerPlayer player) {
        return player.level().dimension().equals(StarterRealmManager.REALM_KEY)
                && OriginProfileManager.profile(player.getUUID()).isPresent();
    }

    private static void notifyRule(ServerPlayer player, String message) {
        long now = player.level().getGameTime();
        long previous = LAST_RULE_MESSAGE.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
        if (now - previous < MESSAGE_COOLDOWN) return;
        LAST_RULE_MESSAGE.put(player.getUUID(), now);
        player.sendSystemMessage(Component.literal(message));
    }
}
