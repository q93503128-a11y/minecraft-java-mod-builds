#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match in {path.name}, got {count}: {old[:220]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def all_existing(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    once(ROOT / "gradle.properties", "mod_version=0.18.14-alpha.1", "mod_version=0.18.15-alpha.1")
    for test in (ROOT / "tools").glob("test_*.py"):
        all_existing(test, "mod_version=0.18.14-alpha.1", "mod_version=0.18.15-alpha.1")

    aspect = JAVA / "VillageBossAspectSystem.java"
    once(aspect,
'''    private static final Map<UUID, Aspect> ACTIVE = new HashMap<>();
    private static final Map<UUID, Vec3> STORM_WARNINGS = new HashMap<>();''',
'''    private static final Map<UUID, Aspect> ACTIVE = new HashMap<>();
    private static final Map<UUID, Vec3> STORM_WARNINGS = new HashMap<>();
    private static final Map<UUID, Vec3> BLOOD_WARNINGS = new HashMap<>();''')
    once(aspect,
'''    public static void reset() { ACTIVE.clear(); STORM_WARNINGS.clear(); }''',
'''    public static void reset() { ACTIVE.clear(); STORM_WARNINGS.clear(); BLOOD_WARNINGS.clear(); }''')
    once(aspect,
'''        ACTIVE.remove(id);
        STORM_WARNINGS.remove(id);''',
'''        ACTIVE.remove(id);
        STORM_WARNINGS.remove(id);
        BLOOD_WARNINGS.remove(id);''')
    once(aspect,
'''    public static Aspect preview(int day, int wave, int bossIndex) {''',
'''    public static Aspect aspectOf(Mob mob) {
        return mob == null ? null : ACTIVE.get(mob.getUUID());
    }

    public static Aspect preview(int day, int wave, int bossIndex) {''')
    once(aspect,
'''            case BLOODBOUND -> {
                if (globalTicks % 100 == 85) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            mob.getX(), mob.getY() + 1.0, mob.getZ(), 18, 1.0, 0.7, 1.0, 0.03);
                    return;
                }
                if (globalTicks % 100 != 0) return;
                float healed = 0.0f;
                for (ServerPlayer player : nearbyPlayers(server, mob, 11.0)) {
                    player.hurtServer(level, level.damageSources().magic(), 3.5f + VillageCouncilState.currentDay() * 0.16f);
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
                    healed += 4.0f;
                }
                if (healed > 0.0f) mob.heal(Math.min(22.0f, healed));
            }''',
'''            case BLOODBOUND -> {
                if (globalTicks % 100 == 85) {
                    Vec3 center = mob.position();
                    BLOOD_WARNINGS.put(mob.getUUID(), center);
                    VillageBossEffectSystem.bloodboundWarning(level, center, 11.0, 15);
                    return;
                }
                if (globalTicks % 100 != 0) return;
                Vec3 center = BLOOD_WARNINGS.remove(mob.getUUID());
                if (center == null) return;
                float healed = 0.0f;
                for (ServerPlayer player : nearbyPlayersAt(server, level, center, 11.0)) {
                    player.hurtServer(level, level.damageSources().magic(), 3.5f + VillageCouncilState.currentDay() * 0.16f);
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
                    healed += 4.0f;
                }
                if (healed > 0.0f) mob.heal(Math.min(22.0f, healed));
                VillageBossEffectSystem.bloodboundImpact(level, center, 11.0);
            }''')
    once(aspect,
'''                        STORM_WARNINGS.put(mob.getUUID(), warningPos);
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                warningPos.x, warningPos.y + 0.15, warningPos.z,
                                24, 0.9, 0.08, 0.9, 0.025);''',
'''                        STORM_WARNINGS.put(mob.getUUID(), warningPos);
                        VillageBossEffectSystem.stormWarning(level, warningPos, 2.4, 15);''')
    once(aspect,
'''    private static java.util.List<ServerPlayer> nearbyPlayers(MinecraftServer server, Mob mob, double radius) {
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == mob.level() && player.isAlive()
                        && !player.isSpectator() && !VillageRespawnSystem.isDowned(player)
                        && player.distanceToSqr(mob) <= squared)
                .toList();
    }''',
'''    private static java.util.List<ServerPlayer> nearbyPlayers(MinecraftServer server, Mob mob, double radius) {
        if (!(mob.level() instanceof ServerLevel level)) return java.util.List.of();
        return nearbyPlayersAt(server, level, mob.position(), radius);
    }

    private static java.util.List<ServerPlayer> nearbyPlayersAt(
            MinecraftServer server, ServerLevel level, Vec3 center, double radius) {
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == level && player.isAlive()
                        && !player.isSpectator() && !VillageRespawnSystem.isDowned(player)
                        && player.position().distanceToSqr(center) <= squared)
                .toList();
    }''')

    boss = JAVA / "VillageSiegeBossSystem.java"
    once(boss, "import net.minecraft.world.phys.AABB;\n", "import net.minecraft.world.phys.AABB;\nimport net.minecraft.world.phys.Vec3;\n")
    once(boss,
'''    private static final Map<UUID, BossDoctrine> ACTIVE = new HashMap<>();
    private static final HashSet<UUID> PHASE_TWO = new HashSet<>();''',
'''    private static final Map<UUID, BossDoctrine> ACTIVE = new HashMap<>();
    private static final HashSet<UUID> PHASE_TWO = new HashSet<>();
    private static final Map<UUID, BreachCast> BREACH_CASTS = new HashMap<>();
    private static final Map<UUID, RitualCast> RITUAL_CASTS = new HashMap<>();
    private static final Map<UUID, DuelCast> DUEL_CASTS = new HashMap<>();''')
    once(boss,
'''    public static void reset() { ACTIVE.clear(); PHASE_TWO.clear(); ticks = 0; }''',
'''    public static void reset() {
        ACTIVE.clear();
        PHASE_TWO.clear();
        BREACH_CASTS.clear();
        RITUAL_CASTS.clear();
        DUEL_CASTS.clear();
        ticks = 0;
    }''')
    once(boss,
'''            if (!(entity instanceof Mob mob) || !mob.isAlive()) { ACTIVE.remove(id); PHASE_TWO.remove(id); continue; }''',
'''            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                ACTIVE.remove(id);
                PHASE_TWO.remove(id);
                BREACH_CASTS.remove(id);
                RITUAL_CASTS.remove(id);
                DUEL_CASTS.remove(id);
                continue;
            }''')
    once(boss,
'''    public static String previewBossMechanic(int day) {
        BossDoctrine doctrine = BossDoctrine.values()[Math.floorMod(day, BossDoctrine.values().length)];
        return doctrine.displayName() + " · " + doctrine.description();
    }''',
'''    public static String previewBossMechanic(int day) {
        return "혼성 보스 교리 · 파성 거신 / 사령 결속자 / 검은 결투원수";
    }

    private static BossDoctrine doctrineFor(
            int day, int wave, VillageEnemyArchetypeSystem.Archetype type) {
        int salt = type == null ? 0 : type.ordinal();
        BossDoctrine[] values = BossDoctrine.values();
        return values[Math.floorMod(day * 5 + wave * 3 + salt, values.length)];
    }''')
    once(boss,
'''            BossDoctrine doctrine = BossDoctrine.values()[Math.floorMod(VillageCouncilState.currentDay(), BossDoctrine.values().length)];
            ACTIVE.put(mob.getUUID(), doctrine);''',
'''            BossDoctrine doctrine = doctrineFor(VillageCouncilState.currentDay(), VillageRaidSystem.waveOf(mob), type);
            ACTIVE.put(mob.getUUID(), doctrine);''')
    once(boss,
'''            if (doctrine == BossDoctrine.BREACH_COLOSSUS) {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60 * 30, 1));
            } else if (doctrine == BossDoctrine.BLACK_MARSHAL) {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 30, 1));
            }
        }''',
'''            if (doctrine == BossDoctrine.BREACH_COLOSSUS) {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60 * 30, 1));
            } else if (doctrine == BossDoctrine.BLACK_MARSHAL) {
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 30, 1));
            }
            VillageBossAspectSystem.Aspect aspect = VillageBossAspectSystem.aspectOf(mob);
            if (aspect != null) VillageBossEffectSystem.presence(level, mob, aspect, doctrine);
        }''')
    once(boss,
'''        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    mob.getX(), mob.getY() + 1.2, mob.getZ(), 42, 1.6, 1.0, 1.6, 0.08);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    mob.getX(), mob.getY() + 0.8, mob.getZ(), 5, 0.8, 0.4, 0.8, 0.02);
        }''',
'''        if (mob.level() instanceof ServerLevel level) {
            VillageBossEffectSystem.phaseTwo(level, mob, doctrine);
        }''')
    once(boss,
'''            if (!VillageSiegeSegmentSystem.touching(segment, mob.blockPosition())) return;
            boolean phaseTwo = PHASE_TWO.contains(mob.getUUID());
            int interval = phaseTwo ? 30 : 45;
            int offset = Math.floorMod(mob.getUUID().hashCode(), interval);
            int phase = Math.floorMod(ticks - offset, interval);
            ServerLevel level = server.overworld();
            if (phase == interval - 10) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,
                        18, 1.2, 0.5, 1.2, 0.03);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        mob.getX(), mob.getY() + 1.1, mob.getZ(), 14, 0.6, 0.6, 0.6, 0.03);
            }
            if (phase == 0) {
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                int damage = phaseTwo ? 72 : 48;
                VillageSiegeSegmentSystem.damage(server, segment, damage, mob.blockPosition());
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5,
                        5, 1.0, 0.5, 1.0, 0.02);
            }''',
'''            boolean touching = VillageSiegeSegmentSystem.touching(segment, mob.blockPosition());
            boolean phaseTwo = PHASE_TWO.contains(mob.getUUID());
            int interval = phaseTwo ? 30 : 45;
            int offset = Math.floorMod(mob.getUUID().hashCode(), interval);
            int phase = Math.floorMod(ticks - offset, interval);
            ServerLevel level = server.overworld();
            if (phase == interval - 10 && touching) {
                BreachCast cast = new BreachCast(segment, target.immutable(), ticks + 10);
                BREACH_CASTS.put(mob.getUUID(), cast);
                VillageBossEffectSystem.breachWarning(level, mob, Vec3.atCenterOf(cast.impact()), 10);
            }
            if (phase == 0) {
                BreachCast cast = BREACH_CASTS.remove(mob.getUUID());
                if (!touching || cast == null || cast.dueTick() > ticks) return;
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                int damage = phaseTwo ? 72 : 48;
                VillageSiegeSegmentSystem.damage(server, cast.segment(), damage, cast.impact());
                VillageBossEffectSystem.breachImpact(level, Vec3.atCenterOf(cast.impact()), 3.4);
            }''')
    once(boss,
'''    private static void tickRitual(ServerLevel level, Mob boss) {
        int offset = Math.floorMod(boss.getUUID().hashCode(), 120);
        int phase = Math.floorMod(ticks - offset, 120);
        if (phase == 100) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    boss.getX(), boss.getY() + 1.3, boss.getZ(), 30, 2.4, 1.0, 2.4, 0.04);
            return;
        }
        if (phase != 0) return;
        for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, boss.position(), 15.0, 20, boss.getUUID())) {
            ally.heal(PHASE_TWO.contains(boss.getUUID()) ? 10.0f : 6.0f);
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100,
                    PHASE_TWO.contains(boss.getUUID()) ? 2 : 1));
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                boss.getX(), boss.getY() + 1.2, boss.getZ(), 30, 2.0, 0.8, 2.0, 0.04);
    }''',
'''    private static void tickRitual(ServerLevel level, Mob boss) {
        int offset = Math.floorMod(boss.getUUID().hashCode(), 120);
        int phase = Math.floorMod(ticks - offset, 120);
        if (phase == 100) {
            Vec3 center = boss.position();
            RITUAL_CASTS.put(boss.getUUID(), new RitualCast(center, ticks + 20));
            VillageBossEffectSystem.ritualWarning(level, center, 15.0, 20);
            return;
        }
        if (phase != 0) return;
        RitualCast cast = RITUAL_CASTS.remove(boss.getUUID());
        if (cast == null || cast.dueTick() > ticks) return;
        for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, cast.center(), 15.0, 20, boss.getUUID())) {
            ally.heal(PHASE_TWO.contains(boss.getUUID()) ? 10.0f : 6.0f);
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100,
                    PHASE_TWO.contains(boss.getUUID()) ? 2 : 1));
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0));
        }
        VillageBossEffectSystem.ritualImpact(level, cast.center(), 15.0);
    }''')
    once(boss,
'''    private static void tickDuel(MinecraftServer server, Mob boss) {
        if (ticks % 35 != 0) return;
        ServerPlayer target = server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == boss.level() && player.isAlive() && !player.isSpectator()
                        && !VillageRespawnSystem.isDowned(player) && player.distanceToSqr(boss) <= 42.0 * 42.0)
                .min(java.util.Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        if (target == null) return;
        boss.setTarget(target);
        boss.getNavigation().moveTo(target, PHASE_TWO.contains(boss.getUUID()) ? 1.58 : 1.34);
        ServerLevel level = server.overworld();
        if (ticks % 105 == 70) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.6, 0.8, 0.6, 0.02);
        }
        if (ticks % 105 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 1));
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    target.getX(), target.getY() + 0.8, target.getZ(), 16, 0.5, 0.5, 0.5, 0.04);
        }
    }''',
'''    private static void tickDuel(MinecraftServer server, Mob boss) {
        if (ticks % 35 != 0) return;
        ServerPlayer target = server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == boss.level() && player.isAlive() && !player.isSpectator()
                        && !VillageRespawnSystem.isDowned(player) && player.distanceToSqr(boss) <= 42.0 * 42.0)
                .min(java.util.Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        if (target != null) {
            boss.setTarget(target);
            boss.getNavigation().moveTo(target, PHASE_TWO.contains(boss.getUUID()) ? 1.58 : 1.34);
        }
        ServerLevel level = server.overworld();
        if (ticks % 105 == 70 && target != null) {
            DUEL_CASTS.put(boss.getUUID(), new DuelCast(target.getUUID(), ticks + 35));
            VillageBossEffectSystem.duelMark(level, target, 35);
        }
        if (ticks % 105 == 0) {
            DuelCast cast = DUEL_CASTS.remove(boss.getUUID());
            if (cast == null || cast.dueTick() > ticks) return;
            var marked = level.getEntity(cast.target());
            if (!(marked instanceof ServerPlayer victim) || !victim.isAlive() || victim.isSpectator()
                    || VillageRespawnSystem.isDowned(victim) || victim.distanceToSqr(boss) > 48.0 * 48.0) return;
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 1));
            VillageBossEffectSystem.duelImpact(level, victim);
        }
    }''')
    once(boss,
'''    public enum BossDoctrine {''',
'''    private record BreachCast(VillageSiegeSegmentSystem.Segment segment, BlockPos impact, int dueTick) {}
    private record RitualCast(Vec3 center, int dueTick) {}
    private record DuelCast(UUID target, int dueTick) {}

    public enum BossDoctrine {''')

    effect = JAVA / "VillageSkillEffectEntity.java"
    once(effect,
'''    private boolean followsOwner() {
        if (kind().startsWith("elite_aura_")) return true;''',
'''    private boolean followsOwner() {
        if (kind().startsWith("elite_aura_") || kind().startsWith("boss_presence_")
                || kind().startsWith("boss_phase_two_") || "boss_duel_mark".equals(kind())) return true;''')
    once(effect,
'''    private boolean tracksOwnerLook() {
        return switch (kind()) {''',
'''    private boolean tracksOwnerLook() {
        if (kind().startsWith("boss_presence_") || kind().startsWith("boss_phase_two_")) return true;
        return switch (kind()) {''')

    mesh = JAVA / "VillageSkillMeshLibrary.java"
    once(mesh,
'''        if (state.kind.startsWith("turret_wreck_")) {
            renderTurretWreck(pose, out, basis, age, state.kind, state.extra);
            return;
        }

        switch (state.kind) {''',
'''        if (state.kind.startsWith("turret_wreck_")) {
            renderTurretWreck(pose, out, basis, age, state.kind, state.extra);
            return;
        }
        if (state.kind.startsWith("boss_presence_")) {
            renderBossPresence(pose, out, basis, age, state.kind, state.extra, false);
            return;
        }
        if (state.kind.startsWith("boss_phase_two_")) {
            renderBossPresence(pose, out, basis, age, state.kind, state.extra, true);
            return;
        }

        switch (state.kind) {''')
    once(mesh,
'''            case "elite_plague_impact" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 2);
            default -> renderFallbackRune(pose, out, basis, age, progress);''',
'''            case "elite_plague_impact" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 2);

            case "boss_phase_two_burst" -> renderBossZone(pose, out, basis, age, progress, state.extra, 0);
            case "boss_breach_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 1);
            case "boss_breach_windup" -> renderPath(pose, out, state, age, progress, 0xFFB35E, true);
            case "boss_breach_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 2);
            case "boss_ritual_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 3);
            case "boss_ritual_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 4);
            case "boss_duel_mark" -> renderBossDuelMark(pose, out, basis, age, progress);
            case "boss_duel_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 5);
            case "boss_bloodbound_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 6);
            case "boss_bloodbound_impact" -> renderBossZone(pose, out, basis, age, progress, state.extra, 7);
            case "boss_storm_warning" -> renderBossZone(pose, out, basis, age, progress, state.extra, 8);
            default -> renderFallbackRune(pose, out, basis, age, progress);''')
    once(mesh,
'''    private static void renderFallbackRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''',
'''    private static void renderBossPresence(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age,
            String kind, String aspectName, boolean phaseTwo) {
        int aspect = switch (aspectName == null ? "" : aspectName) {
            case "berserker" -> 0;
            case "bulwark" -> 1;
            case "bloodbound" -> 2;
            case "stormcaller" -> 3;
            case "warleader" -> 4;
            case "wallbreaker" -> 5;
            default -> -1;
        };
        int color = switch (aspect) {
            case 0 -> rgba(255, 72, 52, phaseTwo ? 225 : 165);
            case 1 -> rgba(242, 207, 112, phaseTwo ? 220 : 160);
            case 2 -> rgba(210, 49, 86, phaseTwo ? 225 : 165);
            case 3 -> rgba(96, 191, 255, phaseTwo ? 230 : 170);
            case 4 -> rgba(255, 153, 61, phaseTwo ? 225 : 165);
            case 5 -> rgba(194, 172, 143, phaseTwo ? 225 : 165);
            default -> rgba(199, 102, 255, phaseTwo ? 220 : 155);
        };
        double amp = phaseTwo ? 1.22 : 1.0;
        double pulse = 0.94 + 0.06 * Math.sin(age * (phaseTwo ? 0.22 : 0.13));
        ring(pose, out, b, 1.28 * amp * pulse, 0.08, 0.07, 72, color, age * 0.018);
        ring(pose, out, b, 0.94 * amp, 2.15, 0.055, 56, withAlpha(color, 125), -age * 0.026);

        if (kind.contains("breach_colossus")) {
            for (int side : new int[]{-1, 1}) {
                spike(pose, out, b.local(side * 0.74, 0.18, 0.0),
                        b.local(side * 1.22, 1.44 * amp, 0.18), 0.11 * amp, color);
            }
            prism(pose, out, b.local(-0.72, 0.15, 0.68), b.local(0.72, 0.15, 0.68), 0.10, withAlpha(color, 150));
        } else if (kind.contains("bone_hierophant")) {
            for (int i = 0; i < 4; i++) {
                double a = age * 0.012 + i * TAU / 4.0;
                Vec3 base = b.local(Math.cos(a) * 0.88, 0.25, Math.sin(a) * 0.88);
                crystal(pose, out, base, 1.10 * amp, 0.16, color);
            }
        } else {
            for (int side : new int[]{-1, 1}) {
                prism(pose, out, b.local(side * 0.62, 0.56, -0.22),
                        b.local(-side * 0.18, 2.16 * amp, 0.78), 0.065, color);
            }
            slashArc(pose, out, b, age * 0.018, 1.10 * amp, 1.12, 0.96, 0.055, withAlpha(color, 145));
        }

        if (phaseTwo) {
            jaggedBolt(pose, out, b.local(-0.9, 0.18, 0.0), b.local(0.9, 2.55, 0.12),
                    8, 0.035, withAlpha(color, 190), (long) age / 3L + 551L);
        }
    }

    private static void renderBossDuelMark(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        int color = rgba(255, 76, 83, (int) (205 * (1.0 - progress * 0.55)));
        double radius = 0.82 + 0.08 * Math.sin(age * 0.25);
        ring(pose, out, b, radius, 0.05, 0.07, 48, color, age * 0.04);
        for (int side : new int[]{-1, 1}) {
            prism(pose, out, b.local(side * 0.45, 0.28, -0.22),
                    b.local(-side * 0.22, 1.90, 0.28), 0.045, color);
        }
    }

    private static void renderBossZone(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress,
            String encodedRadius, int style) {
        double radius = 4.0;
        try { radius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        radius = Math.max(0.8, radius);
        int base = switch (style) {
            case 1, 2 -> rgba(255, 157, 67, 220);
            case 3, 4 -> rgba(128, 220, 255, 205);
            case 5 -> rgba(255, 72, 82, 225);
            case 6, 7 -> rgba(207, 48, 86, 220);
            case 8 -> rgba(103, 198, 255, 220);
            default -> rgba(255, 94, 67, 220);
        };
        boolean warning = style == 1 || style == 3 || style == 6 || style == 8;
        double visible = warning ? radius : 0.35 + radius * Math.min(1.0, progress * 2.2);
        int alpha = warning
                ? (int) (115 + 65 * (0.5 + 0.5 * Math.sin(age * 0.28)))
                : (int) (220 * (1.0 - progress * 0.74));
        int color = withAlpha(base, alpha);
        ring(pose, out, b, visible, 0.055, warning ? 0.11 : 0.20, 80, color, age * 0.014);
        ring(pose, out, b, visible * 0.72, 0.06, 0.045, 60, withAlpha(color, Math.max(30, alpha - 55)), -age * 0.022);
        if (warning) {
            for (int i = 0; i < 8; i++) {
                double a = i * TAU / 8.0 + age * 0.004;
                chevron(pose, out, b, a, radius * 0.90, 0.06, 0.46, withAlpha(color, Math.max(45, alpha - 20)));
            }
        } else {
            sphere(pose, out, Vec3.ZERO, Math.max(0.28, visible * 0.20), 8, 12, withAlpha(color, 70));
        }
    }

    private static void renderFallbackRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''')

    print("[PASS] applied v0.18.15 boss identity and cast-state patch")


if __name__ == "__main__":
    main()
