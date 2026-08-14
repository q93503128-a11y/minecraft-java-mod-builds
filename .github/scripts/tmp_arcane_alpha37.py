from pathlib import Path
import re

root = Path('projects/arcane-circle')
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'


def replace_once(path: Path, old: str, new: str):
    body = path.read_text(encoding='utf-8')
    count = body.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one copy, found {count}: {old[:100]!r}')
    path.write_text(body.replace(old, new), encoding='utf-8')


def sub_once(path: Path, pattern: str, repl: str, flags=0):
    body = path.read_text(encoding='utf-8')
    changed, count = re.subn(pattern, repl, body, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f'{path}: regex expected one match: {pattern[:100]!r}')
    path.write_text(changed, encoding='utf-8')


buff = r'''package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative non-potion identity for self buffs.
 *
 * Vanilla status effects are allowed as readable secondary feedback, but the spell's defining
 * mechanic lives here: reactive shield charges, regenerating armor plates, phase dodges, haste
 * affecting Arcane cast/cooldown timing, persistent freedom/true-sight enforcement, stoneskin
 * impact shaping, solar retaliation, shapechange regeneration and foresight prediction windows.
 */
public final class ArcaneBuffRuntime {
    private record BuffKey(UUID playerId, String spellId) {}

    private static final class State {
        final ServerLevel level;
        final UUID playerId;
        final String spellId;
        final long expiresAt;
        final double power;
        final double radius;
        int charges;
        long nextChargeAt;

        State(ServerLevel level, UUID playerId, String spellId, long expiresAt, double power,
              double radius, int charges, long nextChargeAt) {
            this.level = level;
            this.playerId = playerId;
            this.spellId = spellId;
            this.expiresAt = expiresAt;
            this.power = power;
            this.radius = radius;
            this.charges = charges;
            this.nextChargeAt = nextChargeAt;
        }
    }

    private static final Map<BuffKey, State> STATES = new HashMap<>();

    private ArcaneBuffRuntime() {}

    public static int durationTicks(String spellId) {
        return switch (spellId) {
            case "shield" -> 170;
            case "mage_armor" -> 720;
            case "invisibility" -> 420;
            case "haste" -> 600;
            case "protection_from_energy" -> 600;
            case "greater_invisibility" -> 780;
            case "stoneskin" -> 760;
            case "freedom_of_movement" -> 520;
            case "true_seeing" -> 1200;
            case "solar_guard" -> 600;
            case "shapechange" -> 1800;
            case "foresight" -> 2400;
            default -> 0;
        };
    }

    public static boolean apply(ServerPlayer player, String spellId, double power, double range) {
        int duration = durationTicks(spellId);
        if (duration <= 0) return false;
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        int charges = switch (spellId) {
            case "shield" -> 2;
            case "mage_armor" -> 4;
            case "invisibility" -> 1;
            case "protection_from_energy" -> 5;
            case "solar_guard" -> 4;
            default -> 0;
        };
        long ready = now + rechargeInterval(spellId);
        State state = new State(level, player.getUUID(), spellId, now + duration, power,
                Math.max(18.0, range), charges, ready);
        STATES.put(new BuffKey(player.getUUID(), spellId), state);

        switch (spellId) {
            case "shield" -> notice(player, "§b[아케인 실드] §f2장의 반응 방벽이 다음 충격을 직접 흡수합니다.");
            case "mage_armor" -> notice(player, "§9[메이지 아머] §f4장의 아케인 플레이트가 소모·재생되며 피해를 분산합니다.");
            case "invisibility" -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, true, false));
                notice(player, "§7[인비저빌리티] §f은신막이 첫 피격까지 공격 궤적 하나를 완전히 빗나가게 합니다.");
            }
            case "haste" -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1, true, false));
                notice(player, "§e[헤이스트] §f마법 회로 전개 28% 단축 · 재사용 대기 15% 단축.");
            }
            case "protection_from_energy" -> notice(player,
                    "§d[에너지 보호] §f5중 공명막이 강한 충격을 순차 흡수하며 전투 중 다시 충전됩니다.");
            case "greater_invisibility" -> {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, true, false));
                state.nextChargeAt = now;
                notice(player, "§5[상급 투명화] §f위상막이 유지되며 4초마다 다음 피격을 한 번 지웁니다.");
            }
            case "stoneskin" -> notice(player,
                    "§7[스톤스킨] §f피부가 석질 장갑으로 굳어 큰 타격의 비율 피해와 충격량을 함께 깎습니다.");
            case "freedom_of_movement" -> {
                cleanseMovement(player);
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1, true, false));
                notice(player, "§a[이동의 자유] §f26초 동안 둔화·속박·동결·강제 부양을 계속 씻어냅니다.");
            }
            case "true_seeing" -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false));
                reveal(level, player, state.radius);
                notice(player, "§e[진실의 시야] §f60초 동안 주변 은신을 주기적으로 벗기고 생명 반응을 추적합니다.");
            }
            case "solar_guard" -> {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, true, false));
                notice(player, "§6[솔라 가드] §f4장의 태양 방패가 재충전되며 타격자를 불태우고 밀어냅니다.");
            }
            case "shapechange" -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 3, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 2, true, false));
                notice(player, "§d[셰이프체인지] §f90초 동안 변이 육체가 피해를 흘리고 자체 재생합니다.");
            }
            case "foresight" -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false));
                state.nextChargeAt = now;
                notice(player, "§e[포사이트] §f120초 동안 3초마다 다음 치명 궤적 하나를 미리 읽어 완전히 회피합니다.");
            }
            default -> { }
        }
        return true;
    }

    public static boolean onIncomingDamage(ServerPlayer player, LivingIncomingDamageEvent event, long now) {
        State foresight = active(player, "foresight", now);
        if (foresight != null && now >= foresight.nextChargeAt) {
            foresight.nextChargeAt = now + 60;
            event.setCanceled(true);
            chime(player, 1.72F);
            return true;
        }
        State greaterVeil = active(player, "greater_invisibility", now);
        if (greaterVeil != null && now >= greaterVeil.nextChargeAt) {
            greaterVeil.nextChargeAt = now + 80;
            event.setCanceled(true);
            chime(player, 1.92F);
            return true;
        }
        State veil = active(player, "invisibility", now);
        if (veil != null && veil.charges > 0) {
            veil.charges = 0;
            STATES.remove(new BuffKey(player.getUUID(), "invisibility"));
            player.removeEffect(MobEffects.INVISIBILITY);
            event.setCanceled(true);
            chime(player, 1.55F);
            return true;
        }

        float amount = event.getAmount();
        State shield = active(player, "shield", now);
        if (shield != null && shield.charges > 0) {
            shield.charges--;
            amount -= (float) Math.max(3.0, 2.0 + shield.power * .18);
            chime(player, 1.35F + shield.charges * .12F);
        }

        double multiplier = 1.0;
        double flat = 0.0;
        State energy = active(player, "protection_from_energy", now);
        if (energy != null && energy.charges > 0) {
            energy.charges--;
            if (energy.charges < maxCharges("protection_from_energy") && energy.nextChargeAt <= now)
                energy.nextChargeAt = now + rechargeInterval("protection_from_energy");
            multiplier = Math.min(multiplier, .55);
        }
        State armor = active(player, "mage_armor", now);
        if (armor != null) {
            multiplier = Math.min(multiplier, armor.charges > 0 ? .68 : .84);
            flat = Math.max(flat, .8);
            if (armor.charges > 0) armor.charges--;
        }
        State stone = active(player, "stoneskin", now);
        if (stone != null) {
            multiplier = Math.min(multiplier, .70);
            flat = Math.max(flat, 1.35 + stone.power * .015);
        }
        State shape = active(player, "shapechange", now);
        if (shape != null) multiplier = Math.min(multiplier, .65);
        State solar = active(player, "solar_guard", now);
        if (solar != null && solar.charges > 0) {
            solar.charges--;
            multiplier = Math.min(multiplier, .58);
            Entity source = event.getSource().getEntity();
            if (source instanceof LivingEntity attacker && attacker != player && attacker.isAlive()) {
                Vec3Push.pushAway(attacker, player, 1.15);
                attacker.setRemainingFireTicks(Math.max(attacker.getRemainingFireTicks(), 80));
            }
            chime(player, 1.18F);
        }

        float resolved = (float) Math.max(0.0, amount * multiplier - flat);
        if (resolved <= .05F) {
            event.setCanceled(true);
            return true;
        }
        event.setAmount(resolved);
        return false;
    }

    public static void tick(ServerLevel level, long now) {
        Iterator<Map.Entry<BuffKey, State>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BuffKey, State> entry = iterator.next();
            State state = entry.getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.playerId);
            if (!(raw instanceof ServerPlayer player) || !player.isAlive() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            if ("freedom_of_movement".equals(state.spellId) && now % 5L == 0L) cleanseMovement(player);
            if ("true_seeing".equals(state.spellId) && now % 10L == 0L) reveal(level, player, state.radius);
            if ("shapechange".equals(state.spellId) && now % 20L == 0L)
                player.heal((float) Math.max(.45, .25 + state.power * .004));
            int max = maxCharges(state.spellId);
            int interval = rechargeInterval(state.spellId);
            if (max > 0 && interval > 0 && state.charges < max && now >= state.nextChargeAt) {
                state.charges++;
                state.nextChargeAt = now + interval;
            }
        }
    }

    public static double castTimeMultiplier(ServerPlayer player) {
        long now = ((ServerLevel) player.level()).getGameTime();
        return active(player, "haste", now) == null ? 1.0 : .72;
    }

    public static double cooldownMultiplier(ServerPlayer player) {
        long now = ((ServerLevel) player.level()).getGameTime();
        return active(player, "haste", now) == null ? 1.0 : .85;
    }

    public static int adjustCooldownTicks(ServerPlayer player, int ticks) {
        if (ticks <= 0) return 0;
        return Math.max(1, (int) Math.round(ticks * cooldownMultiplier(player)));
    }

    public static void clear(UUID playerId) {
        STATES.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    public static void clearAll() { STATES.clear(); }

    private static State active(ServerPlayer player, String spellId, long now) {
        BuffKey key = new BuffKey(player.getUUID(), spellId);
        State state = STATES.get(key);
        if (state == null) return null;
        if (state.level != player.level() || state.expiresAt <= now || !player.isAlive()) {
            STATES.remove(key);
            return null;
        }
        return state;
    }

    private static int maxCharges(String spellId) {
        return switch (spellId) {
            case "mage_armor" -> 4;
            case "protection_from_energy" -> 5;
            case "solar_guard" -> 4;
            default -> 0;
        };
    }

    private static int rechargeInterval(String spellId) {
        return switch (spellId) {
            case "mage_armor" -> 90;
            case "protection_from_energy" -> 70;
            case "solar_guard" -> 80;
            default -> 0;
        };
    }

    private static void cleanseMovement(ServerPlayer player) {
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MINING_FATIGUE);
        player.removeEffect(MobEffects.LEVITATION);
        player.setTicksFrozen(0);
    }

    private static void reveal(ServerLevel level, ServerPlayer player, double radius) {
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(Math.max(16.0, radius)),
                value -> value.isAlive() && value != player)) {
            entity.removeEffect(MobEffects.INVISIBILITY);
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 18, 0, true, false));
        }
    }

    private static void notice(ServerPlayer player, String text) {
        ArcaneNoticeService.push(player, Component.literal(text), 70);
    }

    private static void chime(ServerPlayer player, float pitch) {
        ((ServerLevel) player.level()).playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, .72F, pitch);
    }

    /** Keeps knockback math isolated and avoids creating another damage event during solar retaliation. */
    private static final class Vec3Push {
        static void pushAway(LivingEntity target, LivingEntity source, double force) {
            net.minecraft.world.phys.Vec3 delta = target.position().subtract(source.position());
            net.minecraft.world.phys.Vec3 flat = new net.minecraft.world.phys.Vec3(delta.x, 0.0, delta.z);
            if (flat.lengthSqr() < 1.0E-8) flat = new net.minecraft.world.phys.Vec3(0.0, 0.0, 1.0);
            flat = flat.normalize().scale(force);
            target.push(flat.x, .18, flat.z);
        }
    }
}
'''
(magic / 'ArcaneBuffRuntime.java').write_text(buff, encoding='utf-8')

