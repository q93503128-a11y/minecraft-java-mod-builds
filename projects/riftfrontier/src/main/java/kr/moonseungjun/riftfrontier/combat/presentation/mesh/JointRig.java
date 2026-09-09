package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable glTF skin palette used by the project-owned linear-blend runtime.
 * Joint indices in {@link SkinnedTriangleMesh} address this palette directly.
 */
public final class JointRig {
    private final int[] nodeIndices;
    private final int[] parentJoints;
    private final String[] names;
    private final Affine3x4[] inverseBindMatrices;
    private final Affine3x4[] restLocalTransforms;

    public JointRig(
            int[] nodeIndices,
            int[] parentJoints,
            String[] names,
            Affine3x4[] inverseBindMatrices,
            Affine3x4[] restLocalTransforms
    ) {
        this.nodeIndices = copy(nodeIndices, "nodeIndices");
        this.parentJoints = copy(parentJoints, "parentJoints");
        this.names = copy(names, "names");
        this.inverseBindMatrices = copy(inverseBindMatrices, "inverseBindMatrices");
        this.restLocalTransforms = copy(restLocalTransforms, "restLocalTransforms");
        validate();
    }

    public int jointCount() {
        return nodeIndices.length;
    }

    public int nodeIndex(int joint) {
        checkJoint(joint);
        return nodeIndices[joint];
    }

    public int parentJoint(int joint) {
        checkJoint(joint);
        return parentJoints[joint];
    }

    public String name(int joint) {
        checkJoint(joint);
        return names[joint];
    }

    public Affine3x4 inverseBindMatrix(int joint) {
        checkJoint(joint);
        return inverseBindMatrices[joint];
    }

    public Affine3x4 restLocalTransform(int joint) {
        checkJoint(joint);
        return restLocalTransforms[joint];
    }

    public int[] nodeIndices() {
        return nodeIndices.clone();
    }

    public int[] parentJoints() {
        return parentJoints.clone();
    }

    private void validate() {
        int count = nodeIndices.length;
        if (count == 0) {
            throw new IllegalArgumentException("joint rig must contain at least one joint");
        }
        if (parentJoints.length != count || names.length != count
                || inverseBindMatrices.length != count || restLocalTransforms.length != count) {
            throw new IllegalArgumentException("all joint rig arrays must have identical length");
        }

        Set<Integer> uniqueNodes = new HashSet<>();
        for (int joint = 0; joint < count; joint++) {
            if (nodeIndices[joint] < 0 || !uniqueNodes.add(nodeIndices[joint])) {
                throw new IllegalArgumentException("joint node indices must be unique and non-negative");
            }
            if (names[joint] == null || names[joint].isBlank()) {
                throw new IllegalArgumentException("joint names must be non-blank");
            }
            if (inverseBindMatrices[joint] == null || restLocalTransforms[joint] == null) {
                throw new IllegalArgumentException("joint transforms must not be null");
            }
            int parent = parentJoints[joint];
            if (parent < -1 || parent >= count || parent == joint) {
                throw new IllegalArgumentException("invalid parent joint " + parent + " for joint " + joint);
            }
        }

        for (int joint = 0; joint < count; joint++) {
            boolean[] seen = new boolean[count];
            int cursor = joint;
            while (cursor >= 0) {
                if (seen[cursor]) {
                    throw new IllegalArgumentException("joint hierarchy contains a cycle at joint " + cursor);
                }
                seen[cursor] = true;
                cursor = parentJoints[cursor];
            }
        }
    }

    private void checkJoint(int joint) {
        if (joint < 0 || joint >= jointCount()) {
            throw new IndexOutOfBoundsException("joint index outside palette: " + joint);
        }
    }

    private static int[] copy(int[] values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return values.clone();
    }

    private static String[] copy(String[] values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return values.clone();
    }

    private static Affine3x4[] copy(Affine3x4[] values, String name) {
        if (values == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return values.clone();
    }

    @Override
    public String toString() {
        return "JointRig[joints=" + jointCount() + ", nodes=" + Arrays.toString(nodeIndices) + "]";
    }
}
