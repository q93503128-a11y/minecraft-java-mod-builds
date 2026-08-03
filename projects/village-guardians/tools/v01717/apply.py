#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def write(name: str, text: str) -> None:
    (JAVA / name).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 marker, found {count}")
    return text.replace(old, new, 1)


def sub_once(text: str, pattern: str, replacement: str, label: str) -> str:
    result, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 regex match, found {count}")
    return result


# ---------------------------------------------------------------------------
# Version and legacy contract migration
# ---------------------------------------------------------------------------
props = ROOT / "gradle.properties"
props_text = props.read_text(encoding="utf-8")
props_text = replace_once(
    props_text,
    "mod_version=0.17.16-alpha.1",
    "mod_version=0.17.17-alpha.1",
    "version",
)
props.write_text(props_text, encoding="utf-8")

for test in sorted((ROOT / "tools").glob("test_*.py")):
    source = test.read_text(encoding="utf-8")
    migrated = source.replace("mod_version=0.17.16-alpha.1", "mod_version=0.17.17-alpha.1")
    if migrated != source:
        test.write_text(migrated, encoding="utf-8")


# ---------------------------------------------------------------------------
# Event bridge: real bow/crossbow use-time acceleration
# ---------------------------------------------------------------------------
guardians = read("VillageGuardians.java")
guardians = replace_once(
    guardians,
    "import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n",
    "import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n"
    "import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;\n",
    "use item event import",
)
guardians = replace_once(
    guardians,
    "    @SubscribeEvent\n    public void onArrowLoose(ArrowLooseEvent event) {\n"
    "        VillageRoleAbilitySystem.handleArrowLoose(event);\n"
    "    }\n",
    "    @SubscribeEvent\n    public void onArrowLoose(ArrowLooseEvent event) {\n"
    "        VillageRoleAbilitySystem.handleArrowLoose(event);\n"
    "    }\n\n"
    "    @SubscribeEvent\n"
    "    public void onUseItemTick(LivingEntityUseItemEvent.Tick event) {\n"
    "        VillageRoleAbilitySystem.handleUseItemTick(event);\n"
    "    }\n",
    "use item event bridge",
)
write("VillageGuardians.java", guardians)


# ---------------------------------------------------------------------------
# Gameplay: exact range/visual metadata, homing arrows, collision-safe fire orb
# ---------------------------------------------------------------------------
ability = read("VillageRoleAbilitySystem.java")
ability = replace_once(
    ability,
    "import net.minecraft.world.item.ItemStack;\n",
    "import net.minecraft.world.item.BowItem;\n"
    "import net.minecraft.world.item.CrossbowItem;\n"
    "import net.minecraft.world.item.ItemStack;\n",
    "bow imports",
)
ability = replace_once(
    ability,
    "import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n",
    "import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n"
    "import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;\n",
    "ability use event import",
)
ability = replace_once(
    ability,
    "    private static final Map<UUID, Long> RICOCHET_ARROWS = new HashMap<>();\n",
    "    private static final Map<UUID, Long> RICOCHET_ARROWS = new HashMap<>();\n"
    "    private static final Map<UUID, TrackingArrowState> TRACKING_ARROWS = new HashMap<>();\n",
    "tracking arrow map",
)
ability = replace_once(
    ability,
    "        RICOCHET_ARROWS.clear();\n",
    "        RICOCHET_ARROWS.clear();\n        TRACKING_ARROWS.clear();\n",
    "tracking reset",
)
ability = replace_once(
    ability,
    "        tickPlayers(server, now);\n        tickScheduled(server, now);",
    "        tickPlayers(server, now);\n        tickTrackingArrows(server, now);\n        tickScheduled(server, now);",
    "tracking tick call",
)

