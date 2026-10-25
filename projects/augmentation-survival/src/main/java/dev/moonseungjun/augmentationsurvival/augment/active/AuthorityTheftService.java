package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CloneCombatSnapshot;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CloneEntityTypes;
import dev.moonseungjun.augmentationsurvival.augment.companion.CombatCloneEntity;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated Mythic authority channel. Supported boss identities are permanently stolen on the
 * first root-direct kill, then every stolen package is released in one server-authoritative burst.
 */
public final class AuthorityTheftService {
    public static final String AUTHORITY_THEFT_ID = id("authority_theft");
    private static final String COOLDOWN_KEY = AUTHORITY_THEFT_ID + "#release";
    private static final int COOLDOWN_TICKS = 120 * 20;
    private static final String MCENDGAME_DYNAMIC_PROFILE = "mcendgame_dungeon_boss";

    private static final Set<String> VANILLA_PACKAGES = Set.of(
            "vanilla_wither",
            "vanilla_ender_dragon"
    );

    private static final Set<String> CHRONO_DAWN_PACKAGES = Set.of(
            "chronodawn_time_guardian",
            "chronodawn_chronos_warden",
            "chronodawn_clockwork_colossus",
            "chronodawn_temporal_phantom",
            "chronodawn_entropy_keeper",
            "chronodawn_time_tyrant"
    );

    private static final Set<String> WORLD_BOSSES_PACKAGES = Set.of(
            "worldbosses_sylvaron",
            "worldbosses_aureon",
            "worldbosses_nekhraz",
            "worldbosses_varkhul",
            "worldbosses_morvath",
            "worldbosses_elyndra",
            "worldbosses_astrael",
            "worldbosses_shadow_king",
            "worldbosses_dragon_mother",
            "worldbosses_storm_griffin",
            "worldbosses_shadow_weaver"
    );

    private static final Set<String> MCENDGAME_PACKAGES = Set.of(
            "mcendgame_arachne",
            "mcendgame_bonecrusher",
            "mcendgame_elf_duelist",
            "mcendgame_beakburn",
            "mcendgame_beastweaver"
    );

    private AuthorityTheftService() { }

    public static void initialize() {
        AugmentCatalog.require(AUTHORITY_THEFT_ID);
        CloneEntityTypes.initialize();
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(AuthorityTheftService::afterKill);
    }

    private static void afterKill(
            ServerLevel level,
            net.minecraft.world.entity.Entity killer,
            LivingEntity killed,
            net.minecraft.world.damagesource.DamageSource source
    ) {
        if (!(killer instanceof ServerPlayer player)) return;
        if (AugmentationPlayerStateService.state(player).level(AUTHORITY_THEFT_ID) <= 0) return;
        if (!CombatProvenanceContext.resolve(source).isRootDirect()) return;
        EntityCompatibilityProfileService.resolve(killed).ifPresent(profile -> {
            if (!profile.boss()) return;
            String packageId = resolvePackageId(profile.id(), killed);
            if (packageId == null || !supports(packageId)) return;
            AugmentationPlayerState before = AugmentationPlayerStateService.state(player);
            AugmentationPlayerState next = before.withStolenAuthority(packageId);
            if (next.equals(before)) return;
            AugmentationPlayerStateService.replace(player, next);
            player.sendSystemMessage(Component.translatable(
                    "message.augmentation_survival.authority_theft.stolen",
                    killed.getDisplayName()
            ));
        });
    }

    private static String resolvePackageId(String profileId, LivingEntity killed) {
        if (!MCENDGAME_DYNAMIC_PROFILE.equals(profileId)) return profileId;
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(killed.getType());
        if (entityId == null || !"mcendgame".equals(entityId.getNamespace())) return null;
        return switch (entityId.getPath()) {
            case "arachne" -> "mcendgame_arachne";
            case "bonecrusher" -> "mcendgame_bonecrusher";
            case "elf_duelist" -> "mcendgame_elf_duelist";
            case "beakburn" -> "mcendgame_beakburn";
            case "beastweaver" -> "mcendgame_beastweaver";
            default -> null;
        };
    }

    public static boolean tryRelease(ServerPlayer player) {
        if (player.isDeadOrDying()) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(AUTHORITY_THEFT_ID) <= 0 || state.stolenAuthorityBossIds().isEmpty()) return false;
        long now = player.level().getGameTime();
        if (state.cooldownReadyTick(COOLDOWN_KEY) > now) return false;

