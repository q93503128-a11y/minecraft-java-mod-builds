package kr.moonseungjun.survivalascension.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persistent one-time marker for the TBS journal restoration migration. */
public final class TbsJournalRestorationData extends SavedData {
    public static final SavedDataType<TbsJournalRestorationData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "tbs_journal_restoration_v1"),
            TbsJournalRestorationData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("restored", List.of()).forGetter(TbsJournalRestorationData::entries)
            ).apply(instance, TbsJournalRestorationData::new))
    );

    private final Set<String> restored = new HashSet<>();

    public TbsJournalRestorationData() {}

    private TbsJournalRestorationData(List<String> restoredPlayers) {
        restored.addAll(restoredPlayers);
    }

    private List<String> entries() {
        List<String> out = new ArrayList<>(restored);
        out.sort(String::compareTo);
        return out;
    }

    public static TbsJournalRestorationData get(ServerPlayer player) {
        return ((ServerLevel) player.level()).getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isRestored(ServerPlayer player) {
        return restored.contains(player.getUUID().toString());
    }

    public void markRestored(ServerPlayer player) {
        if (restored.add(player.getUUID().toString())) {
            setDirty();
        }
    }
}
