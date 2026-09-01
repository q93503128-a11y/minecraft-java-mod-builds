package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.entity.SpitterEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Server-authoritative residual effects for enemy attacks that outlive the initial hit. */
public final class EnemyAttackEffectService {
    private static final double CORROSION_RADIUS = 2.75D;
    private static final int MAX_ZONES = 32;
    private static final List<CorrosionZone> CORROSION = new CopyOnWriteArrayList<>();

    private record CorrosionZone(ServerLevel level, Vec3 center, long endTick) {}

    private EnemyAttackEffectService() {}

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof Projectile projectile) || !(projectile.getOwner() instanceof SpitterEntity spitter)) return;

        event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0, false, true, true), spitter);
        if (CORROSION.size() >= MAX_ZONES) CORROSION.removeFirst();
        CORROSION.add(new CorrosionZone(level, event.getEntity().position(), level.getGameTime() + 120L));
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % 5 != 0 || !(player.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        CORROSION.removeIf(zone -> zone.endTick() <= now);
        double radiusSqr = CORROSION_RADIUS * CORROSION_RADIUS;
        for (CorrosionZone zone : CORROSION) {
            if (zone.level() != level || zone.center().distanceToSqr(player.position()) > radiusSqr) continue;
            MobEffectInstance poison = player.getEffect(MobEffects.POISON);
            if (poison == null || poison.getDuration() < 20) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 35, 0, false, true, true));
            }
        }
    }

    public static void clearAll() {
        CORROSION.clear();
    }
}
