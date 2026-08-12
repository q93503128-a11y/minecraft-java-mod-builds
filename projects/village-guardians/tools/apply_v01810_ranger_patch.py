#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
ABILITY = JAVA / "VillageRoleAbilitySystem.java"
SKILLS = JAVA / "VillageRoleSkillSystem.java"
PROPS = ROOT / "gradle.properties"
SIEGE_TEST = ROOT / "tools/test_v0189_siege_phase2.py"
TRIGGER = ROOT / ".build-trigger-v01810"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    start_index = text.find(start)
    if start_index < 0:
        raise RuntimeError(f"{label}: start marker missing")
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise RuntimeError(f"{label}: end marker missing")
    return text[:start_index] + replacement + text[end_index:]


def patch_ability() -> None:
    text = ABILITY.read_text(encoding="utf-8")

    text = replace_once(
        text,
        "    private static final Map<UUID, TrackingArrowState> TRACKING_ARROWS = new HashMap<>();\n",
        "    private static final Map<UUID, TrackingArrowState> TRACKING_ARROWS = new HashMap<>();\n"
        "    private static final List<RicochetHop> RICOCHET_HOPS = new ArrayList<>();\n",
        "ricochet hop field")
    text = replace_once(
        text,
        "        TRACKING_ARROWS.clear();\n        ARROW_RAIN_READY.clear();\n",
        "        TRACKING_ARROWS.clear();\n        RICOCHET_HOPS.clear();\n        ARROW_RAIN_READY.clear();\n",
        "ricochet hop reset")
    text = replace_once(
        text,
        "        tickTrackingArrows(server, now);\n        tickScheduled(server, now);\n",
        "        tickTrackingArrows(server, now);\n        tickRicochetHops(server, now);\n        tickScheduled(server, now);\n",
        "ricochet hop tick")

    tracking_method = '''    private static void tickTrackingArrows(MinecraftServer server, long now) {
        ServerLevel level = server.overworld();
        Iterator<Map.Entry<UUID, TrackingArrowState>> iterator = TRACKING_ARROWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackingArrowState> entry = iterator.next();
            TrackingArrowState state = entry.getValue();
            Entity arrowEntity = level.getEntity(entry.getKey());
            if (now > state.until()
                    || !(arrowEntity instanceof AbstractArrow arrow)
                    || !arrow.isAlive()
                    || !(arrow.getOwner() instanceof ServerPlayer owner)
                    || !isRangerContext(owner)) {
                iterator.remove();
                continue;
            }

            Entity lockedEntity = level.getEntity(state.target());
            Mob target = lockedEntity instanceof Mob locked && locked.isAlive() ? locked : null;
            if (target == null) {
                target = bestFlightTarget(level, owner, arrow, 52.0);
                if (target == null) {
                    arrow.setNoGravity(false);
                    iterator.remove();
                    continue;
                }
                entry.setValue(new TrackingArrowState(target.getUUID(), state.until()));
            }

            Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
            Vec3 delta = body.subtract(arrow.position());
            if (delta.lengthSqr() < 0.05) continue;
            double currentSpeed = arrow.getDeltaMovement().length();
            double speed = Math.max(2.0, Math.min(3.6, currentSpeed));
            double leadTicks = Math.min(7.0, Math.sqrt(delta.lengthSqr()) / Math.max(0.1, speed));
            Vec3 predicted = body.add(target.getDeltaMovement().scale(leadTicks * 0.58));
            Vec3 guided = predicted.subtract(arrow.position());
            if (guided.lengthSqr() < 1.0E-5) continue;

            EmpoweredArrowState empowered = RICOCHET_ARROWS.get(entry.getKey());
            int specialRank = empowered == null ? 0 : empowered.specialRank();
            double turnStrength = Math.min(0.76, 0.46 + specialRank * 0.05);
            Vec3 current = arrow.getDeltaMovement().lengthSqr() < 1.0E-5
                    ? guided.normalize() : arrow.getDeltaMovement().normalize();
            Vec3 blended = current.scale(1.0 - turnStrength)
                    .add(guided.normalize().scale(turnStrength));
            if (blended.lengthSqr() < 1.0E-5) blended = guided.normalize();

            arrow.setNoGravity(true);
            arrow.setDeltaMovement(blended.normalize().scale(speed));
            arrow.hurtMarked = true;
            if (now % 3L == 0L) {
                VillageSkillEffectSystem.trackingReticle(
                        level, owner, body, body.subtract(arrow.position()));
            }
        }
    }

    private static void tickRicochetHops(MinecraftServer server, long now) {
        Iterator<RicochetHop> iterator = RICOCHET_HOPS.iterator();
        while (iterator.hasNext()) {
            RicochetHop hop = iterator.next();
            if (hop.executeAt() > now) continue;
            iterator.remove();
            ServerPlayer owner = server.getPlayerList().getPlayer(hop.owner());
            if (owner == null || !(owner.level() instanceof ServerLevel level)) continue;
            Entity entity = level.getEntity(hop.target());
            if (!(entity instanceof Mob target) || !target.isAlive()) continue;
            hurtByPlayer(level, owner, target, hop.damage());
            play(level, target.position(), SoundEvents.ARROW_HIT, 0.62f,
                    1.18f + Math.min(0.34f, hop.hopIndex() * 0.055f));
        }
    }

'''
    text = replace_between(
        text,
        "    private static void tickTrackingArrows(MinecraftServer server, long now) {\n",
        "    private static void tickScheduled(MinecraftServer server, long now) {\n",
        tracking_method,
        "tracking methods")

    old_chain = '''                int ricochetRank = ricochet == null ? 0 : ricochet.specialRank();
                List<Mob> chain = targetsNear(level, attacker, primary.position(),
                        areaRadius(12.0, ricochetRank), 12 + ricochetRank * 2);
                chain.remove(primary);
                chain.sort(Comparator.comparingDouble(primary::distanceToSqr));
                float damage = Math.max(2.0f, event.getAmount() * 0.72f);
                List<Mob> visualChain = new ArrayList<>();
                int maximumChain = 6 + Math.min(5, ricochetRank);
                for (int i = 0; i < Math.min(maximumChain, chain.size()); i++) {
                    Mob target = chain.get(i);
                    visualChain.add(target);
                    hurt(level, target, damage * (1.0f - i * 0.09f));
                    play(level, target.position(), SoundEvents.ARROW_HIT, 0.55f, 1.2f + i * 0.06f);
                }
                VillageSkillEffectSystem.ricochet(level, attacker, primary, visualChain);
'''
    new_chain = '''                int ricochetRank = ricochet == null ? 0 : ricochet.specialRank();
                float damage = Math.max(2.0f, event.getAmount() * 0.72f);
                queueRicochet(level, attacker, primary, damage, ricochetRank);
'''
    text = replace_once(text, old_chain, new_chain, "incoming ricochet chain")

    helper_marker = "    private static void aimAssist(\n"
    helpers = '''    private static void queueRicochet(
            ServerLevel level, ServerPlayer owner, Mob primary, float baseDamage, int specialRank) {
        double bounceRadius = areaRadius(10.5, specialRank);
        int maximumChain = 4 + Math.min(4, Math.max(0, specialRank));
        List<Mob> chain = buildRicochetChain(
                level, owner, primary, bounceRadius, maximumChain);
        if (chain.isEmpty()) return;

        VillageSkillEffectSystem.ricochet(level, owner, primary, chain);
        long now = level.getGameTime();
        for (int i = 0; i < chain.size(); i++) {
            Mob target = chain.get(i);
            float falloff = (float) Math.pow(0.86, i);
            RICOCHET_HOPS.add(new RicochetHop(
                    now + 2L + i * 2L,
                    owner.getUUID(), target.getUUID(),
                    Math.max(1.5f, baseDamage * falloff), i));
        }
    }

    private static List<Mob> buildRicochetChain(
            ServerLevel level, ServerPlayer owner, Mob primary,
            double bounceRadius, int maximumChain) {
        List<Mob> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        visited.add(primary.getUUID());
        Mob cursor = primary;
        for (int hop = 0; hop < maximumChain; hop++) {
            Mob from = cursor;
            Mob next = targetsNear(level, owner, from.position(), bounceRadius, 36).stream()
                    .filter(target -> !visited.contains(target.getUUID()))
                    .filter(from::hasLineOfSight)
                    .min(Comparator.comparingDouble(from::distanceToSqr))
                    .orElse(null);
            if (next == null) break;
            result.add(next);
            visited.add(next.getUUID());
            cursor = next;
        }
        return result;
    }

    private static Mob bestFlightTarget(
            ServerLevel level, ServerPlayer owner, AbstractArrow arrow, double range) {
        Vec3 velocity = arrow.getDeltaMovement();
        Vec3 forward = velocity.lengthSqr() < 1.0E-5
                ? lookDirection(owner) : velocity.normalize();
        Vec3 origin = arrow.position();
        return targetsNear(level, owner, origin, range, 64).stream()
                .filter(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    return to.lengthSqr() > 1.0E-5 && to.normalize().dot(forward) >= 0.30;
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    double alignment = to.normalize().dot(forward);
                    return to.lengthSqr() * (1.15 - Math.max(0.0, alignment));
                }))
                .orElse(null);
    }

'''
    text = replace_once(text, helper_marker, helpers + helper_marker, "ricochet helpers")

    old_best = '''    private static Mob bestAimTarget(
            ServerLevel level, ServerPlayer player, Vec3 origin, double range) {
        Vec3 look = lookDirection(player);
        return targetsNear(level, player, player.position(), range, 80).stream()
                .filter(player::hasLineOfSight)
                .filter(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    return body.subtract(origin).dot(look) > 0.20;
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    double forward = Math.max(0.0, to.dot(look));
                    Vec3 closest = origin.add(look.scale(forward));
                    double miss = body.distanceToSqr(closest);
                    return miss * 5.0 + to.lengthSqr() * 0.012;
                }))
                .orElse(null);
    }
'''
    new_best = '''    private static Mob bestAimTarget(
            ServerLevel level, ServerPlayer player, Vec3 origin, double range) {
        Vec3 look = lookDirection(player);
        return targetsNear(level, player, player.position(), range, 80).stream()
                .filter(player::hasLineOfSight)
                .filter(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    return to.lengthSqr() > 1.0E-5 && to.normalize().dot(look) >= 0.62;
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
                    Vec3 to = body.subtract(origin);
                    double forward = Math.max(0.0, to.dot(look));
                    Vec3 closest = origin.add(look.scale(forward));
                    double miss = body.distanceToSqr(closest);
                    return miss * 6.5 + to.lengthSqr() * 0.010;
                }))
                .orElse(null);
    }
'''
    text = replace_once(text, old_best, new_best, "aim cone")

    text = replace_once(
        text,
        "    private static void hurt(ServerLevel level, Mob target, float damage) {\n"
        "        target.hurtServer(level, level.damageSources().magic(), Math.max(0.1f, damage));\n"
        "    }\n",
        "    private static void hurt(ServerLevel level, Mob target, float damage) {\n"
        "        target.hurtServer(level, level.damageSources().magic(), Math.max(0.1f, damage));\n"
        "    }\n\n"
        "    private static void hurtByPlayer(\n"
        "            ServerLevel level, ServerPlayer owner, Mob target, float damage) {\n"
        "        target.hurtServer(level, level.damageSources().playerAttack(owner), Math.max(0.1f, damage));\n"
        "    }\n",
        "player-attributed ricochet damage")

    text = replace_once(
        text,
        "    private record TrackingArrowState(UUID target, long until) {}\n",
        "    private record RicochetHop(long executeAt, UUID owner, UUID target, float damage, int hopIndex) {}\n\n"
        "    private record TrackingArrowState(UUID target, long until) {}\n",
        "ricochet record")

    ABILITY.write_text(text, encoding="utf-8")


