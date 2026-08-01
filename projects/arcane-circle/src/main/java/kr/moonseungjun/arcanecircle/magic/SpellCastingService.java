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
import java.util.stream.Collectors;

public final class SpellCastingService {
    private record CooldownState(long readyAt, int totalTicks) {}

    private static final Map<UUID, Map<String, CooldownState>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, List<String>> FUSION_QUEUES = new HashMap<>();

    private SpellCastingService() {}

    public static void castSlot(ServerPlayer player, int slot) {
        MagicPlayerData data = data(player);
        castPrepared(player, data, data.prepareSlot(player, slot));
    }

    public static void queueFusionSlot(ServerPlayer player, int slot) {
        MagicPlayerData.MageState state = data(player).state(player);
        String spellId = state.slot(slot);
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (spell == null || spell.circle() > state.circle() || !state.known().contains(spellId)) {
            fail(player, "현재 사용할 수 없는 주문 슬롯입니다.");
            return;
        }
        List<String> queue = FUSION_QUEUES.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        if (queue.size() >= 3) {
            fail(player, "삼중 융합 회로가 이미 가득 찼습니다. X를 놓아 시전하세요.");
            return;
        }
        queue.add(spellId);
        String names = queue.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .collect(Collectors.joining(" §7+ §d"));
        player.sendOverlayMessage(Component.literal("§5[융합 대기] §d" + names + " §7(" + queue.size() + "/3)"));
    }

    public static void commitFusion(ServerPlayer player) {
        List<String> ingredients = new ArrayList<>(FUSION_QUEUES.getOrDefault(player.getUUID(), List.of()));
        FUSION_QUEUES.remove(player.getUUID());
        if (ingredients.isEmpty()) return;
        if (ingredients.size() < 2) {
            fail(player, "융합하려면 X를 누른 채 주문을 두 개 이상 선택해야 합니다.");
            return;
        }
        MagicPlayerData data = data(player);
        castPrepared(player, data, data.prepareFusion(player, ingredients));
    }

    public static void clearFusion(ServerPlayer player) {
        FUSION_QUEUES.remove(player.getUUID());
    }

    public static List<String> pendingFusion(ServerPlayer player) {
        return List.copyOf(FUSION_QUEUES.getOrDefault(player.getUUID(), List.of()));
    }

