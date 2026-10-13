package dev.moonseungjun.openworldrpg.entity;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01MeadowViperPoisonRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerIncomingDamageRuntime;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerPoisePressureRuntime;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Project-owned Meadow Viper runtime.
 *
 * <p>The server owns warning/engage/commit/contact/recovery/spacing and only publishes compact
 * presentation state to clients. Natural R01 spawn admission remains separately gated.</p>
 */
public final class MeadowViperEntity extends PathfinderMob {
    public static final double WARNING_RADIUS = 3.5;
    public static final double BITE_RANGE = 2.2;
    public static final int AMBIENT_WARNING_TICKS = 9;
    public static final int BITE_WINDUP_TICKS = 9;
    public static final int BITE_ACTIVE_TICKS = 3;
    public static final int BITE_RECOVERY_TICKS = 11;
    public static final double SPACING_DISTANCE = 2.0;
    public static final double PLAYER_POISE_PRESSURE = 16.0;

    /** Source-derived review scale: 0.35 * ~2.265 source-unit peak mouth reach. */
    public static final double REVIEW_ATTACK_PEAK_FORWARD_REACH = 0.793;

    /**
     * Runtime navigation seed only. Exact world movement speed is deliberately not accepted as
     * production tuning until the pinned model is reviewed in-client, per R01 canon.
     */
    private static final double REVIEW_MOVEMENT_SPEED = 0.18;

    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(MeadowViperEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> PHASE_START_TICK =
            SynchedEntityData.defineId(MeadowViperEntity.class, EntityDataSerializers.LONG);

    private UUID targetId;
    private boolean provoked;
    private boolean activeImpactResolved;

    public MeadowViperEntity(EntityType<? extends MeadowViperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 75.0)
                .add(Attributes.MOVEMENT_SPEED, REVIEW_MOVEMENT_SPEED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, Phase.NEUTRAL.id());
        builder.define(PHASE_START_TICK, 0L);
    }

    public Phase phase() {
        return Phase.byId(entityData.get(PHASE));
    }

