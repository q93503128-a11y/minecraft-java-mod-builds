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
        MagicPlayerData data = MagicPlayerData.get(player.getServer());
        MagicPlayerData.CastPreparation preparation = data.prepareCast(player, slot);
        if (!preparation.accepted()) {
            fail(player, preparation.message());
            return;
        }

        SpellDefinition spell = preparation.spell();
        long gameTime = player.level().getGameTime();
        long readyAt = COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .getOrDefault(spell.id(), 0L);
        if (readyAt > gameTime) {
            double seconds = (readyAt - gameTime) / 20.0;
            fail(player, String.format("%s 재사용까지 %.1f초", spell.name(), seconds));
            return;
        }

        boolean cast = execute(player, spell.id(), preparation.range(), preparation.power());
        if (!cast) {
            fail(player, "시전할 대상이나 안전한 공간을 찾지 못했습니다.");
            return;
        }

        COOLDOWNS.get(player.getUUID()).put(spell.id(), gameTime + preparation.cooldownTicks());
        MagicPlayerData.CircleAdvance advance = data.completeCast(player, preparation.manaCost(), spell.circle());
        MagicPlayerData.MageState state = data.state(player);
        player.sendOverlayMessage(Component.literal("§b" + spell.name() + " §f시전 · 마력 "
                + (int) state.mana() + "/" + state.maxMana() + " · 쿨 "
                + String.format("%.1f", preparation.cooldownTicks() / 20.0) + "초"));
        if (advance.advanced()) {
            player.sendSystemMessage(Component.literal("§d[써클 승급] §f마력핵이 §5" + advance.current()
                    + "써클§f로 확장되었습니다. 최대 마력과 저써클 숙련 보정이 증가합니다."));
            ServerLevel level = (ServerLevel) player.level();
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                    80, 1.0, 1.2, 1.0, 0.08);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    private static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "arcane_dart" -> targetedDamage(player, range, power, ParticleTypes.ENCHANT, 0, 0);
            case "ember" -> targetedDamage(player, range, power, ParticleTypes.FLAME, 100, 0);
            case "frost_needle" -> targetedDamage(player, range, power, ParticleTypes.SNOWFLAKE, 0, 90);
            case "gale_step" -> dash(player, range);
            case "lesser_ward" -> ward(player, false, power);
            case "mend" -> mend(player, power);
            case "blink" -> blink(player, range, false);
            case "flame_lance" -> targetedDamage(player, range, power, ParticleTypes.FLAME, 160, 0);
            case "ice_shackles" -> shackles(player, range, power);
            case "wind_blade" -> windBlade(player, range, power);
            case "greater_ward" -> ward(player, true, power);
            case "fireball" -> fireball(player, range, power);
            case "frost_nova" -> frostNova(player, range, power);
            case "chain_bolt" -> chainBolt(player, range, power);
            case "rift_step" -> blink(player, range, true);
            default -> false;
        };
    }

    private static boolean targetedDamage(ServerPlayer player, double range, double power, ParticleOptions particle,
                                          int fireTicks, int freezeTicks) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = findLookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        beam(level, player.getEyePosition(), mob.getEyePosition(), particle, 28);
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
        if (freezeTicks > 0) {
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + freezeTicks));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, freezeTicks, 1));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    private static boolean dash(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle().normalize();
        double strength = Math.max(1.1, range / 4.0);
        player.push(look.x * strength, Math.max(0.15, look.y * 0.35 + 0.15), look.z * strength);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(),
                24, 0.35, 0.15, 0.35, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    private static boolean ward(ServerPlayer player, boolean greater, double power) {
        int duration = greater ? 240 : 140;
        int absorptionAmplifier = greater ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, absorptionAmplifier));
        if (greater) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
        ServerLevel level = (ServerLevel) player.level();
        ring(level, player.position().add(0.0, 0.25, 0.0), greater ? 2.2 : 1.5,
                greater ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, greater ? 64 : 40);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.9F);
        return true;
    }

    private static boolean mend(ServerPlayer player, double power) {
        if (player.getHealth() >= player.getMaxHealth()) return false;
        player.heal((float) power);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.6, 0.8, 0.6, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.4F);
        return true;
    }

    private static boolean blink(ServerPlayer player, double range, boolean rift) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        BlockPos origin = player.blockPosition();
        BlockPos destination = null;
        for (int step = (int) Math.floor(range); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(player.position().add(look.scale(step)));
            if (isSafe(level, candidate)) {
                destination = candidate;
                break;
            }
        }
        if (destination == null) return false;
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 70 : 35, 0.5, 0.9, 0.5, 0.12);
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, true);
        if (rift) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 80 : 40, 0.6, 1.0, 0.6, 0.08);
        level.playSound(null, origin, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, rift ? 0.7F : 1.1F);
        return true;
    }

    private static boolean shackles(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = findLookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 180));
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 4));
        ring(level, mob.position().add(0, 0.2, 0), 1.2, ParticleTypes.SNOWFLAKE, 48);
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

    private static boolean fireball(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = findLookTarget(player, range).<Vec3>map(Mob::position)
                .orElse(player.getEyePosition().add(player.getLookAngle().normalize().scale(range)));
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(4.0), Mob::isAlive);
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5, center.z, 120, 2.0, 1.2, 2.0, 0.12);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5, center.z, 40, 1.2, 0.8, 1.2, 0.08);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.1F);
        return true;
    }

    private static boolean frostNova(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = player.position();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range, 3.0, range), Mob::isAlive);
        if (mobs.isEmpty()) return false;
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 220));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 3));
        }
        for (double radius = 1.0; radius <= range; radius += 1.2) ring(level, center.add(0, 0.2, 0), radius, ParticleTypes.SNOWFLAKE, 42);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
        return true;
    }

    private static boolean chainBolt(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> first = findLookTarget(player, range);
        if (first.isEmpty()) return false;
        List<Mob> chain = new ArrayList<>();
        chain.add(first.get());
        while (chain.size() < 5) {
            Mob last = chain.get(chain.size() - 1);
            Optional<Mob> next = level.getEntitiesOfClass(Mob.class, last.getBoundingBox().inflate(6.0),
                            mob -> mob.isAlive() && !chain.contains(mob))
                    .stream().min(Comparator.comparingDouble(last::distanceToSqr));
            if (next.isEmpty()) break;
            chain.add(next.get());
        }
        Vec3 start = player.getEyePosition();
        for (int index = 0; index < chain.size(); index++) {
            Mob mob = chain.get(index);
            beam(level, start, mob.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 22);
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * Math.max(0.55, 1.0 - index * 0.1)));
            start = mob.getEyePosition();
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.35F, 1.6F);
        return true;
    }

    private static Optional<Mob> findLookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0);
        return player.level().getEntitiesOfClass(Mob.class, search, Mob::isAlive).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    if (projection < 0.0 || projection > range) return false;
                    double sideways = to.subtract(look.scale(projection)).length();
                    return sideways <= Math.max(1.0, mob.getBbWidth() + 0.7);
                })
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
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

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }

    private static void beam(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        for (int i = 0; i <= points; i++) {
            double progress = i / (double) points;
            Vec3 at = start.lerp(end, progress);
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void fail(ServerPlayer player, String message) {
        player.sendOverlayMessage(Component.literal("§c[시전 실패] §f" + message));
    }
}
