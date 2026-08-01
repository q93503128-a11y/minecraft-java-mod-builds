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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
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
    private static final long CHARGE_TIMEOUT_TICKS = 240L;

    private static final class FusionQueueState {
        private final List<String> ingredients = new ArrayList<>();
        private long updatedAt;
    }

    private static final class ChargeState {
        private final int slot;
        private final String spellId;
        private final long startedAt;

        private ChargeState(int slot, String spellId, long startedAt) {
            this.slot = slot;
            this.spellId = spellId;
            this.startedAt = startedAt;
        }
    }

    private static final Map<UUID, FusionQueueState> FUSION_QUEUES = new HashMap<>();
    private static final Map<UUID, ChargeState> CHARGES = new HashMap<>();

    private SpellCastingService() {}

    public static void beginSlotCharge(ServerPlayer player, int slot) {
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, slot);
        if (!cast.accepted()) {
            fail(player, cast.message());
            return;
        }
        MagicPlayerData.CooldownStatus cooldown = data.cooldownStatus(player, cast.spell().id());
        if (cooldown.active()) {
            fail(player, String.format("%s 재사용까지 %.1f초", cast.spell().name(), cooldown.remainingTicks() / 20.0));
            return;
        }
        clearFusion(player, false);
        ChargeState previous = CHARGES.put(player.getUUID(),
                new ChargeState(slot, cast.spell().id(), serverClock(player)));
        if (previous == null || !previous.spellId.equals(cast.spell().id())) {
            player.sendOverlayMessage(Component.literal("§5[회로 전개] §f" + cast.spell().name()
                    + " §7· 숫자키를 놓으면 시전"));
            ServerLevel level = (ServerLevel) player.level();
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 0.55F, 1.18F);
        }
        renderCharge(player, cast.spell(), 0L, cast.range());
    }

    public static void releaseSlotCharge(ServerPlayer player, int slot) {
        ChargeState charge = CHARGES.remove(player.getUUID());
        if (charge == null || charge.slot != slot) return;
        if (serverClock(player) - charge.startedAt > CHARGE_TIMEOUT_TICKS) {
            player.sendOverlayMessage(Component.literal("§7[시전 취소] 유지 한계를 넘어 마법진이 해제되었습니다."));
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareSlot(player, slot);
        if (!cast.accepted() || !charge.spellId.equals(cast.spell().id())) {
            fail(player, cast.accepted() ? "충전 중 주문 슬롯이 변경되었습니다." : cast.message());
            return;
        }
        castPrepared(player, data, cast);
    }

    /** Compatibility entry point used only by old integrations; new controls always use begin/release. */
    public static void castSlot(ServerPlayer player, int slot) {
        beginSlotCharge(player, slot);
        releaseSlotCharge(player, slot);
    }

    public static void tickCharge(ServerPlayer player) {
        ChargeState charge = CHARGES.get(player.getUUID());
        if (charge == null) return;
        long now = serverClock(player);
        long elapsed = now - charge.startedAt;
        if (!player.isAlive() || player.isSpectator() || elapsed > CHARGE_TIMEOUT_TICKS) {
            CHARGES.remove(player.getUUID());
            if (elapsed > CHARGE_TIMEOUT_TICKS) {
                player.sendOverlayMessage(Component.literal("§7[시전 취소] 마법진 유지 시간이 끝났습니다."));
            }
            return;
        }
        if ((player.tickCount & 1) != 0) return;
        SpellDefinition spell = SpellCatalog.spell(charge.spellId).orElse(null);
        if (spell == null || !data(player).state(player).known().contains(spell.id())) {
            CHARGES.remove(player.getUUID());
            return;
        }
        MagicPlayerData.CastPreparation cast = data(player).prepareSlot(player, charge.slot);
        if (!cast.accepted()) {
            CHARGES.remove(player.getUUID());
            return;
        }
        renderCharge(player, spell, elapsed, cast.range());
    }

    public static void cancelCharge(ServerPlayer player, boolean notify) {
        ChargeState removed = CHARGES.remove(player.getUUID());
        if (notify && removed != null) {
            player.sendOverlayMessage(Component.literal("§7[시전 취소] 전개한 마법진을 해제했습니다."));
        }
    }

    public static boolean shouldBlockHotbarSwitch(ServerPlayer player) {
        return CHARGES.containsKey(player.getUUID());
    }

    public static String chargingSpell(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        return state == null ? "" : state.spellId;
    }

    public static int chargingSlot(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        return state == null ? -1 : state.slot;
    }

    public static int chargingTicks(ServerPlayer player) {
        ChargeState state = CHARGES.get(player.getUUID());
        if (state == null) return 0;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, serverClock(player) - state.startedAt));
    }

    public static void queueFusionSlot(ServerPlayer player, int slot) {
        cancelCharge(player, false);
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
        CHARGES.remove(playerId);
    }

    public static void clearAllSessions() {
        FUSION_QUEUES.clear();
        CHARGES.clear();
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

        releasePrelude(player, cast);
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

        ServerLevel level = (ServerLevel) player.level();
        if (progress.mastery().registered()) {
            player.sendSystemMessage(Component.literal("§6[융합 각인] §f" + spell.name()
                    + "의 완성 회로가 마력핵에 새겨졌습니다. 이제 1~5 슬롯에 장착할 수 있습니다."));
            horizontalSigil(level, player.position().add(0.0, 0.12, 0.0), spell, 2.4, 2);
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(),
                    48, 0.9, 0.9, 0.9, 0.045);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        if (progress.circle().advanced()) {
            player.sendSystemMessage(Component.literal("§d[써클 승급] §f마력핵이 §5" + progress.circle().current()
                    + "써클§f로 확장되었습니다. 해당 써클 주문서를 해독할 수 있습니다."));
            horizontalSigil(level, player.position().add(0.0, 0.1, 0.0), spell,
                    2.4 + progress.circle().current() * 0.3, 3);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    private static boolean canExecute(ServerPlayer player, String id, double range) {
        return switch (id) {
            case "mend" -> player.getHealth() < player.getMaxHealth();
            case "blink", "rift_step", "spatial_gate" -> findBlinkDestination(player, range).isPresent();
            default -> true;
        };
    }

    private static String displayChain(List<String> spellIds, String separator) {
        return spellIds.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .collect(Collectors.joining(separator));
    }

    private static void releasePrelude(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        ServerLevel level = (ServerLevel) player.level();
        SpellDefinition spell = cast.spell();
        double radius = 0.72 + spell.circle() * 0.20 + (cast.fusion() ? 0.35 : 0.0);
        renderAnchoredSigil(level, player, spell, cast.range(), radius, cast.fusion() ? 3 : 2);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, cast.fusion() ? 1.0F : 0.72F,
                1.25F - spell.circle() * 0.08F);
        if (cast.fusion()) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.85F, cast.ingredients().size() == 3 ? 1.55F : 1.35F);
        }
    }

    private static void renderCharge(ServerPlayer player, SpellDefinition spell, long elapsed, double range) {
        ServerLevel level = (ServerLevel) player.level();
        double pulse = 0.08 * Math.sin(elapsed * 0.38);
        double radius = 0.58 + spell.circle() * 0.17 + Math.min(0.30, elapsed / 90.0) + pulse;
        renderAnchoredSigil(level, player, spell, range, radius, 1);
        if (elapsed > 0 && elapsed % 20L == 0L) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.24F, 1.55F - spell.circle() * 0.08F);
        }
    }

    private static void renderAnchoredSigil(ServerLevel level, ServerPlayer player, SpellDefinition spell,
                                            double range, double radius, int density) {
        switch (spell.sigilAnchor()) {
            case FRONT -> {
                Vec3 look = player.getLookAngle().normalize();
                Vec3 center = player.getEyePosition().add(look.scale(1.35 + spell.circle() * 0.05));
                verticalSigil(level, center, look, spell, radius, density);
            }
            case FEET -> horizontalSigil(level, player.position().add(0.0, 0.08, 0.0), spell, radius, density);
            case BODY -> {
                horizontalSigil(level, player.position().add(0.0, 0.12, 0.0), spell, radius, density);
                ring(level, player.position().add(0.0, 1.05, 0.0), radius * 0.72, schoolParticle(spell),
                        Math.max(12, 12 * density));
            }
            case GROUND_SELF -> horizontalSigil(level, player.position().add(0.0, 0.08, 0.0), spell,
                    radius * 1.25, density);
            case GROUND_TARGET -> horizontalSigil(level, aimGround(player, Math.max(4.0, range)), spell,
                    radius * 1.18, density);
            case TARGET -> {
                Vec3 center = lookTarget(player, Math.max(6.0, range)).map(Mob::position)
                        .orElse(player.getEyePosition().add(player.getLookAngle().normalize().scale(2.0)));
                horizontalSigil(level, center.add(0.0, 0.08, 0.0), spell, radius, density);
            }
        }
    }

    private static void verticalSigil(ServerLevel level, Vec3 center, Vec3 normal, SpellDefinition spell,
                                      double radius, int density) {
        Vec3 upReference = Math.abs(normal.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = normal.cross(upReference).normalize();
        Vec3 up = right.cross(normal).normalize();
        ParticleOptions particle = schoolParticle(spell);
        int points = Math.max(20, (18 + spell.circle() * 4) * density);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Vec3 point = center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            if ((i & 1) == 0) {
                Vec3 inner = center.add(right.scale(Math.cos(-angle * 2.0) * radius * 0.58))
                        .add(up.scale(Math.sin(-angle * 2.0) * radius * 0.58));
                level.sendParticles(ParticleTypes.END_ROD, inner.x, inner.y, inner.z, 1, 0, 0, 0, 0);
            }
        }
        int spokes = 4 + spell.circle();
        for (int spoke = 0; spoke < spokes; spoke++) {
            double angle = Math.PI * 2.0 * spoke / spokes + (spell.id().hashCode() & 15) * 0.03;
            Vec3 edge = center.add(right.scale(Math.cos(angle) * radius * 0.88))
                    .add(up.scale(Math.sin(angle) * radius * 0.88));
            particleLine(level, center, edge, ParticleTypes.ENCHANT, 5 + density * 2);
        }
    }

    private static void horizontalSigil(ServerLevel level, Vec3 center, SpellDefinition spell,
                                        double radius, int density) {
        ParticleOptions particle = schoolParticle(spell);
        int points = Math.max(18, (16 + spell.circle() * 4) * density);
        ring(level, center, radius, particle, points);
        ring(level, center, radius * 0.58, ParticleTypes.END_ROD, Math.max(12, points / 2));
        int sides = 4 + Math.min(4, spell.circle());
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            vertices.add(center.add(Math.cos(angle) * radius * 0.80, 0.02, Math.sin(angle) * radius * 0.80));
        }
        for (int i = 0; i < vertices.size(); i++) {
            particleLine(level, vertices.get(i), vertices.get((i + 1) % vertices.size()),
                    ParticleTypes.ENCHANT, 5 + density * 2);
        }
    }

    private static ParticleOptions schoolParticle(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case WIND -> ParticleTypes.CLOUD;
            case WARD -> ParticleTypes.END_ROD;
            case LIFE -> ParticleTypes.HAPPY_VILLAGER;
            case SPACE -> ParticleTypes.PORTAL;
            default -> ParticleTypes.ENCHANT;
        };
    }

    private static boolean execute(ServerPlayer player, String id, double range, double power) {
        return switch (id) {
            case "arcane_dart" -> bolt(player, range, power, ParticleTypes.ENCHANT, 0, 0);
            case "ember" -> bolt(player, range, power, ParticleTypes.FLAME, 100, 0);
            case "frost_needle" -> bolt(player, range, power, ParticleTypes.SNOWFLAKE, 0, 90);
            case "gale_step" -> dash(player, range, power);
            case "lesser_ward" -> ward(player, 1, power);
            case "mend" -> mend(player, power);
            case "blink" -> blink(player, range, power, 0);
            case "stone_skin" -> stoneSkin(player, power);
            case "lightning_arc" -> lightningArc(player, range, power);
            case "mana_lance" -> piercingLance(player, range, power, ParticleTypes.ENCHANT);
            case "flame_lance" -> bolt(player, range, power, ParticleTypes.FLAME, 180, 0);
            case "ice_shackles" -> shackles(player, range, power);
            case "wind_blade" -> windBlade(player, range, power);
            case "greater_ward" -> ward(player, 2, power);
            case "flame_wave" -> flameWave(player, range, power);
            case "ice_lance" -> iceLance(player, range, power);
            case "arcane_sight" -> arcaneSight(player, range);
            case "levitation" -> levitation(player, power);
            case "fireball" -> areaAtAim(player, range, power, ParticleTypes.FLAME, true, false);
            case "frost_nova" -> areaAt(player, player.position(), range, power, ParticleTypes.SNOWFLAKE, false, true);
            case "chain_bolt" -> chainBolt(player, range, power);
            case "rift_step" -> blink(player, range, power, 1);
            case "triune_barrage" -> triuneBarrage(player, range, power);
            case "tempest_aegis" -> tempestAegis(player, range, power);
            case "phoenix_field" -> phoenixField(player, range, power);
            case "meteor_shard" -> meteorShard(player, range, power);
            case "blizzard_field" -> blizzardField(player, range, power);
            case "thunder_prison" -> thunderPrison(player, range, power);
            case "mass_mend" -> massMend(player, range, power);
            case "spatial_gate" -> blink(player, range, power, 2);
            case "inferno_domain" -> infernoDomain(player, range, power);
            case "absolute_zero" -> absoluteZero(player, range, power);
            case "tempest_domain" -> tempestDomain(player, range, power);
            case "aegis_citadel" -> aegisCitadel(player, range, power);
            case "arcane_annihilation" -> arcaneAnnihilation(player, range, power);
            default -> false;
        };
    }

    private static boolean bolt(ServerPlayer player, double range, double power, ParticleOptions particle,
                                int fireTicks, int freezeBonus) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = frontOrigin(player, 1.35);
        Optional<Mob> target = lookTarget(player, range);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        spiralBeam(level, start, end, particle,
                particle == ParticleTypes.ENCHANT ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT, 34);
        if (target.isPresent()) {
            Mob mob = target.get();
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            if (fireTicks > 0) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), fireTicks));
            if (freezeBonus > 0) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(),
                    mob.getTicksRequiredToFreeze() + freezeBonus + (int) Math.round(power * 8.0)));
            burst(level, mob.getEyePosition(), particle, 20, 0.42);
        } else burst(level, end, particle, 8, 0.18);
        return true;
    }

    private static boolean shackles(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        if (target.isEmpty()) return bolt(player, range, power * 0.55, ParticleTypes.SNOWFLAKE, 0, 70);
        Mob mob = target.get();
        spiralBeam(level, frontOrigin(player, 1.35), mob.getEyePosition(), ParticleTypes.SNOWFLAKE,
                ParticleTypes.END_ROD, 32);
        mob.hurtServer(level, level.damageSources().magic(), (float) power);
        mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze()
                + 180 + (int) Math.round(power * 10.0)));
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3));
        for (double y = 0.15; y < mob.getBbHeight() + 0.3; y += 0.42) {
            ring(level, mob.position().add(0.0, y, 0.0), Math.max(0.65, mob.getBbWidth() * 0.8),
                    ParticleTypes.SNOWFLAKE, 20);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.35F);
        return true;
    }

    private static boolean dash(ServerPlayer player, double range, double power) {
        Vec3 look = player.getLookAngle().normalize();
        double strength = Math.max(1.2, range / 3.4) * Math.max(0.9, Math.sqrt(Math.max(0.1, power)));
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

    private static boolean ward(ServerPlayer player, int tier, double power) {
        int duration = 100 + tier * 90 + (int) Math.round(power * 7.0);
        int amplifier = Math.max(tier - 1, (int) Math.floor(power / 10.0));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier));
        ServerLevel level = (ServerLevel) player.level();
        double radius = 1.25 + tier * 0.55;
        dome(level, player.position().add(0.0, 0.2, 0.0), radius, ParticleTypes.END_ROD);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, tier > 1 ? 0.72F : 0.95F);
        return true;
    }

    private static boolean mend(ServerPlayer player, double power) {
        if (player.getHealth() >= player.getMaxHealth()) return false;
        player.heal((float) power);
        healingVisual((ServerLevel) player.level(), player.position(), 1.0);
        return true;
    }

    private static boolean stoneSkin(ServerPlayer player, double power) {
        int duration = 180 + (int) Math.round(power * 16.0);
        int amplifier = power >= 12.0 ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, amplifier));
        ServerLevel level = (ServerLevel) player.level();
        for (int layer = 0; layer < 4; layer++) {
            ring(level, player.position().add(0.0, 0.2 + layer * 0.45, 0.0), 0.62,
                    ParticleTypes.CRIT, 16);
        }
        return true;
    }

    private static boolean lightningArc(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        List<Mob> targets = chainedTargets(player, range, 3);
        Vec3 from = frontOrigin(player, 1.35);
        if (targets.isEmpty()) {
            particleLine(level, from, from.add(player.getLookAngle().normalize().scale(range)),
                    ParticleTypes.ELECTRIC_SPARK, 36);
            return true;
        }
        double scale = 1.0;
        for (Mob mob : targets) {
            particleLine(level, from, mob.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 28);
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * scale));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 1));
            from = mob.getEyePosition();
            scale *= 0.78;
        }
        return true;
    }

    private static boolean piercingLance(ServerPlayer player, double range, double power, ParticleOptions particle) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = frontOrigin(player, 1.35);
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        spiralBeam(level, start, end, particle, ParticleTypes.END_ROD, 52);
        List<Mob> targets = lineTargets(player, range, 1.25);
        for (int index = 0; index < targets.size(); index++) {
            targets.get(index).hurtServer(level, level.damageSources().magic(),
                    (float) (power * Math.max(0.55, 1.0 - index * 0.12)));
        }
        burst(level, end, particle, 18, 0.35);
        return true;
    }

    private static boolean flameWave(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.position();
        Vec3 look = horizontalLook(player);
        List<Mob> targets = nearbyTargets(player, origin, range, 4.0).stream()
                .filter(mob -> horizontalDirection(origin, mob.position()).dot(look) > 0.45)
                .toList();
        for (Mob mob : targets) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
        }
        for (int step = 1; step <= 12; step++) {
            double distance = range * step / 12.0;
            Vec3 center = origin.add(look.scale(distance)).add(0.0, 0.25, 0.0);
            level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                    8 + step / 2, distance * 0.08, 0.15, distance * 0.08, 0.03);
        }
        return true;
    }

    private static boolean iceLance(ServerPlayer player, double range, double power) {
        boolean cast = bolt(player, range, power * 1.25, ParticleTypes.SNOWFLAKE, 0, 260);
        lookTarget(player, range).ifPresent(mob -> mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 4)));
        return cast;
    }

    private static boolean arcaneSight(ServerPlayer player, double range) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, player.position(), range, 5.0)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
            ring(level, mob.position().add(0.0, mob.getBbHeight() + 0.25, 0.0), 0.42,
                    ParticleTypes.ENCHANT, 16);
        }
        return true;
    }

    private static boolean levitation(ServerPlayer player, double power) {
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 34 + (int) Math.round(power * 3.0), 1));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0));
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                28, 0.45, 0.12, 0.45, 0.04);
        return true;
    }

    private static boolean blink(ServerPlayer player, double range, double power, int tier) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<BlockPos> destinationResult = findBlinkDestination(player, range);
        if (destinationResult.isEmpty()) return false;
        BlockPos origin = player.blockPosition();
        BlockPos destination = destinationResult.get();
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                30 + tier * 18, 0.5, 0.9, 0.5, 0.12);
        player.teleportTo(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                32 + tier * 20, 0.6, 1.0, 0.6, 0.08);
        if (tier > 0) {
            int duration = 35 + tier * 35 + (int) Math.round(power * 18.0);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, tier >= 2 ? 2 : 0));
        }
        level.playSound(null, origin, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.9F, 1.1F - tier * 0.18F);
        return true;
    }

    private static Optional<BlockPos> findBlinkDestination(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        for (int step = (int) Math.floor(range); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(player.position().add(look.scale(step)));
            for (int down = 0; down <= 4; down++) {
                BlockPos lowered = candidate.below(down);
                if (safe(level, lowered)) return Optional.of(lowered);
            }
        }
        return Optional.empty();
    }

    private static boolean windBlade(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = frontOrigin(player, 1.35);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        for (int blade = -1; blade <= 1; blade++) {
            Vec3 offset = new Vec3(-look.z, 0.0, look.x).normalize().scale(blade * 0.45);
            particleLine(level, start.add(offset), end.add(offset), ParticleTypes.CLOUD, 34);
        }
        List<Mob> targets = lineTargets(player, range, 1.45);
        for (Mob mob : targets) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            Vec3 away = mob.position().subtract(player.position()).normalize();
            mob.push(away.x * 0.9, 0.18, away.z * 0.9);
        }
        return true;
    }

    private static boolean areaAtAim(ServerPlayer player, double range, double power, ParticleOptions particle,
                                     boolean fire, boolean freeze) {
        Vec3 center = lookTarget(player, range).map(Mob::position).orElse(aimGround(player, range));
        return areaAt(player, center, Math.max(3.0, range * 0.32), power, particle, fire, freeze);
    }

    private static boolean areaAt(ServerPlayer player, Vec3 center, double radius, double power,
                                  ParticleOptions particle, boolean fire, boolean freeze) {
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, radius, 4.0)) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            if (fire) mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 180));
            if (freeze) mob.setTicksFrozen(Math.max(mob.getTicksFrozen(),
                    mob.getTicksRequiredToFreeze() + 180 + (int) Math.round(power * 8.0)));
        }
        for (int ring = 1; ring <= 4; ring++) {
            ring(level, center.add(0.0, 0.15 + ring * 0.08, 0.0), radius * ring / 4.0,
                    particle, 20 + ring * 8);
        }
        burst(level, center.add(0.0, 0.8, 0.0), particle, 42, Math.max(0.8, radius * 0.35));
        return true;
    }

    private static boolean chainBolt(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        List<Mob> targets = chainedTargets(player, range, 5);
        if (targets.isEmpty()) return lightningArc(player, range, power * 0.6);
        Vec3 from = frontOrigin(player, 1.35);
        double scale = 1.0;
        for (Mob target : targets) {
            spiralBeam(level, from, target.getEyePosition(), ParticleTypes.ELECTRIC_SPARK,
                    ParticleTypes.ENCHANT, 24);
            target.hurtServer(level, level.damageSources().magic(), (float) (power * scale));
            from = target.getEyePosition();
            scale *= 0.82;
        }
        return true;
    }

    private static boolean triuneBarrage(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<Mob> target = lookTarget(player, range);
        Vec3 start = frontOrigin(player, 1.4);
        Vec3 end = target.map(Mob::getEyePosition).orElse(start.add(player.getLookAngle().normalize().scale(range)));
        spiralBeam(level, start, end, ParticleTypes.ENCHANT, ParticleTypes.END_ROD, 44);
        spiralBeam(level, start.add(0.0, 0.14, 0.0), end, ParticleTypes.FLAME, ParticleTypes.ENCHANT, 38);
        spiralBeam(level, start.add(0.0, -0.14, 0.0), end, ParticleTypes.SNOWFLAKE, ParticleTypes.END_ROD, 38);
        if (target.isPresent()) {
            Mob mob = target.get();
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * 1.45));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 140));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 120));
        }
        burst(level, end, ParticleTypes.WITCH, 36, 0.6);
        return true;
    }

    private static boolean tempestAegis(ServerPlayer player, double range, double power) {
        ward(player, 2, power * 0.9);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, player.position(), range, 4.0)) {
            Vec3 away = mob.position().subtract(player.position()).normalize();
            mob.push(away.x * 1.6, 0.35, away.z * 1.6);
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * 0.65));
        }
        dome(level, player.position().add(0.0, 0.2, 0.0), Math.max(2.5, range * 0.45), ParticleTypes.CLOUD);
        return true;
    }

    private static boolean phoenixField(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = player.position();
        AABB box = new AABB(center, center).inflate(range, 4.0, range);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && entity != player)) {
            if (isAlly(player, entity)) {
                entity.heal((float) (power * 0.65));
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            } else if (entity instanceof Mob mob && validTarget(player, mob)) {
                mob.hurtServer(level, level.damageSources().magic(), (float) power);
                mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 220));
            }
        }
        player.heal((float) (power * 0.8));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1));
        for (int wing = -1; wing <= 1; wing += 2) {
            for (int step = 0; step < 20; step++) {
                double t = step / 19.0;
                double angle = t * Math.PI;
                Vec3 p = center.add(wing * Math.sin(angle) * range * 0.65,
                        0.4 + Math.sin(angle) * 2.0, Math.cos(angle) * range * 0.38);
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.01);
            }
        }
        return true;
    }

    private static boolean meteorShard(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = aimGround(player, range);
        Vec3 sky = center.add(0.0, 11.0, 0.0);
        spiralBeam(level, sky, center.add(0.0, 0.6, 0.0), ParticleTypes.FLAME, ParticleTypes.LARGE_SMOKE, 50);
        areaAt(player, center, 5.0, power * 1.15, ParticleTypes.FLAME, true, false);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.25F, 0.75F);
        return true;
    }

    private static boolean blizzardField(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = aimGround(player, range);
        double radius = 6.0;
        for (Mob mob : nearbyTargets(player, center, radius, 5.0)) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 320));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 260, 4));
        }
        for (int i = 0; i < 90; i++) {
            double angle = i * 2.399963229728653;
            double r = radius * Math.sqrt((i + 1) / 90.0);
            Vec3 p = center.add(Math.cos(angle) * r, 0.2 + (i % 9) * 0.45, Math.sin(angle) * r);
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 1, 0.08, 0.15, 0.08, 0.02);
        }
        return true;
    }

    private static boolean thunderPrison(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = aimGround(player, range);
        double radius = 4.5;
        for (int pillar = 0; pillar < 8; pillar++) {
            double angle = Math.PI * 2.0 * pillar / 8.0;
            Vec3 base = center.add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
            particleLine(level, base, base.add(0.0, 4.5, 0.0), ParticleTypes.ELECTRIC_SPARK, 20);
        }
        for (Mob mob : nearbyTargets(player, center, radius, 4.0)) {
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * 1.1));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 5));
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 180, 0));
        }
        return true;
    }

    private static boolean massMend(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        AABB box = new AABB(player.position(), player.position()).inflate(range, 4.0, range);
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity == player || isAlly(player, entity)));
        for (LivingEntity ally : allies) {
            ally.heal((float) power);
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            healingVisual(level, ally.position(), 0.8);
        }
        return true;
    }

    private static boolean infernoDomain(ServerPlayer player, double range, double power) {
        Vec3 center = aimGround(player, range);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, 8.0, 6.0)) {
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * 1.25));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 420));
        }
        for (int ringIndex = 1; ringIndex <= 7; ringIndex++) {
            ring(level, center.add(0.0, 0.12 + ringIndex * 0.04, 0.0), ringIndex * 1.15,
                    ringIndex % 2 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, 24 + ringIndex * 8);
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 1.0, center.z,
                120, 4.5, 1.3, 4.5, 0.04);
        return true;
    }

    private static boolean absoluteZero(ServerPlayer player, double range, double power) {
        Vec3 center = aimGround(player, range);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, 8.0, 6.0)) {
            mob.hurtServer(level, level.damageSources().magic(), (float) (power * 1.2));
            mob.setTicksFrozen(Math.max(mob.getTicksFrozen(), mob.getTicksRequiredToFreeze() + 600));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 360, 6));
        }
        for (int layer = 0; layer < 6; layer++) {
            ring(level, center.add(0.0, layer * 0.42, 0.0), 8.0 - layer * 0.8,
                    ParticleTypes.SNOWFLAKE, 64 - layer * 5);
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z,
                72, 4.0, 1.8, 4.0, 0.01);
        return true;
    }

    private static boolean tempestDomain(ServerPlayer player, double range, double power) {
        Vec3 center = aimGround(player, range);
        ServerLevel level = (ServerLevel) player.level();
        for (Mob mob : nearbyTargets(player, center, 9.0, 7.0)) {
            mob.hurtServer(level, level.damageSources().magic(), (float) power);
            Vec3 away = mob.position().subtract(center).normalize();
            mob.push(away.x * 2.1, 0.9, away.z * 2.1);
            mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 45, 1));
        }
        for (int spiral = 0; spiral < 100; spiral++) {
            double angle = spiral * 0.55;
            double r = 1.2 + spiral * 0.075;
            Vec3 p = center.add(Math.cos(angle) * r, 0.2 + (spiral % 12) * 0.32, Math.sin(angle) * r);
            level.sendParticles(spiral % 4 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.CLOUD,
                    p.x, p.y, p.z, 1, 0.04, 0.08, 0.04, 0.01);
        }
        return true;
    }

    private static boolean aegisCitadel(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        AABB box = new AABB(player.position(), player.position()).inflate(range, 5.0, range);
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (entity == player || isAlly(player, entity)));
        int absorption = Math.max(3, (int) Math.floor(power / 7.0));
        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 520, absorption));
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 420, 2));
        }
        for (int layer = 0; layer < 5; layer++) {
            dome(level, player.position().add(0.0, 0.2, 0.0), 4.0 + layer * 1.0, ParticleTypes.END_ROD);
        }
        return true;
    }

    private static boolean arcaneAnnihilation(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = frontOrigin(player, 1.5);
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        Vec3 right = new Vec3(-player.getLookAngle().z, 0.0, player.getLookAngle().x).normalize();
        for (int beam = -2; beam <= 2; beam++) {
            Vec3 offset = right.scale(beam * 0.13);
            spiralBeam(level, start.add(offset), end.add(offset),
                    beam % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT,
                    ParticleTypes.WITCH, 72);
        }
        List<Mob> targets = lineTargets(player, range, 2.2);
        for (int index = 0; index < targets.size(); index++) {
            targets.get(index).hurtServer(level, level.damageSources().magic(),
                    (float) (power * Math.max(0.65, 1.15 - index * 0.08)));
        }
        burst(level, end, ParticleTypes.WITCH, 80, 1.4);
        return true;
    }

    private static void healingVisual(ServerLevel level, Vec3 center, double scale) {
        for (int layer = 0; layer < 4; layer++) {
            ring(level, center.add(0.0, 0.2 + layer * 0.42, 0.0), (0.55 + layer * 0.12) * scale,
                    ParticleTypes.HAPPY_VILLAGER, 20);
        }
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z,
                16, 0.4 * scale, 0.7, 0.4 * scale, 0.03);
    }

    private static Optional<Mob> lookTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        AABB box = new AABB(start, end).inflate(2.2);
        return ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                        mob -> validTarget(player, mob) && player.hasLineOfSight(mob))
                .stream()
                .filter(mob -> projection(start, look, mob.getEyePosition()) >= 0.0)
                .filter(mob -> projection(start, look, mob.getEyePosition()) <= range + 1.0)
                .min(Comparator.comparingDouble(mob -> rayDistanceSquared(start, look, mob.getEyePosition()))
                        .thenComparingDouble(player::distanceToSqr));
    }

    private static List<Mob> lineTargets(ServerPlayer player, double range, double width) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = start.add(look.scale(range));
        AABB box = new AABB(start, end).inflate(width + 0.8);
        return ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                        mob -> validTarget(player, mob) && player.hasLineOfSight(mob))
                .stream()
                .filter(mob -> projection(start, look, mob.getEyePosition()) >= 0.0)
                .filter(mob -> projection(start, look, mob.getEyePosition()) <= range + 1.0)
                .filter(mob -> rayDistanceSquared(start, look, mob.getEyePosition()) <= width * width)
                .sorted(Comparator.comparingDouble(mob -> projection(start, look, mob.getEyePosition())))
                .toList();
    }

    private static List<Mob> nearbyTargets(ServerPlayer player, Vec3 center, double radius, double vertical) {
        AABB box = new AABB(center, center).inflate(radius, vertical, radius);
        return ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                mob -> validTarget(player, mob));
    }

    private static List<Mob> chainedTargets(ServerPlayer player, double range, int limit) {
        List<Mob> result = new ArrayList<>();
        Optional<Mob> first = lookTarget(player, range);
        if (first.isEmpty()) return result;
        result.add(first.get());
        while (result.size() < limit) {
            Mob last = result.get(result.size() - 1);
            AABB box = last.getBoundingBox().inflate(5.0);
            Optional<Mob> next = ((ServerLevel) player.level()).getEntitiesOfClass(Mob.class, box,
                            mob -> validTarget(player, mob) && !result.contains(mob) && last.hasLineOfSight(mob))
                    .stream().min(Comparator.comparingDouble(last::distanceToSqr));
            if (next.isEmpty()) break;
            result.add(next.get());
        }
        return result;
    }

    private static boolean validTarget(ServerPlayer player, Mob mob) {
        if (!mob.isAlive() || mob.isRemoved()) return false;
        if (mob instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return false;
        return player.getTeam() == null || mob.getTeam() == null || !player.isAlliedTo(mob);
    }

    private static boolean isAlly(ServerPlayer player, LivingEntity entity) {
        if (entity == player) return true;
        if (entity instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(player)) return true;
        if (entity instanceof ServerPlayer other) return player.isAlliedTo(other);
        return player.isAlliedTo(entity);
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        BlockState below = level.getBlockState(feet.below());
        if (!below.isFaceSturdy(level, feet.below(), Direction.UP)) return false;
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) return false;
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition();
        for (int step = (int) Math.max(2, Math.floor(range)); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(origin.add(look.scale(step)));
            for (int down = 0; down <= 8; down++) {
                BlockPos floor = candidate.below(down);
                BlockState state = level.getBlockState(floor);
                if (state.isFaceSturdy(level, floor, Direction.UP)) {
                    return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
                }
            }
        }
        return player.position().add(horizontalLook(player).scale(Math.min(5.0, range))).add(0.0, 0.08, 0.0);
    }

    private static Vec3 frontOrigin(ServerPlayer player, double distance) {
        return player.getEyePosition().add(player.getLookAngle().normalize().scale(distance));
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        return flat.lengthSqr() < 0.0001 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        Vec3 flat = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        return flat.lengthSqr() < 0.0001 ? Vec3.ZERO : flat.normalize();
    }

    private static double projection(Vec3 start, Vec3 direction, Vec3 point) {
        return point.subtract(start).dot(direction);
    }

    private static double rayDistanceSquared(Vec3 start, Vec3 direction, Vec3 point) {
        double projection = Math.max(0.0, point.subtract(start).dot(direction));
        Vec3 closest = start.add(direction.scale(projection));
        return closest.distanceToSqr(point);
    }

    private static void spiralBeam(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions core,
                                   ParticleOptions accent, int points) {
        Vec3 delta = end.subtract(start);
        double length = Math.max(0.001, delta.length());
        Vec3 direction = delta.scale(1.0 / length);
        Vec3 reference = Math.abs(direction.y) > 0.92 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();
        for (int index = 0; index <= points; index++) {
            double t = index / (double) points;
            Vec3 center = start.add(delta.scale(t));
            double angle = t * Math.PI * 8.0;
            double radius = 0.06 + Math.sin(Math.PI * t) * 0.12;
            Vec3 orbit = center.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            level.sendParticles(core, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            if ((index & 1) == 0) level.sendParticles(accent, orbit.x, orbit.y, orbit.z, 1, 0, 0, 0, 0);
        }
    }

    private static void particleLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        Vec3 delta = end.subtract(start);
        for (int index = 0; index <= points; index++) {
            double t = index / (double) points;
            Vec3 p = start.add(delta.scale(t));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0 * index / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void dome(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        for (int layer = 1; layer <= 5; layer++) {
            double phi = Math.PI * layer / 12.0;
            double ringRadius = Math.cos(phi) * radius;
            double y = center.y + Math.sin(phi) * radius;
            ring(level, new Vec3(center.x, y, center.z), ringRadius, particle, 22 + layer * 3);
        }
    }

    private static void burst(ServerLevel level, Vec3 center, ParticleOptions particle, int count, double spread) {
        level.sendParticles(particle, center.x, center.y, center.z, count, spread, spread, spread, 0.04);
    }

    private static void fail(ServerPlayer player, String message) {
        player.sendOverlayMessage(Component.literal("§c[마법 실패] §f" + message));
        ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS, 0.35F, 0.7F);
    }
}
