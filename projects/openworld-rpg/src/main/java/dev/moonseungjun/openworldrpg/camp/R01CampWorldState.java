package dev.moonseungjun.openworldrpg.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.phys.Vec3;

public record R01CampWorldState(
        int schemaVersion,
        long nextGeneration,
        Map<String, ActiveCamp> activeCamps
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01CampWorldState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("r01_camp_world_schema_version")
                            .forGetter(R01CampWorldState::schemaVersion),
                    Codec.LONG.optionalFieldOf("next_generation", 1L)
                            .forGetter(R01CampWorldState::nextGeneration),
                    Codec.unboundedMap(Codec.STRING, ActiveCamp.CODEC)
                            .optionalFieldOf("active_camps", Map.of())
                            .forGetter(R01CampWorldState::activeCamps)
            ).apply(instance, R01CampWorldState::new));

    public R01CampWorldState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION || nextGeneration <= 0L) {
            throw new IllegalArgumentException("Invalid R01 Camp world schema/generation.");
        }
        activeCamps = Map.copyOf(Objects.requireNonNull(activeCamps, "activeCamps"));
        activeCamps.forEach((owner, camp) -> {
            requireUuid(owner);
            if (!owner.equals(Objects.requireNonNull(camp, "camp").ownerUuid())) {
                throw new IllegalArgumentException("Camp owner map mismatch.");
            }
        });
    }

    public static R01CampWorldState initial() {
        return new R01CampWorldState(CURRENT_SCHEMA_VERSION, 1L, Map.of());
    }

    public Optional<ActiveCamp> camp(String ownerUuid) {
        requireUuid(ownerUuid);
        return Optional.ofNullable(activeCamps.get(ownerUuid));
    }

    public DeployResult deploy(
            String ownerUuid,
            R01CampPlacementAuthority.Candidate candidate
    ) {
        requireUuid(ownerUuid);
        Objects.requireNonNull(candidate, "candidate");
        ActiveCamp previous = activeCamps.get(ownerUuid);
        ActiveCamp current = ActiveCamp.create(ownerUuid, nextGeneration, candidate);
        Map<String, ActiveCamp> next = new HashMap<>(activeCamps);
        next.put(ownerUuid, current);
        return new DeployResult(
                new R01CampWorldState(
                        schemaVersion,
                        Math.addExact(nextGeneration, 1L),
                        Map.copyOf(next)
                ),
                Optional.ofNullable(previous),
                current
        );
    }

    public record ActiveCamp(
            String campId,
            String ownerUuid,
            long generation,
            double x,
            double y,
            double z,
            double yawDegrees,
            Set<String> serviceFlags
    ) {
        private static final Codec<Set<String>> STRING_SET_CODEC =
                Codec.STRING.listOf().xmap(
                        Set::copyOf,
                        value -> value.stream().sorted().toList()
                );

        public static final Codec<ActiveCamp> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("camp_id").forGetter(ActiveCamp::campId),
                        Codec.STRING.fieldOf("owner_uuid").forGetter(ActiveCamp::ownerUuid),
                        Codec.LONG.fieldOf("generation").forGetter(ActiveCamp::generation),
                        Codec.DOUBLE.fieldOf("x").forGetter(ActiveCamp::x),
                        Codec.DOUBLE.fieldOf("y").forGetter(ActiveCamp::y),
                        Codec.DOUBLE.fieldOf("z").forGetter(ActiveCamp::z),
                        Codec.DOUBLE.fieldOf("yaw_degrees").forGetter(ActiveCamp::yawDegrees),
                        STRING_SET_CODEC.fieldOf("service_flags").forGetter(ActiveCamp::serviceFlags)
                ).apply(instance, ActiveCamp::new));

        public ActiveCamp {
            requireStableId(campId);
            requireUuid(ownerUuid);
            if (generation <= 0L
                    || !Double.isFinite(x) || !Double.isFinite(y)
                    || !Double.isFinite(z) || !Double.isFinite(yawDegrees)) {
                throw new IllegalArgumentException("Invalid active Camp state.");
            }
            serviceFlags = Set.copyOf(Objects.requireNonNull(serviceFlags, "serviceFlags"));
            if (!serviceFlags.equals(canonicalServiceFlags())) {
                throw new IllegalArgumentException("R01 Camp service flags must remain canonical.");
            }
        }

        static ActiveCamp create(
                String ownerUuid,
                long generation,
                R01CampPlacementAuthority.Candidate candidate
        ) {
            return new ActiveCamp(
                    "openworld_rpg:field_camp/" + ownerUuid,
                    ownerUuid,
                    generation,
                    candidate.anchor().x(),
                    candidate.anchor().y(),
                    candidate.anchor().z(),
                    candidate.yawDegrees(),
                    canonicalServiceFlags()
            );
        }

        public Vec3 anchor() {
            return new Vec3(x, y, z);
        }

        public static Set<String> canonicalServiceFlags() {
            return Set.of(
                    R01CampRules.REST_SERVICE,
                    R01CampRules.COOKING_SERVICE,
                    R01CampRules.MANAGEMENT_SERVICE
            );
        }
    }

    public record DeployResult(
            R01CampWorldState state,
            Optional<ActiveCamp> previous,
            ActiveCamp current
    ) {
        public DeployResult {
            Objects.requireNonNull(state, "state");
            previous = Objects.requireNonNull(previous, "previous");
            Objects.requireNonNull(current, "current");
        }
    }

    private static void requireUuid(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Camp owner UUID must be present.");
        }
        java.util.UUID.fromString(value);
    }

    private static void requireStableId(String value) {
        if (value == null || value.isBlank()
                || value.indexOf(':') <= 0 || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected namespaced Camp id.");
        }
    }
}
