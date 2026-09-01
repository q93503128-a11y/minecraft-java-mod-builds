package kr.moonseungjun.titanbreak.client;

import kr.moonseungjun.titanbreak.entity.BulwarkEntity;
import kr.moonseungjun.titanbreak.entity.BurrowerEntity;
import kr.moonseungjun.titanbreak.entity.BurstlingEntity;
import kr.moonseungjun.titanbreak.entity.ChronoHoundEntity;
import kr.moonseungjun.titanbreak.entity.CinderEntity;
import kr.moonseungjun.titanbreak.entity.CrusherEntity;
import kr.moonseungjun.titanbreak.entity.GliderEntity;
import kr.moonseungjun.titanbreak.entity.HollowColossusEntity;
import kr.moonseungjun.titanbreak.entity.HowlerEntity;
import kr.moonseungjun.titanbreak.entity.JammerEntity;
import kr.moonseungjun.titanbreak.entity.NeedlerEntity;
import kr.moonseungjun.titanbreak.entity.NullEyeEntity;
import kr.moonseungjun.titanbreak.entity.PursuerEntity;
import kr.moonseungjun.titanbreak.entity.RegrowerEntity;
import kr.moonseungjun.titanbreak.entity.RipperEntity;
import kr.moonseungjun.titanbreak.entity.SiphonEntity;
import kr.moonseungjun.titanbreak.entity.SkitterEntity;
import kr.moonseungjun.titanbreak.entity.SpitterEntity;
import kr.moonseungjun.titanbreak.entity.StalkerEntity;
import kr.moonseungjun.titanbreak.entity.TitanGeoEntity;
import kr.moonseungjun.titanbreak.entity.VoltaicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import java.util.Comparator;
import java.util.List;

/** Client-only ocular analysis calculations. No attacks or server-authoritative state changes occur here. */
public final class OcularAnalysisClientService {
    private static int scanTargetId = -1;
    private static int scanStartedTick;

    private OcularAnalysisClientService() {}

    public static int enhancement(String augmentId) {
        TitanClientState.AugmentMeta meta = TitanClientState.augmentMeta(augmentId);
        return meta != null && meta.installed() ? meta.enhancement() : -1;
    }