    public long phaseStartTick() {
        return entityData.get(PHASE_START_TICK);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean applied = super.hurtServer(level, source, damage);
        if (applied && source.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            targetId = player.getUUID();
            provoked = true;
            beginImmediateEngage(level, player);
        }
        return applied;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || !isAlive()) {
            return;
        }
        tickServer(serverLevel);
    }

    private void tickServer(ServerLevel level) {
        long nowTick = level.getGameTime();
        ServerPlayer target = resolveTarget(level).orElse(null);

        if (target == null) {
            ServerPlayer ambient = nearestAmbientPlayer(level).orElse(null);
            if (ambient == null) {
                resetNeutral(nowTick);
                return;
            }
            target = ambient;
            targetId = target.getUUID();
            provoked = false;
            transition(Phase.WARNING, nowTick);
        }

        if (!validTarget(target)) {
            resetNeutral(nowTick);
            return;
        }

        face(target);
        double distance = horizontalDistanceTo(target);
        long phaseAge = Math.max(0L, nowTick - phaseStartTick());

        switch (phase()) {
            case NEUTRAL -> {
                if (provoked) {
                    beginImmediateEngage(level, target);
                } else if (distance <= WARNING_RADIUS) {
                    transition(Phase.WARNING, nowTick);
                }
            }
            case WARNING -> tickWarning(level, target, distance, phaseAge, nowTick);
            case BITE_WINDUP -> tickBiteWindup(target, distance, phaseAge, nowTick);
            case BITE_ACTIVE -> tickBiteActive(target, phaseAge, nowTick);
            case RECOVERY -> {
                getNavigation().stop();
                if (phaseAge >= BITE_RECOVERY_TICKS) {
                    transition(Phase.SPACING, nowTick);
                }
            }
            case SPACING -> tickSpacing(target, distance, nowTick);
        }
    }

    private void tickWarning(
            ServerLevel level,
            ServerPlayer target,
            double distance,
            long phaseAge,
            long nowTick
    ) {
        if (!provoked && distance > WARNING_RADIUS) {
            resetNeutral(nowTick);
            return;
        }
        if (distance > BITE_RANGE) {
            moveOnlyEnoughToCreateBiteReach(target);
            return;
        }
        getNavigation().stop();
        if (phaseAge >= AMBIENT_WARNING_TICKS) {
            activeImpactResolved = false;
            transition(Phase.BITE_ACTIVE, nowTick);
        }
    }

    private void tickBiteWindup(
            ServerPlayer target,
            double distance,
            long phaseAge,
            long nowTick
    ) {
        getNavigation().stop();
        if (!provoked && distance > WARNING_RADIUS) {
            resetNeutral(nowTick);
            return;
        }
        if (phaseAge >= BITE_WINDUP_TICKS) {
            activeImpactResolved = false;
            transition(Phase.BITE_ACTIVE, nowTick);
        }
    }

    private void tickBiteActive(
            ServerPlayer target,
            long phaseAge,
            long nowTick
    ) {
        getNavigation().stop();
        applyCollisionAwareLunge(target, phaseAge);

        // Resolve around the source clip's lunge/contact peak, not at active-frame start.
        if (!activeImpactResolved && phaseAge >= 2L) {
            activeImpactResolved = true;
            double contactDistance = horizontalDistanceTo(target);
            double visibleContactDistance = REVIEW_ATTACK_PEAK_FORWARD_REACH
                    + target.getBbWidth() * 0.5;
            if (contactDistance <= BITE_RANGE
                    && contactDistance <= visibleContactDistance + 1.0e-3
                    && getSensing().hasLineOfSight(target)) {
                applyCoilBite(target);
            }
        }
        if (phaseAge >= BITE_ACTIVE_TICKS) {
            transition(Phase.RECOVERY, nowTick);
        }
    }

    private void tickSpacing(ServerPlayer target, double distance, long nowTick) {
        if (distance >= SPACING_DISTANCE) {
            provoked = false;
            if (distance <= WARNING_RADIUS) {
                transition(Phase.WARNING, nowTick);
            } else {
                resetNeutral(nowTick);
            }
            return;
        }

        Vec3 away = position().subtract(target.position());
        Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
        if (horizontal.lengthSqr() < 1.0e-8) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        }
        horizontal = horizontal.normalize();
        Vec3 destination = position().add(horizontal.scale(SPACING_DISTANCE));
        getNavigation().moveTo(destination.x, getY(), destination.z, 1.0);
    }

    private void beginImmediateEngage(ServerLevel level, ServerPlayer target) {
        long nowTick = level.getGameTime();
        face(target);
        if (horizontalDistanceTo(target) <= BITE_RANGE) {
            transition(Phase.BITE_WINDUP, nowTick);
        } else {
            transition(Phase.WARNING, nowTick);
        }
    }

    private void moveOnlyEnoughToCreateBiteReach(ServerPlayer target) {
        double distance = horizontalDistanceTo(target);
        if (distance <= BITE_RANGE) {
            getNavigation().stop();
            return;
        }
        Vec3 toward = target.position().subtract(position());
        Vec3 horizontal = new Vec3(toward.x, 0.0, toward.z);
        if (horizontal.lengthSqr() < 1.0e-8) {
            getNavigation().stop();
            return;
        }
        Vec3 desired = target.position().subtract(horizontal.normalize().scale(BITE_RANGE));
        getNavigation().moveTo(desired.x, target.getY(), desired.z, 1.0);
    }

    private void applyCollisionAwareLunge(ServerPlayer target, long activeAge) {
        if (activeAge >= BITE_ACTIVE_TICKS) {
            return;
        }
        Vec3 toward = target.position().subtract(position());
        Vec3 horizontal = new Vec3(toward.x, 0.0, toward.z);
        double distance = horizontal.length();
        if (distance < 1.0e-6) {
            return;
        }
        double visibleContactDistance = REVIEW_ATTACK_PEAK_FORWARD_REACH
È\™Ù]™Ù]˜•ÚY

