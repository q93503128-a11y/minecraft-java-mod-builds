package dev.moonseungjun.openworldrpg.camp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.moonseungjun.openworldrpg.gathering.R01GatheringRules;
import java.util.Objects;
import java.util.Optional;

public record R01CampPlayerState(
        int schemaVersion,
        boolean hardwoodEncountered,
        boolean toughHideEncountered,
        boolean kitUnlocked,
        long nextCraftSerial,
        Optional<PendingKitCraft> pendingKitCraft
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<R01CampPlayerState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.intRange(1, CURRENT_SCHEMA_VERSION)
                            .fieldOf("r01_camp_schema_version")
                            .forGetter(R01CampPlayerState::schemaVersion),
                    Codec.BOOL.optionalFieldOf("hardwood_encountered", false)
                            .forGetter(R01CampPlayerState::hardwoodEncountered),
                    Codec.BOOL.optionalFieldOf("tough_hide_encountered", false)
                            .forGetter(R01CampPlayerState::toughHideEncountered),
                    Codec.BOOL.optionalFieldOf("kit_unlocked", false)
                            .forGetter(R01CampPlayerState::kitUnlocked),
                    Codec.LONG.optionalFieldOf("next_craft_serial", 0L)
                            .forGetter(R01CampPlayerState::nextCraftSerial),
                    PendingKitCraft.CODEC.optionalFieldOf("pending_kit_craft")
                            .forGetter(R01CampPlayerState::pendingKitCraft)
            ).apply(instance, R01CampPlayerState::new));

    public R01CampPlayerState {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported R01 Camp player schema.");
        }
        if (nextCraftSerial < 0L) {
            throw new IllegalArgumentException("Camp craft serial must be non-negative.");
        }
        pendingKitCraft = Objects.requireNonNull(pendingKitCraft, "pendingKitCraft");
    }

    public static R01CampPlayerState initial() {
        return new R01CampPlayerState(
                CURRENT_SCHEMA_VERSION, false, false, false, 0L, Optional.empty()
        );
    }

    public boolean recipeKnown() {
        return hardwoodEncountered && toughHideEncountered;
    }

    public R01CampPlayerState recordMaterialEncounter(String materialId) {
        Objects.requireNonNull(materialId, "materialId");
        boolean hardwood = hardwoodEncountered
                || R01GatheringRules.HARDWOOD.equals(materialId);
        boolean hide = toughHideEncountered
                || R01CampRules.TOUGH_HIDE.equals(materialId);
        if (hardwood == hardwoodEncountered && hide == toughHideEncountered) {
            return this;
        }
        return new R01CampPlayerState(
                schemaVersion, hardwood, hide, kitUnlocked,
                nextCraftSerial, pendingKitCraft
        );
    }

    public BeginCraftResult beginKitCraft(String playerUuid) {
        java.util.UUID.fromString(playerUuid);
        if (!recipeKnown()) {
            throw new IllegalStateException("Field Camp Kit recipe is not known.");
        }
        if (kitUnlocked) {
            throw new IllegalStateException("Field Camp Kit is already unlocked.");
        }
        if (pendingKitCraft.isPresent()) {
            return new BeginCraftResult(this, pendingKitCraft.orElseThrow(), false);
        }

        String transactionId = "openworld_rpg:camp_kit/"
                + playerUuid + "/" + nextCraftSerial;
        PendingKitCraft pending = new PendingKitCraft(transactionId);
        return new BeginCraftResult(
                new R01CampPlayerState(
                        schemaVersion,
                        hardwoodEncountered,
                        toughHideEncountered,
                        false,
                        Math.addExact(nextCraftSerial, 1L),
                        Optional.of(pending)
                ),
                pending,
                true
        );
    }

    public R01CampPlayerState completeKitCraft(String transactionId) {
        requireStableId(transactionId);
        if (kitUnlocked && pendingKitCraft.isEmpty()) {
            return this;
        }
        PendingKitCraft pending = pendingKitCraft.orElseThrow(
                () -> new IllegalStateException("No pending Field Camp Kit craft.")
        );
        if (!pending.transactionId().equals(transactionId)) {
            throw new IllegalStateException("Field Camp Kit transaction mismatch.");
        }
        return new R01CampPlayerState(
                schemaVersion,
                hardwoodEncountered,
                toughHideEncountered,
                true,
                nextCraftSerial,
                Optional.empty()
        );
    }

    private static void requireStableId(String value) {
        if (value == null || value.isBlank()
                || value.indexOf(':') <= 0 || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Expected namespaced id.");
        }
    }

    public record PendingKitCraft(String transactionId) {
        public static final Codec<PendingKitCraft> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("transaction_id")
                                .forGetter(PendingKitCraft::transactionId)
                ).apply(instance, PendingKitCraft::new));

        public PendingKitCraft {
            requireStableId(transactionId);
        }
    }

    public record BeginCraftResult(
            R01CampPlayerState state,
            PendingKitCraft pending,
            boolean created
    ) {
        public BeginCraftResult {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(pending, "pending");
        }
    }
}
