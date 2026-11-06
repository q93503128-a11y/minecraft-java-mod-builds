package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.active.ActiveAbilityRouter;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityAttachments;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionEntityState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.cubemob.Slime;

/** Behavioral proof for permanent domination ownership, alliance and combat targeting. */
public final class DominationBehaviorGameTests implements CustomTestMethodInvoker {
    private static final String DOMINATION = AugmentationSurvival.MOD_ID + ":domination";

    @GameTest(maxTicks = 80)
    public void dominationConvertsEnemyIntoPersistentCombatAlly(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        player.setHealth(player.getMaxHealth());

        Mob dominated = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 4).spawn();
        dominated.setTarget(player);
        Mob hostile = helper.spawnMob(EntityTypes.ZOMBIE, 1, 2, 7).withNoFreeWill().spawn();
        AugmentationPlayerStateService.replace(player, AugmentationPlayerState.initial()
                .withAugmentLevel(DOMINATION, 1)
                .withActiveSlots(List.of(DOMINATION)));

        require(ActiveAbilityRouter.tryUseSlot(player, 0), "domination active did not route successfully");
        require(CompanionAugmentService.isDominated(dominated), "domination did not persist companion ownership");
        require(CompanionAugmentService.isOwnedBy(dominated, player.getUUID()), "dominated mob owner was not the caster");
        require(dominated.isPersistenceRequired(), "dominated mob was not protected from normal despawn");
        require(dominated.getTarget() == hostile,
                "dominated mob did not immediately replace its owner target with a nearby hostile");
        require(player.isAlliedTo(dominated) && dominated.isAlliedTo(player),
                "vanilla ally checks did not expose domination ownership");

        ServerLevel level = (ServerLevel) player.level();
        float ownerHealth = player.getHealth();
        DamageSource friendlyAttack = player.damageSources().mobAttack(dominated);
        boolean friendlyApplied = player.hurtServer(level, friendlyAttack, 4.0F);
        require(!friendlyApplied, "dominated companion damage reached its owner");
        require(player.getHealth() == ownerHealth, "dominated companion changed owner health");

