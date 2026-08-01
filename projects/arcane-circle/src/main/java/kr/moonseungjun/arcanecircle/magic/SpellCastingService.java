package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.animal.TamableAnimal;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final long QUEUE_TIMEOUT_TICKS = 200L;

    private static final class FusionQueueState {
        private final List<String> ingredients = new ArrayList<>();
        private long updatedAt;
    }

    private static final Map<UUID, FusionQueueState> FUSION_QUEUES = new HashMap<>();

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

        long now = serverClock(player);
        FusionQueueState queue = FUSION_QUEUES.computeIfAbsent(player.getUUID(), ignored -> new FusionQueueState());
        if (now - queue.updatedAt > QUEUE_TIMEOUT_TICKS) queue.ingredients.clear();
        if (queue.ingredients.size() >= 3) {
            fail(player, "삼중 융합 회로가 이미 가득 찼습니다. X를 놓아 시전하세요.");
            return;
        }

        List<String> proposed = new ArrayList<>(queue.ingredients);
        proposed.add(spellId);
        List<SpellCatalog.FusionFormula> candidates = SpellCatalog.candidatesFor(proposed);
        if (proposed.size() >= 2 && candidates.isEmpty()) {
            fail(player, spell.name() + "을 더하면 완성 가능한 융합식이 없습니다.");
            return;
        }

        queue.ingredients.add(spellId);
        queue.updatedAt = now;
        Optional<SpellCatalog.FusionFormula> exact = SpellCatalog.fusionFor(queue.ingredients);
        String names = displayChain(queue.ingredients, " §7+ §d");
        if (exact.isPresent()) {
            SpellDefinition result = SpellCatalog.spell(exact.get().result()).orElseThrow();
            String extension = SpellCatalog.canExtend(queue.ingredients) ? " §8· 세 번째 회로 추가 가능" : "";
            player.sendOverlayMessage(Component.literal("§5[융합 준비] §d" + names + " §f→ §e"
                    + result.name() + " §7· X를 놓아 시전" + extension));
        } else {
            player.sendOverlayMessage(Component.literal("§5[융합 대기] §d" + names + " §7· 후보 "
                    + candidates.size() + "개 · 주문을 하나 더 선택"));
        }
    }

    public static void commitFusion(ServerPlayer player) {
        FusionQueueState queue = FUSION_QUEUES.remove(player.getUUID());
        if (queue == null || queue.ingredients.isEmpty()) return;
        if (serverClock(player) - queue.updatedAt > QUEUE_TIMEOUT_TICKS) {
            player.sendOverlayMessage(Component.literal("§7[융합 취소] 회로 유지 시간이 지나 해제되었습니다."));
            return;
        }
        List<String> ingredients = List.copyOf(queue.ingredients);
        if (ingredients.size() < 2) {
            player.sendOverlayMessage(Component.literal("§7[융합 취소] 두 개 이상의 주문이 필요합니다."));
            return;
        }
        MagicPlayerData data = data(player);
        castPrepared(player, data, data.prepareFusion(player, ingredients));
    }

    public static void clearFusion(ServerPlayer player) {
        clearFusion(player, false);
    }

    public static void clearFusion(ServerPlayer player, boolean notify) {
        FusionQueueState removed = FUSION_QUEUES.remove(player.getUUID());
        if (notify && removed != null && !removed.ingredients.isEmpty()) {
            player.sendOverlayMessage(Component.literal("§7[융합 취소] 대기 중인 회로를 해제했습니다."));
        }
    }

    public static void clearSession(UUID playerId) {
        FUSION_QUEUES.remove(playerId);
    }

    public static void clearAllSessions() {
        FUSION_QUEUES.clear();
    }

    public static List<String> pendingFusion(ServerPlayer player) {
        FusionQueueState queue = FUSION_QUEUES.get(player.getUUID());
        if (queue == null) return List.of();
        if (serverClock(player) - queue.updatedAt > QUEUE_TIMEOUT_TICKS) {
            FUSION_QUEUES.remove(player.getUUID());
            return List.of();
        }
        return List.copyOf(queue.ingredients);
    }

    private static MagicPlayerData data(ServerPlayer player) {
        return MagicPlayerData.get(((ServerLevel) player.level()).getServer());
    }

    private static long serverClock(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer().overworld().getGameTime();
    }

    private static void castPrepared(ServerPlayer player, MagicPlayerData data, MagicPlayerData.CastPreparation cast) {
        ServerLevel level = (ServerLevel) player.level();
        if (!cast.accepted()) {
            fail(player, cast.message());
            return;
        }

        SpellDefinition spell = cast.spell();
        MagicPlayerData.CooldownStatus cooldown = data.cooldownStatus(player, spell.id());
        if (cooldown.active()) {
            fail(player, String.format("%s 재사용까지 %.1f초", spell.name(), cooldown.remainingTicks() / 20.0));
            return;
        }
        if (!canExecute(player, spell.id(), cast.range())) {
            fail(player, "시전할 대상이나 안전한 공간을 찾지 못했습니다.");
            return;
        }

        prelude(level, player, cast);
        if (!execute(player, spell.id(), cast.range(), cast.power())) {
            fail(player, "시전 조건이 사라져 주문이 중단되었습니다.");
            return;
        }

        data.startCooldown(player, spell.id(), cast.cooldownTicks());
        MagicPlayerData.CastProgress progress = data.completeCast(player, cast);
        MagicPlayerData.MageState state = data.state(player);
        MagicPlayerData.EffectiveStats stats = data.effectiveStats(player);

        if (cast.fusion() && progress.mastery().changed()) {
            String chain = displayChain(cast.ingredients(), " §7× §b");
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
                    48, 0.9, 0.9, 0.9, 0.045);
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

    private static boolean canExecute(ServerPlayer player, String id, double range) {
        return switch (id) {
            case "arcane_dart", "ember", "frost_needle", "flame_lance", "ice_shackles",
                    "chain_bolt", "triune_barrage" -> lookTarget(player, range).isPresent();
            case "wind_blade" -> !lineTargets(player, range, 1.35).isEmpty();
            case "mend" -> player.getHealth() < player.getMaxHealth();
            case "blink", "rift_step" -> findBlinkDestination(player, range).isPresent();
            case "frost_nova" -> !nearbyTargets(player, player.position(), range, 3.0).isEmpty();
            default -> true;
        };
    }

    private static String displayChain(List<String> spellIds, String separator) {
        return spellIds.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .collect(Collectors.joining(separator));
    }

    private static void prelude(ServerLevel level, ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        double radius = cast.fusion() ? 1.75 + Math.max(0, cast.ingredients().size() - 2) * 0.32 : 1.18;
        spellSigil(level, player.position().add(0.0, 0.09, 0.0), cast.spell(), cast.ingredients(), radius);
        if (cast.fusion()) multiSpiral(level, player.position().add(0.0, 1.0, 0.0), cast.ingredients());
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, cast.fusion() ? 1.0F : 0.72F, cast.fusion() ? 0.72F : 1.05F);
        if (cast.fusion()) level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.82F, cast.ingredients().size() == 3 ? 1.55F : 1.35F);
    }

    private static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "arcane_dart" -> bolt(player, range, power, ParticleTypes.ENCHANT, 0, 0);
            case "ember" -> bolt(player, range, power, ParticleTypes.FLAME, 100, 0);
            case "frost_needle" -> bolt(player, range, power, ParticleTypes.SNOWFLAKE, 0, 90);
            case "gale_step" -> dash(player, range, power);
            case "lesser_ward" -> ward(player, false, power);
            case "mend" -> mend(player, power);
            case "blink" -> blink(player, range, power, false);
            case "flame_lance" -> bolt(player, range, power, ParticleTypes.FLAME, 160, 0);
            case "ice_shackles" -> shackles(player, range, power);
            case "wind_blade" -> windBlade(player, range, power);
            case "greater_ward" -> ward(player, true, power);
            case "fireball" -> area(player, range, power, true);
            case "frost_nova" -> area(player, range, power, false);
            case "chain_bolt" -> chainBolt(player, range, power);
            case "rift_step" -> blink(player, range, power, true);
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
                particle == ParticleTypes.ENCHANT ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, 30);
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
        if (freezeBonus > 0) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(),
                mob.getTicksRequiredToFreeze() + freezeBonus + (int) Math.round(power * 8.0)));
        burst(level, mob.getEyePosition(), particle, 20, 0.42);
        return true;
    }

    private static boolean shackles(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return false;
        Mob mob = target.get();
        spiralBeam(level, player.getEyePosition(), mob.getEyePosition(), ParticleTypes.SNOWFLAKE,
                ParticleTypes.END_ROD, 32);
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze()
                + 180 + (int) Math.round(power * 10.0)));
        for (double y = 0.15; y < mob.getBbHeight() + 0.3; y += 0.42) {
            ring(level, mob.position().add(0.0, y, 0.0), Math.max(0.65, mob.getBbWidth() * 0.8),
                    ParticleTypes.SNOWFLAKE, 20);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.35F);
        return true;
    }

    private static boolean dash(ServerPlayer player, double range, double power) {
        Vec3 look = player.getLookAngle().normalize();
        double powerScale = Math.max(0.8, Math.sqrt(Math.max(0.1, power)));
        double strength = Math.max(1.1, range / 4.0) * powerScale;
        player.push(look.x * strength, Math.max(0.15, look.y * 0.35 + 0.15), look.z * strength);
        ServerLevel level = (ServerLevel) player.level();
        for (int index = 0; index < 4; index++) {
            ring(level, player.position().add(0.0, 0.15 + index * 0.18, 0.0), 0.45 + index * 0.17,
                    ParticleTypes.CLOUD, 18);
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
                    ParticleTypes.HAPPY_VILLAGER, 20);
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                16, 0.4, 0.7, 0.4, 0.03);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.7F, 1.4F);
        return true;
    }

    private static boolean blink(ServerPlayer player, double range, double power, boolean rift) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<BlockPos> destinationResult = findBlinkDestination(player, range);
        if (destinationResult.isEmpty()) return false;
        BlockPos origin = player.blockPosition();
        BlockPos destination = destinationResult.get();
        SpellDefinition spell = SpellCatalog.spell(rift ? "rift_step" : "blink").orElseThrow();
        spellSigil(level, player.position().add(0.0, 0.08, 0.0), spell, spell.fusionSources(), rift ? 1.7 : 1.15);
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 54 : 30, 0.5, 0.9, 0.5, 0.12);
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        spellSigil(level, player.position().add(0.0, 0.08, 0.0), spell, spell.fusionSources(), rift ? 1.7 : 1.15);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                rift ? 58 : 32, 0.6, 1.0, 0.6, 0.08);
        if (rift) {
            int duration = 35 + (int) Math.round(power * 24.0);
            int amplifier = power >= 1.6 ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
        }
        level.playSound(null, origin, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9F, rift ? 0.7F : 1.1F);
        return true;
    }

    private static Optional<BlockPos> findBlinkDestination(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        for (int step = (int) Math.floor(range); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(player.position().add(look.scale(step)));
            if (safe(level, candidate)) return Optional.of(candidate);
        }
        return Optional.empty();
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
                    ParticleTypes.CLOUD, ParticleTypes.ENCHANT, 30);
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
        List<Mob> mobs = nearbyTargets(player, center, radius, 3.0);
        if (!fire && mobs.isEmpty()) return false;
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
            else mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze()
                    + 220 + (int) Math.round(power * 8.0)));
        }
        ParticleOptions particle = fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE;
        SpellDefinition spell = SpellCatalog.spell(fire ? "fireball" : "frost_nova").orElseThrow();
        spellSigil(level, center.add(0.0, 0.16, 0.0), spell, spell.fusionSources(), radius);
        for (double r = 1.0; r <= radius; r += 1.25) ring(level, center.add(0.0, 0.2, 0.0), r, particle, 28);
        if (fire) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5, center.z,
                    30, 1.2, 0.8, 1.2, 0.08);
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
                    mob -> validTarget(player, mob) && !chain.contains(mob)).stream()
                    .min(Comparator.comparingDouble(last::distanceToSqr));
            if (next.isEmpty()) break;
            chain.add(next.get());
        }
        Vec3 start = player.getEyePosition();
        for (int index = 0; index < chain.size(); index++) {
            Mob mob = chain.get(index);
            spiralBeam(level, start, mob.getEyePosition(), ParticleTypes.ELECTRIC_SPARK,
                    ParticleTypes.END_ROD, 24);
            mob.hurtServer(level, level.damageSources().magic(),
                    (float) (power * Math.max(0.55, 1.0 - index * 0.1)));
            burst(level, mob.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 14, 0.35);
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
        spiralBeam(level, player.getEyePosition(), primary.getEyePosition(), ParticleTypes.ENCHANT, ParticleTypes.END_ROD, 34);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, primary.getBoundingBox().inflate(3.5),
                mob -> validTarget(player, mob));
        for (Mob mob : mobs) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 120));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 100));
        }
        spellSigil(level, center.add(0.0, 0.15, 0.0), SpellCatalog.spell("triune_barrage").orElseThrow(),
                List.of("arcane_dart", "ember", "frost_needle"), 3.5);
        burst(level, center.add(0.0, 1.0, 0.0), ParticleTypes.FLAME, 22, 1.0);
        burst(level, center.add(0.0, 1.0, 0.0), ParticleTypes.SNOWFLAKE, 22, 1.0);
        burst(level, center.add(0.0, 1.0, 0.0), ParticleTypes.ENCHANT, 22, 1.0);
        level.playSound(null, primary.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 1.35F);
        return true;
    }

    private static boolean tempestAegis(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180 + (int) (power * 6), 2));
        List<Mob> mobs = nearbyTargets(player, player.position(), range, 3.0);
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
        AABB field = player.getBoundingBox().inflate(range, 3.0, range);
        float healing = (float) Math.max(4.0, power * 0.8);
        player.heal(healing);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220, 2));
        for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, field,
                candidate -> candidate.isAlive() && candidate != player && player.isAlliedTo(candidate))) {
            ally.heal(healing);
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
        }
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, field, Mob::isAlive);
        for (Mob mob : mobs) {
            if (isAllied(player, mob)) {
                mob.heal(healing * 0.75F);
                continue;
            }
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 220));
        }
        SpellDefinition spell = SpellCatalog.spell("phoenix_field").orElseThrow();
        spellSigil(level, player.position().add(0.0, 0.1, 0.0), spell, spell.fusionSources(), range);
        for (int layer = 0; layer < 5; layer++) {
            ring(level, player.position().add(0.0, 0.25 + layer * 0.35, 0.0),
                    0.8 + layer * 0.35, layer % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.HAPPY_VILLAGER, 28);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.15F);
        return true;
    }

    private static Optional<Mob> lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0);
        return player.level().getEntitiesOfClass(Mob.class, search,
                        mob -> validTarget(player, mob) && player.hasLineOfSight(mob)).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0.0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.0, mob.getBbWidth() + 0.7);
                })
                .min(Comparator.comparingDouble(mob -> aimScore(eye, look, mob)));
    }

    private static double aimScore(Vec3 eye, Vec3 look, Mob mob) {
        Vec3 to = mob.getEyePosition().subtract(eye);
        double projection = Math.max(0.0, to.dot(look));
        double perpendicular = to.subtract(look.scale(projection)).lengthSqr();
        return perpendicular * 8.0 + projection * 0.01;
    }

    private static List<Mob> lineTargets(ServerPlayer player, double range, double width) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB search = player.getBoundingBox().expandTowards(look.scale(range)).inflate(width + 1.0);
        return player.level().getEntitiesOfClass(Mob.class, search,
                        mob -> validTarget(player, mob) && player.hasLineOfSight(mob)).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0.0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= width;
                }).toList();
    }

    private static List<Mob> nearbyTargets(ServerPlayer player, Vec3 center, double radius, double vertical) {
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(radius, vertical, radius),
                mob -> validTarget(player, mob));
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        return mob.isAlive() && !isAllied(player, mob);
    }

    private static boolean isAllied(ServerPlayer player, Mob mob) {
        if (player.isAlliedTo(mob)) return true;
        return mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player);
    }

    private static boolean safe(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)) return false;
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockPos floorPos = pos.below();
        BlockState floor = level.getBlockState(floorPos);
        return feet.isAir() && head.isAir()
                && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                && floor.isFaceSturdy(level, floorPos, Direction.UP);
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
        int rings = 2 + seed % 2;
        int spokes = 4 + seed % 5;
        for (int ring = 1; ring <= rings; ring++) {
            double ringRadius = radius * ring / rings;
            ring(level, center.add(0.0, ring * 0.003, 0.0), ringRadius, result, 28 + ring * 10);
        }
        schoolMotif(level, center.add(0.0, 0.01, 0.0), radius * 0.74, spell.school(), result);
        for (int spoke = 0; spoke < spokes; spoke++) {
            double angle = Math.PI * 2.0 * spoke / spokes + (seed % 31) * 0.01;
            Vec3 inner = center.add(Math.cos(angle) * radius * 0.23, 0.0, Math.sin(angle) * radius * 0.23);
            Vec3 outer = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            line(level, inner, outer, result, 8);
        }
        if (!ingredients.isEmpty()) {
            for (int index = 0; index < ingredients.size(); index++) {
                SpellDefinition source = SpellCatalog.spell(ingredients.get(index)).orElse(null);
                if (source == null) continue;
                double sourceRadius = radius * (0.32 + index * 0.14);
                ring(level, center.add(0.0, 0.015 + index * 0.003, 0.0), sourceRadius,
                        particleFor(source.school()), 24 + index * 6);
            }
        }
    }

    private static void schoolMotif(ServerLevel level, Vec3 center, double radius,
                                    SpellDefinition.School school, ParticleOptions particle) {
        switch (school) {
            case FIRE -> polygon(level, center, radius, 3, particle);
            case FROST -> {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.PI * i / 3.0;
                    line(level, center, center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius), particle, 10);
                }
                ring(level, center, radius * 0.42, particle, 22);
            }
            case WIND -> {
                for (int i = 0; i < 3; i++) {
                    double angle = Math.PI * 2.0 * i / 3.0;
                    Vec3 shifted = center.add(Math.cos(angle) * radius * 0.28, 0.0, Math.sin(angle) * radius * 0.28);
                    ring(level, shifted, radius * 0.48, particle, 20);
                }
            }
            case WARD -> {
                polygon(level, center, radius, 4, particle);
                polygon(level, center, radius * 0.58, 4, particle);
            }
            case LIFE -> {
                line(level, center.add(-radius, 0.0, 0.0), center.add(radius, 0.0, 0.0), particle, 16);
                line(level, center.add(0.0, 0.0, -radius), center.add(0.0, 0.0, radius), particle, 16);
                polygon(level, center, radius * 0.58, 4, particle);
            }
            case SPACE -> {
                ring(level, center.add(radius * 0.22, 0.0, 0.0), radius * 0.63, particle, 24);
                ring(level, center.add(-radius * 0.22, 0.0, 0.0), radius * 0.63, particle, 24);
            }
            case ARCANE -> star(level, center, radius, 5, particle);
        }
    }

    private static void multiSpiral(ServerLevel level, Vec3 center, List<String> ingredients) {
        int strands = Math.max(2, Math.min(3, ingredients.size()));
        for (int index = 0; index < 36; index++) {
            double progress = index / 35.0;
            double angle = progress * Math.PI * 5.0;
            double radius = 0.92 * (1.0 - progress * 0.72);
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
        if (points == 6) {
            polygon(level, center, radius, 3, particle);
            polygon(level, center, radius, 3, particle, Math.PI);
            return;
        }
        List<Vec3> vertices = new ArrayList<>();
        for (int index = 0; index < points; index++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * index / points;
            vertices.add(center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
        }
        int step = points == 8 ? 3 : 2;
        int cursor = 0;
        for (int index = 0; index < points; index++) {
            int next = (cursor + step) % points;
            line(level, vertices.get(cursor), vertices.get(next), particle, 10);
            cursor = next;
        }
    }

    private static void polygon(ServerLevel level, Vec3 center, double radius, int sides, ParticleOptions particle) {
        polygon(level, center, radius, sides, particle, -Math.PI / 2.0);
    }

    private static void polygon(ServerLevel level, Vec3 center, double radius, int sides,
                                ParticleOptions particle, double rotation) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double angle = rotation + Math.PI * 2.0 * i / sides;
            vertices.add(center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
        }
        for (int i = 0; i < sides; i++) line(level, vertices.get(i), vertices.get((i + 1) % sides), particle, 10);
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
            if (index % 4 == 0) level.sendParticles(accent, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        }
    }

    private static void dome(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        for (int layer = 1; layer <= 5; layer++) {
            double progress = layer / 6.0;
            double y = Math.sin(progress * Math.PI / 2.0) * radius;
            double ringRadius = Math.cos(progress * Math.PI / 2.0) * radius;
            ring(level, center.add(0.0, y, 0.0), ringRadius, particle, 26);
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