        int released = 0;
        for (String bossId : state.stolenAuthorityBossIds()) {
            if (releasePackage(player, bossId)) released++;
        }
        if (released <= 0) return false;

        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(player, latest.withCooldownReadyTick(COOLDOWN_KEY, now + COOLDOWN_TICKS));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.65F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                    90, 0.8D, 1.1D, 0.8D, 0.15D);
        }
        return true;
    }

    public static boolean supports(String bossId) {
        return VANILLA_PACKAGES.contains(bossId)
                || CHRONO_DAWN_PACKAGES.contains(bossId)
                || WORLD_BOSSES_PACKAGES.contains(bossId)
                || MCENDGAME_PACKAGES.contains(bossId);
    }

    private static boolean releasePackage(ServerPlayer player, String bossId) {
        return switch (bossId) {
            case "vanilla_wither" -> { releaseWither(player); yield true; }
            case "vanilla_ender_dragon" -> { releaseEnderDragon(player); yield true; }
            case "chronodawn_time_guardian" -> { releaseTimeGuardian(player); yield true; }
            case "chronodawn_chronos_warden" -> { releaseChronosWarden(player); yield true; }
            case "chronodawn_clockwork_colossus" -> { releaseClockworkColossus(player); yield true; }
            case "chronodawn_temporal_phantom" -> { releaseTemporalPhantom(player); yield true; }
            case "chronodawn_entropy_keeper" -> { releaseEntropyKeeper(player); yield true; }
            case "chronodawn_time_tyrant" -> { releaseTimeTyrant(player); yield true; }
            case "worldbosses_sylvaron" -> { releaseSylvaron(player); yield true; }
            case "worldbosses_aureon" -> { releaseAureon(player); yield true; }
            case "worldbosses_nekhraz" -> { releaseNekhraz(player); yield true; }
            case "worldbosses_varkhul" -> { releaseVarkhul(player); yield true; }
            case "worldbosses_morvath" -> { releaseMorvath(player); yield true; }
            case "worldbosses_elyndra" -> { releaseElyndra(player); yield true; }
            case "worldbosses_astrael" -> { releaseAstrael(player); yield true; }
            case "worldbosses_shadow_king" -> { releaseShadowKing(player); yield true; }
            case "worldbosses_dragon_mother" -> { releaseDragonMother(player); yield true; }
            case "worldbosses_storm_griffin" -> { releaseStormGriffin(player); yield true; }
            case "worldbosses_shadow_weaver" -> { releaseShadowWeaver(player); yield true; }
            case "mcendgame_arachne" -> { releaseArachne(player); yield true; }
            case "mcendgame_bonecrusher" -> { releaseBonecrusher(player); yield true; }
            case "mcendgame_elf_duelist" -> { releaseElfDuelist(player); yield true; }
            case "mcendgame_beakburn" -> { releaseBeakburn(player); yield true; }
            case "mcendgame_beastweaver" -> { releaseBeastweaver(player); yield true; }
            default -> false;
        };
    }

    /** Vanilla Wither: three-target wither pressure, close detonation and one native regeneration pulse. */
    private static void releaseWither(ServerPlayer player) {
        List<LivingEntity> targets = hostiles(player, 24.0D).stream()
                .sorted((left, right) -> Double.compare(player.distanceToSqr(left), player.distanceToSqr(right)))
                .limit(3)
                .toList();
        for (LivingEntity target : targets) {
            hurt(player, target, scaledDamage(player, 0.85D, 8.0D));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1));
        }
        for (LivingEntity enemy : hostiles(player, 5.0D)) {
            hurt(player, enemy, scaledDamage(player, 0.45D, 4.0D));
            pushAway(player, enemy, 0.8D, 0.25D);
        }
        player.heal(1.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.9F, 0.8F);
        particles(player, ParticleTypes.SOUL, 110, 6.0D);
    }

    /** Vanilla Ender Dragon: charge knockback followed by a dragon-breath cone. */
    private static void releaseEnderDragon(ServerPlayer player) {
        for (LivingEntity enemy : hostiles(player, 6.0D)) {
            hurt(player, enemy, scaledDamage(player, 0.65D, 6.0D));
            pushAway(player, enemy, 1.6D, 0.65D);
        }
        for (LivingEntity enemy : hostilesInCone(player, 12.0D, 70.0D)) {
            hurt(player, enemy, scaledDamage(player, 0.8D, 7.0D));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.75F, 1.2F);
        particles(player, PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), 120, 8.0D);
    }

    /** Time Guardian: blink pressure + temporal distortion AoE + slowed primary target. */
    private static void releaseTimeGuardian(ServerPlayer player) {
        LivingEntity target = nearestHostile(player, 20.0D);
        if (target != null) {
            blinkBehind(player, target, 3.0D);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
        }
        float damage = scaledDamage(player, 0.75D, 2.0D);
        for (LivingEntity enemy : hostiles(player, 4.0D)) {
            hurt(player, enemy, damage);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            enemy.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 0));
        }
        particles(player, ParticleTypes.WITCH, 48, 4.0D);
    }

    /** Chronos Warden: five-second stone stance and the source boss's 4-block ground slam. */
    private static void releaseChronosWarden(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 3));
        for (LivingEntity enemy : hostiles(player, 4.0D)) {
            hurt(player, enemy, 2.0F);
            pushAway(player, enemy, 1.15D, 0.45D);
        }
        LivingEntity primary = nearestHostile(player, 5.0D);
        if (primary != null) primary.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 1));
        particles(player, ParticleTypes.SOUL, 56, 4.0D);
    }

    /** Clockwork Colossus: overcharge, repair protocol, gear strike and linear ground shockwave. */
    private static void releaseClockworkColossus(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
        player.heal(player.getMaxHealth() * 0.15F);
        LivingEntity primary = nearestHostile(player, 16.0D);
        if (primary != null) hurt(player, primary, 8.0F);

        Vec3 look = flatLook(player);
        for (LivingEntity enemy : hostiles(player, 11.0D)) {
            Vec3 offset = enemy.position().subtract(player.position());
            double forward = offset.dot(look);
            if (forward < 0.0D || forward > 10.0D) continue;
            double lateral = Math.abs(offset.x * -look.z + offset.z * look.x);
            if (lateral > 1.5D) continue;
            hurt(player, enemy, 6.0F);
            enemy.push(look.x * 0.8D, 0.25D, look.z * 0.8D);
        }
        lineParticles(player, look, ParticleTypes.ELECTRIC_SPARK, 10);
    }

    /** Temporal Phantom: phase shift, warp debuff, two 15-second combat echoes and blink strike. */
    private static void releaseTemporalPhantom(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4));
        LivingEntity target = nearestHostile(player, 20.0D);
        if (target != null) {
            hurt(player, target, scaledDamage(player, 0.8D, 4.0D));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 0));
            if (blinkBehind(player, target, 6.0D)) hurt(player, target, 8.0F);
        }
        spawnAuthorityEchoes(player, 2, 20.0F, 4.0D, 15 * 20);
        particles(player, ParticleTypes.PORTAL, 60, 3.0D);
    }

    /** Entropy Keeper: decay aura, temporal rot and the source boss's 6-block entropy burst. */
    private static void releaseEntropyKeeper(ServerPlayer player) {
        LivingEntity primary = nearestHostile(player, 16.0D);
        if (primary != null) {
            for (LivingEntity enemy : player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    primary.getBoundingBox().inflate(2.0D),
                    candidate -> CompanionAugmentService.isHostileTarget(player, candidate))) {
                enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
        for (LivingEntity enemy : hostiles(player, 6.0D)) {
            if (player.distanceToSqr(enemy) <= 16.0D) {
                enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }
            hurt(player, enemy, 10.0F);
            enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        }
        particles(player, ParticleTypes.SCULK_SOUL, 70, 6.0D);
    }

    /** Time Tyrant: time stop, teleport, acceleration, AoE pressure and one-shot time reversal heal. */
    private static void releaseTimeTyrant(ServerPlayer player) {
        LivingEntity target = nearestHostile(player, 24.0D);
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4));
            blinkBehind(player, target, 3.0D);
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
        for (LivingEntity enemy : hostiles(player, 5.0D)) {
            hurt(player, enemy, scaledDamage(player, 1.0D, 8.0D));
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
        }
        player.heal(player.getMaxHealth() * 0.10F);
        particles(player, ParticleTypes.PORTAL, 100, 5.0D);
    }

    /** Sylvaron: root bind, spore pressure and a short nature-fortification pulse. */
    private static void releaseSylvaron(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        for (LivingEntity enemy : hostiles(player, 7.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            hurt(player, enemy, scaledDamage(player, 0.45D, 3.0D));
        }
        particles(player, ParticleTypes.WITCH, 72, 7.0D);
    }

    /** Aureon: thunder cage around nearby enemies and a concentrated storm strike. */
    private static void releaseAureon(ServerPlayer player) {
        LivingEntity primary = nearestHostile(player, 18.0D);
        if (primary != null) hurt(player, primary, scaledDamage(player, 1.25D, 10.0D));
        for (LivingEntity enemy : hostiles(player, 7.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 2));
            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            hurt(player, enemy, scaledDamage(player, 0.55D, 4.0D));
        }
        particles(player, ParticleTypes.ELECTRIC_SPARK, 96, 7.0D);
    }

    /** Nekhraz: sandstorm impairment followed by a burrow-style strike on the primary target. */
    private static void releaseNekhraz(ServerPlayer player) {
        for (LivingEntity enemy : hostiles(player, 8.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
        }
        LivingEntity target = nearestHostile(player, 18.0D);
        if (target != null && blinkBehind(player, target, 2.5D)) {
            hurt(player, target, scaledDamage(player, 1.15D, 9.0D));
            target.push(0.0D, 0.55D, 0.0D);
        }
        particles(player, ParticleTypes.WITCH, 76, 8.0D);
    }

    /** Varkhul: infernal chains and a flame-prison burst. */
    private static void releaseVarkhul(ServerPlayer player) {
        LivingEntity primary = nearestHostile(player, 16.0D);
        if (primary != null) {
            primary.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 4));
            primary.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
        for (LivingEntity enemy : hostiles(player, 6.0D)) {
            enemy.igniteForSeconds(6.0F);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
            hurt(player, enemy, scaledDamage(player, 0.7D, 6.0D));
        }
        particles(player, ParticleTypes.FLAME, 90, 6.0D);
    }

    /** Morvath: fear pulse plus soul drain converted into player healing. */
    private static void releaseMorvath(ServerPlayer player) {
        for (LivingEntity enemy : hostiles(player, 8.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            pushAway(player, enemy, 0.75D, 0.15D);
        }
        LivingEntity target = nearestHostile(player, 16.0D);
        if (target != null) {
            float damage = scaledDamage(player, 0.9D, 7.0D);
            hurt(player, target, damage);
            player.heal(Math.min(player.getMaxHealth() * 0.15F, damage * 0.5F));
        }
        particles(player, ParticleTypes.SOUL, 86, 8.0D);
    }

    /** Elyndra: void tether pulls enemies inward before a rift burst launches them. */
    private static void releaseElyndra(ServerPlayer player) {
        for (LivingEntity enemy : hostiles(player, 9.0D)) {
            pullToward(player, enemy, 0.8D);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
            hurt(player, enemy, scaledDamage(player, 0.5D, 4.0D));
            enemy.push(0.0D, 0.45D, 0.0D);
        }
        particles(player, ParticleTypes.PORTAL, 110, 9.0D);
    }

    /** Astrael: primordial chains, elemental shift and Cataclysm Nova. */
    private static void releaseAstrael(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0));
        for (LivingEntity enemy : hostiles(player, 9.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            hurt(player, enemy, scaledDamage(player, 1.0D, 9.0D));
            pushAway(player, enemy, 1.1D, 0.35D);
        }
        particles(player, ParticleTypes.ELECTRIC_SPARK, 120, 9.0D);
        particles(player, ParticleTypes.PORTAL, 80, 9.0D);
    }

    /** Shadow King: soul bolt, shadow step, raised shadow warriors and darkness nova. */
    private static void releaseShadowKing(ServerPlayer player) {
        LivingEntity target = nearestHostile(player, 20.0D);
        if (target != null) {
            hurt(player, target, scaledDamage(player, 1.0D, 8.0D));
            blinkBehind(player, target, 4.0D);
        }
        for (LivingEntity enemy : hostiles(player, 7.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            hurt(player, enemy, scaledDamage(player, 0.5D, 4.0D));
        }
        spawnAuthorityEchoes(player, 2, 18.0F, 4.0D, 12 * 20);
        particles(player, ParticleTypes.SOUL, 96, 7.0D);
    }

    /** Dragon Mother: fire breath cone, tail sweep, aerial fireball pressure and brood echoes. */
    private static void releaseDragonMother(ServerPlayer player) {
        Vec3 look = flatLook(player);
        for (LivingEntity enemy : hostiles(player, 10.0D)) {
            Vec3 offset = enemy.position().subtract(player.position());
            double forward = offset.dot(look);
            if (forward >= 0.0D && forward <= 10.0D) {
                double lateral = Math.abs(offset.x * -look.z + offset.z * look.x);
                if (lateral <= Math.max(1.5D, forward * 0.45D)) {
                    enemy.igniteForSeconds(5.0F);
                    hurt(player, enemy, scaledDamage(player, 0.8D, 6.0D));
                }
            }
            if (player.distanceToSqr(enemy) <= 16.0D) {
                pushAway(player, enemy, 1.25D, 0.35D);
            }
        }
        LivingEntity target = nearestHostile(player, 20.0D);
        if (target != null) {
            target.igniteForSeconds(7.0F);
            hurt(player, target, scaledDamage(player, 1.0D, 8.0D));
        }
        spawnAuthorityEchoes(player, 2, 14.0F, 3.5D, 10 * 20);
        particles(player, ParticleTypes.FLAME, 110, 8.0D);
    }

    /** Storm Griffin: wing gust plus lightning pressure on marked nearby enemies. */
    private static void releaseStormGriffin(ServerPlayer player) {
        for (LivingEntity enemy : hostiles(player, 9.0D)) {
            pushAway(player, enemy, 1.65D, 0.55D);
            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            hurt(player, enemy, scaledDamage(player, 0.75D, 6.0D));
        }
        LivingEntity target = nearestHostile(player, 18.0D);
        if (target != null) hurt(player, target, scaledDamage(player, 1.2D, 10.0D));
        particles(player, ParticleTypes.ELECTRIC_SPARK, 120, 9.0D);
    }

    /** Shadow Weaver: binding threads, shadow clones, puppet powers and a short protective veil. */
    private static void releaseShadowWeaver(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 2));
        for (LivingEntity enemy : hostiles(player, 8.0D)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 4));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            hurt(player, enemy, scaledDamage(player, 0.55D, 4.0D));
        }
        spawnAuthorityEchoes(player, 3, 16.0F, 4.0D, 12 * 20);
        LivingEntity target = nearestHostile(player, 18.0D);
        if (target != null) {
            target.igniteForSeconds(4.0F);
            hurt(player, target, scaledDamage(player, 1.1D, 9.0D));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
        particles(player, ParticleTypes.PORTAL, 120, 8.0D);
        particles(player, ParticleTypes.ELECTRIC_SPARK, 70, 8.0D);
    }

    /** MCEndgame Arachne: web shot, three-pull hook bind and the 4x3 frontal melee sweep. */
    private static void releaseArachne(ServerPlayer player) {
        LivingEntity primary = nearestHostile(player, 15.0D);
        if (primary != null) {
            hurt(player, primary, scaledDamage(player, 0.75D, 3.0D));
            primary.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 4));
            pullToward(player, primary, 1.0D);
        }
        Vec3 look = flatLook(player);
        for (LivingEntity enemy : hostiles(player, 4.5D)) {
            Vec3 offset = enemy.position().subtract(player.position());
            double forward = offset.dot(look);
            double lateral = Math.abs(offset.x * -look.z + offset.z * look.x);
            if (forward < 0.0D || forward > 4.0D || lateral > 1.5D) continue;
            hurt(player, enemy, scaledDamage(player, 1.1D, 4.0D));
            enemy.push(look.x * 1.5D, 0.2D, look.z * 1.5D);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 0.7F, 0.8F);
        particles(player, ParticleTypes.WHITE_SMOKE, 70, 5.0D);
    }

    /** MCEndgame Bonecrusher: teleport press, unblockable slam and full-body spin pressure. */
    private static void releaseBonecrusher(ServerPlayer player) {
        LivingEntity primary = nearestHostile(player, 50.0D);
        if (primary != null && player.distanceToSqr(primary) >= 36.0D) {
            blinkBehind(player, primary, 2.0D);
        }
        for (LivingEntity enemy : hostiles(player, 6.0D)) {
            double multiplier = player.distanceToSqr(enemy) <= 16.0D ? 2.5D : 1.5D;
            hurt(player, enemy, scaledDamage(player, multiplier, 8.0D));
            pushAway(player, enemy, 1.0D, 0.4D);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F, 0.9F);
        particles(player, ParticleTypes.CRIT, 120, 6.0D);
    }

    /** MCEndgame Elf Duelist: rapid blade chain, leap/backflip motion and source 50%/100-degree projectile reversal. */
    private static void releaseElfDuelist(ServerPlayer player) {
        deflectDuelistProjectiles(player);
        LivingEntity primary = nearestHostile(player, 10.0D);
        if (primary != null) {
            hurt(player, primary, scaledDamage(player, 1.5D, 6.0D));
        }
        for (LivingEntity enemy : hostiles(player, 3.5D)) {
            hurt(player, enemy, scaledDamage(player, 0.5D, 2.0D));
        }
        Vec3 back = flatLook(player).scale(-0.8D);
        player.push(back.x, 0.35D, back.z);
        particles(player, ParticleTypes.SWEEP_ATTACK, 72, 3.5D);
    }

    /** MCEndgame Beakburn: pounce, wind burst, 70-degree flame breath and 15-block fire-geyser field. */
    private static void releaseBeakburn(ServerPlayer player) {
        LivingEntity primary = nearestHostile(player, 12.0D);
        if (primary != null) hurt(player, primary, scaledDamage(player, 0.55D, 3.0D));
        for (LivingEntity enemy : hostiles(player, 12.0D)) {
            pushAway(player, enemy, 1.0D, 0.35D);
        }
        for (LivingEntity enemy : hostilesInCone(player, 8.0D, 70.0D)) {
            hurt(player, enemy, scaledDamage(player, 0.5D, 3.0D));
            enemy.igniteForSeconds(3.0F);
        }
        for (LivingEntity enemy : hostiles(player, 15.0D)) {
            if (player.getRandom().nextDouble() > 0.07D) continue;
            hurt(player, enemy, scaledDamage(player, 0.75D, 5.0D));
            enemy.igniteForSeconds(4.0F);
        }
        particles(player, ParticleTypes.FLAME, 130, 10.0D);
    }

    /** MCEndgame Beastweaver: beast swipe, elephant stomp, vine control, wolf pack and rhino-charge pressure. */
    private static void releaseBeastweaver(ServerPlayer player) {
        for (LivingEntity enemy : hostiles(player, 3.25D)) {
            hurt(player, enemy, scaledDamage(player, 0.75D, 4.0D));
        }
        for (LivingEntity enemy : hostiles(player, 9.0D)) {
            hurt(player, enemy, scaledDamage(player, 2.0D, 10.0D));
            pushAway(player, enemy, 2.5D, 0.55D);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 95, 3));
        }
        LivingEntity chargeTarget = nearestHostile(player, 50.0D);
        if (chargeTarget != null && player.distanceToSqr(chargeTarget) >= 100.0D) {
            hurt(player, chargeTarget, scaledDamage(player, 1.25D, 7.0D));
            pushAway(player, chargeTarget, 3.0D, 0.3D);
        }
        spawnAuthorityEchoes(player, 3, 20.0F, 6.0D, 15 * 20);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0));
        player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.9F);
        particles(player, ParticleTypes.SPORE_BLOSSOM_AIR, 130, 9.0D);
    }

    private static int deflectDuelistProjectiles(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        Vec3 facing = flatLook(player);
        double minimumDot = Math.cos(Math.toRadians(100.0D));
        int reflected = 0;
        for (Projectile projectile : level.getEntitiesOfClass(
                Projectile.class,
                player.getBoundingBox().inflate(10.0D),
                candidate -> candidate.isAlive() && candidate.getOwner() != player)) {
            Vec3 offset = projectile.position().subtract(player.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() < 1.0E-6D) continue;
            if (facing.dot(horizontal.normalize()) < minimumDot) continue;
            if (player.getRandom().nextDouble() > 0.5D) continue;
            projectile.setOwner(player);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.0D));
            reflected++;
        }
        return reflected;
    }

    private static List<LivingEntity> hostilesInCone(ServerPlayer player, double radius, double angleDegrees) {
        Vec3 look = flatLook(player);
        double minimumDot = Math.cos(Math.toRadians(angleDegrees * 0.5D));
        return hostiles(player, radius).stream().filter(enemy -> {
            Vec3 offset = enemy.position().subtract(player.position());
            Vec3 flat = new Vec3(offset.x, 0.0D, offset.z);
            return flat.lengthSqr() > 1.0E-6D && look.dot(flat.normalize()) >= minimumDot;
        }).toList();
    }

    private static void spawnAuthorityEchoes(ServerPlayer owner, int count, float health, double damage, int durationTicks) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        Vec3 look = flatLook(owner);
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        for (int i = 0; i < count; i++) {
            CombatCloneEntity clone = new CombatCloneEntity(CloneEntityTypes.COMBAT_CLONE, level);
            clone.configure(owner, health, damage, durationTicks);
            double sideOffset = (i - (count - 1) * 0.5D) * 2.4D;
            Vec3 pos = owner.position().add(side.scale(sideOffset)).subtract(look.scale(1.2D));
            clone.snapTo(pos.x, pos.y, pos.z);
            if (level.noCollision(clone, clone.getBoundingBox())) level.addFreshEntity(clone);
        }
    }

    private static boolean blinkBehind(ServerPlayer player, LivingEntity target, double distance) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        Vec3 targetLook = target.getLookAngle();
        Vec3 flatTargetLook = new Vec3(targetLook.x, 0.0D, targetLook.z);
        if (flatTargetLook.lengthSqr() < 1.0E-6D) flatTargetLook = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 desired = target.position().subtract(flatTargetLook.normalize().scale(distance));
        BlockPos base = BlockPos.containing(desired);
        for (int offset = 0; offset <= 3; offset++) {
            for (int yOffset : offset == 0 ? new int[]{0} : new int[]{offset, -offset}) {
                BlockPos pos = base.offset(0, yOffset, 0);
                AABB moved = player.getBoundingBox().move(
                        pos.getX() + 0.5D - player.getX(),
                        pos.getY() - player.getY(),
                        pos.getZ() + 0.5D - player.getZ()
                );
                if (level.noCollision(player, moved)) {
                    return player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                            Set.of(), target.getYRot(), player.getXRot(), true);
                }
            }
        }
        return false;
    }

    private static List<LivingEntity> hostiles(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= radius * radius
        );
    }

    private static LivingEntity nearestHostile(ServerPlayer player, double radius) {
        LivingEntity best = null;
        double bestDistance = radius * radius;
        for (LivingEntity candidate : hostiles(player, radius)) {
            double distance = player.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static void hurt(ServerPlayer player, LivingEntity target, float damage) {
        if (!(player.level() instanceof ServerLevel level) || damage <= 0.0F) return;
        CombatProvenanceContext.runAs(
                CombatProvenance.AUTHORITY,
                () -> target.hurtServer(level, player.damageSources().playerAttack(player), damage)
        );
    }

    private static float scaledDamage(ServerPlayer player, double multiplier, double floor) {
        return (float) Math.max(floor, CloneCombatSnapshot.meleeDamage(player) * multiplier);
    }

    private static void pushAway(ServerPlayer player, LivingEntity enemy, double strength, double vertical) {
        Vec3 away = enemy.position().subtract(player.position());
        Vec3 flat = new Vec3(away.x, 0.0D, away.z);
        if (flat.lengthSqr() < 1.0E-6D) flat = new Vec3(0.0D, 0.0D, 1.0D);
        flat = flat.normalize();
        enemy.push(flat.x * strength, vertical, flat.z * strength);
    }

    private static void pullToward(ServerPlayer player, LivingEntity enemy, double strength) {
        Vec3 toward = player.position().subtract(enemy.position());
        Vec3 flat = new Vec3(toward.x, 0.0D, toward.z);
        if (flat.lengthSqr() < 1.0E-6D) return;
        flat = flat.normalize();
        enemy.push(flat.x * strength, 0.12D, flat.z * strength);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : flat.normalize();
    }

    private static void particles(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle, int count, double radius) {
        if (!(player.level() instanceof ServerLevel level)) return;
        level.sendParticles(particle, player.getX(), player.getY() + 0.8D, player.getZ(), count,
                radius * 0.35D, 0.8D, radius * 0.35D, 0.08D);
    }

    private static void lineParticles(ServerPlayer player, Vec3 direction, net.minecraft.core.particles.ParticleOptions particle, int blocks) {
        if (!(player.level() instanceof ServerLevel level)) return;
        for (int i = 1; i <= blocks * 2; i++) {
            double distance = i * 0.5D;
            level.sendParticles(particle,
                    player.getX() + direction.x * distance,
                    player.getY() + 0.2D,
                    player.getZ() + direction.z * distance,
                    2, 0.25D, 0.08D, 0.25D, 0.01D);
        }
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }
}
