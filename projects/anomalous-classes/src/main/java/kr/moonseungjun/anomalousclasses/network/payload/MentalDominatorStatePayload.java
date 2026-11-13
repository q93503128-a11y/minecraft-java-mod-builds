package kr.moonseungjun.anomalousclasses.network.payload;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MentalDominatorStatePayload(ProfileStatePayload profile, double psyche, double maximumPsyche,
                                          double upkeepPerSecond, int controlledCount, int maxControlled,
                                          int revision, boolean openScreen, boolean codexView, int resultCode, List<MobView> mobs,
                                          List<String> codex) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 2048;
    private static final StreamCodec<ByteBuf, MobView> MOB_CODEC = StreamCodec.of(
            (buffer, mob) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, mob.id());
                ByteBufCodecs.STRING_UTF8.encode(buffer, mob.name());
                ByteBufCodecs.STRING_UTF8.encode(buffer, mob.type());
                ByteBufCodecs.VAR_INT.encode(buffer, mob.command());
                ByteBufCodecs.VAR_INT.encode(buffer, mob.status());
                ByteBufCodecs.BOOL.encode(buffer, mob.allowPlayerDamage());
                ByteBufCodecs.BOOL.encode(buffer, mob.hasLiveHealth());
                ByteBufCodecs.FLOAT.encode(buffer, mob.health());
                ByteBufCodecs.FLOAT.encode(buffer, mob.maxHealth());
                ByteBufCodecs.DOUBLE.encode(buffer, mob.upkeepPerSecond());
                ByteBufCodecs.STRING_UTF8.encode(buffer, mob.dimension());
                ByteBufCodecs.VAR_INT.encode(buffer, mob.x());
                ByteBufCodecs.VAR_INT.encode(buffer, mob.y());
                ByteBufCodecs.VAR_INT.encode(buffer, mob.z());
            }, buffer -> new MobView(ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer)));

    public static final Type<MentalDominatorStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, "mental_dominator_state"));
    public static final StreamCodec<ByteBuf, MentalDominatorStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                ProfileStatePayload.STREAM_CODEC.encode(buffer, value.profile);
                ByteBufCodecs.DOUBLE.encode(buffer, value.psyche);
                ByteBufCodecs.DOUBLE.encode(buffer, value.maximumPsyche);
                ByteBufCodecs.DOUBLE.encode(buffer, value.upkeepPerSecond);
                ByteBufCodecs.VAR_INT.encode(buffer, value.controlledCount);
                ByteBufCodecs.VAR_INT.encode(buffer, value.maxControlled);
                ByteBufCodecs.VAR_INT.encode(buffer, value.revision);
                ByteBufCodecs.BOOL.encode(buffer, value.openScreen);
                ByteBufCodecs.BOOL.encode(buffer, value.codexView);
                ByteBufCodecs.VAR_INT.encode(buffer, value.resultCode);
                ByteBufCodecs.VAR_INT.encode(buffer, value.mobs.size());
                value.mobs.forEach(mob -> MOB_CODEC.encode(buffer, mob));
                ByteBufCodecs.VAR_INT.encode(buffer, value.codex.size());
                value.codex.forEach(species -> ByteBufCodecs.STRING_UTF8.encode(buffer, species));
            }, buffer -> {
                ProfileStatePayload profile = ProfileStatePayload.STREAM_CODEC.decode(buffer);
                double psyche = finite(ByteBufCodecs.DOUBLE.decode(buffer));
                double maximum = finite(ByteBufCodecs.DOUBLE.decode(buffer));
                double upkeep = finite(ByteBufCodecs.DOUBLE.decode(buffer));
                int count = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                int maxCount = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                int revision = Math.max(0, ByteBufCodecs.VAR_INT.decode(buffer));
                boolean openScreen = ByteBufCodecs.BOOL.decode(buffer);
                boolean codexView = ByteBufCodecs.BOOL.decode(buffer);
                int result = ByteBufCodecs.VAR_INT.decode(buffer);
                int mobCount = Math.max(0, Math.min(MAX_ENTRIES, ByteBufCodecs.VAR_INT.decode(buffer)));
                java.util.ArrayList<MobView> mobs = new java.util.ArrayList<>(mobCount);
                for (int index = 0; index < mobCount; index++) mobs.add(MOB_CODEC.decode(buffer));
                int codexCount = Math.max(0, Math.min(MAX_ENTRIES, ByteBufCodecs.VAR_INT.decode(buffer)));
                java.util.ArrayList<String> codex = new java.util.ArrayList<>(codexCount);
                for (int index = 0; index < codexCount; index++) codex.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
                return new MentalDominatorStatePayload(profile, psyche, maximum, upkeep, count, maxCount,
                        revision, openScreen, codexView, result, mobs, codex);
            });

    public MentalDominatorStatePayload {
        psyche = finite(psyche);
        maximumPsyche = Math.max(0.0, finite(maximumPsyche));
        upkeepPerSecond = Math.max(0.0, finite(upkeepPerSecond));
        controlledCount = Math.max(0, controlledCount);
        maxControlled = Math.max(0, maxControlled);
        revision = Math.max(0, revision);
        resultCode = Math.max(0, Math.min(8, resultCode));
        mobs = mobs == null ? List.of() : List.copyOf(mobs.subList(0, Math.min(MAX_ENTRIES, mobs.size())));
        codex = codex == null ? List.of() : codex.stream().limit(MAX_ENTRIES).map(value -> safe(value, 128)).toList();
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record MobView(String id, String name, String type, int command, int status,
                          boolean allowPlayerDamage, boolean hasLiveHealth, float health, float maxHealth,
                          double upkeepPerSecond, String dimension, int x, int y, int z) {
        public MobView {
            id = safe(id, 36); name = safe(name, 128); type = safe(type, 128); dimension = safe(dimension, 128);
            command = command == 1 ? 1 : 0;
            status = Math.max(0, Math.min(3, status));
            health = Float.isFinite(health) ? Math.max(0.0F, health) : 0.0F;
            maxHealth = Float.isFinite(maxHealth) ? Math.max(0.0F, maxHealth) : 0.0F;
            upkeepPerSecond = Math.max(0.0, finite(upkeepPerSecond));
        }

        public UUID uuid() { return UUID.fromString(id); }
    }

    private static String safe(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0; }
}
