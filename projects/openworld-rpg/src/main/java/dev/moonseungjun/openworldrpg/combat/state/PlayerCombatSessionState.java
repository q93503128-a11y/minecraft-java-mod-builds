package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Objects;

/**
 * Persistent reconnect snapshot for project-owned Mana, Stamina and skill cooldown authority.
 *
 * <p>Short-lived input/action windows are intentionally not serialized. Reconnecting cancels the
 * in-flight action, but it never refunds already spent resources or clears committed cooldowns.</p>
 */
public record PlayerCombatSessionState(
        int schemaVersion,
        boolean captured,
        double mana,
        double stamina,
        long savedAtTick,
        long lastManaSpendTick,
        long lastCombatActivityTick,
        long lastHostileHpActivityTick,
        long staminaRegenBlockedUntilTick,
        Map<String, Long> cooldownEndTicks
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<PlayerCombatSessionState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("combat_session_schema_version")
                            .forGetter(PlayerCombatSessionState::schemaVersion),
                    Codec.BOOL.fieldOf("captured")
                            .forGetter(PlayerCombatSessionState::captured),
                    Codec.DOUBLE.fieldOf("mana")
                            .forGetter(PlayerCombatSessionState::mana),
                    Codec.DOUBLE.fieldOf("stamina")
                            .forGetter(PlayerCombatSessionState::stamina),
                    Codec.LONG.fieldOf("saved_at_tick")
                            .forGetter(PlayerCombatSessionState::savedAtTick),
                    Codec.LONG.fieldOf("last_mana_spend_tick")
                            .forGetter(PlayerCombatSessionState::lastManaSpendTick),
                    Codec.LONG.fieldOf("last_combat_activity_tick")
                            .forGetter(PlayerCombatSessionState::lastCombatActivityTick),
                    Codec.LONG.fieldOf("last_hostile_hp_activity_tick")
                            .forGetter(PlayerCombatSessionState::lastHostileHpActivityTick),
                    Codec.LONG.fieldOf("stamina_regen_blocked_until_tick")
                            .forGetter(PlayerCombatSessionState::staminaRegenBlockedUntilTick),
                    Codec.unboundedMap(Codec.STRING, Codec.LONG)
                            .fieldOf("cooldown_end_ticks")
                            .forGetter(PlayerCombatSessionState::cooldownEndTicks)
            ).apply(instance, PlayerCombatSessionState::new));

    public PlayerCombatSessionState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported combat-session schema version: " + schemaVersion
            );
        }
        if (!Double.isFinite(mana) || mana < 0.0
                || !Double.isFinite(stamina) || stamina < 0.0) {
            throw new IllegalArgumentException(
                    "Persisted combat resources must be finite and non-negative."
            );
        }
        if (savedAtTick < 0L) {
            throw new IllegalArgumentException("savedAtTick must be non-negative.");
        }
        cooldownEndTicks = Map.copyOf(
                Objects.requireNonNull(cooldownEndTicks, "cooldownEndTicks")
        );
        cooldownEndTicks.forEach((id, endTick) -> {
            if (id == null
                    || id.isBlank()
                    || id.indexOf(':') <= 0
                    || id.indexOf(' ') >= 0
                    || endTick == null
                    || endTick < 0L) {
                throw new IllegalArgumentException(
                        "Persisted cooldowns require stable IDs and non-negative end ticks."
                );
            }
        });
    }

    public PlayerCombatSessionState(
            int schemaVersion,
            double mana,
            double stamina,
            long savedAtTick,
            long lastManaSpendTick,
            long lastCombatActivityTick,
            long lastHostileHpActivityTick,
            long staminaRegenBlockedUntilTick,
            Map<String, Long> cooldownEndTicks
    ) {
        this(
                schemaVersion,
                true,
                mana,
                stamina,
                savedAtTick,
                lastManaSpendTick,
                lastCombatActivityTick,
                lastHostileHpActivityTick,
                staminaRegenBlockedUntilTick,
                cooldownEndTicks
        );
    }

    public static PlayerCombatSessionState empty() {
        return new PlayerCombatSessionState(
                CURRENT_SCHEMA_VERSION,
                false,
                0.0,
                0.0,
                0L,
                Long.MIN_VALUE / 4,
                Long.MIN_VALUE / 4,
                Long.MIN_VALUE / 4,
                Long.MIN_VALUE / 4,
                Map.of()
        );
    }
}
