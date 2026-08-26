package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Makes optional-content participation in an Apex hunt matter after the fight as well as during it.
 *
 * Apex mobs already carry owner/archetype markers. When an external escort dies we remember that
 * participation for the short lifetime of the hunt; when the marked Apex boss dies, a surviving
 * external escort or that recent mark can convert the encounter into a Resonance recovery reward.
 * The specific Resonance target is selected by Survival-owned item tags per Apex region, so players
 * can farm a desired equipment piece by choosing where they open the hunt without Java-linking the
 * external item's implementation class or registry ID.
 */
public final class ApexContentRewardService {
    private static final String APEX_OWNER_KEY = "survivalascension_apex_owner";
    private static final String APEX_TYPE_KEY = "survivalascension_apex_type";
    private static final String CONTENT_MARK_TYPE_KEY = "survivalascension_apex_content_mark_type";
    private static final String CONTENT_MARK_TICK_KEY = "survivalascension_apex_content_mark_tick";
    private static final long CONTENT_MARK_LIFETIME_TICKS = 1800L;
    private static final Identifier APEX_HEALTH_ID = Identifier.fromNamespaceAndPath(
            SurvivalAscension.MOD_ID, "apex_health");

    private ApexContentRewardService() {}

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        String ownerText = mob.getPersistentData().getStringOr(APEX_OWNER_KEY, "");
        if (ownerText.isEmpty()) return;

        UUID ownerId;
        try {
            ownerId = UUID.fromString(ownerText);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);

        if (isApexBoss(mob)) {
            if (owner != null) rewardBossIfContentParticipated(owner, level, mob, ownerText);
            return;
        }

        if (owner != null && isExternalContentMob(mob)) {
            owner.getPersistentData().putString(CONTENT_MARK_TYPE_KEY,
                    mob.getPersistentData().getStringOr(APEX_TYPE_KEY, ""));
            owner.getPersistentData().putLong(CONTENT_MARK_TICK_KEY, level.getGameTime());
        }
    }

    private static void rewardBossIfContentParticipated(
            ServerPlayer owner,
            ServerLevel level,
            Mob boss,
            String ownerText) {
        String apexType = boss.getPersistentData().getStringOr(APEX_TYPE_KEY, "");
        boolean participated = hasLivingExternalEscort(level, boss, ownerText)
                || hasRecentParticipationMark(owner, apexType, level.getGameTime());
        clearParticipationMark(owner);
        if (!participated) return;

        RewardFocus focus = focusFor(apexType);
        ItemStack reward = randomFocusedReward(level, focus);
        if (reward.isEmpty()) return;
        int rank = WorldAscensionData.get(level.getServer()).stage() >= 2 ? 3 : 2;
        if (!AscensionAffixes.imprint(reward, level.getRandom(), rank)) return;

        String itemName = reward.getHoverName().getString();
        if (!owner.getInventory().add(reward)) owner.drop(reward, false);
        owner.sendSystemMessage(Component.literal("§d[정점 공명 전리품] §f외부 이변 개체가 참여한 정점 사냥에서 §d"
                + itemName + "§f을 회수했습니다. §7"
                + (focus == null ? "범용 회수" : focus.koreanLabel())
                + " · 승천 " + (rank >= 3 ? "III" : "II") + " 각인 포함"));
    }

    private static ItemStack randomFocusedReward(ServerLevel level, RewardFocus focus) {
        if (focus == null) return ContentPackCompatibility.randomResonanceOperationReward(level.getRandom());
        TagKey<Item> tag = TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, focus.tagPath()));
        List<Item> pool = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null || !item.builtInRegistryHolder().is(tag)) continue;
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty() || stack.getMaxStackSize() != 1) continue;
            pool.add(item);
        }
        if (pool.isEmpty()) return ContentPackCompatibility.randomResonanceOperationReward(level.getRandom());
        return new ItemStack(pool.get(level.getRandom().nextInt(pool.size())));
    }

    private static RewardFocus focusFor(String apexType) {
        return switch (apexType) {
            case "WOODLAND_BREAKER" -> new RewardFocus("apex_resonance_woodland", "수림 목표: 공명 도끼");
            case "ARID_COMMANDER" -> new RewardFocus("apex_resonance_arid", "황야 목표: 공명 삽");
            case "WETLAND_PLAGUEHEART" -> new RewardFocus("apex_resonance_wetland", "습원 목표: 공명 괭이");
            case "HIGHLAND_HUNTER" -> new RewardFocus("apex_resonance_highlands", "능선 목표: 공명 검");
            case "OCEAN_TYRANT" -> new RewardFocus("apex_resonance_ocean", "외해 목표: 공명 장화");
            case "DEEP_STALKER" -> new RewardFocus("apex_resonance_deep", "심층 목표: 공명 곡괭이");
            case "FROZEN_WARDEN" -> new RewardFocus("apex_resonance_frozen", "설원 목표: 공명 흉갑");
            case "NETHER_REAVER" -> new RewardFocus("apex_resonance_nether", "네더 목표: 공명 투구");
            case "END_HARBINGER" -> new RewardFocus("apex_resonance_end", "공허 목표: 공명 각반");
            default -> null;
        };
    }

    private static boolean hasLivingExternalEscort(ServerLevel level, Mob boss, String ownerText) {
        return !level.getEntitiesOfClass(
                Mob.class,
                boss.getBoundingBox().inflate(72.0D),
                candidate -> candidate != boss
                        && candidate.isAlive()
                        && ownerText.equals(candidate.getPersistentData().getStringOr(APEX_OWNER_KEY, ""))
                        && isExternalContentMob(candidate)
        ).isEmpty();
    }

    private static boolean hasRecentParticipationMark(ServerPlayer owner, String apexType, long now) {
        String markedType = owner.getPersistentData().getStringOr(CONTENT_MARK_TYPE_KEY, "");
        long markedTick = owner.getPersistentData().getLongOr(CONTENT_MARK_TICK_KEY, Long.MIN_VALUE);
        return !apexType.isEmpty()
                && apexType.equals(markedType)
                && markedTick != Long.MIN_VALUE
                && now >= markedTick
                && now - markedTick <= CONTENT_MARK_LIFETIME_TICKS;
    }

    private static void clearParticipationMark(ServerPlayer owner) {
        owner.getPersistentData().remove(CONTENT_MARK_TYPE_KEY);
        owner.getPersistentData().remove(CONTENT_MARK_TICK_KEY);
    }

    private static boolean isApexBoss(Mob mob) {
        var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        return maxHealth != null && maxHealth.hasModifier(APEX_HEALTH_ID);
    }

    private static boolean isExternalContentMob(Mob mob) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null) return false;
        return !"minecraft".equals(id.getNamespace()) && !SurvivalAscension.MOD_ID.equals(id.getNamespace());
    }

    private record RewardFocus(String tagPath, String koreanLabel) {}
}
