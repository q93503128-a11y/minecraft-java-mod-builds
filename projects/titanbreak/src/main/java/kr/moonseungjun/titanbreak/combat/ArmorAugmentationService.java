package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Directional, projectile, and semantic-channel mitigation for skin / skeletal augmentations. */
public final class ArmorAugmentationService {
    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private static final class RuntimeState {
        Vec3 focusDirection = Vec3.ZERO;
        long lastDirectionalHit = Long.MIN_VALUE / 4L;
        int focusStacks;
        String lastDamageId = "";
    }

    private ArmorAugmentationService() {}

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        float damage = event.getNewDamage();
        if (damage <= 0.0F) return;

        TitanPlayerData.State state = TitanPlayerData.get(level.getServer()).state(player);
        Entity source = event.getSource().getEntity();
        Set<DamageChannelService.Channel> channels = source == null
                ? Set.of() : DamageChannelService.consume(player, source);

        TitanPlayerData.AugmentInstance subdermal = state.firstInstalledInstance("subdermal_armor");
        Entity direct = event.getSource().getDirectEntity();
        if (subdermal != null) {
            if (subdermal.enhancement() >= 7 && direct instanceof Projectile) damage *= 0.80F;
            if (subdermal.enhancement() >= 10
                    && channels.contains(DamageChannelService.Channel.ARMOR_BREAK)) damage *= 0.72F;
        }

        TitanPlayerData.AugmentInstance impact = state.firstInstalledInstance("impact_dispersal_frame");
        if (impact != null && impact.enhancement() >= 10
                && channels.contains(DamageChannelService.Channel.SHOCKWAVE)) {
            damage *= 0.68F;
        }

        TitanPlayerData.AugmentInstance reactive = state.firstInstalledInstance("reactive_dermis");
        if (reactive != null) {
            damage = applyReactiveDermis(player, event, damage, reactive, channels);
        } else {
            RUNTIME.remove(player.getUUID());
        }

        event.setNewDamage(Math.max(0.0F, damage));
    }

    private static float applyReactiveDermis(ServerPlayer player, LivingDamageEvent.Pre event, float damage,
                                             TitanPlayerData.AugmentInstance reactive,
                                             Set<DamageChannelService.Channel> channels) {
        Vec3 sourcePosition = event.getSource().getSourcePosition();
        if (sourcePosition == null && event.getSource().getEntity() != null) {
            sourcePosition = event.getSource().getEntity().position();
        }
        if (sourcePosition == null) return damage;

        Vec3 incoming = sourcePosition.subtract(player.position());
        if (incoming.lengthSqr() <= 1.0E-6D) return damage;
        incoming = incoming.normalize();

        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        long now = player.level().getGameTime();
        int enhancement = reactive.enhancement();
        long memory = enhancement >= 5 ? 50L : 34L;
        double directionTolerance = enhancement >= 5 ? 0.55D : 0.70D;
        boolean recent = now - runtime.lastDirectionalHit <= memory;
        boolean sameDirection = recent && runtime.focusDirection.lengthSqr() > 1.0E-6D
                && runtime.focusDirection.dot(incoming) >= directionTolerance;

        if (sameDirection) runtime.focusStacks = Math.min(3, runtime.focusStacks + 1);
        else runtime.focusStacks = enhancement >= 5 ? 1 : 0;

        String damageId = event.getSource().getMsgId();
        boolean sameDamageFamily = sameDirection && damageId.equals(runtime.lastDamageId);
        runtime.focusDirection = incoming;
        runtime.lastDirectionalHit = now;
        runtime.lastDamageId = damageId;

        if (runtime.focusStacks > 0) {
            double perStack = enhancement >= 5 ? 0.11D : 0.08D;
            double directionalReduction = Math.min(0.30D, perStack * runtime.focusStacks);
            damage *= (float) (1.0D - directionalReduction);
        }

        if (enhancement >= 7 && sameDamageFamily) damage *= 0.88F;

        if (enhancement >= 10 && channels.contains(DamageChannelService.Channel.WEAKPOINT)) {
            damage *= 0.65F;
        } else if (enhancement >= 10 && runtime.focusStacks >= 2
                && damage >= player.getMaxHealth() * 0.18F) {
            damage *= 0.82F;
        }

        return damage;
    }

    public static void clear(UUID playerId) {
        RUNTIME.remove(playerId);
    }

    public static void clearAll() {
        RUNTIME.clear();
    }
}