# Targeted spells now use the actual look ray with only a maximum range.
ability = replace_once(
    ability,
    "            case ARCANIST_FIRE_ORB -> launchMoving(level, player, MovingKind.FIRE_ORB,\n"
    "                    new ItemStack(Items.FIRE_CHARGE), 1.35, 100,\n"
    "                    (12.0f + playerLevel * 0.65f) * power, 4.8, specialRank, sight);\n"
    "            case ARCANIST_FROST_RING -> {\n"
    "                Vec3 center = aimedGround(level, player, 18.0);\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.FROST, center,\n"
    "                        now + Math.max(140, duration), 7.5, power, specialRank, 0));\n"
    "                play(level, center, SoundEvents.GLASS_PLACE, 1.1f, 0.62f);\n"
    "            }\n"
    "            case ARCANIST_CHAIN -> {\n"
    "                Vec3 center = player.position().add(forward.scale(3.0));\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.TORNADO, center,\n"
    "                        now + Math.max(120, duration), 8.5, power, specialRank, 0));\n"
    "                play(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.1f, 0.72f);\n"
    "            }\n"
    "            case ARCANIST_NOVA -> {\n"
    "                Vec3 center = aimedGround(level, player, 22.0);\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.LIGHTNING, center,\n"
    "                        now + Math.max(100, duration / 2), 18.0, power, specialRank, 0));\n"
    "                play(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.85f, 1.15f);\n"
    "            }\n",
    "            case ARCANIST_FIRE_ORB -> launchFireOrb(level, player,\n"
    "                    1.35, 112, (12.0f + playerLevel * 0.65f) * power,\n"
    "                    areaRadius(4.8, specialRank), specialRank, sight);\n"
    "            case ARCANIST_FROST_RING -> {\n"
    "                double radius = areaRadius(7.5, specialRank);\n"
    "                Vec3 center = aimedGround(level, player, maximumRange(28.0, specialRank));\n"
    "                int until = Math.max(140, duration);\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.FROST, center,\n"
    "                        now + until, radius, power, specialRank, 0));\n"
    "                VillageSkillEffectSystem.frostField(level, player, center, until, radius, specialRank);\n"
    "                play(level, center, SoundEvents.GLASS_PLACE, 1.1f, 0.62f);\n"
    "            }\n"
    "            case ARCANIST_CHAIN -> {\n"
    "                double radius = areaRadius(8.5, specialRank);\n"
    "                Vec3 center = aimedGround(level, player, maximumRange(30.0, specialRank));\n"
    "                int until = Math.max(120, duration);\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.TORNADO, center,\n"
    "                        now + until, radius, power, specialRank, 0));\n"
    "                VillageSkillEffectSystem.tornadoField(level, player, center, forward, until, radius, specialRank);\n"
    "                play(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.1f, 0.72f);\n"
    "            }\n"
    "            case ARCANIST_NOVA -> {\n"
    "                double radius = areaRadius(18.0, specialRank);\n"
    "                Vec3 center = aimedGround(level, player, maximumRange(36.0, specialRank));\n"
    "                int until = Math.max(100, duration / 2);\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.LIGHTNING, center,\n"
    "                        now + until, radius, power, specialRank, 0));\n"
    "                VillageSkillEffectSystem.lightningField(level, player, center, until, radius, specialRank);\n"
    "                play(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.85f, 1.15f);\n"
    "            }\n",
    "targeted arcanist skills",
)
ability = replace_once(
    ability,
    "            case LUMINAR_VEIL -> {\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.HEALING, player.position(),\n"
    "                        now + Math.max(160, duration * 2L), 7.5, power, specialRank, 0));\n"
    "                play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f);\n"
    "            }\n",
    "            case LUMINAR_VEIL -> {\n"
    "                double radius = areaRadius(7.5, specialRank);\n"
    "                int until = Math.max(160, duration * 2);\n"
    "                AREAS.add(new AreaState(player.getUUID(), AreaKind.HEALING, player.position(),\n"
    "                        now + until, radius, power, specialRank, 0));\n"
    "                VillageSkillEffectSystem.healingField(level, player, player.position(), until, radius, specialRank);\n"
    "                play(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f);\n"
    "            }\n",
    "healing range metadata",
)

new_tick_areas = '''    private static void tickAreas(MinecraftServer server, long now) {
        Iterator<AreaState> iterator = AREAS.iterator();
        while (iterator.hasNext()) {
            AreaState area = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(area.owner());
            if (owner == null || !(owner.level() instanceof ServerLevel level) || now > area.until()) {
                iterator.remove();
                continue;
            }

            // Exactly three strike pulses per ten ticks: 1.5x the former two pulses.
            if (area.kind() == AreaKind.LIGHTNING) {
                int cycle = (int) Math.floorMod(now + area.phase(), 10L);
                if (cycle != 0 && cycle != 3 && cycle != 6) continue;
                float damage = (7.0f + VillageCouncilState.levelOf(owner.getUUID()) * 0.42f)
                        * area.power();
                double strikeRadius = areaRadius(4.8, area.specialRank());
                List<Mob> fieldTargets = targetsNear(level, owner, area.center(), area.radius(), 80);
                for (int strikeIndex = 0; strikeIndex < 2; strikeIndex++) {
                    Vec3 strike;
                    if (!fieldTargets.isEmpty() && owner.getRandom().nextFloat() < 0.90f) {
                        Mob preferred = fieldTargets.get(owner.getRandom().nextInt(fieldTargets.size()));
                        strike = preferred.position().add(
                                (owner.getRandom().nextDouble() - 0.5) * 1.4,
                                0.0,
                                (owner.getRandom().nextDouble() - 0.5) * 1.4);
                    } else {
                        strike = randomPointInCircle(level, area.center(), area.radius());
                    }
                    spawnVisualLightning(level, strike);
                    for (Mob target : targetsNear(level, owner, strike, strikeRadius, 28)) {
                        hurt(level, target, damage);
                        target.addEffect(new MobEffectInstance(
                                MobEffects.SLOWNESS, 30, 1, false, false, true));
                    }
                }
                continue;
            }

            if (now % 5L != area.phase() % 5L) continue;
            switch (area.kind()) {
                case FROST -> {
                    for (Mob target : targetsNear(level, owner, area.center(), area.radius(), 48)) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 3, false, false, true));
                        if (now % 20L == 0L) hurt(level, target, 2.2f * area.power());
                    }
                    if (now % 20L == 0L) play(level, area.center(), SoundEvents.GLASS_HIT, 0.55f, 0.62f);
                }
                case TORNADO -> {
                    Vec3 next = area.center().add(horizontalLook(owner).scale(0.24));
                    area.moveTo(next);
                    for (Mob target : targetsNear(level, owner, next, area.radius(), 48)) {
                        Vec3 pull = next.subtract(target.position());
                        Vec3 horizontal = new Vec3(pull.x, 0.0, pull.z);
                        if (horizontal.lengthSqr() > 0.01) horizontal = horizontal.normalize().scale(0.24);
                        target.push(horizontal.x, 0.20, horizontal.z);
                        target.hurtMarked = true;
                        if (now % 15L == 0L) hurt(level, target, 1.8f * area.power());
                    }
                    if (now % 15L == 0L) play(level, next, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8f, 0.78f);
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
                case LIGHTNING -> { /* handled above */ }
            }
        }
    }
'''
ability = sub_once(
    ability,
    r"    private static void tickAreas\(MinecraftServer server, long now\) \{.*?\n    \}\n\n    private static void tickMoving",
    new_tick_areas + "\n    private static void tickMoving",
    "tick areas",
)