    public static String cooldownSnapshot(ServerPlayer player) {
        long now = ((ServerLevel) player.level()).getGameTime();
        Map<String, CooldownState> cooldowns = COOLDOWNS.getOrDefault(player.getUUID(), Map.of());
        return cooldowns.entrySet().stream()
                .filter(entry -> entry.getValue().readyAt() > now)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + (entry.getValue().readyAt() - now) + ":" + entry.getValue().totalTicks())
                .collect(Collectors.joining("|"));
    }

    private static MagicPlayerData data(ServerPlayer player) {
        return MagicPlayerData.get(((ServerLevel) player.level()).getServer());
    }

    private static void castPrepared(ServerPlayer player, MagicPlayerData data, MagicPlayerData.CastPreparation cast) {
        ServerLevel level = (ServerLevel) player.level();
        if (!cast.accepted()) {
            fail(player, cast.message());
            return;
        }

        SpellDefinition spell = cast.spell();
        long now = level.getGameTime();
        CooldownState cooldown = COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .get(spell.id());
        if (cooldown != null && cooldown.readyAt() > now) {
            fail(player, String.format("%s 재사용까지 %.1f초", spell.name(), (cooldown.readyAt() - now) / 20.0));
            return;
        }

        prelude(level, player, cast);
        if (!execute(player, spell.id(), cast.range(), cast.power())) {
            fail(player, "시전할 대상이나 안전한 공간을 찾지 못했습니다.");
            return;
        }

        COOLDOWNS.get(player.getUUID()).put(spell.id(), new CooldownState(now + cast.cooldownTicks(), cast.cooldownTicks()));
        MagicPlayerData.CastProgress progress = data.completeCast(player, cast);
        MagicPlayerData.MageState state = data.state(player);
        MagicPlayerData.EffectiveStats stats = data.effectiveStats(player);

        if (cast.fusion() && progress.mastery().changed()) {
            String chain = cast.ingredients().stream()
                    .map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                    .collect(Collectors.joining(" §7× §b"));
            player.sendOverlayMessage(Component.literal("§d" + chain + " §f→ §e" + spell.name()
                    + " §7· 숙련 " + progress.mastery().casts() + "/" + progress.mastery().required()));
        } else {
            player.sendOverlayMessage(Component.literal("§b" + spell.name() + " §f시전 · 마력 "
                    + (int) state.mana() + "/" + stats.maxMana() + " · 쿨 "
                    + String.format("%.1f", cast.cooldownTicks() / 20.0) + "초"));
        }

        if (progress.mastery().registered()) {
            player.sendSystemMessage(Component.literal("§6[융합 각인] §f" + spell.name()
                    + "의 완성 회로가 마력핵에 새겨졌습니다. 이제 1~5 슬롯에 장착할 수 있습니다."));
            spellSigil(level, player.position().add(0.0, 0.12, 0.0), spell, cast.ingredients(), 2.4);
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                    72, 1.0, 1.0, 1.0, 0.05);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        if (progress.circle().advanced()) {
            player.sendSystemMessage(Component.literal("§d[써클 승급] §f마력핵이 §5" + progress.circle().current()
                    + "써클§f로 확장되었습니다."));
            spellSigil(level, player.position().add(0.0, 0.1, 0.0), spell, cast.ingredients(), 2.8);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    private static void prelude(ServerLevel level, ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        double radius = cast.fusion() ? 1.85 + Math.max(0, cast.ingredients().size() - 2) * 0.35 : 1.25;
        spellSigil(level, player.position().add(0.0, 0.09, 0.0), cast.spell(), cast.ingredients(), radius);
        if (cast.fusion()) multiSpiral(level, player.position().add(0.0, 1.0, 0.0), cast.ingredients());
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, cast.fusion() ? 1.0F : 0.75F, cast.fusion() ? 0.72F : 1.05F);
        if (cast.fusion()) level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.85F, cast.ingredients().size() == 3 ? 1.55F : 1.35F);
    }

    private static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "arcane_dart" -> bolt(player, range, power, ParticleTypes.ENCHANT, 0, 0);
            case "ember" -> bolt(player, range, power, ParticleTypes.FLAME, 100, 0);
            case "frost_needle" -> bolt(player, range, power, ParticleTypes.SNOWFLAKE, 0, 90);
            case "gale_step" -> dash(player, range);
            case "lesser_ward" -> ward(player, false, power);
            case "mend" -> mend(player, power);
            case "blink" -> blink(player, range, false);
            case "flame_lance" -> bolt(player, range, power, ParticleTypes.FLAME, 160, 0);
            case "ice_shackles" -> shackles(player, range, power);
            case "wind_blade" -> windBlade(player, range, power);
            case "greater_ward" -> ward(player, true, power);
            case "fireball" -> area(player, range, power, true);
            case "frost_nova" -> area(player, range, power, false);
            case "chain_bolt" -> chainBolt(player, range, power);
            case "rift_step" -> blink(player, range, true);
            case "triune_barrage" -> triuneBarrage(player, range, power);
            case "tempest_aegis" -> tempestAegis(player, range, power);
            case "phoenix_field" -> phoenixField(player, range, power);
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

    private static boolean ward(ServerPlayer player, boolean greater, double power) {
        int bonus = Math.max(0, (int) Math.round(power * 8.0));
        int duration = (greater ? 240 : 140) + bonus;
        int amplifier = greater ? Math.max(2, (int) Math.floor(power / 8.0)) : Math.max(0, (int) Math.floor(power / 6.0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
        ServerLevel level = (ServerLevel) player.level();
        ParticleOptions particle = greater ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT;
        double radius = greater ? 2.3 : 1.55;
        spellSigil(level, player.position().add(0.0, 0.08, 0.0),
                SpellCatalog.spell(greater ? "greater_ward" : "lesser_ward").orElseThrow(), List.of(), radius);
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
        SpellDefinition spell = SpellCatalog.spell(rift ? "rift_step" : "blink").orElseThrow();
        spellSigil(level, player.position().add(0.0, 0.08, 0.0), spell, spell.fusionSources(), rift ? 1.7 : 1.15);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 70 : 35, 0.5, 0.9, 0.5, 0.12);
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        spellSigil(level, player.position().add(0.0, 0.08, 0.0), spell, spell.fusionSources(), rift ? 1.7 : 1.15);
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
        SpellDefinition spell = SpellCatalog.spell(fire ? "fireball" : "frost_nova").orElseThrow();
        spellSigil(level, center.add(0.0, 0.16, 0.0), spell, spell.fusionSources(), radius);
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

    private static boolean triuneBarrage(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob primary = target.get();
        Vec3 center = primary.position();
        spiralBeam(level, player.getEyePosition(), primary.getEyePosition(), ParticleTypes.ENCHANT, ParticleTypes.END_ROD, 42);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, primary.getBoundingBox().inflate(3.5), Mob::isAlive);
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 120));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 100));
        }
        spellSigil(level, center.add(0.0, 0.15, 0.0), SpellCatalog.spell("triune_barrage").orElseThrow(),
                List.of("arcane_dart", "ember", "frost_needle"), 3.5);
        burst(level, center.add(0.0, 1.0, 0.0), ParticleTypes.FLAME, 28, 1.1);
        burst(level, center.add(0.0, 1.0, 0.0), ParticleTypes.SNOWFLAKE, 28, 1.1);
        burst(level, center.add(0.0, 1.0, 0.0), ParticleTypes.ENCHANT, 28, 1.1);
        level.playSound(null, primary.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 1.35F);
        return true;
    }

    private static boolean tempestAegis(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180 + (int) (power * 6), 2));
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range, 3.0, range), Mob::isAlive);
        for (Mob mob : mobs) {
            Vec3 away = mob.position().subtract(player.position()).normalize();
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * 0.65));
            mob.push(away.x * 1.45, 0.45, away.z * 1.45);
        }
        SpellDefinition spell = SpellCatalog.spell("tempest_aegis").orElseThrow();
        spellSigil(level, player.position().add(0.0, 0.1, 0.0), spell, spell.fusionSources(), range);
        dome(level, player.position().add(0.0, 0.2, 0.0), Math.min(range, 4.5), ParticleTypes.CLOUD);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8F, 0.75F);
        return true;
    }

    private static boolean phoenixField(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        player.heal((float) Math.max(4.0, power * 0.8));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, 2));
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range, 3.0, range), Mob::isAlive);
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 220));
        }
        SpellDefinition spell = SpellCatalog.spell("phoenix_field").orElseThrow();
        spellSigil(level, player.position().add(0.0, 0.1, 0.0), spell, spell.fusionSources(), range);
        for (int layer = 0; layer < 5; layer++) {
            ring(level, player.position().add(0.0, 0.25 + layer * 0.35, 0.0),
                    0.8 + layer * 0.35, layer % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.HAPPY_VILLAGER, 36);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.15F);
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

    public static ParticleOptions particleFor(SpellDefinition.School school) {
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

    private static void spellSigil(ServerLevel level, Vec3 center, SpellDefinition spell,
                                   List<String> ingredients, double radius) {
        ParticleOptions result = particleFor(spell.school());
        int seed = spell.id().hashCode() & 0x7fffffff;
        int rings = 2 + seed % 3;
        int spokes = 4 + seed % 6;
        int starPoints = 5 + seed % 4;
        for (int ring = 1; ring <= rings; ring++) {
            double ringRadius = radius * ring / rings;
            ring(level, center.add(0.0, ring * 0.003, 0.0), ringRadius, result, 36 + ring * 12);
        }
        for (int spoke = 0; spoke < spokes; spoke++) {
            double angle = Math.PI * 2.0 * spoke / spokes + (seed % 31) * 0.01;
            Vec3 inner = center.add(Math.cos(angle) * radius * 0.18, 0.0, Math.sin(angle) * radius * 0.18);
            Vec3 outer = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            line(level, inner, outer, result, 12);
        }
        star(level, center.add(0.0, 0.01, 0.0), radius * 0.72, starPoints, result);
        if (!ingredients.isEmpty()) {
            for (int index = 0; index < ingredients.size(); index++) {
                SpellDefinition source = SpellCatalog.spell(ingredients.get(index)).orElse(null);
                if (source == null) continue;
                double sourceRadius = radius * (0.32 + index * 0.13);
                ring(level, center.add(0.0, 0.015 + index * 0.003, 0.0), sourceRadius,
                        particleFor(source.school()), 30 + index * 8);
            }
        }
    }

    private static void multiSpiral(ServerLevel level, Vec3 center, List<String> ingredients) {
        int strands = Math.max(2, Math.min(3, ingredients.size()));
        for (int index = 0; index < 48; index++) {
            double progress = index / 47.0;
            double angle = progress * Math.PI * 5.0;
            double radius = 0.95 * (1.0 - progress * 0.72);
            double y = -0.72 + progress * 1.55;
            for (int strand = 0; strand < strands; strand++) {
                SpellDefinition source = SpellCatalog.spell(ingredients.get(strand)).orElse(null);
                ParticleOptions particle = source == null ? ParticleTypes.ENCHANT : particleFor(source.school());
                double shifted = angle + Math.PI * 2.0 * strand / strands;
                level.sendParticles(particle, center.x + Math.cos(shifted) * radius, center.y + y,
                        center.z + Math.sin(shifted) * radius, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void star(ServerLevel level, Vec3 center, double radius, int points, ParticleOptions particle) {
        List<Vec3> vertices = new ArrayList<>();
        for (int index = 0; index < points; index++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * index / points;
            vertices.add(center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
        }
        int step = points <= 5 ? 2 : 3;
        int cursor = 0;
        for (int index = 0; index < points; index++) {
            int next = (cursor + step) % points;
            line(level, vertices.get(cursor), vertices.get(next), particle, 12);
            cursor = next;
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
