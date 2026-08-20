package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Spell-vs-spell defensive runtime. Globe of Invulnerability is not generic damage reduction:
 * hostile 1-5 circle Arcane effects arriving from outside are erased at the globe boundary while
 * physical attacks and 6+ circle spells pass through normally.
 */
public final class HighWardSpellService {
    public static final int GLOBE_TICKS = 520;
    public static final int MAX_BLOCKED_CIRCLE = 5;
    private static final double BASE_RADIUS = 6.0;

    private static final Map<UUID, GlobeState> GLOBES = new HashMap<>();
    private static final Map<UUID, Long> NOTICE_READY = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private HighWardSpellService() {}

    public static boolean handles(String spellId) {
        return "globe_of_invulnerability".equals(spellId);
    }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (!handles(spellId) || caster == null || !caster.isAlive()) return false;
        ServerLevel level = (ServerLevel) caster.level();
        double radius = Math.max(BASE_RADIUS, Math.min(8.0, BASE_RADIUS + Math.max(0.0, power - 52.0) / 90.0));
        GLOBES.put(caster.getUUID(), new GlobeState(level, caster.getUUID(), radius,
                level.getGameTime() + GLOBE_TICKS));
        ArcaneNoticeService.push(caster, Component.literal("§b[무적의 구체] §f" + one(GLOBE_TICKS / 20.0)
                + "초 · 반경 " + one(radius) + " · 외부에서 들어오는 적대 1~5써클 주문을 경계면에서 소거합니다. "
                + "§76써클 이상 주문과 물리 공격은 그대로 통과합니다."), 110);
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS, 1.0F, 1.25F);
        return true;
    }

    /** Returns true when this authored Arcane cast is erased by an active hostile globe. */
    public static boolean intercepts(LivingEntity caster, SpellDefinition spell,
                                     CastTargetSnapshot snapshot, double range) {
        if (caster == null || spell == null || snapshot == null || !snapshot.validFor(caster)) return false;
        if (spell.circle() > MAX_BLOCKED_CIRCLE || spell.circle() <= 0) return false;

        ServerLevel level = (ServerLevel) caster.level();
        long now = level.getGameTime();
        SpellPresentationProfile.Motion motion = SpellPresentationProfile.profile(spell).motion();
        for (GlobeState state : GLOBES.values()) {
            if (state.level != level || now >= state.expiresAt) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawOwner instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator()) continue;
            if (owner == caster || owner.isAlliedTo(caster)) continue;

            Vec3 center = owner.position().add(0.0, owner.getBbHeight() * .50, 0.0);
            // A caster already inside the shell can cast outward. The globe only erases magic crossing inward.
            if (caster.position().distanceToSqr(center) <= state.radius * state.radius) continue;
            if (!crossesBoundary(spell, motion, snapshot, range, center, state.radius)) continue;

            WorldMagicService.cancelRelease(caster, spell.id());
            level.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, .95F, 1.75F);
            long ready = NOTICE_READY.getOrDefault(owner.getUUID(), 0L);
            if (now >= ready) {
                NOTICE_READY.put(owner.getUUID(), now + 12L);
                ArcaneNoticeService.push(owner, Component.literal("§b[무적의 구체] §f"
                        + spell.name() + " §7(" + spell.circle() + "써클)§f을 경계면에서 소거했습니다."), 45);
            }
            if (caster instanceof ServerPlayer player) {
                ArcaneNoticeService.push(player, Component.literal("§7[주문 소거] §f" + spell.name()
                        + "이 상대의 무적의 구체에 막혔습니다. §76써클 이상 주문은 관통합니다."), 45);
            }
            return true;
        }
        return false;
    }

    private static boolean crossesBoundary(SpellDefinition spell, SpellPresentationProfile.Motion motion,
                                           CastTargetSnapshot snapshot, double range,
                                           Vec3 globeCenter, double globeRadius) {
        Vec3 target = snapshot.target();
        double footprint = switch (motion) {
            case SKY_DROP, STORM, FIELD -> Math.min(24.0,
                    Math.max(1.5, SpellMetrics.effectRadius(spell.id(), range, spell.circle())));
            case WALL -> Math.min(20.0,
                    Math.max(2.0, SpellMetrics.wallWidth(spell.id(), range, spell.circle()) * .5));
            case WAVE -> Math.min(14.0,
                    Math.max(1.0, SpellMetrics.waveEndRadius(spell.id(), range, spell.circle())));
            case TARGET_BURST, PRISON -> 1.5;
            default -> .85;
        };

        return switch (motion) {
            case SKY_DROP, STORM, FIELD, WALL, TARGET_BURST, PRISON ->
                    target.distanceToSqr(globeCenter) <= square(globeRadius + footprint);
            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, BEAM, LANCE, WAVE ->
                    segmentDistanceSqr(snapshot.launchOrigin(), target, globeCenter)
                            <= square(globeRadius + footprint);
            default -> target.distanceToSqr(globeCenter) <= square(globeRadius + footprint);
        };
    }

    private static double segmentDistanceSqr(Vec3 start, Vec3 end, Vec3 point) {
        Vec3 delta = end.subtract(start);
        double lengthSqr = delta.lengthSqr();
        if (lengthSqr < 1.0E-8) return point.distanceToSqr(start);
        double t = point.subtract(start).dot(delta) / lengthSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        return point.distanceToSqr(start.add(delta.scale(t)));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        Iterator<Map.Entry<UUID, GlobeState>> iterator = GLOBES.entrySet().iterator();
        while (iterator.hasNext()) {
            GlobeState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            if (now >= state.expiresAt || !(rawOwner instanceof ServerPlayer owner)
                    || !owner.isAlive() || owner.isSpectator()) {
                NOTICE_READY.remove(state.ownerId);
                iterator.remove();
            }
        }
    }

    public static void clear(LivingEntity subject) {
        if (subject == null) return;
        UUID id = subject.getUUID();
        GlobeState removed = GLOBES.remove(id);
        NOTICE_READY.remove(id);
        if (removed != null) WorldMagicService.cancelRelease(subject, "globe_of_invulnerability");
    }

    public static void clearAll() {
        GLOBES.clear();
        NOTICE_READY.clear();
        LAST_TICK.clear();
    }

    private static double square(double value) { return value * value; }
    private static String one(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static final class GlobeState {
        private final ServerLevel level;
        private final UUID ownerId;
        private final double radius;
        private final long expiresAt;

        private GlobeState(ServerLevel level, UUID ownerId, double radius, long expiresAt) {
            this.level = level;
            this.ownerId = ownerId;
            this.radius = radius;
            this.expiresAt = expiresAt;
        }
    }
}