new_tick_moving = '''    private static void tickMoving(MinecraftServer server, long now) {
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
            Vec3 previous = moving.lastPosition();
            Vec3 position = entity == null ? previous : entity.position();
            boolean blocked = false;
            if (entity != null && previous.distanceToSqr(position) > 1.0E-6) {
                var hit = level.clip(new net.minecraft.world.level.ClipContext(
                        previous, position,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        owner));
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    blocked = true;
                    position = hit.getLocation();
                }
            }
            moving.lastPosition(position);
            moving.age(moving.age() + 1);
            List<Mob> hits = targetsNear(level, owner, position, moving.radius(), 40);
            boolean expired = entity == null || !entity.isAlive() || blocked || moving.age() >= moving.maxAge();
            switch (moving.kind()) {
                case FIRE_ORB -> {
                    if (hits.isEmpty() && !expired) continue;
                    for (Mob target : targetsNear(level, owner, position, moving.radius(), 40)) {
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
            if (moving.effectId() != null) {
                Entity visual = level.getEntity(moving.effectId());
                if (visual != null) visual.discard();
            }
            iterator.remove();
        }
    }
'''
ability = sub_once(
    ability,
    r"    private static void tickMoving\(MinecraftServer server, long now\) \{.*?\n    \}\n\n    private static void cleanupExpired",
    new_tick_moving + "\n    private static void cleanupExpired",
    "tick moving",
)

# Tracking arrows continuously correct toward a predicted body position.
tracking_method = '''
    private static void tickTrackingArrows(MinecraftServer server, long now) {
        ServerLevel level = server.overworld();
        Iterator<Map.Entry<UUID, TrackingArrowState>> iterator = TRACKING_ARROWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackingArrowState> entry = iterator.next();
            TrackingArrowState state = entry.getValue();
            Entity arrowEntity = level.getEntity(entry.getKey());
            Entity targetEntity = level.getEntity(state.target());
            if (now > state.until()
                    || !(arrowEntity instanceof AbstractArrow arrow)
                    || !(targetEntity instanceof Mob target)
                    || !arrow.isAlive() || !target.isAlive()) {
                iterator.remove();
                continue;
            }
            Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
            Vec3 delta = body.subtract(arrow.position());
            if (delta.lengthSqr() < 0.05) continue;
            double speed = Math.max(2.4, arrow.getDeltaMovement().length());
            double leadTicks = Math.min(7.0, Math.sqrt(delta.lengthSqr()) / speed);
            Vec3 predicted = body.add(target.getDeltaMovement().scale(leadTicks * 0.62));
            Vec3 guided = predicted.subtract(arrow.position());
            if (guided.lengthSqr() < 1.0E-5) continue;
            arrow.setNoGravity(true);
            arrow.setDeltaMovement(guided.normalize().scale(speed));
            arrow.hurtMarked = true;
        }
    }
'''
ability = replace_once(
    ability,
    "    private static void tickScheduled(MinecraftServer server, long now) {",
    tracking_method + "\n    private static void tickScheduled(MinecraftServer server, long now) {",
    "tracking method insertion",
)

ability = replace_once(
    ability,
    "        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue() < now);\n",
    "        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue() < now);\n"
    "        TRACKING_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);\n",
    "tracking cleanup",
)

new_arrow_handlers = '''    public static void handleUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isRangerContext(player)) return;
        long now = player.level().getGameTime();
        if (RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) < now) return;
        ItemStack stack = event.getItem();
        if (!(stack.getItem() instanceof BowItem) && !(stack.getItem() instanceof CrossbowItem)) return;
        int special = VillageRoleSkillSystem.specialRank(player, VillageRole.RANGER);
        int acceleration = 3 + Math.min(2, special / 2);
        event.setDuration(Math.max(1, event.getDuration() - acceleration));
    }

    public static void handleArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isRangerContext(player)) return;
        long now = player.level().getGameTime();
        if (RAPID_UNTIL.getOrDefault(player.getUUID(), 0L) >= now) {
            event.setCharge(20);
            return;
        }
        event.setCharge(Math.min(20, event.getCharge() + 5));
    }
'''
ability = sub_once(
    ability,
    r"    public static void handleArrowLoose\(ArrowLooseEvent event\) \{.*?\n    \}\n\n    public static void handleEntityJoin",
    new_arrow_handlers + "\n    public static void handleEntityJoin",
    "bow charge handlers",
)
ability = replace_once(
    ability,
    "            RICOCHET_ARROWS.put(arrow.getUUID(), now + 240L);\n"
    "            aimAssist(level, player, arrow, 0.68);\n",
    "            RICOCHET_ARROWS.put(arrow.getUUID(), now + 240L);\n"
    "            Mob locked = lockArrowOnTarget(level, player, arrow);\n"
    "            if (locked != null) {\n"
    "                TRACKING_ARROWS.put(arrow.getUUID(),\n"
    "                        new TrackingArrowState(locked.getUUID(), now + 240L));\n"
    "            }\n",
    "tracking activation",
)
ability = replace_once(
    ability,
    "                    && RICOCHET_ARROWS.remove(directArrow.getUUID()) != null\n",
    "                    && RICOCHET_ARROWS.remove(directArrow.getUUID()) != null\n",
    "ricochet marker",
)
ability = replace_once(
    ability,
    "                List<Mob> chain = targetsNear(level, attacker, primary.position(), 12.0, 9);",
    "                TRACKING_ARROWS.remove(directArrow.getUUID());\n"
    "                List<Mob> chain = targetsNear(level, attacker, primary.position(),\n"
    "                        areaRadius(12.0, VillageRoleSkillSystem.specialRank(attacker, VillageRole.RANGER)), 12);",
    "tracking removal and chain scaling",
)

