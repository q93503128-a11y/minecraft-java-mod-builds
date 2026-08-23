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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Measures real health changes caused to actual combatants during one cast window. Passive
 * Passive livestock cannot feed insight/economy; progress stays logarithmic and tightly capped so one
 * weak mob or a large area spell cannot skip circles.
 */
public final class CombatGrowthService {
    private static final List<EquipmentSlot> THREAT_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
    private static final int MAX_MASTERY_PER_CAST = 30;
    private static final int MAX_INSIGHT_PER_CAST = 8;
    private static final Set<String> DEFERRED_DAMAGE_SPELLS = Set.of(
            "scorching_ray", "sleet_storm", "wall_of_fire", "ice_storm", "blight",
            "phantasmal_killer", "cloudkill", "flame_strike", "insect_plague", "sunbeam",
            "freezing_sphere", "delayed_blast_fireball", "fire_storm", "earthquake",
            "incendiary_cloud", "control_weather", "prismatic_wall", "weird", "fire_shield", "wall_of_ice",
            "winter_domain");
    private static final Map<DeferredKey, DeferredWindow> DEFERRED = new HashMap<>();

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
    public record DeferredSettlement(String spellId, int spellCircle, Impact impact) {}
    private record DeferredKey(UUID playerId, String spellId) {}
    private static final class AttributedTarget {
        final int threat; double damage; boolean killed;
        AttributedTarget(int threat) { this.threat = Math.max(1, threat); }
    }
    private static final class DeferredWindow {
        final String spellId; final int spellCircle; final long expiresAt;
        final int masteryBudget; final int insightBudget;
        final Map<UUID, AttributedTarget> targets = new HashMap<>();
        DeferredWindow(String spellId, int spellCircle, long expiresAt, int masteryBudget, int insightBudget) {
            this.spellId=spellId; this.spellCircle=spellCircle; this.expiresAt=expiresAt;
            this.masteryBudget=masteryBudget; this.insightBudget=insightBudget;
        }
    }

