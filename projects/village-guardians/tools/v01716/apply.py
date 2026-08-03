#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one marker, found {count}")
    return text.replace(old, new, 1)


def update(path: Path, transforms) -> None:
    text = path.read_text(encoding="utf-8")
    for old, new, label in transforms:
        text = replace_once(text, old, new, label)
    path.write_text(text, encoding="utf-8")


# Version
props = ROOT / "gradle.properties"
update(props, [
    ("mod_version=0.17.15-alpha.1", "mod_version=0.17.16-alpha.1", "version"),
])

ability = JAVA / "VillageRoleAbilitySystem.java"
update(ability, [
    (
        "    private static final Map<UUID, Long> RICOCHET_ARROWS = new HashMap<>();\n"
        "    private static final Map<UUID, EmpoweredArrowState> MEGA_ARROW_READY = new HashMap<>();",
        "    private static final Map<UUID, Long> RICOCHET_ARROWS = new HashMap<>();\n"
        "    private static final Map<UUID, EmpoweredArrowState> ARROW_RAIN_READY = new HashMap<>();\n"
        "    private static final Map<UUID, EmpoweredArrowState> MEGA_ARROW_READY = new HashMap<>();",
        "arrow rain ready state",
    ),
    (
        "        RICOCHET_ARROWS.clear();\n        MEGA_ARROW_READY.clear();",
        "        RICOCHET_ARROWS.clear();\n        ARROW_RAIN_READY.clear();\n        MEGA_ARROW_READY.clear();",
        "reset ranger readies",
    ),
    (
'''            case RANGER_VOLLEY -> {
                int rapidDuration = Math.max(200, duration);
                RAPID_UNTIL.put(player.getUUID(), now + rapidDuration);
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, rapidDuration, 1, false, false, true));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.35f);
            }
            case RANGER_PIERCE -> {
                RICOCHET_UNTIL.put(player.getUUID(), now + Math.max(240, duration * 2L));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), 1.0f, 1.2f);
            }
            case RANGER_RICOCHET -> {
                Vec3 center = aimedGround(level, player, 22.0);
                int fieldDuration = 60;
                VillageSkillEffectSystem.arrowRainField(level, player, center, fieldDuration, 8.5);
                for (int i = 0; i < 10; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 5L + i * 5L, player.getUUID(), skill,
                            ActionKind.ARROW_RAIN, power, durationMultiplier, specialRank, center, Vec3.ZERO));
                }
                play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.75f);
            }
            case RANGER_FIRE_RAIN -> {
                long until = now + Math.max(280L, duration * 2L);
                MEGA_ARROW_READY.put(player.getUUID(), new EmpoweredArrowState(until, power, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.62f);
            }
''',
'''            case RANGER_VOLLEY -> {
                clearRangerReadies(player.getUUID());
                long until = now + Math.max(240L, duration * 2L);
                RAPID_UNTIL.put(player.getUUID(), until);
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 1.35f);
            }
            case RANGER_PIERCE -> {
                clearRangerReadies(player.getUUID());
                RICOCHET_UNTIL.put(player.getUUID(), now + Math.max(240L, duration * 2L));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), 1.0f, 1.2f);
            }
            case RANGER_RICOCHET -> {
                clearRangerReadies(player.getUUID());
                long until = now + Math.max(260L, duration * 2L);
                ARROW_RAIN_READY.put(player.getUUID(),
                        new EmpoweredArrowState(until, power, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 1.0f, 0.92f);
            }
            case RANGER_FIRE_RAIN -> {
                clearRangerReadies(player.getUUID());
                long until = now + Math.max(280L, duration * 2L);
                MEGA_ARROW_READY.put(player.getUUID(), new EmpoweredArrowState(until, power, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
                play(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.62f);
            }
''',
        "queue all ranger skills",
    ),
    (
'''            if (RICOCHET_UNTIL.getOrDefault(id, 0L) >= now && now % 3L == 0L) {
                Vec3 sight = lookDirection(player);
                VillageSkillEffectSystem.trackingReticle(
                        level, player, aimPoint(level, player, 42.0), sight);
            }
''',
'''            if (RICOCHET_UNTIL.getOrDefault(id, 0L) >= now && now % 4L == 0L) {
                Vec3 sight = lookDirection(player);
                Vec3 readyPoint = player.getEyePosition().add(sight.scale(1.45));
                VillageSkillEffectSystem.trackingReticle(level, player, readyPoint, sight);
            }
''',
        "tracking cue in front",
    ),
    (
        "        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue() < now);\n"
        "        MEGA_ARROW_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);",
        "        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue() < now);\n"
        "        ARROW_RAIN_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);\n"
        "        MEGA_ARROW_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);",
        "cleanup rain ready",
    ),
    (
'''        if (!(event.getEntity() instanceof ServerPlayer player)
                || VillageCouncilState.roleOf(player.getUUID()).orElse(null) != VillageRole.RANGER) return;
''',
'''        if (!(event.getEntity() instanceof ServerPlayer player) || !isRangerContext(player)) return;
''',
        "arrow loose test ranger",
    ),
    (
'''    public static void handleEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)
                || VillageCouncilState.roleOf(player.getUUID()).orElse(null) != VillageRole.RANGER) return;
        if (spawningGeneratedArrow) return;
        long now = level.getGameTime();

        EmpoweredArrowState mega = MEGA_ARROW_READY.get(player.getUUID());
        if (mega != null && mega.until() >= now) {
            MEGA_ARROW_READY.remove(player.getUUID());
            event.setCanceled(true);
            launchEnergyArrow(level, player, mega.power(), mega.specialRank());
            return;
        }

        boolean tracking = RICOCHET_UNTIL.getOrDefault(player.getUUID(), 0L) >= now;
        if (tracking) {
            RICOCHET_UNTIL.remove(player.getUUID());
            RICOCHET_ARROWS.put(arrow.getUUID(), now + 240L);
            aimAssist(level, player, arrow, 0.68);
            play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 0.85f, 1.3f);
        } else {
            aimAssist(level, player, arrow, 0.24);
        }

        if (RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) < now) return;
        spawningGeneratedArrow = true;
        try {
            spawnSideArrow(level, player, arrow, -8.0);
            spawnSideArrow(level, player, arrow, 8.0);
        } finally {
            spawningGeneratedArrow = false;
        }
    }
''',
'''    public static void handleEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)
                || !isRangerContext(player)) return;
        if (spawningGeneratedArrow) return;
        long now = level.getGameTime();
        UUID id = player.getUUID();

        EmpoweredArrowState mega = MEGA_ARROW_READY.remove(id);
        if (mega != null && mega.until() >= now) {
            event.setCanceled(true);
            launchEnergyArrow(level, player, mega.power(), mega.specialRank());
            return;
        }

        EmpoweredArrowState rain = ARROW_RAIN_READY.remove(id);
        if (rain != null && rain.until() >= now) {
            activateArrowRain(level, player, rain.power(), rain.specialRank());
        }

        Long trackingUntil = RICOCHET_UNTIL.remove(id);
        boolean tracking = trackingUntil != null && trackingUntil >= now;
        if (tracking) {
            RICOCHET_ARROWS.put(arrow.getUUID(), now + 240L);
            aimAssist(level, player, arrow, 0.68);
            play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 0.85f, 1.3f);
        } else {
            aimAssist(level, player, arrow, 0.24);
        }

        Long rapidUntil = RAPID_UNTIL.remove(id);
        if (rapidUntil == null || rapidUntil < now) return;
        spawningGeneratedArrow = true;
        try {
            spawnSideArrow(level, player, arrow, -8.0);
            spawnSideArrow(level, player, arrow, 8.0);
        } finally {
            spawningGeneratedArrow = false;
        }
    }
''',
        "activate ranger skills on arrow join",
    ),
    (
        "            VillageRole role = VillageCouncilState.roleOf(attacker.getUUID()).orElse(null);",
        "            VillageRole role = activeRole(attacker);",
        "combat role in test mode",
    ),
    (
'''        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || VillageCouncilState.roleOf(killer.getUUID()).orElse(null) != VillageRole.RANGER
''',
'''        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)
                || !isRangerContext(killer)
''',
        "death ranger context",
    ),
    (
'''        appendTimed(states, "§b신속 삼연사", RAPID_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        if (RICOCHET_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) {
            states.add("§e추적 도탄 §f다음 화살");
        }
        EmpoweredArrowState mega = MEGA_ARROW_READY.get(player.getUUID());
        if (mega != null && mega.until() >= now) {
            states.add("§a성멸 대궁 §f다음 화살");
        }
''',
'''        appendReady(states, "§b신속 삼연사", RAPID_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        appendReady(states, "§e추적 도탄", RICOCHET_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        EmpoweredArrowState rain = ARROW_RAIN_READY.get(player.getUUID());
        if (rain != null) appendReady(states, "§9천공 화살비", rain.until(), now);
        EmpoweredArrowState mega = MEGA_ARROW_READY.get(player.getUUID());
        if (mega != null) appendReady(states, "§a성멸 대궁", mega.until(), now);
''',
        "ranger ready hud",
    ),
    (
'''    private static void appendTimed(List<String> output, String label, long until, long now) {
        if (until < now) return;
        output.add(label + " §f" + String.format(java.util.Locale.ROOT, "%.1f초", (until - now) / 20.0));
    }
''',
'''    private static void appendReady(List<String> output, String label, long until, long now) {
        if (until < now) return;
        output.add(label + " §f다음 활 · "
                + String.format(java.util.Locale.ROOT, "%.1f초", (until - now) / 20.0));
    }

    private static void appendTimed(List<String> output, String label, long until, long now) {
        if (until < now) return;
        output.add(label + " §f" + String.format(java.util.Locale.ROOT, "%.1f초", (until - now) / 20.0));
    }
''',
        "ready hud helper",
    ),
    (
'''        launchMovingAt(level, player, MovingKind.BLADE, ItemStack.EMPTY,
                1.75, 24, (5.4f + VillageCouncilState.levelOf(player.getUUID()) * 0.30f) * power,
                1.45, specialRank, player.getEyePosition().add(direction.scale(1.25)), direction);
''',
'''        Vec3 origin = player.position().add(0.0, 0.82, 0.0).add(direction.scale(1.0));
        launchMovingAt(level, player, MovingKind.BLADE, ItemStack.EMPTY,
                1.75, 24, (5.4f + VillageCouncilState.levelOf(player.getUUID()) * 0.30f) * power,
                1.45, specialRank, origin, direction);
''',
        "lower blade wave hit carrier",
    ),
    (
'''        for (int i = 0; i < 7; i++) {
            Vec3 random = randomPointInCircle(level, center, 8.2).add(0.0, 11.0 + level.getRandom().nextDouble() * 3.0, 0.0);
            spawnFallingArrow(level, player, random);
        }
''',
'''        // Falling arrows are rendered by one short-lived synchronized mesh field.
        // No persistent Arrow entities are created, preventing stuck arrows and weapon validation crashes.
''',
        "remove falling arrow entities",
    ),
    (
'''    private static void spawnFallingArrow(ServerLevel level, ServerPlayer owner, Vec3 position) {
        Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), owner.getMainHandItem());
        arrow.setPos(position.x, position.y, position.z);
        arrow.setDeltaMovement(0.0, -2.4, 0.0);
        arrow.setInvisible(true);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        spawningGeneratedArrow = true;
        try { level.addFreshEntity(arrow); }
        finally { spawningGeneratedArrow = false; }
    }

''',
        "",
        "delete unsafe falling arrow constructor",
    ),
    (
        "        Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), owner.getMainHandItem());\n"
        "        arrow.setPos(source.getX(), source.getY(), source.getZ());",
        "        Arrow arrow = new Arrow(level, owner, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));\n"
        "        arrow.setPos(source.getX(), source.getY(), source.getZ());",
        "safe generated side arrow weapon",
    ),
    (
'''    private static Vec3 aimedGround(ServerLevel level, ServerPlayer player, double distance) {
        Vec3 point = aimPoint(level, player, distance);
        return new Vec3(point.x, player.getY(), point.z);
    }
''',
'''    private static Vec3 aimedGround(ServerLevel level, ServerPlayer player, double distance) {
        Vec3 point = aimPoint(level, player, distance);
        Vec3 start = point.add(0.0, 12.0, 0.0);
        Vec3 end = point.add(0.0, -48.0, 0.0);
        var ground = level.clip(new net.minecraft.world.level.ClipContext(
                start, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        if (ground.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return new Vec3(point.x, player.getY() + 0.02, point.z);
        }
        return ground.getLocation().add(0.0, 0.02, 0.0);
    }
''',
        "ground anchored targeting",
    ),
    (
'''    private static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double distance) {
''',
'''    private static void activateArrowRain(
            ServerLevel level, ServerPlayer player, float power, int specialRank) {
        long now = level.getGameTime();
        Vec3 center = aimedGround(level, player, 24.0);
        int fieldDuration = 42;
        VillageSkillEffectSystem.arrowRainField(level, player, center, fieldDuration, 8.5);
        for (int i = 0; i < 8; i++) {
            SCHEDULED.add(new ScheduledAction(now + 2L + i * 4L, player.getUUID(),
                    VillageRoleSkillSystem.ActiveSkill.RANGER_RICOCHET,
                    ActionKind.ARROW_RAIN, power, 1.0f, specialRank, center, Vec3.ZERO));
        }
        play(level, player.position(), SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.75f);
    }

    private static void clearRangerReadies(UUID id) {
        RAPID_UNTIL.remove(id);
        RICOCHET_UNTIL.remove(id);
        ARROW_RAIN_READY.remove(id);
        MEGA_ARROW_READY.remove(id);
    }

    private static VillageRole activeRole(ServerPlayer player) {
        if (VillageSkillTestSystem.isEnabled(player)) {
            return VillageSkillTestSystem.selectedRole(player);
        }
        return VillageCouncilState.roleOf(player.getUUID()).orElse(null);
    }

    private static boolean isRangerContext(ServerPlayer player) {
        return activeRole(player) == VillageRole.RANGER;
    }

    private static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double distance) {
''',
        "ranger activation helpers",
    ),
])

