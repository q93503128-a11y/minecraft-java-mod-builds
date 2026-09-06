package kr.moonseungjun.turnboundre.battle;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Live NeoForge adapter for M2 battle/world isolation.
 *
 * The deterministic battle core owns combat while an entity is bound to a live battle. These hooks
 * prevent the vanilla world simulation from simultaneously applying AI, incoming damage or knockback,
 * and tear down ownership before an entity leaves its level so dimension changes, disconnects and
 * removals cannot silently strand an orphan battle registry entry.
 */
public final class BattleWorldEventHooks {
    private final BattleManager battles;
    private final BattleWorldIsolation isolation;

    public BattleWorldEventHooks(BattleManager battles) {
        if (battles == null) throw new IllegalArgumentException("battles must not be null");
        this.battles = battles;
        this.isolation = new BattleWorldIsolation(battles);
    }

    public void register(IEventBus bus) {
        if (bus == null) throw new IllegalArgumentException("bus must not be null");
        bus.addListener(this::onIncomingDamage);
        bus.addListener(this::onKnockBack);
        bus.addListener(this::onEntityTickPre);
        bus.addListener(this::onEntityLeaveLevel);
    }

    private void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && !isolation.allowWorldDamage(entity.getUUID())) {
            event.setCanceled(true);
        }
    }

    private void onKnockBack(LivingKnockBackEvent event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && !isolation.allowWorldKnockback(entity.getUUID())) {
            event.setCanceled(true);
        }
    }

    private void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide()
                && entity instanceof Mob
                && !isolation.allowWorldAi(entity.getUUID())) {
            event.setCanceled(true);
        }
    }

    private void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        UUID entityId = event.getEntity().getUUID();
        if (!isolation.requiresBattleCleanupBeforeRemoval(entityId)) return;

        battles.battleForEntity(entityId)
                .map(BattleInstance::battleId)
                .ifPresent(battles::cleanup);
    }
}
