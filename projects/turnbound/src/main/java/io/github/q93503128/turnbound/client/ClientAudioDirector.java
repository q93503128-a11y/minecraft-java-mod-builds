package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.AsterMarchRegionCatalog;
import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical v0.4 audio routing state.
 *
 * The design docs define six music roles but do not provide authored audio files. This class therefore owns
 * routing, cross-fade state and SFX concurrency budgets without inventing substitute tracks. A resource-backed
 * audio backend can consume musicMix()/drainAcceptedCues() without changing combat or world logic.
 */
public final class ClientAudioDirector {
    public enum MusicSlot { NONE, HUB, REGION_EXPLORE, BATTLE_NORMAL, BATTLE_ELITE, BATTLE_BOSS, BATTLE_FINAL }
    public enum CueGroup { IMPACT, REACTION, SUPPORT, SYSTEM, SKILL }
    public record Cue(String id, CueGroup group, int priority, String sourceId, String targetId, String detail, int value) {}
    public record MusicMix(MusicSlot outgoing, MusicSlot incoming, float outgoingGain, float incomingGain, boolean transitioning) {}

    private static final long CROSS_FADE_NANOS = 800_000_000L;
    private static final long CUE_WINDOW_NANOS = 70_000_000L;
    private static final int QUEUE_CAP = 24;
    private static final Map<CueGroup, Integer> GROUP_LIMITS = new EnumMap<>(CueGroup.class);
    private static final Map<CueGroup, Deque<Long>> RECENT = new EnumMap<>(CueGroup.class);
    private static final Deque<Cue> ACCEPTED = new ArrayDeque<>();

    static {
        GROUP_LIMITS.put(CueGroup.IMPACT, 3);
        GROUP_LIMITS.put(CueGroup.REACTION, 2);
        GROUP_LIMITS.put(CueGroup.SUPPORT, 2);
        GROUP_LIMITS.put(CueGroup.SYSTEM, 1);
        GROUP_LIMITS.put(CueGroup.SKILL, 2);
        for (CueGroup group : CueGroup.values()) RECENT.put(group, new ArrayDeque<>());
    }

    private static MusicSlot outgoing = MusicSlot.NONE;
    private static MusicSlot incoming = MusicSlot.NONE;
    private static long transitionStarted;

    private ClientAudioDirector() {}

    public static void onBattleSnapshot(ClientBattleState.Snapshot snapshot) {
        if (snapshot != null && snapshot.active()) requestMusic(classifyBattle(snapshot));
        else onFieldSnapshot(ClientFieldState.snapshot());
    }

    public static void onFieldSnapshot(FieldUiSnapshot snapshot) {
        if (ClientBattleState.snapshot().active()) return;
        if (snapshot == null || !snapshot.active()) { requestMusic(MusicSlot.NONE); return; }
        Minecraft minecraft = Minecraft.getInstance();
        boolean radia = minecraft.player != null && AsterMarchRegionCatalog.RADIA.contains(minecraft.player.getX(), minecraft.player.getZ());
        if (!radia) radia = snapshot.travels().stream().anyMatch(travel -> travel.current() && AsterMarchRegionCatalog.FT_RADIA.equals(travel.id()));
        requestMusic(radia ? MusicSlot.HUB : MusicSlot.REGION_EXPLORE);
    }

    public static MusicSlot desiredMusic() { return incoming; }

    public static MusicMix musicMix() {
        if (outgoing == incoming || transitionStarted == 0L) return new MusicMix(incoming, incoming, 0.0F, 1.0F, false);
        double t = Math.min(1.0, Math.max(0.0, (System.nanoTime() - transitionStarted) / (double) CROSS_FADE_NANOS));
        if (t >= 1.0) {
            outgoing = incoming;
            transitionStarted = 0L;
            return new MusicMix(incoming, incoming, 0.0F, 1.0F, false);
        }
        return new MusicMix(outgoing, incoming, (float)(1.0 - t), (float)t, true);
    }

    public static void acceptBatch(String encoded) {
        if (encoded == null || encoded.isBlank()) return;
        for (String line : encoded.split("\\n")) {
            Cue cue = decode(line);
            if (cue != null) accept(cue);
        }
    }

    /** Drained by the future resource-backed audio backend. Routing/budgeting is already live. */
    public static List<Cue> drainAcceptedCues() {
        List<Cue> out = new ArrayList<>(ACCEPTED);
        ACCEPTED.clear();
        return List.copyOf(out);
    }

    public static String debugState() {
        MusicMix mix = musicMix();
        return "music=" + mix.incoming() + (mix.transitioning() ? "<-" + mix.outgoing() : "") + " cues=" + ACCEPTED.size();
    }

    private static MusicSlot classifyBattle(ClientBattleState.Snapshot snapshot) {
        boolean boss = false, elite = false;
        for (ClientBattleState.Unit unit : snapshot.units()) {
            String id = unit.defId();
            if ("B05".equals(id)) return MusicSlot.BATTLE_FINAL;
            boss |= id != null && id.matches("B0[1-4]");
            elite |= id != null && id.startsWith("EL");
        }
        if (boss) return MusicSlot.BATTLE_BOSS;
        if (elite) return MusicSlot.BATTLE_ELITE;
        return MusicSlot.BATTLE_NORMAL;
    }

    private static void requestMusic(MusicSlot slot) {
        MusicSlot requested = slot == null ? MusicSlot.NONE : slot;
        if (requested == incoming) return;
        MusicMix current = musicMix();
        outgoing = current.incomingGain() >= current.outgoingGain() ? current.incoming() : current.outgoing();
        incoming = requested;
        transitionStarted = System.nanoTime();
    }

    private static void accept(Cue cue) {
        long now = System.nanoTime();
        Deque<Long> recent = RECENT.get(cue.group());
        while (!recent.isEmpty() && now - recent.peekFirst() > CUE_WINDOW_NANOS) recent.removeFirst();
        int limit = GROUP_LIMITS.getOrDefault(cue.group(), 1);
        if (recent.size() >= limit && cue.priority() < 3) return;
        if (recent.size() >= limit) recent.removeFirst();
        recent.addLast(now);
        if (ACCEPTED.size() >= QUEUE_CAP) ACCEPTED.removeFirst();
        ACCEPTED.addLast(cue);
    }

    private static Cue decode(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 7) return null;
        try {
            return new Cue(p[0], CueGroup.valueOf(p[1]), Integer.parseInt(p[2]), p[3], p[4], p[5], Integer.parseInt(p[6]));
        } catch (RuntimeException ignored) { return null; }
    }
}
