package kr.moonseungjun.riftfrontier.expedition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.riftfrontier.content.ContentId;

import java.util.Map;

/** Minecraft/DFU serialization adapter for the pure ExpeditionRun domain record. */
public final class ExpeditionRunCodec {
    private static final Codec<ContentId> CONTENT_ID_CODEC = Codec.STRING.xmap(ContentId::parse, ContentId::toString);
    private static final Codec<ExpeditionRun.Status> STATUS_CODEC = Codec.STRING.xmap(
        ExpeditionRun.Status::parse,
        ExpeditionRun.Status::serializedName
    );
    private static final Codec<ExpeditionRun.EndReason> END_REASON_CODEC = Codec.STRING.xmap(
        ExpeditionRun.EndReason::parse,
        ExpeditionRun.EndReason::serializedName
    );

    public static final Codec<ExpeditionRun> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("sequence").forGetter(ExpeditionRun::sequence),
        CONTENT_ID_CODEC.fieldOf("region").forGetter(ExpeditionRun::regionId),
        CONTENT_ID_CODEC.fieldOf("contract").forGetter(ExpeditionRun::contractId),
        Codec.STRING.fieldOf("content_fingerprint").forGetter(ExpeditionRun::contentFingerprint),
        STATUS_CODEC.fieldOf("status").forGetter(ExpeditionRun::status),
        Codec.unboundedMap(CONTENT_ID_CODEC, Codec.INT)
            .optionalFieldOf("recovered_resources", Map.of())
            .forGetter(ExpeditionRun::recoveredResources),
        Codec.LONG.fieldOf("started_game_time").forGetter(ExpeditionRun::startedGameTime),
        Codec.LONG.optionalFieldOf("ended_game_time", -1L).forGetter(ExpeditionRun::endedGameTime),
        END_REASON_CODEC.optionalFieldOf("end_reason", ExpeditionRun.EndReason.NONE).forGetter(ExpeditionRun::endReason)
    ).apply(instance, ExpeditionRun::new));

    private ExpeditionRunCodec() {}
}
