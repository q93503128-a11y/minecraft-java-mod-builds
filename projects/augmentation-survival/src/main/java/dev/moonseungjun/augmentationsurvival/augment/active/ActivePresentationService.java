package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.network.ActivePresentationPayload;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared player-facing feedback for the server-authoritative ten-slot active engine. */
public final class ActivePresentationService {
    private static final Set<String> PROFILE_PATHS = Set.of(
            "cleanse", "pull", "swap", "shield", "temporary_platform", "berserk", "healing_wave",
            "shield_throw", "rewind", "time_acceleration", "clone", "phase_shift", "threat_suppression",
            "death_return", "return_home", "domination", "gigantification", "area_acceleration", "revive",
            "time_stop", "divine_punishment"
    );

    private ActivePresentationService() {
    }

    /** Captures pre-cast geometry needed for effects that move instantly or strike a target chain. */
    public static Capture capture(ServerPlayer player, String abilityId) {
        String path = path(abilityId);
        if ("shield_throw".equals(path)) {
            return new Capture(player.position(), captureShieldThrowPath(player));
        }
        return new Capture(player.position(), List.of());
    }

    /** Called only after the server gameplay route has confirmed success. */
    public static void presentSuccessfulUse(ServerPlayer player, String abilityId, Capture capture) {
        if (!hasProfile(abilityId)) return;
        int level = boundedLevel(player, abilityId);
        Profile profile = profile(player, abilityId, level);
        sendState(player, abilityId, profile.phase(), profile.durationTicks(), profile.radius());
        emitActivation(player, abilityId, profile);

        String path = path(abilityId);
        if ("shield_throw".equals(path) && capture != null && !capture.path().isEmpty()) {
            emitPath(player.level(), capture.path(), ParticleTypes.END_ROD, true);
        }
        if (("swap".equals(path) || "rewind".equals(path) || "death_return".equals(path)) && capture != null) {
            Vec3 destination = player.position();
            if (capture.origin().distanceToSqr(destination) > 1.0D) {
                emitLine(player.level(), capture.origin().add(0.0D, 0.9D, 0.0D),
                        destination.add(0.0D, 0.9D, 0.0D), ParticleTypes.PORTAL, 32);
            }
        }
    }

    /** Runtime services use this when a channel cancels, a sustained state starts/extends, or it ends. */
    public static void notifyState(
            ServerPlayer player,
            String abilityId,
            int phase,
            int durationTicks,
            float radius
    ) {
        if (!hasProfile(abilityId)) return;
        sendState(player, abilityId, phase, durationTicks, radius);
    }

    /** Same as notifyState, with a restrained world pulse for a meaningful state transition. */
    public static void notifyStateAndPulse(
            ServerPlayer player,
            String abilityId,
            int phase,
            int durationTicks,
            float radius
    ) {
        if (!hasProfile(abilityId)) return;
        sendState(player, abilityId, phase, durationTicks, radius);
        Profile base = profile(player, abilityId, boundedLevel(player, abilityId));
        emitActivation(player, abilityId, new Profile(
                phase, durationTicks, radius, base.particle(), base.pitch()
        ));
    }

    public static boolean hasProfile(String abilityId) {
        return abilityId != null && PROFILE_PATHS.contains(path(abilityId));
    }

