package kr.moonseungjun.riftfrontier.combat;

import net.minecraft.server.level.ServerLevel;
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