# Gameplay routing: these buffs no longer fall back to ExpandedSpellEffects' potion-only paths.
gameplay = magic / 'SpellGameplayService.java'
replace_once(gameplay,
'''            "grease", "sleep", "web", "mirror_image", "hold_person", "blur",
''',
'''            "grease", "sleep", "web", "mirror_image", "hold_person", "blur",
            "shield", "mage_armor", "invisibility", "haste", "protection_from_energy",
            "greater_invisibility", "stoneskin",
''')
replace_once(gameplay,
'''        return switch (spellId) {
            case "grease", "web", "slow", "sleet_storm", "cloudkill", "insect_plague",
''',
'''        return switch (spellId) {
            case "shield", "mage_armor", "invisibility", "haste", "protection_from_energy",
                    "greater_invisibility", "stoneskin" -> ArcaneBuffRuntime.apply(player, spellId, power, range);
            case "grease", "web", "slow", "sleet_storm", "cloudkill", "insect_plague",
''')
for old, new in {
    'case "freedom_of_movement" -> freedom(player);': 'case "freedom_of_movement" -> ArcaneBuffRuntime.apply(player, spellId, power, range);',
    'case "true_seeing" -> trueSeeing(player, range);': 'case "true_seeing" -> ArcaneBuffRuntime.apply(player, spellId, power, range);',
    'case "shapechange" -> shapechange(player);': 'case "shapechange" -> ArcaneBuffRuntime.apply(player, spellId, power, range);',
    'case "foresight" -> foresight(player);': 'case "foresight" -> ArcaneBuffRuntime.apply(player, spellId, power, range);',
}.items():
    replace_once(gameplay, old, new)
