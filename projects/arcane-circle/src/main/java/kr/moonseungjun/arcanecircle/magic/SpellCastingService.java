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

    public static void cast(ServerPlayer player, boolean fusion) {
        ServerLevel level = (ServerLevel) player.level();
        MagicPlayerData data = MagicPlayerData.get(level.getServer());
        MagicPlayerData.CastPreparation cast = fusion ? data.prepareFusion(player) : data.prepareDirect(player);
        if (!cast.accepted()) {
            fail(player, cast.message());
            return;
        }

        SpellDefinition spell = cast.spell();
        long now = level.getGameTime();
        long ready = COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .getOrDefault(spell.id(), 0L);
        if (ready > now) {
            fail(player, String.format("%s 재사용까지 %.1f초", spell.name(), (ready - now) / 20.0));
            return;
        }

        prelude(level, player, spell, fusion);
        if (!execute(player, spell.id(), cast.range(), cast.power())) {
            fail(player, "시전할 대상이나 안전한 공간을 찾지 못했습니다.");
            return;
        }

        COOLDOWNS.get(player.getUUID()).put(spell.id(), now + cast.cooldownTicks());
        MagicPlayerData.CastProgress progress = data.completeCast(player, cast);
        MagicPlayerData.MageState state = data.state(player);

        if (fusion && progress.mastery().changed()) {
            SpellDefinition focus = SpellCatalog.spell(state.focus()).orElseThrow();
            SpellDefinition weave = SpellCatalog.spell(state.weave()).orElseThrow();
            player.sendOverlayMessage(Component.literal("§d" + focus.name() + " §7× §b" + weave.name()
                    + " §f→ §e" + spell.name() + " §7· 숙련 " + progress.mastery().casts()
                    + "/" + progress.mastery().required()));
        } else {
            player.sendOverlayMessage(Component.literal("§b" + spell.name() + " §f시전 · 마력 "
                    + (int) state.mana() + "/" + state.maxMana() + " · 쿨 "
                    + String.format("%.1f", cast.cooldownTicks() / 20.0) + "초"));
        }

        if (progress.mastery().registered()) {
            player.sendSystemMessage(Component.literal("§6[융합 각인] §f" + spell.name()
                    + "의 완성 회로가 마력핵에 새겨졌습니다. 이제 단독 주문으로 선택할 수 있습니다."));
            sigil(level, player.position().add(0.0, 0.12, 0.0), 2.2, ParticleTypes.END_ROD, 84, 9);
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                    72, 1.0, 1.0, 1.0, 0.05);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        if (progress.circle().advanced()) {
            player.sendSystemMessage(Component.literal("§d[써클 승급] §f마력핵이 §5" + progress.circle().current()
                    + "써클§f로 확장되었습니다."));
            sigil(level, player.position().add(0.0, 0.1, 0.0), 2.6, ParticleTypes.END_ROD, 96, 9);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    private static void prelude(ServerLevel level, ServerPlayer player, SpellDefinition spell, boolean fusion) {
        ParticleOptions result = particleFor(spell.school());
        sigil(level, player.position().add(0.0, 0.09, 0.0), fusion ? 1.65 : 1.15,
                result, fusion ? 64 : 42, fusion ? 8 : 5);
        if (fusion && spell.fusionSources().size() == 2) {
            ParticleOptions first = SpellCatalog.spell(spell.fusionSources().get(0))
                    .map(source -> particleFor(source.school())).orElse(ParticleTypes.ENCHANT);
            ParticleOptions second = SpellCatalog.spell(spell.fusionSources().get(1))
                    .map(source -> particleFor(source.school())).orElse(ParticleTypes.WITCH);
            dualSpiral(level, player.position().add(0.0, 1.0, 0.0), first, second);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, fusion ? 1.0F : 0.75F, fusion ? 0.72F : 1.05F);
        if (fusion) level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.85F, 1.35F);
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
            case "ice_shackles" -> shackles(player, range, power);
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
        spiralBeam(level, player.getEyePosition(), mob.getEyePosition(), particle,
                particle == ParticleTypes.ENCHANT ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, 34);
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
        if (freezeBonus > 0) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(),
                mob.getTicksRequiredToFreeze() + freezeBonus));
        burst(level, mob.getEyePosition(), particle, 24, 0.45);
        return true;
    }

    private static boolean shackles(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        spiralBeam(level, player.getEyePosition(), mob.getEyePosition(), ParticleTypes.SNOWFLAKE,
                ParticleTypes.END_ROD, 36);
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 180));
        for (double y = 0.15; y < mob.getBbHeight() + 0.3; y += 0.35) {
            ring(level, mob.position().add(0.0, y, 0.0), Math.max(0.65, mob.getBbWidth() * 0.8),
                    ParticleTypes.SNOWFLAKE, 24);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.35F);
        return true;
    }

    private static boolean dash(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle().normalize();
        double strength = Math.max(1.1, range / 4.0);
        player.push(look.x * strength, Math.max(0.15, look.y * 0.35 + 0.15), look.z * strength);
        ServerLevel level = (ServerLevel) player.level();
        for (int index = 0; index < 5; index++) {
            ring(level, player.position().add(0.0, 0.15 + index * 0.16, 0.0), 0.45 + index * 0.15,
                    ParticleTypes.CLOUD, 20);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    private static boolean ward(ServerPlayer player, boolean greater) {
        int duration = greater ? 240 : 140;
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, greater ? 2 : 0));
        ServerLevel level = (ServerLevel) player.level();
        ParticleOptions particle = greater ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT;
        double radius = greater ? 2.3 : 1.55;
        sigil(level, player.position().add(0.0, 0.08, 0.0), radius, particle, greater ? 72 : 48, 6);
        dome(level, player.position().add(0.0, 0.2, 0.0), radius, particle);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, greater ? 0.72F : 0.95F);
        return true;
    }

    private static boolean mend(ServerPlayer player, double power) {
        if (player.getHealth() >= player.getMaxHealth()) return false;
        player.heal((float) power);
        ServerLevel level = (ServerLevel) player.level();
        for (int layer = 0; layer < 4; layer++) {
            ring(level, player.position().add(0.0, 0.2 + layer * 0.42, 0.0), 0.55 + layer * 0.12,
                    ParticleTypes.HAPPY_VILLAGER, 24);
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.4, 0.7, 0.4, 0.03);
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
        sigil(level, player.position().add(0.0, 0.08, 0.0), rift ? 1.7 : 1.15,
                rift ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL, rift ? 70 : 44, rift ? 8 : 5);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 70 : 35, 0.5, 0.9, 0.5, 0.12);
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        sigil(level, player.position().add(0.0, 0.08, 0.0), rift ? 1.7 : 1.15,
                ParticleTypes.END_ROD, rift ? 70 : 44, rift ? 8 : 5);
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
        for (int blade = -1; blade <= 1; blade++) {
            Vec3 offset = new Vec3(0.0, blade * 0.22, 0.0);
            spiralBeam(level, start.add(offset), start.add(look.scale(range)).add(offset),
                    ParticleTypes.CLOUD, ParticleTypes.ENCHANT, 36);
        }
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
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class,
                new AABB(center, center).inflate(radius, 3.0, radius), Mob::isAlive);
        if (!fire && mobs.isEmpty()) return false;
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
            else mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 220));
        }
        ParticleOptions particle = fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE;
        sigil(level, center.add(0.0, 0.16, 0.0), radius, particle, fire ? 96 : 84, 10);
        for (double r = 1.0; r <= radius; r += 1.1) ring(level, center.add(0.0, 0.2, 0.0), r, particle, 36);
        if (fire) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5, center.z,
                    40, 1.2, 0.8, 1.2, 0.08);
            level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.0F, 1.1F);
        } else {
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
            spiralBeam(level, start, mob.getEyePosition(), ParticleTypes.ELECTRIC_SPARK,
                    ParticleTypes.END_ROD, 26);
            mob.hurtServer(level, level.damageSources().magic(),
                    (float) (power * Math.max(0.55, 1.0 - index * 0.1)));
            burst(level, mob.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 16, 0.35);
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

    private static ParticleOptions particleFor(SpellDefinition.School school) {
        return switch (school) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case WIND -> ParticleTypes.CLOUD;
            case WARD -> ParticleTypes.END_ROD;
            case LIFE -> ParticleTypes.HAPPY_VILLAGER;
            case SPACE -> ParticleTypes.PORTAL;
            default -> ParticleTypes.ENCHANT;
        };
    }

    private static void dualSpiral(ServerLevel level, Vec3 center, ParticleOptions first, ParticleOptions second) {
        for (int index = 0; index < 40; index++) {
            double progress = index / 39.0;
            double angle = progress * Math.PI * 4.0;
            double radius = 0.85 * (1.0 - progress * 0.72);
            double y = -0.65 + progress * 1.35;
            level.sendParticles(first, center.x + Math.cos(angle) * radius, center.y + y,
                    center.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            level.sendParticles(second, center.x + Math.cos(angle + Math.PI) * radius, center.y + y,
                    center.z + Math.sin(angle + Math.PI) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void spiralBeam(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions primary,
                                   ParticleOptions accent, int points) {
        for (int index = 0; index <= points; index++) {
            double progress = index / (double) points;
            Vec3 center = start.lerp(end, progress);
            double angle = progress * Math.PI * 6.0;
            double radius = 0.10 + Math.sin(progress * Math.PI) * 0.08;
            Vec3 at = center.add(Math.cos(angle) * radius, Math.sin(angle) * radius,
                    Math.sin(angle + Math.PI / 2.0) * radius);
            level.sendParticles(primary, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            if (index % 3 == 0) level.sendParticles(accent, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        }
    }

    private static void sigil(ServerLevel level, Vec3 center, double radius, ParticleOptions particle,
                              int points, int spokes) {
        ring(level, center, radius, particle, points);
        ring(level, center, radius * 0.58, particle, Math.max(24, points / 2));
        for (int spoke = 0; spoke < spokes; spoke++) {
            double angle = Math.PI * 2.0 * spoke / spokes;
            Vec3 inner = center.add(Math.cos(angle) * radius * 0.22, 0.0, Math.sin(angle) * radius * 0.22);
            Vec3 outer = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            line(level, inner, outer, particle, 12);
        }
    }

    private static void dome(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        for (int layer = 1; layer <= 5; layer++) {
            double progress = layer / 6.0;
            double y = Math.sin(progress * Math.PI / 2.0) * radius;
            double ringRadius = Math.cos(progress * Math.PI / 2.0) * radius;
            ring(level, center.add(0.0, y, 0.0), ringRadius, particle, 32);
        }
    }

    private static void line(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
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

    private static void burst(ServerLevel level, Vec3 center, ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, center.x, center.y, center.z, count, spread, spread, spread, 0.04);
    }

    private static void fail(ServerPlayer player, String message) {
        player.sendOverlayMessage(Component.literal("§c[시전 실패] §f" + message));
    }
}