# Range-aware slam and rain visuals use the same radius as their hit checks.
ability = replace_once(
    ability,
    "        damageRadius(level, player, player.position(), 8.5, 32,\n"
    "                (14.0f + VillageCouncilState.levelOf(player.getUUID()) * 0.72f) * power,\n"
    "                false, 1.05, 0.38);\n"
    "        VillageSkillEffectSystem.slamImpact(level, player);",
    "        double radius = areaRadius(8.5, specialRank);\n"
    "        damageRadius(level, player, player.position(), radius, 40,\n"
    "                (14.0f + VillageCouncilState.levelOf(player.getUUID()) * 0.72f) * power,\n"
    "                false, 1.05, 0.38);\n"
    "        VillageSkillEffectSystem.slamImpact(level, player, radius, specialRank);",
    "slam exact radius",
)
ability = replace_once(
    ability,
    "        VillageSkillEffectSystem.arrowRainImpact(level, player, center);\n"
    "        float damage = (3.3f + VillageCouncilState.levelOf(player.getUUID()) * 0.18f) * power;\n"
    "        for (Mob target : targetsNear(level, player, center, 8.5, 40)) {",
    "        double radius = areaRadius(8.5, specialRank);\n"
    "        VillageSkillEffectSystem.arrowRainImpact(level, player, center, radius, specialRank);\n"
    "        float damage = (3.3f + VillageCouncilState.levelOf(player.getUUID()) * 0.18f) * power;\n"
    "        for (Mob target : targetsNear(level, player, center, radius, 48)) {",
    "rain exact radius",
)
ability = replace_once(
    ability,
    "        Vec3 center = aimedGround(level, player, 24.0);\n"
    "        int fieldDuration = 42;\n"
    "        VillageSkillEffectSystem.arrowRainField(level, player, center, fieldDuration, 8.5);",
    "        double radius = areaRadius(8.5, specialRank);\n"
    "        Vec3 center = aimedGround(level, player, maximumRange(30.0, specialRank));\n"
    "        int fieldDuration = 42;\n"
    "        VillageSkillEffectSystem.arrowRainField(\n"
    "                level, player, center, fieldDuration, radius, specialRank);",
    "rain field metadata",
)

# Collision-linked fire orb and moving-skill visual ownership.
launch_fire_orb = '''
    private static void launchFireOrb(
            ServerLevel level, ServerPlayer player, double speed, int maxAge,
            float damage, double radius, int specialRank, Vec3 direction) {
        Vec3 normalized = direction.normalize();
        Vec3 origin = player.getEyePosition().add(normalized.scale(0.8));
        var projectile = EntityTypes.SNOWBALL.create(level, EntitySpawnReason.EVENT);
        if (projectile == null) return;
        projectile.setOwner(player);
        projectile.setItem(ItemStack.EMPTY);
        projectile.setInvisible(true);
        projectile.setPos(origin.x, origin.y, origin.z);
        projectile.setNoGravity(true);
        projectile.setDeltaMovement(normalized.scale(speed));
        if (!level.addFreshEntity(projectile)) return;
        VillageSkillEffectEntity visual = VillageSkillEffectSystem.fireOrb(
                level, player, origin, normalized, maxAge, (float) speed, specialRank);
        MOVING.put(projectile.getUUID(), new MovingSkill(player.getUUID(), MovingKind.FIRE_ORB,
                maxAge, damage, radius, specialRank, origin,
                visual == null ? null : visual.getUUID()));
    }
'''
ability = replace_once(
    ability,
    "    private static void launchMoving(\n",
    launch_fire_orb + "\n    private static void launchMoving(\n",
    "launch fire orb insertion",
)
ability = replace_once(
    ability,
    "        MOVING.put(projectile.getUUID(), new MovingSkill(player.getUUID(), kind, maxAge,\n"
    "                damage, radius, specialRank, origin));",
    "        MOVING.put(projectile.getUUID(), new MovingSkill(player.getUUID(), kind, maxAge,\n"
    "                damage, radius, specialRank, origin, null));",
    "moving constructor effect id",
)

