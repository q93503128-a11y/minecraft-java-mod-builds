package kr.moonseungjun.riftfrontier.expedition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Minecraft/DFU serialization adapter for the pure ExpeditionRun domain record. */
public final class ExpeditionRunCodec {
    private static final Codec<ContentId> CONTENT_ID_CODEC = Codec.STRING.xmap(ContentId::parse, ContentId::toString);
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<ExpeditionRun.Status> STATUS_CODEC = Codec.STRING.xmap(ExpeditionRun.Status::parse, ExpeditionRun.Status::serializedName);
    private static final Codec<ExpeditionRun.EndReason> END_REASON_CODEC = Codec.STRING.xmap(ExpeditionRun.EndReason::parse, ExpeditionRun.EndReason::serializedName);
    private static final Codec<ExpeditionStartContext> START_CONTEXT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("region_pressure").forGetter(ExpeditionStartContext::regionPressure),
        Codec.INT.fieldOf("preparation_supply_cost").forGetter(ExpeditionStartContext::preparationSupplyCost),
        Codec.INT.fieldOf("planned_hunters").forGetter(ExpeditionStartContext::plannedHunters),
        Codec.INT.fieldOf("planned_scouts").forGetter(ExpeditionStartContext::plannedScouts),
        Codec.INT.fieldOf("planned_elites").forGetter(ExpeditionStartContext::plannedElites),
        Codec.INT.fieldOf("hazard_ticks").forGetter(ExpeditionStartContext::hazardTicks),
        Codec.INT.fieldOf("hazard_amplifier").forGetter(ExpeditionStartContext::hazardAmplifier)
    ).apply(instance, ExpeditionStartContext::new));
    private static final Codec<ExpeditionEvidenceCheckpoint.Stage> EVIDENCE_STAGE_CODEC = Codec.STRING.xmap(
        ExpeditionEvidenceCheckpoint.Stage::parse,
        ExpeditionEvidenceCheckpoint.Stage::serializedName
    );
    private static final Codec<ExpeditionEvidenceCheckpoint> EVIDENCE_CHECKPOINT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        EVIDENCE_STAGE_CODEC.fieldOf("stage").forGetter(ExpeditionEvidenceCheckpoint::stage),
        Codec.LONG.fieldOf("game_time").forGetter(ExpeditionEvidenceCheckpoint::gameTime),
        Codec.INT.fieldOf("recovered_salvage").forGetter(ExpeditionEvidenceCheckpoint::recoveredSalvage),
        Codec.INT.fieldOf("live_threats").forGetter(ExpeditionEvidenceCheckpoint::liveThreats),
        Codec.INT.fieldOf("hub_salvage").forGetter(ExpeditionEvidenceCheckpoint::hubSalvage),
        Codec.INT.fieldOf("expedition_supply").forGetter(ExpeditionEvidenceCheckpoint::expeditionSupply),
        Codec.INT.fieldOf("region_pressure").forGetter(ExpeditionEvidenceCheckpoint::regionPressure)
    ).apply(instance, ExpeditionEvidenceCheckpoint::new));

    public static final Codec<ExpeditionRun> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("sequence").forGetter(ExpeditionRun::sequence),
        CONTENT_ID_CODEC.fieldOf("region").forGetter(ExpeditionRun::regionId),
        CONTENT_ID_CODEC.fieldOf("contract").forGetter(ExpeditionRun::contractId),
        UUID_CODEC.optionalFieldOf("owner_uuid").forGetter(ExpeditionRun::ownerId),
        Codec.STRING.fieldOf("content_fingerprint").forGetter(ExpeditionRun::contentFingerprint),
        START_CONTEXT_CODEC.optionalFieldOf("start_context").forGetter(ExpeditionRun::startContext),
        EVIDENCE_CHECKPOINT_CODEC.listOf().optionalFieldOf("field_evidence", List.of()).forGetter(ExpeditionRun::evidenceTrail),
        STATUS_CODEC.fieldOf("status").forGetter(ExpeditionRun::status),
        Codec.unboundedMap(CONTENT_ID_CODEC, Codec.INT).optionalFieldOf("recovered_resources", Map.of()).forGetter(ExpeditionRun::recoveredResources),
        Codec.LONG.fieldOf("started_game_time").forGetter(ExpeditionRun::startedGameTime),
        Codec.LONG.optionalFieldOf("ended_game_time", -1L).forGetter(ExpeditionRun::endedGameTime),
        END_REASON_CODEC.optionalFieldOf("end_reason", ExpeditionRun.EndReason.NONE).forGetter(ExpeditionRun::endReason)
    ).apply(instance, ExpeditionRun::new));

    private ExpeditionRunCodec() {}
}
