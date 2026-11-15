import java.lang.reflect.Method;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.state.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Deterministic server-contract fixtures. This is not a Minecraft client or multiplayer test. */
public final class ParasiteFoundationVerifier {
    public static void main(String[] args) throws Exception {
        PlayerClassProfile profile = new PlayerClassProfile();
        var pig = ParasiteRules.supported(ResourceLocation.parse("minecraft:pig"));
        var cow = ParasiteRules.supported(ResourceLocation.parse("minecraft:cow"));
        var zombie = ParasiteRules.supported(ResourceLocation.parse("minecraft:zombie"));
        var spider = ParasiteRules.supported(ResourceLocation.parse("minecraft:spider"));
        var creeper = ParasiteRules.supported(ResourceLocation.parse("minecraft:creeper"));
        var slime = ParasiteRules.supported(ResourceLocation.parse("minecraft:slime"));
        var guardian = ParasiteRules.supported(ResourceLocation.parse("minecraft:guardian"));
        var enderman = ParasiteRules.supported(ResourceLocation.parse("minecraft:enderman"));
        check(pig.requiredBd() == 0 && cow.requiredBd() == 20
                && zombie.requiredBd() == 100 && spider.requiredBd() == 160
                && creeper.requiredBd() == 240 && slime.requiredBd() == 180,
                "starter, slime and creeper BD gates");
        check(ParasiteRules.jumpImpulse("minecraft:slime", 0) == 0.58
                && ParasiteRules.jumpImpulse("minecraft:pig", 99) == 0.42
                && ParasiteRules.jumpImpulse("minecraft:pig", 100) == 0.50,
                "slime jump is innate and transfers only at mastery 100");
        check(ParasiteRules.hasSlimeElasticity("minecraft:slime", 0)
                && !ParasiteRules.hasSlimeElasticity("minecraft:zombie", 99)
                && ParasiteRules.hasSlimeElasticity("minecraft:zombie", 100),
                "slime environmental passive mastery boundary");
        check(ParasiteRules.fallDamageFactor("minecraft:zombie", 99) == 1.0F
                && ParasiteRules.fallDamageFactor("minecraft:zombie", 100) == 0.5F,
                "elastic fall mitigation only after inheritance");
        check(guardian.requiredBd() == 600 && enderman.requiredBd() == 900
                && ParasiteRules.infiltrationTicks(guardian, 600, profile) == 160,
                "guardian and enderman BD gates / starting intrusion");
        check(ParasiteRules.hasGuardianAdaptation(true, 0)
                && !ParasiteRules.hasGuardianAdaptation(false, 99)
                && ParasiteRules.hasGuardianAdaptation(false, 100),
                "guardian native vs mastery-100 water passive");
        check(ParasiteRules.hasEnderDodge(true, 0)
                && !ParasiteRules.hasEnderDodge(false, 99)
                && ParasiteRules.hasEnderDodge(false, 100),
                "enderman projectile evasion unlock boundary");
        check(ParasiteRules.supported(ResourceLocation.parse("minecraft:warden")) == null
                && ParasiteRules.plannedRequirement(ResourceLocation.parse("minecraft:warden")) == 5000,
                "unsupported boss must not appear as controllable");
        check(ParasiteRules.infiltrationTicks(pig, 0, profile) == 30, "pig infiltration");
        check(ParasiteRules.infiltrationTicks(zombie, 100, profile) == 80, "zombie infiltration");
        check(ParasiteRules.infiltrationTicks(zombie, 10000, profile) >= 44,
                "infiltration never becomes instant");
        check(ParasiteRules.lockoutTicks(profile) == 1200
                && ParasiteRules.maxEmergencyCharges(profile) == 0, "untrained death lock");
        check(ParasiteRules.humanMaximum(profile) == 8.0F, "weak human");
        check(ParasiteRules.healthMultiplier(100, 0, profile) > 1.049
                && ParasiteRules.damageMultiplier(100, 0, profile) > 1.059, "permanent BD scales host");
        profile.equipFocus(new ClassFocus(ClassId.PARASITE_MORPHER, FocusTier.values()[4]));
        check(ParasiteRules.focus(profile) == .40, "final focus +40%");
        check(ParasiteRules.healthMultiplier(0, 0, profile) == 1.40, "only single focus tier");

        UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        ParasiteSavedData data = new ParasiteSavedData();
        check(data.claim(owner, first, "minecraft:overworld"), "claim starter host");
        check(!data.claim(other, first, "minecraft:overworld"), "no competing player can claim");
        check(data.ownerOf(first).equals(owner), "claim owner index");
        check(data.claim(owner, second, "minecraft:overworld"), "atomic transfer");
        check(data.ownerOf(first) == null && owner.equals(data.ownerOf(second)), "old claim released on transfer");
        check(!data.release(other, second), "foreign owner cannot release");
        data.reward(owner, "minecraft:pig", 12, new PlayerClassProfile());
        check(data.player(owner).bd() == 12 && data.player(owner).mastery("minecraft:pig") == 1,
                "BD and species proficiency are separate permanent progress");
        check(data.firstBiome(owner, "anomalousclasses:minecraft_overworld_minecraft_plains")
                && !data.firstBiome(owner, "anomalousclasses:minecraft_overworld_minecraft_plains"),
                "new biome bonus is one-time");
        check(data.firstMilestone(owner, "anomalousclasses:mine_iron")
                && !data.firstMilestone(owner, "anomalousclasses:mine_iron"),
                "survival milestones cannot be replayed");
        check(!data.recordActiveTicks(owner, 4000, new PlayerClassProfile()), "no passive early activity reward");
        check(data.recordActiveTicks(owner, 800, new PlayerClassProfile()), "4 minutes eligible activity");
        check(data.hostDied(owner, second, 100L, 1200), "host death is single transition");
        check(!data.hostDied(owner, second, 101L, 1200), "death replay rejected");
        check(!data.mayAttempt(owner, 200L) && data.mayAttempt(owner, 1300L),
                "lockout survives repeated attempts");
        check(data.player(owner).bd() == 12, "host death never consumes BD");

        ParasiteSavedData speciesData = new ParasiteSavedData();
        UUID collector = UUID.randomUUID();
        PlayerClassProfile collectorProfile = new PlayerClassProfile();
        speciesData.reward(collector, "minecraft:guardian", 999, collectorProfile);
        check(speciesData.player(collector).mastery("minecraft:guardian") == 99,
                "mastery 99 must not prematurely unlock cross-host traits");
        speciesData.reward(collector, "minecraft:guardian", 1, collectorProfile);
        check(speciesData.player(collector).mastery("minecraft:guardian") == 100
                && speciesData.player(collector).mastery("minecraft:enderman") == 0,
                "guardian proficiency reaches 100 without cross-species inflation");
        CompoundTag collection = speciesData.save(new CompoundTag(), RegistryAccess.EMPTY);
        Method readCollection = ParasiteSavedData.class.getDeclaredMethod(
                "load", CompoundTag.class, HolderLookup.Provider.class);
        readCollection.setAccessible(true);
        ParasiteSavedData reloadedCollection = (ParasiteSavedData)
                readCollection.invoke(null, collection, RegistryAccess.EMPTY);
        check(reloadedCollection.player(collector).mastery("minecraft:guardian") == 100,
                "guardian mastery 100 survives server NBT round-trip");
        CompoundTag encoded = data.save(new CompoundTag(), RegistryAccess.EMPTY);
        Method load = ParasiteSavedData.class.getDeclaredMethod(
                "load", CompoundTag.class, HolderLookup.Provider.class);
        load.setAccessible(true);
        ParasiteSavedData restored = (ParasiteSavedData) load.invoke(null, encoded, RegistryAccess.EMPTY);
        check(restored.player(owner).bd() == 12
                && restored.player(owner).mastery("minecraft:pig") == 1
                && restored.player(owner).host() == null
                && restored.player(owner).deathLockUntil() == 1300L
                && !restored.firstMilestone(owner, "anomalousclasses:mine_iron"),
                "permanent BD, species, claim and death timer save/load");

        ParasiteAbilitySavedData abilities = new ParasiteAbilitySavedData();
        UUID body = UUID.randomUUID();
        check(abilities.ready(owner, 0, 100L) && abilities.anchor(owner, 100L),
                "anchoring consumes first Z cooldown");
        check(!abilities.ready(owner, 0, 100L)
                && abilities.consumeAnchor(owner, 101L)
                && !abilities.consumeAnchor(owner, 102L),
                "anchoring is a single-use buff");
        check(abilities.heal(owner, body, 10.0F, 100L), "X starts gradual repair");
        check(abilities.healTick(owner, body, 101L, 20.0F) > 0.0F,
                "X heals original body over time");
        check(!abilities.heal(owner, body, 10.0F, 102L), "X cooldown cannot be bypassed");
        check(abilities.awaken(owner, body, 100L), "C awakens the original body");
        check(abilities.isAwakened(owner, body, 101L)
                && !abilities.isAwakened(owner, UUID.randomUUID(), 101L),
                "C cannot transfer onto another body");
        abilities.endHost(owner, body);
        check(!abilities.isAwakened(owner, body, 101L)
                && abilities.player(owner).healHost() == null
                && !abilities.ready(owner, 1, 101L),
                "leaving body cancels effects but not cooldowns");
        CompoundTag encodedAbilities = abilities.save(new CompoundTag(), RegistryAccess.EMPTY);
        Method abilityLoad = ParasiteAbilitySavedData.class.getDeclaredMethod(
                "load", CompoundTag.class, HolderLookup.Provider.class);
        abilityLoad.setAccessible(true);
        ParasiteAbilitySavedData restoredAbilities = (ParasiteAbilitySavedData)
                abilityLoad.invoke(null, encodedAbilities, RegistryAccess.EMPTY);
        check(restoredAbilities.player(owner).readyAt(0) == 900L
                && restoredAbilities.player(owner).readyAt(1) == 1000L
                && restoredAbilities.player(owner).readyAt(2) == 800L,
                "ability cooldown save/load");
        ParasiteTraitSavedData traits = new ParasiteTraitSavedData();
        String blast = "anomalousclasses:creeper_blast";
        check(traits.cast(owner, blast, 50, 600, 60), "first inherited cast");
        check(!traits.cast(owner, blast, 100, 600, 60), "individual cooldown blocks repeat");
        check(!traits.cast(owner, "anomalousclasses:other_trait", 100, 200, 60),
                "shared cooldown blocks chained attacks");
        check(traits.cast(owner, "anomalousclasses:other_trait", 110, 200, 60),
                "shared recovery expires independently");
        UUID fighter = UUID.randomUUID();
        check(traits.cast(fighter, ParasiteTraitService.GUARDIAN_BEAM, 50, 480, 60)
                && !traits.cast(fighter, ParasiteTraitService.ENDER_BLINK, 80, 560, 0)
                && traits.cast(fighter, ParasiteTraitService.ENDER_BLINK, 110, 560, 0)
                && !traits.cast(fighter, ParasiteTraitService.ENDER_BLINK, 160, 560, 0)
                && traits.cast(fighter, ParasiteTraitService.CREEPER_BLAST, 111, 600, 60),
                "guardian attack recovery vs non-offensive blink and per-ability cooldown");
        CompoundTag traitTag = traits.save(new CompoundTag(), RegistryAccess.EMPTY);
        Method traitLoad = ParasiteTraitSavedData.class.getDeclaredMethod(
                "load", CompoundTag.class, HolderLookup.Provider.class);
        traitLoad.setAccessible(true);
        ParasiteTraitSavedData savedTraits = (ParasiteTraitSavedData)
                traitLoad.invoke(null, traitTag, RegistryAccess.EMPTY);
        check(!savedTraits.cast(owner, blast, 120, 600, 60)
                && savedTraits.cast(owner, blast, 650, 600, 60),
                "inherited cooldown persists through save/load");
        check(!savedTraits.cast(fighter, ParasiteTraitService.GUARDIAN_BEAM, 200, 480, 60)
                && !savedTraits.cast(fighter, ParasiteTraitService.ENDER_BLINK, 200, 560, 0)
                && savedTraits.cast(fighter, ParasiteTraitService.GUARDIAN_BEAM, 531, 480, 60),
                "guardian and enderman cooldowns persist separately after restart");
        System.out.println("PASS: parasite claims, BD, host gates, guardian/enderman/slime mastery, 3 actives and inherited cooldown persistence");
    }
    private static void check(boolean truth, String label) {
        if (!truth) throw new AssertionError(label);
    }
}
