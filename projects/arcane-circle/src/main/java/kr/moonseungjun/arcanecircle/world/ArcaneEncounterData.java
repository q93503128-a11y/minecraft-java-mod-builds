package kr.moonseungjun.arcanecircle.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Permanent encounter, named-NPC death and faction ranking state. */
public final class ArcaneEncounterData extends SavedData {
    private record PlayerRecord(String uuid, String name, String tradition, int circle) {
        private static final Codec<PlayerRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerRecord::uuid),
                Codec.STRING.optionalFieldOf("name", "마도사").forGetter(PlayerRecord::name),
                Codec.STRING.optionalFieldOf("tradition", "UNBOUND").forGetter(PlayerRecord::tradition),
                Codec.INT.optionalFieldOf("circle", 1).forGetter(PlayerRecord::circle)
        ).apply(instance, PlayerRecord::new));
    }

    public static final SavedDataType<ArcaneEncounterData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "arcane_encounters_v1"),
            ArcaneEncounterData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("built_zones", List.of())
                            .forGetter(value -> value.builtZones.stream().sorted().toList()),
                    Codec.STRING.listOf().optionalFieldOf("defeated_bosses", List.of())
                            .forGetter(value -> value.defeatedBosses.stream().sorted().toList()),
                    Codec.STRING.listOf().optionalFieldOf("dead_named", List.of())
                            .forGetter(value -> value.deadNamed.stream().sorted().toList()),
                    PlayerRecord.CODEC.listOf().optionalFieldOf("players", List.of())
                            .forGetter(ArcaneEncounterData::playerEntries)
            ).apply(instance, ArcaneEncounterData::new))
    );

    private static final class PlayerPower {
        String name = "마도사";
        MagicTradition tradition = MagicTradition.UNBOUND;
        int circle = 1;
    }

    private final Set<String> builtZones = new HashSet<>();
    private final Set<String> defeatedBosses = new HashSet<>();
    private final Set<String> deadNamed = new HashSet<>();
    private final Map<String, PlayerPower> players = new HashMap<>();

    public ArcaneEncounterData() {}

    private ArcaneEncounterData(List<String> built, List<String> defeated, List<String> dead,
                                List<PlayerRecord> records) {
        builtZones.addAll(built);
        defeatedBosses.addAll(defeated);
        deadNamed.addAll(dead);
        for (PlayerRecord record : records) {
            PlayerPower power = new PlayerPower();
            power.name = record.name();
            power.tradition = MagicTradition.parse(record.tradition());
            power.circle = Math.max(1, Math.min(9, record.circle()));
            players.put(record.uuid(), power);
        }
    }

    public static ArcaneEncounterData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public void updatePlayer(ServerPlayer player) {
        PlayerPower power = players.computeIfAbsent(player.getUUID().toString(), ignored -> new PlayerPower());
        MagicTradition tradition = ArcaneWorldData.get(((ServerLevel) player.level()).getServer()).tradition(player);
        int circle = MagicPlayerData.get(((ServerLevel) player.level()).getServer()).state(player).circle();
        String name = player.getScoreboardName();
        if (power.circle != circle || power.tradition != tradition || !power.name.equals(name)) {
            power.circle = circle;
            power.tradition = tradition;
            power.name = name;
            setDirty();
        }
    }

    public boolean isZoneBuilt(String id) { return builtZones.contains(id); }
    public void markZoneBuilt(String id) { if (builtZones.add(id)) setDirty(); }
    public boolean isBossDefeated(String id) { return defeatedBosses.contains(id); }
    public void markBossDefeated(String id) { if (defeatedBosses.add(id)) setDirty(); }
    public boolean isNamedDead(String id) { return deadNamed.contains(id); }
    public void markNamedDead(String id) { if (deadNamed.add(id)) setDirty(); }

    public Champion champion(MagicTradition tradition) {
        FactionProfile.Entry representative = FactionProfile.of(tradition);
        String bestName = isNamedDead(representative.representativeId()) ? "공석" : representative.representativeName();
        int bestCircle = isNamedDead(representative.representativeId()) ? 0 : representative.representativeCircle();
        for (PlayerPower power : players.values()) {
            if (power.tradition != tradition) continue;
            if (power.circle > bestCircle || (power.circle == bestCircle
                    && !"공석".equals(bestName) && power.name.compareTo(bestName) < 0)) {
                bestName = power.name;
                bestCircle = power.circle;
            }
        }
        return new Champion(bestName, bestCircle);
    }

    private List<PlayerRecord> playerEntries() {
        List<PlayerRecord> result = new ArrayList<>();
        players.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            PlayerPower value = entry.getValue();
            result.add(new PlayerRecord(entry.getKey(), value.name, value.tradition.name(), value.circle));
        });
        return result;
    }

    public record Champion(String name, int circle) {}
}
