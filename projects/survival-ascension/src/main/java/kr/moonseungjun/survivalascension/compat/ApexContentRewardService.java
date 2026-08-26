package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

/**
 * Makes optional-content participation in an Apex hunt matter after the fight as well as during it.
 *
 * Apex mobs already carry owner/archetype markers. When an external escort dies we remember that
 * participation for the short lifetime of the hunt; when the marked Apex boss dies, a surviving
 * external escort or that recent mark can convert the encounter into a Resonance recovery reward.
 * No external implementation classes are linked and the reward pool remains Survival-owned data.
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

        ItemStack reward = ContentPackCompatibility.randomResonanceOperationReward(level.getRandom());
        if (reward.isEmpty()) return;
        int rank = WorldAscensionData.get(level.getServer()).stage() >= 2 ? 3 : 2;
        if (!AscensionAffixes.imprint(reward, level.getRandom(), rank)) return;

        if (!owner.getInventory().add(reward)) owner.drop(reward, false);
        owner.sendSystemMessage(Component.literal("§d[정점 공명 전리품] §f외부 이변 개체가 참여한 정점 사냥에서 공명 장비 1개를 회수했습니다."
                + " §7승천 " + (rank >= 3 ? "III" : "II") + " 각인 포함"));
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
}
