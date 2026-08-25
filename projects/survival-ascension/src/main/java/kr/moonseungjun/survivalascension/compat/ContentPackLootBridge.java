package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.elite.EliteMobSystem;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Adds a bounded second acquisition route for equipment already supplied by the locked content pack.
 * No third-party classes are linked: compatible one-stack gear is discovered from standard item tags.
 */
public final class ContentPackLootBridge {
    private static final double RANK_TWO_BONUS_CHANCE = 0.20D;
    private static final double RANK_THREE_BONUS_CHANCE = 0.45D;

    private ContentPackLootBridge() {}

    public static void onEliteDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        if (killer == null) return;

        int rank = EliteMobSystem.rankId(mob);
        if (rank < 2) return;
        double chance = rank >= 3 ? RANK_THREE_BONUS_CHANCE : RANK_TWO_BONUS_CHANCE;
        if (level.getRandom().nextDouble() >= chance) return;

        ItemStack stack = ContentPackCompatibility.randomAffixGear(level.getRandom(), rank);
        if (stack.isEmpty() || !AscensionAffixes.imprint(stack, level.getRandom(), Math.min(3, rank))) return;
        level.addFreshEntity(new ItemEntity(level, mob.getX(), mob.getY() + 0.65D, mob.getZ(), stack));
        killer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§b[콘텐츠 장비] §f고위 정예가 외부 콘텐츠 장비를 떨어뜨렸습니다. §7승천 옵션이 이미 각인되어 있습니다."));
    }
}
