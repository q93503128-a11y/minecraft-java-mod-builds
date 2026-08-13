package kr.moonseungjun.livingkingdoms.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Prevents an uninitialized empty barrel from overwriting saved producer stock. */
public final class ErdenKingdomExteriorContainerSavedData extends SavedData {
    public static final int REVISION = 2;

    private static final Codec<ErdenKingdomExteriorContainerSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("revision", 0).forGetter(data -> data.revision),
            Codec.STRING.listOf().optionalFieldOf("materialized_nodes", List.of())
                    .forGetter(data -> List.copyOf(data.materializedNodes)),
            Codec.STRING.listOf().optionalFieldOf("captured_nodes", List.of())
                    .forGetter(data -> List.copyOf(data.capturedNodes)),
            Codec.LONG.optionalFieldOf("captures", 0L).forGetter(data -> data.captures),
            Codec.LONG.optionalFieldOf("writes", 0L).forGetter(data -> data.writes)
    ).apply(instance, ErdenKingdomExteriorContainerSavedData::new));

    public static final SavedDataType<ErdenKingdomExteriorContainerSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(LivingKingdoms.MOD_ID, "erden_kingdom_exterior_containers"),
            level -> new ErdenKingdomExteriorContainerSavedData(),
            level -> CODEC
    );

    private int revision;
    private final Set<String> materializedNodes;
    private final Set<String> capturedNodes;
    private long captures;
    private long writes;

    public ErdenKingdomExteriorContainerSavedData() {
        this(REVISION, List.of(), List.of(), 0L, 0L);
    }

    private ErdenKingdomExteriorContainerSavedData(
            int revision,
            List<String> materializedNodes,
            List<String> capturedNodes,
            long captures,
            long writes) {
        this.revision = Math.max(0, revision);
        this.materializedNodes = new HashSet<>(materializedNodes);
        this.capturedNodes = new HashSet<>(capturedNodes);
        this.captures = Math.max(0L, captures);
        this.writes = Math.max(0L, writes);
        ensureRevision();
    }

    public boolean isMaterialized(String nodeId) {
        ensureRevision();
        return materializedNodes.contains(nodeId);
    }

    public void markMaterialized(String nodeId) {
        ensureRevision();
        if (materializedNodes.add(nodeId)) setDirty();
    }

    public boolean isCaptured(String nodeId) {
        ensureRevision();
        return capturedNodes.contains(nodeId);
    }

    /** Records that this exact producer has been read back from its physical barrel at least once. */
    public void markCaptured(String nodeId) {
        ensureRevision();
        capturedNodes.add(nodeId);
        captures++;
        setDirty();
    }

    public void recordWrite() {
        ensureRevision();
        writes++;
        setDirty();
    }

    public int materializedCount() {
        ensureRevision();
        return materializedNodes.size();
    }

    public int capturedCount() {
        ensureRevision();
        return capturedNodes.size();
    }

    public long captures() {
        return captures;
    }

    public long writes() {
        return writes;
    }

    private void ensureRevision() {
        if (revision == REVISION) return;
        // Revision 1 already proved its materialized barrels were initialized. Preserve that fact
        // across migration so an existing world never treats a valid physical barrel as pristine.
        // Capture proof is deliberately rebuilt from live containers under revision 2.
        revision = REVISION;
        capturedNodes.clear();
        captures = 0L;
        writes = 0L;
        setDirty();
    }
}
