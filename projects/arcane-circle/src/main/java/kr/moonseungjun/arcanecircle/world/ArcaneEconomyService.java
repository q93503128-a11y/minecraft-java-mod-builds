package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.magic.CombatGrowthService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellWorldLore;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ArcaneEconomyService {
    public static final long FIRST_TRADITION_COST = 750L;
    public static final long TRADITION_CHANGE_COST = 5000L;

    private ArcaneEconomyService() {}

    private static ArcaneWorldData data(ServerPlayer player) {
        return ArcaneWorldData.get(((ServerLevel) player.level()).getServer());
    }

    public static long balance(ServerPlayer player) { return data(player).balance(player); }

    public static long awardCombat(ServerPlayer player, CombatGrowthService.Impact impact, int spellCircle) {
        if (impact == null || !impact.meaningful()) return 0L;
        long reward = Math.max(1L, spellCircle)
                * (impact.hits() * 2L + impact.kills() * 14L + impact.strongHits() * 18L
                + impact.strongKills() * 120L + Math.max(0, impact.damage()) / 8L);
        reward = Math.max(1L, reward);
        data(player).addMarks(player, reward);
        return reward;
    }

    public static long priceFor(ServerPlayer player, AcademyOfferCatalog.Offer offer) {
        // Social affiliation does not discount a magical school. All affiliations use one Arcana market.
        return offer.basePrice();
    }

    public static boolean purchase(ServerPlayer player, String offerId) {
        AcademyOfferCatalog.Offer offer = AcademyOfferCatalog.offer(offerId).orElse(null);
        if (offer == null) {
            ArcaneNoticeService.push(player, Component.literal("§c[학원 상점] §f존재하지 않는 거래입니다."));
            return false;
        }
        long price = priceFor(player, offer);
        ArcaneWorldData world = data(player);
        if (!world.spendMarks(player, price)) {
            ArcaneNoticeService.push(player, Component.literal("§c[학원 상점] §f아르카나가 부족합니다. 필요 "
                    + price + " / 보유 " + world.balance(player)));
            return false;
        }

        ItemStack stack = switch (offer.kind()) {
            case PRIMER -> new ItemStack(ModItems.BEGINNER_GRIMOIRE.get());
            case SPELLBOOK -> new ItemStack(ModItems.spellbook(offer.targetId()).get());
            case STAFF -> new ItemStack(ModItems.staffItem(offer.targetId()).get());
            case GEAR -> new ItemStack(ModItems.gearItem(offer.targetId()).get());
        };
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        ArcaneNoticeService.push(player, Component.literal("§6[학원 상점] §f" + offer.displayName()
                + " 구매 완료 · §d-" + price + " 아르카나 §7(잔액 " + world.balance(player) + ")"));
        return true;
    }

    public static long traditionCost(ServerPlayer player, MagicTradition requested) {
        ArcaneWorldData world = data(player);
        if (requested == null || requested == MagicTradition.UNBOUND || world.tradition(player) == requested) return 0L;
        return world.tradition(player) == MagicTradition.UNBOUND ? FIRST_TRADITION_COST : TRADITION_CHANGE_COST;
    }

    public static boolean chooseTradition(ServerPlayer player, String traditionId) {
        MagicTradition tradition = MagicTradition.parse(traditionId);
        ArcaneWorldData world = data(player);
        long cost = traditionCost(player, tradition);
        if (cost == 0L && world.tradition(player) == tradition) {
            ArcaneNoticeService.push(player, Component.literal("§7[소속 등록] §f이미 " + tradition.displayName() + "에 소속되어 있습니다."));
            return true;
        }
        if (!world.chooseTradition(player, tradition, cost)) {
            ArcaneNoticeService.push(player, Component.literal("§c[소속 등록] §f아르카나가 부족하거나 잘못된 소속입니다. 필요 "
                    + cost + " / 보유 " + world.balance(player)));
            return false;
        }
        ArcaneNoticeService.push(player, Component.literal("§5[소속 등록] §f" + tradition.displayName()
                + " 소속으로 등록되었습니다. §7비용 " + cost + " 아르카나"));
        return true;
    }
}
