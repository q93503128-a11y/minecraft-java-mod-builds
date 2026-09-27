package kr.moonseungjun.campfiresessions.client;

import java.util.List;
import kr.moonseungjun.campfiresessions.registry.ModSounds;

public final class MusicCatalog {
    public static final List<MusicTrack> TRACKS = List.of(
            new MusicTrack("etirwer", "Etirwer", "Kistol", "warm acoustic loop", MusicTheme.CLEAN, 96, ModSounds.ETIRWER::get),
            new MusicTrack("cozy_puzzle", "Cozy Puzzle In-Game 3", "MintoDog", "acoustic · relaxed", MusicTheme.DESERT, 108, ModSounds.COZY_PUZZLE::get),
            new MusicTrack("neon_circuit", "Neon sign Circuit", "MintoDog", "synth · night drive", MusicTheme.NEON, 145, ModSounds.NEON_CIRCUIT::get),
            new MusicTrack("underwater_pad", "Underwater Ambient Pad", "isaiah658", "ambient · submerged", MusicTheme.OCEAN, 70, ModSounds.UNDERWATER_PAD::get),
            new MusicTrack("cozy_puzzle_1", "Cozy Puzzle In-Game 1", "MintoDog", "bossa · bright", MusicTheme.CLEAN, 118, ModSounds.COZY_PUZZLE_1::get),
            new MusicTrack("cozy_title", "Cozy Puzzle Title", "MintoDog", "chill · playful", MusicTheme.CLEAN, 95, ModSounds.COZY_TITLE::get),
            new MusicTrack("beach_stage", "Beach Stage", "MintoDog", "8-bit · seaside", MusicTheme.OCEAN, 128, ModSounds.BEACH_STAGE::get),
            new MusicTrack("space_battle", "Space Battle", "MintoDog", "synth · energetic", MusicTheme.NEON, 130, ModSounds.SPACE_BATTLE::get),
            new MusicTrack("jazzy_battle", "Jazzy Battle Theme", "MintoDog", "jazz · punchy", MusicTheme.ROUGH, 130, ModSounds.JAZZY_BATTLE::get),
            new MusicTrack("desert_pink", "Desert Pink and Navy Blue", "Some Weirdo", "ambient · vast", MusicTheme.DESERT, 90, ModSounds.DESERT_PINK::get)
    );

    private MusicCatalog() {}

    public static MusicTrack get(int index) {
        if (TRACKS.isEmpty()) throw new IllegalStateException("Campfire Sessions has no tracks");
        return TRACKS.get(Math.floorMod(index, TRACKS.size()));
    }
}
