package kr.moonseungjun.survivalascension.progress;

/*
 * Portions of the data-shape approach are adapted from Skill Proficiencies:
 * Copyright (c) 2026 balovich-matje
 * Licensed under the MIT License. See THIRD_PARTY_NOTICES.md.
 */

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillProgressData extends SavedData {
    private static final Codec<Map<String, Long>> SKILL_XP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG);

    private record PlayerEntry(String uuid, Map<String, Long> skills, long legacyMiningXp, boolean introduced) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                SKILL_XP_CODEC.optionalFieldOf("skills", Map.of()).forGetter(PlayerEntry::skills),
                Codec.LONG.optionalFieldOf("mining_xp", 0L).forGetter(PlayerEntry::legacyMiningXp),
                Codec.BOOL.optionalFieldOf("introduced", false).forGetter(PlayerEntry::introduced)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<SkillProgressData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "mining_progress_v1"),
            SkillProgressData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(SkillProgressData::entries)
            ).apply(instance, SkillProgressData::new))
    );

    private static final class PlayerState {
        private final Map<String, Long> xp = new HashMap<>();
        private boolean introduced;

        private PlayerState(Map<String, Long> xp, long legacyMiningXp, boolean introduced) {
            xp.forEach((id, value) -> this.xp.put(id, Math.max(0L, value)));
            if (!this.xp.containsKey(SkillType.MINING.id()) && legacyMiningXp > 0L) {
                this.xp.put(SkillType.MINING.id(), legacyMiningXp);
            }
            this.introduced = introduced;
        }
    }

    public record AddXpResult(SkillType skill, int oldLevel, int newLevel, long oldXp, long newXp) {
        public boolean leveledUp() { return newLevel > oldLevel; }
    }

    private final Map<String, PlayerState> players = new HashMap<>();

    public SkillProgressData() {}

    private SkillProgressData(List<PlayerEntry> entries) {
        for (PlayerEntry entry : entries) {
            players.put(entry.uuid(), new PlayerState(entry.skills(), entry.legacyMiningXp(), entry.introduced()));
        }
    }

    private List<PlayerEntry> entries() {
        List<PlayerEntry> result = new ArrayList<>(players.size());
        players.forEach((uuid, state) -> result.add(new PlayerEntry(uuid, Map.copyOf(state.xp), 0L, state.introduced)));
        return result;
    }

    public static SkillProgressData get(MinecraftServer server) { return server.getDataStorage().computeIfAbsent(TYPE); }
    public static SkillProgressData get(ServerPlayer player) { return get(((ServerLevel) player.level()).getServer()); }

    public boolean ensureProfile(ServerPlayer player) {
        String key = player.getUUID().toString();
        if (players.containsKey(key)) return false;
        players.put(key, new PlayerState(Map.of(), 0L, false));
        setDirty();
        return true;
    }

    private PlayerState state(ServerPlayer player) {
        ensureProfile(player);
        return players.get(player.getUUID().toString());
    }

    public long xp(ServerPlayer player, SkillType skill) { return state(player).xp.getOrDefault(skill.id(), 0L); }
    public int level(ServerPlayer player, SkillType skill) { return SkillTuning.levelFromXp(xp(player, skill)); }
    public Map<String, Long> snapshot(ServerPlayer player) { return Map.copyOf(state(player).xp); }

    public boolean markIntroduced(ServerPlayer player) {
        PlayerState state = state(player);
        if (state.introduced) return false;
        state.introduced = true;
        setDirty();
        return true;
    }

    public AddXpResult addXp(ServerPlayer player, SkillType skill, long amount) {
        PlayerState state = state(player);
        long oldXp = state.xp.getOrDefault(skill.id(), 0L);
        int oldLevel = SkillTuning.levelFromXp(oldXp);
        if (amount <= 0L || oldLevel >= SkillTuning.MAX_LEVEL) return new AddXpResult(skill, oldLevel, oldLevel, oldXp, oldXp);
        long newXp = Math.min(SkillTuning.xpAtLevel(SkillTuning.MAX_LEVEL), oldXp + amount);
        state.xp.put(skill.id(), newXp);
        int newLevel = SkillTuning.levelFromXp(newXp);
        setDirty();
        return new AddXpResult(skill, oldLevel, newLevel, oldXp, newXp);
    }

    public AddXpResult setLevel(ServerPlayer player, SkillType skill, int level) {
        PlayerState state = state(player);
        long oldXp = state.xp.getOrDefault(skill.id(), 0L);
        int oldLevel = SkillTuning.levelFromXp(oldXp);
        int clamped = Math.max(0, Math.min(SkillTuning.MAX_LEVEL, level));
        long newXp = SkillTuning.xpAtLevel(clamped);
        state.xp.put(skill.id(), newXp);
        setDirty();
        return new AddXpResult(skill, oldLevel, clamped, oldXp, newXp);
    }
}