new_aim = '''    private static Mob lockArrowOnTarget(
            ServerLevel level, ServerPlayer player, AbstractArrow arrow) {
        Mob target = bestAimTarget(level, player, arrow.position(), 64.0);
        if (target == null) return null;
        Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
        Vec3 delta = body.subtract(arrow.position());
        if (delta.lengthSqr() < 1.0E-5) return null;
        double speed = Math.max(2.4, arrow.getDeltaMovement().length());
        arrow.setNoGravity(true);
        arrow.setDeltaMovement(delta.normalize().scale(speed));
        arrow.hurtMarked = true;
        return target;
    }

    private static void aimAssist(
            ServerLevel level, ServerPlayer player, AbstractArrow arrow, double strength) {
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 0.1) return;
        Mob target = bestAimTarget(level, player, arrow.position(), 48.0);
        if (target == null) return;
        Vec3 body = target.position().add(0.0, target.getBbHeight() * 0.58, 0.0);
        Vec3 assisted = body.subtract(arrow.position()).normalize();
        double safe = Math.max(0.0, Math.min(0.45, strength));
        Vec3 blended = velocity.normalize().scale(1.0 - safe).add(assisted.scale(safe)).normalize();
        arrow.setDeltaMovement(blended.scale(speed));
        arrow.hurtMarked = true;
    }

    private static Mob bestAimTarget(
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
ability = sub_once(
    ability,
    r"    private static void aimAssist\(.*?\n    \}\n\n\n    private static List<Mob> targetsNear",
    new_aim + "\n\n    private static List<Mob> targetsNear",
    "aim assistance",
)

# Helpers and records.
ability = replace_once(
    ability,
    "    private static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double distance) {",
    "    private static double areaRadius(double base, int specialRank) {\n"
    "        return base * (1.0 + Math.min(5, Math.max(0, specialRank)) * 0.08);\n"
    "    }\n\n"
    "    private static double maximumRange(double base, int specialRank) {\n"
    "        return base + Math.min(5, Math.max(0, specialRank)) * 3.0;\n"
    "    }\n\n"
    "    private static Vec3 aimPoint(ServerLevel level, ServerPlayer player, double distance) {",
    "range helpers",
)
ability = replace_once(
    ability,
    "        private final Set<UUID> hit = new HashSet<>();\n"
    "        private Vec3 lastPosition;",
    "        private final Set<UUID> hit = new HashSet<>();\n"
    "        private final UUID effectId;\n"
    "        private Vec3 lastPosition;",
    "moving effect field",
)
ability = replace_once(
    ability,
    "        private MovingSkill(UUID owner, MovingKind kind, int maxAge, float damage,\n"
    "                            double radius, int specialRank, Vec3 lastPosition) {\n"
    "            this.owner = owner;\n"
    "            this.kind = kind;\n"
    "            this.maxAge = maxAge;\n"
    "            this.damage = damage;\n"
    "            this.radius = radius;\n"
    "            this.specialRank = specialRank;\n"
    "            this.lastPosition = lastPosition;\n"
    "        }",
    "        private MovingSkill(UUID owner, MovingKind kind, int maxAge, float damage,\n"
    "                            double radius, int specialRank, Vec3 lastPosition, UUID effectId) {\n"
    "            this.owner = owner;\n"
    "            this.kind = kind;\n"
    "            this.maxAge = maxAge;\n"
    "            this.damage = damage;\n"
    "            this.radius = radius;\n"
    "            this.specialRank = specialRank;\n"
    "            this.lastPosition = lastPosition;\n"
    "            this.effectId = effectId;\n"
    "        }",
    "moving constructor",
)
ability = replace_once(
    ability,
    "        Set<UUID> hit() { return hit; }\n"
    "        Vec3 lastPosition() { return lastPosition; }",
    "        Set<UUID> hit() { return hit; }\n"
    "        UUID effectId() { return effectId; }\n"
    "        Vec3 lastPosition() { return lastPosition; }",
    "moving effect accessor",
)
ability = replace_once(
    ability,
    "    private record EmpoweredArrowState(long until, float power, int specialRank) {}\n",
    "    private record TrackingArrowState(UUID target, long until) {}\n\n"
    "    private record EmpoweredArrowState(long until, float power, int specialRank) {}\n",
    "tracking record",
)
write("VillageRoleAbilitySystem.java", ability)


# ---------------------------------------------------------------------------
# Scene dispatcher: one source of truth for radius/rank and short charge shield
# ---------------------------------------------------------------------------
effects = read("VillageSkillEffectSystem.java")
effects = replace_once(
    effects,
    "import net.minecraft.world.entity.Mob;\n",
    "import net.minecraft.world.entity.Mob;\n",
    "effect import anchor",
)
effects = replace_once(
    effects,
    "            case ARCANIST_FIRE_ORB -> spawn(level, player, \"arcanist_fire_orb\",\n"
    "                    player.getEyePosition().add(sight.scale(1.0)), sight, 100, 1.35f, \"\");\n"
    "            case ARCANIST_FROST_RING -> spawn(level, player, \"arcanist_frost\",\n"
    "                    player.position().add(forward.scale(18.0)), forward,\n"
    "                    Math.max(140, calculatedDuration), 0.0f, \"\");\n"
    "            case ARCANIST_CHAIN -> spawn(level, player, \"arcanist_tornado\",\n"
    "                    player.position().add(forward.scale(3.0)), forward,\n"
    "                    Math.max(120, calculatedDuration), 0.24f, \"\");\n"
    "            case ARCANIST_NOVA -> spawn(level, player, \"arcanist_lightning\",\n"
    "                    player.position().add(forward.scale(22.0)), forward,\n"
    "                    Math.max(100, calculatedDuration / 2), 0.0f, \"\");",
    "            case ARCANIST_FIRE_ORB, ARCANIST_FROST_RING, ARCANIST_CHAIN, ARCANIST_NOVA -> {\n"
    "                // Spawned at the real raycast origin/target by VillageRoleAbilitySystem.\n"
    "            }",
    "remove fixed arcanist visual origins",
)
effects = replace_once(
    effects,
    "            case LUMINAR_VEIL -> spawn(level, player, \"luminar_healing_field\",\n"
    "                    player.position(), forward, Math.max(160, calculatedDuration * 2), 0.0f, \"\");",
    "            case LUMINAR_VEIL -> {\n"
    "                // Radius-aware healing field is spawned by the gameplay system.\n"
    "            }",
    "remove fixed healing radius",
)
effects = replace_once(
    effects,
    "            case WARDEN_TAUNT -> spawn(level, player, \"warden_charge_cast\",\n"
    "                    player.position(), forward, 32, 0.0f, \"\");",
    "            case WARDEN_TAUNT -> {\n"
    "                // Short charge shield is spawned with the actual dash.\n"
    "            }",
    "remove duplicate charge shield",
)

# Replace effect methods with metadata-aware equivalents.
effects = sub_once(
    effects,
    r"    public static void slamImpact\(ServerLevel level, ServerPlayer player\) \{.*?\n    \}\n",
    '''    public static void slamImpact(
            ServerLevel level, ServerPlayer player, double radius, int specialRank) {
        spawn(level, player, "vanguard_slam_impact",
                player.position(), horizontal(player.getLookAngle()), 30, 0.0f,
                meta(radius, specialRank));
    }
