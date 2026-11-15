import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.ClassFocus;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.FocusTier;
import kr.moonseungjun.anomalousclasses.state.PuppetBlueprint;
import kr.moonseungjun.anomalousclasses.state.PuppetEconomy;
import kr.moonseungjun.anomalousclasses.state.PuppetRegistrySavedData;
import kr.moonseungjun.anomalousclasses.state.PuppeteerRules;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;

/** Verifies the implemented puppet construction/state formulas without launching a client. */
public final class PuppeteerFoundationVerifier {
    private PuppeteerFoundationVerifier() {}
    public static void main(String[] args) throws Exception {
        UUID owner = UUID.randomUUID(), id = UUID.randomUUID();
        Map<String,Integer> paid = PuppetEconomy.starterBlueprintLedger(PuppetBlueprint.Size.NORMAL);
        check(paid.get("minecraft:oak_planks") == 4, "starter plank count");
        check(paid.get("minecraft:stick") == 4, "starter stick count");
        check(paid.get("minecraft:string") == 4, "starter string count");
        check(paid.get("minecraft:copper_ingot") == 3, "starter copper count");
        check(paid.get("minecraft:iron_nugget") == 4, "starter iron nuggets");
        check(paid.get("minecraft:flint") == 1, "starter flint");
        check(paid.get("minecraft:redstone") == 1, "starter redstone");
        check(paid.get("minecraft:glass") == 1, "starter glass");
        check(PuppetEconomy.extraLightFrameMaterials(PuppetBlueprint.Size.NORMAL,
                PuppetBlueprint.Size.GIANT).get("minecraft:oak_planks") == 11,
                "3m expansion plank delta");
        check(PuppetEconomy.lightFrameMaterials(PuppetBlueprint.Size.GIANT)
                .get("minecraft:oak_planks") == 15, "3m frame cost");

        PuppetBlueprint initial = new PuppetBlueprint(id, owner, PuppetBlueprint.Frame.LIGHT,
                PuppetBlueprint.Device.BLADE, PuppetBlueprint.Core.BASIC,
                PuppetBlueprint.Size.NORMAL, .25, 135, paid);
        check(PuppetBlueprint.load(initial.save()).orElseThrow().equals(initial), "NBT blueprint roundtrip");
        check(initial.withParts(PuppetBlueprint.Frame.HEAVY, PuppetBlueprint.Device.LAUNCHER,
                PuppetBlueprint.Core.OUTPUT, PuppetBlueprint.Size.GIANT, paid)
                .puppetId().equals(id), "upgrades retain identity");
        check(initial.withParts(PuppetBlueprint.Frame.HEAVY, PuppetBlueprint.Device.LAUNCHER,
                PuppetBlueprint.Core.OUTPUT, PuppetBlueprint.Size.GIANT, paid)
                .nextAttackTick() == 135, "upgrades retain recovery");
        check(PuppetBlueprint.load(new CompoundTag()).isEmpty(), "malformed blueprint rejected");

        PlayerClassProfile ordinary = new PlayerClassProfile();
        check(PuppeteerRules.maxActive(1, ordinary) == 1, "level-1 slot");
        check(PuppeteerRules.maxActive(100, ordinary) == 3, "level-100 slots");
        check(PuppeteerRules.maxActive(500, ordinary) == 5, "level-500 slots");
        check(PuppeteerRules.maxActive(1_000, ordinary) == 7, "level-1000 slots");
        check(PuppeteerRules.maxActive(500, ordinary) < PuppeteerRules.maxActive(Integer.MAX_VALUE, ordinary),
                "no artificial leveling cap");
        ordinary.equipFocus(new ClassFocus(ClassId.PUPPETEER, FocusTier.INCOMPREHENSIBLE));
        check(PuppeteerRules.maxActive(1, ordinary) == 6, "incomprehensible focus +5 slots");
        check(close(PuppeteerRules.gearStats(ordinary), 1.4), "incomprehensible focus +40%");
        ordinary.equipFocus(new ClassFocus(ClassId.MENTAL_DOMINATOR, FocusTier.INCOMPREHENSIBLE));
        check(PuppeteerRules.maxActive(1, ordinary) == 1, "other-class focus isolation");
        check(close(PuppeteerRules.gearStats(ordinary), 1.0), "other-class damage isolation");

        Set<String> signatures = new HashSet<>();
        for (PuppetBlueprint.Frame frame : PuppetBlueprint.Frame.values()) {
            for (PuppetBlueprint.Device device : PuppetBlueprint.Device.values()) {
                for (PuppetBlueprint.Core core : new PuppetBlueprint.Core[] {
                        PuppetBlueprint.Core.SWIFT, PuppetBlueprint.Core.STABLE, PuppetBlueprint.Core.OUTPUT}) {
                    PuppetBlueprint b = new PuppetBlueprint(UUID.randomUUID(), owner, frame,
                            device, core, PuppetBlueprint.Size.NORMAL, 1, 0, paid);
                    PuppeteerRules.Stats stats = PuppeteerRules.stats(b, 1, ordinary);
                    check(stats.damage() > 0 && stats.maxHealth() > 0 && stats.attackIntervalTicks() > 0,
                            "all 27 combat combinations finite");
                    signatures.add(frame + "/" + device + "/" + core);
                }
            }
        }
        check(signatures.size() == 27, "3 x 3 x 3 specializations");
        PuppetBlueprint larger = initial.withParts(initial.frame(), initial.device(), initial.core(),
                PuppetBlueprint.Size.GIANT, paid);
        check(PuppeteerRules.stats(larger, 1, ordinary).maxHealth()
                > PuppeteerRules.stats(initial, 1, ordinary).maxHealth(), "height increases HP");
        check(PuppeteerRules.stats(larger, 1, ordinary).attackIntervalTicks()
                > PuppeteerRules.stats(initial, 1, ordinary).attackIntervalTicks(), "height slows attack");
        check(PuppeteerRules.stats(larger, 1, ordinary).height() == 3.0, "3m collision baseline");

        var normalRefund = PuppetEconomy.dismantleReturn(initial, false, 0);
        var brokenRefund = PuppetEconomy.dismantleReturn(initial, true, 0);
        check(PuppetEconomy.refundNeverExceedsInput(initial, normalRefund), "normal refund bounded");
        check(PuppetEconomy.refundNeverExceedsInput(initial, brokenRefund), "broken refund bounded");
        for (String name : paid.keySet())
            check(brokenRefund.getOrDefault(name, 0) <= normalRefund.getOrDefault(name, 0),
                    "broken dismantle cannot improve refund");

        PuppetRegistrySavedData ledger = new PuppetRegistrySavedData();
        check(ledger.registerCrafted(initial), "register once");
        check(!ledger.registerCrafted(initial), "cannot register duplicate");
        check(ledger.deploy(owner, id, 1).isPresent(), "deploy stored");
        check(ledger.deployedCount(owner) == 1, "deployed owner count");
        check(ledger.deploy(owner, id, 1).isEmpty(), "stale item cannot deploy");
        check(ledger.recall(UUID.randomUUID(), id, 2, initial).isEmpty(), "different owner cannot recall");
        check(ledger.recall(owner, id, 2, initial.withCondition(.23, 140)).isPresent(),
                "recall retains damaged HP and recovery");
        check(ledger.deployedCount(owner) == 0, "stored puppet not active");
        check(ledger.deploy(owner, id, 2).isEmpty(), "old item revision invalid");
        check(ledger.deploy(owner, id, 3).isPresent(), "new revision can deploy");
        check(ledger.breakPuppet(owner, id, 4, initial.withCondition(0, 150)).isPresent(),
                "broken entity becomes broken record");
        check(ledger.dismantle(owner, id, 5).isPresent(), "broken item disassembled once");
        check(ledger.dismantle(owner, id, 6).isEmpty(), "retired puppet cannot be refunded twice");

        CompoundTag saved = ledger.save(new CompoundTag(), RegistryAccess.EMPTY);
        Method load = PuppetRegistrySavedData.class.getDeclaredMethod(
                "load", CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class);
        load.setAccessible(true);
        PuppetRegistrySavedData restored = (PuppetRegistrySavedData) load.invoke(
                null, saved, RegistryAccess.EMPTY);
        check(restored.find(id).orElseThrow().location() == PuppetRegistrySavedData.Location.RETIRED,
                "retired identity persists");
        check(restored.deploy(owner, id, 6).isEmpty(), "retired identity cannot redeploy after reload");
        System.out.println("PASS: puppet 27 roles, 5 sizes, gear slots, NBT data, material costs/refunds and anti-duplication lifecycle");
    }
    private static boolean close(double a, double b) { return Math.abs(a-b) < 1e-8; }
    private static void check(boolean result, String name) {
        if (!result) throw new IllegalStateException("Puppeteer foundation: " + name);
    }
}
