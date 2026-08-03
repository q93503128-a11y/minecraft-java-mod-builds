package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.world.ArcaneMageService;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Measures real health changes caused during one cast window. Progress is intentionally
 * logarithmic and tightly capped so one weak mob or a large area spell cannot skip circles.
 */
public final class CombatGrowthService {
    private static final List<EquipmentSlot> THREAT_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
    private static final int MAX_MASTERY_PER_CAST = 30;
    private static final int MAX_INSIGHT_PER_CAST = 8;

    private CombatGrowthService() {}

    public record Sample(Mob mob, float health, float maxHealth, int threat) {}
    public record Snapshot(List<Sample> samples) {
        public static final Snapshot EMPTY = new Snapshot(List.of());
    }
    public record Impact(int hits, int kills, int strongHits, int strongKills, int damage, int masteryGain,
                         int insightGain, int threatPoints, int peakThreat, long combatValue) {
        public static final Impact NONE = new Impact(0, 0, 0, 0, 0, 0, 0, 0, 0, 0L);
        public boolean meaningful() { return hits > 0 || kills > 0; }
    }

    public static Snapshot capture(ServerPlayer player, double range) {
        double radius = Math.min(72.0, Math.max(10.0, range + 7.0));
        AABB box = player.getBoundingBox().inflate(radius, Math.max(8.0, radius * 0.45), radius);
        List<Sample> samples = new ArrayList<>();
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob))) {
            samples.add(new Sample(mob, mob.getHealth(), mob.getMaxHealth(), threatScore(mob)));
        }
        return new Snapshot(List.copyOf(samples));
    }

    public static Impact measure(Snapshot snapshot, int spellCircle) {
        if (snapshot == null || snapshot.samples().isEmpty()) return Impact.NONE;
        int hits = 0;
        int kills = 0;
        int strongHits = 0;
        int strongKills = 0;
        double damage = 0.0;
        int threatPoints = 0;
        int peakThreat = 0;
        long combatValue = 0L;
        double masteryScore = 0.0;
        double insightScore = 0.0;

        for (Sample sample : snapshot.samples()) {
            Mob mob = sample.mob();
            float after = mob.isAlive() && !mob.isRemoved() ? Math.max(0.0F, mob.getHealth()) : 0.0F;
            double dealt = Math.max(0.0, sample.health() - after);
            boolean killed = sample.health() > 0.0F
                    && (!mob.isAlive() || mob.isRemoved() || after <= 0.0F);
            if (dealt <= 0.001 && !killed) continue;

            hits++;
            damage += dealt;
            int threat = Math.max(1, sample.threat());
            peakThreat = Math.max(peakThreat, threat);
            int tier = threatTier(threat);
            if (tier > 0) strongHits++;
            if (killed) {
                kills++;
                if (tier > 0) strongKills++;
            }

            threatPoints = Math.min(2_000_000, threatPoints
                    + (killed ? threat : Math.max(1, threat / 8)));
            double threatLog = Math.log1p(threat);
            masteryScore += 0.65 + Math.min(2.4, threatLog * 0.42)
                    + Math.min(1.6, Math.sqrt(dealt) * 0.12);
            if (killed) masteryScore += 0.8 + Math.min(4.0, threatLog * 0.72);

            // Ordinary livestock/zombies give almost no circle insight. Strong enemies and kills do.
            if (tier > 0) insightScore += 0.20 + tier * 0.18;
            if (killed) insightScore += tier > 0 ? 0.45 + tier * 0.25 : 0.22;

            long hitValue = Math.max(1L, Math.round(Math.sqrt(threat) * 0.45));
            long killValue = killed ? Math.max(1L, Math.round(Math.pow(threat, 1.12) * 0.24)) : 0L;
            combatValue = Math.min(2_000_000_000L, combatValue + hitValue + killValue);
        }

        if (hits == 0 && kills == 0) return Impact.NONE;
        int mastery = Math.min(MAX_MASTERY_PER_CAST, Math.max(1, (int) Math.round(masteryScore)));
        int insight = Math.min(MAX_INSIGHT_PER_CAST, Math.max(0, (int) Math.floor(insightScore)));
        // A genuine kill still advances a little, but never enough to chain multiple circle-ups.
        if (kills > 0 && insight == 0) insight = 1;
        return new Impact(hits, kills, strongHits, strongKills, (int) Math.round(damage),
                mastery, insight, threatPoints, peakThreat, combatValue);
    }

    public static int threatScore(Mob mob) {
        double health = Math.max(1.0, mob.getMaxHealth());
        double attack = attribute(mob, Attributes.ATTACK_DAMAGE);
        double armor = attribute(mob, Attributes.ARMOR);
        double toughness = attribute(mob, Attributes.ARMOR_TOUGHNESS);
        double speed = attribute(mob, Attributes.MOVEMENT_SPEED);
        double follow = attribute(mob, Attributes.FOLLOW_RANGE);
        int equipment = 0;
        for (EquipmentSlot slot : THREAT_SLOTS) if (!mob.getItemBySlot(slot).isEmpty()) equipment++;

        double score = 1.0
                + Math.sqrt(health) * 0.80
                + Math.pow(Math.max(0.0, attack), 1.25) * 0.80
                + Math.pow(Math.max(0.0, armor), 1.12) * 0.36
                + toughness * 0.90
                + speed * 8.0
                + follow / 20.0
                + equipment * 1.5
                + mob.getActiveEffects().size() * 1.25;
        if (mob instanceof Enemy) score *= 1.10;
        if (ArcaneMageService.isMage(mob)) {
            int circle = ArcaneMageService.circle(mob);
            score += circle * circle * 2.6;
        }

        String type = typePath(mob);
        score += switch (type) {
            case "warden" -> 80.0;
            case "wither" -> 95.0;
            case "ender_dragon" -> 110.0;
            case "elder_guardian" -> 34.0;
            case "ravager" -> 25.0;
            case "evoker" -> 20.0;
            case "piglin_brute" -> 14.0;
            default -> 0.0;
        };
        return Math.max(1, Math.min(5000, (int) Math.round(score)));
    }

    private static double attribute(Mob mob, Holder<Attribute> attribute) {
        AttributeInstance instance = mob.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }

    private static int threatTier(int threat) {
        if (threat >= 1600) return 9;
        if (threat >= 900) return 8;
        if (threat >= 450) return 7;
        if (threat >= 150) return 6;
        if (threat >= 100) return 5;
        if (threat >= 65) return 4;
        if (threat >= 40) return 3;
        if (threat >= 24) return 2;
        if (threat >= 16) return 1;
        return 0;
    }

    private static String typePath(Mob mob) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return key == null ? "" : key.getPath();
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;
        return player.getTeam() == null || mob.getTeam() == null || !player.isAlliedTo(mob);
    }
}
