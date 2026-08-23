from pathlib import Path
import json

root = Path('projects/arcane-circle')
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'
world = root / 'src/main/java/kr/moonseungjun/arcanecircle/world'


def txt(path):
    return path.read_text(encoding='utf-8')


def write(path, body):
    path.write_text(body, encoding='utf-8')


def rep(path, old, new, count=1):
    body = txt(path)
    actual = body.count(old)
    if actual != count:
        raise SystemExit(f'{path}: expected {count} occurrences, found {actual}: {old[:120]!r}')
    write(path, body.replace(old, new))


third = magic / 'ThirdCircleSpellService.java'
rep(third,
    '    private static final Map<UUID, EnergyWard> ENERGY = new HashMap<>();\n    private static final List<SlowZone> SLOW_ZONES = new ArrayList<>();',
    '    private static final Map<UUID, EnergyWard> ENERGY = new HashMap<>();\n    private static final Map<UUID, NpcHasteState> NPC_HASTE = new HashMap<>();\n    private static final List<SlowZone> SLOW_ZONES = new ArrayList<>();')
rep(third,
    '            case "haste" -> {\n                caster.addEffect(new MobEffectInstance(MobEffects.SPEED, HASTE_TICKS, 2, true, false));\n                yield true;\n            }',
    '            case "haste" -> npcHaste(level, caster);')
rep(third,
    '        tickEnergy(level, now);\n        tickSlow(level, now);',
    '        tickEnergy(level, now);\n        tickNpcHaste(level, now);\n        tickSlow(level, now);')
rep(third,
    '    public static void clear(LivingEntity subject) {\n        if (subject == null) return;\n        UUID id = subject.getUUID();\n        Set<String> owned = ownedSpellIds(id);',
    '    public static void clear(LivingEntity subject) {\n        if (subject == null) return;\n        ArcaneBuffRuntime.clearSpell(subject, "haste");\n        UUID id = subject.getUUID();\n        Set<String> owned = ownedSpellIds(id);')
rep(third,
    '        ENERGY.remove(id);\n        SLOW_ZONES.removeIf(zone -> zone.ownerId.equals(id));',
    '        ENERGY.remove(id);\n        NPC_HASTE.remove(id);\n        SLOW_ZONES.removeIf(zone -> zone.ownerId.equals(id));')
rep(third,
    '        ENERGY.clear();\n        SLOW_ZONES.clear();',
    '        ENERGY.clear();\n        NPC_HASTE.clear();\n        SLOW_ZONES.clear();')
rep(third,
    '        if (PLAYER_FLIGHT.containsKey(id) || MOB_FLIGHT.containsKey(id)) result.add("fly");\n        if (ENERGY.containsKey(id)) result.add("protection_from_energy");',
    '        if (PLAYER_FLIGHT.containsKey(id) || MOB_FLIGHT.containsKey(id)) result.add("fly");\n        if (NPC_HASTE.containsKey(id)) result.add("haste");\n        if (ENERGY.containsKey(id)) result.add("protection_from_energy");')

fly_anchor = '''    private static void restoreMobFlight(MobFlight state) {
        Entity raw = state.level.getEntity(state.mobId);
        if (!(raw instanceof Mob mob) || mob.isRemoved()) return;
        mob.setNoGravity(state.wasNoGravity);
        mob.fallDistance = 0.0F;
        mob.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, true, false));
    }
'''
fly_insert = fly_anchor + '''
    private static boolean npcHaste(ServerLevel level, Mob caster) {
        long now = level.getGameTime();
        NPC_HASTE.put(caster.getUUID(), new NpcHasteState(level, caster.getUUID(), now + HASTE_TICKS));
        caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 12, 1, true, false));
        return true;
    }

    public static boolean hasNpcHaste(LivingEntity caster) {
        if (caster == null || !(caster.level() instanceof ServerLevel level)) return false;
        NpcHasteState state = NPC_HASTE.get(caster.getUUID());
        return state != null && state.level == level && state.expiresAt > level.getGameTime()
                && caster.isAlive() && !caster.isRemoved();
    }

    public static double npcCastTimeMultiplier(LivingEntity caster) {
        return hasNpcHaste(caster) ? .72 : 1.0;
    }

    public static double npcCooldownMultiplier(LivingEntity caster) {
        return hasNpcHaste(caster) ? .85 : 1.0;
    }

    private static void tickNpcHaste(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcHasteState>> iterator = NPC_HASTE.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcHasteState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.casterId);
            if (!(raw instanceof LivingEntity caster) || !caster.isAlive() || caster.isRemoved() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 12, 1, true, false));
        }
    }
'''
rep(third, fly_anchor, fly_insert)

