package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.battle.BattleEvent;
import kr.moonseungjun.turnboundre.network.BattleNetworkPayloads;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Presentation-only action timeline reconstructed from authoritative server battle events.
 * It never predicts damage, targeting or turn order; it only spaces already-published actions into readable beats.
 */
public final class BattleActionTimelineState {
    static final long WINDUP_NANOS = 130_000_000L;
    static final long IMPACT_NANOS = 150_000_000L;
    static final long RECOVERY_NANOS = 170_000_000L;
    static final long BEAT_NANOS = WINDUP_NANOS + IMPACT_NANOS + RECOVERY_NANOS;
    private static final int MAX_BEATS = 12;

    public enum Phase { WINDUP, IMPACT, RECOVERY }
    public enum MotionStyle { CLOSE, RANGED, CAST, UTILITY }
    public enum ImpactStyle { NONE, MELEE, PROJECTILE, FIRE, BLAST, ARCANE, VOID }
    public enum PresentationStyle { STANDARD, HEAVY, VOLLEY, AREA, RITUAL, RIFT, SLAM }

    public record Cue(
            String actorId,
            String actionId,
            List<String> targetIds,
            MotionStyle motionStyle,
            ImpactStyle impactStyle,
            PresentationStyle presentationStyle,
            Phase phase,
            double phaseProgress,
            int beatIndex,
            int beatCount
    ) {
        public Cue {
            if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("actorId required");
            if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("actionId required");
            targetIds = targetIds == null ? List.of() : List.copyOf(targetIds);
            if (motionStyle == null) motionStyle = MotionStyle.UTILITY;
            if (impactStyle == null) impactStyle = ImpactStyle.NONE;
            if (presentationStyle == null) presentationStyle = PresentationStyle.STANDARD;
            if (phase == null) throw new IllegalArgumentException("phase required");
            if (!Double.isFinite(phaseProgress)) phaseProgress = 0.0D;
            phaseProgress = Math.max(0.0D, Math.min(1.0D, phaseProgress));
            if (beatIndex < 0 || beatCount <= 0 || beatIndex >= beatCount) {
                throw new IllegalArgumentException("invalid beat index/count");
            }
        }

        /** Compatibility constructor for callers that already provide an impact family. */
        public Cue(
                String actorId,
                String actionId,
                List<String> targetIds,
                MotionStyle motionStyle,
                ImpactStyle impactStyle,
                Phase phase,
                double phaseProgress,
                int beatIndex,
                int beatCount
        ) {
            this(actorId, actionId, targetIds, motionStyle, impactStyle, PresentationStyle.STANDARD,
                    phase, phaseProgress, beatIndex, beatCount);
        }

        /** Compatibility constructor for presentation callers that only care about motion. */
        public Cue(
                String actorId,
                String actionId,
                List<String> targetIds,
                MotionStyle motionStyle,
                Phase phase,
                double phaseProgress,
                int beatIndex,
                int beatCount
        ) {
            this(actorId, actionId, targetIds, motionStyle, ImpactStyle.NONE, PresentationStyle.STANDARD,
                    phase, phaseProgress, beatIndex, beatCount);
        }
    }

    private record Beat(
            String actorId,
            String actionId,
            List<String> targetIds,
            MotionStyle motionStyle,
            ImpactStyle impactStyle,
            PresentationStyle presentationStyle
    ) {}

    private static final Object LOCK = new Object();
    private static UUID battleId;
    private static long resultingRevision = Long.MIN_VALUE;
    private static int fromIndex = Integer.MIN_VALUE;
    private static long startedNanos;
    private static List<Beat> beats = List.of();

    private BattleActionTimelineState() {}

    public static void acceptEvents(BattleNetworkPayloads.DecodedEvents decoded) {
        acceptEvents(decoded, System.nanoTime());
    }

    static void acceptEvents(BattleNetworkPayloads.DecodedEvents decoded, long nowNanos) {
        if (decoded == null) return;
        List<Beat> parsed = parseBeats(decoded.events());
        if (parsed.isEmpty()) return;

        synchronized (LOCK) {
            boolean sameBattle = decoded.battleId().equals(battleId);
            if (sameBattle && decoded.resultingRevision() < resultingRevision) return;
            if (sameBattle && decoded.resultingRevision() == resultingRevision && decoded.fromIndex() <= fromIndex) return;

            battleId = decoded.battleId();
            resultingRevision = decoded.resultingRevision();
            fromIndex = decoded.fromIndex();
            startedNanos = nowNanos;
            beats = parsed.size() <= MAX_BEATS ? List.copyOf(parsed) : List.copyOf(parsed.subList(0, MAX_BEATS));
        }
    }

