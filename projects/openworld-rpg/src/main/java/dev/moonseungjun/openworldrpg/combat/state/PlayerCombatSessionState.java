package dev.moonseungjun.openworldrpg.combat.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Persistent reconnect snapshot for project-owned combat state that must not reset on relog.
 *
 * <p>Mana, Stamina, committed skill cooldowns, player poise, Shock/Conductive and authored
 * negative-status state survive reconnect. Short-lived input/action windows such as dodge i-frames,
 * held guard and accepted in-flight casts intentionally do not.</p>
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
        Map<String, Long> cooldownEndTicks,
        Optional<PoiseSnapshot> poise,
        Optional<ShockSnapshot> shock,
        Optional<NegativeStatusesSnapshot> negativeStatuses,
        Optional<UltimateSnapshot> ultimate
) {
    public static final int CURRENT_SCHEMA_VERSION = 3;

    private static final Codec<Set<String>> STRING_SET_CODEC =
            Codec.STRING.listOf().xmap(
                    Set::copyOf,
                    values -> values.stream().sorted().toList()
            );

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
                            .forGetter(PlayerCombatSessionState::cooldownEndTicks),
                    PoiseSnapshot.CODEC.optionalFieldOf("poise")
                            .forGetter(PlayerCombatSessionState::poise),
                    ShockSnapshot.CODEC.optionalFieldOf("shock")
                            .forGetter(PlayerCombatSessionState::shock),
                    NegativeStatusesSnapshot.CODEC.optionalFieldOf("negative_statuses")
                            .forGetter(PlayerCombatSessionState::negativeStatuses),
                    UltimateSnapshot.CODEC.optionalFieldOf("ultimate")
                            .forGetter(PlayerCombatSessionState::ultimate)
            ).apply(instance, PlayerCombatSessionState::new));

    public PlayerCombatSessionState {
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
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
            requireStableId(id, "cooldown action id");
            if (endTick == null || endTick < 0L) {
                throw new IllegalArgumentException(
                        "Persisted cooldown end ticks must be non-negative."
                );
            }
        });
        poise = Objects.requireNonNull(poise, "poise");
        shock = Objects.requireNonNull(shock, "shock");
        negativeStatuses = Objects.requireNonNull(
                negativeStatuses,
                "negativeStatuses"
        );
        ultimate = Objects.requireNonNull(ultimate, "ultimate");
    }

    /** Resource-only constructor retained for existing callers and legacy tests. */
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
                cooldownEndTicks,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public PlayerCombatSessionState withRuntimeSnapshots(
            Optional<PoiseSnapshot> poise,
            Optional<ShockSnapshot> shock,
            Optional<NegativeStatusesSnapshot> negativeStatuses
    ) {
        return new PlayerCombatSessionState(
                CURRENT_SCHEMA_VERSION,
                captured,
                mana,
                stamina,
                savedAtTick,
                lastManaSpendTick,
                lastCombatActivityTick,
                lastHostileHpActivityTick,
                staminaRegenBlockedUntilTick,
                cooldownEndTicks,
                Objects.requireNonNull(poise, "poise"),
                Objects.requireNonNull(shock, "shock"),
                Objects.requireNonNull(negativeStatuses, "negativeStatuses"),
                ultimate
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
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public record UltimateSnapshot(
            double charge,
            long lockoutUntilTick,
            double gainBudget,
            long gainBudgetRefreshTick
    ) {
        public static final Codec<UltimateSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("charge")
                                .forGetter(UltimateSnapshot::charge),
                        Codec.LONG.fieldOf("lockout_until_tick")
                                .forGetter(UltimateSnapshot::lockoutUntilTick),
                        Codec.DOUBLE.fieldOf("gain_budget")
                                .forGetter(UltimateSnapshot::gainBudget),
                        Codec.LONG.fieldOf("gain_budget_refresh_tick")
                                .forGetter(UltimateSnapshot::gainBudgetRefreshTick)
                ).apply(instance, UltimateSnapshot::new));

        public UltimateSnapshot {
            if (!Double.isFinite(charge)
                    || charge < 0.0
                    || charge > 100.0
                    || !Double.isFinite(gainBudget)
                    || gainBudget < 0.0
                    || gainBudget > 12.0
                    || gainBudgetRefreshTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid persisted Ultimate Gauge snapshot."
                );
            }
        }
    }

    public record PoiseSnapshot(
            double maxPoiseAtCapture,
            double currentPoise,
            long savedAtTick,
            long lastPressureTick,
            long immunityUntilTick
    ) {
        public static final Codec<PoiseSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("max_poise_at_capture")
                                .forGetter(PoiseSnapshot::maxPoiseAtCapture),
                        Codec.DOUBLE.fieldOf("current_poise")
                                .forGetter(PoiseSnapshot::currentPoise),
                        Codec.LONG.fieldOf("saved_at_tick")
                                .forGetter(PoiseSnapshot::savedAtTick),
                        Codec.LONG.fieldOf("last_pressure_tick")
                                .forGetter(PoiseSnapshot::lastPressureTick),
                        Codec.LONG.fieldOf("immunity_until_tick")
                                .forGetter(PoiseSnapshot::immunityUntilTick)
                ).apply(instance, PoiseSnapshot::new));

        public PoiseSnapshot {
            requirePositive("maxPoiseAtCapture", maxPoiseAtCapture);
            if (!Double.isFinite(currentPoise)
                    || currentPoise < 0.0
                    || currentPoise > maxPoiseAtCapture) {
                throw new IllegalArgumentException(
                        "currentPoise must remain inside the captured max."
                );
            }
            requireSavedTick(savedAtTick);
        }
    }

    public record ShockSnapshot(
            double thresholdAtCapture,
            double buildup,
            long savedAtTick,
            long lastBuildupTick,
            long conductiveUntilTick
    ) {
        public static final Codec<ShockSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.DOUBLE.fieldOf("threshold_at_capture")
                                .forGetter(ShockSnapshot::thresholdAtCapture),
                        Codec.DOUBLE.fieldOf("buildup")
                                .forGetter(ShockSnapshot::buildup),
                        Codec.LONG.fieldOf("saved_at_tick")
                                .forGetter(ShockSnapshot::savedAtTick),
                        Codec.LONG.fieldOf("last_buildup_tick")
                                .forGetter(ShockSnapshot::lastBuildupTick),
                        Codec.LONG.fieldOf("conductive_until_tick")
                                .forGetter(ShockSnapshot::conductiveUntilTick)
                ).apply(instance, ShockSnapshot::new));

        public ShockSnapshot {
            requirePositive("thresholdAtCapture", thresholdAtCapture);
            if (!Double.isFinite(buildup)
                    || buildup < 0.0
                    || buildup > thresholdAtCapture) {
                throw new IllegalArgumentException(
                        "Shock buildup must remain inside the captured threshold."
                );
            }
            requireSavedTick(savedAtTick);
        }
    }

    public record NegativeStatusesSnapshot(
            long savedAtTick,
            Map<String, ActiveNegativeStatusSnapshot> activeStatuses,
            long negativeBuildupResistanceUntilTick
    ) {
        public static final Codec<NegativeStatusesSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("saved_at_tick")
                                .forGetter(NegativeStatusesSnapshot::savedAtTick),
                        Codec.unboundedMap(
                                        Codec.STRING,
                                        ActiveNegativeStatusSnapshot.CODEC
                                )
                                .fieldOf("active_statuses")
                                .forGetter(NegativeStatusesSnapshot::activeStatuses),
                        Codec.LONG.fieldOf("negative_buildup_resistance_until_tick")
                                .forGetter(
                                        NegativeStatusesSnapshot
                                                ::negativeBuildupResistanceUntilTick
                                )
                ).apply(instance, NegativeStatusesSnapshot::new));

        public NegativeStatusesSnapshot {
            requireSavedTick(savedAtTick);
            activeStatuses = Map.copyOf(
                    Objects.requireNonNull(activeStatuses, "activeStatuses")
            );
            activeStatuses.forEach((id, value) -> {
                requireStableId(id, "negative status id");
                Objects.requireNonNull(value, "active negative status");
            });
        }
    }

    public record ActiveNegativeStatusSnapshot(
            Set<String> tags,
            long expiresAtTick
    ) {
        public static final Codec<ActiveNegativeStatusSnapshot> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        STRING_SET_CODEC.fieldOf("tags")
                                .forGetter(ActiveNegativeStatusSnapshot::tags),
                        Codec.LONG.fieldOf("expires_at_tick")
                                .forGetter(ActiveNegativeStatusSnapshot::expiresAtTick)
                ).apply(instance, ActiveNegativeStatusSnapshot::new));

        public ActiveNegativeStatusSnapshot {
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
            tags.forEach(PlayerCombatSessionState::requireStableTag);
            if (expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Negative-status expiry must be non-negative."
                );
            }
        }
    }

    private static void requireSavedTick(long tick) {
        if (tick < 0L) {
            throw new IllegalArgumentException("savedAtTick must be non-negative.");
        }
    }

    private static void requirePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and positive."
            );
        }
    }

    private static void requireStableId(String value, String name) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    name + " must be a stable namespaced id."
            );
        }
    }

    private static void requireStableTag(String value) {
        if (value == null || value.isBlank() || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected stable status tag.");
        }
    }
}