# Skill descriptions now match the actual queued-shot behavior.
role_skills = JAVA / "VillageRoleSkillSystem.java"
update(role_skills, [
    (
        'RANGER_VOLLEY("ranger_volley", VillageRole.RANGER, 0, "신속 삼연사", 2, 70, 16, "일정 시간 활 충전이 크게 빨라지고 발사한 화살이 세 갈래로 분열합니다. 남은 시간이 기술 HUD에 표시됩니다."),',
        'RANGER_VOLLEY("ranger_volley", VillageRole.RANGER, 0, "신속 삼연사", 2, 70, 16, "기술 사용 후 다음 실제 활·석궁 발사를 대기하며, 그 한 발이 즉시 세 갈래 화살로 강화됩니다."),',
        "rapid description",
    ),
    (
        'RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "추적 도탄", 7, 190, 22, "조준 표식이 바라보는 위치를 따라다니며, 다음에 실제로 발사한 화살이 추적·연쇄 도탄 화살로 강화됩니다."),',
        'RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "추적 도탄", 7, 190, 22, "기술 사용 후 다음 실제 활·석궁 발사를 대기하며, 플레이어 바로 앞의 표식과 함께 그 한 발이 추적·연쇄 도탄 화살로 강화됩니다."),',
        "tracking description",
    ),
    (
        'RANGER_RICOCHET("ranger_ricochet", VillageRole.RANGER, 2, "천공 화살비", 13, 380, 30, "조준한 넓은 지역에 실제 화살이 여러 차례 떨어져 지속 광역 피해를 줍니다."),',
        'RANGER_RICOCHET("ranger_ricochet", VillageRole.RANGER, 2, "천공 화살비", 13, 380, 30, "기술 사용 후 다음 실제 활·석궁 발사 시 조준한 바닥에 짧고 강한 화살비가 펼쳐져 지속 광역 피해를 줍니다."),',
        "rain description",
    ),
    (
        'RANGER_FIRE_RAIN("ranger_fire_rain", VillageRole.RANGER, 3, "성멸 대궁", 21, 680, 40, "다음에 실제로 발사하는 화살을 밝은 초록색 초대형 성멸 화살로 바꾸어 넓은 전방을 관통합니다."),',
        'RANGER_FIRE_RAIN("ranger_fire_rain", VillageRole.RANGER, 3, "성멸 대궁", 21, 680, 40, "기술 사용 후 다음 실제 활·석궁 발사를 밝은 초록색 초대형 성멸 화살로 바꾸어 넓은 전방을 관통합니다."),',
        "mega description",
    ),
])

