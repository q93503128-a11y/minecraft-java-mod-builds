package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One shared bounded earthwork project. This auxiliary data never stores settlement items/currency. */
public final class SettlementCivilWorkData extends SavedData {
    public static final SavedDataType<SettlementCivilWorkData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "civil_work"),
            SettlementCivilWorkData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    CivilWorkState.CODEC.optionalFieldOf("project", CivilWorkState.EMPTY)
                            .forGetter(data -> data.project)
            ).apply(instance, SettlementCivilWorkData::new))
    );

    private CivilWorkState project;

    public SettlementCivilWorkData() { this(CivilWorkState.EMPTY); }
    public SettlementCivilWorkData(CivilWorkState project) { this.project = project == null ? CivilWorkState.EMPTY : project; }

    public static SettlementCivilWorkData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public CivilWorkState project() { return project; }

    public void begin(CivilWorkState next) {
        if (next == null || !next.active()) return;
        project = next;
        setDirty();
    }

    public void replace(CivilWorkState next) {
        next = next == null ? CivilWorkState.EMPTY : next;
        if (project.equals(next)) return;
        project = next;
        setDirty();
    }

    public void clear() {
        if (!project.active()) return;
        project = CivilWorkState.EMPTY;
        setDirty();
    }
}
