import java.lang.reflect.Method;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.PactSavedData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Ensures only a committed, player-owned Nerea offence has a bounded repair. */
public final class PactNereaSanctumVerifier {
    private static void yes(boolean ok,String why) {
        if(!ok)throw new AssertionError(why);
    }
    private static UUID uuid(int value) {
        return UUID.fromString("00000000-0000-0000-0000-"+String.format("%012d",value));
    }
    private static PactSavedData load(CompoundTag saved) throws Exception {
        Method method=PactSavedData.class.getDeclaredMethod(
                "load",CompoundTag.class,net.minecraft.core.HolderLookup.Provider.class);
        method.setAccessible(true);
        return (PactSavedData)method.invoke(null,saved,RegistryAccess.EMPTY);
    }
    private static PactSavedData restart(PactSavedData original) throws Exception {
        return load(original.save(new CompoundTag(),RegistryAccess.EMPTY));
    }
    public static void main(String[] args) throws Exception {
        UUID owner=uuid(220),enemy1=uuid(301),enemy2=uuid(302);
        PactSavedData pacts=new PactSavedData();
        yes(!pacts.defileNereaSanctum(owner),"Unsigned player cannot break an oath");
        yes(pacts.reconcileNereaSanctum(owner,enemy1)==0,"Unsigned player cannot repair an oath");
        yes(pacts.sign(owner,"nerea"),"Nerea contract signed");
        yes(pacts.sign(owner,"karda"),"Second contract coexists");
        pacts.relation(owner,"nerea",5);
        yes(pacts.player(owner).bond("nerea").relation()==55,"Distinct initial affinity");
        yes(pacts.defileNereaSanctum(owner),"First intentional sanctum violation");
        yes(pacts.player(owner).bond("nerea").nereaSanctumBreach()
                && pacts.player(owner).bond("nerea").nereaAffinityDebt()==8
                && pacts.player(owner).bond("nerea").relation()==47,
                "Single bounded affinity cost, no other grace touched");
        int revision=pacts.player(owner).revision();
        yes(!pacts.defileNereaSanctum(owner)
                && pacts.player(owner).revision()==revision
                && pacts.player(owner).bond("nerea").relation()==47,
                "Repeated block events cannot charge recurring fees");
        yes(pacts.player(owner).bond("karda").relation()==50,
                "Sanctum violation belongs only to Nerea");
        pacts=restart(pacts);
        yes(pacts.player(owner).bond("nerea").nereaSanctumBreach()
                && pacts.player(owner).bond("nerea").relation()==47,
                "A live breach persists after restart");
        yes(pacts.reconcileNereaSanctum(owner,enemy1)==1,
                "One distinct real enemy reconciles half the vow");
        yes(pacts.player(owner).bond("nerea").relation()==51,
                "Only paid affinity is refunded");
        yes(pacts.reconcileNereaSanctum(owner,enemy1)==0,
                "One enemy cannot be repeatedly used to refund");
        pacts=restart(pacts);
        yes(pacts.player(owner).bond("nerea").nereaReconciliation()==1
                && pacts.reconcileNereaSanctum(owner,enemy1)==0,
                "Reconciliation evidence persists across restarts");
        yes(pacts.reconcileNereaSanctum(owner,enemy2)==2,
                "Second unique enemy fully reconciles the sanctum");
        yes(!pacts.player(owner).bond("nerea").nereaSanctumBreach()
                && pacts.player(owner).bond("nerea").nereaAffinityDebt()==0
                && pacts.player(owner).bond("nerea").relation()==55,
                "Reconciliation ends exactly at pre-offense affinity");
        yes(pacts.reconcileNereaSanctum(owner,uuid(303))==0,
                "No further free relationship gain after reparation");
        yes(pacts.defileNereaSanctum(owner),
                "New voluntary offence may trigger a new one-off consequence");
        yes(pacts.player(owner).bond("nerea").relation()==47,
                "New offence subtracts once, not forever");
        yes(pacts.breakBond(owner,"nerea"),"Formal severance");
        yes(pacts.reconcileNereaSanctum(owner,enemy1)==0,
                "Breaking a bond ends the prior reconciliation path");
        yes(pacts.sign(owner,"nerea")
                && !pacts.player(owner).bond("nerea").nereaSanctumBreach(),
                "Re-signing does not inherit or refund old Nerea debt");

        UUID low=uuid(221);
        yes(pacts.sign(low,"nerea"),"Low affinity owner");
        pacts.relation(low,"nerea",-100);
        yes(pacts.defileNereaSanctum(low)
                && pacts.player(low).bond("nerea").nereaAffinityDebt()==0,
                "Zero affinity cannot be charged below zero");
        yes(pacts.reconcileNereaSanctum(low,enemy1)==1
                && pacts.reconcileNereaSanctum(low,enemy2)==2
                && pacts.player(low).bond("nerea").relation()==0,
                "Zero-affinity pledge may be reconciled but not minted into free power");

        UUID oldOwner=uuid(222);
        yes(pacts.sign(oldOwner,"nerea"),"Legacy Nerea test created");
        CompoundTag before=pacts.save(new CompoundTag(),RegistryAccess.EMPTY);
        ListTag players=before.getList("players",Tag.TAG_COMPOUND);
        for(int i=0;i<players.size();i++) {
            CompoundTag row=players.getCompound(i);
            if(!row.getUUID("player").equals(oldOwner))continue;
            ListTag bonds=row.getList("bonds",Tag.TAG_COMPOUND);
            for(int j=0;j<bonds.size();j++) {
                CompoundTag bond=bonds.getCompound(j);
                if(bond.getString("id").equals("nerea")) {
                    bond.remove("sanctumBreach");
                    bond.remove("sanctumDebt");
                    bond.remove("sanctumRestitution");
                }
            }
        }
        PactSavedData restored=load(before);
        yes(!restored.player(oldOwner).bond("nerea").nereaSanctumBreach()
                && restored.player(oldOwner).bond("nerea").relation()==50,
                "Old saves without new fields stay unaffected");
        System.out.println("Nerea unique hostile water rites, single sanctuary breach, "
                +"exact affinity restitution, persistent proof and legacy migration: PASS");
    }
}
