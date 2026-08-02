package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stores completed upper-floor conversions and deterministic home/work assignments. */
public final class ErdenUrbanLifeSavedData extends SavedData {
    public record Assignment(
            String citizenId,
            String citizenName,
            int homeX,
            int homeZ,
            int workX,
            int workZ,
            String workRole) {
        private static final Codec<Assignment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("citizen_id").forGetter(Assignment::citizenId),
                Codec.STRING.fieldOf("citizen_name").forGetter(Assignment::citizenName),
                Codec.INT.fieldOf("home_x").forGetter(Assignment::homeX),
                Codec.INT.fieldOf("home_z").forGetter(Assignment::homeZ),
                Codec.INT.fieldOf("work_x").forGetter(Assignment::workX),
                Codec.INT.fieldOf("work_z").forGetter(Assignment::workZ),
                Codec.STRING.fieldOf("work_role").forGetter(Assignment::workRole)
        ).apply(instance, Assignment::new));
    }

    private static final Codec<ErdenUrbanLifeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("upper_floor_revision", 0)
                    .forGetter(data -> data.upperFloorRevision),
            Codec.LONG.listOf().optionalFieldOf("completed_upper_floors", List.of())
                    .forGetter(data -> List.copyOf(data.completedUpperFloors)),
            Assignment.CODEC.listOf().optionalFieldOf("citizen_assignments", List.of())
                    .forGetter(data -> List.copyOf(data.assignments))
    ).apply(instance, ErdenUrbanLifeSavedData::new));

    public static final SavedDataType<ErdenUrbanLifeSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_urban_life"),
            level -> new ErdenUrbanLifeSavedData(),
            level -> CODEC
    );

    private int upperFloorRevision;
    private final Set<Long> completedUpperFloors;
    private final List<Assignment> assignments;

    public ErdenUrbanLifeSavedData() {
        this(0, List.of(), List.of());
    }

    private ErdenUrbanLifeSavedData(
            int upperFloorRevision,
            List<Long> completedUpperFloors,
            List<Assignment> assignments) {
        this.upperFloorRevision = Math.max(0, upperFloorRevision);
        this.completedUpperFloors = new HashSet<>(completedUpperFloors);
        this.assignments = new ArrayList<>(assignments);
    }

    public boolean isUpperFloorComplete(long entranceKey, int currentRevision) {
        return upperFloorRevision == currentRevision
                && completedUpperFloors.contains(entranceKey);
    }

    public void markUpperFloorComplete(long entranceKey, int currentRevision) {
        if (upperFloorRevision != currentRevision) {
            upperFloorRevision = currentRevision;
            completedUpperFloors.clear();
        }
        if (completedUpperFloors.add(entranceKey)) setDirty();
    }

    public int completedUpperFloorCount(int currentRevision) {
        return upperFloorRevision == currentRevision ? completedUpperFloors.size() : 0;
    }

    public List<Assignment> assignments() {
        return List.copyOf(assignments);
    }

    public void replaceAssignments(List<Assignment> replacement) {
        if (assignments.equals(replacement)) return;
        assignments.clear();
        assignments.addAll(replacement);
        setDirty();
    }
}
