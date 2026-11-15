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
 * V controls number-key access; species mastery is the permanent unlock.
 * Slot 1: creeper burst. Slot 2: guardian beam. Slot 3: ender blink.
 * Environmental adaptations and automatic dodge remain active with V OFF.
 */
public final class ParasiteTraitService {
    public static final int CREEPER_SLOT = 0;
    public static final int GUARDIAN_SLOT = 1;
    public static final int ENDERMAN_SLOT = 2;
    public static final String CREEPER_BLAST = "anomalousclasses:creeper_blast";
    public static final String GUARDIAN_BEAM = "anomalousclasses:guardian_beam";
    public static final String ENDER_BLINK = "anomalousclasses:ender_blink";
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
        String ability;
        String species;
        boolean nativeSpecies;
        int cooldownTicks;
        int recoveryTicks;
        switch (slot) {
            case CREEPER_SLOT -> {
                ability = CREEPER_BLAST;
                species = "minecraft:creeper";
                nativeSpecies = host.getType() == EntityType.CREEPER;
                cooldownTicks = 600;
                recoveryTicks = 60;
            }
            case GUARDIAN_SLOT -> {
                ability = GUARDIAN_BEAM;
                species = "minecraft:guardian";
                nativeSpecies = host.getType() == EntityType.GUARDIAN;
                cooldownTicks = 480;
                recoveryTicks = 60;
            }
            case ENDERMAN_SLOT -> {
                ability = ENDER_BLINK;
                species = "minecraft:enderman";
                nativeSpecies = host.getType() == EntityType.ENDERMAN;
                cooldownTicks = 560;
                recoveryTicks = 0;
            }
            default -> {
                tell(owner, "message.anomalousclasses.parasite_trait.empty");
                return;
            }
        }
        var parasite = ParasiteSavedData.get(owner.getServer()).player(owner.getUUID());
        if (!nativeSpecies && parasite.mastery(species) < 100) {
            tell(owner, switch (slot) {
                case GUARDIAN_SLOT -> "message.anomalousclasses.parasite_trait.guardian_unlearned";
                case ENDERMAN_SLOT -> "message.anomalousclasses.parasite_trait.ender_unlearned";
                default -> "message.anomalousclasses.parasite_trait.unlearned";
            });
            return;
        }

        long now = owner.serverLevel().getGameTime();
        ParasiteTraitSavedData cooldowns = ParasiteTraitSavedData.get(owner.getServer());
        long remaining = cooldowns.remaining(owner.getUUID(), ability, now);
        if (remaining > 0) {
            owner.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.parasite_ability.cooldown",
                    (remaining + 19L) / 20L), true);
            return;
        }
        // Only a successfully landed beam or safe teleport charges cooldown.
        if (slot == GUARDIAN_SLOT && !ParasiteSpeciesService.guardianBeam(owner, host)) {
            tell(owner, "message.anomalousclasses.parasite_trait.beam_no_target");
            return;
        }
        if (slot == ENDERMAN_SLOT && !ParasiteSpeciesService.blink(owner, host)) {
            tell(owner, "message.anomalousclasses.parasite_trait.blink_no_space");
            return;
        }
        if (!cooldowns.cast(owner.getUUID(), ability, now, cooldownTicks, recoveryTicks)) return;
        if (slot == CREEPER_SLOT) burst(owner, host, nativeSpecies);
        else tell(owner, slot == GUARDIAN_SLOT
                ? "message.anomalousclasses.parasite_trait.guardian_beam"
                : "message.anomalousclasses.parasite_trait.ender_blink");
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
