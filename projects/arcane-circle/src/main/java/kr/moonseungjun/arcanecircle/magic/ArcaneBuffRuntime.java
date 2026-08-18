package kr.moonseungjun.arcanecircle.magic;

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
            WorldMagicService.cancelRelease(player, "invisibility");
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
            if (armor.charges > 0) {
                armor.charges--;
                if (armor.nextChargeAt <= now) armor.nextChargeAt = now + rechargeInterval("mage_armor");
            }
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
            if (solar.nextChargeAt <= now) solar.nextChargeAt = now + rechargeInterval("solar_guard");
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
        Iterator<Map.Entry<BuffKey, State>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BuffKey, State> entry = iterator.next();
            if (!entry.getKey().playerId().equals(playerId)) continue;
            State state = entry.getValue();
            Entity raw = state.level.getEntity(playerId);
            if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, state.spellId);
            iterator.remove();
        }
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