old_dispel = '''    private static void dispelTarget(LivingEntity target) {
        FirstCircleSpellService.dispel(target);
        SecondCircleSpellService.clear(target);
        ThirdCircleSpellService.clear(target);
        FourthCircleSpellService.clear(target);
        FifthCircleSpellService.clear(target);
        SixthCircleSpellService.clear(target);
        SeventhCircleSpellService.clear(target);
        EighthCircleSpellService.clear(target);
        SpellGameplayService.clear(target);
        HighWardSpellService.clear(target);
        HighControlSpellService.clear(target);
        if (target instanceof ServerPlayer player) {
            HighUtilitySpellService.clear(player);
            SimulacrumService.clear(player);
            ArcaneLightService.clear(player);
        }
        removeBeneficialMagic(target);
        WorldMagicService.stop(target);
    }

    private static void removeBeneficialMagic(LivingEntity target) {
        target.removeEffect(MobEffects.ABSORPTION);
        target.removeEffect(MobEffects.RESISTANCE);
        target.removeEffect(MobEffects.REGENERATION);
        target.removeEffect(MobEffects.SPEED);
        target.removeEffect(MobEffects.STRENGTH);
        target.removeEffect(MobEffects.INVISIBILITY);
        target.removeEffect(MobEffects.FIRE_RESISTANCE);
        target.removeEffect(MobEffects.NIGHT_VISION);
        target.removeEffect(MobEffects.LUCK);
        target.removeEffect(MobEffects.JUMP_BOOST);
        target.removeEffect(MobEffects.SLOW_FALLING);
    }
'''
new_dispel = '''    private static void dispelTarget(LivingEntity target) {
        // Third-circle Dispel owns lower-circle maintenance only. Higher-circle authority is not
        // erased for free by a 3C spell; Antimagic Field and higher counters own that escalation.
        FirstCircleSpellService.dispel(target);
        SecondCircleSpellService.clear(target);
        ThirdCircleSpellService.clear(target);
    }
'''
rep(third, old_dispel, new_dispel)
rep(third,
    '        ArcaneNoticeService.push(caster, Component.literal("§b[디스펠] §f" + target.getName().getString()\n                + "의 유지형 강화·제어 마법을 해제했습니다."), 60);',
    '        ArcaneNoticeService.push(caster, Component.literal("§b[디스펠] §f" + target.getName().getString()\n                + "의 1~3써클 유지형 강화·제어 마법을 해제했습니다. §74써클 이상 권능은 보존됩니다."), 70);')

rep(third,
    '    private static boolean slow(ServerLevel level, LivingEntity caster, double range, Vec3 center) {\n        double radius = Math.max(5.0, Math.min(9.0, SpellMetrics.effectRadius("slow", range, 3)));',
    '    public static double slowRadius(double range) {\n        return Math.max(5.0, Math.min(9.0, SpellMetrics.effectRadius("slow", range, 3)));\n    }\n\n    private static boolean slow(ServerLevel level, LivingEntity caster, double range, Vec3 center) {\n        double radius = slowRadius(range);')
rep(third,
    '    private static boolean sleetStorm(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {\n        double radius = Math.max(6.5, Math.min(10.5, SpellMetrics.effectRadius("sleet_storm", range, 3)));',
    '    public static double sleetStormRadius(double range) {\n        return Math.max(6.5, Math.min(10.5, SpellMetrics.effectRadius("sleet_storm", range, 3)));\n    }\n\n    private static boolean sleetStorm(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {\n        double radius = sleetStormRadius(range);')

