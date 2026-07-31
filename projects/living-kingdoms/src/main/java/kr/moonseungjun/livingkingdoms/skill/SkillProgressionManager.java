package kr.moonseungjun.livingkingdoms.skill;

import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

/** Applies racial traits and unlocked skills to real server-side gameplay. */
public final class SkillProgressionManager {
    private SkillProgressionManager() {
    }

    public static SkillProgressionSavedData.SkillState state(ServerPlayer player) {
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        String species = profile == null ? "human" : profile.speciesId();
        SkillProgressionSavedData data = data(player);
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
        if (!player.level().dimension().equals(StarterRealmManager.REALM_KEY)
                || player.level().getGameTime() % 100L != 0L) {
            return;
        }

        boolean elfSight = "elf".equals(profile.speciesId());
        if (elfSight || data.has(player.getUUID(), profile.speciesId(), "explore_night_sight")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, true));
        }
        if (data.has(player.getUUID(), profile.speciesId(), "explore_trailblazer")) {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 0, true, false, true));
        }
    }

    public static void modifyDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            OriginProfile profile = OriginProfileManager.profile(victim.getUUID()).orElse(null);
            if (profile != null) {
                SkillProgressionSavedData data = data(victim);
                float multiplier = 1.0F;
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
            if (profile != null && data(attacker).has(attacker.getUUID(), profile.speciesId(), "combat_training")) {
                event.setAmount(event.getAmount() * 1.10F);
            }
        }
    }

    public static void modifyDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        OriginProfile profile = OriginProfileManager.profile(player.getUUID()).orElse(null);
        if (profile == null) return;
        SkillProgressionSavedData data = data(player);
        if (data.has(player.getUUID(), profile.speciesId(), "life_gatherer")
                && !event.getDrops().isEmpty() && event.getLevel().getRandom().nextFloat() < 0.20F) {
            event.getDrops().getFirst().getItem().grow(1);
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

    private static SkillProgressionSavedData data(ServerPlayer player) {
        return player.level().getServer().overworld().getDataStorage().computeIfAbsent(SkillProgressionSavedData.TYPE);
    }
}
