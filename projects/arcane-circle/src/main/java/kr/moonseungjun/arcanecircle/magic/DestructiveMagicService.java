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
 * Server-authoritative terrain rupture for spells whose fiction is explicitly destructive.
 * Weak materials fail farther from the impact; hard/blast-resistant materials require a much
 * stronger local impulse. Unbreakable blocks, block entities, fluids and unloaded chunks are
 * never mutated. Per-impact caps are reinforced by shared per-level tick budgets.
 */
public final class DestructiveMagicService {
    public enum TerrainClass { MAJOR, CONDITIONAL, NONE }

    private static final Set<String> MAJOR = Set.of(
            "disintegrate", "delayed_blast_fireball", "fire_storm", "earthquake",
            "meteor_swarm", "world_sunder", "arcane_annihilation");
    private static final Set<String> CONDITIONAL = Set.of(
            "fireball", "shatter", "flame_strike", "meteor_shard", "move_earth",
            "lightning_bolt", "thunderwave", "gust_of_wind");

    private static final int MAX_BLOCK_CHANGES_PER_TICK = 420;
    private static final int MAX_BLOCK_SCANS_PER_TICK = 24_000;
    private static final int MAX_DROPPED_BLOCKS_PER_TICK = 96;
    private static final Map<ServerLevel, TickBudget> BUDGETS = new WeakHashMap<>();

    private record Candidate(BlockPos pos, double overload) {}
    private record Profile(double radiusScale, double baseEnergy, int maxBlocks, boolean drops) {}

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

        double radius = Math.max(.75, Math.min(10.5, requestedRadius * profile.radiusScale()));
        double energy = profile.baseEnergy() * (.84 + .16 * Math.sqrt(Math.max(.1, power)));
        int bound = (int) Math.ceil(radius);
        List<Candidate> candidates = new ArrayList<>();
        BlockPos origin = BlockPos.containing(center);
        double vertical = Math.max(1.25, radius * .62);

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
                    double falloff = Math.pow(Math.max(0.0, 1.0 - normalized), .78);
                    double local = energy * (.20 + .80 * falloff);
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

    public static int ray(ServerPlayer player, String spellId, Vec3 start, Vec3 end, double power) {
        if (start == null || end == null) return 0;
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < .05) return 0;
        Vec3 unit = delta.scale(1.0 / length);
        int changed = 0;
        int samples = Math.min(72, Math.max(1, (int) Math.ceil(length / .75)));
        for (int i = 1; i <= samples; i++) {
            Vec3 at = start.add(unit.scale(length * i / (double) samples));
            changed += impact(player, spellId, at, 1.15, power);
            if (changed >= 72 || budget((ServerLevel) player.level()).changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * Makes World Sunder read as a split in the battlefield rather than only another round crater.
     * The irregular seven-point cut reuses impact() and therefore cannot bypass chunk protection,
     * hardness/resistance checks or the shared per-tick mutation budgets.
     */
    public static int fissure(ServerPlayer player, String spellId, Vec3 center,
                              Vec3 direction, double range, double power) {
        if (center == null) return 0;
        Vec3 flat = direction == null ? Vec3.ZERO : new Vec3(direction.x, 0.0, direction.z);
        if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        double halfLength = Math.max(7.0, Math.min(15.0, range * .20));
        int changed = 0;
        for (int i = -3; i <= 3; i++) {
            double t = i / 3.0;
            double weight = 1.0 - Math.abs(t);
            double wobble = Math.sin((i + 3) * 1.73) * (1.0 + weight * .55);
            Vec3 at = center.add(flat.scale(t * halfLength)).add(right.scale(wobble));
            changed += impact(player, spellId, at, 3.0 + weight * 2.2,
                    power * (.78 + weight * .26));
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
            case "fireball" -> new Profile(.72, 7.6, 72, false);
            case "shatter" -> new Profile(.74, 9.0, 88, true);
            case "flame_strike" -> new Profile(.62, 10.5, 96, false);
            case "meteor_shard" -> new Profile(.88, 12.5, 112, false);
            case "disintegrate" -> new Profile(.78, 24.0, 18, false);
            case "delayed_blast_fireball" -> new Profile(.92, 15.5, 150, false);
            case "fire_storm" -> new Profile(.66, 11.5, 52, false);
            case "move_earth" -> new Profile(.54, 11.0, 170, false);
            case "earthquake" -> new Profile(.58, 14.5, 240, false);
            case "meteor_swarm" -> new Profile(.92, 16.5, 34, false);
            case "world_sunder" -> new Profile(.62, 28.0, 320, false);
            case "arcane_annihilation" -> new Profile(.70, 22.0, 48, false);
            case "lightning_bolt" -> new Profile(.30, 10.0, 18, false);
            case "thunderwave" -> new Profile(.34, 6.6, 22, false);
            case "gust_of_wind" -> new Profile(.28, 4.2, 10, false);
            default -> null;
        };
    }
}