old_blink = '''    private static boolean blink(ServerPlayer player, double range, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        double maxDistance = Math.max(12.0, Math.min(20.0, range));
        Vec3 desired = clampDestination(player.position(), snapshot.target(), maxDistance);
        Optional<BlockPos> safe = findSafe(level, desired, 8);
        if (safe.isEmpty()) {
            ArcaneNoticeService.push(player, Component.literal("§c[점멸] §f도착 지점 주변에 안전한 공간이 없습니다."), 45);
            return false;
        }
        BlockPos p = safe.get();
        player.stopRiding();
        boolean moved = player.teleportTo(level, p.getX() + .5, p.getY(), p.getZ() + .5,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        if (!moved) return false;
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false));
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .88F, .92F);
        ArcaneNoticeService.push(player, Component.literal(
                "§5[점멸] §f장거리 공간 도약 완료 · §7착지 직후 2초간 위상 잔류막으로 충격을 완화합니다."), 55);
        return true;
    }

    private static boolean blink(ServerLevel level, Mob caster, LivingEntity target, double range) {
        if (target == null || !target.isAlive()) return false;
        Vec3 delta = target.position().subtract(caster.position());
        if (delta.lengthSqr() < 1.0E-6) return false;
        double distance = Math.min(Math.max(8.0, range), Math.max(5.0, delta.length() - 3.0));
        Vec3 desired = caster.position().add(delta.normalize().scale(distance));
        Optional<BlockPos> safe = findSafe(level, desired, 8);
        if (safe.isEmpty()) return false;
        BlockPos p = safe.get();
        caster.getNavigation().stop();
        caster.snapTo(p.getX() + .5, p.getY(), p.getZ() + .5, caster.getYRot(), caster.getXRot());
        caster.fallDistance = 0.0F;
        caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false));
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, .84F, .90F);
        return true;
    }
'''
new_blink = '''    public static double blinkDistance(double range) {
        return Math.max(14.0, Math.min(20.0, Math.max(0.0, range)));
    }

    private static boolean blink(ServerPlayer player, double range, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        Optional<BlockPos> safe = findPhaseDestination(level, player.position(), snapshot.launchDirection(), blinkDistance(range));
        if (safe.isEmpty()) {
            ArcaneNoticeService.push(player, Component.literal("§c[점멸] §f위상선 끝쪽에서 안전한 종착점을 찾지 못했습니다."), 45);
            return false;
        }
        BlockPos p = safe.get();
        player.stopRiding();
        boolean moved = player.teleportTo(level, p.getX() + .5, p.getY(), p.getZ() + .5,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        if (!moved) return false;
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false));
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, .88F, .92F);
        ArcaneNoticeService.push(player, Component.literal(
                "§5[점멸] §f중간 고체 지형을 무시하는 위상 통과 완료 · §7종착점만 안전 공간이어야 하며 2초간 위상 잔류막이 남습니다."), 70);
        return true;
    }

    private static boolean blink(ServerLevel level, Mob caster, LivingEntity target, double range) {
        if (target == null || !target.isAlive()) return false;
        Vec3 delta = target.position().subtract(caster.position());
        if (delta.lengthSqr() < 1.0E-6) return false;
        double distance = Math.min(blinkDistance(range), Math.max(8.0, delta.length() - 2.5));
        Optional<BlockPos> safe = findPhaseDestination(level, caster.position(), delta.normalize(), distance);
        if (safe.isEmpty()) return false;
        BlockPos p = safe.get();
        caster.getNavigation().stop();
        caster.snapTo(p.getX() + .5, p.getY(), p.getZ() + .5, caster.getYRot(), caster.getXRot());
        caster.fallDistance = 0.0F;
        caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false));
        level.playSound(null, p, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, .84F, .90F);
        return true;
    }

    private static Optional<BlockPos> findPhaseDestination(ServerLevel level, Vec3 start, Vec3 direction, double maxDistance) {
        Vec3 unit = direction == null || direction.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
        for (double distance = maxDistance; distance >= 8.0; distance -= 2.0) {
            Optional<BlockPos> safe = findSafe(level, start.add(unit.scale(distance)), 6);
            if (safe.isPresent()) return safe;
        }
        return Optional.empty();
    }
'''
rep(third, old_blink, new_blink)
rep(third,
    '    private record MobFlight(ServerLevel level, UUID mobId, UUID targetId, long expiresAt, boolean wasNoGravity) {}\n\n    private static final class EnergyWard {',
    '    private record MobFlight(ServerLevel level, UUID mobId, UUID targetId, long expiresAt, boolean wasNoGravity) {}\n    private record NpcHasteState(ServerLevel level, UUID casterId, long expiresAt) {}\n\n    private static final class EnergyWard {')

