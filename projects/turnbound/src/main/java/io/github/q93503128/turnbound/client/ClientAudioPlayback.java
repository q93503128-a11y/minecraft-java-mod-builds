package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.TurnboundSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Resource-backed playback backend for the semantic routing owned by {@link ClientAudioDirector}. */
public final class ClientAudioPlayback {
    private static final float BASE_MUSIC_GAIN = 0.68F;
    private static final Map<ClientAudioDirector.MusicSlot, Supplier<SoundEvent>> MUSIC = new EnumMap<>(ClientAudioDirector.MusicSlot.class);
    private static final Map<String, Supplier<SoundEvent>> SFX = Map.ofEntries(
            Map.entry("skill", () -> TurnboundSounds.SFX_SKILL.get()),
            Map.entry("hit_light", () -> TurnboundSounds.SFX_HIT_LIGHT.get()),
            Map.entry("hit_heavy", () -> TurnboundSounds.SFX_HIT_HEAVY.get()),
            Map.entry("reaction_hit", () -> TurnboundSounds.SFX_REACTION_HIT.get()),
            Map.entry("dot_tick", () -> TurnboundSounds.SFX_DOT_TICK.get()),
            Map.entry("heal", () -> TurnboundSounds.SFX_HEAL.get()),
            Map.entry("barrier", () -> TurnboundSounds.SFX_BARRIER.get()),
            Map.entry("revive", () -> TurnboundSounds.SFX_REVIVE.get()),
            Map.entry("down", () -> TurnboundSounds.SFX_DOWN.get()),
            Map.entry("boss_phase", () -> TurnboundSounds.SFX_BOSS_PHASE.get()),
            Map.entry("spawn", () -> TurnboundSounds.SFX_SPAWN.get()));
    private static final Map<ClientAudioDirector.MusicSlot, MusicLoop> ACTIVE_MUSIC = new EnumMap<>(ClientAudioDirector.MusicSlot.class);

    static {
        MUSIC.put(ClientAudioDirector.MusicSlot.HUB, () -> TurnboundSounds.MUSIC_HUB.get());
        MUSIC.put(ClientAudioDirector.MusicSlot.REGION_EXPLORE, () -> TurnboundSounds.MUSIC_REGION_EXPLORE.get());
        MUSIC.put(ClientAudioDirector.MusicSlot.BATTLE_NORMAL, () -> TurnboundSounds.MUSIC_BATTLE_NORMAL.get());
        MUSIC.put(ClientAudioDirector.MusicSlot.BATTLE_ELITE, () -> TurnboundSounds.MUSIC_BATTLE_ELITE.get());
        MUSIC.put(ClientAudioDirector.MusicSlot.BATTLE_BOSS, () -> TurnboundSounds.MUSIC_BATTLE_BOSS.get());
        MUSIC.put(ClientAudioDirector.MusicSlot.BATTLE_FINAL, () -> TurnboundSounds.MUSIC_BATTLE_FINAL.get());
    }