replace_once(gameplay,
'''    public static int visualDurationTicks(String spellId) {
        return switch (spellId) {
''',
'''    public static int visualDurationTicks(String spellId) {
        int buffDuration = ArcaneBuffRuntime.durationTicks(spellId);
        if (buffDuration > 0) return buffDuration;
        return switch (spellId) {
''')
replace_once(gameplay, 'case "prismatic_wall" -> 600;', 'case "prismatic_wall" -> 280;')
replace_once(gameplay, '        tickFlight(level, now);\n', '        ArcaneBuffRuntime.tick(level, now);\n        tickFlight(level, now);\n')
replace_once(gameplay,
'        ReductionWard ward = REDUCTION.get(id);\n',
'        if (ArcaneBuffRuntime.onIncomingDamage(player, event, now)) return;\n        ReductionWard ward = REDUCTION.get(id);\n')
replace_once(gameplay,
'        MIRRORS.remove(id); REDUCTION.remove(id); FIRE_SHIELDS.remove(id); DEATH_WARDS.remove(id);\n',
'        MIRRORS.remove(id); REDUCTION.remove(id); FIRE_SHIELDS.remove(id); DEATH_WARDS.remove(id);\n        ArcaneBuffRuntime.clear(id);\n')
replace_once(gameplay,
'        FLIGHT.clear(); MIRRORS.clear(); REDUCTION.clear(); FIRE_SHIELDS.clear(); DEATH_WARDS.clear();\n',
'        FLIGHT.clear(); MIRRORS.clear(); REDUCTION.clear(); FIRE_SHIELDS.clear(); DEATH_WARDS.clear();\n        ArcaneBuffRuntime.clearAll();\n')