    private static Profile profile(ServerPlayer player, String abilityId, int level) {
        String path = path(abilityId);
        return switch (path) {
            case "pull" -> new Profile(
                    ActivePresentationPayload.INSTANT, 0,
                    (float) ExpansionTuning.current().pullRadius(level), ParticleTypes.PORTAL, 0.85F
            );
            // Shield has its own live gauge; a second duration timer would become misleading under Overtime.
            case "shield" -> new Profile(
                    ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.END_ROD, 1.15F
            );
            // The platform is itself the sustained world indicator and can be extended by Overtime.
            case "temporary_platform" -> new Profile(
                    ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.END_ROD, 1.3F
            );
            case "berserk" -> new Profile(
                    ActivePresentationPayload.SUSTAINED,
                    ExpansionTuning.current().berserkDurationTicks(), 0.0F, ParticleTypes.CRIT, 0.65F
            );
            case "healing_wave" -> new Profile(
                    ActivePresentationPayload.INSTANT, 0,
                    (float) ExpansionTuning.current().healingWaveRadius(), ParticleTypes.HAPPY_VILLAGER, 1.45F
            );
            // First phase only. The actual sustained phase is driven by TemporalStateNetworking.
            case "time_acceleration" -> new Profile(
                    ActivePresentationPayload.CHANNEL,
                    AdvancedActiveTuning.current().timeAccelerationFocusTicks(), 0.0F, ParticleTypes.ENCHANT, 1.25F
            );
            // Actual sustained state is driven by PhaseStateNetworking.
            case "phase_shift" -> new Profile(
                    ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.PORTAL, 1.55F
            );
            case "threat_suppression" -> new Profile(
                    ActiveAbilityRouter.isThreatSuppressionEnabled(player)
                            ? ActivePresentationPayload.TOGGLE_ON : ActivePresentationPayload.TOGGLE_OFF,
                    0, 0.0F, ParticleTypes.WITCH, 0.95F
            );
            case "return_home" -> new Profile(
                    ActivePresentationPayload.CHANNEL,
                    SpecialActiveTuning.current().returnHomeFocusTicks(), 0.0F, ParticleTypes.PORTAL, 1.05F
            );
            case "gigantification" -> new Profile(
                    ActivePresentationPayload.SUSTAINED,
                    AdvancedWorldTuning.current().gigantificationDurationTicks(),
                    0.0F, ParticleTypes.CRIT, 0.6F
            );
            case "area_acceleration" -> new Profile(
                    ActivePresentationPayload.SUSTAINED, 30 * 20, 24.0F, ParticleTypes.ENCHANT, 1.35F
            );
            case "revive" -> new Profile(
                    ActivePresentationPayload.CHANNEL, 3 * 20, 20.0F, ParticleTypes.HAPPY_VILLAGER, 1.5F
            );
            case "time_stop" -> new Profile(
                    ActivePresentationPayload.SUSTAINED,
                    SpecialActiveTuning.current().timeStopDurationTicks(),
                    (float) SpecialActiveTuning.current().timeStopRadius(), ParticleTypes.END_ROD, 0.55F
            );
            case "divine_punishment" -> new Profile(
                    ActivePresentationPayload.INSTANT, 0,
                    (float) SpecialActiveTuning.current().divinePunishmentRadius(), ParticleTypes.END_ROD, 0.72F
            );
            case "cleanse" -> new Profile(ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.WITCH, 1.4F);
            case "swap", "rewind", "death_return" ->
                    new Profile(ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.PORTAL, 1.2F);
            case "shield_throw" -> new Profile(ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.END_ROD, 1.0F);
            case "clone" -> new Profile(ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.CLOUD, 0.9F);
            case "domination" -> new Profile(ActivePresentationPayload.INSTANT, 0,
                    (float) AdvancedWorldTuning.current().dominationRange(), ParticleTypes.WITCH, 0.8F);
            default -> new Profile(ActivePresentationPayload.INSTANT, 0, 0.0F, ParticleTypes.END_ROD, 1.0F);
        };
    }

    private static void emitActivation(ServerPlayer player, String abilityId, Profile profile) {
        ServerLevel level = player.level();
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        int count = path(abilityId).equals("time_stop") ? 28 : 14;
        level.sendParticles(profile.particle(), center.x, center.y, center.z, count,
                0.65D, 0.75D, 0.65D, 0.035D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, profile.pitch());
        if (profile.radius() > 0.0F) {
            emitRing(level, player.position(), profile.radius(), profile.particle());
        }
    }

