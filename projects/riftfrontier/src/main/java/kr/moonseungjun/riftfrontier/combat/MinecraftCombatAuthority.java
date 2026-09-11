package kr.moonseungjun.riftfrontier.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Shared Minecraft-side combat admission rules for authoritative attack runtimes. */
final class MinecraftCombatAuthority {
    private MinecraftCombatAuthority() {}

    static boolean isEligibleServerActor(LivingEntity actor) {
        return actor != null
            && actor.level() instanceof ServerLevel
            && actor.isAlive()
            && !actor.isRemoved()
            && !actor.isSpectator();
    }

    static boolean isEligibleServerActor(ServerLevel level, LivingEntity actor) {
        return level != null
            && isEligibleServerActor(actor)
            && actor.level() == level;
    }

    /**
     * Production boss authority is deliberately disjoint from authenticated player-combat authority.
     * This does not lock a concrete boss entity type; it only prevents one ServerPlayer instance from
     * simultaneously owning a validated boss capability and its player weapon capability.
     */
    static boolean isEligibleBossActor(ServerLevel level, LivingEntity actor) {
        return isEligibleServerActor(level, actor)
            && !(actor instanceof ServerPlayer);
    }

    static boolean isEligibleTarget(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        return level != null
            && attacker != null
            && target != null
            && target != attacker
            && target.level() == level
            && target.isAlive()
            && !target.isRemoved()
            && !target.isSpectator();
    }
}