# Haste changes Arcane mechanics, not just walking speed.
casting = magic / 'SpellCastingService.java'
replace_once(casting,
'        double raw = sameCircleTicks[circle] * gapScale * masteryScale * chosen.castTimeMultiplier() * staffScale;\n',
'        double raw = sameCircleTicks[circle] * gapScale * masteryScale * chosen.castTimeMultiplier() * staffScale\n                * ArcaneBuffRuntime.castTimeMultiplier(player);\n')
replace_once(casting,
'        int minimum = Math.max(1, (int) Math.round(baseMinimum * staffScale));\n',
'        int minimum = Math.max(1, (int) Math.round(baseMinimum * staffScale\n                * ArcaneBuffRuntime.castTimeMultiplier(player)));\n')
replace_once(casting,
'            magic.startCooldown(player, ingredient, total);\n',
'            magic.startCooldown(player, ingredient, ArcaneBuffRuntime.adjustCooldownTicks(player, total));\n')
replace_once(casting,
'        data.startCooldown(player, spell.id(), cast.cooldownTicks());\n',
'        data.startCooldown(player, spell.id(), ArcaneBuffRuntime.adjustCooldownTicks(player, cast.cooldownTicks()));\n')
replace_once(casting,
'                    + (cast.cooldownTicks() <= 0 ? "없음" : String.format("%.1f", cast.cooldownTicks() / 20.0) + "초")));\n',
'                    + (cast.cooldownTicks() <= 0 ? "없음" : String.format("%.1f",\n                    ArcaneBuffRuntime.adjustCooldownTicks(player, cast.cooldownTicks()) / 20.0) + "초")));\n')

# Solar Guard keeps its enemy pulse, but the defensive identity is now the solar-charge runtime.
fusion = magic / 'FusionSpellEffects.java'
sub_once(fusion,
    r'    private static boolean solarGuard\(ServerPlayer player, double range, double power\) \{.*?\n    \}\n\n    private static boolean voidLance',
'''    private static boolean solarGuard(ServerPlayer player, double range, double power) {
        ServerLevel level = (ServerLevel) player.level();
        ArcaneBuffRuntime.apply(player, "solar_guard", power, range);
        for (Mob mob : hostiles(player, Math.min(8.0, range))) {
            ArcaneDamage.hurt(level, player, mob, (float) (power * 0.34));
            mob.setRemainingFireTicks(Math.max(mob.getRemainingFireTicks(), 100));
        }
        sound(level, player, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.3F);
        return true;
    }

    private static boolean voidLance''', flags=re.S)

# Grimoire summaries now describe the actual custom mechanics.
summary = magic / 'SpellEffectSummary.java'
summary_replacements = {
    'shield': '약 8.5초 · 반응 방벽 2장으로 다음 충격의 고정 피해를 직접 흡수',
    'mage_armor': '36초 · 4장 재생형 아케인 플레이트가 소모·재충전되며 피해 분산',
    'invisibility': '21초 투명화 · 첫 피격 궤적 1회를 은신막이 완전 회피 후 해제',
    'haste': '30초 · 마법진 전개 28% 단축 + 주문 재사용 대기 15% 단축 + 이동 가속',
    'protection_from_energy': '30초 · 5중 공명막이 강한 충격을 흡수하고 전투 중 재충전',
    'greater_invisibility': '39초 상급 위상 은신 · 4초마다 다음 피격 1회 완전 회피',
    'stoneskin': '38초 석질 장갑 · 큰 타격의 비율 피해와 고정 피해를 동시에 경감',
    'freedom_of_movement': '26초 · 둔화·속박·동결·강제 부양을 지속적으로 정화 + 이동 강화',
    'true_seeing': '60초 · 주변 은신을 주기적으로 벗기고 생명체를 계속 추적 표시',
    'shapechange': '90초 변이 육체 · 35%급 피해 경감 + 자체 재생 + 전투 신체 강화',
    'foresight': '120초 예지 · 3초마다 다음 피격 1회 완전 회피 + 예지 시야',
    'solar_guard': '30초 태양 방패 4장 · 피해 흡수·재충전 + 공격자 점화·넉백',
    'prismatic_wall': '14초 지속 7색 장벽 · 수명 90%까지 선명 유지 · 반복 피해·상태이상·통과 저지',
}
body = summary.read_text(encoding='utf-8')
for spell_id, description in summary_replacements.items():
    pattern = rf'(            case "{re.escape(spell_id)}" -> ").*?(";)'
    body, count = re.subn(pattern, rf'\g<1>{description}\g<2>', body, count=1)
    if count != 1:
        raise SystemExit(f'summary case missing: {spell_id}')
summary.write_text(body, encoding='utf-8')

