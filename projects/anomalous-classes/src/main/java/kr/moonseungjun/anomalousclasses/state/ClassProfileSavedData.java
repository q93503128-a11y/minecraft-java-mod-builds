package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;

public final class ClassProfileSavedData extends SavedData {
    private static final String DATA_NAME = "anomalousclasses_player_profiles";
    private static final Factory<ClassProfileSavedData> FACTORY = new Factory<>(ClassProfileSavedData::new, ClassProfileSavedData::load, DataFixTypes.LEVEL);

    private final Map<UUID, PlayerClassProfile> profiles = new HashMap<>();

    public static ClassProfileSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public PlayerClassProfile profile(UUID playerId) {
        return profiles.computeIfAbsent(playerId, ignored -> new PlayerClassProfile());
    }

    public void updateProgress(UUID playerId, ClassId classId, ClassProgress progress) {
        profile(playerId).updateProgress(classId, progress);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag playerList = new ListTag();
        for (Map.Entry<UUID, PlayerClassProfile> profileEntry : profiles.entrySet()) {
            PlayerClassProfile profile = profileEntry.getValue();
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", profileEntry.getKey());
            playerTag.putString("currentClass", profile.currentClass().map(ClassId::key).orElse(""));
            playerTag.putInt("revision", profile.revision());

            ListTag progressList = new ListTag();
            for (ClassId id : ClassId.values()) {
                ClassProgress progress = profile.progress(id);
                CompoundTag progressTag = new CompoundTag();
                progressTag.putString("class", id.key());
                progressTag.putBoolean("played", progress.played());
                progressTag.putInt("level", progress.level());
                progressTag.putLong("experience", progress.experience());
                progressTag.putInt("skillPoints", progress.skillPoints());
                progressTag.put("permanentData", progress.permanentData());
                ListTag ranks = new ListTag();
                for (int rank : progress.skillRanks()) ranks.add(IntTag.valueOf(rank));
                progressTag.put("skillRanks", ranks);
                progressList.add(progressTag);
            }
            playerTag.put("progress", progressList);
            playerList.add(playerTag);
        }
        tag.put("players", playerList);
        tag.putInt("schemaVersion", 2);
        return tag;
    }

    private static ClassProfileSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ClassProfileSavedData data = new ClassProfileSavedData();
        ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);
        for (int index = 0; index < playerList.size(); index++) {
            CompoundTag playerTag = playerList.getCompound(index);
            if (!playerTag.hasUUID("player")) continue;
            UUID playerId = playerTag.getUUID("player");
            ClassId selected = ClassId.fromKey(playerTag.getString("currentClass")).orElse(null);
            EnumMap<ClassId, ClassProgress> progress = new EnumMap<>(ClassId.class);
            ListTag progressList = playerTag.getList("progress", Tag.TAG_COMPOUND);
            for (int progressIndex = 0; progressIndex < progressList.size(); progressIndex++) {
                CompoundTag progressTag = progressList.getCompound(progressIndex);
                ClassId id = ClassId.fromKey(progressTag.getString("class")).orElse(null);
                if (id == null) continue;
                CompoundTag permanentData = progressTag.getCompound("permanentData");
                ListTag rankList = progressTag.getList("skillRanks", Tag.TAG_INT);
                int[] ranks = new int[rankList.size()];
                for (int rankIndex = 0; rankIndex < rankList.size(); rankIndex++) {
                    ranks[rankIndex] = Math.max(0, rankList.getInt(rankIndex));
                }
                progress.put(id, new ClassProgress(
                        progressTag.getBoolean("played"),
                        progressTag.contains("level", Tag.TAG_ANY_NUMERIC) ? progressTag.getInt("level") : 1,
                        progressTag.getLong("experience"),
                        progressTag.getInt("skillPoints"),
                        permanentData,
                        ranks));
            }
            PlayerClassProfile profile = new PlayerClassProfile();
            profile.load(selected, progress, playerTag.getInt("revision"));
            data.profiles.put(playerId, profile);
        }
        return data;
    }
}