# Effect locations and short-lived readiness/rain actors.
effects = JAVA / "VillageSkillEffectSystem.java"
update(effects, [
    (
'''            case RANGER_VOLLEY -> spawn(level, player, "ranger_rapid",
                    player.position(), forward, Math.max(200, calculatedDuration), 0.0f, "");
            case RANGER_PIERCE -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 28, 0.0f, "");
            case RANGER_RICOCHET -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 20, 0.0f, "");
            case RANGER_FIRE_RAIN -> spawn(level, player, "ranger_energy_charge",
                    player.position(), forward, Math.max(280, calculatedDuration * 2), 0.0f, "");
''',
'''            case RANGER_VOLLEY -> spawn(level, player, "ranger_rapid",
                    player.position(), forward, 30, 0.0f, "");
            case RANGER_PIERCE -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 28, 0.0f, "");
            case RANGER_RICOCHET -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 28, 0.0f, "");
            case RANGER_FIRE_RAIN -> spawn(level, player, "ranger_energy_charge",
                    player.position(), forward, 36, 0.0f, "");
''',
        "short ranger ready visuals",
    ),
    (
'''        spawn(level, player, "vanguard_blade_wave",
                player.getEyePosition().add(forward.scale(1.25)),
                forward, 24, 1.75f, "");
''',
'''        spawn(level, player, "vanguard_blade_wave",
                player.position().add(0.0, 0.82, 0.0).add(forward.scale(1.0)),
                forward, 24, 1.75f, "");
''',
        "lower blade wave visual",
    ),
    (
'''        spawn(level, player, "ranger_rain_impact",
                center, horizontal(player.getLookAngle()), 24, 0.0f, "");
''',
'''        spawn(level, player, "ranger_rain_impact",
                center, horizontal(player.getLookAngle()), 10, 0.0f, "");
''',
        "short rain impact",
    ),
])