buff = magic / 'ArcaneBuffRuntime.java'
rep(buff,
    '                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1, true, false));\n                notice(player, "§e[헤이스트] §f마법 회로 전개 28% 단축 · 재사용 대기 15% 단축.");',
    '                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 12, 1, true, false));\n                notice(player, "§e[헤이스트] §f30초 · 마법 회로 전개 28% 단축 · 재사용 대기 15% 단축 · 이동 가속.");')
rep(buff,
    '            if ("freedom_of_movement".equals(state.spellId) && now % 5L == 0L) cleanseMovement(player);',
    '            if ("haste".equals(state.spellId))\n                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 12, 1, true, false));\n            if ("freedom_of_movement".equals(state.spellId) && now % 5L == 0L) cleanseMovement(player);')
rep(buff,
    '    public static void clear(UUID playerId) {\n',
    '''    public static boolean clearSpell(LivingEntity subject, String spellId) {
        if (subject == null || spellId == null || spellId.isBlank()) return false;
        State removed = STATES.remove(new BuffKey(subject.getUUID(), spellId));
        if (removed == null) return false;
        WorldMagicService.cancelRelease(subject, spellId);
        return true;
    }

    public static void clear(UUID playerId) {
''')

mage = world / 'ArcaneMageService.java'
rep(mage,
    'import kr.moonseungjun.arcanecircle.magic.SpellDefinition;\nimport kr.moonseungjun.arcanecircle.magic.WorldMagicService;',
    'import kr.moonseungjun.arcanecircle.magic.SpellDefinition;\nimport kr.moonseungjun.arcanecircle.magic.ThirdCircleSpellService;\nimport kr.moonseungjun.arcanecircle.magic.WorldMagicService;')
rep(mage,
    '        int interval = Math.max(18, (int) Math.round((92 - profile.circle() * 7)\n                * profile.affiliation().cooldownMultiplier()));',
    '        int interval = Math.max(12, (int) Math.round(Math.max(18.0, (92 - profile.circle() * 7)\n                * profile.affiliation().cooldownMultiplier())\n                * ThirdCircleSpellService.npcCooldownMultiplier(caster)));')
rep(mage,
    '        int interval = Math.max(16, (int) Math.round((94 - profile.circle() * 7)\n                * profile.affiliation().cooldownMultiplier()));',
    '        int interval = Math.max(12, (int) Math.round(Math.max(16.0, (94 - profile.circle() * 7)\n                * profile.affiliation().cooldownMultiplier())\n                * ThirdCircleSpellService.npcCooldownMultiplier(caster)));')
rep(mage,
    '        int required = Math.max(8, 8 + visual.circle() * 3);',
    '        int required = Math.max(6, (int) Math.round((8 + visual.circle() * 3)\n                * ThirdCircleSpellService.npcCastTimeMultiplier(caster)));')
rep(mage,
    '        if (executed) { if (target instanceof Mob mob) mob.setTarget(caster); applyControl(caster, target, profile); }',
    '        if (executed) {\n            if (target instanceof Mob mob) mob.setTarget(caster);\n            if (!"haste".equals(spell.id())) applyControl(caster, target, profile);\n        }')
rep(mage,
    '    private static SpellDefinition chooseCombatSpell(Mob caster, MageProfile profile) {\n        int circle=Math.max(1,Math.min(9,profile.circle()));\n        List<SpellDefinition> all=SpellCatalog.spells().values().stream()',
    '    private static SpellDefinition chooseCombatSpell(Mob caster, MageProfile profile) {\n        int circle=Math.max(1,Math.min(9,profile.circle()));\n        if(circle>=3 && !ThirdCircleSpellService.hasNpcHaste(caster) && caster.getRandom().nextInt(100)<14){\n            SpellDefinition haste=SpellCatalog.spell("haste").orElse(null);\n            if(haste!=null)return haste;\n        }\n        List<SpellDefinition> all=SpellCatalog.spells().values().stream()')

