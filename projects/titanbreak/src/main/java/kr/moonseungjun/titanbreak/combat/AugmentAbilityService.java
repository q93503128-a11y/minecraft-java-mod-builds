package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.network.TitanbreakNetwork;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AugmentAbilityService {
    private static final Set<String> REPLACEMENT_ARMS = Set.of(
            "blade_arm", "high_frequency_blade_arm", "power_arm", "wire_hook_arm",
            "rail_projector_arm", "photon_emitter_arm", "shock_palm", "shield_projector_arm");

    private static final Map<UUID, Map<String, Long>> NEXT_READY = new ConcurrentHashMap<>();
    private static final Map<UUID, long[]> PHASE_CHARGES = new ConcurrentHashMap<>();
    private static final Map<UUID, ShieldState> SHIELDS = new ConcurrentHashMap<>();

    private record RayTarget(Entity entity, double distance) {}
    private record ShieldState(long endTick, float baselineAbsorption) {}

    private AugmentAbilityService() {}

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        AugmentationCatalog.Slot slot = event.getHand() == InteractionHand.MAIN_HAND
                ? AugmentationCatalog.Slot.RIGHT_ARM_MAIN : AugmentationCatalog.Slot.LEFT_ARM_MAIN;
        String augmentId = state.installed(slot);
        if (!REPLACEMENT_ARMS.contains(augmentId)) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        useArm(player, slot);
    }

    public static void tick(ServerPlayer player) {
        ShieldState shield = SHIELDS.get(player.getUUID());
        if (shield == null || !(player.level() instanceof ServerLevel level)) return;
        if (level.getGameTime() < shield.endTick()) return;
        player.setAbsorptionAmount(Math.min(player.getAbsorptionAmount(), shield.baselineAbsorption()));
        SHIELDS.remove(player.getUUID());
    }

    public static void usePhaseStep(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        TitanPlayerData.AugmentInstance phase = state.firstInstalledInstance("phase_step_spine");
        if (phase == null) return;

        long now = level.getGameTime();
        int chargeCount = phase.enhancement() >= 7 ? 2 : 1;
        long[] charges = PHASE_CHARGES.computeIfAbsent(player.getUUID(), ignored -> new long[]{0L, 0L});
        int charge = -1;
        for (int i = 0; i < chargeCount; i++) {
            if (charges[i] <= now) {
                charge = i;
                break;
            }
        }
        if (charge < 0) return;

        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() < 1.0E-6D) return;
        direction = direction.normalize();
        double distance = 5.5D + Math.max(0, phase.mk() - 1) * 0.35D + (phase.enhancement() >= 5 ? 2.0D : 0.0D);
        Vec3 destination = phaseDestination(level, player, direction, distance);
        if (destination == null) return;

        if (!spend(player, state, "phase_step_spine", 0.60D, 0.70D)) return;
        player.teleportTo(destination.x, destination.y, destination.z);
        player.setDeltaMovement(direction.scale(0.18D));
        player.hurtMarked = true;
        charges[charge] = now + phaseCooldown(phase);
        data.addMasteryXp(player, "phase_step_spine", 3);
        TitanbreakNetwork.sync(player);
    }

    public static void useHook(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        AugmentationCatalog.Slot slot = "wire_hook_arm".equals(state.installed(AugmentationCatalog.Slot.RIGHT_ARM_MAIN))
                ? AugmentationCatalog.Slot.RIGHT_ARM_MAIN
                : AugmentationCatalog.Slot.LEFT_ARM_MAIN;
        if (!"wire_hook_arm".equals(state.installed(slot))) return;
        useArm(player, slot);
    }

    public static void useArm(ServerPlayer player, AugmentationCatalog.Slot slot) {
        if (!(player.level() instanceof ServerLevel level)) return;
        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        String augmentId = state.installed(slot);
        if (!REPLACEMENT_ARMS.contains(augmentId)) return;
        String cooldownKey = augmentId + "@" + slot.name();
        long now = level.getGameTime();
        if (!ready(player, cooldownKey, now)) return;

        boolean used = switch (augmentId) {
            case "blade_arm" -> slash(player, state, augmentId, false);
            case "high_frequency_blade_arm" -> slash(player, state, augmentId, true);
            case "power_arm" -> powerArm(player, state);
            case "wire_hook_arm" -> wireHook(player, state);
            case "rail_projector_arm" -> railShot(player, state);
            case "photon_emitter_arm" -> photonBeam(player, state);
            case "shock_palm" -> shockPalm(player, state);
            case "shield_projector_arm" -> shield(player, state);
            default -> false;
        };
        if (!used) return;

        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance(augmentId);
        int mk = instance == null ? 1 : instance.mk();
        setCooldown(player, cooldownKey, now + cooldownFor(augmentId, mk,
                instance == null ? 0 : instance.enhancement()));
        data.addMasteryXp(player, augmentId, switch (augmentId) {
            case "rail_projector_arm", "photon_emitter_arm" -> 4;
            case "high_frequency_blade_arm", "shock_palm", "shield_projector_arm" -> 3;
            default -> 2;
        });
        TitanbreakNetwork.sync(player);
    }

    private static boolean slash(ServerPlayer player, TitanPlayerData.State state, String augmentId, boolean highFrequency) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance(augmentId);
        int enhancement = instance == null ? 0 : instance.enhancement();
        double range = highFrequency ? 5.5D : 4.2D;
        double width = highFrequency ? 1.25D : 1.0D;
        List<RayTarget> targets = rayTargets(player, range + (enhancement >= 10 ? 1.5D : 0.0D), width);
        if (!spend(player, state, augmentId, highFrequency ? 0.42D : 0.30D, highFrequency ? 0.55D : 0.30D)) return false;
        int maxTargets = highFrequency && enhancement >= 10 ? 3 : 1;
        double damage = highFrequency ? 72.0D : 46.0D;
        for (int i = 0; i < Math.min(maxTargets, targets.size()); i++) damage(player, targets.get(i).entity(), damage);
        return true;
    }

    private static boolean powerArm(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance("power_arm");
        int enhancement = instance == null ? 0 : instance.enhancement();
        List<RayTarget> targets = rayTargets(player, 4.2D, 1.35D);
        if (!spend(player, state, "power_arm", 0.45D, 0.45D)) return false;
        if (!targets.isEmpty()) {
            Entity target = targets.getFirst().entity();
            damage(player, target, 58.0D);
            shove(player, target, enhancement >= 3 ? 1.65D : 1.25D);
        }
        if (enhancement >= 10) {
            for (Entity target : nearbyTargets(player, 4.5D)) {
                if (!targets.isEmpty() && target == targets.getFirst().entity()) continue;
                damage(player, target, 22.0D);
                shove(player, target, 0.85D);
            }
        }
        return true;
    }

    private static boolean wireHook(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance("wire_hook_arm");
        int enhancement = instance == null ? 0 : instance.enhancement();
        double range = enhancement >= 5 ? 32.0D : 24.0D;

        if (enhancement >= 7) {
            List<RayTarget> entities = rayTargets(player, range, 1.2D);
            if (!entities.isEmpty() && entities.getFirst().entity() instanceof LivingEntity target) {
                if (!spend(player, state, "wire_hook_arm", 0.55D, 0.35D)) return false;
                Vec3 pull = player.position().subtract(target.position());
                if (pull.lengthSqr() > 1.0E-6D) {
                    pull = pull.normalize().scale(1.15D).add(0.0D, 0.18D, 0.0D);
                    target.setDeltaMovement(pull);
                    target.hurtMarked = true;
                }
                return true;
            }
        }

        HitResult hit = player.pick(range, 1.0F, false);
        if (hit.getType() == HitResult.Type.MISS) return false;
        if (!spend(player, state, "wire_hook_arm", 0.55D, 0.35D)) return false;
        Vec3 direction = hit.getLocation().subtract(player.position());
        if (direction.lengthSqr() <= 1.0E-6D) return false;
        player.setDeltaMovement(direction.normalize().scale(enhancement >= 10 ? 1.78D : 1.55D).add(0.0D, 0.18D, 0.0D));
        player.hurtMarked = true;
        return true;
    }

    private static boolean railShot(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance("rail_projector_arm");
        int enhancement = instance == null ? 0 : instance.enhancement();
        int mk = instance == null ? 1 : instance.mk();
        double range = 46.0D + (mk - 1) * 4.0D;
        int maxTargets = enhancement >= 5 ? 5 : 2;
        double damage = enhancement >= 7 ? 96.0D : 82.0D;
        if (enhancement >= 10 && ReflexDriveService.active(player.getUUID())) damage *= 1.20D;
        if (!spend(player, state, "rail_projector_arm", 0.72D, 0.72D)) return false;
        List<RayTarget> targets = rayTargets(player, range, 0.75D);
        for (int i = 0; i < Math.min(maxTargets, targets.size()); i++) {
            damage(player, targets.get(i).entity(), damage * Math.max(0.60D, 1.0D - i * 0.10D));
        }
        return true;
    }

    private static boolean photonBeam(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance("photon_emitter_arm");
        int enhancement = instance == null ? 0 : instance.enhancement();
        int mk = instance == null ? 1 : instance.mk();
        boolean overclock = enhancement >= 10;
        double range = (enhancement >= 5 ? 52.0D : 38.0D) + (mk - 1) * 3.0D;
        double width = enhancement >= 5 ? 1.55D : 0.95D;
        int maxTargets = enhancement >= 7 ? 8 : 2;
        double damage = overclock ? 210.0D : 112.0D;
        double factor = overclock ? 1.15D : 0.72D;
        if (!spend(player, state, "photon_emitter_arm", factor, factor)) return false;
        List<RayTarget> targets = rayTargets(player, range, width);
        for (int i = 0; i < Math.min(maxTargets, targets.size()); i++) damage(player, targets.get(i).entity(), damage);
        return true;
    }

    private static boolean shockPalm(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance("shock_palm");
        int enhancement = instance == null ? 0 : instance.enhancement();
        double radius = enhancement >= 5 ? 6.0D : 4.0D;
        if (!spend(player, state, "shock_palm", 0.62D, 0.65D)) return false;
        List<Entity> firstRing = nearbyTargets(player, radius);
        for (Entity target : firstRing) {
            if (target instanceof LivingEntity living && enhancement >= 7 && living.getAbsorptionAmount() > 0.0F) {
                living.setAbsorptionAmount(Math.max(0.0F, living.getAbsorptionAmount() - 6.0F));
            }
            damage(player, target, 44.0D);
            shove(player, target, 1.05D);
        }
        if (enhancement >= 10) {
            for (Entity target : nearbyTargets(player, radius + 4.0D)) {
                if (firstRing.contains(target)) continue;
                damage(player, target, 20.0D);
                shove(player, target, 0.55D);
            }
        }
        return true;
    }

    private static boolean shield(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance instance = state.firstInstalledInstance("shield_projector_arm");
        int enhancement = instance == null ? 0 : instance.enhancement();
        if (!spend(player, state, "shield_projector_arm", 0.62D, 0.60D)) return false;
        ServerLevel level = (ServerLevel) player.level();
        ShieldState previous = SHIELDS.remove(player.getUUID());
        float current = player.getAbsorptionAmount();
        if (previous != null) current = Math.min(current, previous.baselineAbsorption());
        float baseline = current;
        float shieldAmount = (float) CombatScale.toInternal(enhancement >= 10 ? 70.0D : enhancement >= 5 ? 50.0D : 35.0D);
        player.setAbsorptionAmount(baseline + shieldAmount);
        SHIELDS.put(player.getUUID(), new ShieldState(level.getGameTime() + (enhancement >= 10 ? 80L : 55L), baseline));
        return true;
    }

    private static boolean spend(ServerPlayer player, TitanPlayerData.State state, String augmentId,
                                 double powerFactor, double heatFactor) {
        AugmentationCatalog.Definition definition = AugmentationCatalog.byId(augmentId);
        if (definition == null) return false;
        double power = Math.max(0.0D, definition.powerLoad()) * powerFactor * state.powerLoadMultiplier(augmentId);
        if (!AugmentationResourceService.trySpendBurstPower(player, state, power)) return false;
        if (definition.heatLoad() > 0 && heatFactor > 0.0D) {
            TitanPlayerData data = TitanPlayerData.get(((ServerLevel) player.level()).getServer());
            double rawHeat = definition.heatLoad() * heatFactor * state.heatLoadMultiplier(augmentId);
            data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
        }
        return true;
    }

    private static Vec3 phaseDestination(ServerLevel level, ServerPlayer player, Vec3 direction, double distance) {
        Vec3 origin = player.position();
        for (double step = distance; step >= 1.0D; step -= 0.5D) {
            Vec3 delta = direction.scale(step);
            if (level.noCollision(player, player.getBoundingBox().move(delta))) return origin.add(delta);
        }
        return null;
    }

    private static List<RayTarget> rayTargets(ServerPlayer player, double range, double width) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(range));
        AABB search = new AABB(start, end).inflate(width + 2.0D);
        List<RayTarget> result = new ArrayList<>();
        for (Entity entity : level.getEntities(player, search, target -> target.isAlive() && target.isPickable())) {
            Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            Vec3 relative = center.subtract(start);
            double distance = relative.dot(direction);
            if (distance < 0.0D || distance > range) continue;
            double radialSqr = relative.subtract(direction.scale(distance)).lengthSqr();
            double allowance = width + Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.35D;
            if (radialSqr <= allowance * allowance) result.add(new RayTarget(entity, distance));
        }
        result.sort(Comparator.comparingDouble(RayTarget::distance));
        return result;
    }

    private static List<Entity> nearbyTargets(ServerPlayer player, double radius) {
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntities(player, player.getBoundingBox().inflate(radius),
                target -> target.isAlive() && target.isPickable() && target.distanceToSqr(player) <= radius * radius);
    }

    private static void damage(ServerPlayer player, Entity target, double visibleDamage) {
        if (!(player.level() instanceof ServerLevel level) || visibleDamage <= 0.0D) return;
        target.hurtServer(level, player.damageSources().playerAttack(player), (float) CombatScale.toInternal(visibleDamage));
    }

    private static void shove(ServerPlayer player, Entity target, double strength) {
        if (!(target instanceof LivingEntity living)) return;
        Vec3 away = living.position().subtract(player.position());
        if (away.lengthSqr() <= 1.0E-6D) return;
        away = away.normalize();
        living.push(away.x * strength, 0.20D, away.z * strength);
        living.hurtMarked = true;
    }

    private static boolean ready(ServerPlayer player, String key, long now) {
        return NEXT_READY.getOrDefault(player.getUUID(), Map.of()).getOrDefault(key, 0L) <= now;
    }

    private static void setCooldown(ServerPlayer player, String key, long readyTick) {
        NEXT_READY.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>()).put(key, readyTick);
    }

    private static int cooldownFor(String augmentId, int mk, int enhancement) {
        int base = switch (augmentId) {
            case "blade_arm" -> 11;
            case "high_frequency_blade_arm" -> 16;
            case "power_arm" -> 20;
            case "wire_hook_arm" -> enhancement >= 10 ? 7 : 16;
            case "rail_projector_arm" -> enhancement >= 7 ? 30 : 24;
            case "photon_emitter_arm" -> enhancement >= 10 ? 64 : 42;
            case "shock_palm" -> 28;
            case "shield_projector_arm" -> 62;
            default -> 20;
        };
        return Math.max(6, base - Math.max(0, mk - 1) * 2);
    }

    private static int phaseCooldown(TitanPlayerData.AugmentInstance instance) {
        return Math.max(30, 70 - Math.max(0, instance.mk() - 1) * 6);
    }

    public static void clear(UUID playerId) {
        NEXT_READY.remove(playerId);
        PHASE_CHARGES.remove(playerId);
        SHIELDS.remove(playerId);
    }

    public static void clearAll() {
        NEXT_READY.clear();
        PHASE_CHARGES.clear();
        SHIELDS.clear();
    }
}
