package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongEncounterService;
import dev.moonseungjun.openworldrpg.progression.r01.R01NatureSpiritRewardService;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fail-closed R01 bridge for a real server-authoritative control/debuff effect applied to an
 * encounter enemy.
 *
 * <p>Callers invoke this only after their own control authority has accepted a non-no-op effect.
 * Proximity, party membership and attempted/fully suppressed control never enter this bridge.</p>
 */
public final class R01EnemyControlContributionBridge {
    private R01EnemyControlContributionBridge() {
    }

    public static Contribution recordSuccessfulControl(
            ServerPlayer sourcePlayer,
            LivingEntity target
    ) {
        Objects.requireNonNull(sourcePlayer, "sourcePlayer");
        Objects.requireNonNull(target, "target");

        if (sourcePlayer.level().isClientSide()
                || sourcePlayer.level() != target.level()
                || !sourcePlayer.isAlive()
                || !target.isAlive()) {
            return Contribution.none();
        }

        Identifier targetId = BuiltInRegistries.ENTITY_TYPE.getKey(
                target.getType()
        );
        ActorKind actorKind = actorKind(
                targetId == null ? null : targetId.toString(),
                ExternalActorBindingRuntime.isAuthoredSpawn(target)
        );

        return switch (actorKind) {
            case EARTHLOONG -> new Contribution(
                    actorKind,
                    R01EarthloongEncounterService
                            .recordValidatedSupportContribution(
                                    target,
                                    sourcePlayer
                            )
            );
            case NATURE_SPIRIT -> new Contribution(
                    actorKind,
                    R01NatureSpiritRewardService
                            .recordValidatedSupportContribution(
                                    target,
                                    sourcePlayer
                            )
            );
            case NONE -> Contribution.none();
        };
    }

    static ActorKind actorKind(
            String entityTypeId,
            boolean authoredSpawn
    ) {
        if (R01ExternalActorCatalog.EARTHLOONG.equals(
                entityTypeId
        )) {
            return ActorKind.EARTHLOONG;
        }
        if (authoredSpawn
                && R01ExternalActorCatalog.NATURE_SPIRIT.equals(
                        entityTypeId
                )) {
            return ActorKind.NATURE_SPIRIT;
        }
        return ActorKind.NONE;
    }

    enum ActorKind {
        NONE,
        EARTHLOONG,
        NATURE_SPIRIT
    }

    public record Contribution(
            ActorKind actorKind,
            boolean newParticipation
    ) {
        public Contribution {
            Objects.requireNonNull(actorKind, "actorKind");
            if (actorKind == ActorKind.NONE
                    && newParticipation) {
                throw new IllegalArgumentException(
                        "Unknown control target cannot create participation."
                );
            }
        }

        public static Contribution none() {
            return new Contribution(
                    ActorKind.NONE,
                    false
            );
        }
    }
}
