package kr.moonseungjun.anomalousclasses.state;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/**
 * A single physical marionette's identity and construction. The same id is
 * carried by a stored item OR its deployed entity, never both simultaneously.
 */
public record PuppetBlueprint(UUID puppetId, UUID ownerId, Frame frame, Device device, Core core,
                              Size size, double healthRatio, long nextAttackTick,
                              Map<String, Integer> materialLedger) {
    public static final int DATA_VERSION = 1;

    public enum Frame { LIGHT, BALANCED, HEAVY }
    public enum Device { BLADE, LAUNCHER, SHIELD }
    public enum Core { BASIC, SWIFT, STABLE, OUTPUT }
    public enum Size {
        SMALL(1.0), NORMAL(1.5), TALL(2.0), LARGE(2.5), GIANT(3.0);
        private final double height;
        Size(double height) { this.height = height; }
        public double height() { return height; }
    }

    public PuppetBlueprint {
        if (puppetId == null || ownerId == null || frame == null || device == null
                || core == null || size == null || materialLedger == null)
            throw new IllegalArgumentException("Marionette identity, parts and ledger are required");
        if (!Double.isFinite(healthRatio) || healthRatio < 0.0 || healthRatio > 1.0)
            throw new IllegalArgumentException("Health ratio must be 0..1");
        if (nextAttackTick < 0L) throw new IllegalArgumentException("Attack recovery cannot be negative");
        TreeMap<String, Integer> sorted = new TreeMap<>();
        for (var contribution : materialLedger.entrySet()) {
            if (contribution.getKey() == null || contribution.getKey().isBlank()
                    || contribution.getKey().length() > 128
                    || net.minecraft.resources.ResourceLocation.tryParse(contribution.getKey()) == null
                    || contribution.getValue() == null || contribution.getValue() < 1
                    || contribution.getValue() > 100000)
                throw new IllegalArgumentException("Invalid material entry");
            sorted.merge(contribution.getKey(), contribution.getValue(), Math::addExact);
        }
        materialLedger = Map.copyOf(sorted);
    }

    public PuppetBlueprint withCondition(double ratio, long recovery) {
        return new PuppetBlueprint(puppetId, ownerId, frame, device, core, size,
                Math.max(0, Math.min(1, ratio)), Math.max(0, recovery), materialLedger);
    }

    public PuppetBlueprint withParts(Frame nextFrame, Device nextDevice, Core nextCore,
                                    Size nextSize, Map<String, Integer> updatedLedger) {
        return new PuppetBlueprint(puppetId, ownerId, nextFrame, nextDevice, nextCore, nextSize,
                healthRatio, nextAttackTick, updatedLedger);
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("version", DATA_VERSION);
        nbt.putUUID("puppet", puppetId);
        nbt.putUUID("owner", ownerId);
        nbt.putString("frame", frame.name());
        nbt.putString("device", device.name());
        nbt.putString("core", core.name());
        nbt.putString("size", size.name());
        nbt.putDouble("healthRatio", healthRatio);
        nbt.putLong("nextAttack", nextAttackTick);
        CompoundTag ledger = new CompoundTag();
        materialLedger.forEach(ledger::putInt);
        nbt.put("materials", ledger);
        return nbt;
    }

    public static Optional<PuppetBlueprint> load(CompoundTag nbt) {
        if (nbt == null || nbt.getInt("version") != DATA_VERSION
                || !nbt.hasUUID("puppet") || !nbt.hasUUID("owner")) return Optional.empty();
        try {
            CompoundTag serialized = nbt.getCompound("materials");
            Map<String, Integer> ledger = new TreeMap<>();
            for (String key : serialized.getAllKeys()) ledger.put(key, serialized.getInt(key));
            return Optional.of(new PuppetBlueprint(nbt.getUUID("puppet"), nbt.getUUID("owner"),
                    Frame.valueOf(nbt.getString("frame")),
                    Device.valueOf(nbt.getString("device")),
                    Core.valueOf(nbt.getString("core")),
                    Size.valueOf(nbt.getString("size")),
                    nbt.getDouble("healthRatio"), nbt.getLong("nextAttack"), ledger));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }
}
