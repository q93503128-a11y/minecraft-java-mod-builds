package kr.moonseungjun.campfiresessions.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;

public final class LocalMusicLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path MUSIC_DIR = FMLPaths.CONFIGDIR.get().resolve(CampfireSessions.MOD_ID).resolve("music");
    private static final Path PACK_ROOT = MUSIC_DIR.resolve(".campfiresessions-pack");
    private static final long MAX_OGG_BYTES = 256L * 1024L * 1024L;

    private LocalMusicLibrary() {}

    public static void register(IEventBus modBus) {
        ensureMusicDirectory();
        modBus.addListener(LocalMusicLibrary::onAddPackFinders);
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        try {
            List<MusicTrack> localTracks = rebuildLocalPack();
            MusicCatalog.setLocalTracks(localTracks);
            if (localTracks.isEmpty()) return;

            Pack pack = Pack.readMetaAndCreate(
                    new PackLocationInfo(
                            "campfiresessions_local_music",
                            Component.literal("Campfire Sessions Local Music"),
                            PackSource.BUILT_IN,
                            Optional.empty()
                    ),
                    new PathPackResources.PathResourcesSupplier(PACK_ROOT),
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            );
            if (pack != null) {
                event.addRepositorySource(consumer -> consumer.accept(pack));
            }
        } catch (Exception exception) {
            MusicCatalog.setLocalTracks(List.of());
            CampfireSessions.LOGGER.error("Failed to prepare local custom music. Bundled playlist will remain available.", exception);
        }
    }

    private static List<MusicTrack> rebuildLocalPack() throws IOException {
        ensureMusicDirectory();
        deleteTree(PACK_ROOT);
        Files.createDirectories(PACK_ROOT.resolve("assets/campfiresessions/sounds/local"));

        JsonObject packRoot = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Campfire Sessions local custom music");
        pack.add("min_format", GSON.toJsonTree(new int[] {88, 0}));
        pack.add("max_format", GSON.toJsonTree(new int[] {88, 0}));
        packRoot.add("pack", pack);
        Files.writeString(PACK_ROOT.resolve("pack.mcmeta"), GSON.toJson(packRoot) + System.lineSeparator(), StandardCharsets.UTF_8);

        List<Path> oggFiles;
        try (var stream = Files.list(MUSIC_DIR)) {
            oggFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .limit(ModSounds.localTrackSlotCount())
                    .toList();
        }

        JsonObject soundDefinitions = new JsonObject();
        ArrayList<MusicTrack> tracks = new ArrayList<>();
        MusicMetadata.clearRuntimeDurations();

        for (Path source : oggFiles) {
            int slot = tracks.size();
            try {
                if (Files.size(source) <= 0L || Files.size(source) > MAX_OGG_BYTES) {
                    CampfireSessions.LOGGER.warn("Skipping local music {} because its file size is invalid", source.getFileName());
                    continue;
                }
                int duration = oggDurationSeconds(Files.readAllBytes(source));
                LocalMetadata metadata = readMetadata(source);
                String soundId = String.format(Locale.ROOT, "local_%02d", slot);
                String slotFile = String.format(Locale.ROOT, "slot_%02d.ogg", slot);
                Path target = PACK_ROOT.resolve("assets/campfiresessions/sounds/local").resolve(slotFile);
                linkOrCopy(source, target);

                JsonObject sound = new JsonObject();
                var sounds = new com.google.gson.JsonArray();
                JsonObject entry = new JsonObject();
                entry.addProperty("name", "campfiresessions:local/" + slotFile.substring(0, slotFile.length() - 4));
                entry.addProperty("stream", true);
                sounds.add(entry);
                sound.add("sounds", sounds);
                soundDefinitions.add(soundId, sound);

                MusicMetadata.registerRuntimeDuration(soundId, duration);
                tracks.add(new MusicTrack(
                        soundId,
                        metadata.title(),
                        metadata.artist(),
                        "local custom OGG",
                        metadata.theme(),
                        metadata.bpm(),
                        ModSounds.localTrack(slot)
                ));
                CampfireSessions.LOGGER.info("Loaded local music {} ({}s, {} BPM)", source.getFileName(), duration, metadata.bpm());
            } catch (Exception exception) {
                CampfireSessions.LOGGER.warn("Skipping invalid local music file {}", source.getFileName(), exception);
            }
        }

        Path soundsJson = PACK_ROOT.resolve("assets/campfiresessions/sounds.json");
        Files.createDirectories(soundsJson.getParent());
        Files.writeString(soundsJson, GSON.toJson(soundDefinitions) + System.lineSeparator(), StandardCharsets.UTF_8);
        return List.copyOf(tracks);
    }

    private static LocalMetadata readMetadata(Path ogg) {
        String fileName = ogg.getFileName().toString();
        String stem = fileName.substring(0, fileName.length() - 4);
        String defaultTitle = stem.replace('_', ' ').replace('-', ' ').strip();
        if (defaultTitle.isBlank()) defaultTitle = "Local Track";

        Path metadataPath = ogg.resolveSibling(stem + ".json");
        String title = defaultTitle;
        String artist = "Local Music";
        int bpm = 100;
        MusicTheme theme = MusicTheme.CLEAN;

        if (Files.isRegularFile(metadataPath)) {
            try (var reader = Files.newBufferedReader(metadataPath, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("title") && !json.get("title").getAsString().isBlank()) title = json.get("title").getAsString().strip();
                if (json.has("artist") && !json.get("artist").getAsString().isBlank()) artist = json.get("artist").getAsString().strip();
                if (json.has("bpm")) {
                    int parsed = json.get("bpm").getAsInt();
                    if (parsed >= 30 && parsed <= 300) bpm = parsed;
                }
                if (json.has("theme")) {
                    try {
                        theme = MusicTheme.valueOf(json.get("theme").getAsString().strip().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        CampfireSessions.LOGGER.warn("Unknown local music theme in {}; using CLEAN", metadataPath.getFileName());
                    }
                }
            } catch (Exception exception) {
                CampfireSessions.LOGGER.warn("Could not read {}; using filename/default metadata", metadataPath.getFileName(), exception);
            }
        }
        return new LocalMetadata(title, artist, bpm, theme);
    }

    private static int oggDurationSeconds(byte[] data) {
        int marker = indexOf(data, new byte[] {1, 'v', 'o', 'r', 'b', 'i', 's'}, 0);
        if (marker < 0 || marker + 16 > data.length) throw new IllegalArgumentException("Vorbis identification header not found");
        long sampleRate = uint32LE(data, marker + 12);
        if (sampleRate <= 0L) throw new IllegalArgumentException("Invalid Vorbis sample rate");

        long maxGranule = 0L;
        int offset = 0;
        byte[] pageMarker = new byte[] {'O', 'g', 'g', 'S'};
        while (true) {
            int page = indexOf(data, pageMarker, offset);
            if (page < 0 || page + 27 > data.length) break;
            long granule = int64LE(data, page + 6);
            if (granule >= 0L) maxGranule = Math.max(maxGranule, granule);
            int segmentCount = Byte.toUnsignedInt(data[page + 26]);
            int tableEnd = page + 27 + segmentCount;
            if (tableEnd > data.length) break;
            int bodySize = 0;
            for (int i = page + 27; i < tableEnd; i++) bodySize += Byte.toUnsignedInt(data[i]);
            offset = Math.min(data.length, tableEnd + bodySize);
        }
        if (maxGranule <= 0L) throw new IllegalArgumentException("No usable OGG granule position found");
        return Math.max(1, (int) Math.round(maxGranule / (double) sampleRate));
    }

    private static long uint32LE(byte[] data, int offset) {
        return (Byte.toUnsignedLong(data[offset]))
                | (Byte.toUnsignedLong(data[offset + 1]) << 8)
                | (Byte.toUnsignedLong(data[offset + 2]) << 16)
                | (Byte.toUnsignedLong(data[offset + 3]) << 24);
    }

    private static long int64LE(byte[] data, int offset) {
        long value = 0L;
        for (int i = 0; i < 8; i++) value |= Byte.toUnsignedLong(data[offset + i]) << (8 * i);
        return value;
    }

    private static int indexOf(byte[] data, byte[] needle, int start) {
        outer:
        for (int i = Math.max(0, start); i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static void linkOrCopy(Path source, Path target) throws IOException {
        Files.deleteIfExists(target);
        try {
            Files.createLink(target, source);
        } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void ensureMusicDirectory() {
        try {
            Files.createDirectories(MUSIC_DIR);
            Path readme = MUSIC_DIR.resolve("README.txt");
            String instructions = """
                    Campfire Sessions - Local Custom Music

                    Put OGG/Vorbis files directly in this folder, then restart Minecraft.
                    Each song may have an optional same-name JSON file, for example song.ogg + song.json:
                    {
                      "title": "My Song",
                      "artist": "Artist",
                      "bpm": 120,
                      "theme": "NEON"
                    }

                    Themes: CLEAN, NEON, OCEAN, DESERT, ROUGH
                    BPM range: 30-300. Missing metadata defaults to filename / Local Music / 100 BPM / CLEAN.
                    Up to 32 local songs are loaded. These files stay on this PC and are never bundled into the mod JAR.
                    Only add audio you are allowed to use.
                    """;
            if (Files.notExists(readme) || !Files.readString(readme, StandardCharsets.UTF_8).equals(instructions)) {
                Files.writeString(readme, instructions, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            CampfireSessions.LOGGER.error("Failed to initialize local music folder {}", MUSIC_DIR, exception);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (Files.notExists(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record LocalMetadata(String title, String artist, int bpm, MusicTheme theme) {}
}
