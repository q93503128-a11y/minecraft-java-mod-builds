package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-game-time cooldowns survive logout, relog, class change and restart.
 * Link Drive is an owner-wide timestamp, not an individual mob attribute that
 * can be reset by storing/deploying the same blueprint.
 */
public final class PuppetAbilitySavedData extends SavedData {
    private static final String FILE = "anomalousclasses_puppet_abilities";
    private static final Factory<PuppetAbilitySavedData> FACTORY = new Factory<>(
            PuppetAbilitySavedData::new, PuppetAbilitySavedData::load, DataFixTypes.LEVEL);
    private static final long[] COOLDOWN_TICKS = {320L, 480L, 600L};
    private static final long DRIVE_TICKS = 120L;

    public record AbilityState(long pullReady, long stitchReady, long driveReady, long driveUntil) {
        public AbilityState {
            if (pullReady < 0 || stitchReady < 0 || driveReady < 0 || driveUntil < 0)
                throw new IllegalArgumentException("Invalid ability state");
        }
        public long cooldown(int ability) {
            return switch (ability) {
                case 0 -> pullReady;
                case 1 -> stitchReady;
                case 2 -> driveReady;
                default -> Long.MAX_VALUE;
            };
        }
        public AbilityState use(int skill, long now) {
            long ready = now > Long.MAX_VALUE - COOLDOWN_TICKS[skill]
                    ? Long.MAX_VALUE : now + COOLDOWN_TICKS[skill];
            return switch (skill) {
                case 0 -> new AbilityState(ready, stitchReady, driveReady, driveUntil);
                case 1 -> new AbilityState(pullReady, ready, driveReady, driveUntil);
                case 2 -> new AbilityState(pullReady, stitchReady, ready,
                        now > Long.MAX_VALUE - DRIVE_TICKS ? Long.MAX_VALUE : now + DRIVE_TICKS);
                default -> this;
            };
        }
    }

    private final Map<UUID, AbilityState> owners = new HashMap<>();

    public static PuppetAbilitySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE);
    }

    public static long gameTime(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    public AbilityState state(UUID owner) {
        return owners.getOrDefault(owner, new AbilityState(0, 0, 0, 0));
    }

    public boolean ready(UUID owner, int ability, long now) {
        return ability >= 0 && ability < COOLDOWN_TICKS.length
                && now >= state(owner).cooldown(ability);
    }

    public boolean driveActive(UUID owner, long now) {
        return now < state(owner).driveUntil();
    }

    /** Call only after game-world validation and cost commitment succeed. */
    public boolean use(UUID owner, int skill, long now) {
        if (owner == null || !ready(owner, skill, now)) return false;
        owners.put(owner, state(owner).use(skill, now));
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag saved = new ListTag();
        owners.forEach((id, state) -> {
            CompoundTag owner = new CompoundTag();
            owner.putUUID("owner", id);
            owner.putLong("pull", state.pullReady());
            owner.putLong("stitch", state.stitchReady());
            owner.putLong("drive", state.driveReady());
            owner.putLong("until", state.driveUntil());
            saved.add(owner);
        });
        tag.put("owners", saved);
        return tag;
    }

    private static PuppetAbilitySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PuppetAbilitySavedData saved = new PuppetAbilitySavedData();
        for (Tag entry : tag.getList("owners", Tag.TAG_COMPOUND)) {
            if (!(entry instanceof CompoundTag data) || !data.hasUUID("owner")) continue;
            try {
                saved.owners.putIfAbsent(data.getUUID("owner"), new AbilityState(
                        data.getLong("pull"), data.getLong("stitch"),
                        data.getLong("drive"), data.getLong("until")));
            } catch (IllegalArgumentException ignored) {
                // Corrupted cooldowns never grant arbitrary combat advantage.
            }
        }
        return saved;
    }
}