    public static LivingEntity resolveTarget(Minecraft mc, Entity raw) {
        LivingEntity direct = asLiving(raw);
        if (direct != null && direct.isAlive()) return direct;
        if (mc.player == null || mc.level == null) return null;

        int thermal = enhancement("thermal_eye");
        if (thermal >= 0) {
            AABB area = mc.player.getBoundingBox().inflate(24.0D);
            LivingEntity hidden = mc.level.getEntitiesOfClass(LivingEntity.class, area,
                            entity -> entity != mc.player && entity.isAlive() && entity.isInvisible()
                                    && mc.player.hasLineOfSight(entity))
                    .stream().min(Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
            if (hidden != null) return hidden;
        }

        int electromagnetic = enhancement("electromagnetic_eye");
        if (electromagnetic >= 5) {
            AABB area = mc.player.getBoundingBox().inflate(18.0D);
            return mc.level.getEntitiesOfClass(LivingEntity.class, area,
                            entity -> entity != mc.player && entity.isAlive() && entity instanceof TitanGeoEntity)
                    .stream().min(Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
        }
        return null;
    }

    private static LivingEntity asLiving(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity living) return living;
        return null;
    }

    public static double scanProgress(Minecraft mc, LivingEntity target) {
        if (mc.player == null || target == null) return 0.0D;
        if (scanTargetId != target.getId()) {
            scanTargetId = target.getId();
            scanStartedTick = mc.player.tickCount;
        }
        int tactical = enhancement("tactical_eye");
        int duration = tactical >= 3 ? 12 : tactical >= 0 ? 20 : 14;
        return Mth.clamp((mc.player.tickCount - scanStartedTick + 1) / (double) duration, 0.0D, 1.0D);
    }

    public static double armor(LivingEntity target) {
        return target == null ? 0.0D : Math.max(0.0D, target.getAttributeValue(Attributes.ARMOR));
    }

    public static int armorThicknessPips(LivingEntity target) {
        return Mth.clamp((int) Math.ceil(armor(target) / 6.0D), 0, 5);
    }

    public static double motionPredictionSeconds() {
        int enhancement = enhancement("motion_prediction_eye");
        if (enhancement < 0) return 0.0D;
        if (enhancement >= 10) return 0.65D;
        if (enhancement >= 7) return 0.45D;
        if (enhancement >= 5) return 0.25D;
        return 0.12D;
    }

    public static double predictedTravel(LivingEntity target, double seconds) {
        if (target == null || seconds <= 0.0D) return 0.0D;
        return target.getDeltaMovement().scale(seconds * 20.0D).length();
    }

    public static double ballisticLead(Minecraft mc, LivingEntity target, double distance) {
        int enhancement = enhancement("ballistic_eye");
        if (enhancement < 0 || target == null) return 0.0D;
        double seconds = Mth.clamp(distance / (enhancement >= 7 ? 88.0D : 68.0D), 0.12D, enhancement >= 7 ? 0.65D : 0.48D);
        Vec3 relative = target.getDeltaMovement();
        if (enhancement >= 5 && mc.player != null) relative = relative.subtract(mc.player.getDeltaMovement());
        if (enhancement >= 10 && TitanClientState.flag("active")) seconds *= 0.72D;
        return relative.scale(seconds * 20.0D).horizontalDistance();
    }

    public static int weakpointScore(Minecraft mc, LivingEntity target) {
        if (target == null) return 0;
        int enhancement = enhancement("weakpoint_analysis_eye");
        int tactical = enhancement("tactical_eye");
        if (enhancement < 0 && tactical < 10) return 0;

        double ratio = preferredAimHeight(target);
        if (mc.hitResult instanceof EntityHitResult hit && asLiving(hit.getEntity()) == target && target.getBbHeight() > 0.01F) {
            ratio = Mth.clamp((hit.getLocation().y - target.getY()) / target.getBbHeight(), 0.0D, 1.0D);
        }
        int score = ratio >= 0.74D ? 76 : ratio >= 0.42D && ratio <= 0.68D ? 68 : 48;
        if (enhancement >= 5 && armor(target) >= 12.0D) score = Math.max(score, 74);
        if (enhancement >= 7 && armor(target) > 0.0D) score += 8;
        if (enhancement >= 10 && TitanClientState.hasInstalled("target_assist")) score += 7;
        if (target instanceof PursuerEntity pursuer) {
            int mask = pursuer.brokenPartsMask();
            if ((mask & PursuerEntity.PART_CHEST_CORE) == 0) score = Math.max(score, 92);
            else if ((mask & PursuerEntity.PART_SPINE_REACTION) == 0) score = Math.max(score, 84);
            else if ((mask & (PursuerEntity.PART_LEFT_EYE | PursuerEntity.PART_RIGHT_EYE)) !=
                    (PursuerEntity.PART_LEFT_EYE | PursuerEntity.PART_RIGHT_EYE)) score = Math.max(score, 78);
        }
        return Mth.clamp(score, 0, 100);
    }

    public static double preferredAimHeight(LivingEntity target) {
        if (target instanceof PursuerEntity pursuer) {
            int mask = pursuer.brokenPartsMask();
            if ((mask & PursuerEntity.PART_CHEST_CORE) == 0) return 15.0D / 32.0D;
            if ((mask & PursuerEntity.PART_SPINE_REACTION) == 0) return 20.0D / 32.0D;
            if ((mask & (PursuerEntity.PART_LEFT_EYE | PursuerEntity.PART_RIGHT_EYE)) !=
                    (PursuerEntity.PART_LEFT_EYE | PursuerEntity.PART_RIGHT_EYE)) return 29.0D / 32.0D;
        }
        return armor(target) >= 12.0D ? 0.56D : 0.72D;
    }

    public static String structuralOutcomeGlyph(LivingEntity target) {
        if (!(target instanceof PursuerEntity pursuer)) return "";
        int mask = pursuer.brokenPartsMask();
        if ((mask & PursuerEntity.PART_SPINE_REACTION) == 0) return "TR↓";
        int limbMask = PursuerEntity.PART_LEFT_FORE_UPPER | PursuerEntity.PART_LEFT_FORE_LOWER
                | PursuerEntity.PART_RIGHT_FORE_UPPER | PursuerEntity.PART_RIGHT_FORE_LOWER;
        if ((mask & limbMask) != limbMask) return "↔↓";
        if ((mask & PursuerEntity.PART_CHEST_CORE) == 0) return "×1.55";
        return "";
    }

    public static Component dropHint(LivingEntity target) {
        if (target instanceof RipperEntity) return Component.translatable("item.titanbreak.high_density_muscle_fiber");
        if (target instanceof SkitterEntity) return Component.translatable("item.titanbreak.servo_bundle");
        if (target instanceof BulwarkEntity) return Component.translatable("item.titanbreak.composite_armor_plate");
        if (target instanceof SpitterEntity) return Component.translatable("item.titanbreak.suture_polymer");
        if (target instanceof NeedlerEntity) return Component.translatable("item.titanbreak.optic_sensor_cluster");
        if (target instanceof GliderEntity) return Component.translatable("item.titanbreak.thermal_optic_cluster");
        if (target instanceof HowlerEntity) return Component.translatable("item.titanbreak.resonant_neural_ganglion");
        if (target instanceof JammerEntity) return Component.translatable("item.titanbreak.calculation_core");
        if (target instanceof VoltaicEntity) return Component.translatable("item.titanbreak.capacitor_stack");
        if (target instanceof CinderEntity) return Component.translatable("item.titanbreak.heat_sink");
        if (target instanceof RegrowerEntity) return Component.translatable("item.titanbreak.regenerative_tissue");
        if (target instanceof BurrowerEntity) return Component.translatable("item.titanbreak.dense_bone_lattice");
        if (target instanceof CrusherEntity) return Component.translatable("item.titanbreak.impact_core");
        if (target instanceof StalkerEntity) return Component.translatable("item.titanbreak.resonant_neural_ganglion");
        if (target instanceof BurstlingEntity) return Component.translatable("item.titanbreak.cooling_cell");
        if (target instanceof SiphonEntity) return Component.translatable("item.titanbreak.circulation_core");
        if (target instanceof ChronoHoundEntity) return Component.translatable("item.titanbreak.reaction_temporal_matrix");
        if (target instanceof NullEyeEntity) return Component.translatable("item.titanbreak.thermal_optic_cluster");
        if (target instanceof PursuerEntity) return Component.translatable("item.titanbreak.pursuer_reaction_organ");
        return null;
    }

    public static int thermalContacts(Minecraft mc) {
        if (mc.player == null || mc.level == null || enhancement("thermal_eye") < 0) return 0;
        AABB area = mc.player.getBoundingBox().inflate(24.0D);
        return mc.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mc.player && entity.isAlive() && mc.player.hasLineOfSight(entity)).size();
    }