    private ClientAudioPlayback() {}

    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopAll(minecraft.getSoundManager());
            ClientAudioDirector.resetSession();
            return;
        }
        syncMusic(minecraft);
        playCues(minecraft, ClientAudioDirector.drainAcceptedCues());
    }

    private static void syncMusic(Minecraft minecraft) {
        SoundManager manager = minecraft.getSoundManager();
        ClientAudioDirector.MusicMix mix = ClientAudioDirector.musicMix();
        ClientAudioDirector.MusicSlot desired = mix.incoming();

        if (desired == ClientAudioDirector.MusicSlot.NONE) {
            stopAll(manager);
            return;
        }

        // TURNBOUND owns music while its field/battle session is active; prevent vanilla music from stacking underneath it.
        minecraft.getMusicManager().stopPlaying();
        ensurePlaying(manager, desired);
        if (mix.transitioning() && mix.outgoing() != ClientAudioDirector.MusicSlot.NONE) ensurePlaying(manager, mix.outgoing());

        Iterator<Map.Entry<ClientAudioDirector.MusicSlot, MusicLoop>> it = ACTIVE_MUSIC.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ClientAudioDirector.MusicSlot, MusicLoop> entry = it.next();
            MusicLoop instance = entry.getValue();
            boolean stillRelevant = entry.getKey() == desired || (mix.transitioning() && entry.getKey() == mix.outgoing());
            if (!stillRelevant || instance.isStopped()) {
                manager.stop(instance);
                it.remove();
            }
        }
    }

    private static void ensurePlaying(SoundManager manager, ClientAudioDirector.MusicSlot slot) {
        Supplier<SoundEvent> supplier = MUSIC.get(slot);
        if (supplier == null) return;
        MusicLoop existing = ACTIVE_MUSIC.get(slot);
        if (existing != null && manager.isActive(existing) && !existing.isStopped()) return;
        MusicLoop created = new MusicLoop(supplier.get(), slot);
        ACTIVE_MUSIC.put(slot, created);
        manager.play(created);
    }

    private static void playCues(Minecraft minecraft, List<ClientAudioDirector.Cue> cues) {
        if (cues.isEmpty()) return;
        ClientBattleState.Snapshot snapshot = ClientBattleState.snapshot();
        for (ClientAudioDirector.Cue cue : cues) {
            Supplier<SoundEvent> supplier = SFX.get(cue.id());
            if (supplier == null) continue;
            float volume = switch (cue.group()) {
                case IMPACT -> cue.priority() >= 3 ? 0.92F : 0.72F;
                case REACTION -> 0.88F;
                case SUPPORT -> 0.68F;
                case SYSTEM -> cue.priority() >= 3 ? 0.94F : 0.82F;
                case SKILL -> 0.62F;
            };
            float pitch = 0.96F + Math.min(3, Math.max(0, cue.priority())) * 0.025F;
            ClientBattleState.Unit unit = cueUnit(snapshot, cue);
            double x = unit != null ? unit.x() : minecraft.player.getX();
            double y = unit != null ? unit.y() : minecraft.player.getY();
            double z = unit != null ? unit.z() : minecraft.player.getZ();
            minecraft.level.playLocalSound(x, y, z, supplier.get(), SoundSource.PLAYERS, volume, pitch, false);
        }
    }

    private static ClientBattleState.Unit cueUnit(ClientBattleState.Snapshot snapshot, ClientAudioDirector.Cue cue) {
        if (snapshot == null || snapshot.units().isEmpty()) return null;
        boolean sourceCentric = "skill".equals(cue.id()) || "boss_phase".equals(cue.id());
        String preferred = sourceCentric ? cue.sourceId() : cue.targetId();
        String fallback = sourceCentric ? cue.targetId() : cue.sourceId();
        ClientBattleState.Unit unit = findUnit(snapshot, preferred);
        return unit != null ? unit : findUnit(snapshot, fallback);
    }

    private static ClientBattleState.Unit findUnit(ClientBattleState.Snapshot snapshot, String id) {
        if (id == null || id.isBlank()) return null;
        for (ClientBattleState.Unit unit : snapshot.units()) {
            if (id.equals(unit.id())) return unit;
        }
        return null;
    }

    private static float musicGain(ClientAudioDirector.MusicSlot slot) {
        float roleMultiplier = switch (slot) {
            case HUB -> 0.84F;
            case REGION_EXPLORE -> 0.88F;
            case BATTLE_NORMAL -> 0.94F;
            case BATTLE_ELITE -> 0.97F;
            case BATTLE_BOSS -> 1.00F;
            case BATTLE_FINAL -> 1.04F;
            case NONE -> 0.0F;
        };
        return BASE_MUSIC_GAIN * roleMultiplier;
    }

    private static void stopAll(SoundManager manager) {
        for (MusicLoop music : ACTIVE_MUSIC.values()) manager.stop(music);
        ACTIVE_MUSIC.clear();
        ClientAudioDirector.drainAcceptedCues();
    }

    /** A relative stereo loop whose volume follows the director's context-sensitive cross-fade envelope. */
    private static final class MusicLoop extends AbstractTickableSoundInstance {
        private final ClientAudioDirector.MusicSlot slot;

        private MusicLoop(SoundEvent event, ClientAudioDirector.MusicSlot slot) {
            super(event, SoundSource.MUSIC, RandomSource.create());
            this.slot = slot;
            this.looping = true;
            this.relative = true;
            this.volume = 0.0F;
            this.pitch = 1.0F;
        }

        @Override
        public boolean canStartSilent() { return true; }

        @Override
        public void tick() {
            ClientAudioDirector.MusicMix mix = ClientAudioDirector.musicMix();
            float gain;
            if (slot == mix.incoming()) gain = mix.incomingGain();
            else if (mix.transitioning() && slot == mix.outgoing()) gain = mix.outgoingGain();
            else {
                stop();
                return;
            }
            this.volume = Math.max(0.0F, Math.min(1.0F, gain)) * musicGain(slot);
        }
    }
}