''',
    "slam effect metadata",
)
effects = sub_once(
    effects,
    r"    public static void arrowRainImpact\(ServerLevel level, ServerPlayer player, Vec3 center\) \{.*?\n    \}\n",
    '''    public static void arrowRainImpact(
            ServerLevel level, ServerPlayer player, Vec3 center,
            double radius, int specialRank) {
        spawn(level, player, "ranger_rain_impact",
                center, horizontal(player.getLookAngle()), 10, 0.0f,
                meta(radius, specialRank));
    }
''',
    "rain impact metadata",
)
effects = sub_once(
    effects,
    r"    public static void shieldCharge\(ServerLevel level, ServerPlayer player, Vec3 direction\) \{.*?\n    \}\n",
    '''    public static void shieldCharge(ServerLevel level, ServerPlayer player, Vec3 direction) {
        spawn(level, player, "warden_charge_cast",
                player.position(), horizontal(direction), 12, 0.0f, "");
    }
''',
    "short charge shield",
)
effects = sub_once(
    effects,
    r"    public static void arrowRainField\(.*?\n    \}\n",
    '''    public static void arrowRainField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "ranger_rain_field", center, horizontal(player.getLookAngle()),
                Math.max(20, duration), 0.0f, meta(radius, specialRank));
    }
''',
    "rain field metadata",
)

area_effect_methods = '''
    public static VillageSkillEffectEntity fireOrb(
            ServerLevel level, ServerPlayer player, Vec3 origin, Vec3 direction,
            int duration, float speed, int specialRank) {
        return spawn(level, player, "arcanist_fire_orb", origin, normalized(direction),
                duration, speed, meta(0.0, specialRank));
    }

    public static void frostField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "arcanist_frost", center, horizontal(player.getLookAngle()),
                duration, 0.0f, meta(radius, specialRank));
    }

    public static void tornadoField(
            ServerLevel level, ServerPlayer player, Vec3 center, Vec3 direction,
            int duration, double radius, int specialRank) {
        spawn(level, player, "arcanist_tornado", center, horizontal(direction),
                duration, 0.24f, meta(radius, specialRank));
    }

    public static void lightningField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "arcanist_lightning", center, horizontal(player.getLookAngle()),
                duration, 0.0f, meta(radius, specialRank));
    }

    public static void healingField(
            ServerLevel level, ServerPlayer player, Vec3 center,
            int duration, double radius, int specialRank) {
        spawn(level, player, "luminar_healing_field", center, horizontal(player.getLookAngle()),
                duration, 0.0f, meta(radius, specialRank));
    }