# Presentation: persistent buff silhouettes + genuinely 3D high-circle authority layers.
overhaul = client / 'ArcaneSpellVisualOverhaul.java'
replace_once(overhaul,
'''    private static final Set<String> CELESTIAL = Set.of(
            "ice_storm", "flame_strike", "fire_storm", "control_weather", "insect_plague",
            "incendiary_cloud", "sunburst", "meteor_swarm", "phoenix_requiem");
''',
'''    private static final Set<String> CELESTIAL = Set.of(
            "ice_storm", "flame_strike", "fire_storm", "control_weather", "insect_plague",
            "incendiary_cloud", "sunburst", "meteor_swarm", "phoenix_requiem");
    private static final Set<String> BUFFS = Set.of(
            "shield", "mage_armor", "mirror_image", "invisibility", "blur", "haste",
            "protection_from_energy", "greater_invisibility", "resilient_sphere", "stoneskin",
            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "fire_shield",
            "solar_guard", "shapechange", "foresight");
''')
sub_once(overhaul,
    r'    static ArcaneWorldMesh chargeSigil\(SpellDefinition spell, Vec3 direction, double progress,\n                                       double range, long startedAtNanos\) \{.*?\n    \}\n\n    static ArcaneWorldMesh chargeBody',
'''    static ArcaneWorldMesh chargeSigil(SpellDefinition spell, Vec3 direction, double progress,
                                       double range, long startedAtNanos) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.fineBuilder(SIGIL_BUDGET);
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        double p = smooth(clamp(progress, 0.0, 1.0));
        double time = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        double r = Math.max(.62, profile.radius()) * (.44 + .56 * p);
        int seed = spell.id().hashCode();
        ArcaneWorldMesh.Basis basis = signatureBasis(spell, direction);

        if (PORTALS.contains(spell.id()))
            portalContract(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else if (PRISONS.contains(spell.id()))
            bindingContract(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else if ("time_stop".equals(spell.id()))
            temporalAstrolabe(m, basis, r, p, time, seed);
        else if ("wish".equals(spell.id()))
            wishCrown(m, basis, r, p, time, seed);
        else if (DEATH.contains(spell.id()))
            executionFormula(m, basis, r, p, time, seed, spell.circle());
        else if (TERRAIN.contains(spell.id()))
            tectonicFormula(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else if (WALLS.contains(spell.id()))
            wallCovenant(m, basis, r, p, time, seed, spell.circle());
        else if (CELESTIAL.contains(spell.id()))
            celestialFormula(m, ArcaneWorldMesh.Basis.ground(), r, p, time, seed, spell.circle());
        else {
            switch (spell.school()) {
                case FIRE -> combustionFormula(m, basis, r, p, time, seed, spell.circle());
                case FROST -> frostFormula(m, basis, r, p, time, seed, spell.circle());
                case WIND -> windFormula(m, basis, r, p, time, seed, spell.circle());
                case WARD -> wardFormula(m, basis, r, p, time, seed, spell.circle());
                case LIFE -> lifeFormula(m, basis, r, p, time, seed, spell.circle());
                case SPACE -> spaceFormula(m, basis, r, p, time, seed, spell.circle());
                case ARCANE -> arcaneFormula(m, basis, r, p, time, seed, spell.circle());
            }
        }
        if (spell.circle() >= 6) highCircleCrown(m, basis, r, p, time, seed, spell.circle());
        return m.build();
    }

    static ArcaneWorldMesh chargeBody''', flags=re.S)