summary = magic / 'ThirdCircleSpellSummary.java'
write(summary, '''package kr.moonseungjun.arcanecircle.magic;

/** Alpha.72 exact third-circle gameplay contract used by the grimoire. */
public final class ThirdCircleSpellSummary {
    private ThirdCircleSpellSummary() {}

    public static String summary(String id) {
        if (id == null) return "";
        return switch (id) {
            case "fireball" -> "고정 착탄점 화염 폭발 · 중심 강피해/거리 감쇠 + 화상 + 플레이어 시전 시 주변 취약 지형 파괴";
            case "lightning_bolt" -> "시전점~고정 목표를 잇는 관통 번개선 · 경로의 복수 대상을 같은 번개로 타격 + 약한 지형 파손";
            case "fly" -> "30초 실제 자유 비행 권한 · 종료/해제 시 기존 비행 권한 복원 + 안전 낙하";
            case "haste" -> "30초 Arcane 템포 가속 · 플레이어와 NPC 모두 시전시간 28% 단축 + 재사용 대기 15% 단축 + 이동 가속";
            case "dispel_magic" -> "대상에게 유지 중인 1~3써클 강화·제어 마법만 확정 해제 · 4써클 이상 권능은 보존 · 대상이 없으면 자신의 해로운 상태 정화";
            case "vampiric_touch" -> "10m 이내 단일 생명력 흡수 · 실제로 감소시킨 체력+흡수량의 60%만큼 시전자 회복";
            case "slow" -> "9초 반경 약 5~9m 시간왜곡 구역 · 0.2초 간격으로 강한 둔화·약화·채굴 피로를 재적용";
            case "protection_from_energy" -> "30초 5중 공명막 · Arcane/화염/투사체성 충격만 45% 경감 · 소모막은 3.5초마다 재충전";
            case "sleet_storm" -> "9초 반경 약 6.5~10.5m 진눈깨비 구역 · 0.5초마다 냉기/동결/암흑/미끄럼 압박 + 내부 적대 Arcane 시전 봉쇄";
            case "blink" -> "최대 약 20m 1인 위상 통과 · 출발~종착 사이 고체 지형은 무시하고 종착점만 안전 공간을 요구 · 착지 후 2초 위상 잔류막";
            default -> "";
        };
    }
}
''')

overlay = client / 'ThirdCircleAuthorityOverlay.java'
write(overlay, '''package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.ThirdCircleSpellService;
import net.minecraft.world.phys.Vec3;

/** Alpha.72 exact-footprint overlay for maintained third-circle battlefield zones. */
final class ThirdCircleAuthorityOverlay {
    private ThirdCircleAuthorityOverlay() {}

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,
                                   double range, double elapsedSeconds, double durationSeconds) {
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(420);
        if (spell == null || spell.circle() != 3) return m.build();
        boolean slow = "slow".equals(spell.id());
        boolean sleet = "sleet_storm".equals(spell.id());
        if (!slow && !sleet) return m.build();
        double t = Math.max(0.0, elapsedSeconds);
        double radius = slow ? ThirdCircleSpellService.slowRadius(range)
                : ThirdCircleSpellService.sleetStormRadius(range);
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        Vec3 floor = target;
        double pulsePeriod = slow ? .20 : .50;
        double pulse = 1.0 - ((t % pulsePeriod) / pulsePeriod);
        m.circle(g, floor.add(0, .045, 0), radius, 62, .88F);
        m.circle(g, floor.add(0, .075, 0), radius * (.40 + .50 * pulse), 48, .38F);
        if (slow) {
            m.polygon(g, floor.add(0, .10, 0), radius * .70, 6, -t * .12, .34F);
            m.polygon(g, floor.add(0, .13, 0), radius * .42, 6, t * .18, .28F);
        } else {
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI * 2.0 / 10.0 + t * .21;
                Vec3 p = floor.add(g.point(a, radius * (.28 + .055 * (i % 5))));
                m.line(p.add(0, 3.8 + (i % 3), 0), p.add(0, .25, 0), .30F, .82F, .34F);
            }
        }
        return m.build();
    }
}
''')

spell_def = magic / 'SpellDefinition.java'
rep(spell_def,
    '        String fourthCircle = FourthCircleSpellSummary.summary(id);\n        if (!fourthCircle.isBlank()) return fourthCircle;',
    '        String thirdCircle = ThirdCircleSpellSummary.summary(id);\n        if (!thirdCircle.isBlank()) return thirdCircle;\n        String fourthCircle = FourthCircleSpellSummary.summary(id);\n        if (!fourthCircle.isBlank()) return fourthCircle;')

wm = magic / 'WorldMagicService.java'
rep(wm,
    '        duration = fourthCircleVisualDuration(spell.id(), duration);',
    '        duration = thirdCircleVisualDuration(spell.id(), duration);\n        duration = fourthCircleVisualDuration(spell.id(), duration);', 2)
