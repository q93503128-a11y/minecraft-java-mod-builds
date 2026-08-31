package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Compatibility projection for the old FieldSession API.
 *
 * The alpha.16 64x64 village/field generator is retired from live v0.4 gameplay. Radia is now the only hub and
 * SouthgateChapterWorld authors M01/M02 directly in the canonical Southgate region. This type remains temporarily
 * so the already-tested patrol/session code can consume the same immutable position bundle without a risky rewrite.
 */
public final class StarterSliceWorld {
    public static final int LAYOUT_VERSION = 18;

    public record BuiltSlice(
            int baseY,
            Vec3 spawn,
            Vec3 npc,
            Vec3 relay,
            Vec3 m01Home,
            Vec3 m01End,
            Vec3 m02Home,
            Vec3 m02End
    ) {}

    /** Legacy enum retained for source compatibility; live bootstrap no longer performs staged starter-slice generation. */
    public enum BuildStage { DONE }

    /** Legacy job retained only for API compatibility with old tooling. It performs no world writes. */
    public static final class BuildJob {
        private BuildJob() {}
        public int baseY() { return 65; }
        public BuildStage stage() { return BuildStage.DONE; }
        public boolean done() { return true; }
        public String stageLabel() { return "라디아 진입 준비"; }
        public int progressPercent() { return 100; }
        public boolean tick(ServerLevel level, int columnBudget) { return true; }
        public BuiltSlice result() { return built(); }
    }

    private StarterSliceWorld() {}

    public static BuildJob begin(ServerLevel level) { return new BuildJob(); }
    public static BuiltSlice findExisting(ServerLevel level) { return built(); }
    public static BuiltSlice build(ServerLevel level) { return built(); }

    /** Early Southgate corridor only; the rest of Chapter 1 is owned by SouthgateChapterWorld.contains(). */
    public static boolean contains(BuiltSlice slice, Vec3 position) {
        return position != null
                && position.x >= -42 && position.x <= 58
                && position.z >= 118 && position.z <= 198
                && position.y >= 54 && position.y <= 90;
    }

    private static BuiltSlice built() {
        return new BuiltSlice(
                65,
                new Vec3(0.5, 66.0, 123.5),
                new Vec3(-6.5, 66.0, 132.5),
                new Vec3(0.5, 66.0, 126.5),
                new Vec3(-12.0, 66.0, 151.0),
                new Vec3(-2.0, 66.0, 157.0),
                new Vec3(13.0, 67.0, 173.0),
                new Vec3(2.0, 67.0, 181.0));
    }
}
