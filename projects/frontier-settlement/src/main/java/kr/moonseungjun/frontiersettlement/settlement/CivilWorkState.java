package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/** Persisted bounded selected-area earthwork state. Project-local earth balance is never a settlement resource. */
public record CivilWorkState(boolean active,
                             int minX, int maxX, int minZ, int maxZ,
                             int gradeY,
                             int phase,
                             int earthBank,
                             int completedSteps,
                             int initialCutBlocks,
                             int initialFillBlocks,
                             int initialRetainingBlocks) {
    public static final int PHASE_CUT = 0;
    public static final int PHASE_FILL = 1;
    public static final int PHASE_RETURN = 2;
    public static final int PHASE_RETAIN = 3;
    public static final CivilWorkState EMPTY = new CivilWorkState(false, 0, 0, 0, 0, 0, PHASE_CUT, 0, 0, 0, 0, 0);

    public static final Codec<CivilWorkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("active", false).forGetter(CivilWorkState::active),
            Codec.INT.optionalFieldOf("min_x", 0).forGetter(CivilWorkState::minX),
            Codec.INT.optionalFieldOf("max_x", 0).forGetter(CivilWorkState::maxX),
            Codec.INT.optionalFieldOf("min_z", 0).forGetter(CivilWorkState::minZ),
            Codec.INT.optionalFieldOf("max_z", 0).forGetter(CivilWorkState::maxZ),
            Codec.INT.optionalFieldOf("grade_y", 0).forGetter(CivilWorkState::gradeY),
            Codec.INT.optionalFieldOf("phase", PHASE_CUT).forGetter(CivilWorkState::phase),
            Codec.INT.optionalFieldOf("earth_bank", 0).forGetter(CivilWorkState::earthBank),
            Codec.INT.optionalFieldOf("completed_steps", 0).forGetter(CivilWorkState::completedSteps),
            Codec.INT.optionalFieldOf("initial_cut_blocks", 0).forGetter(CivilWorkState::initialCutBlocks),
            Codec.INT.optionalFieldOf("initial_fill_blocks", 0).forGetter(CivilWorkState::initialFillBlocks),
            Codec.INT.optionalFieldOf("initial_retaining_blocks", 0).forGetter(CivilWorkState::initialRetainingBlocks)
    ).apply(instance, CivilWorkState::new));

    public CivilWorkState {
        if (!active) {
            minX = maxX = minZ = maxZ = gradeY = earthBank = completedSteps = initialCutBlocks = initialFillBlocks = initialRetainingBlocks = 0;
            phase = PHASE_CUT;
        } else {
            if (minX > maxX) { int swap = minX; minX = maxX; maxX = swap; }
            if (minZ > maxZ) { int swap = minZ; minZ = maxZ; maxZ = swap; }
            phase = phase == PHASE_FILL || phase == PHASE_RETURN || phase == PHASE_RETAIN ? phase : PHASE_CUT;
            earthBank = Math.max(0, earthBank);
            completedSteps = Math.max(0, completedSteps);
            initialCutBlocks = Math.max(0, initialCutBlocks);
            initialFillBlocks = Math.max(0, initialFillBlocks);
            initialRetainingBlocks = Math.max(0, initialRetainingBlocks);
        }
    }

    public int width() { return active ? maxX - minX + 1 : 0; }
    public int depth() { return active ? maxZ - minZ + 1 : 0; }
    public int area() { return width() * depth(); }
    public int totalSteps() { return initialCutBlocks + initialRetainingBlocks + initialFillBlocks; }
    public int completedRetainingBlocks() {
        if (phase == PHASE_CUT) return 0;
        if (phase == PHASE_RETAIN) {
            return Math.min(initialRetainingBlocks, Math.max(0, completedSteps - initialCutBlocks));
        }
        return initialRetainingBlocks;
    }
    public int remainingRetainingBlocks() {
        return Math.max(0, initialRetainingBlocks - completedRetainingBlocks());
    }
    public int progressPercent() {
        if (phase == PHASE_RETURN) return 100;
        return totalSteps() <= 0 ? 0 : Math.min(100, completedSteps * 100 / totalSteps());
    }
    public BlockPos center() { return new BlockPos((minX + maxX) / 2, gradeY, (minZ + maxZ) / 2); }

    public CivilWorkState afterCut() {
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, PHASE_CUT,
                earthBank + 1, completedSteps + 1, initialCutBlocks, initialFillBlocks, initialRetainingBlocks);
    }

    public CivilWorkState beginRetaining() {
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, PHASE_RETAIN,
                earthBank, completedSteps, initialCutBlocks, initialFillBlocks, initialRetainingBlocks);
    }

    public CivilWorkState afterRetaining() {
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, PHASE_RETAIN,
                earthBank, completedSteps + 1, initialCutBlocks, initialFillBlocks, initialRetainingBlocks);
    }

    public CivilWorkState beginFill() {
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, PHASE_FILL,
                earthBank, completedSteps, initialCutBlocks, initialFillBlocks, initialRetainingBlocks);
    }

    public CivilWorkState afterFill() {
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, PHASE_FILL,
                Math.max(0, earthBank - 1), completedSteps + 1, initialCutBlocks, initialFillBlocks, initialRetainingBlocks);
    }

    public CivilWorkState beginReturn() {
        return new CivilWorkState(true, minX, maxX, minZ, maxZ, gradeY, PHASE_RETURN,
                earthBank, completedSteps, initialCutBlocks, initialFillBlocks, initialRetainingBlocks);
    }
}
