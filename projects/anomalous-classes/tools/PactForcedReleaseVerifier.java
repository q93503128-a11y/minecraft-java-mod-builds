import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import kr.moonseungjun.anomalousclasses.state.PactCatalog;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Actual server-owned NBT obligations. This does NOT emulate a client GUI. */
public final class PactForcedReleaseVerifier {
    private static void require(boolean yes, String why) {
        if (!yes) throw new AssertionError(why);
    }
    private static PactSavedData restart(PactSavedData old) throws Exception {
        Method loader = PactSavedData.class.getDeclaredMethod("load", CompoundTag.class,
                net.minecraft.core.HolderLookup.Provider.class);
        loader.setAccessible(true);
        return (PactSavedData) loader.invoke(null,
                old.save(new CompoundTag(), RegistryAccess.EMPTY), RegistryAccess.EMPTY);
    }
    private static Map<ResourceLocation, Integer> amounts(List<PactCatalog.Material> list) {
        return list.stream().collect(Collectors.toMap(PactCatalog.Material::itemId,
                PactCatalog.Material::count, Math::addExact));
    }

    public static void main(String[] args) throws Exception {
        require(PactCatalog.all().size() == 16, "all sixteen patrons must be covered");
        PactSavedData storage = new PactSavedData();
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000207");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000208");

        require(!storage.forceBreakBond(owner, "isael"), "cannot break unsigned covenant");
        require(!storage.settleForcedRelease(owner, "isael"), "no phantom bill");
        require(storage.sign(other, "isael"), "other player independent");
        for (var patron : PactCatalog.all().values()) {
            String id = patron.id();
            require(storage.sign(owner, id), "original covenant signed " + id);
            int previous = storage.player(owner).revision();
            require(storage.forceBreakBond(owner, id), "forced release recorded " + id);
            require(storage.player(owner).revision() > previous, "revision increments " + id);
            require(!storage.player(owner).has(id), "powers removed from data " + id);
            require(!storage.forceBreakBond(owner, id), "cannot duplicate forced bill " + id);
            var expected = amounts(patron.release());
            require(amounts(storage.player(owner).outstandingRelease(id)).equals(expected),
                    "existing release price exact " + id);
            require(!storage.sign(owner, id), "re-sign blocked until paid " + id);
            storage = restart(storage);
            require(amounts(storage.player(owner).outstandingRelease(id)).equals(expected),
                    "precise bill survives relog " + id);
            require(storage.player(other).has("isael")
                    && !storage.player(other).hasOutstandingRelease(id),
                    "no debt leaked to another player " + id);
            require(storage.settleForcedRelease(owner, id), "exactly one settlement " + id);
            require(!storage.settleForcedRelease(owner, id), "cannot settle twice " + id);
            require(storage.sign(owner, id), "re-sign allowed after settlement " + id);
            require(storage.breakBond(owner, id), "regular release compatible " + id);
        }

        require(storage.sign(owner, "hastel"), "Hastel debt test");
        storage.debt(owner, "hastel", 7);
        require(storage.forceBreakBond(owner, "hastel"), "force exit with unpaid combat debt");
        var hastel = amounts(storage.player(owner).outstandingRelease("hastel"));
        require(hastel.get(ResourceLocation.withDefaultNamespace("gold_ingot")) == 19,
                "release gold 12 plus actual combat debt 7");
        require(hastel.get(ResourceLocation.withDefaultNamespace("emerald")) == 16,
                "original sixteen emeralds not inflated");
        storage = restart(storage);
        require(amounts(storage.player(owner).outstandingRelease("hastel")).equals(hastel),
                "Hastel combat debt preserved on restart");
        require(!storage.sign(owner, "hastel"), "Hastel cannot freely re-sign");
        require(storage.settleForcedRelease(owner, "hastel"),
                "Hastel combat and release bill together settled");
        require(storage.sign(owner, "hastel"),
                "re-sign after Hastel debt settlement");
        require(storage.player(owner).bond("hastel").debt() == 0,
                "new bond does not inherit old combat debt");

        UUID legacy = UUID.fromString("00000000-0000-0000-0000-000000000209");
        require(storage.sign(legacy, "velkan"), "legacy fixture");
        var tag = storage.save(new CompoundTag(), RegistryAccess.EMPTY);
        var players = tag.getList("players", 10);
        for (int i = 0; i < players.size(); i++)
            players.getCompound(i).remove("unpaidReleaseBills");
        Method loader = PactSavedData.class.getDeclaredMethod("load",
                CompoundTag.class, net.minecraft.core.HolderLookup.Provider.class);
        loader.setAccessible(true);
        var loadedLegacy = (PactSavedData) loader.invoke(null, tag, RegistryAccess.EMPTY);
        require(loadedLegacy.player(legacy).has("velkan")
                && loadedLegacy.player(legacy).outstandingReleases().isEmpty(),
                "old worlds remain valid with zero extra debt");
        require(storage.player(other).has("isael")
                && storage.player(other).outstandingReleases().isEmpty(),
                "other player's faithful covenant intact");
        System.out.println("Sixteen exact quoted release bills, forced-exit no-payment, "
                + "multi-user isolation, Hastel debt preservation, NBT migration and redemption: PASS");
    }
}