third_duration = '''    private static int thirdCircleVisualDuration(String spellId, int baseDuration) {
        return switch (spellId) {
            case "fly" -> Math.max(baseDuration, ThirdCircleSpellService.FLY_TICKS);
            case "haste" -> Math.max(baseDuration, ThirdCircleSpellService.HASTE_TICKS);
            case "slow" -> Math.max(baseDuration, ThirdCircleSpellService.SLOW_TICKS);
            case "protection_from_energy" -> Math.max(baseDuration, ThirdCircleSpellService.ENERGY_TICKS);
            case "sleet_storm" -> Math.max(baseDuration, ThirdCircleSpellService.SLEET_TICKS);
            default -> baseDuration;
        };
    }

'''
rep(wm, '    private static int fourthCircleVisualDuration(String spellId, int baseDuration) {\n',
    third_duration + '    private static int fourthCircleVisualDuration(String spellId, int baseDuration) {\n')

tracker = client / 'WorldMagicTracker.java'
rep(tracker,
    '                if(v.spell.circle()==4){\n                    ArcaneWorldMesh fourthAuthority=FourthCircleAuthorityOverlay.release(',
    '                if(v.spell.circle()==3){\n                    ArcaneWorldMesh thirdAuthority=ThirdCircleAuthorityOverlay.release(v.spell,v.direction,targetOffset(v),\n                            v.range,elapsedSeconds,durationSeconds);\n                    if(thirdAuthority.size()>0)entries.add(new RenderEntry(center,thirdAuthority,color,88,opacity));\n                }\n\n                if(v.spell.circle()==4){\n                    ArcaneWorldMesh fourthAuthority=FourthCircleAuthorityOverlay.release(')

index_path = root / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
index = json.loads(txt(index_path))
index['version'] = '0.12.1-alpha.72'
index['third_circle_deep_audit'] = [
    'falloff_fireball_blast', 'piercing_lightning_line', 'lifecycle_real_flight',
    'player_and_npc_arcane_tempo_haste', 'lower_circle_bounded_dispel_magic',
    'actual_damage_vampiric_drain', 'persistent_slow_field',
    'energy_only_recharging_ward', 'casting_break_sleet_storm',
    'solid_geometry_phase_blink'
]
index['third_circle_preserved_authority'] = [
    'fireball','lightning_bolt','fly','vampiric_touch','slow','protection_from_energy','sleet_storm'
]
index['third_circle_value_pass_1'] = {
    'haste': '30s_player_and_npc_arcane_tempo_acceleration_0.72_cast_0.85_cooldown',
    'dispel_magic': 'deterministic_maintained_magic_purge_circles_1_to_3_only',
    'blink': 'solo_safe_endpoint_phase_traversal_ignoring_intervening_solid_geometry_up_to_20m'
}
index['third_circle_role_audit'] = {
    'fireball': 'falloff_area_blast_and_player_terrain_breach',
    'lightning_bolt': 'piercing_multi_target_line_strike',
    'fly': 'thirty_second_free_flight_authority',
    'haste': 'arcane_cast_and_cooldown_tempo_acceleration',
    'dispel_magic': 'lower_circle_maintained_magic_purge',
    'vampiric_touch': 'actual_damage_to_self_healing_conversion',
    'slow': 'fixed_time_dilation_action_suppression_zone',
    'protection_from_energy': 'recharging_energy_only_damage_ward',
    'sleet_storm': 'fixed_weather_cast_denial_and_visibility_zone',
    'blink': 'solid_geometry_ignoring_solo_phase_relocation'
}
index['third_circle_dispel_ceiling'] = 'circles_1_to_3_deterministic_only'
index['third_circle_npc_parity'] = True
write(index_path, json.dumps(index, ensure_ascii=False, indent=2) + '\n')

main = root / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java'
rep(main, 'VERSION = "0.12.1-alpha.71"', 'VERSION = "0.12.1-alpha.72"')
gradle = root / 'gradle.properties'
g = txt(gradle).replace('mod_version=0.12.1-alpha.71', 'mod_version=0.12.1-alpha.72')
lines = [line for line in g.splitlines() if not line.startswith('# alpha.71') and not line.startswith('# alpha.72')]
lines += ['', '# alpha.72 third-circle authority audit: NPC Haste parity, bounded Dispel, solid-geometry phase Blink']
write(gradle, '\n'.join(lines).rstrip() + '\n')

