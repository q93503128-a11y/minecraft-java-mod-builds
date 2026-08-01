package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SpellCastingService {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    private SpellCastingService() {}

    public static void cast(ServerPlayer player, int slot) {
        ServerLevel level = (ServerLevel) player.level();
        MagicPlayerData data = MagicPlayerData.get(level.getServer());
        MagicPlayerData.CastPreparation cast = data.prepareCast(player, slot);
        if (!cast.accepted()) { fail(player, cast.message()); return; }

        SpellDefinition spell = cast.spell();
        long now = level.getGameTime();
        long ready = COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .getOrDefault(spell.id(), 0L);
        if (ready > now) {
            fail(player, String.format("%s 재사용까지 %.1f초", spell.name(), (ready - now) / 20.0));
            return;
        }
        if (!execute(player, spell.id(), cast.range(), cast.power())) {
            fail(player, "시전할 대상이나 안전한 공간을 찾지 못했습니다.");
            return;
        }

        COOLDOWNS.get(player.getUUID()).put(spell.id(), now + cast.cooldownTicks());
        MagicPlayerData.CircleAdvance advance = data.completeCast(player, cast.manaCost(), spell.circle());
        MagicPlayerData.MageState state = data.state(player);
        player.sendOverlayMessage(Component.literal("§b" + spell.name() + " §f시전 · 마력 "
                + (int) state.mana() + "/" + state.maxMana() + " · 쿨 "
                + String.format("%.1f", cast.cooldownTicks() / 20.0) + "초"));
        if (advance.advanced()) {
            player.sendSystemMessage(Component.literal("§d[써클 승급] §f마력핵이 §5" + advance.current()
                    + "써클§f로 확장되었습니다. 최대 마력과 저써클 숙련 보정이 증가합니다."));
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                    80, 1.0, 1.2, 1.0, 0.08);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    private static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "arcane_dart" -> bolt(player, range, power, ParticleTypes.ENCHANT, 0, 0);
            case "ember" -> bolt(player, range, power, ParticleTypes.FLAME, 100, 0);
            case "frost_needle" -> bolt(player, range, power, ParticleTypes.SNOWFLAKE, 0, 90);
            case "gale_step" -> dash(player, range);
            case "lesser_ward" -> ward(player, false);
            case "mend" -> mend(player, power);
            case "blink" -> blink(player, range, false);
            case "flame_lance" -> bolt(player, range, power, ParticleTypes.FLAME, 160, 0);
            case "ice_shackles" -> bolt(player, range, power, ParticleTypes.SNOWFLAKE, 0, 180);
            case "wind_blade" -> windBlade(player, range, power);
            case "greater_ward" -> ward(player, true);
            case "fireball" -> area(player, range, power, true);
            case "frost_nova" -> area(player, range, power, false);
            case "chain_bolt" -> chainBolt(player, range, power);
            case "rift_step" -> blink(player, range, true);
            default -> false;
        };
    }

    private static boolean bolt(ServerPlayer player, double range, double power, ParticleOptions particle,
                                int fireTicks, int freezeBonus) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        beam(level, player.getEyePosition(), mob.getEyePosition(), particle, 28);
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
        if (freezeBonus > 0) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(),
                mob.getTicksRequiredToFreeze() + freezeBonus));
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    private static boolean dash(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle().normalize();
        double strength = Math.max(1.1, range / 4.0);
        player.push(look.x * strength, Math.max(0.15, look.y * 0.35 + 0.15), look.z * strength);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(),
                24, 0.35, 0.15, 0.35, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    private static boolean ward(ServerPlayer player, boolean greater) {
        int duration = greater ? 240 : 140;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, greater ? 2 : 0));
        ServerLevel level = (ServerLevel) player.level();
        ring(level, player.position().add(0.0, 0.25, 0.0), greater ? 2.2 : 1.5,
                greater ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, greater ? 64 : 40);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    private static boolean mend(ServerPlayer player, double power) {
        if (player.getHealth() >= player.getMaxHealth()) return false;
        player.heal((float) power);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.6, 0.8, 0.6, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.7F, 1.4F);
        return true;
    }

    private static boolean blink(ServerPlayer player, double range, boolean rift) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        BlockPos origin = player.blockPosition();
        BlockPos destination = null;
        for (int step = (int) Math.floor(range); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(player.position().add(look.scale(step)));
            if (safe(level, candidate)) { destination = candidate; break; }
        }
        if (destination == null) return false;
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 70 : 35, 0.5, 0.9, 0.5, 0.12);
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 80 : 40, 0.6, 1.0, 0.6, 0.08);
        if (rift) player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 50, 0));
        level.playSound(null, origin, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9F, rift ? 0.7F : 1.1F);
        return true;
    }

    private static boolean windBlade(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        List<Mob> targets = lineTargets(player, range, 1.35);
        if (targets.isEmpty()) return false;
        beam(level, start, start.add(look.scale(range)), ParticleTypes.CLOUD, 40);
        for (Mob mob : targets) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.push(look.x * 0.7, 0.18, look.z * 0.7);
        }
        return true;
    }

    private static boolean area(ServerPlayer player, double range, double power, boolean fire) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = fire
                ? lookTarget(player, range).<Vec3>map(Mob::position)
                    .orElse(player.getEyePosition().add(player.getLookAngle().normalize().scale(range)))
                : player.position();
        double radius = fire ? 4.0 : range;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(radius, 3.0, radius), Mob::isAlive);
        if (!fire && mobs.isEmpty()) return false;
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
            else mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 220));
        }
        if (fire) {
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5, center.z, 120, 2.0, 1.2, 2.0, 0.12);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5, center.z, 40, 1.2, 0.8, 1.2, 0.08);
            level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.0F, 1.1F);
        } else {
            for (double ring = 1.0; ring <= radius; ring += 1.2) ring(level, center.add(0, 0.2, 0), ring,
                    ParticleTypes.SNOWFLAKE, 42);
            level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
        }
        return true;
    }

    private static boolean chainBolt(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> first = lookTarget(player, range);
        if (first.isEmpty()) return false;
        List<Mob> chain = new ArrayList<>();
        chain.add(first.get());
        while (chain.size() < 5) {
            Mob last = chain.get(chain.size() - 1);
            Optional<Mob> next = level.getEntitiesOfClass(Mob.class, last.getBoundingBox().inflate(6.0),
                    mob -> mob.isAlive() && !chain.contains(mob)).stream()
                    .min(Comparator.comparingDouble(last::distanceToSqr));
            if (next.isEmpty()) break;
            chain.add(next.get());
        }
        Vec3 start = player.getEyePosition();
        for (int index = 0; index < chain.size(); index++) {
            Mob mob = chain.get(index);
            beam(level, start, mob.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 22);
            mob.hurtServer(level, level.damageSources().magic(),
                    (float) (power * Math.max(0.55, 1.0 - index * 0.1)));
            start = mob.getEyePosition();
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 0.35F, 1.6F);
        return true;
    }

    private static Optional<Mob> lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0);
        return player.level().getEntitiesOfClass(Mob.class, search, Mob::isAlive).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0.0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.0, mob.getBbWidth() + 0.7);
                }).min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private static List<Mob> lineTargets(ServerPlayer player, double range, double width) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(width + 1.0);
        return player.level().getEntitiesOfClass(Mob.class, search, Mob::isAlive).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0.0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= width;
                }).toList();
    }

    private static boolean safe(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }

    private static void beam(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        for (int index = 0; index <= points; index++) {
            Vec3 at = start.lerp(end, index / (double) points);
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0 * index / points;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void fail(ServerPlayer player, String message) {
        player.sendOverlayMessage(Component.literal("§c[시전 실패] §f" + message));
    }
}