def patch_skills() -> None:
    text = SKILLS.read_text(encoding="utf-8")
    old = 'RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "추적 도탄", 7, 190, 22, "기술 사용 후 다음 실제 활·석궁 발사를 대기하며, 플레이어 바로 앞의 표식과 함께 그 한 발이 추적·연쇄 도탄 화살로 강화됩니다."),'
    new = 'RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "추적 도탄", 7, 190, 22, "기술 사용 후 다음 실제 활·석궁 한 발이 전방 표적을 추적합니다. 표적이 사라지면 비행 경로 전방의 새 적을 재포착하고, 적중 후에는 가까운 적을 중복 없이 순차 도탄합니다."),'
    text = replace_once(text, old, new, "tracking ricochet description")
    SKILLS.write_text(text, encoding="utf-8")


def patch_version_and_regression() -> None:
    props = PROPS.read_text(encoding="utf-8")
    props = replace_once(props, "mod_version=0.18.9-alpha.1", "mod_version=0.18.10-alpha.1", "version")
    PROPS.write_text(props, encoding="utf-8")

    siege = SIEGE_TEST.read_text(encoding="utf-8")
    siege = replace_once(siege,
                         'assert "mod_version=0.18.9-alpha.1" in props',
                         'assert "mod_version=0.18.10-alpha.1" in props',
                         "siege regression version")
    SIEGE_TEST.write_text(siege, encoding="utf-8")
    TRIGGER.write_text("Village Guardians v0.18.10-alpha.1 ranger ricochet acceptance — 2026-08-12\n", encoding="utf-8")


def main() -> None:
    patch_ability()
    patch_skills()
    patch_version_and_regression()
    print("[PASS] patched tracking reacquisition, smooth steering and sequential ricochet")
    print("[PASS] secondary ricochet hits now use player-attributed damage")
    print("[PASS] bumped Village Guardians to 0.18.10-alpha.1")


if __name__ == "__main__":
    main()
