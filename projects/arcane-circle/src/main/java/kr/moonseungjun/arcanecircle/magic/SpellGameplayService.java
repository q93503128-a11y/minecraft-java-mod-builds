package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.registry.ModItems;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative runtime for spells whose fiction cannot be represented by a one-shot
 * potion pulse. It owns persistent zones/walls, real hard control, temporary flight, defensive
 * interception, death substitutes and target-locked ground effects. Visuals remain client world
 * geometry, with active-field lifetimes matched through {@link #visualDurationTicks(String)}.
 */
public final class SpellGameplayService {
    private static final Set<String> HANDLED = Set.of(
            "grease", "sleep", "web", "mirror_image", "hold_person", "blur",
            "shield", "mage_armor", "invisibility", "haste", "protection_from_energy",
            "greater_invisibility", "stoneskin",
            "fly", "slow", "sleet_storm", "resilient_sphere", "freedom_of_movement",
            "wall_of_fire", "ice_storm", "wall_of_force", "cloudkill", "hold_monster",
            "flame_strike", "dominate_person", "insect_plague", "true_seeing", "flesh_to_stone",
            "globe_of_invulnerability", "mass_suggestion", "move_earth", "circle_of_death",
            "delayed_blast_fireball", "finger_of_death", "fire_storm", "forcecage", "prismatic_spray",
            "reverse_gravity", "simulacrum", "teleport",
            "clone", "control_weather", "dominate_monster", "earthquake", "incendiary_cloud", "maze", "sunburst",
            "prismatic_wall", "shapechange", "true_polymorph", "weird", "foresight",
            "ice_knife", "wind_wall", "counterspell", "fire_shield", "wall_of_ice",
            "thunder_cage", "winter_domain", "astral_prison", "phoenix_requiem");

    private static final Map<UUID, FlightState> FLIGHT = new HashMap<>();
    private static final Map<UUID, MirrorState> MIRRORS = new HashMap<>();
    private static final Map<UUID, ReductionWard> REDUCTION = new HashMap<>();
    private static final Map<UUID, FireShieldState> FIRE_SHIELDS = new HashMap<>();
    private static final Map<UUID, DeathWard> DEATH_WARDS = new HashMap<>();
    private static final Map<UUID, ControlState> CONTROLS = new HashMap<>();
    private static final List<ZoneState> ZONES = new ArrayList<>();
    private static final Map<UUID, WeatherState> WEATHER = new HashMap<>();
    private static final Map<UUID, Long> WEATHER_SPECIAL_READY = new HashMap<>();
    private static final Map<UUID, WeatherBarrage> WEATHER_BARRAGES = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> RETALIATING = ThreadLocal.withInitial(() -> false);

    private SpellGameplayService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer player, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (snapshot == null || !snapshot.validFor(player)) return false;
        return switch (spellId) {
            case "shield", "mage_armor", "invisibility", "haste", "protection_from_energy",
                    "greater_invisibility", "stoneskin" -> ArcaneBuffRuntime.apply(player, spellId, power, range);
            case "grease", "web", "slow", "sleet_storm", "cloudkill", "insect_plague",
                    "incendiary_cloud", "winter_domain" -> startZone(player, spellId, range, power, snapshot);
            case "wall_of_fire", "wall_of_force", "wind_wall", "wall_of_ice", "prismatic_wall" ->
                    startWall(player, spellId, range, power, snapshot);
            case "delayed_blast_fireball", "finger_of_death", "fire_storm", "prismatic_spray",
                    "reverse_gravity", "teleport" ->
                    SeventhCircleSpellService.execute(player, spellId, range, power, snapshot);
            case "ice_storm", "flame_strike", "move_earth", "circle_of_death", "earthquake",
                    "sunburst", "weird" -> areaImpact(player, spellId, range, power, snapshot);
            case "sleep" -> sleep(player, range, snapshot);
            case "hold_person" -> controlSingle(player, spellId, power, snapshot, 180);
            case "hold_monster" -> controlSingle(player, spellId, power, snapshot, 300);
            case "flesh_to_stone" -> controlSingle(player, spellId, power, snapshot, 360);
            case "forcecage" -> controlSingle(player, spellId, power, snapshot, 400);
            case "dominate_person" -> controlSingle(player, spellId, power, snapshot, 260);
            case "dominate_monster" -> controlSingle(player, spellId, power, snapshot, 480);
            case "maze" -> controlSingle(player, spellId, power, snapshot, 360);
            case "true_polymorph" -> controlSingle(player, spellId, power, snapshot, 480);
            case "thunder_cage" -> controlSingle(player, spellId, power, snapshot, 160);
            case "astral_prison" -> controlSingle(player, spellId, power, snapshot, 220);
            case "counterspell" -> counterspell(player, snapshot);
            case "mass_suggestion" -> massSuggestion(player, range, snapshot);
            case "mirror_image" -> mirrorImage(player);
            case "blur" -> blur(player);
            case "fly" -> fly(player);
            case "resilient_sphere" -> resilientSphere(player, power);
            case "globe_of_invulnerability" -> globe(player, power);
            case "freedom_of_movement" -> ArcaneBuffRuntime.apply(player, spellId, power, range);
            case "true_seeing" -> ArcaneBuffRuntime.apply(player, spellId, power, range);
            case "simulacrum" -> simulacrum(player, power);
            case "clone" -> cloneWard(player, power);
            case "control_weather" -> controlWeather(player, range, power);
            case "shapechange" -> ArcaneBuffRuntime.apply(player, spellId, power, range);
            case "foresight" -> ArcaneBuffRuntime.apply(player, spellId, power, range);
            case "ice_knife" -> iceKnife(player, range, power, snapshot);
            case "fire_shield" -> fireShield(player, power);
            case "phoenix_requiem" -> phoenixRequiem(player, range, power);
            default -> false;
        };
    }

    public static int visualDurationTicks(String spellId) {
        int buffDuration = ArcaneBuffRuntime.durationTicks(spellId);
        if (buffDuration > 0) return buffDuration;
        return switch (spellId) {
            case "time_stop" -> ArcaneFieldService.TIME_STOP_TICKS;
            case "antimagic_field" -> ArcaneFieldService.ANTIMAGIC_TICKS;
            case "feather_fall" -> 120;
            case "sleep" -> 140;
            case "mass_suggestion" -> 160;
            case "mirror_image" -> 260;
            case "blur" -> 360;
            case "fly" -> 600;
            case "simulacrum" -> 1200;
            case "clone" -> 1800;
            case "grease" -> 160;
            case "web" -> 220;
            case "slow" -> 180;
            case "sleet_storm" -> 180;
            case "wall_of_fire" -> 200;
            case "wall_of_force" -> 240;
            case "cloudkill" -> 220;
            case "insect_plague" -> 220;
            case "wind_wall" -> 180;
            case "wall_of_ice" -> 220;
            case "incendiary_cloud" -> 240;
            case "prismatic_wall" -> 400;
            case "winter_domain" -> 240;
            case "control_weather" -> 900;
            case "hold_person" -> 180;
            case "hold_monster" -> 300;
            case "forcecage" -> 400;
            case "reverse_gravity" -> SeventhCircleSpellService.REVERSE_GRAVITY_TICKS;
            case "true_polymorph" -> 480;
            case "flesh_to_stone" -> 360;
            case "dominate_monster" -> 480;
            case "dominate_person" -> 260;
            case "maze" -> 360;
            case "thunder_cage" -> 160;
            case "astral_prison" -> 220;
            case "resilient_sphere" -> 400;
            case "globe_of_invulnerability" -> 520;
            case "fire_shield" -> 620;
            default -> 0;
        };
    }

    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        ControlState state = CONTROLS.get(caster.getUUID());
        return state != null && state.active();
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        ArcaneBuffRuntime.tick(level, now);
        tickFlight(level, now);
        tickControls(level, now);
        tickZones(level, now);
        tickWeather(level, now);
        cleanupPersonalStates(now);
    }

    /** Secondary G-key authority of a maintained spell. Currently Control Weather owns it. */
    public static boolean useMaintainedAuthority(ServerPlayer player) {
        if (player == null || !player.isAlive()) return false;
        WeatherState state = WEATHER.get(player.getUUID());
        long now = ((ServerLevel) player.level()).getGameTime();
        if (state == null || state.level() != player.level() || !state.active()) {
            ArcaneNoticeService.push(player, Component.literal("§7[보조 권능] §f현재 G키로 발동할 유지형 권능이 없습니다."), 35);
            return false;
        }
        long ready = WEATHER_SPECIAL_READY.getOrDefault(player.getUUID(), 0L);
        if (now < ready) {
            ArcaneNoticeService.push(player, Component.literal("§9[천후 지배] §f낙뢰 회로 재정렬 "
                    + one((ready - now) / 20.0) + "초"), 28);
            return false;
        }
        Vec3 center = weatherAim(player, Math.min(52.0, Math.max(28.0, state.radius())));
        WEATHER_SPECIAL_READY.put(player.getUUID(), now + 50L);
        WEATHER_BARRAGES.put(player.getUUID(), new WeatherBarrage(state.level(), player.getUUID(), center,
                state.power(), 12, now));
        ArcaneNoticeService.push(player, Component.literal("§b[천후 지배 · 낙뢰 명령] §f바라본 지점에 12연속 번개를 호출합니다. §7재사용 2.5초"), 70);
        return true;
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        long now = ((ServerLevel) player.level()).getGameTime();
        UUID id = player.getUUID();
        MirrorState mirror = MIRRORS.get(id);
        if (mirror != null && mirror.expiresAt() > now && mirror.charges() > 0) {
            int remaining = mirror.charges() - 1;
            if (remaining <= 0) {
                MIRRORS.remove(id);
                WorldMagicService.cancelRelease(player, "mirror_image");
            } else MIRRORS.put(id, new MirrorState(remaining, mirror.expiresAt()));
            event.setCanceled(true);
            ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, .8F, 1.7F);
            ArcaneNoticeService.push(player, Component.literal("§b[미러 이미지] §f환영이 공격을 대신 받았습니다. §7남은 환영 " + remaining), 35);
            return;
        }
        if (ArcaneBuffRuntime.onIncomingDamage(player, event, now)) return;
        ReductionWard ward = REDUCTION.get(id);
        if (ward != null && ward.expiresAt() > now) event.setAmount((float) Math.max(0.0, event.getAmount() * (1.0 - ward.reduction())));
        DeathWard death = DEATH_WARDS.get(id);
        float effectivePool = player.getHealth() + player.getAbsorptionAmount();
        if (death != null && death.expiresAt() > now && event.getAmount() >= effectivePool - .001F) {
            DEATH_WARDS.remove(id);
            WorldMagicService.cancelRelease(player, death.kind());
            event.setCanceled(true);
            float restored = "clone".equals(death.kind()) ? player.getMaxHealth() : Math.max(1.0F, player.getMaxHealth() * .48F);
            player.setHealth(restored);
            player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 4.0F));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 3));
            ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE,
                    SoundSource.PLAYERS, 1.0F, "clone".equals(death.kind()) ? .82F : 1.18F);
            ArcaneNoticeService.push(player, Component.literal("§6[" + ("clone".equals(death.kind()) ? "클론" : "시뮬라크럼")
                    + "] §f치명상을 대리체가 대신 받았습니다."), 80);
            return;
        }
        FireShieldState shield = FIRE_SHIELDS.get(id);
        if (shield != null && shield.expiresAt() > now && !RETALIATING.get()) {
            Entity source = event.getSource().getEntity();
            if (source instanceof LivingEntity attacker && attacker != player && attacker.isAlive() && attacker.distanceToSqr(player) <= 49.0) {
                RETALIATING.set(true);
                try {
                    ArcaneDamage.hurt((ServerLevel) player.level(), player, attacker, (float) Math.max(2.0, shield.power() * .26));
                    attacker.setRemainingFireTicks(Math.max(attacker.getRemainingFireTicks(), 80));
                } finally { RETALIATING.set(false); }
            }
        }
    }

    /** Clears gameplay state and cancels the matching maintained world-geometry releases. */
    public static void clear(LivingEntity subject) {
        if(subject==null)return;
        UUID id=subject.getUUID();
        Set<String> own=new HashSet<>();
        if(FLIGHT.containsKey(id))own.add("fly");
        if(MIRRORS.containsKey(id))own.add("mirror_image");
        ReductionWard reduction=REDUCTION.get(id); if(reduction!=null)own.add(reduction.kind());
        if(FIRE_SHIELDS.containsKey(id))own.add("fire_shield");
        DeathWard death=DEATH_WARDS.get(id); if(death!=null)own.add(death.kind());
        if(WEATHER.containsKey(id))own.add("control_weather");
        for(ZoneState zone:ZONES)if(zone.ownerId.equals(id))own.add(zone.spellId);
        for(ControlState state:CONTROLS.values()){
            if(state.ownerId().equals(id))own.add(state.kind());
            else if(state.targetId().equals(id)){
                Entity rawOwner=state.level().getEntity(state.ownerId());
                if(rawOwner instanceof LivingEntity livingOwner)WorldMagicService.cancelRelease(livingOwner,state.kind());
            }
        }
        clear(id);
        for(String spellId:own)WorldMagicService.cancelRelease(subject,spellId);
    }

    public static void clear(UUID id) {
        FlightState flight = FLIGHT.remove(id);
        if (flight != null) revokeFlight(flight);
        MIRRORS.remove(id); REDUCTION.remove(id); FIRE_SHIELDS.remove(id); DEATH_WARDS.remove(id);
        ArcaneBuffRuntime.clear(id);
        WeatherState weather = WEATHER.remove(id);
        WEATHER_SPECIAL_READY.remove(id);
        WEATHER_BARRAGES.remove(id);
        if (weather != null && WEATHER.values().stream().noneMatch(state -> state.level() == weather.level() && state.active())) {
            setWeather(weather.level(), false, 100);
        }
        Iterator<ControlState> control = CONTROLS.values().iterator();
        while (control.hasNext()) {
            ControlState state = control.next();
            if (!state.ownerId().equals(id) && !state.targetId().equals(id)) continue;
            restoreControl(state); control.remove();
        }
        ZONES.removeIf(zone -> zone.ownerId.equals(id));
    }

    public static void clearAll() {
        for (FlightState state : FLIGHT.values()) revokeFlight(state);
        for (ControlState state : CONTROLS.values()) restoreControl(state);
        FLIGHT.clear(); MIRRORS.clear(); REDUCTION.clear(); FIRE_SHIELDS.clear(); DEATH_WARDS.clear();
        ArcaneBuffRuntime.clearAll();
        CONTROLS.clear(); ZONES.clear(); WEATHER.clear(); WEATHER_SPECIAL_READY.clear();
        WEATHER_BARRAGES.clear(); LAST_TICK.clear();
    }

    private static boolean startZone(ServerPlayer player, String id, double range, double power, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = switch (id) {
            case "grease" -> Math.max(3.2, range * .30); case "web" -> Math.max(4.2, range * .32);
            case "slow" -> Math.max(5.0, range * .30); case "sleet_storm" -> Math.max(6.5, range * .36);
            case "cloudkill", "insect_plague" -> Math.max(7.5, range * .36);
            case "incendiary_cloud" -> Math.max(9.0, range * .34); case "winter_domain" -> Math.max(9.0, range * .45);
            default -> 5.0;
        };
        int duration = visualDurationTicks(id); Vec3 center = snapshot.target();
        ZONES.removeIf(zone -> zone.ownerId.equals(player.getUUID()) && zone.spellId.equals(id));
        ZONES.add(new ZoneState(level, player.getUUID(), id, center, flat(snapshot.launchDirection()), radius, 0.0,
                level.getGameTime() + duration, power, level.getGameTime()));
        ArcaneNoticeService.push(player, Component.literal("§d[지속 마법] §f" + displayName(id) + " §7· " + one(duration / 20.0) + "초 · 반경 " + one(radius)), 55);
        return true;
    }

    private static boolean startWall(ServerPlayer player, String id, double range, double power, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        int circle = switch (id) { case "wind_wall" -> 3; case "wall_of_fire", "wall_of_ice" -> 4; case "wall_of_force" -> 5; default -> 9; };
        double halfWidth = Math.max(3.5, SpellMetrics.wallWidth(id, Math.max(8.0, range), circle) * .5);
        int duration = visualDurationTicks(id); Vec3 center = snapshot.target();
        ZONES.removeIf(zone -> zone.ownerId.equals(player.getUUID()) && zone.spellId.equals(id));
        ZONES.add(new ZoneState(level, player.getUUID(), id, center, flat(snapshot.launchDirection()), Math.max(4.0, halfWidth), halfWidth,
                level.getGameTime() + duration, power, level.getGameTime()));
        ArcaneNoticeService.push(player, Component.literal("§5[장벽 유지] §f" + displayName(id) + " §7· " + one(duration / 20.0) + "초 · 폭 " + one(halfWidth * 2.0)), 55);
        return true;
    }

    private static boolean areaImpact(ServerPlayer player, String id, double range, double power, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level(); Vec3 center = snapshot.target();
        double radius = switch (id) {
            case "ice_storm" -> Math.max(6.0, range * .30); case "flame_strike" -> Math.max(5.5, range * .25);
            case "move_earth" -> Math.max(8.0, range * .34); case "circle_of_death" -> Math.max(10.0, range * .30);
            case "delayed_blast_fireball" -> Math.max(13.0, range * .38);
            case "fire_storm" -> Math.max(14.0, range * .38); case "reverse_gravity" -> Math.max(13.0, range * .40);
            case "earthquake" -> Math.max(18.0, range * .42); case "sunburst" -> Math.max(18.0, range * .42);
            case "weird" -> Math.max(18.0, range * .40); default -> 7.0;
        };
        if ("fire_storm".equals(id)) {
            for (int i = 0; i < 7; i++) {
                double angle = Math.PI * 2.0 * i / 7.0, offset = i == 0 ? 0.0 : radius * .48;
                Vec3 at = center.add(Math.cos(angle) * offset, 0.0, Math.sin(angle) * offset);
                for (LivingEntity target : enemies(player, at, 4.8, 6.0)) {
                    ArcaneDamage.hurt(level, player, target, (float) (power * .72)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 260));
                }
                DestructiveMagicService.impact(player, "fire_storm", at, 4.8, power * .72);
            }
            level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.3F, .66F); return true;
        }
        for (LivingEntity target : enemies(player, center, radius, Math.max(6.0, radius * .72))) {
            switch (id) {
                case "ice_storm" -> { ArcaneDamage.hurt(level, player, target, (float) power); target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 300)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 180, 4)); }
                case "flame_strike" -> { ArcaneDamage.hurt(level, player, target, (float) power); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 260)); }
                case "delayed_blast_fireball" -> { ArcaneDamage.hurt(level, player, target, (float) (power * 1.20)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 360)); }
                case "move_earth", "earthquake" -> { double scale="earthquake".equals(id)?1.15:1.0; ArcaneDamage.hurt(level, player, target, (float) (power*scale)); Vec3 away = flat(target.position().subtract(center)); double force = "earthquake".equals(id) ? 2.6 : 1.2; target.push(away.x * force, "earthquake".equals(id) ? 2.1 : 1.0, away.z * force); }
                case "circle_of_death" -> ArcaneDamage.hurt(level, player, target, (float) power);
                case "reverse_gravity" -> { ArcaneDamage.hurt(level, player, target, (float) (power * .65)); target.push(0.0, 4.0 + power / 90.0, 0.0); target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 220, 7)); }
                case "sunburst" -> { ArcaneDamage.hurt(level, player, target, (float) (power * 1.15)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 240)); target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 260, 3)); }
                case "weird" -> { ArcaneDamage.hurt(level, player, target, (float) (power * 1.25)); target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 700, 5)); target.addEffect(new MobEffectInstance(MobEffects.WITHER, 700, 6)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 700, 6)); }
                default -> { }
            }
        }
        if (Set.of("flame_strike", "delayed_blast_fireball", "move_earth", "earthquake").contains(id)) DestructiveMagicService.impact(player, id, center, radius, power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, "ice_storm".equals(id) ? 1.25F : .78F);
        return true;
    }

    private static boolean sleep(ServerPlayer player, double range, CastTargetSnapshot snapshot) {
        Vec3 center = snapshot.target(); double radius = Math.max(4.0, range * .32); int affected = 0;
        for (LivingEntity target : enemies(player, center, radius, 5.0)) { control(player, target, "sleep", 140); target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0)); affected++; }
        return affected > 0;
    }

    private static boolean controlSingle(ServerPlayer player, String kind, double power, CastTargetSnapshot snapshot, int duration) {
        LivingEntity target = lockedTarget(player, snapshot); if (target == null) return false;
        switch (kind) {
            case "flesh_to_stone" -> { ArcaneDamage.hurt((ServerLevel) player.level(), player, target, (float) (power * .42)); target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 3)); }
            case "thunder_cage" -> ArcaneDamage.hurt((ServerLevel) player.level(), player, target, (float) power);
            case "astral_prison" -> ArcaneDamage.hurt((ServerLevel) player.level(), player, target, (float) (power * .58));
            case "true_polymorph" -> ArcaneDamage.hurt((ServerLevel) player.level(), player, target, (float) (power * .50));
            default -> { }
        }
        control(player, target, kind, duration);
        if ("maze".equals(kind)) { target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 3)); target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration, 1)); }
        if ("thunder_cage".equals(kind) || "astral_prison".equals(kind)) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        ArcaneNoticeService.push(player, Component.literal("§5[제어 성공] §f" + displayName(kind) + " §7· " + one(duration / 20.0) + "초"), 50); return true;
    }

    private static boolean counterspell(ServerPlayer player, CastTargetSnapshot snapshot) {
        LivingEntity target = lockedTarget(player, snapshot); if (target == null) return false;
        target.removeEffect(MobEffects.SPEED); target.removeEffect(MobEffects.RESISTANCE); target.removeEffect(MobEffects.ABSORPTION); target.removeEffect(MobEffects.REGENERATION); target.removeEffect(MobEffects.INVISIBILITY);
        control(player, target, "counterspell", 35); WorldMagicService.stop(target);
        ArcaneNoticeService.push(player, Component.literal("§b[스펠 브레이커] §f대상 주문 회로를 끊고 1.8초간 재시전을 봉쇄했습니다."), 45); return true;
    }

    private static boolean massSuggestion(ServerPlayer player, double range, CastTargetSnapshot snapshot) {
        Vec3 center = snapshot.target(); double radius = Math.max(7.0, range * .27); int affected = 0;
        for (LivingEntity target : enemies(player, center, radius, radius * .7)) { control(player, target, "mass_suggestion", 160); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 3)); affected++; }
        return affected > 0;
    }

    private static void control(ServerPlayer owner, LivingEntity target, String kind, int duration) {
        if (target == owner || owner.isAlliedTo(target)) return; ServerLevel level = (ServerLevel) owner.level(); long expires = level.getGameTime() + Math.max(1, duration);
        ControlState old = CONTROLS.get(target.getUUID());
        if (old != null && old.active()) {
            double oldScale = old.oldScale();
            if (Double.isNaN(oldScale) && "true_polymorph".equals(kind)) { AttributeInstance scale = target.getAttribute(Attributes.SCALE); if (scale != null) { oldScale = scale.getBaseValue(); scale.setBaseValue(Math.max(.35, oldScale * .58)); } }
            CONTROLS.put(target.getUUID(), new ControlState(old.level(), owner.getUUID(), target.getUUID(), kind, Math.max(old.expiresAt(), expires), old.wasNoAi(), oldScale));
        } else {
            boolean noAi = target instanceof Mob mob && mob.isNoAi(); double oldScale = Double.NaN;
            if ("true_polymorph".equals(kind)) { AttributeInstance scale = target.getAttribute(Attributes.SCALE); if (scale != null) { oldScale = scale.getBaseValue(); scale.setBaseValue(Math.max(.35, oldScale * .58)); } }
            CONTROLS.put(target.getUUID(), new ControlState(level, owner.getUUID(), target.getUUID(), kind, expires, noAi, oldScale));
        }
        if (target instanceof Mob mob) mob.setNoAi(true); target.setDeltaMovement(Vec3.ZERO);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.max(5, duration), 255, true, false)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(5, duration), 5, true, false));
    }

    private static boolean mirrorImage(ServerPlayer player) { long now = ((ServerLevel) player.level()).getGameTime(); MIRRORS.put(player.getUUID(), new MirrorState(3, now + 260)); ArcaneNoticeService.push(player, Component.literal("§b[미러 이미지] §f환영 3체가 다음 3회의 공격을 대신 받습니다."), 60); return true; }
    private static boolean blur(ServerPlayer player) { ServerLevel level = (ServerLevel) player.level(); REDUCTION.put(player.getUUID(), stronger(REDUCTION.get(player.getUUID()), new ReductionWard("blur", level.getGameTime() + 360, .32))); player.addEffect(new MobEffectInstance(MobEffects.SPEED, 360, 1)); return true; }
    private static boolean fly(ServerPlayer player) { ServerLevel level = (ServerLevel) player.level(); FLIGHT.put(player.getUUID(), new FlightState(level, player.getUUID(), level.getGameTime() + 600)); player.getAbilities().mayfly = true; player.getAbilities().flying = true; player.onUpdateAbilities(); player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 640, 0)); ArcaneNoticeService.push(player, Component.literal("§b[플라이] §f30초 동안 자유 비행이 활성화되었습니다."), 70); return true; }
    private static boolean resilientSphere(ServerPlayer player, double power) { ServerLevel level = (ServerLevel) player.level(); REDUCTION.put(player.getUUID(), stronger(REDUCTION.get(player.getUUID()), new ReductionWard("resilient_sphere", level.getGameTime() + 400, .82))); player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, Math.max(3, (int) power / 8))); player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 2)); ArcaneNoticeService.push(player, Component.literal("§b[탄성 구체] §f20초 동안 들어오는 피해의 82%를 역장이 흡수합니다."), 70); return true; }
    private static boolean globe(ServerPlayer player, double power) { ServerLevel level = (ServerLevel) player.level(); REDUCTION.put(player.getUUID(), stronger(REDUCTION.get(player.getUUID()), new ReductionWard("globe_of_invulnerability", level.getGameTime() + 520, .70))); player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 520, 6 + Math.max(0, (int) (power / 45.0)))); player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 520, 3)); return true; }

    private static boolean freedom(ServerPlayer player) { player.removeEffect(MobEffects.SLOWNESS); player.removeEffect(MobEffects.WEAKNESS); player.removeEffect(MobEffects.POISON); player.removeEffect(MobEffects.DARKNESS); player.removeEffect(MobEffects.LEVITATION); player.removeEffect(MobEffects.MINING_FATIGUE); player.setTicksFrozen(0); player.addEffect(new MobEffectInstance(MobEffects.SPEED, 520, 1)); ArcaneNoticeService.push(player, Component.literal("§a[이동의 자유] §f속박·둔화·동결·부양을 해제했습니다."), 50); return true; }
    private static boolean trueSeeing(ServerPlayer player, double range) { ServerLevel level = (ServerLevel) player.level(); int duration = 1200; player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0)); double radius = Math.max(20.0, range); for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), value -> value.isAlive() && value != player)) { entity.removeEffect(MobEffects.INVISIBILITY); entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0)); } ArcaneNoticeService.push(player, Component.literal("§e[진실의 시야] §f60초 동안 주변 투명화를 벗기고 생명체를 표시합니다."), 65); return true; }
    private static boolean simulacrum(ServerPlayer player, double power) { ServerLevel level = (ServerLevel) player.level(); DEATH_WARDS.put(player.getUUID(), new DeathWard("simulacrum", level.getGameTime() + 1200)); player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 8 + (int) (power / 45.0))); player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 1)); ArcaneNoticeService.push(player, Component.literal("§b[시뮬라크럼] §f60초 안의 다음 치명상을 얼음 대리체가 대신 받습니다."), 80); return true; }
    private static boolean cloneWard(ServerPlayer player, double power) { ServerLevel level = (ServerLevel) player.level(); player.setHealth(player.getMaxHealth()); DEATH_WARDS.put(player.getUUID(), new DeathWard("clone", level.getGameTime() + 1800)); player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1800, 12 + (int) (power / 45.0))); player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1800, 2)); ArcaneNoticeService.push(player, Component.literal("§6[클론] §f90초 안의 다음 치명상을 클론이 대신 받고 체력을 완전히 복구합니다."), 90); return true; }
    private static boolean controlWeather(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        int duration = 900;
        setWeather(level, true, duration);
        WEATHER.put(player.getUUID(), new WeatherState(level, player.getUUID(), Math.max(32.0, range * .82),
                power, level.getGameTime() + duration, level.getGameTime()));
        WEATHER_SPECIAL_READY.put(player.getUUID(), level.getGameTime());
        WEATHER_BARRAGES.remove(player.getUUID());
        ArcaneNoticeService.push(player, Component.literal("§9[기후 조종] §f45초간 실제 폭우·뇌우를 지배합니다. "
                + "§bG키§f를 누르면 바라본 지점에 12연속 낙뢰를 명령합니다. §7재사용 2.5초"), 110);
        return true;
    }
    private static boolean shapechange(ServerPlayer player) { ServerLevel level = (ServerLevel) player.level(); int duration = 1800; REDUCTION.put(player.getUUID(), stronger(REDUCTION.get(player.getUUID()), new ReductionWard("shapechange", level.getGameTime() + duration, .35))); player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 5)); player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 3)); player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 3)); player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 3)); player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 2)); player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 12)); return true; }
    private static boolean foresight(ServerPlayer player) { ServerLevel level = (ServerLevel) player.level(); int duration = 2400; REDUCTION.put(player.getUUID(), stronger(REDUCTION.get(player.getUUID()), new ReductionWard("foresight", level.getGameTime() + duration, .38))); player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0)); player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 4)); player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 2)); player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 4)); player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 8)); return true; }

    private static boolean iceKnife(ServerPlayer player, double range, double power, CastTargetSnapshot snapshot) { ServerLevel level = (ServerLevel) player.level(); Vec3 center = snapshot.target(); LivingEntity primary = lockedTarget(player, snapshot); boolean any = false; if (primary != null) { any |= ArcaneDamage.hurt(level, player, primary, (float) (power * .65)); primary.setTicksFrozen(Math.max(primary.getTicksFrozen(), primary.getTicksRequiredToFreeze() + 160)); } double radius = Math.max(2.4, range * .16); for (LivingEntity target : enemies(player, center, radius, 4.0)) { if (target == primary) continue; any |= ArcaneDamage.hurt(level, player, target, (float) (power * .55)); target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 120)); } return any; }
    private static boolean fireShield(ServerPlayer player, double power) { ServerLevel level = (ServerLevel) player.level(); FIRE_SHIELDS.put(player.getUUID(), new FireShieldState(level.getGameTime() + 620, power)); player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 620, 0)); player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 620, Math.max(2, (int) power / 12))); ArcaneNoticeService.push(player, Component.literal("§6[블레이징 이지스] §f31초 동안 근접 공격자를 자동으로 태워 반격합니다."), 65); return true; }
    private static boolean phoenixRequiem(ServerPlayer player, double range, double power) { ServerLevel level = (ServerLevel) player.level(); double radius = Math.max(8.0, range * .42); player.heal((float) Math.max(8.0, power * .35)); player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 2)); player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 360, 0)); for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(radius), value -> value.isAlive() && !value.isSpectator() && (value == player || player.isAlliedTo(value)))) { ally.heal((float) Math.max(4.0, power * .16)); ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1)); } for (LivingEntity enemy : enemies(player, player.position(), radius, radius * .7)) { ArcaneDamage.hurt(level, player, enemy, (float) (power * .62)); enemy.setRemainingFireTicks(Math.max(enemy.getRemainingFireTicks(), 180)); } level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.05F); return true; }

    private static void tickFlight(ServerLevel level, long now) { Iterator<Map.Entry<UUID, FlightState>> iterator = FLIGHT.entrySet().iterator(); while (iterator.hasNext()) { FlightState state = iterator.next().getValue(); if (state.level() != level) continue; Entity raw = level.getEntity(state.playerId()); if (!(raw instanceof ServerPlayer player) || !player.isAlive() || now >= state.expiresAt()) { revokeFlight(state); iterator.remove(); continue; } if (!player.getAbilities().mayfly) { player.getAbilities().mayfly = true; player.onUpdateAbilities(); } } }
    private static void revokeFlight(FlightState state) { Entity raw = state.level().getEntity(state.playerId()); if (!(raw instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return; if (player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.RIFT_BOOTS.get()) return; player.getAbilities().flying = false; player.getAbilities().mayfly = false; player.onUpdateAbilities(); }
    private static void tickControls(ServerLevel level, long now) { Iterator<Map.Entry<UUID, ControlState>> iterator = CONTROLS.entrySet().iterator(); while (iterator.hasNext()) { ControlState state = iterator.next().getValue(); if (state.level() != level) continue; Entity raw = level.getEntity(state.targetId()); if (!(raw instanceof LivingEntity target) || !target.isAlive() || now >= state.expiresAt()) { restoreControl(state); iterator.remove(); continue; } target.setDeltaMovement(Vec3.ZERO); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 255, true, false)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 5, true, false)); if (target instanceof Mob mob) { mob.setNoAi(true); WorldMagicService.stop(mob); } else if (target instanceof ServerPlayer player) { if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false); if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false); SpellKineticsService.cancel(player); } } }
    private static void restoreControl(ControlState state) { Entity raw = state.level().getEntity(state.targetId()); if (!(raw instanceof LivingEntity target)) return; if (target instanceof Mob mob) mob.setNoAi(state.wasNoAi()); if (!Double.isNaN(state.oldScale())) { AttributeInstance scale = target.getAttribute(Attributes.SCALE); if (scale != null) scale.setBaseValue(state.oldScale()); } }

    private static void tickZones(ServerLevel level, long now) { Iterator<ZoneState> iterator = ZONES.iterator(); while (iterator.hasNext()) { ZoneState zone = iterator.next(); if (zone.level != level) continue; Entity ownerRaw = level.getEntity(zone.ownerId); if (!(ownerRaw instanceof ServerPlayer owner) || !owner.isAlive() || now >= zone.expiresAt) { iterator.remove(); continue; } if (now < zone.nextPulse) continue; zone.nextPulse = now + pulseInterval(zone.spellId); if (zone.halfWidth > 0.0) pulseWall(owner, zone); else pulseArea(owner, zone); } }
    private static void pulseArea(ServerPlayer owner, ZoneState zone) { ServerLevel level = zone.level; for (LivingEntity target : enemies(owner, zone.center, zone.radius, Math.max(4.0, zone.radius * .7))) { switch (zone.spellId) { case "grease" -> { target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 28, 4)); double angle = Math.toRadians(Math.floorMod(target.getUUID().hashCode() + (int) level.getGameTime(), 360)); target.push(Math.cos(angle) * .08, 0.0, Math.sin(angle) * .08); } case "web" -> { target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 36, 7)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 36, 2)); } case "slow" -> { target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 5)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2)); } case "sleet_storm" -> { ArcaneDamage.hurt(level, owner, target, (float) (zone.power * .12)); target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 80)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 4)); } case "cloudkill" -> { ArcaneDamage.hurt(level, owner, target, (float) (zone.power * .12)); target.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 2)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 2)); } case "insect_plague" -> { ArcaneDamage.hurt(level, owner, target, (float) (zone.power * .13)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, 2)); } case "incendiary_cloud" -> { ArcaneDamage.hurt(level, owner, target, (float) (zone.power * .14)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 90)); } case "winter_domain" -> { ArcaneDamage.hurt(level, owner, target, (float) (zone.power * .11)); target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 100)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 5)); target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 50, 2)); } default -> { } } } }
    private static void pulseWall(ServerPlayer owner, ZoneState zone) { Vec3 right = new Vec3(-zone.direction.z, 0.0, zone.direction.x); AABB box = new AABB(zone.center, zone.center).inflate(zone.halfWidth + 2.0, 6.0, zone.halfWidth + 2.0); for (LivingEntity target : zone.level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(owner, value))) { Vec3 delta = target.position().subtract(zone.center); double lateral = Math.abs(delta.dot(right)), depth = delta.dot(zone.direction); if (lateral > zone.halfWidth + target.getBbWidth() || Math.abs(depth) > 1.6 + target.getBbWidth()) continue; switch (zone.spellId) { case "wall_of_fire" -> { ArcaneDamage.hurt(zone.level, owner, target, (float) (zone.power * .16)); target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100)); } case "wall_of_force" -> pushFromWall(target, zone, depth, 1.25); case "wind_wall" -> pushFromWall(target, zone, depth, .92); case "wall_of_ice" -> { ArcaneDamage.hurt(zone.level, owner, target, (float) (zone.power * .10)); target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 90)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 4)); pushFromWall(target, zone, depth, .38); } case "prismatic_wall" -> { ArcaneDamage.hurt(zone.level, owner, target, (float) (zone.power * .13)); target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 6)); int roll = Math.floorMod(target.getUUID().hashCode() + (int) zone.level.getGameTime() / 10, 3); target.addEffect(new MobEffectInstance(roll == 0 ? MobEffects.BLINDNESS : roll == 1 ? MobEffects.WEAKNESS : MobEffects.WITHER, 45, 2)); pushFromWall(target, zone, depth, .72); } default -> { } } } }
    private static void pushFromWall(LivingEntity target, ZoneState zone, double depth, double strength) { double sign = depth >= 0.0 ? 1.0 : -1.0; target.push(zone.direction.x * strength * sign, .12, zone.direction.z * strength * sign); }

    private static void tickWeather(ServerLevel level, long now) {
        boolean hadState = false;
        Iterator<Map.Entry<UUID, WeatherState>> iterator = WEATHER.entrySet().iterator();
        while (iterator.hasNext()) {
            WeatherState state = iterator.next().getValue();
            if (state.level() != level) continue;
            hadState = true;
            Entity raw = level.getEntity(state.ownerId());
            if (!(raw instanceof ServerPlayer owner) || !owner.isAlive() || now >= state.expiresAt()) {
                WEATHER_SPECIAL_READY.remove(state.ownerId());
                WEATHER_BARRAGES.remove(state.ownerId());
                iterator.remove();
                continue;
            }
            if (now < state.nextPulse()) continue;
            WEATHER.put(state.ownerId(), new WeatherState(level, state.ownerId(), state.radius(), state.power(),
                    state.expiresAt(), now + 20));
            // Passive storm pressure remains useful without stealing the identity of the active G-key barrage.
            List<LivingEntity> targets = enemies(owner, owner.position(), state.radius(), state.radius() * .78);
            int pulses = Math.min(3, targets.size());
            for (int i = 0; i < pulses; i++) {
                LivingEntity target = targets.get(Math.floorMod(i * 7 + (int) now, targets.size()));
                ArcaneDamage.hurt(level, owner, target, (float) (state.power() * .16));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 2));
                level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.WEATHER, .54F, 1.22F + i * .05F);
            }
        }

        Iterator<Map.Entry<UUID, WeatherBarrage>> barrages = WEATHER_BARRAGES.entrySet().iterator();
        while (barrages.hasNext()) {
            Map.Entry<UUID, WeatherBarrage> entry = barrages.next();
            WeatherBarrage barrage = entry.getValue();
            if (barrage.level() != level) continue;
            WeatherState authority = WEATHER.get(barrage.ownerId());
            Entity raw = level.getEntity(barrage.ownerId());
            if (!(raw instanceof ServerPlayer owner) || !owner.isAlive() || authority == null || !authority.active()) {
                barrages.remove();
                continue;
            }
            if (now < barrage.nextStrike()) continue;
            int batch = Math.min(2, barrage.remaining());
            for (int j = 0; j < batch; j++) {
                int index = 12 - barrage.remaining() + j;
                double angle = index * 2.399963229728653 + (barrage.ownerId().hashCode() & 31) * .03;
                double distance = index == 0 ? 0.0 : 1.8 + (index % 4) * 1.35;
                Vec3 requested = barrage.center().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
                Vec3 strike = groundStrike(level, requested);
                summonLightning(level, strike);
                for (LivingEntity target : enemies(owner, strike, 3.4, 6.0)) {
                    ArcaneDamage.hurt(level, owner, target, (float) (barrage.power() * .44));
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 3));
                }
            }
            int remaining = barrage.remaining() - batch;
            if (remaining <= 0) barrages.remove();
            else entry.setValue(new WeatherBarrage(level, barrage.ownerId(), barrage.center(), barrage.power(),
                    remaining, now + 2));
        }

        boolean activeNow = WEATHER.values().stream().anyMatch(state -> state.level() == level && state.active());
        if (hadState && !activeNow) setWeather(level, false, 100);
    }

    private static void cleanupPersonalStates(long now) { MIRRORS.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now || entry.getValue().charges() <= 0); REDUCTION.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now); FIRE_SHIELDS.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now); DEATH_WARDS.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now); }
    private static int pulseInterval(String id) { return switch (id) { case "grease", "web", "slow", "wall_of_force", "wind_wall" -> 5; default -> 10; }; }
    private static LivingEntity lockedTarget(ServerPlayer player, CastTargetSnapshot snapshot) { return snapshot.targetEntity(player).filter(value -> enemy(player, value)).orElse(null); }
    private static List<LivingEntity> enemies(ServerPlayer owner, Vec3 center, double radius, double vertical) { return ((ServerLevel) owner.level()).getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, vertical, radius), value -> enemy(owner, value)); }
    private static boolean enemy(ServerPlayer owner, LivingEntity target) { if (!target.isAlive() || target.isRemoved() || target == owner || owner.isAlliedTo(target)) return false; return !(target instanceof TamableAnimal tame && tame.isTame() && tame.isOwnedBy(owner)); }
    private static Vec3 flat(Vec3 value) { Vec3 flat = new Vec3(value.x, 0.0, value.z); return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize(); }
    private static ReductionWard stronger(ReductionWard old, ReductionWard next) { if (old == null) return next; if (old.reduction() >= next.reduction()) return new ReductionWard(old.kind(), Math.max(old.expiresAt(), next.expiresAt()), old.reduction()); return next.expiresAt() >= old.expiresAt() ? next : new ReductionWard(next.kind(), old.expiresAt(), next.reduction()); }
    private static String displayName(String id) { return SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id); }
    private static String one(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }

    private static Vec3 weatherAim(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(Math.max(8.0, range)));
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 result = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        return groundStrike(level, result);
    }

    private static Vec3 groundStrike(ServerLevel level, Vec3 requested) {
        BlockPos pos = BlockPos.containing(requested);
        int steps = 0;
        while (steps++ < 48 && pos.getY() > level.getMinY() + 1 && level.getBlockState(pos).isAir()) pos = pos.below();
        return new Vec3(pos.getX() + .5, pos.getY() + 1.05, pos.getZ() + .5);
    }

    private static void summonLightning(ServerLevel level, Vec3 at) {
        CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, new Vec2(0.0F, 0.0F),
                level, LevelBasedPermissionSet.ADMIN, "ArcaneCircle", Component.literal("ArcaneCircle"),
                level.getServer(), null);
        String command = String.format(java.util.Locale.ROOT, "/summon minecraft:lightning_bolt %.2f %.2f %.2f",
                at.x, at.y, at.z);
        level.getServer().getCommands().performPrefixedCommand(source, command);
    }

    private static void setWeather(ServerLevel level, boolean thunder, int durationTicks) {
        CommandSourceStack source = new CommandSourceStack(
                CommandSource.NULL,
                Vec3.ZERO,
                new Vec2(0.0F, 0.0F),
                level,
                LevelBasedPermissionSet.ADMIN,
                "ArcaneCircle",
                Component.literal("ArcaneCircle"),
                level.getServer(),
                null);
        int seconds = Math.max(1, (durationTicks + 19) / 20);
        level.getServer().getCommands().performPrefixedCommand(
                source, (thunder ? "/weather thunder " : "/weather clear ") + seconds);
    }

    private record FlightState(ServerLevel level, UUID playerId, long expiresAt) {}
    private record MirrorState(int charges, long expiresAt) {}
    private record ReductionWard(String kind, long expiresAt, double reduction) {}
    private record FireShieldState(long expiresAt, double power) {}
    private record DeathWard(String kind, long expiresAt) {}
    private record ControlState(ServerLevel level, UUID ownerId, UUID targetId, String kind, long expiresAt, boolean wasNoAi, double oldScale) { boolean active() { return level.getGameTime() < expiresAt; } }
    private record WeatherState(ServerLevel level, UUID ownerId, double radius, double power, long expiresAt, long nextPulse) { boolean active() { return level.getGameTime() < expiresAt; } }
    private record WeatherBarrage(ServerLevel level, UUID ownerId, Vec3 center, double power, int remaining, long nextStrike) {}
    private static final class ZoneState {
        private final ServerLevel level; private final UUID ownerId; private final String spellId; private final Vec3 center; private final Vec3 direction; private final double radius; private final double halfWidth; private final long expiresAt; private final double power; private long nextPulse;
        private ZoneState(ServerLevel level, UUID ownerId, String spellId, Vec3 center, Vec3 direction, double radius, double halfWidth, long expiresAt, double power, long nextPulse) { this.level = level; this.ownerId = ownerId; this.spellId = spellId; this.center = center; this.direction = direction; this.radius = radius; this.halfWidth = halfWidth; this.expiresAt = expiresAt; this.power = power; this.nextPulse = nextPulse; }
    }
}
