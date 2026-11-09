package kr.moonseungjun.anomalousclasses.state;

import net.minecraft.nbt.CompoundTag;

public record ClassProgress(boolean played, int level, long experience, int skillPoints,
                            CompoundTag permanentData, int[] skillRanks) {
    public ClassProgress(boolean played, int level, long experience, int skillPoints, CompoundTag permanentData) {
        this(played, level, experience, skillPoints, permanentData, new int[0]);
    }

    public ClassProgress {
        level = Math.max(1, level);
        experience = Math.max(0L, experience);
        skillPoints = Math.max(0, skillPoints);
        permanentData = permanentData == null ? new CompoundTag() : permanentData.copy();
        skillRanks = skillRanks == null ? new int[0] : skillRanks.clone();
        for (int index = 0; index < skillRanks.length; index++) skillRanks[index] = Math.max(0, skillRanks[index]);
    }

    public static ClassProgress fresh() {
        return new ClassProgress(false, 1, 0L, 0, new CompoundTag());
    }

    public ClassProgress markPlayed() {
        return played ? this : new ClassProgress(true, level, experience, skillPoints, permanentData);
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
        return new ClassProgress(played, level, experience, points, permanentData, ranks);
    }

    public ClassProgress withLevelAndExperience(int newLevel, long newExperience, int newSkillPoints) {
        return new ClassProgress(played, newLevel, newExperience, newSkillPoints, permanentData, skillRanks);
    }
}
