package kr.moonseungjun.arcanecircle.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ArcaneWorldData extends SavedData {
    private record PlayerEntry(String uuid, long marks, String tradition, boolean arrived) {
        private static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                Codec.LONG.optionalFieldOf("marks", 0L).forGetter(PlayerEntry::marks),
                Codec.STRING.optionalFieldOf("tradition", "UNBOUND").forGetter(PlayerEntry::tradition),
                Codec.BOOL.optionalFieldOf("arrived", false).forGetter(PlayerEntry::arrived)
        ).apply(instance, PlayerEntry::new));
    }

    public static final SavedDataType<ArcaneWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "arcane_world_v1"),
            ArcaneWorldData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ArcaneWorldData::entries),
                    Codec.BOOL.optionalFieldOf("academy_built", false).forGetter(value -> value.academyBuilt),
                    Codec.INT.optionalFieldOf("academy_x", 0).forGetter(value -> value.academyOrigin.getX()),
                    Codec.INT.optionalFieldOf("academy_y", 80).forGetter(value -> value.academyOrigin.getY()),
                    Codec.INT.optionalFieldOf("academy_z", 0).forGetter(value -> value.academyOrigin.getZ())
            ).apply(instance, ArcaneWorldData::new))
    );

    private static final class Profile {
        long marks;
        MagicTradition tradition = MagicTradition.UNBOUND;
        boolean arrived;
    }

    private final Map<String, Profile> players = new HashMap<>();
    private boolean academyBuilt;
    private BlockPos academyOrigin = new BlockPos(0, 80, 0);

    public ArcaneWorldData() {}

    private ArcaneWorldData(List<PlayerEntry> entries, boolean built, int x, int y, int z) {
        for (PlayerEntry entry : entries) {
            Profile profile = new Profile();
            profile.marks = Math.max(0L, entry.marks());
            profile.tradition = MagicTradition.parse(entry.tradition());
            profile.arrived = entry.arrived();
            players.put(entry.uuid(), profile);
        }
        academyBuilt = built;
        academyOrigin = new BlockPos(x, y, z);
    }

    public static ArcaneWorldData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private Profile profile(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID().toString(), ignored -> {
            setDirty();
            return new Profile();
        });
    }

    public long balance(ServerPlayer player) { return profile(player).marks; }

    public long addMarks(ServerPlayer player, long amount) {
        if (amount <= 0L) return balance(player);
        Profile profile = profile(player);
        profile.marks = Math.min(9_000_000_000L, profile.marks + amount);
        setDirty();
        return profile.marks;
    }

    public boolean spendMarks(ServerPlayer player, long amount) {
        if (amount < 0L) return false;
        Profile profile = profile(player);
        if (profile.marks < amount) return false;
        profile.marks -= amount;
        setDirty();
        return true;
    }

    public MagicTradition tradition(ServerPlayer player) { return profile(player).tradition; }

    public boolean chooseTradition(ServerPlayer player, MagicTradition tradition, long attunementCost) {
        if (tradition == null || tradition == MagicTradition.UNBOUND || attunementCost < 0L) return false;
        Profile profile = profile(player);
        if (profile.tradition == tradition) return true;
        if (!spendMarks(player, attunementCost)) return false;
        profile.tradition = tradition;
        setDirty();
        return true;
    }

    public boolean claimFirstArrival(ServerPlayer player) {
        Profile profile = profile(player);
        if (profile.arrived) return false;
        profile.arrived = true;
        setDirty();
        return true;
    }

    public boolean academyBuilt() { return academyBuilt; }
    public BlockPos academyOrigin() { return academyOrigin; }

    public void setAcademy(BlockPos origin) {
        academyBuilt = true;
        academyOrigin = origin.immutable();
        setDirty();
    }

    private List<PlayerEntry> entries() {
        return players.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new PlayerEntry(entry.getKey(), entry.getValue().marks,
                        entry.getValue().tradition.name(), entry.getValue().arrived))
                .toList();
    }
}
