package kr.moonseungjun.turnboundre.battle;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Live NeoForge adapter for M2 battle/world isolation.
 *
 * <p>The deterministic battle core owns combat while an entity is bound to a live battle. These hooks
 * prevent the vanilla world simulation from simultaneously applying AI, attacks, incoming damage or
 * knockback, and tear down ownership before an entity leaves its level so dimension changes, disconnects
 * and removals cannot silently strand an orphan battle registry entry.</p>
 *
 * <p>The source-side damage interception follows the integration pattern used by Stephen-Seo's
 * TurnBasedMinecraftMod {@code AttackEventHandler} (MIT, neoforge commit
 * {@code 4d685cb187f91b2573a469d09fc47df270b90a4e}). TURNBOUND keeps its own deterministic battle rules;
 * only the Minecraft-world combat interception boundary is adapted.</p>
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
        bus.addListener(this::onPlayerAttackEntity);
        bus.addListener(this::onIncomingDamage);
        bus.addListener(this::onKnockBack);
        bus.addListener(this::onEntityTickPre);
        bus.addListener(this::onEntityLeaveLevel);
    }

    private void onPlayerAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!isolation.allowWorldAttackFrom(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    private void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity target = event.getEntity();
        if (target.level().isClientSide()) return;

        Entity source = event.getSource().getEntity();
        UUID sourceId = source == null ? null : source.getUUID();
        if (!isolation.allowWorldDamage(target.getUUID(), sourceId)) {
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
        cleanupBattleForEntity(entityId);
    }

    private void cleanupBattleForEntity(UUID entityId) {
        battles.battleForEntity(entityId).ifPresent(battle -> {
            // Preserve the canonical terminal cleanup when possible, but never require terminal state
            // to release Minecraft entity ownership during removal/disconnect/dimension transitions.
            if (battle.state() == BattleState.REWARD) battle.cleanup();
            battles.cleanup(battle.battleId());
        });
    }
}