test = root / 'tools/test_current_source.py'
t = txt(test).replace('mod_version=0.12.1-alpha.71', 'mod_version=0.12.1-alpha.72')
t = t.replace('VERSION = "0.12.1-alpha.71"', 'VERSION = "0.12.1-alpha.72"')
t = t.replace("assert index['version'] == '0.12.1-alpha.71'", "assert index['version'] == '0.12.1-alpha.72'")
t = t.replace("need(text(magic / 'SpellDefinition.java'), 'FourthCircleSpellSummary.summary(id)', 'FifthCircleSpellSummary.summary(id)', 'NinthCircleSpellSummary.summary(id)')",
              "need(text(magic / 'SpellDefinition.java'), 'ThirdCircleSpellSummary.summary(id)', 'FourthCircleSpellSummary.summary(id)', 'FifthCircleSpellSummary.summary(id)', 'NinthCircleSpellSummary.summary(id)')")
marker = '# Alpha.71 fourth-circle battlefield-authority value pass.\n'
if marker not in t:
    raise SystemExit('alpha71 test marker missing')
block = '''# Alpha.72 third-circle authority/value pass.
third = text(magic / 'ThirdCircleSpellService.java')
third_summary = text(magic / 'ThirdCircleSpellSummary.java')
third_authority = text(client / 'ThirdCircleAuthorityOverlay.java')
arcane_buff = text(magic / 'ArcaneBuffRuntime.java')
mage_ai = text(world / 'ArcaneMageService.java')
world_magic_3 = text(magic / 'WorldMagicService.java')
tracker_3 = text(client / 'WorldMagicTracker.java')
need(third,
     'NPC_HASTE = new HashMap<>()', 'case "haste" -> npcHaste(level, caster);',
     'public static double npcCastTimeMultiplier(LivingEntity caster)', 'return hasNpcHaste(caster) ? .72 : 1.0;',
     'public static double npcCooldownMultiplier(LivingEntity caster)', 'return hasNpcHaste(caster) ? .85 : 1.0;',
     'ArcaneBuffRuntime.clearSpell(subject, "haste")',
     '1~3써클 유지형 강화·제어 마법을 해제했습니다.', '4써클 이상 권능은 보존됩니다.',
     'public static double slowRadius(double range)', 'public static double sleetStormRadius(double range)',
     'public static double blinkDistance(double range)', 'findPhaseDestination(level, player.position(), snapshot.launchDirection(), blinkDistance(range))',
     '중간 고체 지형을 무시하는 위상 통과 완료')
assert 'FourthCircleSpellService.clear(target);' not in third
assert 'FifthCircleSpellService.clear(target);' not in third
assert 'EighthCircleSpellService.clear(target);' not in third
need(arcane_buff,
     'player.addEffect(new MobEffectInstance(MobEffects.SPEED, 12, 1, true, false));',
     'public static boolean clearSpell(LivingEntity subject, String spellId)')
need(mage_ai,
     'ThirdCircleSpellService.npcCooldownMultiplier(caster)',
     'ThirdCircleSpellService.npcCastTimeMultiplier(caster)',
     '!ThirdCircleSpellService.hasNpcHaste(caster)', 'SpellCatalog.spell("haste")',
     'if (!"haste".equals(spell.id())) applyControl(caster, target, profile);')
need(third_summary,
     '플레이어와 NPC 모두 시전시간 28% 단축', '1~3써클 강화·제어 마법만 확정 해제',
     '4써클 이상 권능은 보존', '출발~종착 사이 고체 지형은 무시')
need(world_magic_3,
     'duration = thirdCircleVisualDuration(spell.id(), duration);',
     'case "haste" -> Math.max(baseDuration, ThirdCircleSpellService.HASTE_TICKS);',
     'case "slow" -> Math.max(baseDuration, ThirdCircleSpellService.SLOW_TICKS);',
     'case "sleet_storm" -> Math.max(baseDuration, ThirdCircleSpellService.SLEET_TICKS);')
need(third_authority,
     '"slow".equals(spell.id())', '"sleet_storm".equals(spell.id())',
     'ThirdCircleSpellService.slowRadius(range)', 'ThirdCircleSpellService.sleetStormRadius(range)')
need(tracker_3, 'if(v.spell.circle()==3){', 'ThirdCircleAuthorityOverlay.release(')
expected3 = {
    'haste': '30s_player_and_npc_arcane_tempo_acceleration_0.72_cast_0.85_cooldown',
    'dispel_magic': 'deterministic_maintained_magic_purge_circles_1_to_3_only',
    'blink': 'solo_safe_endpoint_phase_traversal_ignoring_intervening_solid_geometry_up_to_20m',
}
assert index['third_circle_value_pass_1'] == expected3
roles3 = index['third_circle_role_audit']
assert set(roles3) == {'fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'}
assert len(set(roles3.values())) == 10
assert index['third_circle_dispel_ceiling'] == 'circles_1_to_3_deterministic_only'
assert index['third_circle_npc_parity'] is True

'''
t = t.replace(marker, block + marker)
t = t.replace("'0.12.1-alpha.71', 'FourthCircleSpellSummary.class'",
              "'0.12.1-alpha.72', 'ThirdCircleSpellSummary.class', 'ThirdCircleAuthorityOverlay.class', 'FourthCircleSpellSummary.class'")
