package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Persisted time debt for one outpost. These numbers are never resource authority: they only allow
 * later loaded physical work to catch up after the outpost/route spent time unloaded.
 */
public record OutpostDeferredState(int outpostId,
                                   long productionTicks,
                                   long logisticsTicks,
                                   String observedOverlay,
                                   boolean transportObserved) {
    public static final Codec<OutpostDeferredState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("outpost_id").forGetter(OutpostDeferredState::outpostId),
            Codec.LONG.optionalFieldOf("production_ticks", 0L).forGetter(OutpostDeferredState::productionTicks),
            Codec.LONG.optionalFieldOf("logistics_ticks", 0L).forGetter(OutpostDeferredState::logisticsTicks),
            Codec.STRING.optionalFieldOf("observed_overlay", "general").forGetter(OutpostDeferredState::observedOverlay),
            Codec.BOOL.optionalFieldOf("transport_observed", false).forGetter(OutpostDeferredState::transportObserved)
    ).apply(instance, OutpostDeferredState::new));

    public OutpostDeferredState {
        productionTicks = Math.max(0L, productionTicks);
        logisticsTicks = Math.max(0L, logisticsTicks);
        observedOverlay = observedOverlay == null || observedOverlay.isBlank() ? "general" : observedOverlay;
    }

    public static OutpostDeferredState empty(int outpostId) {
        return new OutpostDeferredState(outpostId, 0L, 0L, "general", false);
    }

    public OutpostDeferredState withProductionTicks(long ticks) {
        return new OutpostDeferredState(outpostId, ticks, logisticsTicks, observedOverlay, transportObserved);
    }

    public OutpostDeferredState withLogisticsTicks(long ticks) {
        return new OutpostDeferredState(outpostId, productionTicks, ticks, observedOverlay, transportObserved);
    }

    public OutpostDeferredState withObservedOverlay(String overlay) {
        return new OutpostDeferredState(outpostId, productionTicks, logisticsTicks, overlay, transportObserved);
    }

    public OutpostDeferredState withTransportObserved(boolean observed) {
        return new OutpostDeferredState(outpostId, productionTicks, logisticsTicks, observedOverlay, observed);
    }
}