    public static int thermalStrength(LivingEntity target) {
        if (target == null) return 0;
        int strength = 42;
        if (target instanceof CinderEntity) strength += 38;
        if (target.isOnFire()) strength += 45;
        strength += (int) Math.min(25.0D, target.getDeltaMovement().length() * 30.0D);
        if (target.getHealth() <= target.getMaxHealth() * 0.35F) strength += 8;
        return Mth.clamp(strength, 0, 100);
    }

    public static int toxicContacts(Minecraft mc) {
        if (mc.player == null || mc.level == null || enhancement("multispectrum_eye") < 7) return 0;
        AABB area = mc.player.getBoundingBox().inflate(24.0D);
        return mc.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mc.player && entity.isAlive() && entity.hasEffect(MobEffects.POISON)).size();
    }

    public static int electromagneticContacts(Minecraft mc) {
        int enhancement = enhancement("electromagnetic_eye");
        int multispectrum = enhancement("multispectrum_eye");
        if (mc.player == null || mc.level == null || (enhancement < 0 && multispectrum < 10)) return 0;
        double range = enhancement >= 5 ? 24.0D : 18.0D;
        AABB area = mc.player.getBoundingBox().inflate(range);
        List<LivingEntity> contacts = mc.level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != mc.player && entity.isAlive() && entity instanceof TitanGeoEntity);
        if (enhancement >= 5 || multispectrum >= 10) return contacts.size();
        return (int) contacts.stream().filter(mc.player::hasLineOfSight).count();
    }

    public static int electromagneticStrength(LivingEntity target) {
        if (!(target instanceof TitanGeoEntity)) return 0;
        if (target instanceof NullEyeEntity) return 100;
        if (target instanceof PursuerEntity || target instanceof HollowColossusEntity) return 92;
        if (target instanceof VoltaicEntity) return 90;
        if (target instanceof JammerEntity) return 82;
        if (target instanceof ChronoHoundEntity) return 78;
        if (target instanceof CrusherEntity) return 74;
        if (target instanceof CinderEntity) return 72;
        if (target instanceof SiphonEntity) return 70;
        if (target instanceof NeedlerEntity) return 68;
        if (target instanceof GliderEntity || target instanceof BurrowerEntity || target instanceof StalkerEntity) return 64;
        if (target instanceof BurstlingEntity) return 62;
        if (target instanceof RegrowerEntity) return 55;
        return 58;
    }

    public static boolean nullPatternNearby(Minecraft mc) {
        if (mc.player == null || mc.level == null || enhancement("electromagnetic_eye") < 10) return false;
        AABB area = mc.player.getBoundingBox().inflate(32.0D);
        return !mc.level.getEntitiesOfClass(NullEyeEntity.class, area, Entity::isAlive).isEmpty();
    }
}
