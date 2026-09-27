package kr.moonseungjun.campfiresessions.client;

import java.util.ArrayList;
import java.util.List;
import kr.moonseungjun.campfiresessions.registry.ModSounds;

public final class MusicCatalog {
    private static final List<MusicTrack> BUILT_IN_TRACKS = List.of(
            new MusicTrack("etirwer", "Etirwer", "Kistol", "warm acoustic loop", MusicTheme.CLEAN, 96, ModSounds.ETIRWER::get),
            new MusicTrack("cozy_puzzle", "Cozy Puzzle In-Game 3", "MintoDog", "acoustic · relaxed", MusicTheme.DESERT, 108, ModSounds.COZY_PUZZLE::get),
            new MusicTrack("neon_circuit", "Neon sign Circuit", "MintoDog", "synth · night drive", MusicTheme.NEON, 145, ModSounds.NEON_CIRCUIT::get),
            new MusicTrack("underwater_pad", "Underwater Ambient Pad", "isaiah658", "ambient · submerged", MusicTheme.OCEAN, 70, ModSounds.UNDERWATER_PAD::get),
            new MusicTrack("cozy_puzzle_1", "Cozy Puzzle In-Game 1", "MintoDog", "bossa · bright", MusicTheme.CLEAN, 118, ModSounds.COZY_PUZZLE_1::get),
            new MusicTrack("cozy_title", "Cozy Puzzle Title", "MintoDog", "chill · playful", MusicTheme.CLEAN, 95, ModSounds.COZY_TITLE::get),
            new MusicTrack("beach_stage", "Beach Stage", "MintoDog", "8-bit · seaside", MusicTheme.OCEAN, 128, ModSounds.BEACH_STAGE::get),
            new MusicTrack("space_battle", "Space Battle", "MintoDog", "synth · energetic", MusicTheme.NEON, 130, ModSounds.SPACE_BATTLE::get),
            new MusicTrack("jazzy_battle", "Jazzy Battle Theme", "MintoDog", "jazz · punchy", MusicTheme.ROUGH, 130, ModSounds.JAZZY_BATTLE::get),
            new MusicTrack("desert_pink", "Desert Pink and Navy Blue", "Some Weirdo", "ambient · vast", MusicTheme.DESERT, 90, ModSounds.DESERT_PINK::get),
            new MusicTrack("nighttime_solitude", "Nighttime Solitude", "celestialghost8", "electronic · late night", MusicTheme.NEON, 110, ModSounds.NIGHTTIME_SOLITUDE::get),
            new MusicTrack("fairy_adventure", "Fairy Adventure", "MintoDog", "fantasy · bright journey", MusicTheme.OCEAN, 140, ModSounds.FAIRY_ADVENTURE::get),
            new MusicTrack("other_center", "Other Center", "zesona", "playful · mysterious", MusicTheme.CLEAN, 134, ModSounds.OTHER_CENTER::get),
            new MusicTrack("magic_puzzle_1", "Magic Puzzle In-Game 1", "MintoDog", "strings · magic puzzle", MusicTheme.DESERT, 110, ModSounds.MAGIC_PUZZLE_1::get),
            new MusicTrack("urban_boss_battle", "Urban Boss Battle", "MintoDog", "brass · urban groove", MusicTheme.ROUGH, 135, ModSounds.URBAN_BOSS_BATTLE::get)
    );

    private static volatile List<MusicTrack> tracks = BUILT_IN_TRACKS;
    private static volatile int localTrackCount;

    private MusicCatalog() {}

    public static List<MusicTrack> tracks() {
        return tracks;
    }

    public static int size() {
        return tracks.size();
    }

    public static int builtInTrackCount() {
        return BUILT_IN_TRACKS.size();
    }

    public static int localTrackCount() {
        return localTrackCount;
    }

    static synchronized void setLocalTracks(List<MusicTrack> localTracks) {
        ArrayList<MusicTrack> combined = new ArrayList<>(BUILT_IN_TRACKS.size() + localTracks.size());
        combined.addAll(BUILT_IN_TRACKS);
        combined.addAll(localTracks);
        tracks = List.copyOf(combined);
        localTrackCount = localTracks.size();
        CampfireMusicClient.catalogChanged();
    }

    public static MusicTrack get(int index) {
        List<MusicTrack> current = tracks;
        if (current.isEmpty()) throw new IllegalStateException("Campfire Sessions has no tracks");
        return current.get(Math.floorMod(index, current.size()));
    }
}