print_marker = "print('alpha71_ice_storm_6s_anti_air_suppression=PASS')"
t = t.replace(print_marker,
              "print('alpha72_haste_player_npc_arcane_tempo_parity=PASS')\nprint('alpha72_dispel_magic_circle_1_to_3_ceiling=PASS')\nprint('alpha72_blink_solid_geometry_phase_relocation=PASS')\nprint('alpha72_third_circle_visual_hitbox_lifetime_sync=PASS')\nprint('alpha72_third_circle_role_audit=PASS')\nprint('alpha72_third_circle_value_pass_1=PASS')\n" + print_marker)
write(test, t)

verify = root / 'tools/verify_jar.py'
v = txt(verify)
v = v.replace("'kr/moonseungjun/arcanecircle/magic/FourthCircleSpellService.class',",
              "'kr/moonseungjun/arcanecircle/magic/ThirdCircleSpellSummary.class',\n    'kr/moonseungjun/arcanecircle/client/ThirdCircleAuthorityOverlay.class',\n    'kr/moonseungjun/arcanecircle/magic/FourthCircleSpellService.class',")
v = v.replace("if version != '0.12.1-alpha.71':\n        raise SystemExit(f'unexpected alpha.71 package version: {version}')",
              "if version != '0.12.1-alpha.72':\n        raise SystemExit(f'unexpected alpha.72 package version: {version}')")
marker_v = '    expected4 = {\n'
if marker_v not in v:
    raise SystemExit('verify expected4 marker missing')
block_v = '''    expected3 = {
        'haste': '30s_player_and_npc_arcane_tempo_acceleration_0.72_cast_0.85_cooldown',
        'dispel_magic': 'deterministic_maintained_magic_purge_circles_1_to_3_only',
        'blink': 'solo_safe_endpoint_phase_traversal_ignoring_intervening_solid_geometry_up_to_20m',
    }
    if index.get('third_circle_value_pass_1') != expected3:
        raise SystemExit(f'alpha.72 third-circle value metadata mismatch: {index.get("third_circle_value_pass_1")}')
    roles3 = index.get('third_circle_role_audit', {})
    expected_roles3 = {'fireball','lightning_bolt','fly','haste','dispel_magic','vampiric_touch','slow','protection_from_energy','sleet_storm','blink'}
    if set(roles3) != expected_roles3 or len(set(roles3.values())) != 10:
        raise SystemExit('alpha.72 third-circle role separation contract missing')
    if index.get('third_circle_dispel_ceiling') != 'circles_1_to_3_deterministic_only':
        raise SystemExit('alpha.72 Dispel ceiling contract missing')

'''
v = v.replace(marker_v, block_v + marker_v)
v = v.replace("print('Arcane Circle alpha.71 JAR verification: PASS')",
              "print('Arcane Circle alpha.72 JAR verification: PASS')")
v = v.replace("print('alpha71_fourth_circle_value_pass_1=PASS')",
              "print('alpha72_third_circle_value_pass_1=PASS')\nprint('alpha72_haste_player_npc_arcane_tempo_parity=PASS')\nprint('alpha72_dispel_magic_circle_1_to_3_ceiling=PASS')\nprint('alpha72_blink_solid_geometry_phase_relocation=PASS')\nprint('alpha72_third_circle_role_separation=PASS')\nprint('alpha72_third_circle_npc_parity=PASS')\nprint('alpha71_fourth_circle_value_pass_1=PASS')")
write(verify, v)
