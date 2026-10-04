package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.multiplayer.ProjectDownedRuntime;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared authored player hit-reaction gate.
 *
 * <p>Animation adapters may render the reaction, but this runtime owns the server duration and
 * action/dodge lock. Combat Temper modifies only ordinary non-launch stagger as locked by canon.</p>
 */
public final class ProjectPlayerReactionRuntime {
    public static final String GUARD_BREAK_REACTION_ID =
            "openworld_rpg:reaction/guard_break";

    private ProjectPlayerReactionRuntime() {
    }

    public static Application apply(
            ServerPlayer player,
            ReactionSpec spec
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spec, "spec");
        if (spec.kind() == ReactionKind.NONE) {
            return Application.none();
        }

        ProjectDownedRuntime.interruptRevive(player);
        long nowTick = player.level().getGameTime();
        double multiplier = spec.ordinaryNonLaunch()
                ? WarriorSkillRuntime
                        .ordinaryHitStaggerTakenMultiplier(
                                player,
                                nowTick
                        )
                : 1.0;
        int resolvedTicks = resolveDurationTicks(
                spec.durationTicks(),
                multiplier
        );
        var result = ProjectPlayerActionRuntime.applyReaction(
                player,
                spec.reactionId(),
                resolvedTicks
        );
        return new Application(
                result.accepted(),
                spec.kind(),
                spec.durationTicks(),
                resolvedTicks,
                result.endTick()
        );
    }

    public static Application applyGuardBreak(
            ServerPlayer player,
            int durationTicks
    ) {
        return apply(
                player,
                new ReactionSpec(
                        GUARD_BREAK_REACTION_ID,
                        ReactionKind.HARD_STAGGER,
                        durationTicks,
                        false
                )
        );
    }

    public static int resolveDurationTicks(
            int authoredDurationTicks,
            double durationMultiplier
    ) {
        if (authoredDurationTicks <= 0
                || !Double.isFinite(durationMultiplier)
                || durationMultiplier <= 0.0
                || durationMultiplier > 1.0) {
            throw new IllegalArgumentException(
                    "Invalid reaction duration inputs."
            );
        }
        return Math.max(
                1,
                (int) Math.ceil(
                        authoredDurationTicks
                                * durationMultiplier
                                - 1.0e-9
                )
        );
    }

    public enum ReactionKind {
        NONE,
        STAGGER,
        HARD_STAGGER,
        KNOCKDOWN,
        LAUNCH
    }

    public record ReactionSpec(
            String reactionId,
            ReactionKind kind,
            int durationTicks,
            boolean ordinaryNonLaunch
    ) {
        public ReactionSpec {
            Objects.requireNonNull(reactionId, "reactionId");
            Objects.requireNonNull(kind, "kind");
            if (kind == ReactionKind.NONE) {
                if (durationTicks != 0
                        || ordinaryNonLaunch
                        || !reactionId.isEmpty()) {
                    throw new IllegalArgumentException(
                            "NONE reaction cannot carry authored state."
                    );
                }
            } else if (reactionId.isBlank()
                    || durationTicks <= 0
                    || (ordinaryNonLaunch
                            && kind != ReactionKind.STAGGER)) {
                throw new IllegalArgumentException(
                        "Invalid authored reaction spec."
                );
            }
        }

        public static ReactionSpec none() {
            return new ReactionSpec(
                    "",
                    ReactionKind.NONE,
                    0,
                    false
            );
        }
    }

    public record Application(
            boolean applied,
            ReactionKind kind,
            int authoredDurationTicks,
            int resolvedDurationTicks,
            long endTick
    ) {
        private static Application none() {
            return new Application(
                    false,
                    ReactionKind.NONE,
                    0,
                    0,
                    Long.MIN_VALUE / 4
            );
        }
    }
}