replace_once(overhaul,
'            case AURA -> auraMantle(m, spell, targetOffset, rise, elapsedSeconds);\n',
'''            case AURA -> {
                if (BUFFS.contains(spell.id())) buffMantle(m, spell, targetOffset, rise, elapsedSeconds);
                else auraMantle(m, spell, targetOffset, rise, elapsedSeconds);
            }
''')
replace_once(overhaul,
'''        if (PORTALS.contains(spell.id())) {
            portalPair(m, spell, direction, targetOffset, rise, elapsedSeconds, true);
            return m.build();
        }
''',
'''        if (PORTALS.contains(spell.id())) {
            portalPair(m, spell, direction, targetOffset, rise, elapsedSeconds, true);
            if (spell.circle() >= 7) highCircleAfterimage(m, spell, targetOffset, rise, elapsedSeconds);
            return m.build();
        }
''')
replace_once(overhaul,
'''        if (PRISONS.contains(spell.id())) {
            if ("resilient_sphere".equals(spell.id())) risingSphere(m, targetOffset, rise, elapsedSeconds, spell.circle());
            else risingPrison(m, targetOffset, rise, elapsedSeconds, spell.circle(), spell.id().hashCode());
            return m.build();
        }
''',
'''        if (PRISONS.contains(spell.id())) {
            if ("resilient_sphere".equals(spell.id())) risingSphere(m, targetOffset, rise, elapsedSeconds, spell.circle());
            else risingPrison(m, targetOffset, rise, elapsedSeconds, spell.circle(), spell.id().hashCode());
            if (spell.circle() >= 7) highCircleAfterimage(m, spell, targetOffset, rise, elapsedSeconds);
            return m.build();
        }
''')
sub_once(overhaul,
    r'    private static void highCircleCrown\(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,\n                                        double p, double t, int seed, int circle\) \{.*?\n    \}\n\n    private static void portalContract',
'''    private static void highCircleCrown(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                        double p, double t, int seed, int circle) {
        if (p < .34) return;
        double reveal = smooth(clamp((p - .34) / .66, 0.0, 1.0));
        double authority = r * (.92 + .10 * reveal);
        int seals = Math.min(12, 4 + circle);
        m.brokenBand(b, Vec3.ZERO, authority * .91, authority, 76 + circle * 4, 7,
                1.05F, (float) (.10 + .06 * reveal));
        m.runeRing(b, Vec3.ZERO, authority * .82, seals, r * .035, seed ^ 0x7A11,
                -t * .028, .34F);

        // 7C stops being a flat decal: a second plane cuts through the primary formula.
        if (circle >= 7 && p > .48) {
            ArcaneWorldMesh.Basis cross = ArcaneWorldMesh.Basis.fromNormal(b.right(), b.up());
            m.circle(cross, Vec3.ZERO, r * .48, 54, .44F);
            m.circle(cross, b.normal().scale(r * .07), r * .33, 44, .30F);
        }
        // 8C owns three dimensions with a counter-rotating gyroscope.
        if (circle >= 8 && p > .60) {
            ArcaneWorldMesh.Basis cross2 = ArcaneWorldMesh.Basis.fromNormal(b.up(), b.normal());
            m.circle(cross2, Vec3.ZERO, r * .60, 62, .52F);
            m.brokenBand(cross2, Vec3.ZERO, r * .38, r * .43, 48, 6, 1.0F, .10F);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2.0 + t * .025;
                Vec3 c = b.point(a, r * .70).add(b.normal().scale((i % 2 == 0 ? 1 : -1) * r * .16));
                m.runeGlyph(b, c, r * .052, seed + i * 137, -t * .035 + a, .38F);
            }
        }
        // 9C is not merely larger: nine independent satellite formulae surround the authority core.
        if (circle >= 9 && p > .72) {
            Vec3 n = b.normal();
            m.brokenBand(b, n.scale(r * .10), r * 1.07, r * 1.14, 92, 8, 1.10F, .12F);
            m.brokenBand(b, n.scale(-r * .07), r * .69, r * .75, 72, 7, 1.02F, .10F);
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2.0 / 9.0 - t * .018;
                Vec3 c = b.point(a, r * 1.08).add(n.scale(((i % 3) - 1) * r * .08));
                double sr = r * (.055 + (i % 2) * .009);
                m.circle(b, c, sr, 18, .42F);
                m.polygon(b, c, sr * .68, 3 + i % 4, a + t * .02, .34F);
                m.line(c, b.point(a, r * .78), .24F);
            }
        }
    }

    private static void portalContract''', flags=re.S)

