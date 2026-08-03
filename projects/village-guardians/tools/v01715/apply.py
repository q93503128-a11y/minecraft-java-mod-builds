#!/usr/bin/env python3
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[2]
STAGE = Path(__file__).resolve().parent
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
TOOLS = ROOT / "tools"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def replace_method(text: str, marker: str, replacement: str, label: str) -> str:
    start = text.find(marker)
    if start < 0:
        raise SystemExit(f"{label}: marker not found: {marker}")
    brace = text.find("{", start)
    if brace < 0:
        raise SystemExit(f"{label}: opening brace not found")
    depth = 0
    end = -1
    for index in range(brace, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise SystemExit(f"{label}: closing brace not found")
    return text[:start] + replacement.rstrip() + text[end:]


shutil.copyfile(STAGE / "VillageSkillHudOverlay.java", JAVA / "VillageSkillHudOverlay.java")
shutil.copyfile(STAGE / "generate_skill_mesh_texture.py", TOOLS / "generate_skill_mesh_texture.py")
shutil.copyfile(STAGE / "test_v01715_skill_combat_visuals.py", TOOLS / "test_v01715_skill_combat_visuals.py")

# Version and item-scaled skill power.
path = ROOT / "gradle.properties"
text = read(path)
text = replace_once(text, "mod_version=0.17.14-alpha.1", "mod_version=0.17.15-alpha.1", "version")
write(path, text)

path = JAVA / "VillageRoleSkillSystem.java"
text = read(path)
text = replace_once(
    text,
    "        float power = powerMultiplier(player, role)\n"
    "                * VillageProgressionSystem.learnedSkillDamageMultiplier(player);\n",
    "        float power = powerMultiplier(player, role)\n"
    "                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)\n"
    "                * VillageEquipmentRaritySystem.skillMultiplier(player);\n",
    "equipment-scaled skill power",
)
text = text.replace(
    '"일정 시간 활 충전이 크게 빨라지고 발사한 화살이 세 갈래로 분열합니다."',
    '"일정 시간 활 충전이 크게 빨라지고 발사한 화살이 세 갈래로 분열합니다. 남은 시간이 기술 HUD에 표시됩니다."')
text = text.replace(
    '"다음 화살의 조준을 강하게 보정하고 첫 적중 뒤 주변 여러 적에게 연쇄 도탄 피해를 줍니다."',
    '"조준 표식이 바라보는 위치를 따라다니며, 다음에 실제로 발사한 화살이 추적·연쇄 도탄 화살로 강화됩니다."')
text = text.replace(
    '"잠시 기를 모은 뒤 초대형 에너지 화살을 발사해 전방의 적을 관통하고 초토화합니다."',
    '"다음에 실제로 발사하는 화살을 밝은 초록색 초대형 성멸 화살로 바꾸어 넓은 전방을 관통합니다."')
write(path, text)

# Real gameplay states, arrow-triggered ranger skills, larger magic ranges and live-facing shields.
path = JAVA / "VillageRoleAbilitySystem.java"
text = read(path)
text = replace_once(
    text,
    "    private static final Map<UUID, Long> RICOCHET_UNTIL = new HashMap<>();\n",
    "    private static final Map<UUID, Long> RICOCHET_UNTIL = new HashMap<>();\n"
    "    private static final Map<UUID, Long> RICOCHET_ARROWS = new HashMap<>();\n"
    "    private static final Map<UUID, EmpoweredArrowState> MEGA_ARROW_READY = new HashMap<>();\n",
    "ranger trigger state fields",
)
text = replace_once(
    text,
    "        RICOCHET_UNTIL.clear();\n",
    "        RICOCHET_UNTIL.clear();\n"
    "        RICOCHET_ARROWS.clear();\n"
    "        MEGA_ARROW_READY.clear();\n",
    "ranger trigger state reset",
)

cast_method = r'''    public static void cast(
            ServerLevel level,
            ServerPlayer player,
            VillageRoleSkillSystem.ActiveSkill skill,
            float power,
            float durationMultiplier,
            int specialRank) {
        long now = level.getGameTime();
        int playerLevel = VillageCouncilState.levelOf(player.getUUID());
        int duration = Math.max(40, Math.round((120 + playerLevel * 3) * durationMultiplier));
        Vec3 forward = horizontalLook(player);
        Vec3 sight = lookDirection(player);
        Vec3 visualDirection = skill == VillageRoleSkillSystem.ActiveSkill.ARCANIST_FIRE_ORB
                ? sight : forward;
        VillageSkillEffectSystem.startCast(level, player, skill, duration, visualDirection);
        switch (skill) {
            case VANGUARD_WHIRLWIND -> {
                int spinDuration = Math.max(48, duration / 2);
                SPIN_UNTIL.put(player.getUUID(), now + spinDuration);
                VillageNetwork.sendSkillMotion(level, player, "vanguard_spin", spinDuration + 8);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, spinDuration, 0, false, false, true));
                play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.72f);
            }
            case VANGUARD_BREAKER -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, 1 + Math.min(1, specialRank / 3), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 1 + Math.min(1, specialRank / 4), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration, 0, false, false, true));
                for (ServerPlayer ally : allies(player, 11.0)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration, specialRank >= 3 ? 1 : 0, false, false, true));
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, false, true));
                }
                play(level, player.position(), SoundEvents.RAVAGER_ROAR, 0.85f, 1.18f);
            }
            case VANGUARD_CRY -> {
                CHARGE_UNTIL.put(player.getUUID(), now + 22);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 26, 2, false, false, true));
                for (int i = 0; i < 6; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 4L + i * 4L, player.getUUID(), skill,
                            ActionKind.BLADE_WAVE, power, durationMultiplier, specialRank, player.position(), forward));
                }
                play(level, player.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.72f);
            }
            case VANGUARD_STORM -> {
                SLAMS.put(player.getUUID(), new SlamState(now, power, specialRank, player.position()));
                player.setDeltaMovement(forward.scale(0.52).add(0.0, 1.05, 0.0));
                player.hurtMarked = true;
                play(level, player.position(), SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.35f);
            }

            case RANGER_VOLLEY -> {
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

            case ARCANIST_FIRE_ORB -> launchMoving(level, player, MovingKind.FIRE_ORB,
                    new ItemStack(Items.FIRE_CHARGE), 1.35, 100,
                    (12.0f + playerLevel * 0.65f) * power, 4.8, specialRank, sight);
            case ARCANIST_FROST_RING -> {
                Vec3 center = aimedGround(level, player, 18.0);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.FROST, center,
                        now + Math.max(140, duration), 7.5, power, specialRank, 0));
                play(level, center, SoundEvents.GLASS_PLACE, 1.1f, 0.62f);
            }
            case ARCANIST_CHAIN -> {
                Vec3 center = player.position().add(forward.scale(3.0));
                AREAS.add(new AreaState(player.getUUID(), AreaKind.TORNADO, center,
                        now + Math.max(120, duration), 8.5, power, specialRank, 0));
                play(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.1f, 0.72f);
            }
            case ARCANIST_NOVA -> {
                Vec3 center = aimedGround(level, player, 22.0);
                AREAS.add(new AreaState(player.getUUID(), AreaKind.LIGHTNING, center,
                        now + Math.max(100, duration / 2), 18.0, power, specialRank, 0));
                play(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.85f, 1.15f);
            }

            case LUMINAR_HEAL -> healLowestAlly(player,
                    (10.0f + playerLevel * 0.7f) * power, specialRank, false);
            case LUMINAR_CLEANSE -> cleanseAllies(player,
                    (3.0f + playerLevel * 0.22f) * power, specialRank);
            case LUMINAR_VEIL -> {
                AREAS.add(new AreaState(player.getUUID(), AreaKind.HEALING, player.position(),
                        now + Math.max(160, duration * 2L), 7.5, power, specialRank, 0));
                play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f);
            }
            case LUMINAR_SANCTUARY -> miracle(player,
                    (15.0f + playerLevel * 0.9f) * power, Math.max(2, specialRank), duration * 2);

            case WARDEN_TAUNT -> {
                AEGIS_UNTIL.remove(player.getUUID());
                SCHEDULED.add(new ScheduledAction(now, player.getUUID(), skill,
                        ActionKind.SHIELD_CHARGE, power, durationMultiplier, specialRank,
                        player.position(), forward));
                player.setDeltaMovement(forward.scale(1.05).add(0.0, 0.08, 0.0));
                player.hurtMarked = true;
            }
            case WARDEN_BASH -> tauntShout(level, player,
                    (4.0f + playerLevel * 0.25f) * power, duration, specialRank);
            case WARDEN_FORMATION -> {
                FORTRESS_UNTIL.put(player.getUUID(), now + Math.max(120, duration));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.max(120, duration), 5 + Math.min(3, specialRank), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(120, duration), 3, false, false, true));
                play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 1.4f, 0.55f);
            }
            case WARDEN_FIELD -> {
                AEGIS_UNTIL.put(player.getUUID(), now + Math.max(180, duration * 2L));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(180, duration * 2), 1, false, false, true));
                play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 0.7f);
            }
        }

        if (skill.role() == VillageRole.ARCANIST && !replayingEcho) {
            int echoes = 0;
            if (player.getRandom().nextFloat() < 0.30f) echoes++;
            if (player.getRandom().nextFloat() < 0.12f) echoes++;
            for (int i = 0; i < echoes; i++) {
                SCHEDULED.add(new ScheduledAction(now + 8L + i * 8L, player.getUUID(), skill,
                        ActionKind.ARCANE_ECHO, power * 0.72f, durationMultiplier, specialRank,
                        player.position(), visualDirection));
            }
        }
    }'''
text = replace_method(text, "    public static void cast(", cast_method, "ability cast")

tick_players = r'''    private static void tickPlayers(MinecraftServer server, long now) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level) || VillageRespawnSystem.isDowned(player)) continue;
            UUID id = player.getUUID();
            long spinUntil = SPIN_UNTIL.getOrDefault(id, 0L);
            if (spinUntil >= now) {
                player.setYBodyRot((float) ((now * 38.0) % 360.0));
                if (now % 6L == 0L) {
                    VillageNetwork.sendSkillMotion(level, player, "vanguard_spin",
                            (int) Math.max(8L, spinUntil - now + 8L));
                }
                if (now % 3L == 0L) {
                    damageRadius(level, player, player.position(), 4.7, 10,
                            2.4f + VillageCouncilState.levelOf(id) * 0.16f,
                            false, 0.32, 0.05);
                    play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f,
                            0.8f + (now % 4) * 0.05f);
                }
            }
            if (RICOCHET_UNTIL.getOrDefault(id, 0L) >= now && now % 3L == 0L) {
                Vec3 sight = lookDirection(player);
                VillageSkillEffectSystem.trackingReticle(
                        level, player, aimPoint(level, player, 42.0), sight);
            }
            SlamState slam = SLAMS.get(id);
            if (slam != null && now > slam.startedAt() + 5L
                    && (player.onGround() || now > slam.startedAt() + 34L)) {
                groundSlam(level, player, slam.power(), slam.specialRank());
                SLAMS.remove(id);
            }
            if (FORTRESS_UNTIL.getOrDefault(id, 0L) >= now) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                if (now % 5L == 0L) {
                    pushFront(level, player, 3.6, 16, 0.38, 0.04, 0.0f);
                }
            } else if (AEGIS_UNTIL.getOrDefault(id, 0L) >= now) {
                if (now % 3L == 0L) pushFront(level, player, 7.0, 30, 0.7, 0.08, 1.2f);
                if (player.isSprinting() && now - LAST_AEGIS_DASH.getOrDefault(id, -100L) >= 14L) {
                    LAST_AEGIS_DASH.put(id, now);
                    Vec3 forward = horizontalLook(player);
                    player.setDeltaMovement(forward.scale(0.78).add(0.0, 0.05, 0.0));
                    player.hurtMarked = true;
                    play(level, player.position(), SoundEvents.SHIELD_BLOCK.value(), 0.8f, 1.15f);
                }
            }
            if (VillageCouncilState.roleOf(id).orElse(null) == VillageRole.WARDEN && now % 40L == 0L) {
                player.heal(0.8f);
            }
        }
    }'''
text = replace_method(text, "    private static void tickPlayers(", tick_players, "ability tick players")

tick_areas = r'''    private static void tickAreas(MinecraftServer server, long now) {
        Iterator<AreaState> iterator = AREAS.iterator();
        while (iterator.hasNext()) {
            AreaState area = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(area.owner());
            if (owner == null || !(owner.level() instanceof ServerLevel level) || now > area.until()) {
                iterator.remove();
                continue;
            }
            if (now % 5L != area.phase() % 5L) continue;
            switch (area.kind()) {
                case FROST -> {
                    for (Mob target : targetsNear(level, owner, area.center(), area.radius(), 40)) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 3, false, false, true));
                        if (now % 20L == 0L) hurt(level, target, 2.2f * area.power());
                    }
                    if (now % 20L == 0L) play(level, area.center(), SoundEvents.GLASS_HIT, 0.55f, 0.62f);
                }
                case TORNADO -> {
                    Vec3 next = area.center().add(horizontalLook(owner).scale(0.24));
                    area.moveTo(next);
                    for (Mob target : targetsNear(level, owner, next, area.radius(), 36)) {
                        Vec3 pull = next.subtract(target.position());
                        Vec3 horizontal = new Vec3(pull.x, 0.0, pull.z);
                        if (horizontal.lengthSqr() > 0.01) horizontal = horizontal.normalize().scale(0.24);
                        target.push(horizontal.x, 0.20, horizontal.z);
                        target.hurtMarked = true;
                        if (now % 15L == 0L) hurt(level, target, 1.8f * area.power());
                    }
                    if (now % 15L == 0L) play(level, next, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8f, 0.78f);
                }
                case LIGHTNING -> {
                    if (now % 5L == 0L) {
                        float damage = (7.0f + VillageCouncilState.levelOf(owner.getUUID()) * 0.42f)
                                * area.power();
                        for (int strikeIndex = 0; strikeIndex < 2; strikeIndex++) {
                            Vec3 strike = randomPointInCircle(level, area.center(), area.radius());
                            List<Mob> nearby = targetsNear(level, owner, strike, 4.8, 24);
                            if (!nearby.isEmpty() && owner.getRandom().nextFloat() < 0.72f) {
                                strike = nearby.get(owner.getRandom().nextInt(nearby.size())).position();
                            }
                            spawnVisualLightning(level, strike);
                            for (Mob target : targetsNear(level, owner, strike, 4.8, 24)) {
                                hurt(level, target, damage);
                                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, false, false, true));
                            }
                        }
                    }
                }
                case HEALING -> {
                    if (now % 20L == 0L) {
                        for (ServerPlayer ally : alliesAt(owner, area.center(), area.radius())) {
                            healScaled(ally, (2.6f + area.specialRank() * 0.35f) * area.power());
                            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 35, 0, false, false, true));
                        }
                        play(level, area.center(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.55f, 1.15f);
                    }
                }
            }
        }
    }'''
text = replace_method(text, "    private static void tickAreas(", tick_areas, "ability tick areas")

tick_moving = r'''    private static void tickMoving(MinecraftServer server, long now) {
        Iterator<Map.Entry<UUID, MovingSkill>> iterator = MOVING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MovingSkill> entry = iterator.next();
            MovingSkill moving = entry.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(moving.owner());
            if (owner == null || !(owner.level() instanceof ServerLevel level)) {
                iterator.remove();
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            Vec3 position = entity == null ? moving.lastPosition() : entity.position();
            moving.lastPosition(position);
            moving.age(moving.age() + 1);
            List<Mob> hits = targetsNear(level, owner, position, moving.radius(), 36);
            boolean expired = entity == null || moving.age() >= moving.maxAge();
            switch (moving.kind()) {
                case FIRE_ORB -> {
                    if (hits.isEmpty() && !expired) continue;
                    for (Mob target : targetsNear(level, owner, position, moving.radius(), 36)) {
                        hurt(level, target, moving.damage());
                        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),
                                120 + moving.specialRank() * 35));
                    }
                    VillageSkillEffectSystem.fireImpact(level, owner, position, moving.radius());
                    play(level, position, SoundEvents.GENERIC_EXPLODE.value(), 1.05f, 1.08f);
                }
                case BLADE -> {
                    for (Mob target : hits) {
                        if (moving.hit().add(target.getUUID())) {
                            hurt(level, target, moving.damage());
                            knockFrom(position, target, 0.5, 0.05);
                        }
                    }
                    if (!expired) continue;
                }
                case ENERGY_ARROW -> {
                    for (Mob target : hits) {
                        if (moving.hit().add(target.getUUID())) {
                            hurt(level, target, moving.damage());
                            knockFrom(position, target, 1.35, 0.18);
                        }
                    }
                    if (!expired) continue;
                    play(level, position, SoundEvents.GENERIC_EXPLODE.value(), 1.3f, 0.62f);
                }
            }
            if (entity != null) entity.discard();
            iterator.remove();
        }
    }'''
text = replace_method(text, "    private static void tickMoving(", tick_moving, "ability tick moving")

cleanup = r'''    private static void cleanupExpired(MinecraftServer server, long now) {
        SPIN_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RAPID_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RICOCHET_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue() < now);
        MEGA_ARROW_READY.entrySet().removeIf(entry -> entry.getValue().until() < now);
        FORTRESS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        AEGIS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        CHARGE_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
    }'''
text = replace_method(text, "    private static void cleanupExpired(", cleanup, "ability cleanup")

handle_join = r'''    public static void handleEntityJoin(EntityJoinLevelEvent event) {
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
    }'''
text = replace_method(text, "    public static void handleEntityJoin(", handle_join, "arrow join handler")

handle_damage = r'''    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            VillageRole role = VillageCouncilState.roleOf(attacker.getUUID()).orElse(null);
            if (role == VillageRole.VANGUARD && !(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                attacker.heal(Math.min(2.5f, event.getAmount() * 0.055f));
            }
            if (role == VillageRole.RANGER
                    && event.getSource().getDirectEntity() instanceof AbstractArrow directArrow
                    && event.getEntity() instanceof Mob primary
                    && RICOCHET_ARROWS.remove(directArrow.getUUID()) != null
                    && attacker.level() instanceof ServerLevel level) {
                List<Mob> chain = targetsNear(level, attacker, primary.position(), 12.0, 9);
                chain.remove(primary);
                chain.sort(Comparator.comparingDouble(primary::distanceToSqr));
                float damage = Math.max(2.0f, event.getAmount() * 0.72f);
                List<Mob> visualChain = new ArrayList<>();
                for (int i = 0; i < Math.min(6, chain.size()); i++) {
                    Mob target = chain.get(i);
                    visualChain.add(target);
                    hurt(level, target, damage * (1.0f - i * 0.09f));
                    play(level, target.position(), SoundEvents.ARROW_HIT, 0.55f, 1.2f + i * 0.06f);
                }
                VillageSkillEffectSystem.ricochet(level, attacker, primary, visualChain);
            }
        }
        if (event.getEntity() instanceof ServerPlayer defender
                && VillageCouncilState.roleOf(defender.getUUID()).orElse(null) == VillageRole.WARDEN) {
            event.setAmount(event.getAmount() * 0.82f);
        }
    }'''
text = replace_method(text, "    public static void handleIncomingDamage(", handle_damage, "incoming damage handler")

active_hud = r'''    public static String activeSkillHud(ServerPlayer player) {
        if (player == null) return "";
        long now = player.level().getGameTime();
        List<String> states = new ArrayList<>();
        appendTimed(states, "§b신속 삼연사", RAPID_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        if (RICOCHET_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) {
            states.add("§e추적 도탄 §f다음 화살");
        }
        EmpoweredArrowState mega = MEGA_ARROW_READY.get(player.getUUID());
        if (mega != null && mega.until() >= now) {
            states.add("§a성멸 대궁 §f다음 화살");
        }
        appendTimed(states, "§9거대 방패 태세", FORTRESS_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        appendTimed(states, "§3대수호 진군", AEGIS_UNTIL.getOrDefault(player.getUUID(), 0L), now);
        return String.join(" §8· ", states);
    }

    private static void appendTimed(List<String> output, String label, long until, long now) {
        if (until < now) return;
        output.add(label + " §f" + String.format(java.util.Locale.ROOT, "%.1f초", (until - now) / 20.0));
    }'''
insert_marker = "    public static boolean isRapidFire(ServerPlayer player) {"
idx = text.find(insert_marker)
if idx < 0:
    raise SystemExit("active skill HUD insertion marker missing")
text = text[:idx] + active_hud + "\n\n" + text[idx:]

blade_wave = r'''    private static void bladeWave(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 direction = horizontalLook(player);
        VillageSkillEffectSystem.bladeWave(level, player, direction);
        launchMovingAt(level, player, MovingKind.BLADE, new ItemStack(Items.IRON_SWORD),
                1.75, 24, (5.4f + VillageCouncilState.levelOf(player.getUUID()) * 0.30f) * power,
                1.45, specialRank, player.getEyePosition().add(direction.scale(1.25)), direction);
        play(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f,
                0.78f + player.getRandom().nextFloat() * 0.18f);
    }'''
text = replace_method(text, "    private static void bladeWave(", blade_wave, "blade wave")

arrow_rain = r'''    private static void arrowRain(ServerLevel level, ServerPlayer player, Vec3 center, float power, int specialRank) {
        VillageSkillEffectSystem.arrowRainImpact(level, player, center);
        float damage = (3.3f + VillageCouncilState.levelOf(player.getUUID()) * 0.18f) * power;
        for (Mob target : targetsNear(level, player, center, 8.5, 40)) {
            hurt(level, target, damage);
            if (specialRank >= 3) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
        }
        for (int i = 0; i < 7; i++) {
            Vec3 random = randomPointInCircle(level, center, 8.2).add(0.0, 11.0 + level.getRandom().nextDouble() * 3.0, 0.0);
            spawnFallingArrow(level, player, random);
        }
        play(level, center, SoundEvents.ARROW_SHOOT, 0.8f, 1.45f);
    }'''
text = replace_method(text, "    private static void arrowRain(", arrow_rain, "arrow rain")

energy_arrow = r'''    private static void launchEnergyArrow(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.stopUsingItem();
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 direction = lookDirection(player);
        Vec3 origin = player.getEyePosition().add(direction.scale(2.8));
        VillageSkillEffectSystem.energyArrow(level, player, origin, direction);
        float damage = (31.0f + VillageCouncilState.levelOf(player.getUUID()) * 1.35f) * power;
        launchMovingAt(level, player, MovingKind.ENERGY_ARROW, new ItemStack(Items.SPECTRAL_ARROW),
                2.65, 55, damage, 5.0, specialRank, origin, direction);
        play(level, player.position(), SoundEvents.ENDER_DRAGON_SHOOT, 1.45f, 0.78f);
    }'''
text = replace_method(text, "    private static void launchEnergyArrow(", energy_arrow, "energy arrow")

text = text.replace("targetsNear(level, player, player.position(), 10.0, 30)",
                    "targetsNear(level, player, player.position(), 20.0, 60)")

aim_assist = r'''    private static void aimAssist(
            ServerLevel level, ServerPlayer player, AbstractArrow arrow, double strength) {
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.1) return;
        Vec3 direction = velocity.normalize();
        Mob target = targetsNear(level, player, player.position(), 44.0, 48).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(arrow.position());
                    return to.lengthSqr() > 0.1 && to.normalize().dot(direction) >= 0.72;
                })
                .min(Comparator.comparingDouble(mob ->
                        mob.getEyePosition().distanceToSqr(aimPoint(level, player, 44.0))))
                .orElse(null);
        if (target == null) return;
        Vec3 assisted = target.getEyePosition().subtract(arrow.position()).normalize();
        double safe = Math.max(0.0, Math.min(0.82, strength));
        Vec3 blended = direction.scale(1.0 - safe).add(assisted.scale(safe)).normalize();
        arrow.setDeltaMovement(blended.scale(speed));
        arrow.hurtMarked = true;
    }'''
text = replace_method(text, "    private static void aimAssist(", aim_assist, "aim assist")

aimed_ground = r'''    private static Vec3 aimedGround(ServerLevel level, ServerPlayer player, double distance) {
        Vec3 point = aimPoint(level, player, distance);
        return new Vec3(point.x, player.getY(), point.z);
    }

    private static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double distance) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(lookDirection(player).scale(distance));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(
                eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static Vec3 lookDirection(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        return look.lengthSqr() < 0.001 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
    }'''
text = replace_method(text, "    private static Vec3 aimedGround(", aimed_ground, "aim helpers")

lightning_helpers = r'''    private static Vec3 randomPointInCircle(ServerLevel level, Vec3 center, double radius) {
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(level.getRandom().nextDouble()) * radius;
        return center.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
    }

    private static void spawnVisualLightning(ServerLevel level, Vec3 position) {
        var lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (lightning == null) return;
        lightning.setVisualOnly(true);
        lightning.setPos(position.x, position.y, position.z);
        level.addFreshEntity(lightning);
    }'''
insert_marker = "    private static void play(ServerLevel level, Vec3 position, SoundEvent sound, float volume, float pitch) {"
idx = text.find(insert_marker)
if idx < 0:
    raise SystemExit("lightning helper insertion marker missing")
text = text[:idx] + lightning_helpers + "\n\n" + text[idx:]

record_marker = "    private record SlamState(long startedAt, float power, int specialRank, Vec3 origin) {}"
text = replace_once(
    text,
    record_marker,
    "    private record EmpoweredArrowState(long until, float power, int specialRank) {}\n\n"
    + record_marker,
    "empowered arrow record",
)
write(path, text)

# Server visual dispatcher: moving reticle, random rain field, green giant arrow and fire impact.
path = JAVA / "VillageSkillEffectSystem.java"
text = read(path)
start_cast = r'''    public static void startCast(
            ServerLevel level,
            ServerPlayer player,
            VillageRoleSkillSystem.ActiveSkill skill,
            int calculatedDuration,
            Vec3 direction) {
        if (level == null || player == null || skill == null) return;
        Vec3 forward = horizontal(direction);
        Vec3 sight = normalized(direction);
        switch (skill) {
            case VANGUARD_WHIRLWIND -> {
                int duration = Math.max(48, calculatedDuration / 2);
                spawn(level, player, "vanguard_spin", player.position(), forward, duration, 0.0f, "");
                VillageNetwork.sendSkillMotion(level, player, "vanguard_spin", duration + 8);
            }
            case VANGUARD_BREAKER -> spawn(level, player, "vanguard_rally",
                    player.position(), forward, 60, 0.0f, "");
            case VANGUARD_CRY -> spawn(level, player, "vanguard_blade_charge",
                    player.position(), forward, 26, 0.0f, "");
            case VANGUARD_STORM -> spawn(level, player, "vanguard_slam_charge",
                    player.position(), forward, 44, 0.0f, "");

            case RANGER_VOLLEY -> spawn(level, player, "ranger_rapid",
                    player.position(), forward, Math.max(200, calculatedDuration), 0.0f, "");
            case RANGER_PIERCE -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 28, 0.0f, "");
            case RANGER_RICOCHET -> spawn(level, player, "ranger_focus",
                    player.position(), forward, 20, 0.0f, "");
            case RANGER_FIRE_RAIN -> spawn(level, player, "ranger_energy_charge",
                    player.position(), forward, Math.max(280, calculatedDuration * 2), 0.0f, "");

            case ARCANIST_FIRE_ORB -> spawn(level, player, "arcanist_fire_orb",
                    player.getEyePosition().add(sight.scale(1.0)), sight, 100, 1.35f, "");
            case ARCANIST_FROST_RING -> spawn(level, player, "arcanist_frost",
                    player.position().add(forward.scale(18.0)), forward,
                    Math.max(140, calculatedDuration), 0.0f, "");
            case ARCANIST_CHAIN -> spawn(level, player, "arcanist_tornado",
                    player.position().add(forward.scale(3.0)), forward,
                    Math.max(120, calculatedDuration), 0.24f, "");
            case ARCANIST_NOVA -> spawn(level, player, "arcanist_lightning",
                    player.position().add(forward.scale(22.0)), forward,
                    Math.max(100, calculatedDuration / 2), 0.0f, "");

            case LUMINAR_HEAL -> spawn(level, player, "luminar_heal_cast",
                    player.position(), forward, 32, 0.0f, "");
            case LUMINAR_CLEANSE -> spawn(level, player, "luminar_cleanse_cast",
                    player.position(), forward, 44, 0.0f, "");
            case LUMINAR_VEIL -> spawn(level, player, "luminar_healing_field",
                    player.position(), forward, Math.max(160, calculatedDuration * 2), 0.0f, "");
            case LUMINAR_SANCTUARY -> spawn(level, player, "luminar_miracle_cast",
                    player.position(), forward, 72, 0.0f, "");

            case WARDEN_TAUNT -> spawn(level, player, "warden_charge_cast",
                    player.position(), forward, 32, 0.0f, "");
            case WARDEN_BASH -> spawn(level, player, "warden_taunt",
                    player.position(), forward, 48, 0.0f, "");
            case WARDEN_FORMATION -> spawn(level, player, "warden_fortress",
                    player.position(), forward, Math.max(120, calculatedDuration), 0.0f, "");
            case WARDEN_FIELD -> spawn(level, player, "warden_aegis",
                    player.position(), forward, Math.max(180, calculatedDuration * 2), 0.0f, "");
        }
    }'''
text = replace_method(text, "    public static void startCast(", start_cast, "effect start cast")

blade_effect = r'''    public static void bladeWave(ServerLevel level, ServerPlayer player, Vec3 direction) {
        Vec3 forward = horizontal(direction);
        spawn(level, player, "vanguard_blade_wave",
                player.getEyePosition().add(forward.scale(1.25)),
                forward, 24, 1.75f, "");
    }'''
text = replace_method(text, "    public static void bladeWave(", blade_effect, "blade wave effect")

energy_effect = r'''    public static void energyArrow(ServerLevel level, ServerPlayer player, Vec3 direction) {
        Vec3 sight = normalized(direction);
        energyArrow(level, player, player.getEyePosition().add(sight.scale(2.8)), sight);
    }

    public static void energyArrow(
            ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 direction) {
        spawn(level, player, "ranger_energy_projectile",
                origin, normalized(direction), 55, 2.65f, "");
    }'''
text = replace_method(text, "    public static void energyArrow(", energy_effect, "energy arrow effect")

new_effect_methods = r'''    public static void trackingReticle(
            ServerLevel level, ServerPlayer player, Vec3 target, Vec3 direction) {
        spawn(level, player, "ranger_lock", target, normalized(direction), 7, 0.0f, "");
    }

    public static void arrowRainField(
            ServerLevel level, ServerPlayer player, Vec3 center, int duration, double radius) {
        spawn(level, player, "ranger_rain_field", center, horizontal(player.getLookAngle()),
                Math.max(20, duration), 0.0f,
                String.format(Locale.ROOT, "%.2f", Math.max(2.0, radius)));
    }

    public static void fireImpact(
            ServerLevel level, ServerPlayer player, Vec3 center, double radius) {
        spawn(level, player, "arcanist_fire_impact", center, normalized(player.getLookAngle()),
                24, 0.0f, String.format(Locale.ROOT, "%.2f", Math.max(1.0, radius)));
    }

'''
insert_marker = "    public static void ricochet("
idx = text.find(insert_marker)
if idx < 0:
    raise SystemExit("effect helper insertion marker missing")
text = text[:idx] + new_effect_methods + text[idx:]

normalized_helper = r'''    private static Vec3 normalized(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : value;
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }

'''
insert_marker = "    private static Vec3 horizontal(Vec3 value) {"
idx = text.find(insert_marker)
if idx < 0:
    raise SystemExit("normalized helper insertion marker missing")
text = text[:idx] + normalized_helper + text[idx:]
write(path, text)

# Visual actors never cull, update live owner direction and keep aim reticles at their target point.
path = JAVA / "VillageSkillEffectEntity.java"
text = read(path)
text = replace_once(
    text,
    "        noPhysics = true;\n        setNoGravity(true);\n",
    "        noPhysics = true;\n        noCulling = true;\n        setNoGravity(true);\n",
    "effect no-culling",
)
entity_tick = r'''    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        int duration = Math.max(1, duration());
        if (tickCount > duration) {
            discard();
            return;
        }
        Entity owner = ownerEntity();
        if (followsOwner() && owner != null && owner.isAlive()) {
            if (tracksOwnerLook()) {
                Vec3 look = owner.getLookAngle();
                if (kind().startsWith("warden_")) look = new Vec3(look.x, 0.0, look.z);
                if (look.lengthSqr() > 1.0E-6) setDirection(look.normalize());
            }
            Vec3 target = switch (kind()) {
                case "ranger_energy_charge" -> owner.getEyePosition().add(direction().scale(2.35));
                case "ranger_focus" -> owner.getEyePosition().add(direction().scale(1.15));
                case "vanguard_slam_charge" -> owner.position().add(0.0, 0.2, 0.0);
                default -> owner.position();
            };
            setPos(target);
        } else if (speed() != 0.0f) {
            setPos(position().add(direction().scale(speed())));
        }
    }

    private boolean tracksOwnerLook() {
        return switch (kind()) {
            case "ranger_focus", "ranger_energy_charge", "warden_charge_cast",
                    "warden_fortress", "warden_aegis" -> true;
            default -> false;
        };
    }'''
text = replace_method(text, "    @Override\n    public void tick()", entity_tick, "effect entity tick")

follows = r'''    private boolean followsOwner() {
        return switch (kind()) {
            case "vanguard_spin", "vanguard_rally", "vanguard_blade_charge",
                    "vanguard_slam_charge", "ranger_rapid", "ranger_focus",
                    "ranger_energy_charge", "luminar_heal_cast", "luminar_cleanse_cast",
                    "luminar_healing_field", "luminar_miracle_cast",
                    "warden_charge_cast", "warden_taunt", "warden_fortress", "warden_aegis" -> true;
            default -> false;
        };
    }'''
text = replace_method(text, "    private boolean followsOwner()", follows, "effect follow owner")
write(path, text)

# Smooth repeated motion refresh: avatar and held weapon rotate, camera orientation remains untouched.
path = JAVA / "VillageSkillEffectClient.java"
text = read(path)
accept_motion = r'''    public static void acceptMotion(VillageNetwork.SkillMotionPayload payload) {
        if (payload == null || payload.entityId() < 0 || payload.durationTicks() <= 0) return;
        long now = System.nanoTime();
        MOTIONS.compute(payload.entityId(), (id, old) -> {
            long startedAt = old != null && old.name.equals(payload.motion())
                    ? old.startedAt : now;
            return new Motion(payload.motion(), startedAt,
                    Math.max(old == null ? 0L : old.expiresAt,
                            now + payload.durationTicks() * 50_000_000L));
        });
    }'''
text = replace_method(text, "    public static void acceptMotion(", accept_motion, "motion accept")
render_player = r'''    private static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        long now = System.nanoTime();
        Iterator<Map.Entry<Integer, Motion>> iterator = MOTIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt < now) iterator.remove();
        }
        int id = event.getRenderState().id;
        Motion motion = MOTIONS.get(id);
        if (motion == null || !"vanguard_spin".equals(motion.name)) return;

        float elapsedSeconds = (now - motion.startedAt) / 1_000_000_000.0f;
        float radians = elapsedSeconds * (float) Math.toRadians(900.0);
        PoseStack stack = event.getPoseStack();
        stack.mulPose(new Quaternionf().rotateY(radians));

        // Rotate the whole rendered avatar including the held weapon, not the camera.
        event.getRenderState().bodyRot = 0.0f;
        event.getRenderState().yRot = 0.0f;
        event.getRenderState().xRot = 0.0f;
        event.getRenderState().walkAnimationSpeed = 0.0f;
    }'''
text = replace_method(text, "    private static void onRenderPlayer(", render_player, "player spin render")
write(path, text)

# Mesh redesign: solid material, no generic arrowhead decorations, clean slash and correct shields.
path = JAVA / "VillageSkillMeshLibrary.java"
text = read(path)
text = replace_once(
    text,
    '            case "ranger_rapid" -> renderRapidFire(pose, out, basis, age, progress);\n'
    '            case "ranger_lock" -> renderTargetLock(pose, out, basis, age, progress);\n',
    '            case "ranger_rapid" -> renderRapidFire(pose, out, basis, age, progress);\n'
    '            case "ranger_focus" -> renderRangerFocus(pose, out, basis, age, progress);\n'
    '            case "ranger_lock" -> renderTargetLock(pose, out, basis, age, progress);\n',
    "ranger focus mesh switch",
)
text = replace_once(
    text,
    '            case "arcanist_fire_orb" -> renderFireOrb(pose, out, basis, age, progress);\n',
    '            case "arcanist_fire_orb" -> renderFireOrb(pose, out, basis, age, progress);\n'
    '            case "arcanist_fire_impact" -> renderFireImpact(pose, out, basis, age, progress, state.extra);\n',
    "fire impact mesh switch",
)

mesh_methods = {
"    private static void renderVanguardSpin(": r'''    private static void renderVanguardSpin(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.16);
        for (int layer = 0; layer < 4; layer++) {
            double angle = age * 0.78 + layer * TAU / 4.0;
            double radius = 1.35 + layer * 0.34;
            double y = 0.72 + layer * 0.24;
            slashArc(pose, out, b, angle, radius, y, 1.18, 0.14 + layer * 0.018,
                    rgba(255, 188 - layer * 14, 72, (int) (190 * fade)));
        }
        ring(pose, out, b, 1.05, 0.06, 0.08, 56,
                rgba(255, 103, 38, (int) (120 * fade)), -age * 0.035);
    }''',
"    private static void renderBladeCharge(": r'''    private static void renderBladeCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.10, 0.20);
        Vec3 core = b.local(0.42, 1.05, 0.52);
        sphere(pose, out, core, 0.16 + progress * 0.10, 8, 12,
                rgba(165, 228, 255, (int) (205 * fade)));
        for (int i = 0; i < 3; i++) {
            slashArc(pose, out, b, age * 0.16 + i * TAU / 3.0,
                    0.72 + i * 0.16, 0.92 + i * 0.13,
                    0.82, 0.055,
                    rgba(128, 211, 255, (int) ((150 - i * 20) * fade)));
        }
    }''',
"    private static void renderBladeWave(": r'''    private static void renderBladeWave(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = 1.0 - progress * 0.70;
        horizontalSlash(pose, out, b, 3.4, 1.05, 0.20, 0.30,
                rgba(120, 215, 255, (int) (225 * fade)));
        horizontalSlash(pose, out, b, 2.8, 1.06, 0.09, 0.20,
                rgba(236, 252, 255, (int) (190 * fade)));
    }''',
"    private static void renderRapidFire(": r'''    private static void renderRapidFire(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.16);
        for (int i = 0; i < 3; i++) {
            ringVertical(pose, out, b, 0.58 + i * 0.19,
                    1.20, 0.035, 48,
                    rgba(112, 220, 255, (int) ((155 - i * 24) * fade)),
                    age * (0.045 + i * 0.012));
        }
        helixRibbon(pose, out, b, age * 0.18, 0.78, 2.1, 28,
                rgba(103, 206, 255, (int) (125 * fade)));
        helixRibbon(pose, out, b, Math.PI + age * 0.18, 0.78, 2.1, 28,
                rgba(205, 246, 255, (int) (105 * fade)));
    }''',
"    private static void renderTargetLock(": r'''    private static void renderTargetLock(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.05, 0.18);
        double radius = 0.72 + Math.sin(age * 0.32) * 0.06;
        ringVertical(pose, out, b, radius, 0.65, 0.045, 56,
                rgba(255, 207, 76, (int) (220 * fade)), -age * 0.05);
        ringVertical(pose, out, b, radius * 0.45, 0.65, 0.028, 40,
                rgba(255, 247, 180, (int) (175 * fade)), age * 0.07);
        for (int i = 0; i < 4; i++) {
            reticleBracket(pose, out, b, i * Math.PI / 2.0, radius + 0.22, 0.65,
                    rgba(255, 224, 112, (int) (205 * fade)));
        }
    }''',
"    private static void renderArrowRainField(": r'''    private static void renderArrowRainField(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, Random random) {
        double radius = 8.5;
        ring(pose, out, b, radius, 0.045, 0.11, 96,
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
    }''',
"    private static void renderArrowRainImpact(": r'''    private static void renderArrowRainImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, Random random) {
        ring(pose, out, b, 0.45 + progress * 2.8, 0.045, 0.10, 56,
                rgba(124, 211, 255, (int) (180 * (1.0 - progress))), 0.0);
        ring(pose, out, b, 0.25 + progress * 1.5, 0.06, 0.045, 40,
                rgba(225, 249, 255, (int) (130 * (1.0 - progress))), age * 0.02);
    }''',
"    private static void renderEnergyCharge(": r'''    private static void renderEnergyCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double pulse = 0.34 + Math.sin(age * 0.20) * 0.05;
        Vec3 core = b.local(0.0, 0.0, 0.0);
        sphere(pose, out, core, pulse, 10, 16, rgba(91, 255, 104, 225));
        Vec3 top = b.local(0.0, 1.15, 0.18);
        Vec3 middle = b.local(0.0, 0.0, 0.70);
        Vec3 bottom = b.local(0.0, -1.15, 0.18);
        prism(pose, out, top, middle, 0.065, rgba(131, 255, 139, 190));
        prism(pose, out, middle, bottom, 0.065, rgba(131, 255, 139, 190));
        prism(pose, out, top, bottom, 0.025, rgba(220, 255, 221, 135));
        for (int i = 0; i < 3; i++) {
            ringVertical(pose, out, b, 0.55 + i * 0.22, 0.0, 0.035, 42,
                    rgba(80, 255, 96, 125 - i * 20), age * (0.055 + i * 0.012));
        }
    }''',
"    private static void renderEnergyProjectile(": r'''    private static void renderEnergyProjectile(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double pulse = 1.0 + Math.sin(age * 0.42) * 0.05;
        customArrow(pose, out, b, Vec3.ZERO, 5.2 * pulse, 0.42,
                rgba(91, 255, 104, 245));
        sphere(pose, out, b.local(0.0, 0.0, -1.55), 0.48, 9, 14,
                rgba(195, 255, 198, 170));
        for (int i = 0; i < 4; i++) {
            double a = age * 0.20 + i * TAU / 4.0;
            Vec3 start = b.local(Math.cos(a) * 0.65, Math.sin(a) * 0.65, -2.4);
            Vec3 end = b.local(Math.cos(a) * 0.12, Math.sin(a) * 0.12, 0.9);
            taperedRibbon(pose, out, b, start, end, 0.13,
                    rgba(154, 255, 160, 145));
        }
    }''',
"    private static void renderTornado(": r'''    private static void renderTornado(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        for (int strand = 0; strand < 8; strand++) {
            double phase = strand * TAU / 8.0 + age * (0.19 + strand * 0.006);
            int shade = 118 + (strand % 4) * 22;
            tornadoRibbon(pose, out, b, phase, 5.8, 46,
                    rgba(shade, shade + 4, shade + 9, 125 + strand * 8));
        }
        for (int i = 0; i < 24; i++) {
            double cycle = fract(age * 0.035 + i * 0.117);
            double y = 0.18 + cycle * 5.4;
            double radius = 0.65 + cycle * 2.7 + (i % 3) * 0.14;
            double angle = age * 0.17 + i * 2.399963229728653;
            Vec3 start = b.local(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 end = start.add(b.local(-Math.sin(angle) * 0.28, 0.10,
                    Math.cos(angle) * 0.28));
            int shade = 105 + (i % 5) * 18;
            prism(pose, out, start, end, 0.06 + (i % 3) * 0.018,
                    rgba(shade, shade, shade + 5, 155));
        }
        ring(pose, out, b, 1.25 + Math.sin(age * 0.18) * 0.18, 0.06, 0.18, 56,
                rgba(174, 178, 186, 165), age * 0.07);
    }''',
"    private static void renderLightningField(": r'''    private static void renderLightningField(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, Random random) {
        ring(pose, out, b, 14.0, 0.035, 0.13, 112,
                rgba(188, 128, 255, 120), -age * 0.012);
        ring(pose, out, b, 9.0, 0.045, 0.055, 88,
                rgba(229, 207, 255, 95), age * 0.018);
        for (int i = 0; i < 8; i++) {
            double a = i * TAU / 8.0 + Math.sin(age * 0.05 + i) * 0.55;
            double r = 2.0 + (i % 4) * 3.0;
            Vec3 end = b.local(Math.cos(a) * r, 0.05, Math.sin(a) * r);
            Vec3 start = end.add(0.0, 8.5 + (i % 3) * 1.2, 0.0);
            jaggedBolt(pose, out, start, end, 11, 0.11,
                    rgba(236, 220, 255, 205), stateSeed(random, i, (int) age / 2));
        }
    }''',
"    private static void renderShieldCharge(": r'''    private static void renderShieldCharge(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double forward = 1.15 + progress * 1.65;
        Vec3 center = b.local(0.0, 1.25, forward);
        curvedShield(pose, out, b, center, 3.5, 3.1, 0.58,
                rgba(62, 157, 255, 205));
        curvedShield(pose, out, b, center.add(b.forward.scale(0.055)), 2.75, 2.35, 0.46,
                rgba(171, 227, 255, 125));
        shieldFrame(pose, out, b, center.add(b.forward.scale(0.09)), 3.5, 3.1, 0.58,
                rgba(220, 246, 255, 220));
    }''',
"    private static void renderTaunt(": r'''    private static void renderTaunt(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        for (int i = 0; i < 4; i++) {
            ring(pose, out, b, 1.0 + progress * (7.5 + i * 1.4),
                    0.45 + i * 0.24, 0.11, 80,
                    rgba(78, 171, 255, (int) ((185 - i * 24) * (1.0 - progress))),
                    age * 0.012);
        }
        sphere(pose, out, b.local(0.0, 1.1, 0.0), 0.65 + progress * 0.9,
                8, 14, rgba(150, 218, 255, (int) (80 * (1.0 - progress))));
    }''',
"    private static void renderFortress(": r'''    private static void renderFortress(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress, boolean compact) {
        double width = compact ? 5.2 : 17.5;
        double height = compact ? 4.0 : 11.5;
        double distance = compact ? 2.35 : 5.0;
        double curve = compact ? 0.62 : 1.45;
        Vec3 center = b.local(0.0, height * 0.48, distance);
        curvedShield(pose, out, b, center, width, height, curve,
                rgba(48, 137, 244, compact ? 210 : 195));
        curvedShield(pose, out, b, center.add(b.forward.scale(0.07)),
                width * 0.88, height * 0.84, curve * 0.82,
                rgba(133, 209, 255, compact ? 115 : 105));
        shieldFrame(pose, out, b, center.add(b.forward.scale(0.12)),
                width, height, curve, rgba(220, 246, 255, 220));
        int ribs = compact ? 3 : 7;
        for (int i = 1; i <= ribs; i++) {
            double u = i / (double) (ribs + 1);
            Vec3 low = shieldPoint(b, center.add(b.forward.scale(0.14)), width, height, curve, u, 0.05);
            Vec3 high = shieldPoint(b, center.add(b.forward.scale(0.14)), width, height, curve, u, 0.95);
            prism(pose, out, low, high, compact ? 0.045 : 0.07,
                    rgba(188, 232, 255, compact ? 120 : 105));
        }
    }''',
"    private static void renderPath(": r'''    private static void renderPath(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress, int rgb, boolean healing) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        int color = rgba((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                (int) (205 * (1.0 - progress * 0.65)));
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i).subtract(origin);
            Vec3 b = points.get(i + 1).subtract(origin);
            braidedBeam(pose, out, a, b, age + i * 1.7,
                    healing ? 0.10 : 0.075, color);
        }
    }''',
"    private static void runeDisc(": r'''    private static void runeDisc(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double radius, double y, double phase, int color) {
        ring(pose, out, b, radius, y, Math.max(0.035, radius * 0.018), 72, color, phase);
        ring(pose, out, b, radius * 0.58, y + 0.006, Math.max(0.028, radius * 0.014), 56, color, -phase * 1.7);
        for (int i = 0; i < 6; i++) {
            double a0 = phase + i * TAU / 6.0;
            double a1 = phase + (i + 2) * TAU / 6.0;
            Vec3 p0 = b.local(Math.cos(a0) * radius * 0.62, y + 0.012,
                    Math.sin(a0) * radius * 0.62);
            Vec3 p1 = b.local(Math.cos(a1) * radius * 0.62, y + 0.012,
                    Math.sin(a1) * radius * 0.62);
            prism(pose, out, p0, p1, Math.max(0.018, radius * 0.010), color);
        }
    }''',
"    private static Vec3 shieldPoint(": r'''    private static Vec3 shieldPoint(
            Basis b, Vec3 center, double width, double height, double curve, double u, double v) {
        double x = (u - 0.5) * width;
        double y = (v - 0.5) * height;
        double edge = Math.pow(Math.abs(x) / Math.max(0.001, width * 0.5), 1.7);
        double z = -Math.abs(curve) * edge + 0.05 * Math.cos((v - 0.5) * Math.PI);
        return center.add(b.local(x, y, z));
    }''',
}
for marker, replacement in mesh_methods.items():
    text = replace_method(text, marker, replacement, f"mesh {marker.strip()}")

extra_mesh = r'''    private static void renderRangerFocus(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {
        double fade = envelope(progress, 0.08, 0.20);
        Vec3 core = b.local(0.0, 1.52, 0.65);
        sphere(pose, out, core, 0.18 + Math.sin(age * 0.25) * 0.035,
                8, 12, rgba(255, 211, 88, (int) (210 * fade)));
        ringVertical(pose, out, b, 0.48, 1.52, 0.035, 44,
                rgba(255, 235, 150, (int) (160 * fade)), age * 0.06);
    }

    private static void renderFireImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String encodedRadius) {
        double radius = 4.8;
        try { radius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        double spread = 0.45 + progress * radius;
        sphere(pose, out, Vec3.ZERO, Math.max(0.35, spread * 0.42), 10, 16,
                rgba(255, 78, 31, (int) (125 * (1.0 - progress))));
        ring(pose, out, b, spread, 0.10, 0.16, 72,
                rgba(255, 146, 52, (int) (220 * (1.0 - progress))), 0.0);
        ring(pose, out, b, spread * 0.72, 0.42, 0.09, 56,
                rgba(255, 229, 118, (int) (160 * (1.0 - progress))), age * 0.02);
    }

'''
insert_marker = "    private static void renderTargetLock("
idx = text.find(insert_marker)
if idx < 0:
    raise SystemExit("extra ranger mesh insertion marker missing")
text = text[:idx] + extra_mesh + text[idx:]

mesh_helpers = r'''    private static void horizontalSlash(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double width, double y, double thickness, double arch, int color) {
        int segments = 30;
        for (int i = 0; i < segments; i++) {
            double n0 = i / (double) segments;
            double n1 = (i + 1) / (double) segments;
            double t0 = n0 * 2.0 - 1.0;
            double t1 = n1 * 2.0 - 1.0;
            double x0 = t0 * width * 0.5;
            double x1 = t1 * width * 0.5;
            double center0 = y + (1.0 - t0 * t0) * arch;
            double center1 = y + (1.0 - t1 * t1) * arch;
            double half0 = Math.max(0.018, thickness * Math.sin(Math.PI * n0));
            double half1 = Math.max(0.018, thickness * Math.sin(Math.PI * n1));
            Vec3 p0 = b.local(x0, center0 - half0, 0.0);
            Vec3 p1 = b.local(x0, center0 + half0, 0.055);
            Vec3 p2 = b.local(x1, center1 + half1, 0.055);
            Vec3 p3 = b.local(x1, center1 - half1, 0.0);
            quadTwoSided(pose, out, p0, p1, p2, p3, color);
        }
    }

    private static void shieldFrame(
            PoseStack.Pose pose, VertexConsumer out, Basis b, Vec3 center,
            double width, double height, double curve, int color) {
        Vec3 bottomLeft = shieldPoint(b, center, width, height, curve, 0.0, 0.0);
        Vec3 topLeft = shieldPoint(b, center, width, height, curve, 0.0, 1.0);
        Vec3 topRight = shieldPoint(b, center, width, height, curve, 1.0, 1.0);
        Vec3 bottomRight = shieldPoint(b, center, width, height, curve, 1.0, 0.0);
        double thickness = Math.max(0.055, Math.min(width, height) * 0.025);
        prism(pose, out, bottomLeft, topLeft, thickness, color);
        prism(pose, out, topLeft, topRight, thickness, color);
        prism(pose, out, topRight, bottomRight, thickness, color);
        prism(pose, out, bottomRight, bottomLeft, thickness, color);
    }

'''
insert_marker = "    // ---- Mesh primitives"
idx = text.find(insert_marker)
if idx < 0:
    raise SystemExit("mesh helper insertion marker missing")
text = text[:idx] + mesh_helpers + text[idx:]
write(path, text)

# Separate skill row above the status action bar.
path = JAVA / "VillageNetwork.java"
text = read(path)
text = replace_once(
    text,
    "        registrar.playToClient(SkillMotionPayload.TYPE, SkillMotionPayload.STREAM_CODEC);\n",
    "        registrar.playToClient(SkillMotionPayload.TYPE, SkillMotionPayload.STREAM_CODEC);\n"
    "        registrar.playToClient(SkillHudPayload.TYPE, SkillHudPayload.STREAM_CODEC);\n",
    "skill HUD registration",
)
text = replace_once(
    text,
    "    public static void sendPlayerStatus(ServerPlayer player) {\n",
    "    public static void sendSkillHud(ServerPlayer player, String text) {\n"
    "        if (player == null) return;\n"
    "        PacketDistributor.sendToPlayer(player, new SkillHudPayload(text == null ? \"\" : text));\n"
    "    }\n\n"
    "    public static void sendPlayerStatus(ServerPlayer player) {\n",
    "skill HUD sender",
)
record_marker = "    public record VillageUiActionPayload(String action) implements CustomPacketPayload {"
skill_hud_record = r'''    public record SkillHudPayload(String text) implements CustomPacketPayload {
        public static final Type<SkillHudPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(VillageGuardians.MOD_ID, "skill_hud"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SkillHudPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, SkillHudPayload::text,
                        SkillHudPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

'''
text = replace_once(text, record_marker, skill_hud_record + record_marker, "skill HUD payload")
write(path, text)

path = JAVA / "VillageClientUi.java"
text = read(path)
text = replace_once(
    text,
    "        event.register(VillageNetwork.SkillMotionPayload.TYPE,\n"
    "                (payload, context) -> VillageSkillEffectClient.acceptMotion(payload));\n",
    "        event.register(VillageNetwork.SkillMotionPayload.TYPE,\n"
    "                (payload, context) -> VillageSkillEffectClient.acceptMotion(payload));\n"
    "        event.register(VillageNetwork.SkillHudPayload.TYPE,\n"
    "                (payload, context) -> VillageSkillHudOverlay.accept(payload));\n",
    "skill HUD client handler",
)
write(path, text)

path = JAVA / "VillageHudSystem.java"
text = read(path)
text = replace_once(
    text,
    "            LAST_TEXT.put(player.getUUID(), text);\n"
    "            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));\n",
    "            LAST_TEXT.put(player.getUUID(), text);\n"
    "            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(text)));\n"
    "            VillageNetwork.sendSkillHud(player, VillageRespawnSystem.isDowned(player)\n"
    "                    ? \"\" : buildSkillText(player));\n",
    "skill HUD server send",
)
old_skill = "        String skillHud = VillageRoleSkillSystem.hudSlotText(player, 0)\n                + \" §8· \" + VillageRoleSkillSystem.hudSlotText(player, 1);\n"
text = replace_once(text, old_skill, "", "remove skill HUD from status line")
text = replace_once(
    text,
    "                + \" §8│ §f\" + role\n"
    "                + \" §8│ \" + skillHud\n"
    "                + \" §8│ §e\" + VillageProgressionSystem.coins(player) + \"주화\"\n",
    "                + \" §8│ §f\" + role\n"
    "                + \" §8│ §e\" + VillageProgressionSystem.coins(player) + \"주화\"\n",
    "compact status line",
)
insert = r'''
    private static String buildSkillText(ServerPlayer player) {
        String base = VillageRoleSkillSystem.hudSlotText(player, 0)
                + " §8│ " + VillageRoleSkillSystem.hudSlotText(player, 1);
        String active = VillageRoleAbilitySystem.activeSkillHud(player);
        return active.isBlank() ? base : base + " §8│ " + active;
    }
'''
text = text[:-2] + insert + "}\n"
write(path, text)

# Verifier and all current static contracts move to the new version.
path = TOOLS / "verify_jar.py"
text = read(path)
text = replace_once(
    text,
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectSystem.class",\n',
    '    "kr/moonseungjun/villageguardians/VillageSkillEffectSystem.class",\n'
    '    "kr/moonseungjun/villageguardians/VillageSkillHudOverlay.class",\n',
    "HUD overlay verifier class",
)
write(path, text)

for test_path in TOOLS.glob("test_*.py"):
    test_text = read(test_path)
    test_text = test_text.replace("mod_version=0.17.14-alpha.1", "mod_version=0.17.15-alpha.1")
    write(test_path, test_text)

print("Applied Village Guardians v0.17.15 skill combat, mesh, shield and HUD overhaul")
