package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum SpellRecipe {
    FLAME_NOVA("불꽃 신성", List.of(MagicRune.FIRE_ESSENCE, MagicRune.RING_FORM, MagicRune.BURST_FUEL), ParticleTypes.FLAME) {
        @Override protected int affectMobs(ServerLevel level, BlockPos pos, Player caster) {
            int affected = 0;
            for (Mob mob : nearbyMobs(level, pos)) {
                mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 120));
                affected++;
            }
            return affected;
        }
    },
    FROST_SEAL("서리 봉인", List.of(MagicRune.FROST_ESSENCE, MagicRune.SEAL_FORM, MagicRune.CALM_FUEL), ParticleTypes.SNOWFLAKE) {
        @Override protected int affectMobs(ServerLevel level, BlockPos pos, Player caster) {
            int affected = 0;
            for (Mob mob : nearbyMobs(level, pos)) {
                int frozen = Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 80);
                mob.setTicksFrozen(frozen);
                affected++;
            }
            return affected;
        }
    },
    ARCANE_PULSE("비전 파동", List.of(MagicRune.VOID_ESSENCE, MagicRune.PULSE_FORM, MagicRune.LIGHT_FUEL), ParticleTypes.PORTAL) {
        @Override protected int affectMobs(ServerLevel level, BlockPos pos, Player caster) {
            int affected = 0;
            double centerX = pos.getX() + 0.5;
            double centerZ = pos.getZ() + 0.5;
            for (Mob mob : nearbyMobs(level, pos)) {
                double dx = mob.getX() - centerX;
                double dz = mob.getZ() - centerZ;
                double length = Math.max(0.2, Math.sqrt(dx * dx + dz * dz));
                mob.push(dx / length * 1.2, 0.45, dz / length * 1.2);
                affected++;
            }
            return affected;
        }
    };

    private final String displayName;
    private final List<MagicRune> runes;
    private final ParticleOptions particle;

    SpellRecipe(String displayName, List<MagicRune> runes, ParticleOptions particle) {
        this.displayName = displayName;
        this.runes = runes;
        this.particle = particle;
    }

    public String displayName() { return displayName; }
    public List<Integer> codes() { return runes.stream().map(MagicRune::code).toList(); }

    public int cast(ServerLevel level, BlockPos pos, Player caster) {
        drawMagicCircle(level, pos, particle);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,
                0.85F + level.random.nextFloat() * 0.25F);
        return affectMobs(level, pos, caster);
    }

    protected abstract int affectMobs(ServerLevel level, BlockPos pos, Player caster);

    public static Optional<SpellRecipe> match(List<Integer> codes) {
        return Arrays.stream(values()).filter(recipe -> recipe.codes().equals(codes)).findFirst();
    }

    private static List<Mob> nearbyMobs(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(6.0, 3.0, 6.0), Mob::isAlive);
    }

    public static void drawMagicCircle(ServerLevel level, BlockPos pos, ParticleOptions particle) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.22;
        double centerZ = pos.getZ() + 0.5;
        for (int index = 0; index < 72; index++) {
            double angle = Math.PI * 2.0 * index / 72.0;
            point(level, particle, centerX + Math.cos(angle) * 2.75, centerY, centerZ + Math.sin(angle) * 2.75);
        }
        double[][] star = new double[5][2];
        for (int index = 0; index < 5; index++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * index / 5.0;
            star[index][0] = centerX + Math.cos(angle) * 2.15;
            star[index][1] = centerZ + Math.sin(angle) * 2.15;
        }
        int[] order = {0, 2, 4, 1, 3, 0};
        for (int index = 0; index < order.length - 1; index++) {
            line(level, particle, star[order[index]], star[order[index + 1]], centerY);
        }
        level.sendParticles(ParticleTypes.END_ROD, centerX, centerY + 0.4, centerZ, 24, 0.7, 0.15, 0.7, 0.04);
    }

    public static void drawInsertionPulse(ServerLevel level, BlockPos pos, int slot) {
        ParticleOptions particle = slot == 1 ? ParticleTypes.ENCHANT : slot == 2 ? ParticleTypes.WITCH : ParticleTypes.END_ROD;
        double radius = 0.75 + slot * 0.45;
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.2;
        double centerZ = pos.getZ() + 0.5;
        for (int index = 0; index < 36; index++) {
            double angle = Math.PI * 2.0 * index / 36.0;
            point(level, particle, centerX + Math.cos(angle) * radius, centerY, centerZ + Math.sin(angle) * radius);
        }
    }

    public static void drawFailure(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5,
                30, 1.2, 0.15, 1.2, 0.02);
    }

    private static void line(ServerLevel level, ParticleOptions particle, double[] start, double[] end, double y) {
        for (int step = 0; step <= 18; step++) {
            double progress = step / 18.0;
            point(level, particle, start[0] + (end[0] - start[0]) * progress, y,
                    start[1] + (end[1] - start[1]) * progress);
        }
    }

    private static void point(ServerLevel level, ParticleOptions particle, double x, double y, double z) {
        level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