mesh = JAVA / "VillageSkillMeshLibrary.java"
update(mesh, [
    (
'''    private static void renderSlamCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double height = 3.0 - progress * 1.25;
        Vec3 root = b.local(0.0, height, 0.15);
        Vec3 tip = b.local(0.0, 0.35, 0.45);
        energyBlade(pose, out, root, tip, 0.36 + progress * 0.22,
                rgba(255, 85, 52, 220));
        for (int i = 0; i < 3; i++) {
            ring(pose, out, b, 0.45 + i * 0.38 + progress * 0.55,
                    0.16 + i * 0.18, 0.07, 36,
                    rgba(255, 96 + i * 28, 70, 125), -age * (0.06 + i * 0.01));
        }
        for (int i = 0; i < 6; i++) {
            double a = i * TAU / 6.0 + age * 0.04;
            spike(pose, out, b.local(Math.cos(a) * 1.0, 0.08, Math.sin(a) * 1.0),
                    b.local(Math.cos(a) * 1.5, 0.75 + progress * 0.45,
                            Math.sin(a) * 1.5),
                    0.12, rgba(214, 45, 44, 155));
        }
    }
''',
'''    private static void renderSlamCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.18);
        for (int i = 0; i < 3; i++) {
            ring(pose, out, b, 0.55 + i * 0.46 + progress * 0.42,
                    0.05 + i * 0.11, 0.075, 42,
                    rgba(255, 92 + i * 26, 66, (int) ((155 - i * 18) * fade)),
                    -age * (0.045 + i * 0.012));
        }
        for (int i = 0; i < 8; i++) {
            double a = i * TAU / 8.0 + age * 0.025;
            Vec3 start = b.local(Math.cos(a) * 0.58,
                    2.35 - progress * 1.28, Math.sin(a) * 0.58);
            Vec3 end = b.local(Math.cos(a) * 1.22,
                    0.22, Math.sin(a) * 1.22);
            prism(pose, out, start, end, 0.055,
                    rgba(238, 67, 54, (int) (150 * fade)));
        }
    }
''',
        "remove slam arrow",
    ),
    (
'''        horizontalSlash(pose, out, b, 3.4, 1.05, 0.20, 0.30,
                rgba(120, 215, 255, (int) (225 * fade)));
        horizontalSlash(pose, out, b, 2.8, 1.06, 0.09, 0.20,
                rgba(236, 252, 255, (int) (190 * fade)));
''',
'''        horizontalSlash(pose, out, b, 3.6, 0.0, 0.15, 0.07,
                rgba(120, 215, 255, (int) (225 * fade)));
        horizontalSlash(pose, out, b, 3.0, 0.01, 0.065, 0.035,
                rgba(236, 252, 255, (int) (190 * fade)));
''',
        "horizontal low blade mesh",
    ),
    (
'''        ringVertical(pose, out, b, radius, 0.65, 0.045, 56,
                rgba(255, 207, 76, (int) (220 * fade)), -age * 0.05);
        ringVertical(pose, out, b, radius * 0.45, 0.65, 0.028, 40,
                rgba(255, 247, 180, (int) (175 * fade)), age * 0.07);
        for (int i = 0; i < 4; i++) {
            reticleBracket(pose, out, b, i * Math.PI / 2.0, radius + 0.22, 0.65,
''',
'''        ringVertical(pose, out, b, radius, 0.0, 0.045, 56,
                rgba(255, 207, 76, (int) (220 * fade)), -age * 0.05);
        ringVertical(pose, out, b, radius * 0.45, 0.0, 0.028, 40,
                rgba(255, 247, 180, (int) (175 * fade)), age * 0.07);
        for (int i = 0; i < 4; i++) {
            reticleBracket(pose, out, b, i * Math.PI / 2.0, radius + 0.22, 0.0,
''',
        "front tracking reticle center",
    ),
    (
'''        ring(pose, out, b, radius, 0.045, 0.11, 96,
                rgba(88, 188, 255, 170), 0.0);
        ring(pose, out, b, radius * 0.72, 0.052, 0.045, 72,
                rgba(149, 223, 255, 90), -age * 0.008);
        for (int i = 0; i < 26; i++) {
            double a = i * 2.399963229728653 + (i % 3) * 0.17;
            double r = Math.sqrt((i + 0.5) / 26.0) * radius * 0.92;
            double cycle = fract(progress * 3.4 + i * 0.137);
            double y = 11.0 - cycle * 13.0;
            Vec3 p = b.local(Math.cos(a) * r, y, Math.sin(a) * r);
            customArrow(pose, out, Basis.DOWN, p, 0.95 + (i % 4) * 0.09, 0.07,
                    rgba(164, 228, 255, (int) (205 * (1.0 - Math.max(0.0, cycle - 0.86) / 0.14))));
        }
''',
'''        ring(pose, out, b, radius, 0.012, 0.11, 96,
                rgba(88, 188, 255, 170), 0.0);
        ring(pose, out, b, radius * 0.72, 0.018, 0.045, 72,
                rgba(149, 223, 255, 90), -age * 0.008);
        for (int i = 0; i < 18; i++) {
            double a = i * 2.399963229728653 + (i % 3) * 0.17;
            double r = Math.sqrt((i + 0.5) / 18.0) * radius * 0.92;
            double cycle = fract(progress * 5.8 + i * 0.173);
            double y = 8.5 - cycle * 9.5;
            Vec3 p = b.local(Math.cos(a) * r, y, Math.sin(a) * r);
            double fadeOut = 1.0 - Math.max(0.0, cycle - 0.72) / 0.28;
            customArrow(pose, out, Basis.DOWN, p, 0.82 + (i % 4) * 0.07, 0.06,
                    rgba(164, 228, 255, (int) (195 * fadeOut)));
        }
''',
        "grounded short rain mesh",
    ),
    (
'''        ring(pose, out, b, 0.45 + progress * 2.8, 0.045, 0.10, 56,
                rgba(124, 211, 255, (int) (180 * (1.0 - progress))), 0.0);
        ring(pose, out, b, 0.25 + progress * 1.5, 0.06, 0.045, 40,
''',
'''        ring(pose, out, b, 0.45 + progress * 2.8, 0.012, 0.10, 56,
                rgba(124, 211, 255, (int) (180 * (1.0 - progress))), 0.0);
        ring(pose, out, b, 0.25 + progress * 1.5, 0.018, 0.045, 40,
''',
        "grounded rain impact",
    ),
])

hud = JAVA / "VillageSkillHudOverlay.java"
update(hud, [
    ("        int y = graphics.guiHeight() - 82;", "        int y = graphics.guiHeight() - 92;", "raise skill hud"),
])

print("Applied Village Guardians v0.17.16 queued ranger shot, rain crash, mesh and HUD fixes")
