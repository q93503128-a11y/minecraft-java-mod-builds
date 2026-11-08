package kr.moonseungjun.anomalousclasses.state;

import net.minecraft.nbt.CompoundTag;

public record ClassProgress(boolean played, int level, long experience, int skillPoints, CompoundTag permanentData) {
    public ClassProgress {
        level = Math.max(1, level);
        experience = Math.max(0L, experience);
        skillPoints = Math.max(0, skillPoints);
        permanentData = permanentData == null ? new CompoundTag() : permanentData.copy();
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
}