'''
effects = replace_once(
    effects,
    "    public static void fireImpact(\n",
    area_effect_methods + "\n    public static void fireImpact(\n",
    "area effect methods",
)
effects = replace_once(
    effects,
    "    private static List<ServerPlayer> positions(List<ServerPlayer> players, Vec3 fallback) {",
    "    private static String meta(double radius, int specialRank) {\n"
    "        return String.format(Locale.ROOT, \"%.2f|%d\",\n"
    "                Math.max(0.0, radius), Math.max(0, specialRank));\n"
    "    }\n\n"
    "    private static List<ServerPlayer> positions(List<ServerPlayer> players, Vec3 fallback) {",
    "effect meta helper",
)
write("VillageSkillEffectSystem.java", effects)


# ---------------------------------------------------------------------------
# Procedural meshes: visible footprints exactly match gameplay radii.
# ---------------------------------------------------------------------------
mesh = read("VillageSkillMeshLibrary.java")
mesh = replace_once(
    mesh,
    "            case \"vanguard_slam_impact\" -> renderSlamImpact(pose, out, basis, age, progress, random);",
    "            case \"vanguard_slam_impact\" -> renderSlamImpact(pose, out, basis, age, progress, random, state.extra);",
    "slam switch extra",
)
mesh = replace_once(
    mesh,
    "            case \"ranger_rain_field\" -> renderArrowRainField(pose, out, basis, age, progress, random);\n"
    "            case \"ranger_rain_impact\" -> renderArrowRainImpact(pose, out, basis, age, progress, random);",
    "            case \"ranger_rain_field\" -> renderArrowRainField(pose, out, basis, age, progress, random, state.extra);\n"
    "            case \"ranger_rain_impact\" -> renderArrowRainImpact(pose, out, basis, age, progress, random, state.extra);",
    "rain switch extra",
)
mesh = replace_once(
    mesh,
    "            case \"arcanist_frost\" -> renderFrostField(pose, out, basis, age, progress);\n"
    "            case \"arcanist_tornado\" -> renderTornado(pose, out, basis, age, progress);\n"
    "            case \"arcanist_lightning\" -> renderLightningField(pose, out, basis, age, progress, random);",
    "            case \"arcanist_frost\" -> renderFrostField(pose, out, basis, age, progress, state.extra);\n"
    "            case \"arcanist_tornado\" -> renderTornado(pose, out, basis, age, progress, state.extra);\n"
    "            case \"arcanist_lightning\" -> renderLightningField(pose, out, basis, age, progress, random, state.extra);",
    "arcanist switch extra",
)
mesh = replace_once(
    mesh,
    "            case \"luminar_healing_field\" -> renderHealingField(pose, out, basis, age, progress);",
    "            case \"luminar_healing_field\" -> renderHealingField(pose, out, basis, age, progress, state.extra);",
    "healing switch extra",
)

mesh = sub_once(
    mesh,
    r"    private static void renderSlamImpact\(.*?\n    \}\n\n    private static void renderRapidFire",
    '''    private static void renderSlamImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double radius = 0.8 + progress * Math.max(0.2, meta.radius() - 0.8);
        ring(pose, out, b, radius, 0.035, 0.18 + progress * 0.22, 72,
                rgba(255, 74, 48, (int) (210 * (1.0 - progress))), age * 0.01);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.72, 0.042, 0.07, 56,
                    rgba(255, 161, 77, (int) (145 * (1.0 - progress))), -age * 0.014);
        }
        random.setSeed(random.nextLong() ^ 0x5A17L);
        int cracks = 18 + meta.rank() * 2;
        for (int i = 0; i < cracks; i++) {
            double a = i * TAU / cracks + random.nextDouble() * 0.16;
            double inner = 0.35 + random.nextDouble() * 0.4;
            double outer = radius * (0.72 + random.nextDouble() * 0.28);
            groundCrack(pose, out, b, a, inner, outer, 0.05 + random.nextDouble() * 0.06,
                    rgba(255, 103, 48, (int) (190 * (1.0 - progress))));
        }
    }

    private static void renderRapidFire''',
    "slam mesh",
)
mesh = sub_once(
    mesh,
    r"    private static void renderArrowRainField\(.*?\n    \}\n\n    private static void renderArrowRainImpact",
    '''    private static void renderArrowRainField(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double radius = meta.radius();
        ring(pose, out, b, radius, 0.012, 0.11, 112,
                rgba(88, 188, 255, 180), 0.0);
        ring(pose, out, b, radius * 0.72, 0.018, 0.045, 88,
                rgba(149, 223, 255, 95), -age * 0.008);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.42, 0.022, 0.035, 64,
                    rgba(210, 242, 255, 90), age * 0.012);
        }
        int arrows = 18 + meta.rank() * 3;
        for (int i = 0; i < arrows; i++) {
            double a = i * 2.399963229728653 + (i % 3) * 0.17;
            double r = Math.sqrt((i + 0.5) / arrows) * radius * 0.92;
            double cycle = fract(progress * 5.8 + i * 0.173);
            double y = 8.5 - cycle * 9.5;
            Vec3 p = b.local(Math.cos(a) * r, y, Math.sin(a) * r);
            double fadeOut = 1.0 - Math.max(0.0, cycle - 0.72) / 0.28;
            customArrow(pose, out, Basis.DOWN, p, 0.82 + (i % 4) * 0.07, 0.06,
                    rgba(164, 228, 255, (int) (195 * fadeOut)));
        }
    }

    private static void renderArrowRainImpact''',
    "rain field mesh",
)
mesh = sub_once(
    mesh,
    r"    private static void renderArrowRainImpact\(.*?\n    \}\n\n    private static void renderEnergyCharge",
    '''    private static void renderArrowRainImpact(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double pulse = Math.min(meta.radius(), 0.45 + progress * meta.radius());
        ring(pose, out, b, pulse, 0.012, 0.08, 72,
                rgba(124, 211, 255, (int) (145 * (1.0 - progress))), 0.0);
    }

    private static void renderEnergyCharge''',
    "rain impact mesh",
)
mesh = sub_once(
    mesh,
    r"    private static void renderFrostField\(.*?\n    \}\n\n    private static void renderTornado",
    '''    private static void renderFrostField(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra) {
        EffectMeta meta = effectMeta(extra, 7.5);
        double radius = meta.radius();
        runeDisc(pose, out, b, radius, 0.022, age * 0.012,
                rgba(112, 218, 255, 105));
        ring(pose, out, b, radius, 0.028, 0.10, 96,
                rgba(189, 246, 255, 165), -age * 0.018);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.62, 0.034, 0.055, 72,
                    rgba(220, 251, 255, 105), age * 0.024);
        }
        int crystals = 12 + meta.rank() * 2;
        for (int i = 0; i < crystals; i++) {
            double a = i * TAU / crystals + (i % 2) * 0.12;
            double r = radius * (0.28 + 0.58 * ((i % 5) / 4.0));
            double h = 0.7 + (i % 4) * 0.33 + Math.sin(age * 0.12 + i) * 0.12;
            crystal(pose, out, b.local(Math.cos(a) * r, 0.02, Math.sin(a) * r),
                    h, 0.18 + (i % 3) * 0.04,
                    rgba(146, 228, 255, 175));
        }
    }

    private static void renderTornado''',
    "frost mesh",
)
mesh = sub_once(
    mesh,
    r"    private static void renderTornado\(.*?\n    \}\n\n    private static void renderLightningField",
    '''    private static void renderTornado(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra) {
        EffectMeta meta = effectMeta(extra, 8.5);
        double scale = meta.radius() / 8.5;
        ring(pose, out, b, meta.radius(), 0.018, 0.085, 112,
                rgba(150, 155, 164, 115), age * 0.012);
        if (meta.rank() >= 3) {
            ring(pose, out, b, meta.radius() * 0.68, 0.024, 0.048, 88,
                    rgba(190, 194, 202, 90), -age * 0.018);
        }
        int strands = 8 + Math.min(4, meta.rank());
        for (int strand = 0; strand < strands; strand++) {
            double phase = strand * TAU / strands + age * (0.19 + strand * 0.006);
            int shade = 118 + (strand % 4) * 22;
            tornadoRibbon(pose, out, b, phase, 5.8 * Math.min(1.28, scale), 46,
                    rgba(shade, shade + 4, shade + 9, 125 + Math.min(8, strand) * 8));
        }
        int fragments = 24 + meta.rank() * 4;
        for (int i = 0; i < fragments; i++) {
            double cycle = fract(age * 0.035 + i * 0.117);
            double y = 0.18 + cycle * 5.4;
            double radius = (0.65 + cycle * 2.7 + (i % 3) * 0.14) * Math.min(1.35, scale);
            double angle = age * 0.17 + i * 2.399963229728653;
            Vec3 start = b.local(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Vec3 end = start.add(b.local(-Math.sin(angle) * 0.28, 0.10,
                    Math.cos(angle) * 0.28));
            int shade = 105 + (i % 5) * 18;
            prism(pose, out, start, end, 0.06 + (i % 3) * 0.018,
                    rgba(shade, shade, shade + 5, 155));
        }
    }

    private static void renderLightningField''',
    "tornado mesh",
)
mesh = sub_once(
    mesh,
    r"    private static void renderLightningField\(.*?\n    \}\n\n    private static void renderHealCast",
    '''    private static void renderLightningField(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, Random random, String extra) {
        EffectMeta meta = effectMeta(extra, 18.0);
        double radius = meta.radius();
        ring(pose, out, b, radius, 0.018, 0.15, 128,
                rgba(188, 128, 255, 145), -age * 0.012);
        ring(pose, out, b, radius * 0.55, 0.026, 0.06, 96,
                rgba(229, 207, 255, 105), age * 0.018);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.78, 0.032, 0.035, 112,
                    rgba(218, 183, 255, 85), -age * 0.026);
        }
        if (meta.rank() >= 5) {
            for (int i = 0; i < 12; i++) {
                double a = i * TAU / 12.0;
                Vec3 start = b.local(Math.cos(a) * radius * 0.90, 0.025,
                        Math.sin(a) * radius * 0.90);
                Vec3 end = b.local(Math.cos(a) * radius, 0.028,
                        Math.sin(a) * radius);
                prism(pose, out, start, end, 0.035,
                        rgba(238, 220, 255, 115));
            }
        }
        // Vertical worm-like procedural bolts were removed. Actual visual-only
        // Minecraft lightning entities now provide every strike column.
    }

    private static void renderHealCast''',
    "lightning mesh",
)
mesh = sub_once(
    mesh,
    r"    private static void renderHealingField\(.*?\n    \}\n\n    private static void renderMiracleCast",
    '''    private static void renderHealingField(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, String extra) {
        EffectMeta meta = effectMeta(extra, 7.5);
        double radius = meta.radius();
        runeDisc(pose, out, b, radius, 0.018, age * 0.008,
                rgba(255, 239, 153, 105));
        ring(pose, out, b, radius, 0.024, 0.09, 112,
                rgba(255, 248, 188, 145), age * 0.015);
        ring(pose, out, b, radius * 0.62, 0.032, 0.06, 88,
                rgba(255, 248, 188, 110), -age * 0.018);
        if (meta.rank() >= 3) {
            ring(pose, out, b, radius * 0.34, 0.040, 0.045, 64,
                    rgba(255, 255, 220, 100), age * 0.025);
        }
        int pillars = 8 + meta.rank() * 2;
        for (int i = 0; i < pillars; i++) {
            double a = i * TAU / pillars + age * 0.014;
            Vec3 root = b.local(Math.cos(a) * radius * 0.62, 0.05,
                    Math.sin(a) * radius * 0.62);
            verticalBlade(pose, out, b, root,
                    1.0 + 0.35 * Math.sin(age * 0.13 + i),
                    0.055, rgba(255, 255, 215, 115));
        }
    }

    private static void renderMiracleCast''',
    "healing mesh",
)
mesh = replace_once(
    mesh,
    "    private static void renderFallbackRune(\n",
    "    private static EffectMeta effectMeta(String encoded, double fallbackRadius) {\n"
    "        double radius = fallbackRadius;\n"
    "        int rank = 0;\n"
    "        if (encoded != null && !encoded.isBlank()) {\n"
    "            String[] parts = encoded.split(\"\\\\|\", -1);\n"
    "            try { if (parts.length > 0) radius = Double.parseDouble(parts[0]); }\n"
    "            catch (NumberFormatException ignored) {}\n"
    "            try { if (parts.length > 1) rank = Integer.parseInt(parts[1]); }\n"
    "            catch (NumberFormatException ignored) {}\n"
    "        }\n"
    "        return new EffectMeta(Math.max(0.25, radius), Math.max(0, rank));\n"
    "    }\n\n"
    "    private record EffectMeta(double radius, int rank) {}\n\n"
    "    private static void renderFallbackRune(\n",
    "effect meta parser",
)
write("VillageSkillMeshLibrary.java", mesh)

print("Applied Village Guardians v0.17.17 range, bow, homing, fire-orb and lightning fixes")
