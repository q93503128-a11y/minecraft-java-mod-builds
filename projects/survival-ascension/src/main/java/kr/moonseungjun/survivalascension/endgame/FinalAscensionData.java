package kr.moonseungjun.survivalascension.endgame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-level permanent closure state written only after the unique final boss dies. */
public final class FinalAscensionData extends SavedData {
    public static final SavedDataType<FinalAscensionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "final_ascension_v1"),
            FinalAscensionData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("complete", false).forGetter(FinalAscensionData::isComplete),
                    Codec.STRING.optionalFieldOf("first_conqueror", "").forGetter(FinalAscensionData::firstConqueror),
                    Codec.LONG.optionalFieldOf("completed_game_time", 0L).forGetter(FinalAscensionData::completedGameTime)
            ).apply(instance, FinalAscensionData::new))
    );

    private boolean complete;
    private String firstConqueror = "";
    private long completedGameTime;

    public FinalAscensionData() {}

    private FinalAscensionData(boolean complete, String firstConqueror, long completedGameTime) {
        this.complete = complete;
        this.firstConqueror = firstConqueror == null ? "" : firstConqueror;
        this.completedGameTime = Math.max(0L, completedGameTime);
    }

    public static FinalAscensionData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isComplete() { return complete; }
    public String firstConqueror() { return firstConqueror; }
    public long completedGameTime() { return completedGameTime; }

    public boolean complete(ServerPlayer player) {
        if (complete) return false;
        complete = true;
        firstConqueror = player.getUUID().toString();
        completedGameTime = Math.max(0L, player.level().getGameTime());
        setDirty();
        return true;
    }

    public static void sendStatus(ServerPlayer player) {
        FinalAscensionData data = get(((ServerLevel) player.level()).getServer());
        if (data.isComplete()) {
            player.sendSystemMessage(Component.literal("§d[최후의 승천] §a월드 정복 완료 §7· §f최종 숙련 권한이 개방되었습니다."));
        } else {
            player.sendSystemMessage(Component.literal("§d[최후의 승천] §e최종 관문 미완료 §7· §f1~3막 뒤 최심부의 경계를 돌파해야 합니다."));
        }
    }
}
