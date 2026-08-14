package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-authoritative terrain rupture for physically destructive spells.
 *
 * Alpha.38 separates footprint from visual hype: surgical spells stay narrow, while genuine
 * catastrophe-class magic owns broad/deep terrain shapes.  Every shape still funnels through the
 * same hardness, blast-resistance, chunk, fluid and block-entity protections and a shared per-tick
 * budget, so larger spectacle does not mean an unbounded world-edit spike.
 */
public final class DestructiveMagicService {
    public enum TerrainClass { MAJOR, CONDITIONAL, NONE }

    private static final Set<String> MAJOR = Set.of(
            "disintegrate", "delayed_blast_fireball", "fire_storm", "earthquake",
            "meteor_swarm", "world_sunder", "arcane_annihilation");
    private static final Set<String> CONDITIONAL = Set.of(
            "fireball", "shatter", "flame_strike", "meteor_shard", "move_earth",
            "lightning_bolt", "thunderwave", "gust_of_wind");

    // Catastrophe spells may touch a lot of terrain, but all calls on a level still share these.
    private static final int MAX_BLOCK_CHANGES_PER_TICK = 720;
    private static final int MAX_BLOCK_SCANS_PER_TICK = 48_000;
    private static final int MAX_DROPPED_BLOCKS_PER_TICK = 96;
    private static final Map<ServerLevel, TickBudget> BUDGETS = new WeakHashMap<>();

    private record Candidate(BlockPos pos, double overload) {}
    private record Profile(double radiusScale, double baseEnergy, int maxBlocks, boolean drops,
                           double maxRadius, double verticalScale) {}

    private static final class TickBudget {
        private long tick = Long.MIN_VALUE;
        private int changes;
        private int scans;
        private int droppedBlocks;

        void reset(long currentTick) {
            if (tick == currentTick) return;
            tick = currentTick;
            changes = 0;
            scans = 0;
            droppedBlocks = 0;
        }

        boolean scanAvailable() { return scans < MAX_BLOCK_SCANS_PER_TICK; }
        int changesRemaining() { return Math.max(0, MAX_BLOCK_CHANGES_PER_TICK - changes); }
        int dropChangesRemaining() { return Math.max(0, MAX_DROPPED_BLOCKS_PER_TICK - droppedBlocks); }
        void scanned() { scans++; }
        void changed(boolean drops) {
            changes++;
            if (drops) droppedBlocks++;
        }
    }

    private DestructiveMagicService() {}

    public static TerrainClass classification(String spellId) {
        if (MAJOR.contains(spellId)) return TerrainClass.MAJOR;
        if (CONDITIONAL.contains(spellId)) return TerrainClass.CONDITIONAL;
        return TerrainClass.NONE;
    }

