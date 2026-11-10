package kr.moonseungjun.anomalousclasses.state;

import java.util.Set;
import java.util.TreeSet;
import net.minecraft.nbt.CompoundTag;

public record ClassProgress(boolean played, int level, long experience, int skillPoints,
                            CompoundTag permanentData, int[] skillRanks,
                            int skillTreeVersion, Set<String> purchasedNodes) {
    public ClassProgress(boolean played, int level, long experience, int skillPoints, CompoundTag permanentData) {
        this(played, level, experience, skillPoints, permanentData, new int[0],
                GraphSkillTreeCatalog.CURRENT_VERSION, Set.of());
    }

    public ClassProgress(boolean played, int level, long experience, int skillPoints,
                         CompoundTag permanentData, int[] skillRanks) {
        this(played, level, experience, skillPoints, permanentData, skillRanks, 0, Set.of());
    }

    public ClassProgress {
        level = Math.max(1, level);
        experience = Math.max(0L, experience);
        skillPoints = Math.max(0, skillPoints);
        permanentData = permanentData == null ? new CompoundTag() : permanentData.copy();
        skillRanks = skillRanks == null ? new int[0] : skillRanks.clone();
        for (int index = 0; index < skillRanks.length; index++) skillRanks[index] = Math.max(0, skillRanks[index]);
        skillTreeVersion = Math.max(0, skillTreeVersion);
        purchasedNodes = purchasedNodes == null ? Set.of() : Set.copyOf(new TreeSet<>(purchasedNodes));
    }

    public static ClassProgress fresh() {
        return new ClassProgress(false, 1, 0L, 0, new CompoundTag());
    }

    public ClassProgress markPlayed() {
        return played ? this : copy(true, skillPoints, skillRanks, skillTreeVersion, purchasedNodes);
    }

    @Override
    public CompoundTag permanentData() {
        return permanentData.copy();
    }

    @Override
    public int[] skillRanks() {
        return skillRanks.clone();
    }

    public ClassProgress withSkillTree(int points, int[] ranks) {
        return copy(played, points, ranks, skillTreeVersion, purchasedNodes);
    }

    public ClassProgress withGraphTree(int points, Set<String> nodes, int version) {
        return copy(played, points, new int[0], version, nodes);
    }

    public ClassProgress withLevelAndExperience(int newLevel, long newExperience, int newSkillPoints) {
        return new ClassProgress(played, newLevel, newExperience, newSkillPoints, permanentData,
                skillRanks, skillTreeVersion, purchasedNodes);
    }

    private ClassProgress copy(boolean newPlayed, int points, int[] ranks, int treeVersion, Set<String> nodes) {
        return new ClassProgress(newPlayed, level, experience, points, permanentData, ranks, treeVersion, nodes);
    }
}