buff_mantle = r'''
    private static void buffMantle(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 center,
                                   double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis front = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        ArcaneWorldMesh.Basis side = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        int seed = spell.id().hashCode();
        double pulse = .96 + .04 * Math.sin(time * 2.0);
        switch (spell.id()) {
            case "shield" -> {
                Vec3 c = center.add(0, 1.15, .62);
                for (int i = 0; i < 3; i++) m.polygon(front, c.add(0, 0, i * .045),
                        (.70 - i * .11) * pulse, 6, time * (i % 2 == 0 ? .08 : -.06), i == 0 ? .92F : .46F);
            }
            case "mage_armor" -> {
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 4.0 + i * Math.PI / 2.0 + time * .08;
                    Vec3 c = center.add(g.point(a, .72)).add(0, .75 + (i % 2) * .62, 0);
                    m.diamond(front, c, .28, -a, 1.10F, .20F);
                    m.runeGlyph(front, c, .13, seed + i * 31, a, .42F);
                }
            }
            case "mirror_image" -> {
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2.0 / 3.0 + time * .62;
                    Vec3 c = center.add(g.point(a, 1.15)).add(0, .85 + .18 * Math.sin(time * 1.4 + i), 0);
                    m.circle(front, c, .34, 24, .58F);
                    m.line(c.add(0, -.42, 0), c.add(0, .42, 0), .34F);
                }
            }
            case "invisibility", "greater_invisibility" -> {
                int rings = "greater_invisibility".equals(spell.id()) ? 4 : 2;
                for (int i = 0; i < rings; i++) {
                    double y = .35 + i * .38;
                    m.arc(g, center.add(0, y, 0), .72 + i * .13, time * (.45 - i * .09) + i,
                            Math.PI * 1.35, 30, i == 0 ? .66F : .38F);
                }
                if (rings > 2) m.circle(side, center.add(0, 1.0, 0), .78, 34, .38F);
            }
            case "blur" -> {
                for (int i = -2; i <= 2; i++) {
                    double x = i * .18 + Math.sin(time * 5.0 + i) * .08;
                    m.arc(front, center.add(x, 1.0, 0), .76, time * .8 + i, Math.PI * 1.18, 24,
                            i == 0 ? .70F : .30F);
                }
            }
            case "haste" -> {
                m.circle(g, center.add(0, .04, 0), 1.05, 54, .72F);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2.0 / 12.0 + time * .30;
                    m.line(center.add(g.point(a, .82)), center.add(g.point(a, 1.08)), i % 3 == 0 ? .68F : .32F);
                }
                m.helix(center.add(0, .05, 0), new Vec3(0, 1, 0), front, 1.65, .42, 2, 36, .42F, true);
            }
            case "protection_from_energy" -> {
                for (int i = 0; i < 5; i++) {
                    double a = i * Math.PI * 2.0 / 5.0 - time * .22;
                    Vec3 c = center.add(g.point(a, 1.02)).add(0, .95, 0);
                    m.diamond(front, c, .30, a + time * .08, 1.14F, .22F);
                }
                m.circle(g, center.add(0, .05, 0), .78, 40, .42F);
            }
            case "resilient_sphere", "globe_of_invulnerability" -> {
                double r = "globe_of_invulnerability".equals(spell.id()) ? 1.85 : 1.25;
                Vec3 c = center.add(0, 1.05, 0);
                m.sphere(c, r * pulse, 6, .62F);
                m.brokenBand(g, center.add(0, .04, 0), r * .92, r * 1.02, 58, 7, 1.05F, .12F);
            }
            case "stoneskin" -> {
                for (int i = 0; i < 7; i++) {
                    double a = i * Math.PI * 2.0 / 7.0 + time * .07;
                    Vec3 c = center.add(g.point(a, .72 + .12 * (i % 2))).add(0, .45 + .22 * (i % 4), 0);
                    m.polygon(front, c, .24 + .03 * (i % 3), 5, -a, i % 2 == 0 ? .66F : .38F);
                }
            }
            case "freedom_of_movement" -> {
                m.helix(center.add(0, .02, 0), new Vec3(0, 1, 0), front, 1.85, .68, 3, 42, .52F, true);
                m.arc(g, center.add(0, .05, 0), 1.12, time * .48, Math.PI * 1.55, 34, .72F);
            }
            case "true_seeing" -> {
                Vec3 eye = center.add(0, 1.65, 0);
                m.arc(front, eye, .92, Math.PI * .10, Math.PI * .80, 34, .72F);
                m.arc(front, eye, .92, Math.PI * 1.10, Math.PI * .80, 34, .72F);
                m.circle(front, eye, .28, 28, .78F);
                m.runeGlyph(front, eye, .16, seed, -time * .08, .42F);
                m.brokenBand(g, center.add(0, .05, 0), 1.25, 1.35, 54, 8, 1.02F, .10F);
            }
            case "fire_shield", "solar_guard" -> {
                int n = "solar_guard".equals(spell.id()) ? 8 : 6;
                double orbit = "solar_guard".equals(spell.id()) ? 1.22 : .92;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2.0 / n + time * .18;
                    Vec3 c = center.add(g.point(a, orbit)).add(0, .82 + .30 * Math.sin(a + time), 0);
                    m.star(front, c, .25, .10, 4, -a, i % 2 == 0 ? .74F : .42F);
                }
                if ("solar_guard".equals(spell.id())) m.circle(front, center.add(0, 1.05, 0), .70, 42, .62F);
            }
            case "shapechange" -> {
                for (int i = 0; i < 6; i++) {
                    double y = .18 + i * .30;
                    double rr = .48 + i * .10 + .08 * Math.sin(time * 1.2 + i);
                    m.polygon(g, center.add(0, y, 0), rr, 3 + (i + seed & 3),
                            time * (i % 2 == 0 ? .18 : -.14), i % 2 == 0 ? .66F : .38F);
                }
            }
            case "foresight" -> {
                Vec3 eye = center.add(0, 2.05, 0);
                m.circle(front, eye, .82, 52, .62F);
                m.runeGlyph(front, eye, .28, seed ^ 0xF012, -time * .04, .58F);
                m.circle(g, center.add(0, .05, 0), 1.32, 62, .52F);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2.0 / 12.0 - time * .08;
                    m.line(center.add(g.point(a, 1.12)), center.add(g.point(a, 1.34)), i % 3 == 0 ? .72F : .34F);
                }
                m.circle(side, center.add(0, 1.05, 0), .92, 42, .36F);
            }
            default -> auraMantle(m, spell, center, rise, time);
        }
    }
'''
body = overhaul.read_text(encoding='utf-8')
marker = '    private static void skyConvergence('
if marker not in body:
    raise SystemExit('buff mantle insertion marker missing')
body = body.replace(marker, buff_mantle + '\n' + marker, 1)
overhaul.write_text(body, encoding='utf-8')
sub_once(overhaul,
    r'    private static void highCircleAfterimage\(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 target,\n                                             double rise, double time\) \{.*?\n    \}\n\n    private static Vec3 flat',
'''    private static void highCircleAfterimage(ArcaneWorldMesh.Builder m, SpellDefinition spell, Vec3 target,
                                             double rise, double time) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        ArcaneWorldMesh.Basis x = ArcaneWorldMesh.Basis.facing(new Vec3(1, 0, 0));
        ArcaneWorldMesh.Basis z = ArcaneWorldMesh.Basis.facing(new Vec3(0, 0, 1));
        double r = (1.6 + spell.circle() * .28) * (.45 + .55 * rise);
        m.brokenBand(g, target.add(0, .04, 0), r * .82, r, 62, 6, 1.04F, .13F);
        if (spell.circle() >= 8) {
            Vec3 c = target.add(0, 1.1, 0);
            m.circle(x, c, r * .42, 42, .38F);
            m.circle(z, c, r * .55, 48, .44F);
        }
        if (spell.circle() >= 9) {
            for (int i = 0; i < 9; i++) {
                double a = i * Math.PI * 2.0 / 9.0 + time * .035;
                Vec3 c = target.add(g.point(a, r * .92)).add(0, .18 + (i % 3) * .26, 0);
                m.runeGlyph(g, c, r * .055, spell.id().hashCode() + i * 71, -time * .04, .36F);
            }
        }
    }

    private static Vec3 flat''', flags=re.S)

