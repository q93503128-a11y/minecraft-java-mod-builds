package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class RotatedBlueprints {
    private RotatedBlueprints() {}

    public static List<BuildingBlueprints.Placement> create(BuildingType type, BlockPos origin, int rotationId) {
        BuildingRotation rotation = BuildingRotation.fromId(rotationId);
        List<BuildingBlueprints.Placement> raw = BuildingBlueprints.create(type, origin);
        if (rotation == BuildingRotation.NONE) return raw;

        List<BuildingBlueprints.Placement> result = new ArrayList<>(raw.size());
        for (BuildingBlueprints.Placement placement : raw) {
            BlockPos rotatedPos = rotation.rotateLocal(origin, placement.pos(), type.width(), type.depth());
            result.add(new BuildingBlueprints.Placement(
                    rotatedPos,
                    rotation.rotateState(placement.state()),
                    placement.phase()));
        }
        return result;
    }
}
