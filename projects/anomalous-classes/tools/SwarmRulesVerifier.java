import kr.moonseungjun.anomalousclasses.state.SwarmRules;
import kr.moonseungjun.anomalousclasses.state.SwarmSavedData;
import kr.moonseungjun.anomalousclasses.state.SwarmFlightRules;
import kr.moonseungjun.anomalousclasses.state.SwarmGrowthRules;
import kr.moonseungjun.anomalousclasses.state.GraphSkillTreeCatalog;
import kr.moonseungjun.anomalousclasses.state.ClassFocus;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.ClassProgress;
import kr.moonseungjun.anomalousclasses.state.FocusTier;
import kr.moonseungjun.anomalousclasses.state.PlayerClassProfile;
import java.util.HashSet;
import java.util.Set;
import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class SwarmRulesVerifier {
    public static void main(String[] args) {
        require(SwarmRules.maximum(1) == 16, "level 1");
        require(SwarmRules.maximum(25) == 48, "level 25");
        require(SwarmRules.maximum(100) == 80, "level 100");
        require(SwarmRules.maximum(500) == 128, "level 500");
        require(SwarmRules.maximum(100000) == 128, "bounded entity budget");
        require(SwarmFlightRules.requiredLift(1) == 0, "no low-level flight");
        require(SwarmFlightRules.requiredLift(25) == 24, "level 25");
        require(SwarmFlightRules.requiredLift(100) == 30, "level 100");
        require(SwarmFlightRules.requiredLift(500) == 30, "max support");
        require(!SwarmFlightRules.qualified(25, 23), "flight needs actual organisms");
        require(SwarmFlightRules.qualified(25, 24), "available lift");
        require(SwarmFlightRules.flyingSpeed(500) > SwarmFlightRules.flyingSpeed(25),
                "later flight grows");
        for (int pct : new int[]{0, 40, 50, 100}) {
            var split = SwarmRules.distribute(80, 30, 0, pct);
            require(split.total() == 80, "flight conservation");
            require(split.lift() == 30, "flight reservation");
            require(split.attack() == 50 * pct / 100, "tactic exactness");
        }
        var state = new SwarmSavedData.State();
        require(state.alive() == 16 && state.regroup() == 16, "no injected tactic");
        require(state.activeName().isBlank() && state.tactics().isEmpty(), "no starter preset");
        require(state.saveTactic("방어", 0), "user tactic 0");
        for (int tick = 1; tick <= 50; tick++) state.advance(tick * 10L, 16);
        require(state.attack() == 0 && state.guard() == 16, "0:100");
        require(state.saveTactic("돌격", 100), "user tactic 100");
        for (int tick = 51; tick <= 130; tick++) state.advance(tick * 10L, 16);
        require(state.attack() == 16 && state.guard() == 0, "100:0");
        int beforeFlight = state.alive();
        for (int x = 0; x < 8; x++) {
            int moved = state.reserveLift(24, true, true);
            require(moved <= 2, "flight deployment rate limit");
        }
        require(state.lift() == 16 && state.attack() == 0, "lift cannot create creatures");
        require(state.alive() == beforeFlight, "lift N conservation");
        require(state.lose(SwarmSavedData.SwarmNodeRole.LIFT, 3) == 3, "lift can be killed");
        require(state.lift() == 13 && state.alive() == beforeFlight - 3, "lift casualties real");
        require(state.releaseLift() == 13, "landing returns survivors only");
        require(state.lift() == 0, "landing clears lift");
        state.verify();
        for (int t = 1; t <= 20; t++) state.advance(10000 + 10L * t, 16);
        require(state.attack() > 0, "attack resuming after landing");
        int beforeAttackLoss = state.alive();
        int casualties = Math.min(5, state.attack());
        require(state.lose(SwarmSavedData.SwarmNodeRole.ATTACK, casualties) == casualties, "real loss");
        require(state.alive() == beforeAttackLoss - casualties, "no free refill");
        require(state.saveTactic("균등", 50), "mid tactic");
        for (int tick = 131; tick <= 160; tick++) state.advance(tick * 10L, 16);
        state.verify();
        require(!state.saveTactic("bad", 101), "reject invalid ratio");
        require(SwarmRules.sacrificeCost(0) == 0, "zero attack cannot sacrifice");
        require(SwarmRules.sacrificeCost(1) == 1, "minimum sacrifice cost");
        require(SwarmRules.sacrificeCost(16) == 4, "sacrifice based on attacking not total");
        require(SwarmRules.sacrificeDamage(0, 1) == 0F, "no damage without paid organisms");
        require(SwarmRules.rushDamage(0, 1) == 0F, "no attack when allocated zero");
        require(state.cooldownRemaining(1, 1000) == 0, "fresh rush");
        require(state.commitCooldown(1, 1000, 160), "first rush cast accepted");
        require(state.cooldownRemaining(1, 1000) == 160, "rush cooldown starts");
        require(!state.commitCooldown(1, 1001, 160), "same skill cannot spam");
        require(state.commitCooldown(2, 1002, 240), "separate sacrifice cooldown");
        require(state.cooldownRemaining(2, 1003) == 239, "sacrifice cooldown active");
        require(state.commitCooldown(1, 1160, 160), "rush ready again");
        verifyGraphGrowth();
        verifyInjuryAndSpeed();
        verifyTacticIdentity();
        verifyLegacyNamedTacticLoad();
        System.out.println("Swarm rules / nine graph perks / focus / fractional injury VERIFIED");
    }
    private static void verifyGraphGrowth() {
        PlayerClassProfile profile = new PlayerClassProfile();
        ClassId id = ClassId.SWARM_CASTER;
        var none = SwarmGrowthRules.from(profile);
        require(none.sharpJaws() == 0 && none.naturalFission() == 0
                && none.focusBonus() == 0.0, "unequipped rank zero");
        String[] ids = {"sharp_jaws", "assault_tactics", "sacrifice_instinct",
                "hard_shell", "ballistic_sense", "escort_formation",
                "natural_fission", "regrouping", "pack_tracking"};
        Set<String> all = new HashSet<>();
        for (String skill : ids) {
            var treeSkill = GraphSkillTreeCatalog.tree(id).skills().get(skill);
            require(treeSkill != null, "existing source graph skill " + skill);
            var stages = treeSkill.segments();
            var first = stages.get(0);
            require(first.oldRankTo() >= 10, "first major stage value");
            all.add(stages.get(stages.size() - 1).majorNodeId());
        }
        // Graph minor nodes alone grant generic stats, not the unique swarm
        // perk before the corresponding major is actually purchased.
        var minorOnly = profile.progress(id).withGraphTree(100,
                Set.of("sharp_jaws.s1.m1"), GraphSkillTreeCatalog.CURRENT_VERSION);
        profile.updateProgress(id, minorOnly);
        require(SwarmGrowthRules.from(profile).sharpJaws() == 0, "major required");
        var firstStage = profile.progress(id).withGraphTree(100,
                Set.of("sharp_jaws.s1.major", "assault_tactics.s2.major"),
                GraphSkillTreeCatalog.CURRENT_VERSION);
        profile.updateProgress(id, firstStage);
        require(SwarmGrowthRules.from(profile).sharpJaws() == 10, "stage one is rank 10");
        require(SwarmGrowthRules.from(profile).assaultTactics() == 20,
                "stage two rank not just two");
        profile.updateProgress(id, profile.progress(id).withGraphTree(0, all,
                GraphSkillTreeCatalog.CURRENT_VERSION));
        var perks = SwarmGrowthRules.from(profile);
        require(perks.sharpJaws() == 30 && perks.assaultTactics() == 30
                && perks.sacrificeInstinct() == 30 && perks.hardShell() == 30
                && perks.ballisticSense() == 30 && perks.escortFormation() == 30
                && perks.naturalFission() == 30 && perks.regrouping() == 30
                && perks.packTracking() == 30, "all nine graph ranks");
        require(close(perks.normalAttackFactor(), 1.18), "normal offense");
        require(close(perks.rushAttackFactor(), 1.36), "rush adds only rush rank");
        require(close(perks.sacrificeAttackFactor(), 1.36), "sacrifice adds only sacrifice rank");
        require(close(perks.durabilityFactor(SwarmSavedData.SwarmNodeRole.GUARD), 1.33),
                "guard shell and formation additive");
        require(close(perks.durabilityFactor(SwarmSavedData.SwarmNodeRole.LIFT), 1.18),
                "lift shell without free guard");
        require(perks.regenerationTicks() == 30, "36 percent faster multiplication");
        require(perks.transferBudget() == 6, "regroup plus escort milestone");
        require(close(perks.chaseSpeedFactor(), 1.24), "pack tracking travel");
        require(close(perks.guardMoveFactor(), 1.24), "ballistic guard movement");
        require(close(perks.regroupMoveFactor(), 1.21), "regroup path movement");
        double[] bonuses = {.05, .10, .15, .20, .40};
        for (FocusTier tier : FocusTier.values()) {
            profile.equipFocus(new ClassFocus(ClassId.SWARM_CASTER, tier));
            var equipped = SwarmGrowthRules.from(profile);
            require(close(equipped.focusBonus(), bonuses[tier.index()]), "focus tier " + tier);
            require(close(equipped.normalAttackFactor(), 1.18 + bonuses[tier.index()]),
                    "single focus offense " + tier);
            require(close(equipped.durabilityFactor(SwarmSavedData.SwarmNodeRole.GUARD),
                    1.33 + bonuses[tier.index()]), "single focus guard " + tier);
        }
        profile.equipFocus(new ClassFocus(ClassId.PUPPETEER, FocusTier.INCOMPREHENSIBLE));
        require(SwarmGrowthRules.from(profile).focusBonus() == 0.0, "wrong-class focus ignored");
    }

    private static void verifyInjuryAndSpeed() {
        var state = new SwarmSavedData.State();
        state.saveTactic("attack", 100);
        for (int i = 0; i < 50; i++) state.advance(i * 10L, 16);
        int original = state.alive();
        require(state.attack() == original, "physical attack group assembled");
        require(state.applyDamage(SwarmSavedData.SwarmNodeRole.ATTACK, 1.5F, 4.0) == 0,
                "weak hits accumulate without guaranteed death");
        require(state.alive() == original, "no instant casualty");
        require(close(state.pendingDamage(SwarmSavedData.SwarmNodeRole.ATTACK), 1.5),
                "injury stored between hits");
        require(state.applyDamage(SwarmSavedData.SwarmNodeRole.ATTACK, 2.5F, 4.0) == 1,
                "threshold crossing costs exactly one organism");
        require(state.alive() == original - 1, "one valid casualty");
        require(state.applyDamage(SwarmSavedData.SwarmNodeRole.ATTACK, 2.0F, 5.6) == 0,
                "equipped durability takes effect without a free heal");
        require(close(state.pendingDamage(SwarmSavedData.SwarmNodeRole.ATTACK), 2.0),
                "raw persistent injury survives durability change");
        state.saveTactic("defense", 0);
        for (int i = 0; i < 70; i++) state.advance(1000 + i * 10L, 16);
        state.verify();
        double injurySum = 0;
        for (var role : SwarmSavedData.SwarmNodeRole.values()) injurySum += state.pendingDamage(role);
        require(injurySum >= 2.0, "reassignment cannot erase carried injury");
        var high = new SwarmSavedData.State();
        high.saveTactic("attack", 100);
        high.advance(0, 16, 40, 2);
        high.advance(30, 16, 30, 2);
        high.verify();
    }

    private static void verifyTacticIdentity() {
        var state = new SwarmSavedData.State();
        require(state.orderedTactics().isEmpty(), "no injected tactical presets");
        int revision0 = state.tacticRevision();
        var first = state.createTactic("내 전술", 0);
        require(first != null && state.tacticRevision() > revision0, "first custom tactic");
        require(state.activeId().equals(first) && state.attackPercent() == 0, "zero attack accepted");
        var duplicate = state.createTactic("내 전술", 100);
        require(duplicate != null && !first.equals(duplicate), "duplicate names use unique IDs");
        require(state.orderedTactics().size() == 2, "both name duplicates retained");
        require(state.activeId().equals(duplicate) && state.attackPercent() == 100,
                "second custom tactic active");
        require(state.applyTemporary(33) && state.isTemporary() && state.attackPercent() == 33,
                "temporary manual ratio");
        require(state.updateTactic(first, "돌격", 100), "rename by stable ID");
        require(state.orderedTactics().get(0).id().equals(first), "rename never rekeys ID");
        require(state.moveTactic(first, 1) && state.orderedTactics().get(1).id().equals(first),
                "quick order moves without renaming");
        require(state.activate(first) && !state.isTemporary() && state.attackPercent() == 100,
                "stored tactic restoration");
        require(state.applyTemporary(0) && state.clearTemporary(),
                "temporary zero can be reverted without overwriting");
        require(state.attackPercent() == 100, "revert restores stored ratio");
        require(state.removeTactic(duplicate) && state.orderedTactics().size() == 1,
                "delete exact duplicate by ID");
        require(state.removeTactic(first) && state.activeId() == null
                && state.attackPercent() == -1, "deleting active does not inject fallback");
        require(!state.saveTactic(" ", 50) && !state.saveTactic("invalid", -1)
                && !state.saveTactic("invalid", 101), "validation boundaries");
        for (int i = 0; i < 8; i++)
            require(state.createTactic("전술" + i, i * 10) != null, "8 named tactics");
        require(state.createTactic("아홉번째", 50) == null, "no ninth tactic");
        require(state.saveTactic("전술0", 55), "legacy command edits existing name");
        require(state.orderedTactics().size() == 8, "legacy update never duplicates");
        require(state.activate("전술1") && state.attackPercent() == 10,
                "legacy command selects by name");
        require(state.removeTactic("전술1"), "legacy command deletes by name");
    }

    private static void verifyLegacyNamedTacticLoad() {
        try {
            CompoundTag old = new CompoundTag();
            old.putInt("alive", 16);
            old.putInt("regroup", 16);
            ListTag tactics = new ListTag();
            CompoundTag item = new CompoundTag();
            item.putString("name", "기존 전술");
            item.putInt("attackPercent", 75);
            tactics.add(item);
            old.put("tactics", tactics);
            old.putString("active", "기존 전술");
            Method load = SwarmSavedData.State.class.getDeclaredMethod("load", CompoundTag.class);
            load.setAccessible(true);
            var state = (SwarmSavedData.State) load.invoke(null, old);
            require(state.orderedTactics().size() == 1, "legacy tactic preserved");
            require(state.attackPercent() == 75, "legacy selected percent preserved");
            var stable = state.activeId();
            require(stable != null, "legacy entry gains stable UUID");
            Method save = SwarmSavedData.State.class.getDeclaredMethod("save");
            save.setAccessible(true);
            var persisted = (CompoundTag) save.invoke(state);
            var first = persisted.getList("tactics", 10).getCompound(0);
            require(first.hasUUID("id") && first.getUUID("id").equals(stable),
                    "next save persists migrated ID");
            require(persisted.getUUID("activeId").equals(stable),
                    "next save preserves active ID");
        } catch (ReflectiveOperationException unexpected) {
            throw new AssertionError("legacy tactic migration inspection failed", unexpected);
        }
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) < 0.0001;
    }

    private static void require(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
}
