package kr.moonseungjun.villageguardians;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class VillageSavedData extends SavedData {
    private static final int NO_CENTER = Integer.MIN_VALUE;

    private static final Codec<VillageSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("mayor_id", "").forGetter(data -> data.mayorId),
            Codec.STRING.optionalFieldOf("mayor_name", "없음").forGetter(data -> data.mayorName),
            Codec.INT.optionalFieldOf("village_day", 1).forGetter(data -> data.villageDay),
            Codec.STRING.optionalFieldOf("time_phase", VillageTimePhase.DAY.name()).forGetter(data -> data.timePhase),
            Codec.INT.optionalFieldOf("village_center_x", NO_CENTER).forGetter(data -> data.villageCenterX),
            Codec.INT.optionalFieldOf("village_center_y", NO_CENTER).forGetter(data -> data.villageCenterY),
            Codec.INT.optionalFieldOf("village_center_z", NO_CENTER).forGetter(data -> data.villageCenterZ),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("roles", Map.of())
                    .forGetter(data -> data.roles),
            Codec.unboundedMap(Codec.STRING, RpgProgress.CODEC)
                    .optionalFieldOf("rpg_progression", Map.of())
                    .forGetter(data -> data.rpgProgression)
    ).apply(instance, VillageSavedData::new));

    public static final SavedDataType<VillageSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "village_council"),
            level -> new VillageSavedData(),
            level -> CODEC);

    private String mayorId;
    private String mayorName;
    private int villageDay;
    private String timePhase;
    private int villageCenterX;
    private int villageCenterY;
    private int villageCenterZ;
    private Map<String, String> roles;
    private Map<String, RpgProgress> rpgProgression;

    public VillageSavedData() {
        this("", "없음", 1, VillageTimePhase.DAY.name(),
                NO_CENTER, NO_CENTER, NO_CENTER, Map.of(), Map.of());
    }

    private VillageSavedData(
            String mayorId,
            String mayorName,
            int villageDay,
            String timePhase,
            int villageCenterX,
            int villageCenterY,
            int villageCenterZ,
            Map<String, String> roles,
            Map<String, RpgProgress> rpgProgression) {
        this.mayorId = mayorId;
        this.mayorName = mayorName;
        this.villageDay = Math.max(1, villageDay);
        this.timePhase = timePhase;
        this.villageCenterX = villageCenterX;
        this.villageCenterY = villageCenterY;
        this.villageCenterZ = villageCenterZ;
        this.roles = new LinkedHashMap<>(roles);
        this.rpgProgression = new LinkedHashMap<>(rpgProgression);
    }

    public Optional<UUID> mayorId() {
        if (mayorId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(mayorId));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public String mayorName() {
        return mayorName;
    }

    public int villageDay() {
        return Math.max(1, villageDay);
    }

    public VillageTimePhase timePhase() {
        return "NIGHT".equalsIgnoreCase(timePhase) ? VillageTimePhase.NIGHT : VillageTimePhase.DAY;
    }

    public Optional<BlockPos> villageCenter() {
        if (villageCenterX == NO_CENTER || villageCenterY == NO_CENTER || villageCenterZ == NO_CENTER) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(villageCenterX, villageCenterY, villageCenterZ));
    }

    public Map<UUID, VillageRole> roles() {
        Map<UUID, VillageRole> parsed = new LinkedHashMap<>();
        roles.forEach((uuidText, roleId) -> {
            try {
                UUID uuid = UUID.fromString(uuidText);
                VillageRole.parse(roleId).ifPresent(role -> parsed.put(uuid, role));
            } catch (IllegalArgumentException ignored) {
                // Ignore a corrupted entry while preserving all other village data.
            }
        });
        return parsed;
    }

    public Map<UUID, RpgProgress> rpgProgression() {
        Map<UUID, RpgProgress> parsed = new LinkedHashMap<>();
        rpgProgression.forEach((uuidText, progress) -> {
            try {
                parsed.put(UUID.fromString(uuidText), progress == null ? RpgProgress.initial() : progress);
            } catch (IllegalArgumentException ignored) {
                // Ignore only the damaged player entry.
            }
        });
        return parsed;
    }

    public void replaceState(
            UUID mayorId,
            String mayorName,
            int villageDay,
            VillageTimePhase timePhase,
            BlockPos villageCenter,
            Map<UUID, VillageRole> roles,
            Map<UUID, RpgProgress> rpgProgression) {
        this.mayorId = mayorId == null ? "" : mayorId.toString();
        this.mayorName = mayorName == null || mayorName.isBlank() ? "없음" : mayorName;
        this.villageDay = Math.max(1, villageDay);
        this.timePhase = timePhase == null ? VillageTimePhase.DAY.name() : timePhase.name();

        if (villageCenter == null) {
            this.villageCenterX = NO_CENTER;
            this.villageCenterY = NO_CENTER;
            this.villageCenterZ = NO_CENTER;
        } else {
            this.villageCenterX = villageCenter.getX();
            this.villageCenterY = villageCenter.getY();
            this.villageCenterZ = villageCenter.getZ();
        }

        Map<String, String> encodedRoles = new LinkedHashMap<>();
        roles.forEach((uuid, role) -> encodedRoles.put(uuid.toString(), role.id()));
        this.roles = encodedRoles;

        Map<String, RpgProgress> encodedProgression = new LinkedHashMap<>();
        rpgProgression.forEach((uuid, progress) -> encodedProgression.put(uuid.toString(), progress));
        this.rpgProgression = encodedProgression;
        setDirty();
    }
}
