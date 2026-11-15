package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteTraitModeStatePayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * V mode grants numeric access, not new unlocked powers. The only currently
 * implemented direct species ability is creeper burst at slot 1. Other slots
 * are deliberately unassigned until their real host adapters are implemented.
 */
public final class ParasiteTraitService {
    public static final int CREEPER_SLOT = 0;
    public static final String CREEPER_BLAST = "anomalousclasses:creeper_blast";
    private static final Map<UUID, Boolean> MODES = new HashMap<>();
    private ParasiteTraitService() {}

    public static void input(ServerPlayer owner, int slot) {
        if (owner.getServer() == null || !owner.isAlive() || !ParasiteService.current(owner)) {
            if (slot == -1 && owner.getServer() != null)
                ClassNetwork.sendToPlayer(owner, new ParasiteTraitModeStatePayload(false));
            return;
        }
        if (slot == -1) {
            boolean enabled = !MODES.getOrDefault(owner.getUUID(), false);
            MODES.put(owner.getUUID(), enabled);
            ClassNetwork.sendToPlayer(owner, new ParasiteTraitModeStatePayload(enabled));
            tell(owner, enabled ? "message.anomalousclasses.parasite_trait.enabled"
                    : "message.anomalousclasses.parasite_trait.disabled");
            return;
        }
        if (slot < 0 || slot > 9 || !MODES.getOrDefault(owner.getUUID(), false)) return;
        Mob host = ParasiteService.activeHost(owner);
        if (host == null) {
            tell(owner, "message.anomalousclasses.parasite_ability.no_host");
            return;
        }
        if (slot != CREEPER_SLOT) {
            tell(owner, "message.anomalousclasses.parasite_trait.empty");
            return;
        }
        var parasite = ParasiteSavedData.get(owner.getServer()).player(owner.getUUID());
        boolean nativeCreeper = host.getType() == EntityType.CREEPER;
        if (!nativeCreeper && parasite.mastery("minecraft:creeper") < 100) {
            tell(owner, "message.anomalousclasses.parasite_trait.unlearned");
            return;
        }
        long now = owner.serverLevel().getGameTime();
        ParasiteTraitSavedData cooldowns = ParasiteTraitSavedData.get(owner.getServer());
        long remaining = cooldowns.remaining(owner.getUUID(), CREEPER_BLAST, now);
        if (remaining > 0L) {
            owner.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.parasite_ability.cooldown",
                    (remaining + 19L) / 20L), true);
            return;
        }
        if (!cooldowns.cast(owner.getUUID(), CREEPER_BLAST, now, 600, 60)) return;
        burst(owner, host, nativeCreeper);
    }

    public static void reset(ServerPlayer owner) {
        MODES.remove(owner.getUUID());
        ClassNetwork.sendToPlayer(owner, new ParasiteTraitModeStatePayload(false));
    }
    public static void logout(UUID owner) {
        MODES.remove(owner);
    }

    private static void burst(ServerPlayer owner, Mob host, boolean nativeCreeper) {
        var level = owner.serverLevel();
        double radius = 3.5;
        AABB area = host.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != owner && entity != host)) {
            if (!canHit(owner, target)) continue;
            double distance = target.position().distanceTo(host.position());
            if (distance > radius) continue;
            float strength = (float) Math.max(2.0, 8.0 * (1.0 - distance / (radius + .5)));
            target.hurt(owner.damageSources().playerAttack(owner), strength);
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
                host.getX(), host.getY() + host.getBbHeight() * .5,
                host.getZ(), 6, 0.75, 0.5, 0.75, 0.06);
        level.playSound(null, host.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.9F, nativeCreeper ? 0.85F : 1.15F);
        tell(owner, "message.anomalousclasses.parasite_trait.burst");
        if (nativeCreeper) {
            // Native creeper blast consumes the original host. Normal host death
            // ejects its player and imposes the established re-intrusion lock.
            host.hurt(owner.damageSources().genericKill(),
                    Math.max(1000.0F, host.getMaxHealth() + 1.0F));
        } else {
            // Inherited blast does not destroy blocks, but costs real host HP.
            host.hurt(owner.damageSources().generic(),
                    Math.max(1.0F, host.getMaxHealth() * .12F));
        }
    }

    /** Shared hostile/friendly authorization for inherited bursts and host melee. */
    static boolean canHit(ServerPlayer owner, LivingEntity target) {
        if (owner.isAlliedTo(target)) return false;
        if (target instanceof Player && !owner.getServer().isPvpAllowed()) return false;
        if (target instanceof TamableAnimal tame && tame.isTame()) return false;
        if (target instanceof PuppetEntity puppet) {
            UUID ownerId = puppet.puppetOwner();
            if (ownerId != null) {
                if (ownerId.equals(owner.getUUID())) return false;
                ServerPlayer other = owner.getServer().getPlayerList().getPlayer(ownerId);
                if (other == null || owner.isAlliedTo(other)
                        || !owner.getServer().isPvpAllowed()) return false;
            }
        }
        UUID parasiteOwner = ParasiteSavedData.get(owner.getServer()).ownerOf(target.getUUID());
        if (parasiteOwner != null) {
            if (parasiteOwner.equals(owner.getUUID())) return false;
            ServerPlayer other = owner.getServer().getPlayerList().getPlayer(parasiteOwner);
            if (other == null || owner.isAlliedTo(other)
                    || !owner.getServer().isPvpAllowed()) return false;
        }
        var controlled = MentalDominatorSavedData.get(owner.getServer()).controlled(target.getUUID());
        if (controlled != null) {
            if (controlled.ownerId().equals(owner.getUUID())) return false;
            ServerPlayer other = owner.getServer().getPlayerList().getPlayer(controlled.ownerId());
            if (other == null || owner.isAlliedTo(other)
                    || !owner.getServer().isPvpAllowed()) return false;
        }
        return true;
    }
    private static void tell(ServerPlayer owner, String key) {
        owner.displayClientMessage(Component.translatable(key), true);
    }
}