    public static Optional<Cue> cue(UUID expectedBattleId) {
        return cue(expectedBattleId, System.nanoTime());
    }

    static Optional<Cue> cue(UUID expectedBattleId, long nowNanos) {
        if (expectedBattleId == null) return Optional.empty();
        synchronized (LOCK) {
            if (!expectedBattleId.equals(battleId) || beats.isEmpty()) return Optional.empty();
            long elapsed = Math.max(0L, nowNanos - startedNanos);
            long total = safeMultiply(BEAT_NANOS, beats.size());
            if (elapsed >= total) {
                beats = List.of();
                return Optional.empty();
            }

            int beatIndex = (int) Math.min(beats.size() - 1L, elapsed / BEAT_NANOS);
            long within = elapsed - safeMultiply(BEAT_NANOS, beatIndex);
            Beat beat = beats.get(beatIndex);
            Phase phase;
            double progress;
            if (within < WINDUP_NANOS) {
                phase = Phase.WINDUP;
                progress = within / (double) WINDUP_NANOS;
            } else if (within < WINDUP_NANOS + IMPACT_NANOS) {
                phase = Phase.IMPACT;
                progress = (within - WINDUP_NANOS) / (double) IMPACT_NANOS;
            } else {
                phase = Phase.RECOVERY;
                progress = (within - WINDUP_NANOS - IMPACT_NANOS) / (double) RECOVERY_NANOS;
            }
            return Optional.of(new Cue(
                    beat.actorId(), beat.actionId(), beat.targetIds(), beat.motionStyle(), beat.impactStyle(),
                    beat.presentationStyle(), phase, progress, beatIndex, beats.size()));
        }
    }

    public static boolean isPlaying(UUID expectedBattleId) {
        return cue(expectedBattleId).isPresent();
    }

    static boolean isPlaying(UUID expectedBattleId, long nowNanos) {
        return cue(expectedBattleId, nowNanos).isPresent();
    }

    /** Current authoritative event revision backing the active timeline, or Long.MIN_VALUE when unavailable. */
    public static long timelineRevision(UUID expectedBattleId) {
        if (expectedBattleId == null) return Long.MIN_VALUE;
        synchronized (LOCK) {
            return expectedBattleId.equals(battleId) ? resultingRevision : Long.MIN_VALUE;
        }
    }

    /** Remaining delay before this participant's first authoritative impact beat; zero if no direct target event names it. */
    public static long firstImpactDelayNanos(UUID expectedBattleId, String participantId) {
        return firstImpactDelayNanos(expectedBattleId, participantId, System.nanoTime());
    }

    static long firstImpactDelayNanos(UUID expectedBattleId, String participantId, long nowNanos) {
        if (expectedBattleId == null || participantId == null || participantId.isBlank()) return 0L;
        synchronized (LOCK) {
            if (!expectedBattleId.equals(battleId) || beats.isEmpty()) return 0L;
            for (int index = 0; index < beats.size(); index++) {
                if (beats.get(index).targetIds().contains(participantId)) {
                    long impactOffset = safeAdd(safeMultiply(BEAT_NANOS, index), WINDUP_NANOS);
                    long impactAt = safeAdd(startedNanos, impactOffset);
                    return Math.max(0L, impactAt - nowNanos);
                }
            }
            return 0L;
        }
    }

    public static void beginBattle(UUID nextBattleId) {
        synchronized (LOCK) {
            if (nextBattleId != null && nextBattleId.equals(battleId)) return;
            battleId = nextBattleId;
            resultingRevision = Long.MIN_VALUE;
            fromIndex = Integer.MIN_VALUE;
            startedNanos = 0L;
            beats = List.of();
        }
    }

    public static void clear() {
        beginBattle(null);
    }

    private static List<Beat> parseBeats(List<BattleEvent> events) {
        if (events == null || events.isEmpty()) return List.of();
        List<Beat> out = new ArrayList<>();
        MutableBeat current = null;
        for (BattleEvent event : events) {
            if (event == null) continue;
            if ("COMMAND_ACCEPTED".equals(event.type()) || "AI_COMMAND".equals(event.type())) {
                if (current != null) addBeat(out, current);
                current = event.actorId() == null || event.actorId().isBlank()
                        || event.detail() == null || event.detail().isBlank()
                        ? null
                        : new MutableBeat(event.actorId(), event.detail());
                continue;
            }
            if (current == null) continue;
            if ("ACTION_PRESENTATION".equals(event.type()) && current.actorId.equals(event.actorId())) {
                String action = detailValue(event.detail(), "action");
                if (action.isBlank() || current.actionId.equals(action)) {
                    String kind = detailValue(event.detail(), "kind");
                    String tag = detailValue(event.detail(), "tag");
                    String team = detailValue(event.detail(), "team");
                    String shape = detailValue(event.detail(), "shape");
                    int count = positiveInt(detailValue(event.detail(), "count"));
                    current.motionStyle = motionStyle(kind, tag);
                    current.impactStyle = impactStyle(tag);
                    current.presentationStyle = presentationStyle(kind, tag, team, shape, count);
                    for (String target : splitTargets(detailValue(event.detail(), "targets"))) {
                        current.targetIds.add(target);
                    }
                }
                continue;
            }
            String target = targetId(event);
            if (!target.isBlank()) current.targetIds.add(target);
        }
        if (current != null) addBeat(out, current);
        return List.copyOf(out);
    }

