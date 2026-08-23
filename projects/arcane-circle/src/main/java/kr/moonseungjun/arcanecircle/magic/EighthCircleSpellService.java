package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Eighth-circle regional/reality authority with player and NPC role parity. */
public final class EighthCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "antimagic_field", "clone", "control_weather", "demiplane", "dominate_monster",
            "earthquake", "feeblemind", "incendiary_cloud", "maze", "sunburst");

    public static final int NPC_ANTIMAGIC_TICKS = ArcaneFieldService.ANTIMAGIC_TICKS;
    public static final int NPC_WEATHER_TICKS = 900;
    public static final int NPC_DEMIPLANE_TICKS = 160;
    public static final int NPC_CLONE_TICKS = 1800;
    public static final int NPC_DOMINATE_TICKS = 1200;
    public static final int EARTHQUAKE_TICKS = 180;
    public static final int NPC_FEEBLEMIND_TICKS = 1800;
    public static final int INCENDIARY_CLOUD_TICKS = 240;
    public static final int NPC_MAZE_TICKS = 480;

    private static final Map<UUID, NpcAntimagicState> NPC_ANTIMAGIC = new HashMap<>();
    private static final Map<UUID, NpcWeatherState> NPC_WEATHER = new HashMap<>();
    private static final Map<UUID, NpcPocketState> NPC_DEMIPLANE = new HashMap<>();
    private static final Map<UUID, NpcDominateState> NPC_DOMINATE = new HashMap<>();
    private static final Map<UUID, NpcFeeblemindState> NPC_FEEBLEMIND = new HashMap<>();
    private static final Map<UUID, NpcMazeState> NPC_MAZE = new HashMap<>();
    private static final Map<UUID, NpcCloneState> NPC_CLONES = new HashMap<>();
    private static final List<EarthquakeField> EARTHQUAKES = new ArrayList<>();
    private static final List<IncendiaryCloudField> INCENDIARY_CLOUDS = new ArrayList<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private EighthCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        ServerLevel level = (ServerLevel) caster.level();
        return switch (spellId) {
            case "antimagic_field" -> ArcaneFieldService.executeSpecial(caster, spellId, range, power, snapshot);
            case "clone" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);
            case "control_weather" -> SpellGameplayService.execute(caster, spellId, range, power, snapshot);
            case "demiplane" -> PlanarSpellService.execute(caster, spellId);
            case "dominate_monster", "feeblemind" ->
                    HighControlSpellService.execute(caster, spellId, range, power, snapshot);
            case "earthquake" -> earthquake(level, caster, snapshot, range, power, true);
            case "incendiary_cloud" -> incendiaryCloud(level, caster, snapshot, range, power);
            case "maze" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);
            case "sunburst" -> sunburst(level, caster, snapshot, range, power);
            default -> false;
        };
    }

    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity target,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        return switch (spell.id()) {
            case "antimagic_field" -> npcAntimagic(level, caster, range);
            case "clone" -> npcClone(level, caster, target);
            case "control_weather" -> npcControlWeather(level, caster, range, power);
            case "demiplane" -> npcDemiplane(level, caster);
            case "dominate_monster" -> npcDominate(level, caster, target, snapshot);
            case "earthquake" -> earthquake(level, caster, snapshot, range, power, false);
            case "feeblemind" -> npcFeeblemind(level, caster, target, snapshot);
            case "incendiary_cloud" -> incendiaryCloud(level, caster, snapshot, range, power);
            case "maze" -> npcMaze(level, caster, target, snapshot);
            case "sunburst" -> sunburst(level, caster, snapshot, range, power);
            default -> false;
        };
    }

    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        long now = caster.level() instanceof ServerLevel level ? level.getGameTime() : Long.MAX_VALUE;
        NpcFeeblemindState feeble = NPC_FEEBLEMIND.get(caster.getUUID());
        if (feeble != null && feeble.level == caster.level() && now < feeble.expiresAt) return true;
        NpcMazeState maze = NPC_MAZE.get(caster.getUUID());
        if (maze != null && maze.level == caster.level() && now < maze.expiresAt) return true;
        for (NpcAntimagicState field : NPC_ANTIMAGIC.values()) {
            if (field.level != caster.level() || now >= field.expiresAt) continue;
            Entity rawOwner = field.level.getEntity(field.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) continue;
            if (owner.position().distanceToSqr(caster.position()) <= field.radius * field.radius) return true;
        }
        return false;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickNpcAntimagic(level, now);
        tickNpcWeather(level, now);
        tickNpcDemiplane(level, now);
        tickNpcDominate(level, now);
        tickNpcFeeblemind(level, now);
        tickNpcMaze(level, now);
        tickNpcClones(level, now);
        tickEarthquakes(level, now);
        tickIncendiaryClouds(level, now);
    }

    public static void clear(LivingEntity subject) {
        if (subject == null) return;
        UUID id = subject.getUUID();
        NpcAntimagicState anti = NPC_ANTIMAGIC.remove(id);
        if (anti != null) cancelOwnerVisual(anti.level, anti.ownerId, "antimagic_field");
        NpcWeatherState weather = NPC_WEATHER.remove(id);
        if (weather != null) cancelOwnerVisual(weather.level, weather.ownerId, "control_weather");
        NpcPocketState pocket = NPC_DEMIPLANE.remove(id);
        if (pocket != null) restoreNpcDemiplane(pocket);
        removeDominateRelated(id);
        removeFeeblemindRelated(id);
        removeMazeRelated(id);
        removeCloneRelated(id);
        clearEarthquakeRelated(id);
        clearIncendiaryRelated(id);
    }

    public static void clearAll() {
        for (NpcPocketState state : new ArrayList<>(NPC_DEMIPLANE.values())) restoreNpcDemiplane(state);
        for (NpcDominateState state : new ArrayList<>(NPC_DOMINATE.values())) restoreNpcDominate(state);
        for (NpcMazeState state : new ArrayList<>(NPC_MAZE.values())) restoreNpcMaze(state);
        for (NpcCloneState state : new ArrayList<>(NPC_CLONES.values())) discardNpcClone(state);
        NPC_ANTIMAGIC.clear();
        NPC_WEATHER.clear();
        NPC_DEMIPLANE.clear();
        NPC_DOMINATE.clear();
        NPC_FEEBLEMIND.clear();
        NPC_MAZE.clear();
        NPC_CLONES.clear();
        EARTHQUAKES.clear();
        INCENDIARY_CLOUDS.clear();
        LAST_TICK.clear();
    }

    private static boolean earthquake(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                      double range, double power, boolean destructiveTerrain) {
        Vec3 center = snapshot.target();
        double radius = Math.max(14.0, Math.min(22.0, range * .40));
        EARTHQUAKES.removeIf(field -> field.ownerId.equals(caster.getUUID()));
        EARTHQUAKES.add(new EarthquakeField(level, caster.getUUID(), center, radius, power,
                level.getGameTime() + EARTHQUAKE_TICKS, level.getGameTime()));
        pulseEarthquake(level, caster, center, radius, power * .34);
        if (destructiveTerrain && caster instanceof ServerPlayer player)
            DestructiveMagicService.impact(player, "earthquake", center, radius, power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.25F, .48F);
        return true;
    }

    private static void tickEarthquakes(ServerLevel level, long now) {
        Iterator<EarthquakeField> iterator = EARTHQUAKES.iterator();
        while (iterator.hasNext()) {
            EarthquakeField field = iterator.next();
            if (field.level != level) continue;
            Entity rawOwner = level.getEntity(field.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || now >= field.expiresAt) {
                cancelOwnerVisual(level, field.ownerId, "earthquake");
                iterator.remove();
                continue;
            }
            if (now < field.nextPulse) continue;
            field.nextPulse = now + 10L;
            pulseEarthquake(level, owner, field.center, field.radius, field.power * .18);
        }
    }

    private static void pulseEarthquake(ServerLevel level, LivingEntity caster, Vec3 center,
                                        double radius, double pulsePower) {
        for (LivingEntity target : enemies(level, caster, center, radius, Math.max(8.0, radius * .45))) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            double falloff = Math.max(.46, 1.0 - distance / Math.max(1.0, radius) * .54);
            ArcaneDamage.hurt(level, caster, target, (float) (pulsePower * falloff));
            Vec3 away = horizontal(target.position().subtract(center));
            double kick = .22 + falloff * .30;
            target.push(away.x * kick, .32 + falloff * .42, away.z * kick);
        }
    }

    private static boolean incendiaryCloud(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                           double range, double power) {
        Vec3 center = snapshot.target();
        Vec3 drift = horizontal(snapshot.launchDirection()).scale(.16);
        double radius = Math.max(10.0, Math.min(16.0, range * .30));
        INCENDIARY_CLOUDS.removeIf(field -> field.ownerId.equals(caster.getUUID()));
        INCENDIARY_CLOUDS.add(new IncendiaryCloudField(level, caster.getUUID(), center, drift, radius, power,
                level.getGameTime() + INCENDIARY_CLOUD_TICKS, level.getGameTime()));
        pulseIncendiary(level, caster, center, radius, power * .25);
        return true;
    }

    private static void tickIncendiaryClouds(ServerLevel level, long now) {
        Iterator<IncendiaryCloudField> iterator = INCENDIARY_CLOUDS.iterator();
        while (iterator.hasNext()) {
            IncendiaryCloudField field = iterator.next();
            if (field.level != level) continue;
            Entity rawOwner = level.getEntity(field.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || now >= field.expiresAt) {
                cancelOwnerVisual(level, field.ownerId, "incendiary_cloud");
                iterator.remove();
                continue;
            }
            field.center = field.center.add(field.drift);
            if (now < field.nextPulse) continue;
            field.nextPulse = now + 8L;
            pulseIncendiary(level, owner, field.center, field.radius, field.power * .15);
        }
    }

    private static void pulseIncendiary(ServerLevel level, LivingEntity caster, Vec3 center,
                                        double radius, double pulsePower) {
        for (LivingEntity target : enemies(level, caster, center, radius, Math.max(7.0, radius * .65))) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            double falloff = Math.max(.52, 1.0 - distance / Math.max(1.0, radius) * .48);
            ArcaneDamage.hurt(level, caster, target, (float) (pulsePower * falloff));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 120));
        }
    }

    private static boolean sunburst(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                    double range, double power) {
        Vec3 center = snapshot.target();
        double radius = Math.max(15.0, Math.min(23.0, range * .39));
        boolean hit = false;
        for (LivingEntity target : enemies(level, caster, center, radius, Math.max(10.0, radius * .62))) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            double falloff = Math.max(.58, 1.0 - distance / Math.max(1.0, radius) * .42);
            ArcaneDamage.hurt(level, caster, target, (float) (power * 1.18 * falloff));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 140));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 260, 2, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, true, false));
            hit = true;
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_POWER_SELECT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.25F, 1.55F);
        return hit || snapshot.validFor(caster);
    }

    private static boolean npcAntimagic(ServerLevel level, Mob caster, double range) {
        double radius = Math.max(12.0, Math.min(22.0, range * .72));
        NPC_ANTIMAGIC.put(caster.getUUID(), new NpcAntimagicState(level, caster.getUUID(), radius,
                level.getGameTime() + NPC_ANTIMAGIC_TICKS));
        applyNpcAntimagic(level, caster, radius);
        return true;
    }

    private static void tickNpcAntimagic(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcAntimagicState>> iterator = NPC_ANTIMAGIC.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcAntimagicState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.ownerId);
            if (!(raw instanceof Mob owner) || !owner.isAlive() || owner.isRemoved() || now >= state.expiresAt) {
                cancelOwnerVisual(level, state.ownerId, "antimagic_field");
                iterator.remove();
                continue;
            }
            applyNpcAntimagic(level, owner, state.radius);
        }
    }

    private static void applyNpcAntimagic(ServerLevel level, Mob owner, double radius) {
        AABB box = owner.getBoundingBox().inflate(radius, radius * .75, radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value.isAlive() && !value.isRemoved() && value != owner
                        && owner.position().distanceToSqr(value.position()) <= radius * radius)) {
            FirstCircleSpellService.dispel(entity);
            SecondCircleSpellService.clear(entity);
            ThirdCircleSpellService.clear(entity);
            FourthCircleSpellService.clear(entity);
            FifthCircleSpellService.clear(entity);
            SixthCircleSpellService.clear(entity);
            SeventhCircleSpellService.clear(entity);
            SpellGameplayService.clear(entity);
            HighWardSpellService.clear(entity);
            HighControlSpellService.clear(entity);
            clearNonFieldStates(entity.getUUID());
            if (entity instanceof ServerPlayer player) {
                HighUtilitySpellService.clear(player);
                SimulacrumService.clear(player);
                if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
                SpellKineticsService.cancel(player);
            } else if (entity instanceof Mob mob) {
                WorldMagicService.stop(mob);
            }
        }
    }

    private static boolean npcClone(ServerLevel level, Mob caster, LivingEntity target) {
        NpcCloneState old = NPC_CLONES.remove(caster.getUUID());
        if (old != null) discardNpcClone(old);
        Entity raw = caster.getType().create(level, EntitySpawnReason.EVENT);
        if (!(raw instanceof Mob copy)) return false;
        Vec3 side = new Vec3(-caster.getLookAngle().z, 0.0, caster.getLookAngle().x);
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(1.0, 0.0, 0.0);
        else side = side.normalize();
        Vec3 at = caster.position().add(side.scale(2.4));
        copy.snapTo(at.x, at.y, at.z, caster.getYRot(), caster.getXRot());
        copy.finalizeSpawn(level, level.getCurrentDifficultyAt(caster.blockPosition()), EntitySpawnReason.EVENT, null);
        copyAttribute(caster, copy, Attributes.MAX_HEALTH);
        copyAttribute(caster, copy, Attributes.ATTACK_DAMAGE);
        copyAttribute(caster, copy, Attributes.ARMOR);
        copyAttribute(caster, copy, Attributes.ARMOR_TOUGHNESS);
        copyAttribute(caster, copy, Attributes.MOVEMENT_SPEED);
        copy.setHealth(copy.getMaxHealth());
        copy.setCustomName(Component.literal("§d[NPC 클론] §f" + caster.getName().getString()));
        copy.setCustomNameVisible(true);
        copy.setPersistenceRequired();
        copy.addTag("arcanecircle_npc_clone");
        level.addFreshEntityWithPassengers(copy);
        if (target != null && target.isAlive() && !caster.isAlliedTo(target)) copy.setTarget(target);
        NPC_CLONES.put(caster.getUUID(), new NpcCloneState(level, caster.getUUID(), copy.getUUID(),
                level.getGameTime() + NPC_CLONE_TICKS));
        return true;
    }

    private static void tickNpcClones(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcCloneState>> iterator = NPC_CLONES.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcCloneState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawCopy = level.getEntity(state.copyId);
            if (!(rawOwner instanceof Mob owner) || !owner.isAlive() || owner.isRemoved()
                    || !(rawCopy instanceof Mob copy) || !copy.isAlive() || copy.isRemoved()
                    || now >= state.expiresAt) {
                discardNpcClone(state);
                iterator.remove();
                continue;
            }
            LivingEntity target = owner.getTarget();
            if (target != null && target.isAlive() && !owner.isAlliedTo(target) && target != copy) copy.setTarget(target);
            if (copy.distanceToSqr(owner) > 100.0) copy.getNavigation().moveTo(owner, 1.15);
        }
    }

    private static boolean npcControlWeather(ServerLevel level, Mob caster, double range, double power) {
        setWeather(level, true, NPC_WEATHER_TICKS);
        NPC_WEATHER.put(caster.getUUID(), new NpcWeatherState(level, caster.getUUID(),
                Math.max(32.0, range * .82), power, level.getGameTime() + NPC_WEATHER_TICKS, level.getGameTime()));
        return true;
    }

    private static void tickNpcWeather(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcWeatherState>> iterator = NPC_WEATHER.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcWeatherState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.ownerId);
            if (!(raw instanceof Mob owner) || !owner.isAlive() || owner.isRemoved() || now >= state.expiresAt) {
                cancelOwnerVisual(level, state.ownerId, "control_weather");
                iterator.remove();
                continue;
            }
            if (now < state.nextPulse) continue;
            state.nextPulse = now + 20L;
            List<LivingEntity> targets = enemies(level, owner, owner.position(), state.radius, state.radius * .76);
            int count = Math.min(3, targets.size());
            targets.sort(Comparator.comparingDouble(owner::distanceToSqr));
            for (int i = 0; i < count; i++) {
                LivingEntity target = targets.get(i);
                summonLightning(level, target.position());
                ArcaneDamage.hurt(level, owner, target, (float) (state.power * .22));
            }
        }
    }

    private static boolean npcDemiplane(ServerLevel level, Mob caster) {
        NpcPocketState old = NPC_DEMIPLANE.remove(caster.getUUID());
        if (old != null) restoreNpcDemiplane(old);
        NpcPocketState state = new NpcPocketState(level, caster.getUUID(), level.getGameTime() + NPC_DEMIPLANE_TICKS,
                caster.isInvisible(), caster.isInvulnerable(), caster.isNoGravity(), caster.isSilent(), caster.noPhysics,
                caster.getTarget() == null ? null : caster.getTarget().getUUID(), caster.position());
        NPC_DEMIPLANE.put(caster.getUUID(), state);
        caster.setTarget(null);
        caster.getNavigation().stop();
        caster.setInvisible(true);
        caster.setInvulnerable(true);
        caster.setNoGravity(true);
        caster.setSilent(true);
        caster.noPhysics = true;
        caster.setDeltaMovement(Vec3.ZERO);
        return true;
    }

    private static void tickNpcDemiplane(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcPocketState>> iterator = NPC_DEMIPLANE.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcPocketState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.ownerId);
            if (!(raw instanceof Mob caster) || !caster.isAlive() || caster.isRemoved() || now >= state.expiresAt) {
                restoreNpcDemiplane(state);
                iterator.remove();
                continue;
            }
            caster.setTarget(null);
            caster.getNavigation().stop();
            caster.setInvisible(true);
            caster.setInvulnerable(true);
            caster.setNoGravity(true);
            caster.setSilent(true);
            caster.noPhysics = true;
            caster.setDeltaMovement(Vec3.ZERO);
            caster.snapTo(state.anchor.x, state.anchor.y, state.anchor.z, caster.getYRot(), caster.getXRot());
        }
    }

    private static boolean npcDominate(ServerLevel level, Mob caster, LivingEntity fallback,
                                       CastTargetSnapshot snapshot) {
        LivingEntity target = targetEntity(level, fallback, snapshot);
        if (target == null || target == caster || caster.isAlliedTo(target)) return false;
        UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
        NPC_DOMINATE.put(target.getUUID(), new NpcDominateState(level, caster.getUUID(), target.getUUID(),
                level.getGameTime() + NPC_DOMINATE_TICKS, oldTarget));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        WorldMagicService.stop(target);
        return true;
    }

    private static void tickNpcDominate(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcDominateState>> iterator = NPC_DOMINATE.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcDominateState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof Mob owner) || !owner.isAlive() || owner.isRemoved()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restoreNpcDominate(state);
                iterator.remove();
                continue;
            }
            if (target instanceof Mob mob) {
                LivingEntity current = mob.getTarget();
                if (current == owner || (current != null && owner.isAlliedTo(current))) mob.setTarget(null);
                Mob threat = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(28.0),
                                candidate -> candidate != mob && candidate != owner && candidate.isAlive() && !candidate.isRemoved()
                                        && !owner.isAlliedTo(candidate))
                        .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                if (threat != null) mob.setTarget(threat);
                else if (mob.distanceToSqr(owner) > 20.0) mob.getNavigation().moveTo(owner, 1.12);
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 10, 0, true, false));
            } else if (target instanceof ServerPlayer player) {
                suppressPlayerCasting(player);
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 6, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 2, true, false));
            }
        }
    }

    private static boolean npcFeeblemind(ServerLevel level, Mob caster, LivingEntity fallback,
                                         CastTargetSnapshot snapshot) {
        LivingEntity target = targetEntity(level, fallback, snapshot);
        if (target == null || target == caster || caster.isAlliedTo(target)) return false;
        NPC_FEEBLEMIND.put(target.getUUID(), new NpcFeeblemindState(level, caster.getUUID(), target.getUUID(),
                level.getGameTime() + NPC_FEEBLEMIND_TICKS));
        ArcaneDamage.hurt(level, caster, target, 18.0F);
        applyNpcFeeblemind(target);
        return true;
    }

    private static void tickNpcFeeblemind(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcFeeblemindState>> iterator = NPC_FEEBLEMIND.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcFeeblemindState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                cancelOwnerVisual(level, state.ownerId, "feeblemind");
                iterator.remove();
                continue;
            }
            applyNpcFeeblemind(target);
        }
    }

    private static void applyNpcFeeblemind(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 12, 7, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 12, 6, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 12, 2, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 12, 1, true, false));
        if (target instanceof ServerPlayer player) suppressPlayerCasting(player);
        else if (target instanceof Mob mob) WorldMagicService.stop(mob);
    }

    private static boolean npcMaze(ServerLevel level, Mob caster, LivingEntity fallback,
                                   CastTargetSnapshot snapshot) {
        LivingEntity target = targetEntity(level, fallback, snapshot);
        if (target == null || target == caster || caster.isAlliedTo(target)) return false;
        NpcMazeState old = NPC_MAZE.remove(target.getUUID());
        if (old != null) restoreNpcMaze(old);
        UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
        NpcMazeState state = new NpcMazeState(level, caster.getUUID(), target.getUUID(),
                level.getGameTime() + NPC_MAZE_TICKS, target.position(), target.getYRot(), target.getXRot(),
                target.isInvisible(), target.isInvulnerable(), target.isNoGravity(), target.isSilent(),
                target.noPhysics, oldTarget);
        NPC_MAZE.put(target.getUUID(), state);
        applyNpcMaze(target, state);
        return true;
    }

    private static void tickNpcMaze(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcMazeState>> iterator = NPC_MAZE.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcMazeState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restoreNpcMaze(state);
                iterator.remove();
                continue;
            }
            applyNpcMaze(target, state);
        }
    }

    private static void applyNpcMaze(LivingEntity target, NpcMazeState state) {
        target.setInvisible(true);
        target.setInvulnerable(true);
        target.setNoGravity(true);
        target.setSilent(true);
        target.noPhysics = true;
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0.0F;
        target.snapTo(state.anchor.x, state.anchor.y, state.anchor.z, state.yaw, state.pitch);
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 12, 2, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 12, 2, true, false));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        } else if (target instanceof ServerPlayer player) {
            suppressPlayerCasting(player);
        }
    }

    private static void restoreNpcDemiplane(NpcPocketState state) {
        Entity raw = state.level.getEntity(state.ownerId);
        if (!(raw instanceof Mob caster) || caster.isRemoved()) return;
        caster.setInvisible(state.oldInvisible);
        caster.setInvulnerable(state.oldInvulnerable);
        caster.setNoGravity(state.oldNoGravity);
        caster.setSilent(state.oldSilent);
        caster.noPhysics = state.oldNoPhysics;
        LivingEntity oldTarget = living(state.level, state.oldTargetId);
        caster.setTarget(oldTarget);
        caster.fallDistance = 0.0F;
        cancelOwnerVisual(state.level, state.ownerId, "demiplane");
    }

    private static void restoreNpcDominate(NpcDominateState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (raw instanceof Mob mob && mob.isAlive() && !mob.isRemoved()) {
            mob.getNavigation().stop();
            mob.setTarget(living(state.level, state.oldTargetId));
        }
        cancelOwnerVisual(state.level, state.ownerId, "dominate_monster");
    }

    private static void restoreNpcMaze(NpcMazeState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (raw instanceof LivingEntity target && !target.isRemoved()) {
            target.setInvisible(state.oldInvisible);
            target.setInvulnerable(state.oldInvulnerable);
            target.setNoGravity(state.oldNoGravity);
            target.setSilent(state.oldSilent);
            target.noPhysics = state.oldNoPhysics;
            target.fallDistance = 0.0F;
            target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 1, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, true, false));
            if (target instanceof Mob mob) mob.setTarget(living(state.level, state.oldTargetId));
        }
        cancelOwnerVisual(state.level, state.ownerId, "maze");
    }

    private static void suppressPlayerCasting(ServerPlayer player) {
        if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
        if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
        SpellKineticsService.cancel(player);
    }

    private static void clearNonFieldStates(UUID id) {
        removeDominateRelated(id);
        removeFeeblemindRelated(id);
        removeMazeRelated(id);
        removeCloneRelated(id);
        clearEarthquakeRelated(id);
        clearIncendiaryRelated(id);
    }

    private static void removeDominateRelated(UUID id) {
        Iterator<Map.Entry<UUID, NpcDominateState>> iterator = NPC_DOMINATE.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcDominateState state = iterator.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreNpcDominate(state);
            iterator.remove();
        }
    }

    private static void removeFeeblemindRelated(UUID id) {
        Iterator<Map.Entry<UUID, NpcFeeblemindState>> iterator = NPC_FEEBLEMIND.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcFeeblemindState state = iterator.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            cancelOwnerVisual(state.level, state.ownerId, "feeblemind");
            iterator.remove();
        }
    }

    private static void removeMazeRelated(UUID id) {
        Iterator<Map.Entry<UUID, NpcMazeState>> iterator = NPC_MAZE.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcMazeState state = iterator.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreNpcMaze(state);
            iterator.remove();
        }
    }

    private static void removeCloneRelated(UUID id) {
        Iterator<Map.Entry<UUID, NpcCloneState>> iterator = NPC_CLONES.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcCloneState state = iterator.next().getValue();
            if (!state.ownerId.equals(id) && !state.copyId.equals(id)) continue;
            discardNpcClone(state);
            iterator.remove();
        }
    }

    private static void clearEarthquakeRelated(UUID id) {
        EARTHQUAKES.removeIf(field -> field.ownerId.equals(id));
    }

    private static void clearIncendiaryRelated(UUID id) {
        INCENDIARY_CLOUDS.removeIf(field -> field.ownerId.equals(id));
    }

    private static void discardNpcClone(NpcCloneState state) {
        Entity raw = state.level.getEntity(state.copyId);
        if (raw != null && !raw.isRemoved()) raw.discard();
    }

    private static void copyAttribute(Mob source, Mob copy,
                                      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        AttributeInstance from = source.getAttribute(attribute);
        AttributeInstance to = copy.getAttribute(attribute);
        if (from != null && to != null) to.setBaseValue(from.getBaseValue());
    }

    private static LivingEntity targetEntity(ServerLevel level, LivingEntity fallback, CastTargetSnapshot snapshot) {
        if (snapshot.targetEntityId() != null) {
            Entity raw = level.getEntity(snapshot.targetEntityId());
            return raw instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
        }
        return fallback != null && fallback.isAlive() && !fallback.isRemoved() ? fallback : null;
    }

    private static LivingEntity living(ServerLevel level, UUID id) {
        if (id == null) return null;
        Entity raw = level.getEntity(id);
        return raw instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static List<LivingEntity> enemies(ServerLevel level, LivingEntity caster, Vec3 center,
                                              double radius, double vertical) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, vertical, radius),
                value -> value != caster && value.isAlive() && !value.isRemoved() && !caster.isAlliedTo(value)
                        && center.distanceToSqr(value.position()) <= radius * radius + vertical * vertical * .12);
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 horizontal = new Vec3(value.x, 0.0, value.z);
        if (horizontal.lengthSqr() < 1.0E-8) return new Vec3(1.0, 0.0, 0.0);
        return horizontal.normalize();
    }

    private static void summonLightning(ServerLevel level, Vec3 at) {
        CommandSourceStack source = commandSource(level);
        String command = String.format(java.util.Locale.ROOT, "/summon minecraft:lightning_bolt %.2f %.2f %.2f",
                at.x, at.y, at.z);
        level.getServer().getCommands().performPrefixedCommand(source, command);
    }

    private static void setWeather(ServerLevel level, boolean thunder, int durationTicks) {
        int seconds = Math.max(1, (durationTicks + 19) / 20);
        level.getServer().getCommands().performPrefixedCommand(commandSource(level),
                (thunder ? "/weather thunder " : "/weather clear ") + seconds);
    }

    private static CommandSourceStack commandSource(ServerLevel level) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, new Vec2(0.0F, 0.0F),
                level, LevelBasedPermissionSet.ADMIN, "ArcaneCircle", Component.literal("ArcaneCircle"),
                level.getServer(), null);
    }

    private static void cancelOwnerVisual(ServerLevel level, UUID ownerId, String spellId) {
        Entity raw = level.getEntity(ownerId);
        if (raw instanceof LivingEntity owner && !owner.isRemoved()) WorldMagicService.cancelRelease(owner, spellId);
    }

    private static final class EarthquakeField {
        private final ServerLevel level; private final UUID ownerId; private final Vec3 center;
        private final double radius, power; private final long expiresAt; private long nextPulse;
        private EarthquakeField(ServerLevel level, UUID ownerId, Vec3 center, double radius, double power,
                                long expiresAt, long nextPulse) {
            this.level=level; this.ownerId=ownerId; this.center=center; this.radius=radius;
            this.power=power; this.expiresAt=expiresAt; this.nextPulse=nextPulse;
        }
    }

    private static final class IncendiaryCloudField {
        private final ServerLevel level; private final UUID ownerId; private Vec3 center;
        private final Vec3 drift; private final double radius, power; private final long expiresAt; private long nextPulse;
        private IncendiaryCloudField(ServerLevel level, UUID ownerId, Vec3 center, Vec3 drift, double radius,
                                     double power, long expiresAt, long nextPulse) {
            this.level=level; this.ownerId=ownerId; this.center=center; this.drift=drift;
            this.radius=radius; this.power=power; this.expiresAt=expiresAt; this.nextPulse=nextPulse;
        }
    }

    private static final class NpcWeatherState {
        private final ServerLevel level; private final UUID ownerId; private final double radius, power;
        private final long expiresAt; private long nextPulse;
        private NpcWeatherState(ServerLevel level, UUID ownerId, double radius, double power, long expiresAt, long nextPulse) {
            this.level=level; this.ownerId=ownerId; this.radius=radius; this.power=power;
            this.expiresAt=expiresAt; this.nextPulse=nextPulse;
        }
    }

    private static final class NpcAntimagicState {
        private final ServerLevel level; private final UUID ownerId; private final double radius; private final long expiresAt;
        private NpcAntimagicState(ServerLevel level, UUID ownerId, double radius, long expiresAt) {
            this.level=level; this.ownerId=ownerId; this.radius=radius; this.expiresAt=expiresAt;
        }
    }

    private static final class NpcPocketState {
        private final ServerLevel level; private final UUID ownerId; private final long expiresAt;
        private final boolean oldInvisible, oldInvulnerable, oldNoGravity, oldSilent, oldNoPhysics;
        private final UUID oldTargetId; private final Vec3 anchor;
        private NpcPocketState(ServerLevel level, UUID ownerId, long expiresAt, boolean oldInvisible,
                               boolean oldInvulnerable, boolean oldNoGravity, boolean oldSilent,
                               boolean oldNoPhysics, UUID oldTargetId, Vec3 anchor) {
            this.level=level; this.ownerId=ownerId; this.expiresAt=expiresAt; this.oldInvisible=oldInvisible;
            this.oldInvulnerable=oldInvulnerable; this.oldNoGravity=oldNoGravity; this.oldSilent=oldSilent;
            this.oldNoPhysics=oldNoPhysics; this.oldTargetId=oldTargetId; this.anchor=anchor;
        }
    }

    private static final class NpcDominateState {
        private final ServerLevel level; private final UUID ownerId, targetId; private final long expiresAt; private final UUID oldTargetId;
        private NpcDominateState(ServerLevel level, UUID ownerId, UUID targetId, long expiresAt, UUID oldTargetId) {
            this.level=level; this.ownerId=ownerId; this.targetId=targetId; this.expiresAt=expiresAt; this.oldTargetId=oldTargetId;
        }
    }

    private static final class NpcFeeblemindState {
        private final ServerLevel level; private final UUID ownerId, targetId; private final long expiresAt;
        private NpcFeeblemindState(ServerLevel level, UUID ownerId, UUID targetId, long expiresAt) {
            this.level=level; this.ownerId=ownerId; this.targetId=targetId; this.expiresAt=expiresAt;
        }
    }

    private static final class NpcMazeState {
        private final ServerLevel level; private final UUID ownerId, targetId; private final long expiresAt; private final Vec3 anchor;
        private final float yaw, pitch; private final boolean oldInvisible, oldInvulnerable, oldNoGravity, oldSilent, oldNoPhysics;
        private final UUID oldTargetId;
        private NpcMazeState(ServerLevel level, UUID ownerId, UUID targetId, long expiresAt, Vec3 anchor,
                             float yaw, float pitch, boolean oldInvisible, boolean oldInvulnerable,
                             boolean oldNoGravity, boolean oldSilent, boolean oldNoPhysics, UUID oldTargetId) {
            this.level=level; this.ownerId=ownerId; this.targetId=targetId; this.expiresAt=expiresAt; this.anchor=anchor;
            this.yaw=yaw; this.pitch=pitch; this.oldInvisible=oldInvisible; this.oldInvulnerable=oldInvulnerable;
            this.oldNoGravity=oldNoGravity; this.oldSilent=oldSilent; this.oldNoPhysics=oldNoPhysics; this.oldTargetId=oldTargetId;
        }
    }

    private record NpcCloneState(ServerLevel level, UUID ownerId, UUID copyId, long expiresAt) {}
}