    public static int impact(ServerPlayer player, String spellId, Vec3 center,
                             double requestedRadius, double power) {
        Profile profile = profile(spellId);
        if (profile == null || center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        TickBudget budget = budget(level);
        int changeLimit = Math.min(profile.maxBlocks(), budget.changesRemaining());
        if (profile.drops()) changeLimit = Math.min(changeLimit, budget.dropChangesRemaining());
        if (changeLimit <= 0 || !budget.scanAvailable()) return 0;

        double radius = Math.max(.75, Math.min(profile.maxRadius(),
                Math.max(.75, requestedRadius) * profile.radiusScale()));
        double energy = profile.baseEnergy() * (.78 + .22 * Math.sqrt(Math.max(.1, power)));
        int bound = (int) Math.ceil(radius);
        List<Candidate> candidates = new ArrayList<>();
        BlockPos origin = BlockPos.containing(center);
        double vertical = Math.max(1.15, radius * profile.verticalScale());

        scan:
        for (int x = -bound; x <= bound; x++) {
            for (int z = -bound; z <= bound; z++) {
                for (int y = -(int) Math.ceil(vertical); y <= Math.ceil(vertical); y++) {
                    double nx = x / radius, nz = z / radius, ny = y / vertical;
                    double normalized = Math.sqrt(nx * nx + nz * nz + ny * ny);
                    if (normalized > 1.0) continue;
                    if (!budget.scanAvailable()) break scan;
                    budget.scanned();

                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) continue;
                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0) continue;
                    float blast = Math.max(0F, state.getBlock().getExplosionResistance());
                    if (blast >= 1000F) continue;

                    double strength = 1.0 + Math.max(0, hardness) * 2.6 + Math.sqrt(blast) * 1.45;
                    double falloff = Math.pow(Math.max(0.0, 1.0 - normalized), .74);
                    double local = energy * (.18 + .82 * falloff);
                    if (local < strength) continue;
                    candidates.add(new Candidate(pos, local / Math.max(.25, strength)));
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(Candidate::overload).reversed());
        int changed = 0;
        for (Candidate candidate : candidates) {
            if (changed >= changeLimit || budget.changesRemaining() <= 0) break;
            if (profile.drops() && budget.dropChangesRemaining() <= 0) break;
            if (!level.hasChunkAt(candidate.pos())) continue;
            if (level.destroyBlock(candidate.pos(), profile.drops(), player)) {
                changed++;
                budget.changed(profile.drops());
            }
        }
        return changed;
    }

    /** Narrow line damage for lightning/disintegrate style spells. */
    public static int ray(ServerPlayer player, String spellId, Vec3 start, Vec3 end, double power) {
        if (start == null || end == null) return 0;
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < .05) return 0;
        Vec3 unit = delta.scale(1.0 / length);
        int changed = 0;
        int samples = Math.min(96, Math.max(1, (int) Math.ceil(length / .70)));
        for (int i = 1; i <= samples; i++) {
            Vec3 at = start.add(unit.scale(length * i / (double) samples));
            changed += impact(player, spellId, at, 1.18, power);
            if (changed >= 120 || budget((ServerLevel) player.level()).changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * 8C Earthquake is a battlefield event, not one round crater.  A dense epicenter fails first,
     * then eight uneven secondary foci tear the outer ground.  Shared budgets keep it bounded.
     */
    public static int quakeField(ServerPlayer player, Vec3 center, double requestedRadius, double power) {
        if (center == null) return 0;
        double field = Math.max(11.0, Math.min(24.0, requestedRadius));
        int changed = impact(player, "earthquake", center.add(0, -.35, 0), field * .58, power * 1.14);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8.0 + Math.sin(i * 1.91) * .12;
            double d = field * (.42 + .055 * (i % 3));
            Vec3 at = center.add(Math.cos(a) * d, -.18 - .08 * (i % 2), Math.sin(a) * d);
            changed += impact(player, "earthquake", at, field * (.27 + .025 * (i % 2)),
                    power * (.72 + .05 * (i % 3)));
            TickBudget tick = budget((ServerLevel) player.level());
            if (!tick.scanAvailable() || tick.changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * One Meteor Swarm projectile creates a deep bowl plus an irregular fractured rim.  Sixteen
     * staggered strikes therefore leave a real bombardment field instead of sixteen pinholes.
     */
    public static int meteorCrater(ServerPlayer player, Vec3 center, double radius, double power) {
        if (center == null) return 0;
        double r = Math.max(4.2, Math.min(9.5, radius * 1.38));
        int changed = impact(player, "meteor_swarm", center.add(0, -.45, 0), r, power * 1.16);
        changed += impact(player, "meteor_swarm", center.add(0, -1.75, 0), r * .70, power * 1.05);
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2.0 / 5.0 + .41;
            Vec3 rim = center.add(Math.cos(a) * r * .46, -.20, Math.sin(a) * r * .46);
            changed += impact(player, "meteor_swarm", rim, r * .34, power * .72);
            TickBudget tick = budget((ServerLevel) player.level());
            if (!tick.scanAvailable() || tick.changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * Arcane Annihilation is a true deletion corridor.  It samples a thick beam from safely in
     * front of the caster to the sealed endpoint and carves through intervening terrain.
     */
    public static int annihilationCorridor(ServerPlayer player, Vec3 start, Vec3 end, double power) {
        if (start == null || end == null) return 0;
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0) return 0;
        Vec3 unit = delta.scale(1.0 / length);
        double bore = Math.max(2.15, Math.min(3.75, 2.0 + Math.sqrt(Math.max(.1, power)) * .16));
        int changed = 0;
        double first = Math.min(length, 2.8); // never erase the block directly under the caster.
        for (double d = first; d <= length + .001; d += 2.35) {
            double t = d / Math.max(1.0, length);
            Vec3 at = start.add(unit.scale(d));
            changed += impact(player, "arcane_annihilation", at, bore * (.86 + .20 * t),
                    power * (.88 + .16 * t));
            TickBudget tick = budget((ServerLevel) player.level());
            if (changed >= 520 || !tick.scanAvailable() || tick.changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * World Sunder is a long, deep thirteen-point cut.  The center breaks widest/deepest and the
     * crack meanders instead of reading as a row of identical circular explosions.
     */
    public static int fissure(ServerPlayer player, String spellId, Vec3 center,
                              Vec3 direction, double range, double power) {
        if (center == null) return 0;
        Vec3 flat = direction == null ? Vec3.ZERO : new Vec3(direction.x, 0.0, direction.z);
        if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        double halfLength = Math.max(12.0, Math.min(28.0, range * .34));
        int changed = 0;
        for (int i = -6; i <= 6; i++) {
            double t = i / 6.0;
            double weight = 1.0 - Math.abs(t);
            double wobble = Math.sin((i + 6) * 1.41) * (1.15 + weight * 1.45);
            Vec3 at = center.add(flat.scale(t * halfLength)).add(right.scale(wobble))
                    .add(0, -.28 - weight * .72, 0);
            changed += impact(player, spellId, at, 3.8 + weight * 3.8,
                    power * (.74 + weight * .42));
            TickBudget tickBudget = budget((ServerLevel) player.level());
            if (!tickBudget.scanAvailable() || tickBudget.changesRemaining() <= 0) break;
        }
        return changed;
    }

    public static void applyPhysicalAftermath(ServerPlayer player, String spellId,
                                              CastTargetSnapshot snapshot, double range, double power) {
        if (snapshot == null || !snapshot.validFor(player)) return;
        switch (spellId) {
            case "lightning_bolt" -> ray(player, "lightning_bolt",
                    snapshot.launchOrigin(), snapshot.target(), power * .72);
            case "thunderwave" -> ray(player, "thunderwave",
                    snapshot.launchOrigin(), snapshot.target(), power * .52);
            case "gust_of_wind" -> ray(player, "gust_of_wind",
                    snapshot.launchOrigin(), snapshot.target(), power * .38);
            case "world_sunder" -> fissure(player, "world_sunder", snapshot.target(),
                    snapshot.launchDirection(), range, power);
            default -> {
            }
        }
    }

    private static TickBudget budget(ServerLevel level) {
        TickBudget budget = BUDGETS.computeIfAbsent(level, ignored -> new TickBudget());
        budget.reset(level.getGameTime());
        return budget;
    }

    private static Profile profile(String id) {
        return switch (id) {
            case "fireball" -> new Profile(.78, 8.2, 100, false, 6.0, .58);
            case "shatter" -> new Profile(.76, 9.2, 100, true, 5.5, .46);
            case "flame_strike" -> new Profile(.72, 12.0, 130, false, 7.0, .78);
            case "meteor_shard" -> new Profile(1.00, 14.5, 180, false, 8.5, .74);
            case "disintegrate" -> new Profile(.90, 26.0, 44, false, 2.1, .54);
            case "delayed_blast_fireball" -> new Profile(1.00, 18.5, 280, false, 12.0, .76);
            case "fire_storm" -> new Profile(.92, 15.5, 120, false, 8.5, .72);
            case "move_earth" -> new Profile(.78, 14.0, 260, false, 12.0, .44);
            case "earthquake" -> new Profile(1.00, 19.5, 380, false, 18.0, .52);
            case "meteor_swarm" -> new Profile(1.00, 22.5, 190, false, 10.0, .72);
            case "world_sunder" -> new Profile(1.00, 31.0, 480, false, 15.0, .82);
            case "arcane_annihilation" -> new Profile(1.00, 28.0, 110, false, 3.8, .70);
            case "lightning_bolt" -> new Profile(.30, 10.0, 18, false, 1.4, .44);
            case "thunderwave" -> new Profile(.34, 6.6, 22, false, 1.6, .42);
            case "gust_of_wind" -> new Profile(.28, 4.2, 10, false, 1.25, .38);
            default -> null;
        };
    }
}
