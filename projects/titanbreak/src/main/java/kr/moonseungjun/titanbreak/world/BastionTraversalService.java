package kr.moonseungjun.titanbreak.world;

import kr.moonseungjun.titanbreak.entity.BastionWalkerEntity;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class BastionTraversalService {
    private BastionTraversalService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State progression) {
        if (!(player.level() instanceof ServerLevel level) || player.isCreative() || player.isSpectator()) return;

        BastionWalkerEntity walker = level.getEntitiesOfClass(BastionWalkerEntity.class,
                        player.getBoundingBox().inflate(52.0D, 180.0D, 52.0D), Entity::isAlive)
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        if (walker == null) return;

        double relativeY = player.getY() - walker.getY();
        if (relativeY < 7.0D || relativeY > 138.0D) return;

        Vec3 offset = player.position().subtract(walker.position());
        double radial = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        if (radial < 18.0D || radial > 39.0D) return;

        int route = routeIndex(offset.x, offset.z);
        boolean gateOpen = walker.routeGateOpen(route, relativeY);
        boolean mobilityRig = progression.firstInstalledInstance("wire_hook_arm") != null
                || progression.firstInstalledInstance("wall_run_spurs") != null
                || progression.firstInstalledInstance("propulsion_legs") != null;

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (radial > 28.5D) {
            Vec3 inward = new Vec3(-offset.x, 0.0D, -offset.z);
            if (inward.horizontalDistanceSqr() > 1.0E-6D) {
                inward = inward.normalize().scale(Math.min(0.12D, (radial - 28.5D) * 0.025D));
                velocity = velocity.add(inward);
            }
        } else if (radial < 22.0D) {
            Vec3 outward = new Vec3(offset.x, 0.0D, offset.z);
            if (outward.horizontalDistanceSqr() > 1.0E-6D) {
                outward = outward.normalize().scale(Math.min(0.10D, (22.0D - radial) * 0.025D));
                velocity = velocity.add(outward);
            }
        }

        if (walker.armorClosed()) {
            if (velocity.y > 0.05D) velocity = new Vec3(velocity.x, 0.05D, velocity.z);
            player.setDeltaMovement(velocity);
            player.fallDistance = Math.min(player.fallDistance, 2.0D);
            return;
        }

        if (gateOpen) {
            double fallFloor = mobilityRig ? -0.045D : -0.11D;
            double vertical = Math.max(velocity.y, fallFloor);
            if (player.isSprinting() && horizontalSpeed > 0.025D) {
                vertical = Math.max(vertical, mobilityRig ? 0.18D : 0.095D);
            }
            velocity = new Vec3(velocity.x, vertical, velocity.z);
            player.fallDistance = Math.min(player.fallDistance, mobilityRig ? 0.35D : 1.25D);
        } else {
            // Intact armor is a traversal gate. Existing hook/propulsion movement is never cancelled,
            // but the free hull-climb assist stops until the plate on this route is removed.
            if (!mobilityRig && velocity.y > 0.20D) {
                velocity = new Vec3(velocity.x, 0.20D, velocity.z);
            }
            if (velocity.y < -0.16D) {
                velocity = new Vec3(velocity.x, -0.16D, velocity.z);
                player.fallDistance = Math.min(player.fallDistance, 2.0D);
            }
        }

        player.setDeltaMovement(velocity);
    }

    private static int routeIndex(double x, double z) {
        if (Math.abs(x) > Math.abs(z)) return x >= 0.0D ? 1 : 3;
        return z >= 0.0D ? 2 : 0;
    }
}