# Prismatic wall gameplay stays at the original 14s; only perceived lifetime was wrong.
# Version bump.
for path in [
    root / 'gradle.properties',
    root / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
    root / 'src/main/resources/data/arcanecircle/spell_catalog/index.json',
]:
    body = path.read_text(encoding='utf-8')
    if '0.12.1-alpha.36' not in body:
        raise SystemExit(f'alpha.36 version missing: {path}')
    path.write_text(body.replace('0.12.1-alpha.36', '0.12.1-alpha.37'), encoding='utf-8')

# Project contract documents the reason for the presentation change rather than just the numbers.
project = root / 'PROJECT.md'
body = project.read_text(encoding='utf-8')
append = r'''

## Alpha.37 presentation + buff identity contracts

- Prismatic Wall gameplay lifetime is 14 seconds (280 ticks). The alpha.36 30-second increase was reverted: the original problem was full-life visual fading, not server duration. Seven panels remain fully readable for the first 90% of life and only dissolve in the final 10%.
- 6C+ presentation complexity is structural, not radius-only. 7C adds a second ritual plane, 8C adds a three-dimensional gyroscope, and 9C adds nine satellite formulae plus displaced authority bands. Compact spells such as Power Word Kill may stay physically small while still reading as 9C.
- Portal/prison/high-circle release geometry may occupy multiple planes and anchors, but Gate's caster-side aperture still rises from the floor in front of the caster and must never center a giant frame on the caster body.
- Self buffs use `ArcaneBuffRuntime` for their defining mechanics. Vanilla effects are supplemental feedback only. Haste changes Arcane cast/cooldown timing; Shield/Mage Armor/Energy Protection/Solar Guard own charge mechanics; Invisibility/Greater Invisibility/Foresight own miss windows; Freedom and True Seeing are continuously enforced; Stoneskin/Shapechange own incoming-damage shaping.
- Persistent buff visuals are spell-authored silhouettes rather than one shared body halo: armor plates, mirror satellites, phase arcs, haste clock/helix, energy diamonds, stone facets, freedom spiral, true-sight eye, fire/solar crowns, shapechange morph rings and foresight eye-clock.
'''
if '## Alpha.37 presentation + buff identity contracts' not in body:
    body += append
project.write_text(body, encoding='utf-8')

# Source audit tracks the new contracts and the corrected 14-second wall.
audit = root / 'tools/test_current_source.py'
body = audit.read_text(encoding='utf-8')
body = body.replace('0.12.1-alpha.36', '0.12.1-alpha.37')
body = body.replace("'30초 지속'", "'14초 지속'")
body = body.replace("'case \"prismatic_wall\" -> 600'", "'case \"prismatic_wall\" -> 280'")
body = body.replace("assert 'case \"prismatic_wall\" -> 600;' in gameplay", "assert 'case \"prismatic_wall\" -> 280;' in gameplay")
body = body.replace("assert '30초 지속 7색 장벽' in summary", "assert '14초 지속 7색 장벽' in summary")
body = body.replace('# Alpha.36 presentation contract:', '# Alpha.37 presentation contract:')
marker = '# Active-tree hygiene: history is the archive.\n'
extra = r'''# Alpha.37 non-potion buff identity + 3D high-circle authority.
buff=text(magic/'ArcaneBuffRuntime.java')
for token in ['durationTicks','onIncomingDamage','castTimeMultiplier','adjustCooldownTicks',
              'protection_from_energy','greater_invisibility','freedom_of_movement','true_seeing',
              'solar_guard','shapechange','foresight','rechargeInterval','reveal']:
    assert token in buff, token
for token in ['ArcaneBuffRuntime.apply','ArcaneBuffRuntime.tick','ArcaneBuffRuntime.onIncomingDamage',
              'ArcaneBuffRuntime.clear','ArcaneBuffRuntime.clearAll']:
    assert token in gameplay, token
for token in ['ArcaneBuffRuntime.castTimeMultiplier(player)','ArcaneBuffRuntime.adjustCooldownTicks(player']:
    assert token in casting_service, token
for token in ['BUFFS = Set.of','buffMantle','highCircleCrown','Basis.fromNormal',
              'nine independent satellite formulae','spell.circle() >= 9','age < .90']:
    assert token in overhaul, token
assert 'ArcaneBuffRuntime.apply(player, "solar_guard", power, range)' in fusion
assert 'case "prismatic_wall" -> 280;' in gameplay
assert '14초 지속 7색 장벽' in summary

'''
if marker not in body:
    raise SystemExit('audit active-tree marker missing')
body = body.replace(marker, extra + marker, 1)
audit.write_text(body, encoding='utf-8')

print('alpha.37 presentation/buff migration applied')