    public static Snapshot capture(ServerPlayer player, double range) {
        // Pre-release growth capture must contain every entity that the locked spell can legitimately
        // reach later. Small/medium spells keep a compact envelope; long-range 7-9C authority scales
        // up far enough to include Meteor Swarm's full cityfall radius at the maximum authored range.
        double radius = range >= 60.0
                ? Math.min(300.0, Math.max(10.0, range * 2.65 + 10.0))
                : Math.min(120.0, Math.max(10.0, range * 1.55 + 8.0));
        AABB box = player.getBoundingBox().inflate(radius, Math.max(8.0, radius * 0.45), radius);
        List<Sample> samples = new ArrayList<>();
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob))) {
            samples.add(new Sample(mob, mob.getHealth(), mob.getMaxHealth(), threatScore(mob)));
        }
        return new Snapshot(List.copyOf(samples));
    }

    public static int deferredCreditTicks(String spellId) {
        if (!DEFERRED_DAMAGE_SPELLS.contains(spellId)) return 0;
        return switch (spellId) {
            case "scorching_ray" -> 30;
            case "sleet_storm" -> 180;
            case "wall_of_fire" -> 240;
            case "ice_storm" -> 120;
            case "blight" -> 160;
            case "phantasmal_killer" -> 280;
            case "cloudkill" -> 220;
            case "flame_strike" -> 80;
            case "insect_plague" -> 220;
            case "sunbeam" -> 120;
            case "freezing_sphere" -> 200;
            case "delayed_blast_fireball" -> 90;
            case "fire_storm" -> 70;
            case "earthquake" -> 180;
            case "incendiary_cloud" -> 240;
            case "control_weather" -> 900;
            case "prismatic_wall" -> 400;
            case "weird" -> 300;
            case "fire_shield" -> 620;
            case "wall_of_ice" -> 220;
            case "winter_domain" -> 240;
            default -> 0;
        };
    }

    /** Remove a previous same-spell ledger before a recast so the new opening hit cannot leak into it. */
    public static DeferredSettlement takeDeferred(ServerPlayer player, String spellId) {
        if (player == null || spellId == null) return null;
        return settlement(DEFERRED.remove(new DeferredKey(player.getUUID(), spellId)));
    }

    /** Start after the synchronous cast result was measured; only later ArcaneDamage is credited here. */
    public static void startDeferred(ServerPlayer player, String spellId, int spellCircle, Impact immediate) {
        int ticks = deferredCreditTicks(spellId);
        if (player == null || ticks <= 0) return;
        Impact first = immediate == null ? Impact.NONE : immediate;
        long now = ((net.minecraft.server.level.ServerLevel) player.level()).getGameTime();
        DeferredKey key = new DeferredKey(player.getUUID(), spellId);
        DEFERRED.put(key, new DeferredWindow(spellId, Math.max(1, spellCircle), now + ticks,
                Math.max(0, MAX_MASTERY_PER_CAST - Math.max(0, first.masteryGain())),
                Math.max(0, MAX_INSIGHT_PER_CAST - Math.max(0, first.insightGain()))));
    }

    public static boolean attributableTarget(ServerPlayer player, Mob mob) {
        return player != null && mob != null && validTarget(player, mob);
    }

    public static void recordAttributed(ServerPlayer player, String spellId, Mob mob, double actualDamage, boolean killed) {
        if (player == null || mob == null || spellId == null || spellId.isBlank() || actualDamage <= .001) return;
        DeferredWindow window = DEFERRED.get(new DeferredKey(player.getUUID(), spellId));
        if (window == null || ((net.minecraft.server.level.ServerLevel) player.level()).getGameTime() > window.expiresAt) return;
        AttributedTarget target = window.targets.computeIfAbsent(mob.getUUID(), ignored -> new AttributedTarget(threatScore(mob)));
        target.damage += actualDamage;
        target.killed |= killed;
    }

    public static List<DeferredSettlement> drainReady(ServerPlayer player) {
        if (player == null) return List.of();
        long now = ((net.minecraft.server.level.ServerLevel) player.level()).getGameTime();
        List<DeferredSettlement> ready = new ArrayList<>();
        Iterator<Map.Entry<DeferredKey, DeferredWindow>> iterator = DEFERRED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DeferredKey, DeferredWindow> entry = iterator.next();
            if (!entry.getKey().playerId().equals(player.getUUID()) || now < entry.getValue().expiresAt) continue;
            DeferredSettlement value = settlement(entry.getValue());
            iterator.remove();
            if (value != null && value.impact().meaningful()) ready.add(value);
        }
        return List.copyOf(ready);
    }

    public static void clear(UUID playerId) {
        if (playerId != null) DEFERRED.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    public static void clearAll() { DEFERRED.clear(); }

    private static DeferredSettlement settlement(DeferredWindow window) {
        if (window == null || window.targets.isEmpty()) return null;
        int hits=0, kills=0, strongHits=0, strongKills=0, threatPoints=0, peakThreat=0;
        long combatValue=0L; double damage=0.0, masteryScore=0.0, insightScore=0.0;
        for (AttributedTarget target : window.targets.values()) {
            if (target.damage <= .001) continue;
            hits++; damage += target.damage;
            int threat=Math.max(1,target.threat), tier=threatTier(threat);
            peakThreat=Math.max(peakThreat,threat);
            if (tier>0) strongHits++;
            if (target.killed) { kills++; if (tier>0) strongKills++; }
            threatPoints=Math.min(2_000_000, threatPoints + (target.killed ? threat : Math.max(1, threat/8)));
            double threatLog=Math.log1p(threat);
            masteryScore += .65 + Math.min(2.4, threatLog*.42) + Math.min(1.6, Math.sqrt(target.damage)*.12);
            if (target.killed) masteryScore += .8 + Math.min(4.0, threatLog*.72);
            if (tier>0) insightScore += .20 + tier*.18;
            if (target.killed) insightScore += tier>0 ? .45+tier*.25 : .22;
            long hitValue=Math.max(1L,Math.round(Math.sqrt(threat)*.45));
            long killValue=target.killed ? Math.max(1L,Math.round(Math.pow(threat,1.12)*.24)) : 0L;
            combatValue=Math.min(2_000_000_000L,combatValue+hitValue+killValue);
        }
        if (hits==0) return null;
        int mastery=Math.min(window.masteryBudget, Math.min(MAX_MASTERY_PER_CAST, Math.max(1,(int)Math.round(masteryScore))));
        int insight=Math.min(window.insightBudget, Math.min(MAX_INSIGHT_PER_CAST, Math.max(0,(int)Math.floor(insightScore))));
        if (kills>0 && insight==0 && window.insightBudget>0) insight=1;
        return new DeferredSettlement(window.spellId, window.spellCircle,
                new Impact(hits,kills,strongHits,strongKills,(int)Math.round(damage),mastery,insight,
                        threatPoints,peakThreat,combatValue));
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
        if (player.isAlliedTo(mob)) return false;
        return mob instanceof Enemy || ArcaneMageService.isMage(mob) || mob.getTarget() == player;
    }
}
