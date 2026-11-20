import java.lang.reflect.Method;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Persistence, migration and single-event restitution; no Minecraft client boot required. */
public final class PactCovenantConflictVerifier {
    private static void must(boolean works,String label) {
        if(!works)throw new AssertionError(label);
    }
    private static PactSavedData reload(PactSavedData data) throws Exception {
        Method method=PactSavedData.class.getDeclaredMethod("load",
                CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        method.setAccessible(true);
        return (PactSavedData)method.invoke(null,data.save(new CompoundTag(),RegistryAccess.EMPTY),
                RegistryAccess.EMPTY);
    }
    private static UUID threat(int index) {
        return UUID.fromString("00000000-0000-0000-0000-000000000"+String.format("%03d",index));
    }
    public static void main(String[] args) throws Exception {
        UUID owner=UUID.fromString("00000000-0000-0000-0000-000000000101");
        PactSavedData saved=new PactSavedData();
        must(!saved.markIsaelConflict(owner),"Unsigned covenants cannot cause conflicts");
        must(saved.sign(owner,"isael"),"Isael first");
        must(!saved.markIsaelConflict(owner),"Only one patron has been signed");
        must(saved.player(owner).bond("isael").relation()==50,"Initial faith is intact");
        must(saved.sign(owner,"velkan"),"Voluntary incompatible covenant");
        must(saved.markIsaelConflict(owner),"Second covenant creates one recorded breach");
        must(saved.player(owner).bond("isael").vowConflict()
                && saved.player(owner).bond("isael").relation()==40,
                "Conflict recorded and single -10 applied");
        int revision=saved.player(owner).revision();
        must(!saved.markIsaelConflict(owner)
                && saved.player(owner).revision()==revision
                && saved.player(owner).bond("isael").relation()==40,
                "Repeated server events and reloads cannot repeat the penalty");
        must(saved.protectVillagerForIsael(owner,threat(1))==0,
                "Ongoing forbidden covenant cannot be forgiven by farming protection");
        saved=reload(saved);
        must(saved.player(owner).bond("isael").vowConflict()
                && saved.player(owner).bond("isael").relation()==40,
                "Active conflict persists after server NBT round trip");
        must(saved.breakBond(owner,"velkan"),"Formal partner release");
        must(saved.player(owner).bond("isael").vowConflict(),
                "Releasing the conflicting partner does not instantly clear the vow");
        must(saved.protectVillagerForIsael(owner,threat(1))==1,
                "First distinct hostile protection qualifies");
        must(saved.player(owner).bond("isael").relation()==42,
                "First restitution partially restores faith");
        must(saved.protectVillagerForIsael(owner,threat(1))==0,
                "Repeated protection against the same threat never double-counts");
        saved=reload(saved);
        must(saved.player(owner).bond("isael").restitution()==1
                && saved.player(owner).bond("isael").vowConflict(),
                "Unique protection history persists across logout/restart");
        must(saved.protectVillagerForIsael(owner,threat(2))==2,
                "Second distinct threat qualifies");
        must(saved.player(owner).bond("isael").relation()==44,
                "Two rescues restore exactly four of ten faith");
        must(saved.protectVillagerForIsael(owner,threat(3))==3,
                "Third distinct threat reconciles the vow");
        must(!saved.player(owner).bond("isael").vowConflict()
                && saved.player(owner).bond("isael").restitution()==0
                && saved.player(owner).bond("isael").relation()==50,
                "Full restoration exactly offsets the one-time breach, without overflow");
        must(saved.protectVillagerForIsael(owner,threat(4))==0,
                "Post-restoration events cannot repeatedly refund the same breach");
        must(saved.sign(owner,"velkan") && saved.markIsaelConflict(owner),
                "A NEW incompatible signature starts a new breach");
        must(saved.player(owner).bond("isael").relation()==40,
                "A new violation never claims the prior repair twice");
        must(saved.breakBond(owner,"isael"),"End Isael covenant");
        must(saved.protectVillagerForIsael(owner,threat(7))==0,
                "No resurrection of restitution after breaking the owner covenant");

        UUID reverse=UUID.fromString("00000000-0000-0000-0000-000000000102");
        must(saved.sign(reverse,"velkan") && saved.sign(reverse,"isael")
                && saved.markIsaelConflict(reverse),
                "Signing Isael after Velkan also creates the same single breach");
        must(saved.player(reverse).bond("isael").relation()==40,
                "Signing order cannot avoid the conflict cost");

        UUID legacy=UUID.fromString("00000000-0000-0000-0000-000000000103");
        must(saved.sign(legacy,"isael") && saved.sign(legacy,"velkan"),
                "Construct legacy two-contract state");
        saved.relation(legacy,"isael",-10); // Old PactService charged this before tracking flag.
        CompoundTag old=saved.save(new CompoundTag(),RegistryAccess.EMPTY);
        ListTag profiles=old.getList("players",Tag.TAG_COMPOUND);
        for(int i=0;i<profiles.size();i++) {
            CompoundTag row=profiles.getCompound(i);
            if(!row.getUUID("player").equals(legacy))continue;
            ListTag bonds=row.getList("bonds",Tag.TAG_COMPOUND);
            for(int k=0;k<bonds.size();k++) {
                CompoundTag bond=bonds.getCompound(k);
                if(bond.getString("id").equals("isael")) {
                    bond.remove("vowConflict");
                    bond.remove("vowRestitution");
                }
            }
        }
        Method load=PactSavedData.class.getDeclaredMethod("load",
                CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        load.setAccessible(true);
        PactSavedData converted=(PactSavedData)load.invoke(null,old,RegistryAccess.EMPTY);
        must(converted.player(legacy).bond("isael").vowConflict()
                && converted.player(legacy).bond("isael").relation()==40,
                "Legacy double covenant gets status migration without a second -10");
        must(!converted.markIsaelConflict(legacy)
                && converted.player(legacy).bond("isael").relation()==40,
                "Legacy migration rejects duplicated contract penalties");
        converted=reload(converted);
        must(converted.player(legacy).bond("isael").vowConflict(),
                "Migration result remains stable over two saves");

        must(converted.sign(owner,"hastel"),"Independent debt contract remains available");
        converted.debt(owner,"hastel",12);
        converted=reload(converted);
        must(converted.player(owner).bond("hastel").debt()==12,
                "Debt persists regardless of vow breach/release");
        converted.debt(owner,"hastel",0);
        must(converted.player(owner).bond("hastel").debt()==0
                && converted.player(owner).has("hastel"),
                "Paid settlement clears debt without silently releasing the contract");
        must(converted.player(reverse).bond("isael").vowConflict(),
                "Other players' conflicting covenants remain isolated");

        System.out.println("One-time celestial breach, no upkeep, unique-threat restoration, "
                +"legacy no-double-charge, multiplayer separation, debt persistence: PASS");
    }
}
