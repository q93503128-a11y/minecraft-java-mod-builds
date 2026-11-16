import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import kr.moonseungjun.anomalousclasses.state.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Deterministic server-contract fixtures. This is not a Minecraft client or multiplayer test. */
public final class ParasiteFoundationVerifier {
    public static void main(String[] args) throws Exception {
        // Ground/camera contract: no diagonal speed advantage, no forged
        // NaN/pitch or keymask, zero-motion LOOK stays valid for stationary aim.
        check(ParasiteMotionRules.validInput(0, 0F, -90F)
                && ParasiteMotionRules.validInput(63, 360F, 90F)
                && !ParasiteMotionRules.validInput(64, 0F, 0F)
                && !ParasiteMotionRules.validInput(-1, 0F, 0F)
                && !ParasiteMotionRules.validInput(1, Float.NaN, 0F)
                && !ParasiteMotionRules.validInput(1, Float.POSITIVE_INFINITY, 0F)
                && !ParasiteMotionRules.validInput(1, 0F, -90.1F),
                "reject invalid movement masks and angles");
        check(ParasiteMotionRules.fresh(100, 90)
                && ParasiteMotionRules.fresh(100, 100)
                && !ParasiteMotionRules.fresh(101, 90)
                && !ParasiteMotionRules.fresh(90, 101),
                "ten-tick input freshness and monotonic server time");
        check(ParasiteMotionRules.planar(0, 75F).lengthSqr() == 0
                && ParasiteMotionRules.fresh(100, 95),
                "stationary host still has fresh camera rotation");
        check(Math.abs(ParasiteMotionRules.planar(1, 0).z - 1) < .0001
                && Math.abs(ParasiteMotionRules.planar(1 | 8, 0).length() - 1) < .0001
                && Math.abs(ParasiteMotionRules.planar(1 | 2, 0).length()) < .0001,
                "forward, diagonal normalization and opposing movement keys");
        check(ParasiteMotionRules.sprint(1 | 32)
                && !ParasiteMotionRules.sprint(2 | 32)
                && !ParasiteMotionRules.sprint(1 | 2 | 32)
                && !ParasiteMotionRules.sprint(8 | 32),
                "sprint requires forward travel and cannot activate when blocked by reverse");
        check(Math.abs(ParasiteMotionRules.swimVertical(-90F, .1) - .1) < .0001
                && Math.abs(ParasiteMotionRules.swimVertical(90F, .1) + .1) < .0001
                && ParasiteMotionRules.swimVertical(-90F, 2) == .24
                && ParasiteMotionRules.swimVertical(0F, .5) == 0,
                "aquatic pitch and bounded vertical movement");
        check(ParasiteMotionRules.yaw(360F) == 0F
                && ParasiteMotionRules.yaw(-270F) == 90F,
                "canonical wraparound camera yaw");
        // Mounted actions: server validates host-eye direction and caps the
        // extra vanilla network reach to the actual host's permissible range.
        check(ParasiteHostReachService.blockLimit(4.5) == 4.5
                && ParasiteHostReachService.blockLimit(50) == 5.5
                && ParasiteHostReachService.blockLimit(-1) == 3,
                "host-block interaction distance is bounded");
        check(ParasiteHostReachService.entityLimit(3.0) == 3.0
                && ParasiteHostReachService.entityLimit(30) == 5.0,
                "host-entity interaction distance is bounded");
        Vec3 eye = new Vec3(0, 2, 0);
        check(ParasiteHostReachService.aimedToward(eye, new Vec3(0, 2, 3), new Vec3(0, 0, 1))
                && !ParasiteHostReachService.aimedToward(eye, new Vec3(0, 2, -3), new Vec3(0, 0, 1))
                && !ParasiteHostReachService.aimedToward(eye, new Vec3(3, 2, 0), new Vec3(0, 0, 1)),
                "server host interaction cannot reach behind or sideways");
        check(ParasiteHostCombatService.bodyRange(5.75, .7) == 3
                && ParasiteHostCombatService.bodyRange(5.75, 2.0) > 3
                && ParasiteHostCombatService.bodyRange(100, 100) == 5.5,
                "large body melee scaling is bounded without using mounting bonus");
        check(ParasiteHostCombatService.bodySwingCone(
                new Vec3(0, 0, 1), new Vec3(0, -1, 3), 2.7)
                && !ParasiteHostCombatService.bodySwingCone(
                new Vec3(0, 0, 1), new Vec3(0, -1, -3), 2.7)
                && !ParasiteHostCombatService.bodySwingCone(
                new Vec3(0, 0, 1), new Vec3(0, -1, 3), 1.5),
                "large host chest swing is forward-only and size-gated");
        check(ParasiteSpeciesService.passengerDestination(
                new Vec3(10, 50, -8), new Vec3(10, 52, -8),
                new Vec3(14, 51, 1)).equals(new Vec3(14, 53, 1)),
                "teleport projects the mounted player seat with the original host");
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
        check(ParasiteRules.hasTurtleStability("minecraft:turtle", 0)
                && !ParasiteRules.hasTurtleStability("minecraft:pig", 99)
                && ParasiteRules.hasTurtleStability("minecraft:pig", 100),
                "turtle innate shell resistance and mastery-100 permanent inheritance");
        check(ParasiteRules.supported(ResourceLocation.parse("minecraft:ravager")) != null,
                "native ravager may use the fourth direct species slot");
        // 34 vanilla original bodies + 20 audited external non-boss profiles.
        // Exact mod Java fingerprints are a safety boundary after updates.
        check(ParasiteRules.supportedCount() == 54, "34 vanilla + 20 pinned external profiles");
        String[] externalIds = {
                "born_in_chaos_v1:maggot", "born_in_chaos_v1:baby_spider",
                "born_in_chaos_v1:diamond_termite", "born_in_chaos_v1:decaying_zombie",
                "born_in_chaos_v1:barrel_zombie", "born_in_chaos_v1:zombie_fisherman",
                "born_in_chaos_v1:zombie_clown", "born_in_chaos_v1:zombie_lumberjack",
                "born_in_chaos_v1:dread_hound", "born_in_chaos_v1:zombie_bruiser",
                "cataclysm:koboleton", "cataclysm:draugr", "cataclysm:deepling",
                "cataclysm:elite_draugr", "cataclysm:deepling_brute",
                "cataclysm:royal_draugr", "cataclysm:coral_golem",
                "mowziesmobs:grottol", "mowziesmobs:bluff",
                "mutantmonsters:spider_pig"
        };
        check(externalIds.length == 20, "bounded curated external set");
        for (String id : externalIds) {
            var key = ResourceLocation.parse(id);
            var spec = ParasiteRules.supported(key);
            check(spec != null && !spec.expectedClass().isBlank()
                    && ParasiteRules.matchesHostClass(key, spec.expectedClass())
                    && !ParasiteRules.matchesHostClass(key, "unverified.mod.Entity"),
                    "exact external EntityType/class fingerprint " + id);
        }
        check(ParasiteRules.matchesHostClass(ResourceLocation.parse("minecraft:pig"),
                "net.minecraft.world.entity.animal.Pig")
                && !ParasiteRules.matchesHostClass(ResourceLocation.parse("cataclysm:ignis"),
                "any.ClassName"),
                "unlisted bosses cannot be authorized");
        String[] newGroundHosts = {
                "mooshroom", "rabbit", "fox", "goat", "horse", "donkey", "mule",
                "panda", "polar_bear", "hoglin", "zoglin", "husk", "zombie_villager",
                "zombified_piglin", "piglin", "piglin_brute", "vindicator", "ravager",
                "sniffer", "armadillo", "cave_spider", "frog", "ocelot", "turtle"
        };
        for (String species : newGroundHosts) {
            var entry = ParasiteRules.supported(ResourceLocation.parse("minecraft:" + species));
            check(entry != null && "ground".equals(entry.adapter())
                    && entry.requiredBd() >= 0 && entry.speedFactor() > 0,
                    "configured original ground host " + species);
        }
        check(ParasiteRules.supported(ResourceLocation.parse("minecraft:witch")) == null
                && ParasiteRules.supported(ResourceLocation.parse("minecraft:phantom")) == null
                && ParasiteRules.supported(ResourceLocation.parse("cataclysm:ignis")) == null,
                "unimplemented flying/ranged/external boss must remain unsupported");
        check("aquatic".equals(guardian.adapter()), "guardian catalog aquatic control");
        var amphibious = new ParasiteSavedData();
        UUID swimmer = UUID.randomUUID();
        check(ParasiteRules.hasAquaticAdaptation("cataclysm:deepling", amphibious.player(swimmer))
                && !ParasiteRules.hasAquaticAdaptation("minecraft:pig", amphibious.player(swimmer)),
                "modded deepling water affinity native but not inherited");
        amphibious.reward(swimmer, "cataclysm:deepling", 999, new PlayerClassProfile());
        check(!ParasiteRules.hasAquaticAdaptation("minecraft:pig", amphibious.player(swimmer)),
                "aquatic affinity must not transfer at mastery 99");
        amphibious.reward(swimmer, "cataclysm:deepling", 1, new PlayerClassProfile());
        check(ParasiteRules.hasAquaticAdaptation("minecraft:pig", amphibious.player(swimmer)),
                "aquatic affinity transfers only at mastery 100");
        var climbing = new ParasiteSavedData();
        UUID climber = UUID.randomUUID();
        check(ParasiteRules.hasWallClimb("born_in_chaos_v1:baby_spider", climbing.player(climber))
                && !ParasiteRules.hasWallClimb("minecraft:pig", climbing.player(climber)),
                "modded native spider climb");
        climbing.reward(climber, "born_in_chaos_v1:baby_spider", 999,
                new PlayerClassProfile());
        check(!ParasiteRules.hasWallClimb("minecraft:pig", climbing.player(climber)),
                "modded spider climb must not transfer at mastery 99");
        climbing.reward(climber, "born_in_chaos_v1:baby_spider", 1,
                new PlayerClassProfile());
        check(ParasiteRules.hasWallClimb("minecraft:pig", climbing.player(climber)),
                "modded spider climb transfers permanently at 100");
        var leapData = new ParasiteSavedData();
        UUID jumper = UUID.randomUUID();
        check(ParasiteRules.jumpImpulse("minecraft:goat", leapData.player(jumper)) == 0.56
                && ParasiteRules.jumpImpulse("minecraft:pig", leapData.player(jumper)) == 0.42,
                "native goat leap must work on first possession");
        leapData.reward(jumper, "minecraft:goat", 999, new PlayerClassProfile());
        check(leapData.player(jumper).mastery("minecraft:goat") == 99
                && ParasiteRules.jumpImpulse("minecraft:pig", leapData.player(jumper)) == 0.42,
                "goat jump does not transfer at mastery 99");
        leapData.reward(jumper, "minecraft:goat", 1, new PlayerClassProfile());
        check(ParasiteRules.jumpImpulse("minecraft:pig", leapData.player(jumper)) == 0.56
                && ParasiteRules.jumpImpulse("minecraft:slime", leapData.player(jumper)) == 0.58,
                "ground jump transfers at 100 without replacing stronger native slime");
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
        // Regression: applying guardian damage or moving the ender host before
        // checking global recovery previously allowed free effects on rejected casts.
        ParasiteTraitSavedData transactional = new ParasiteTraitSavedData();
        UUID actor = UUID.randomUUID();
        AtomicInteger effects = new AtomicInteger();
        check(!transactional.castIf(actor, ParasiteTraitService.GUARDIAN_BEAM,
                80, 480, 60, () -> { effects.incrementAndGet(); return false; })
                && transactional.remaining(actor, ParasiteTraitService.GUARDIAN_BEAM, 80) == 0,
                "failed beam target spends no cooldown");
        check(transactional.castIf(actor, ParasiteTraitService.RAVAGER_ROAR,
                100, 800, 60, () -> { effects.incrementAndGet(); return true; }),
                "ravager roar successfully executes and commits its cooldown");
        check(!transactional.castIf(actor, ParasiteTraitService.GUARDIAN_BEAM,
                105, 480, 60, () -> { effects.incrementAndGet(); return true; })
                && effects.get() == 2,
                "shared recovery rejects guardian effect BEFORE its damage callback");
        check(!transactional.castIf(actor, ParasiteTraitService.ENDER_BLINK,
                105, 560, 0, () -> { effects.incrementAndGet(); return true; })
                && effects.get() == 2,
                "shared recovery rejects ender teleport BEFORE the relocation");
        check(transactional.castIf(actor, ParasiteTraitService.GUARDIAN_BEAM,
                160, 480, 60, () -> {
                    effects.incrementAndGet();
                    return !transactional.castIf(actor, ParasiteTraitService.ENDER_BLINK,
                            160, 560, 0, () -> { effects.incrementAndGet(); return true; });
                }) && effects.get() == 3,
                "reentrant cast blocked while a valid beam executes");
        check(transactional.remaining(actor, ParasiteTraitService.GUARDIAN_BEAM, 200) > 0
                && transactional.remaining(actor, ParasiteTraitService.RAVAGER_ROAR, 200) > 0,
                "individual cooldowns remain intact after both successful casts");
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
        System.out.println("PASS: 54 hosts, original-host interaction reach, giant melee cone, mounted teleport projection, cooldown, input, mastery and NBT");
    }
    private static void check(boolean truth, String label) {
        if (!truth) throw new AssertionError(label);
    }
}
