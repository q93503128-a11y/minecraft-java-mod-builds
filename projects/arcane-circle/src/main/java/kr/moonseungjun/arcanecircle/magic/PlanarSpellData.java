package kr.moonseungjun.arcanecircle.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistent return anchors for the Demiplane spell. */
public final class PlanarSpellData extends SavedData {
    public record ReturnAnchor(String uuid, String dimension, double x, double y, double z, float yaw, float pitch) {
        private static final Codec<ReturnAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(ReturnAnchor::uuid),
                Codec.STRING.fieldOf("dimension").forGetter(ReturnAnchor::dimension),
                Codec.DOUBLE.fieldOf("x").forGetter(ReturnAnchor::x),
                Codec.DOUBLE.fieldOf("y").forGetter(ReturnAnchor::y),
                Codec.DOUBLE.fieldOf("z").forGetter(ReturnAnchor::z),
                Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(ReturnAnchor::yaw),
                Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(ReturnAnchor::pitch)
        ).apply(instance, ReturnAnchor::new));
    }

    public static final SavedDataType<PlanarSpellData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "planar_spell_v1"),
            PlanarSpellData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    ReturnAnchor.CODEC.listOf().optionalFieldOf("returns", List.of())
                            .forGetter(PlanarSpellData::entries)
            ).apply(instance, PlanarSpellData::new))
    );

    private final Map<String, ReturnAnchor> returns = new HashMap<>();

    public PlanarSpellData() {}

    private PlanarSpellData(List<ReturnAnchor> entries) {
        for (ReturnAnchor entry : entries) returns.put(entry.uuid(), entry);
    }

    public static PlanarSpellData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<ReturnAnchor> entries() { return List.copyOf(returns.values()); }

    /** Demiplane currently binds to the three canonical Minecraft planes it can actually traverse. */
    public static boolean canRemember(ServerPlayer player) {
        return player.level().dimension().equals(Level.OVERWORLD)
                || player.level().dimension().equals(Level.NETHER)
                || player.level().dimension().equals(Level.END);
    }

    public void remember(ServerPlayer player) {
        String dimension = dimensionId(player);
        if (dimension == null) return;
        returns.put(player.getUUID().toString(), new ReturnAnchor(
                player.getUUID().toString(), dimension,
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        setDirty();
    }

    private static String dimensionId(ServerPlayer player) {
        if (player.level().dimension().equals(Level.OVERWORLD)) return "minecraft:overworld";
        if (player.level().dimension().equals(Level.NETHER)) return "minecraft:the_nether";
        if (player.level().dimension().equals(Level.END)) return "minecraft:the_end";
        return null;
    }

    public Optional<ReturnAnchor> anchor(ServerPlayer player) {
        return Optional.ofNullable(returns.get(player.getUUID().toString()));
    }

    public void clear(ServerPlayer player) {
        if (returns.remove(player.getUUID().toString()) != null) setDirty();
    }
}