H
ˆNÂˆÝX›H™YYYHX]›X^
Œ\Ý[˜ÙHHš\ÚX›PÛÛXÝ\Ý[˜ÙJNÂˆËÈ™XXÚš\ÚX›HÛÛXÝžHXÝ]™HXÚÈ‹Ú\™HHÛÝ\˜ÙH]XÚÈÛ\\È™X\ˆXZÈ^[œÚ[Û‹‚ˆ[™[XZ[š[™ÐÛÛXÝÝ\ÈHX]›X^
KÈH
[
HXÝ]™PYÙJNÂˆÝX›H\ÕXÚÈH™YYYÈ™[XZ[š[™ÐÛÛXÝÝ\ÎÂˆYˆ
\ÕXÚÈˆKŒKM
HÂˆ[Ý™J[Ý™\•\K”ÑS‹Üš^›Û[››Ü›X[^™J
KœØØ[J\ÕXÚÊJNÂˆBˆB‚ˆš]˜]H›ÚY\PÛÚ[š]JÙ\™\”^Y\ˆ\™Ù]
HÂˆÝX›H˜]Ñ[XYÙHH›Ú™XÝÛÛX˜][\Ëœ˜]Ñ[™[^T\ÚXØ[[XYÙQœ›ÛP™[˜ÚX\šÔÚ\™J‹ŒJNÂˆ^Y\‘Y™[œÙP]]Üš]K’[˜ÛÛZ[™Ò]]H^Y\‘Y™[œÙP]]Üš]K’[˜ÛÛZ[™Ò]˜˜\Ù[[™Jˆ˜]Ñ[XYÙKˆ›Ú™XÝ[\XÝ˜[œØXÝ[Û‹‘[XYÙTØÚÛÛ”TÒPÐSˆ‹ˆ^Y\‘Y™[œÙP]]Üš]K‘ÝX\™™\ÜÝ\™P˜[™“QÒˆYKˆYKˆYBˆ
NÂˆ›Ú™XÝ^Y\’[˜ÛÛZ[™Ñ[XYÙT[[YK’[˜ÛÛZ[™Ð\XØ][Ûˆ[˜ÛÛZ[™ÈBˆ›Ú™XÝ^Y\’[˜ÛÛZ[™Ñ[XYÙT[[YK˜\T›Ú™XÝÝÛ™YXÝÜ’]
\Ë\™Ù]]
NÂˆYˆ
Z[˜ÛÛZ[™Ë˜XØÙ\Y

JHÂˆ™]\›ŽÂˆB‚ˆ›Ú™XÝ^Y\”Ú\ÙT™\ÜÝ\™T[[YK˜\P]]Ü™Y™\ÜÝ\™J\™Ù]VQT—ÔÒTÑWÔ‘TÔÕT‘JNÂˆÝX›HÝ]\Ó][\Y\ˆH[˜ÛÛZ[™Ëœ™\ÛÛ][ÛŠ
Bˆ›X\
™\Ý[OˆÚ\ÛÛZ[\][\Y\Š\™Ù]™\Ý[
JBˆ›Ü‘[ÙJŒ
NÂˆŒSYXYÝÕš\\”Ú\ÛÛ”[[YK˜\PZ[\
ˆ\™Ù]ˆŒSYXYÝÕš\\”Ú\ÛÛ”[[YK’UWÐ•RSTˆÝ]\Ó][\Y\‚ˆ
NÂˆB‚ˆš]˜]HÝ]XÈÝX›HÚ\ÛÛZ[\][\Y\ŠˆÙ\™\”^Y\ˆ\™Ù]ˆ]‹›[ÛÛœÙ][™Ú[‹›Ü[ÛÜ›œË˜ÛÛX˜]œÝ]K”^Y\‘Y™[œÙT[[YTÝ]K’[˜ÛÛZ[™ÑY™[œÙT™\Ý[™\Ý[ˆ
HÂˆYˆ
™\Ý[™ÙÙY

H™\Ý[œ\™™XÝÝX\™Y

JHÂˆ™]\›ˆŒÂˆBˆYˆ
\™\Ý[™ÝX\™Y

JHÂˆ™]\›ˆKŒÂˆBˆ˜\ˆY™[œÙHHÛÛX˜]Ý]TÙ\šXÙ\Ë™Y™[œÙTÛ˜\ÚÝÊ
BˆœÛ˜\ÚÝ
\™Ù]™Ù]URQ

JBˆ›Ü‘[ÙJ[
NÂˆYˆ
Y™[œÙHOH[Y™[œÙK™ÝX\™\J
Kš\Ñ[\J
JHÂˆ™]\›ˆKŒÂˆBˆÝX›HXœÛÜœ[ÛˆH^Y\‘Y™[œÙP]]Üš]K™ÝX\™XœÛÜœ[ÛŠˆY™[œÙK™ÝX\™\J
K›Ü‘[ÙU›ÝÊ
Kˆ›Ú™XÝ[\XÝ˜[œØXÝ[Û‹‘[XYÙTØÚÛÛ”TÒPÐSˆ
NÂˆYˆ
™\Ý[™ÝX\™œ›ÚÙ[Š
JHÂˆXœÛÜœ[Ûˆ
HNÂˆBˆ™]\›ˆX]›X^
ŒKŒHXœÛÜœ[ÛŠNÂˆB‚ˆš]˜]HÜ[Û˜[Ù\™\”^Y\ˆ™\ÛÛ™U\™Ù]
Ù\™\“]™[]™[
HÂˆYˆ
\™Ù]YOH[
HÂˆ™]\›ˆÜ[Û˜[™[\J
NÂˆBˆ™]\›ˆÜ[Û˜[›Ù“[X›J]™[™Ù]^Y\žUURQ
\™Ù]Y
JBˆ™š[\ŠÙ\™\”^Y\‹˜Û\ÜÎŽš\Ò[œÝ[˜ÙJBˆ›X\
Ù\™\”^Y\‹˜Û\ÜÎŽ˜Ø\Ý
Bˆ™š[\Š\ÎŽ˜[Y\™Ù]
NÂˆB‚ˆš]˜]HÜ[Û˜[Ù\™\”^Y\ˆ™X\™\Ý[XšY[^Y\ŠÙ\™\“]™[]™[
HÂˆ™]\›ˆ]™[œ^Y\œÊ
KœÝ™X[J
Bˆ™š[\ŠÙ\™\”^Y\‹˜Û\ÜÎŽš\Ò[œÝ[˜ÙJBˆ›X\
Ù\™\”^Y\‹˜Û\ÜÎŽ˜Ø\Ý
Bˆ™š[\Š\ÎŽ˜[Y\™Ù]
Bˆ™š[\Š^Y\ˆOˆÜš^›Û[\Ý[˜ÙUÊ^Y\ŠHHÐT“’S‘×ÔQUTÊBˆ›Z[ŠÛÛ\\˜]Ü‹˜ÛÛ\\š[™ÑÝX›J^Y\ˆOˆ\Ý[˜ÙUÔÜ\Š^Y\ŠJJNÂˆB‚ˆš]˜]H›ÛÛX[ˆ˜[Y\™Ù]
Ù\™\”^Y\ˆ^Y\ŠHÂˆ™]\›ˆ^Y\ˆOH[ˆ	‰ˆ^Y\‹š\Ð[]™J
Bˆ	‰ˆ\^Y\‹š\ÔÜXÝ]ÜŠ
Bˆ	‰ˆ\^Y\‹š\ÐÜ™X]]™J
Bˆ	‰ˆ^Y\‹›]™[

HOH]™[

NÂˆB‚ˆš]˜]HÝX›HÜš^›Û[\Ý[˜ÙUÊÙ\™\”^Y\ˆ^Y\ŠHÂˆÝX›HH^Y\‹™Ù]

HHÙ]

NÂˆÝX›HˆH^Y\‹™Ù]Š
HHÙ]Š
NÂˆ™]\›ˆX]œÜ\

ˆ
Èˆ
ˆŠNÂˆB‚ˆš]˜]H›ÚY˜XÙJÙ\™\”^Y\ˆ\™Ù]
HÂˆÙ]ÛÚÐÛÛ›Û

KœÙ]ÛÚÐ]
\™Ù]ÌŒ‹ÌŒŠNÂˆB‚ˆš]˜]H›ÚY˜[œÚ][ÛŠ\ÙH™^Û™È›ÝÕXÚÊHÂˆYˆ
\ÙJ
HOH™^
HÂˆ™]\›ŽÂˆBˆ[]Q]KœÙ]
TÑK™^šY

JNÂˆ[]Q]KœÙ]
TÑWÔÕT•ÕPÒË›ÝÕXÚÊNÂˆB‚ˆš]˜]H›ÚY™\Ù]™]]˜[
Û™È›ÝÕXÚÊHÂˆ\™Ù]YH[Âˆ›Ý›ÚÙYH˜[ÙNÂˆXÝ]™R[\XÝ™\ÛÛ™YH˜[ÙNÂˆÙ]˜]šYØ][ÛŠ
KœÝÜ

NÂˆ˜[œÚ][ÛŠ\ÙK“‘UUS›ÝÕXÚÊNÂˆB‚ˆX›XÈ[[H\ÙHÂˆ‘UUS

KˆÐT“’S‘ÊJKˆ’UWÕÒS‘T
ŠKˆ’UWÐPÕU‘JÊKˆ‘PÓÕ‘T–J
KˆÔPÒS‘ÊJNÂ‚ˆš]˜]Hš[˜[[YÂ‚ˆ\ÙJ[Y
HÂˆ\ËšYHYÂˆB‚ˆX›XÈ[Y

HÂˆ™]\›ˆYÂˆB‚ˆX›XÈÝ]XÈ\ÙHžRY
[Y
HÂˆ›Üˆ
\ÙH\ÙHˆ˜[Y\Ê
JHÂˆYˆ
\ÙKšYOHY
HÂˆ™]\›ˆ\ÙNÂˆBˆBˆ™]\›ˆ‘UUSÂˆBˆBŸB