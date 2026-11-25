package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-only, per-owner counts and user-authored tactics, never client authority. */
public final class SwarmSavedData extends SavedData {
    private static final String KEY = "anomalousclasses_swarm";
    private static final Factory<SwarmSavedData> FACTORY =
            new Factory<>(SwarmSavedData::new, SwarmSavedData::load, DataFixTypes.LEVEL);
    private final Map<UUID, State> players = new HashMap<>();

    public static SwarmSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, KEY);
    }

    public State player(UUID id) {
        return players.computeIfAbsent(id, ignored -> new State());
    }

    public void changed() { setDirty(); }

    public static final class State {
        private int alive = 16;
        private int attack;
        private int guard;
        private int lift;
        private int regroup = 16;
        private long lastRegeneration = -1L;
        private long nextRushTick;
        private long nextSacrificeTick;
        private final EnumMap<SwarmNodeRole, Double> pendingDamage =
                new EnumMap<>(SwarmNodeRole.class);
        private String active = "";
        private final LinkedHashMap<String, Integer> tactics = new LinkedHashMap<>();

        public int alive() { return alive; }
        public int attack() { return attack; }
        public int guard() { return guard; }
        public int lift() { return lift; }
        public int regroup() { return regroup; }
        public String activeName() { return active; }
        public Map<String, Integer> tactics() { return Map.copyOf(tactics); }

        public long cooldownRemaining(int ability, long now) {
            long next = ability == 1 ? nextRushTick
                    : ability == 2 ? nextSacrificeTick : 0L;
            return Math.max(0L, next - now);
        }

        /** Called only after all server validation and actual ability acceptance. */
        public boolean commitCooldown(int ability, long now, long duration) {
            if (now < 0 || duration <= 0 || (ability != 1 && ability != 2)
                    || cooldownRemaining(ability, now) > 0L) return false;
            if (ability == 1) nextRushTick = now + duration;
            else nextSacrificeTick = now + duration;
            return true;
        }

        public double pendingDamage(SwarmNodeRole role) {
            return pendingDamage.getOrDefault(role, 0.0);
        }

        /** Persistent fractional injury: one weak arrow does not guarantee a kill. */
        public int applyDamage(SwarmNodeRole role, float incoming, double unitDurability) {
            if (!(incoming > 0.0F) || !Float.isFinite(incoming)
                    || !Double.isFinite(unitDurability) || unitDurability <= 0
                    || count(role) == 0) return 0;
            double amount = pendingDamage(role) + Math.min(100000.0, incoming);
            int before = count(role);
            int casualties = (int) Math.min(before, Math.floor(amount / unitDurability + 1.0e-9));
            if (casualties > 0) lose(role, casualties);
            double leftover = count(role) == 0 ? 0.0 :
                    Math.max(0.0, amount - casualties * unitDurability);
            pendingDamage.put(role, leftover);
            verify();
            return casualties;
        }

        private void transfer(SwarmNodeRole from, SwarmNodeRole to, int units) {
            if (units < 0 || units > count(from) || from == to)
                throw new IllegalArgumentException("invalid role transfer");
            if (units == 0) return;
            boolean wholeRole = units == count(from);
            switch (from) {
                case ATTACK -> attack -= units;
                case GUARD -> guard -= units;
                case LIFT -> lift -= units;
                case REGROUP -> regroup -= units;
            }
            switch (to) {
                case ATTACK -> attack += units;
                case GUARD -> guard += units;
                case LIFT -> lift += units;
                case REGROUP -> regroup += units;
            }
            // If the last source organism moves, its carried injury must
            // not vanish with the now-empty role. Otherwise it stays behind.
            if (wholeRole) {
                double injury = pendingDamage(from);
                pendingDamage.remove(from);
                if (injury > 0) pendingDamage.merge(to, injury, Double::sum);
            }
        }

        public int count(SwarmNodeRole role) {
            return switch (role) {
                case ATTACK -> attack;
                case GUARD -> guard;
                case LIFT -> lift;
                case REGROUP -> regroup;
            };
        }

        public boolean saveTactic(String name, int percent) {
            if (name == null || name.isBlank() || name.length() > 24 || percent < 0 || percent > 100
                    || name.chars().anyMatch(Character::isISOControl)
                    || (!tactics.containsKey(name) && tactics.size() >= 8)) return false;
            tactics.put(name, percent);
            active = name;
            return true;
        }

        public boolean activate(String name) {
            if (!tactics.containsKey(name)) return false;
            active = name;
            return true;
        }

        public boolean removeTactic(String name) {
            if (tactics.remove(name) == null) return false;
            if (name.equals(active)) active = "";
            return true;
        }

        public boolean advance(long time, int maximum) {
            return advance(time, maximum, 40, 2);
        }

        public boolean advance(long time, int maximum, int regenTicks, int transferBudget) {
            boolean changed = false;
            regenTicks = Math.clamp(regenTicks, 20, 200);
            transferBudget = Math.clamp(transferBudget, 1, 8);
            int max = Math.max(1, maximum);
            if (alive > max) {
                int excess = alive - max;
                excess = trim(excess);
                alive -= excess;
                changed = true;
            }
            if (lastRegeneration < 0 || time < lastRegeneration) {
                lastRegeneration = time;
                changed = true;
            } else if (time - lastRegeneration >= regenTicks) {
                lastRegeneration = time;
                if (alive < max) {
                    alive++;
                    regroup++;
                    changed = true;
                }
            }
            int intendedAttack = 0;
            int intendedGuard = 0;
            if (tactics.containsKey(active)) {
                SwarmRules.Split wanted = SwarmRules.distribute(alive, lift, 0, tactics.get(active));
                intendedAttack = wanted.attack();
                intendedGuard = wanted.guard();
            }
            int previouslyRegrouping = regroup;
            int out = Math.min(transferBudget, Math.max(0, attack - intendedAttack));
            transfer(SwarmNodeRole.ATTACK, SwarmNodeRole.REGROUP, out);
            int outGuard = Math.min(transferBudget - out, Math.max(0, guard - intendedGuard));
            transfer(SwarmNodeRole.GUARD, SwarmNodeRole.REGROUP, outGuard);
            if (out + outGuard > 0) changed = true;
            if (previouslyRegrouping > 0 && out + outGuard == 0) {
                int available = Math.min(transferBudget, previouslyRegrouping);
                int addAttack = Math.min(available, Math.max(0, intendedAttack - attack));
                transfer(SwarmNodeRole.REGROUP, SwarmNodeRole.ATTACK, addAttack);
                int addGuard = Math.min(available - addAttack, Math.max(0, intendedGuard - guard));
                transfer(SwarmNodeRole.REGROUP, SwarmNodeRole.GUARD, addGuard);
                if (addAttack + addGuard > 0) changed = true;
            }
            verify();
            return changed;
        }

        private int trim(int count) {
            int remaining = count;
            int take = Math.min(regroup, remaining); regroup -= take; remaining -= take;
            take = Math.min(attack, remaining); attack -= take; remaining -= take;
            take = Math.min(guard, remaining); guard -= take; remaining -= take;
            take = Math.min(lift, remaining); lift -= take; remaining -= take;
            if (remaining != 0) throw new IllegalStateException("swarm count mismatch");
            for (SwarmNodeRole role : SwarmNodeRole.values())
                if (count(role) == 0) pendingDamage.remove(role);
            return count;
        }

        /**
         * Only already gathered or physically nearby units may become lift.
         * This operation never changes totalAlive.
         */
        public int reserveLift(int required, boolean attackNearby, boolean guardNearby) {
            int missing = Math.min(2, Math.max(0, required - lift));
            int added = 0;
            int moved = Math.min(missing, regroup);
            transfer(SwarmNodeRole.REGROUP, SwarmNodeRole.LIFT, moved);
            added += moved; missing -= moved;
            if (missing > 0 && guardNearby) {
                moved = Math.min(missing, guard);
                transfer(SwarmNodeRole.GUARD, SwarmNodeRole.LIFT, moved);
                added += moved; missing -= moved;
            }
            if (missing > 0 && attackNearby) {
                moved = Math.min(missing, attack);
                transfer(SwarmNodeRole.ATTACK, SwarmNodeRole.LIFT, moved);
                added += moved;
            }
            verify();
            return added;
        }

        /** Landing restores the same surviving units to regroup. */
        public int releaseLift() {
            int count = lift;
            transfer(SwarmNodeRole.LIFT, SwarmNodeRole.REGROUP, count);
            verify();
            return count;
        }

        public int lose(SwarmNodeRole role, int requested) {
            int lost = Math.min(count(role), Math.max(0, requested));
            switch (role) {
                case ATTACK -> attack -= lost;
                case GUARD -> guard -= lost;
                case LIFT -> lift -= lost;
                case REGROUP -> regroup -= lost;
            }
            alive -= lost;
            if (count(role) == 0) pendingDamage.remove(role);
            verify();
            return lost;
        }

        public void verify() {
            if (alive < 0 || attack < 0 || guard < 0 || lift < 0 || regroup < 0
                    || (long) attack + guard + lift + regroup != alive)
                throw new IllegalStateException("swarm conservation violated");
            for (SwarmNodeRole role : SwarmNodeRole.values()) {
                double injury = pendingDamage(role);
                if (!Double.isFinite(injury) || injury < 0.0
                        || (count(role) == 0 && injury > 0.0))
                    throw new IllegalStateException("swarm injury state invalid");
            }
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("alive", alive);
            tag.putInt("attack", attack);
            tag.putInt("guard", guard);
            tag.putInt("lift", lift);
            tag.putInt("regroup", regroup);
            tag.putLong("lastRegeneration", lastRegeneration);
            tag.putLong("nextRushTick", nextRushTick);
            tag.putLong("nextSacrificeTick", nextSacrificeTick);
            for (SwarmNodeRole role : SwarmNodeRole.values())
                tag.putDouble("pending_" + role.name().toLowerCase(java.util.Locale.ROOT),
                        pendingDamage(role));
            tag.putString("active", active);
            ListTag items = new ListTag();
            tactics.forEach((name, pct) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("name", name);
                entry.putInt("attackPercent", pct);
                items.add(entry);
            });
            tag.put("tactics", items);
            return tag;
        }

        private static State load(CompoundTag tag) {
            State state = new State();
            state.alive = Math.max(0, tag.getInt("alive"));
            state.attack = Math.max(0, tag.getInt("attack"));
            state.guard = Math.max(0, tag.getInt("guard"));
            state.lift = Math.max(0, tag.getInt("lift"));
            state.regroup = Math.max(0, tag.getInt("regroup"));
            state.lastRegeneration = tag.getLong("lastRegeneration");
            state.nextRushTick = Math.max(0L, tag.getLong("nextRushTick"));
            state.nextSacrificeTick = Math.max(0L, tag.getLong("nextSacrificeTick"));
            for (SwarmNodeRole role : SwarmNodeRole.values()) {
                double pending = tag.getDouble("pending_" + role.name().toLowerCase(java.util.Locale.ROOT));
                if (Double.isFinite(pending) && pending > 0 && pending < 100000
                        && state.count(role) > 0) state.pendingDamage.put(role, pending);
            }
            ListTag items = tag.getList("tactics", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size() && state.tactics.size() < 8; i++) {
                CompoundTag entry = items.getCompound(i);
                String name = entry.getString("name");
                int pct = entry.getInt("attackPercent");
                if (!name.isBlank() && name.length() <= 24 && pct >= 0 && pct <= 100
                        && name.chars().noneMatch(Character::isISOControl))
                    state.tactics.put(name, pct);
            }
            String selected = tag.getString("active");
            state.active = state.tactics.containsKey(selected) ? selected : "";
            // Recover from old/corrupt saves without ever minting extra units.
            if ((long) state.attack + state.guard + state.lift + state.regroup != state.alive) {
                state.attack = state.guard = state.lift = 0;
                state.regroup = state.alive;
                state.pendingDamage.clear();
            }
            state.verify();
            return state;
        }
    }

    public enum SwarmNodeRole { ATTACK, GUARD, LIFT, REGROUP }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registry) {
        ListTag entries = new ListTag();
        players.forEach((id, state) -> {
            CompoundTag entry = state.save();
            entry.putUUID("owner", id);
            entries.add(entry);
        });
        tag.put("players", entries);
        tag.putInt("version", 2);
        return tag;
    }

    private static SwarmSavedData load(CompoundTag tag, HolderLookup.Provider registry) {
        SwarmSavedData data = new SwarmSavedData();
        ListTag entries = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.hasUUID("owner"))
                data.players.put(entry.getUUID("owner"), State.load(entry));
        }
        return data;
    }
}
