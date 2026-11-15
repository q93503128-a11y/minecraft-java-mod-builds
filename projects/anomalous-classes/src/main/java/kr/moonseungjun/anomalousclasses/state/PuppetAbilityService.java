package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Z: string pull, X: material-paid emergency stitch, C: owner-wide link drive. */
public final class PuppetAbilityService {
    private static final Map<UUID, Pull> PULLS = new HashMap<>();
    private record Pull(UUID owner, Vec3 destination, int steps) {}
    private PuppetAbilityService() {}

    public static void use(ServerPlayer player, int skill) {
        if (skill < 0 || skill > 2 || !PuppetService.isPuppeteer(player) || !player.isAlive()) return;
        PuppetAbilitySavedData data = PuppetAbilitySavedData.get(player.getServer());
        long now = PuppetAbilitySavedData.gameTime(player.getServer());
        if (!data.ready(player.getUUID(), skill, now)) {
            long remaining = data.state(player.getUUID()).cooldown(skill) - now;
            player.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.puppet_ability.cooldown",
                    Math.max(1L, (remaining + 19L) / 20L)), true);
            return;
        }
        boolean success = switch (skill) {
            case 0 -> startPull(player);
            case 1 -> emergencyStitch(player);
            case 2 -> hasActivePuppets(player) && !data.driveActive(player.getUUID(), now);
            default -> false;
        };
        if (success && data.use(player.getUUID(), skill, now)) {
            player.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.puppet_ability.success_" + skill), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.puppet_ability.failed_" + skill), true);
        }
    }

    private static PuppetEntity aimedPuppet(ServerPlayer player, double distance) {
        ServerLevel world = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 ray = player.getLookAngle().scale(distance);
        var hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(ray),
                player.getBoundingBox().expandTowards(ray).inflate(1.0),
                e -> e instanceof PuppetEntity p && p.isAlive()
                        && player.getUUID().equals(p.puppetOwner())
                        && p.distanceToSqr(player) <= distance * distance,
                distance * distance);
        if (hit == null || !(hit.getEntity() instanceof PuppetEntity puppet)) return null;
        BlockHitResult wall = world.clip(new ClipContext(eye, eye.add(ray),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS
                && wall.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye))
            return null;
        var record = PuppetRegistrySavedData.get(player.getServer()).find(puppet.puppetId()).orElse(null);
        if (record == null || record.location() != PuppetRegistrySavedData.Location.DEPLOYED
                || record.revision() != puppet.revision()) return null;
        return puppet;
    }

    private static boolean safeStep(ServerLevel world, PuppetEntity puppet, AABB box) {
        BlockPos feet = BlockPos.containing(box.getCenter().x, box.minY + .1, box.getCenter().z);
        BlockPos floor = BlockPos.containing(box.getCenter().x, box.minY - .12, box.getCenter().z);
        return world.getWorldBorder().isWithinBounds(box)
                && world.noCollision(puppet, box)
                && world.getFluidState(feet).isEmpty()
                && world.getBlockState(floor).isFaceSturdy(world, floor, Direction.UP);
    }

    /** A straight, prevalidated, incremental physical pull; never a wall teleport. */
    private static boolean startPull(ServerPlayer player) {
        PuppetEntity puppet = aimedPuppet(player, 12.0);
        if (puppet == null || PULLS.containsKey(puppet.puppetId())) return false;
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < .01) return false;
        Vec3 at = player.position().add(horizontal.normalize().scale(2.5));
        Vec3 goal = new Vec3(at.x, player.getY(), at.z);
        Vec3 path = goal.subtract(puppet.position());
        if (path.lengthSqr() <= .16 || path.lengthSqr() > 144) return false;
        int steps = (int) Math.ceil(path.length() / .45);
        Vec3 step = path.scale(1.0 / steps);
        AABB box = puppet.getBoundingBox();
        for (int i = 0; i < steps; i++) {
            box = box.move(step);
            if (!safeStep(player.serverLevel(), puppet, box)) return false;
        }
        PULLS.put(puppet.puppetId(), new Pull(player.getUUID(), goal, steps));
        puppet.getNavigation().stop();
        return true;
    }

    /** Called by a puppet's own server AI. No chunk loading and no damage immunity. */
    public static boolean tickPull(PuppetEntity puppet) {
        UUID id = puppet.puppetId();
        if (id == null) return false;
        Pull pull = PULLS.get(id);
        if (pull == null) return false;
        if (!(puppet.level() instanceof ServerLevel world) || !puppet.isAlive()
                || !pull.owner().equals(puppet.puppetOwner())
                || !PuppetService.isPuppeteer(world.getServer().getPlayerList().getPlayer(pull.owner()))) {
            PULLS.remove(id);
            return false;
        }
        Vec3 distance = pull.destination().subtract(puppet.position());
        if (pull.steps() <= 0 || distance.lengthSqr() < .045) {
            PULLS.remove(id);
            return false;
        }
        Vec3 move = distance.scale(Math.min(.45 / Math.sqrt(distance.lengthSqr()), 1.0));
        if (!safeStep(world, puppet, puppet.getBoundingBox().move(move))) {
            PULLS.remove(id);
            return false;
        }
        puppet.getNavigation().stop();
        puppet.move(MoverType.SELF, move);
        PULLS.put(id, new Pull(pull.owner(), pull.destination(), pull.steps() - 1));
        return true;
    }

    public static void stopPull(UUID id) { if (id != null) PULLS.remove(id); }

    private static boolean emergencyStitch(ServerPlayer player) {
        PuppetEntity puppet = aimedPuppet(player, 8.0);
        if (puppet == null || puppet.getHealth() <= 0) return false;
        float missing = puppet.getMaxHealth() - puppet.getHealth();
        if (missing < .05F) return false;
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        double skill = 1.0 + .006 * Math.min(25, PuppeteerRules.rank(profile, "emergency_repair"));
        float heal = (float) Math.min(missing, puppet.getMaxHealth() * .18 * skill);
        Map<String, Integer> bill = PuppetEconomy.emergencyStitchMaterials(puppet.blueprint(),
                heal / puppet.getMaxHealth());
        if (bill.isEmpty() || !PuppetService.hasMaterials(player.getInventory(), bill)) return false;
        PuppetService.consume(player.getInventory(), bill);
        puppet.heal(heal);
        puppet.syncSnapshot();
        return true;
    }

    private static boolean hasActivePuppets(ServerPlayer owner) {
        for (var record : PuppetRegistrySavedData.get(owner.getServer()).deployedBy(owner.getUUID())) {
            Entity e = owner.serverLevel().getEntity(record.blueprint().puppetId());
            if (e instanceof PuppetEntity puppet && puppet.isAlive()
                    && puppet.revision() == record.revision()) return true;
        }
        return false;
    }

    public static boolean driveActive(MinecraftServer server, UUID owner) {
        return server != null && owner != null
                && PuppetAbilitySavedData.get(server).driveActive(owner, PuppetAbilitySavedData.gameTime(server));
    }
}