        GameTestPlayers.runAfterDelay(helper, 3, () -> {
            require(dominated.getTarget() == hostile,
                    "dominated companion did not retain a nearby hostile target");
            require(!CompanionAugmentService.areAllied(dominated, hostile),
                    "unowned hostile was incorrectly treated as a domination ally");

            float hostileHealth = hostile.getHealth();
            // This assertion exercises the damage boundary; an earlier real attack can have
            // started vanilla hurt immunity before this deliberate test strike.
            // Minecraft 26.2 still uses Entity.invulnerableTime for damage cooldown.
            // damageCooldownTime was introduced in the 26.3 mapping transition.
            hostile.invulnerableTime = 0;
            DamageSource companionAttack = hostile.damageSources().mobAttack(dominated);
            boolean hostileApplied = hostile.hurtServer(level, companionAttack, 4.0F);
            boolean directGenericControl = !hostileApplied
                    && hostile.hurtServer(level, hostile.damageSources().generic(), 2.0F);
            require(hostileApplied, "dominated companion could not damage hostile: controlGeneric="
                    + directGenericControl + " defenderRemoved=" + hostile.isRemoved()
                    + " defenderInvulnerable=" + hostile.isInvulnerable()
                    + " defenderAlive=" + hostile.isAlive() + ", policy="
                    + CompanionAugmentService.mayDominatedMobDamage(dominated, hostile)
                    + ", isHostile=" + CompanionAugmentService.isHostileEntity(hostile)
                    + ", allied=" + CompanionAugmentService.areAllied(dominated, hostile)
                    + ", vanillaAllied=" + dominated.isAlliedTo(hostile)
                    + ", victimIFrames=" + hostile.invulnerableTime
                    + ", victimHP=" + hostile.getHealth()
                    + ", attacker=" + companionAttack.getEntity());
            require(hostile.getHealth() < hostileHealth,
                    "dominated companion attack did not reduce hostile health");
            GameTestPlayers.succeed(helper);
        });
    }


    @GameTest(maxTicks = 80)
    public void dominatedSlimeUsesOriginalTouchDamageOnlyAgainstApprovedTarget(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        Slime slime = helper.spawnMob(EntityTypes.SLIME, 3, 2, 3).withNoFreeWill().spawn();
        slime.setSize(2, true);
        Mob zombie = helper.spawnMob(EntityTypes.ZOMBIE, 3, 2, 3).withNoFreeWill().spawn();
        Mob cow = helper.spawnMob(EntityTypes.COW, 3, 2, 3).withNoFreeWill().spawn();
        slime.setAttached(CompanionEntityAttachments.ENTITY_STATE,
                CompanionEntityState.dominatedBy(player.getUUID()));

        slime.setTarget(zombie);
        float zombieHp = zombie.getHealth();
        slime.push(zombie);
        require(zombie.getHealth() < zombieHp,
                "dominated medium slime could not use its vanilla contact attack on a hostile mob");

        slime.setTarget(cow);
        float cowHp = cow.getHealth();
        slime.push(cow);
        require(Math.abs(cow.getHealth() - cowHp) < 0.001F,
                "dominated slime attacked a neutral cow without an owner command");

        zombie.invulnerableTime = 0;
        slime.setSize(1, true);
        slime.setTarget(zombie);
        float smallTargetHp = zombie.getHealth();
        slime.push(zombie);
        require(Math.abs(zombie.getHealth() - smallTargetHp) < 0.001F,
                "small vanilla slime incorrectly gained damaging contact");
        GameTestPlayers.succeed(helper);
    }

    @GameTest(maxTicks = 50)
    public void dominatedCreeperSelfRemovalReclaimsTheSlot(GameTestHelper helper) {
        ServerPlayer owner = GameTestPlayers.survivalPlayer(helper);
        Mob creeper = helper.spawnMob(EntityTypes.CREEPER, 2, 2, 2).withNoFreeWill().spawn();
        creeper.setAttached(CompanionEntityAttachments.ENTITY_STATE,
                CompanionEntityState.dominatedBy(owner.getUUID()));
        CompanionAugmentService.onRevivedCompanion(creeper);
        require(CompanionAugmentService.ownerRoster(owner).occupied(owner.getUUID()) > 0,
                "dominated creeper never claimed a slot");
        creeper.discard(); // A creeper explosion uses the same removal reason, not LivingEntity.die.
        GameTestPlayers.runAfterDelay(helper, 4, () -> {
            require(CompanionAugmentService.ownerRoster(owner).companions(owner.getUUID()).stream()
                            .noneMatch(record -> record.uuid().equals(creeper.getUUID().toString())),
                    "self-discarded creeper remains in the saved roster");
            require(CompanionAugmentService.ownerRoster(owner).occupied(owner.getUUID()) == 0,
                    "self-discarded creeper still occupies a domination slot");
            GameTestPlayers.succeed(helper);
        });
    }

    @GameTest(maxTicks = 50)
    public void hostileIllagerRetaliatesAgainstDominatedIllager(GameTestHelper helper) {
        ServerPlayer owner = GameTestPlayers.survivalPlayer(helper);
        Mob ally = helper.spawnMob(EntityTypes.EVOKER, 2, 2, 2).withNoFreeWill().spawn();
        Mob defender = helper.spawnMob(EntityTypes.EVOKER, 3, 2, 2).withNoFreeWill().spawn();
        ally.setAttached(CompanionEntityAttachments.ENTITY_STATE,
                CompanionEntityState.dominatedBy(owner.getUUID()));
        CompanionAugmentService.onRevivedCompanion(ally);
        require(!ally.isAlliedTo(defender), "controlled illager still allied with hostile illager");
        require(!defender.isAlliedTo(ally), "hostile illager still considers controlled illager allied");
        defender.hurtServer((ServerLevel) owner.level(), defender.damageSources().mobAttack(ally), 2.0F);
        require(defender.getTarget() == ally,
                "hostile illager did not retaliate against controlled illager damage");
        GameTestPlayers.succeed(helper);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
