package kr.moonseungjun.anomalousclasses.state;

import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Server-side ownership, shield geometry and frame/core damage decisions.
 * Never assumes scoreboard friendly fire alone protects against projectiles.
 */
public final class PuppetCombatService {
    private static final double GUARD_DISTANCE = 4.25;
    private PuppetCombatService() {}

    public static boolean allied(PuppetEntity puppet, LivingEntity other) {
        if (puppet.puppetOwner() == null || puppet.level().getServer() == null) return true;
        MinecraftServer server = puppet.level().getServer();
        ServerPlayer owner = server.getPlayerList().getPlayer(puppet.puppetOwner());
        if (other.getUUID().equals(puppet.puppetOwner())) return true;
        if (other instanceof Player player) {
            // Fail closed if owner is offline, so an orphan does not attack strangers.
            return owner == null || owner.isAlliedTo(player);
        }
        if (other instanceof PuppetEntity friend) {
            if (friend.puppetOwner() == null || owner == null) return true;
            if (friend.puppetOwner().equals(puppet.puppetOwner())) return true;
            ServerPlayer friendOwner = server.getPlayerList().getPlayer(friend.puppetOwner());
            return friendOwner == null || owner.isAlliedTo(friendOwner);
        }
        var linked = MentalDominatorSavedData.get(server).controlled(other.getUUID());
        if (linked != null && linked.linkState() == MentalDominatorSavedData.LinkState.ACTIVE) {
            if (linked.ownerId().equals(puppet.puppetOwner())) return true;
            ServerPlayer friendOwner = server.getPlayerList().getPlayer(linked.ownerId());
            return owner == null || friendOwner == null || owner.isAlliedTo(friendOwner);
        }
        return false;
    }

    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || event.getNewDamage() <= 0.0F) return;
        DamageSource source = event.getSource();
        Entity actor = source.getEntity();
        if (actor instanceof PuppetEntity puppet && allied(puppet, victim)) {
            event.setNewDamage(0.0F);
            return;
        }
        if (victim instanceof PuppetEntity puppet) {
            if (actor instanceof LivingEntity attacker && allied(puppet, attacker)) {
                event.setNewDamage(0.0F);
                return;
            }
            if (puppet.puppetOwner() != null && puppet.blueprint() != null && actor != null) {
                var profile = ClassProfileSavedData.get(level.getServer()).profile(puppet.puppetOwner());
                float reduced = (float) Math.max(0.0,
                        event.getNewDamage() * PuppeteerRules.incomingDamageFactor(profile, puppet.blueprint()));
                event.setNewDamage(reduced);
            }
        }

        // Guard is physical and directional, never an omnipresent team-wide shield.
        // A puppet protects the owner or allied player only when its actual body
        // occupies the line between that player and a hostile damage origin.
        if (!(victim instanceof ServerPlayer player) || actor == null || actor == victim
                || event.getNewDamage() <= 0.0F) return;
        Vec3 sourcePosition = source.getDirectEntity() != null
                ? source.getDirectEntity().position() : actor.position();
        Vec3 direction = sourcePosition.subtract(victim.position());
        direction = new Vec3(direction.x, 0, direction.z);
        double lengthSquared = direction.lengthSqr();
        if (lengthSquared < 0.09) return;
        for (PuppetEntity shield : level.getEntitiesOfClass(PuppetEntity.class,
                victim.getBoundingBox().inflate(GUARD_DISTANCE),
                p -> p.isAlive() && p.guarding() && p.device() == PuppetBlueprint.Device.SHIELD
                        && p.puppetOwner() != null && allied(p, player))) {
            Vec3 offset = shield.position().subtract(victim.position());
            offset = new Vec3(offset.x, 0, offset.z);
            double projected = offset.dot(direction) / lengthSquared;
            if (projected <= 0.05 || projected >= 0.98) continue;
            double perpendicular = offset.subtract(direction.scale(projected)).lengthSqr();
            double coverage = shield.getBbWidth() * .5 + .45;
            if (perpendicular > coverage * coverage) continue;
            // Defense takes place at this shield's actual server hitbox location.
            float blockFraction = shield.frame() == PuppetBlueprint.Frame.HEAVY ? .48F : .36F;
            event.setNewDamage(Math.max(0.0F, event.getNewDamage() * (1.0F - blockFraction)));
            return; // No stacking shields for exploitative permanent invulnerability.
        }
    }
}
