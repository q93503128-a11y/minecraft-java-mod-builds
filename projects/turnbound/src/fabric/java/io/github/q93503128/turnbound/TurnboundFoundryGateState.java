package io.github.q93503128.turnbound;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.PersistentState;

/**
 * Dedicated TURNBOUND facility state; never consumes Drehmal scoreboard or reward data.
 * A lever becomes known only after an actual interaction on an opted-in server.
 */
public final class TurnboundFoundryGateState extends PersistentState {
    public static final String STORAGE_KEY = "turnbound_foundry_gate_v1";
    private static final int SCHEMA = 1;
    private static final int LEVERS = 0b111;
    private int known;
    private int powered;
    private boolean opened;

    public TurnboundFoundryGateState() {}

    public static TurnboundFoundryGateState fromNbt(NbtCompound nbt) {
        if (!nbt.contains("Schema", NbtElement.INT_TYPE)
                || nbt.getInt("Schema") != SCHEMA
                || !nbt.contains("Known", NbtElement.INT_TYPE)
                || !nbt.contains("Powered", NbtElement.INT_TYPE)
                || !nbt.contains("Opened", NbtElement.BYTE_TYPE)) {
            throw new IllegalArgumentException("Invalid TURNBOUND Foundry state schema");
        }
        int known = nbt.getInt("Known");
        int powered = nbt.getInt("Powered");
        int opened = nbt.getByte("Opened");
        if (known < 0 || known > LEVERS || powered < 0
                || (powered & ~known) != 0 || (powered & ~LEVERS) != 0
                || (opened != 0 && opened != 1)) {
            throw new IllegalArgumentException("Invalid TURNBOUND Foundry state flags");
        }
        var result = new TurnboundFoundryGateState();
        result.known = known;
        result.powered = powered;
        result.opened = opened == 1;
        return result;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("Schema", SCHEMA);
        nbt.putInt("Known", known);
        nbt.putInt("Powered", powered);
        nbt.putByte("Opened", (byte) (opened ? 1 : 0));
        return nbt;
    }

    public void observeLever(int leverIndex, boolean isPowered) {
        if (leverIndex < 0 || leverIndex >= 3) {
            throw new IllegalArgumentException("Unknown Foundry lever");
        }
        int bit = 1 << leverIndex;
        int newKnown = known | bit;
        int newPowered = isPowered ? (powered | bit) : (powered & ~bit);
        if (newKnown != known || newPowered != powered) {
            known = newKnown;
            powered = newPowered;
            markDirty();
        }
    }

    public boolean ready() {
        return known == LEVERS && powered == LEVERS;
    }

    public boolean opened() {
        return opened;
    }

    public void markOpened() {
        if (!ready()) throw new IllegalStateException("Foundry gate is not ready");
        if (!opened) {
            opened = true;
            markDirty();
        }
    }

    public int knownCount() {
        return Integer.bitCount(known);
    }
}
