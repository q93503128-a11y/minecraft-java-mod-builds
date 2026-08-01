package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Measures the real combat result of one cast instead of rewarding empty spam. */
public final class CombatGrowthService {
    private CombatGrowthService() {}

    public record Sample(Mob mob, float health, float maxHealth) {}
    public record Snapshot(List<Sample> samples) {
        public static final Snapshot EMPTY = new Snapshot(List.of());
    }
    public record Impact(int hits, int kills, int strongHits, int strongKills, int damage, int masteryGain,
                         int insightGain) {
        public static final Impact NONE = new Impact(0, 0, 0, 0, 0, 1, 0);
        public boolean meaningful() { return hits > 0 || kills > 0; }
    }

    public static Snapshot capture(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = Math.max(12.0, range + 12.0);
        AABB box = player.getBoundingBox().inflate(radius, Math.max(10.0, radius * 0.55), radius);
        List<Sample> samples = new ArrayList<>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> validTarget(player, mob))) {
            samples.add(new Sample(mob, mob.getHealth(), mob.getMaxHealth()));
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
        int threat = 0;

        for (Sample sample : snapshot.samples()) {
            Mob mob = sample.mob();
            float after = mob.isAlive() && !mob.isRemoved() ? Math.max(0.0F, mob.getHealth()) : 0.0F;
            double dealt = Math.max(0.0, sample.health() - after);
            boolean killed = sample.health() > 0.0F && (!mob.isAlive() || mob.isRemoved() || after <= 0.0F);
            if (dealt <= 0.001 && !killed) continue;

            hits++;
            damage += dealt;
            int tier = threatTier(sample.maxHealth());
            if (tier > 0) {
                strongHits++;
                threat += tier;
            }
            if (killed) {
                kills++;
                if (tier > 0) strongKills++;
                threat += 2 + tier * 2;
            }
        }

        if (hits == 0 && kills == 0) return Impact.NONE;
        int damagePoints = Math.min(12, (int) Math.floor(damage / 12.0));
        int mastery = 1 + hits + kills * 3 + threat + damagePoints;
        int insight = hits + kills * 3 + threat * 2 + Math.max(0, spellCircle - 1);
        return new Impact(hits, kills, strongHits, strongKills, (int) Math.round(damage), mastery, insight);
    }

    private static int threatTier(float maxHealth) {
        if (maxHealth >= 300.0F) return 6;
        if (maxHealth >= 160.0F) return 4;
        if (maxHealth >= 80.0F) return 3;
        if (maxHealth >= 40.0F) return 2;
        if (maxHealth >= 25.0F) return 1;
        return 0;
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;
        return player.getTeam() == null || mob.getTeam() == null || !player.isAlliedTo(mob);
    }
}
