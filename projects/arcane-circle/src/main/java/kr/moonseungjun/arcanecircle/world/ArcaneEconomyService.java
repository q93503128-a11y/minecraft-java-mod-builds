
package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.CombatGrowthService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellWorldLore;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ArcaneEconomyService {
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
        long price = offer.basePrice();
        MagicTradition chosen = data(player).tradition(player);
        if (offer.kind() == AcademyOfferCatalog.Kind.SPELLBOOK
                && chosen != MagicTradition.UNBOUND
                && SpellWorldLore.tradition(offer.targetId()) == chosen) {
            price = Math.max(1L, Math.round(price * 0.82));
        }
        return price;
    }

    public static boolean purchase(ServerPlayer player, String offerId) {
        AcademyOfferCatalog.Offer offer = AcademyOfferCatalog.offer(offerId).orElse(null);
        if (offer == null) {
            player.sendSystemMessage(Component.literal("§c[학원 상점] §f존재하지 않는 거래입니다."));
            return false;
        }
        long price = priceFor(player, offer);
        ArcaneWorldData world = data(player);
        if (!world.spendMarks(player, price)) {
            player.sendSystemMessage(Component.literal("§c[학원 상점] §f아르카나가 부족합니다. 필요 "
                    + price + " / 보유 " + world.balance(player)));
            return false;
        }

        ItemStack stack = switch (offer.kind()) {
            case PRIMER -> new ItemStack(ModItems.BEGINNER_GRIMOIRE.get());
            case SPELLBOOK -> new ItemStack(ModItems.spellbook(offer.targetId()).get());
            case STAFF -> new ItemStack(ModItems.staffItem(offer.targetId()).get());
        };
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.sendSystemMessage(Component.literal("§6[학원 상점] §f" + offer.displayName()
                + " 구매 완료 · §d-" + price + " 아르카나 §7(잔액 " + world.balance(player) + ")"));
        return true;
    }

    public static boolean chooseTradition(ServerPlayer player, String traditionId) {
        MagicTradition tradition = MagicTradition.parse(traditionId);
        ArcaneWorldData world = data(player);
        MagicTradition before = world.tradition(player);
        long cost = before == MagicTradition.UNBOUND ? 0L : 5000L;
        if (!world.chooseTradition(player, tradition, cost)) {
            player.sendSystemMessage(Component.literal("§c[학부 변경] §f아르카나가 부족하거나 잘못된 학부입니다."));
            return false;
        }
        player.sendSystemMessage(Component.literal("§5[학부 조율] §f" + tradition.displayName()
                + "에 마력핵을 조율했습니다." + (cost > 0 ? " §7비용 " + cost + " 아르카나" : "")));
        return true;
    }
}