    private static void addBeat(List<Beat> out, MutableBeat source) {
        if (out.size() >= MAX_BEATS) return;
        out.add(new Beat(
                source.actorId,
                source.actionId,
                List.copyOf(source.targetIds),
                source.motionStyle,
                source.impactStyle,
                source.presentationStyle));
    }

    private static MotionStyle motionStyle(String kind, String tag) {
        return switch (tag) {
            case "MELEE" -> MotionStyle.CLOSE;
            case "PROJECTILE" -> MotionStyle.RANGED;
            case "FIRE", "BLAST", "ARCANE", "VOID" -> MotionStyle.CAST;
            default -> "GUARD".equals(kind) ? MotionStyle.UTILITY : MotionStyle.UTILITY;
        };
    }

    private static ImpactStyle impactStyle(String tag) {
        return switch (tag) {
            case "MELEE" -> ImpactStyle.MELEE;
            case "PROJECTILE" -> ImpactStyle.PROJECTILE;
            case "FIRE" -> ImpactStyle.FIRE;
            case "BLAST" -> ImpactStyle.BLAST;
            case "ARCANE" -> ImpactStyle.ARCANE;
            case "VOID" -> ImpactStyle.VOID;
            default -> ImpactStyle.NONE;
        };
    }

    private static PresentationStyle presentationStyle(
            String kind,
            String tag,
            String team,
            String shape,
            int count
    ) {
        boolean multi = "MULTI".equals(shape) || count > 1;
        if ("BURST".equals(kind)) {
            if ("VOID".equals(tag)) return PresentationStyle.RIFT;
            if ("ARCANE".equals(tag) && "ALLY".equals(team)) return PresentationStyle.RITUAL;
            if (("PROJECTILE".equals(tag) || "FIRE".equals(tag)) && multi) return PresentationStyle.VOLLEY;
            if ("BLAST".equals(tag)) return PresentationStyle.AREA;
            if ("MELEE".equals(tag)) return multi ? PresentationStyle.SLAM : PresentationStyle.HEAVY;
        }
        if (("PROJECTILE".equals(tag) || "FIRE".equals(tag)) && multi) return PresentationStyle.VOLLEY;
        if ("BLAST".equals(tag) && multi) return PresentationStyle.AREA;
        if ("MELEE".equals(tag) && multi) return PresentationStyle.SLAM;
        if ("ARCANE".equals(tag) && "ALLY".equals(team) && multi) return PresentationStyle.RITUAL;
        return PresentationStyle.STANDARD;
    }

    private static String targetId(BattleEvent event) {
        return detailValue(event.detail(), "target");
    }

    private static String detailValue(String detail, String key) {
        if (detail == null || detail.isBlank() || key == null || key.isBlank()) return "";
        String prefix = key + "=";
        for (String token : detail.split("\\s+")) {
            if (token.startsWith(prefix) && token.length() > prefix.length()) {
                return token.substring(prefix.length());
            }
        }
        return "";
    }

    private static int positiveInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static List<String> splitTargets(String packed) {
        if (packed == null || packed.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String value : packed.split(",")) {
            if (value != null && !value.isBlank()) out.add(value);
        }
        return List.copyOf(out);
    }

    private static long safeAdd(long value, long delta) {
        if (delta > 0L && value > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        return value + delta;
    }

    private static long safeMultiply(long value, long multiplier) {
        if (value <= 0L || multiplier <= 0L) return 0L;
        if (value > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE;
        return value * multiplier;
    }

    private static final class MutableBeat {
        private final String actorId;
        private final String actionId;
        private final Set<String> targetIds = new LinkedHashSet<>();
        private MotionStyle motionStyle = MotionStyle.UTILITY;
        private ImpactStyle impactStyle = ImpactStyle.NONE;
        private PresentationStyle presentationStyle = PresentationStyle.STANDARD;

        private MutableBeat(String actorId, String actionId) {
            this.actorId = actorId;
            this.actionId = actionId;
        }
    }
}