    private static void emitRing(ServerLevel level, Vec3 center, float radius, ParticleOptions particle) {
        int points = Math.max(18, Math.min(56, Math.round(radius * 1.75F)));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y + 0.15D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void emitPath(ServerLevel level, List<Vec3> path, ParticleOptions particle, boolean impacts) {
        for (int i = 1; i < path.size(); i++) {
            emitLine(level, path.get(i - 1), path.get(i), particle, 24);
            if (impacts) {
                Vec3 impact = path.get(i);
                level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z,
                        8, 0.35D, 0.35D, 0.35D, 0.08D);
            }
        }
    }

    private static void emitLine(
            ServerLevel level,
            Vec3 start,
            Vec3 end,
            ParticleOptions particle,
            int cap
    ) {
        double distance = start.distanceTo(end);
        int steps = Math.max(2, Math.min(cap, (int) Math.ceil(distance * 2.0D)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 point = start.lerp(end, t);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static List<Vec3> captureShieldThrowPath(ServerPlayer player) {
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        String abilityId = id("shield_throw");
        int level = boundedLevel(state, abilityId);
        if (level <= 0) return List.of();
        double range = AdvancedActiveTuning.current().shieldThrowRange(level);
        int limit = AdvancedActiveTuning.current().shieldThrowTargets(level);
        double rangeSquared = range * range;
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= rangeSquared
                        && player.hasLineOfSight(candidate)
        );
        if (candidates.isEmpty()) return List.of();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity primary = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 delta = candidate.getBoundingBox().getCenter().subtract(eye);
            double along = delta.dot(look);
            if (along <= 0.0D || along > range) continue;
            double perpendicularSq = Math.max(0.0D, delta.lengthSqr() - along * along);
            double aimRadius = Math.max(1.0D, candidate.getBbWidth());
            if (perpendicularSq > aimRadius * aimRadius) continue;
            double score = perpendicularSq + along * 1.0E-4D;
            if (score < best) {
                best = score;
                primary = candidate;
            }
        }
        if (primary == null) return List.of();

        List<LivingEntity> targets = new ArrayList<>();
        targets.add(primary);
        LivingEntity cursor = primary;
        while (targets.size() < limit) {
            LivingEntity from = cursor;
            LivingEntity next = candidates.stream()
                    .filter(candidate -> !targets.contains(candidate))
                    .min(Comparator.comparingDouble(from::distanceToSqr))
                    .orElse(null);
            if (next == null) break;
            targets.add(next);
            cursor = next;
        }

        List<Vec3> points = new ArrayList<>();
        points.add(eye);
        for (LivingEntity target : targets) points.add(target.getBoundingBox().getCenter());
        points.add(eye);
        return List.copyOf(points);
    }

    private static void sendState(ServerPlayer player, String abilityId, int phase, int durationTicks, float radius) {
        // World-space readability is gameplay-side state and must not depend on whether a client packet
        // connection exists (embedded GameTest players and early join windows legitimately have none).
        ActiveRangeTelegraphService.onState(player, abilityId, phase, durationTicks, radius);
        if (player.connection == null || !ServerPlayNetworking.canSend(player, ActivePresentationPayload.TYPE)) return;
        ServerPlayNetworking.send(player, new ActivePresentationPayload(abilityId, phase, durationTicks, radius));
    }

    private static int boundedLevel(ServerPlayer player, String abilityId) {
        return boundedLevel(AugmentationPlayerStateService.state(player), abilityId);
    }

    private static int boundedLevel(AugmentationPlayerState state, String abilityId) {
        int level = state.level(abilityId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(abilityId).maxLevel());
    }

    private static String path(String abilityId) {
        int split = abilityId == null ? -1 : abilityId.indexOf(':');
        return split >= 0 ? abilityId.substring(split + 1) : abilityId == null ? "" : abilityId;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private record Profile(int phase, int durationTicks, float radius, ParticleOptions particle, float pitch) {
    }

    public record Capture(Vec3 origin, List<Vec3> path) {
        public Capture {
            origin = origin == null ? Vec3.ZERO : origin;
            path = path == null ? List.of() : List.copyOf(path);
        }
    }
}
