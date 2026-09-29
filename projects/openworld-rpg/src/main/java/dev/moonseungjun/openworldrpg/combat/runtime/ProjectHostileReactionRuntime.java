package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Server-owned forced-movement seam for project-bound hostile actors. */
public final class ProjectHostileReactionRuntime {
    private ProjectHostileReactionRuntime() {
    }

    public static PullApplication pullToward(
            LivingEntity target,
            Vec3 anchor,
            double maximumBlocks
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(anchor, "anchor");
        if (!Double.isFinite(maximumBlocks)
                || maximumBlocks < 0.0) {
            throw new IllegalArgumentException(
                    "maximumBlocks must be finite and non-negative."
            );
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElse(null);
        if (profile == null
                || !profile.reactionCapabilities()
                        .pullToward()
                || maximumBlocks == 0.0) {
            return PullApplication.rejected();
        }

        Vec3 before = target.position();
        Vec3 horizontal = new Vec3(
                anchor.x - before.x,
                0.0,
                anchor.z - before.z
        );
        double distance = horizontal.length();
        if (distance <= 1.0e-9) {
            return new PullApplication(
                    true,
                    0.0,
                    0.0
            );
        }

        double requested = Math.min(
                maximumBlocks,
                distance
        );
        target.move(
                MoverType.SELF,
                horizontal.normalize().scale(requested)
        );
        Vec3 after = target.position();
        double applied = Math.hypot(
                after.x - before.x,
                after.z - before.z
        );
        return new PullApplication(
                true,
                requested,
                applied
        );
    }

    public record PullApplication(
            boolean accepted,
            double requestedBlocks,
            double appliedBlocks
    ) {
        public static PullApplication rejected() {
            return new PullApplication(
                    false,
                    0.0,
                    0.0
            );
        }
    }
}
