package kr.moonseungjun.livingkingdoms.skill;

import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

/** Applies racial traits, secondary tree perks, and action-driven mastery progression. */
public final class SkillProgressionManager {
    private SkillProgressionManager() {
    }

    public static SkillProgressionSavedData.SkillState state(ServerPlayer player) {
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        String species = profile == null ? "human" : profile.speciesId();
        SkillProgressionSavedData data = data(player);
        masteryData(player).state(player.getUUID());
        return data.syncLevel(player.getUUID(), species, player.experienceLevel);
    }

    public static SkillProgressionSavedData.UnlockResult unlock(ServerPlayer player, String skillId) {
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        if (profile == null) {
            return new SkillProgressionSavedData.UnlockResult(false, "먼저 출신 선택을 완료하십시오.",
                    new SkillProgressionSavedData.SkillState(0, 0, List.of()));
        }
        SkillProgressionSavedData data = data(player);
        data.syncLevel(player.getUUID(), profile.speciesId(), player.experienceLevel);
        return data.unlock(player.getUUID(), profile.speciesId(), skillId);
    }

    public static void tick(ServerPlayer player) {
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        if (profile == null) return;
        SkillProgressionSavedData data = data(player);
        data.syncLevel(player.getUUID(), profile.speciesId(), player.experienceLevel);

        long gameTime = player.level().getGameTime();
        if (gameTime % 20L == 0L && player.getDeltaMovement().horizontalDistanceSqr() > 0.0025D) {
            addMastery(player, MasteryProgressionSavedData.EXPLORATION, player.isSprinting() ? 2L : 1L);
        }

        if (!player.level().dimension().equals(StarterRealmManager.REALM_KEY) || gameTime % 100L != 0L) return;

        boolean elfSight = "elf".equals(profile.speciesId());
        if (elfSight || data.has(player.getUUID(), profile.speciesId(), "explore_night_sight")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, true));
        }
        if (data.has(player.getUUID(), profile.speciesId(), "explore_trailblazer")) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0, true, false, true));
        }
    }

    public static void modifyDamage(LivingIncomingDamageEvent event) {
        float originalAmount = Math.max(0.0F, event.getAmount());

        if (event.getEntity() instanceof ServerPlayer victim) {
            OriginProfile profile = OriginProfileManager.profile(victim.getUUID()).orElse(null);
            if (profile != null) {
                addMastery(victim, MasteryProgressionSavedData.DEFENSE,
                        Math.max(1L, Math.round(originalAmount * 2.0F)));
                int defenseLevel = masteryLevel(victim, MasteryProgressionSavedData.DEFENSE);
                float multiplier = defenseMultiplier(defenseLevel);

                SkillProgressionSavedData data = data(victim);
                if ("dwarf".equals(profile.speciesId())) multiplier *= 0.95F;
                if (data.has(victim.getUUID(), profile.speciesId(), "combat_endurance")) multiplier *= 0.95F;
                if (data.has(victim.getUUID(), profile.speciesId(), "combat_last_stand")
                        && victim.getHealth() <= victim.getMaxHealth() * 0.35F) {
                    multiplier *= 0.85F;
                }
                if (data.has(victim.getUUID(), profile.speciesId(), "explore_safe_fall")
                        && event.getSource().is(DamageTypeTags.IS_FALL)) {
                    multiplier *= 0.60F;
                }
                event.setAmount(event.getAmount() * multiplier);
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            OriginProfile profile = OriginProfileManager.profile(attacker.getUUID()).orElse(null);
            if (profile != null) {
                addMastery(attacker, MasteryProgressionSavedData.COMBAT,
                        Math.max(1L, Math.round(originalAmount * 3.0F)));
                float multiplier = combatMultiplier(masteryLevel(attacker, MasteryProgressionSavedData.COMBAT));
                if (data(attacker).has(attacker.getUUID(), profile.speciesId(), "combat_training")) {
                    multiplier *= 1.10F;
                }
                event.setAmount(event.getAmount() * multiplier);
            }
        }
    }

    public static void modifyDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        if (profile == null) return;

        String track = masteryTrack(event);
        int baseXp = 2 + Math.min(6, event.getDrops().size());
        SkillProgressionSavedData data = data(player);
        if (data.has(player.getUUID(), profile.speciesId(), "life_artisan")) {
            baseXp = Math.max(baseXp + 1, Math.round(baseXp * 1.25F));
        }
        addMastery(player, track, baseXp);

        int level = masteryLevel(player, track);
        double yield = Math.log1p(level) / 12.0D;
        if (data.has(player.getUUID(), profile.speciesId(), "life_gatherer")) yield += 0.20D;
        if (!event.getDrops().isEmpty() && yield > 0.0D) {
            int extra = (int) Math.floor(yield);
            double fraction = yield - extra;
            if (event.getLevel().getRandom().nextDouble() < fraction) extra++;
            if (extra > 0) event.getDrops().getFirst().getItem().grow(Math.min(16, extra));
        }

        if (data.has(player.getUUID(), profile.speciesId(), "life_artisan")
                && event.getDroppedExperience() > 0) {
            event.setDroppedExperience(Math.max(1, Math.round(event.getDroppedExperience() * 1.25F)));
        }
    }

    public static boolean has(ServerPlayer player, String skillId) {
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        return profile != null && data(player).has(player.getUUID(), profile.speciesId(), skillId);
    }

    public static int masteryLevel(ServerPlayer player, String track) {
        return masteryData(player).level(player.getUUID(), track);
    }

    public static long masteryXp(ServerPlayer player, String track) {
        return masteryData(player).xp(player.getUUID(), track);
    }

    public static float masteryProgress(ServerPlayer player, String track) {
        return MasteryProgressionSavedData.progress(masteryXp(player, track));
    }

    public static void addMastery(ServerPlayer player, String track, long amount) {
        masteryData(player).add(player.getUUID(), track, amount);
    }

    public static int propertyCrimeSeverity(ServerPlayer player, int baseSeverity) {
        return has(player, "society_citizen_ties") ? Math.max(1, baseSeverity - 1) : baseSeverity;
    }

    public static String traitTitle(ServerPlayer player) {
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        return SkillTreeCatalog.speciesTraitTitle(profile == null ? "human" : profile.speciesId());
    }

    public static String traitDescription(ServerPlayer player) {
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        return SkillTreeCatalog.speciesTraitDescription(profile == null ? "human" : profile.speciesId());
    }

    private static float combatMultiplier(int level) {
        return (float) (1.0D + Math.log1p(level) / 55.0D);
    }

    private static float defenseMultiplier(int level) {
        return (float) (1.0D / (1.0D + Math.log1p(level) / 80.0D));
    }

    private static String masteryTrack(BlockDropsEvent event) {
        if (event.getState().getBlock() instanceof CropBlock) return MasteryProgressionSavedData.FARMING;
        if (event.getState().is(BlockTags.LOGS)) return MasteryProgressionSavedData.LOGGING;
        if (event.getState().is(BlockTags.MINEABLE_WITH_PICKAXE)) return MasteryProgressionSavedData.MINING;
        return MasteryProgressionSavedData.GATHERING;
    }

    private static SkillProgressionSavedData data(ServerPlayer player) {
        return player.level().getServer().overworld().getDataStorage().computeIfAbsent(SkillProgressionSavedData.TYPE);
    }

    private static MasteryProgressionSavedData masteryData(ServerPlayer player) {
        return player.level().getServer().overworld().getDataStorage().computeIfAbsent(MasteryProgressionSavedData.TYPE);
    }
}
