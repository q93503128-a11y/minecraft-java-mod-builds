package kr.moonseungjun.campfiresessions.client;

import java.util.List;
import kr.moonseungjun.campfiresessions.registry.ModSounds;

public final class MusicCatalog {
    public static final List<MusicTrack> TRACKS = List.of(
            new MusicTrack("etirwer", "Etirwer", "Kistol", "warm acoustic loop", MusicTheme.CLEAN, ModSounds.ETIRWER::get),
            new MusicTrack("cozy_puzzle", "Cozy Puzzle In-Game 3", "MintoDog", "acoustic · relaxed", MusicTheme.CLEAN, ModSounds.COZY_PUZZLE::get),
            new MusicTrack("neon_circuit", "Neon sign Circuit", "MintoDog", "synth · night drive", MusicTheme.NEON, ModSounds.NEON_CIRCUIT::get),
            new MusicTrack("underwater_pad", "Underwater Ambient Pad", "isaiah658", "ambient · submerged", MusicTheme.OCEAN, ModSounds.UNDERWATER_PAD::get)
    );

    private MusicCatalog() {}

    public static MusicTrack get(int index) {
        if (TRACKS.isEmpty()) throw new IllegalStateException("Campfire Sessions has no tracks");
        return TRACKS.get(Math.floorMod(index, TRACKS.size()));
    }
}
