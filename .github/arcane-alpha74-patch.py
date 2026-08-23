from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
P = ROOT / 'projects/arcane-circle'


def rep(path, old, new, count=1):
    path = Path(path)
    text = path.read_text(encoding='utf-8')
    found = text.count(old)
    if found != count:
        raise SystemExit(f'{path}: expected {count} matches, found {found}: {old[:120]!r}')
    path.write_text(text.replace(old, new, count), encoding='utf-8')


def write(path, content):
    Path(path).write_text(content, encoding='utf-8')

# Version.
rep(P / 'gradle.properties', 'mod_version=0.12.1-alpha.73', 'mod_version=0.12.1-alpha.74')
rep(P / 'gradle.properties', '# alpha.73 second-circle authority audit: LOS Misty Step, apex-hover Levitate, maintained VFX/dispel cleanup', '# alpha.74 first-circle authority audit: NPC ward/light/sleep parity, exact grease/sleep footprints, bounded dispel lifecycle')
rep(P / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java', 'VERSION = "0.12.1-alpha.73"', 'VERSION = "0.12.1-alpha.74"')

arcane = P / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java'
rep(arcane,
'''        NeoForge.EVENT_BUS.addListener(ArcaneVitalityService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(FirstCircleSpellService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(SecondCircleSpellService::onIncomingDamage);''',
'''        NeoForge.EVENT_BUS.addListener(ArcaneVitalityService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(SecondCircleSpellService::onIncomingDamage);''')
rep(arcane,
'''        NeoForge.EVENT_BUS.addListener(HighUtilitySpellService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(SpellGameplayService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(ArcaneMageService::onIncomingDamage);''',
'''        NeoForge.EVENT_BUS.addListener(HighUtilitySpellService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(SpellGameplayService::onIncomingDamage);\n        // First-circle wake/ward resolution runs after higher-priority interceptors so a fully\n        // negated trajectory does not consume a 1C ward or wake Sleep.\n        NeoForge.EVENT_BUS.addListener(FirstCircleSpellService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(ArcaneMageService::onIncomingDamage);''')

first = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.java'
rep(first,
'''import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.effect.MobEffectInstance;''',
'''import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.tags.DamageTypeTags;\nimport net.minecraft.world.effect.MobEffectInstance;''')
rep(first,
'''    private static final int SLEEP_TICKS = 140;\n    private static final int GREASE_TICKS = 160;\n    private static final int GREASE_PULSE = 4;''',
'''    public static final int SLEEP_TICKS = 140;\n    public static final int GREASE_TICKS = 160;\n    public static final int SHIELD_TICKS = 170;\n    public static final int MAGE_ARMOR_TICKS = 720;\n    private static final int MAGE_ARMOR_RECHARGE_TICKS = 90;\n    private static final int GREASE_PULSE = 4;''')
rep(first,
'''    private static final List<GreaseZone> GREASE = new ArrayList<>();\n    private static final Map<UUID, SleepState> SLEEP = new HashMap<>();\n    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();''',
'''    private static final List<GreaseZone> GREASE = new ArrayList<>();\n    private static final Map<UUID, SleepState> SLEEP = new HashMap<>();\n    private static final Map<UUID, NpcWardState> NPC_SHIELD = new HashMap<>();\n    private static final Map<UUID, NpcWardState> NPC_MAGE_ARMOR = new HashMap<>();\n    private static final Map<UUID, NpcTimedState> NPC_FEATHER_FALL = new HashMap<>();\n    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();''')

rep(first,
'''            case "shield" -> {\n                caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 170, 1, true, false));\n                yield true;\n            }\n            case "feather_fall" -> {\n                caster.fallDistance = 0.0F;\n                caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false));\n                yield true;\n            }\n            case "light" -> {\n                caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0, true, false));\n                yield true;\n            }''',
'''            case "shield" -> npcShield(level, caster, power);\n            case "feather_fall" -> {\n                caster.fallDistance = 0.0F;\n                caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false));\n                NPC_FEATHER_FALL.put(caster.getUUID(), new NpcTimedState(level, caster.getUUID(), level.getGameTime() + 120));\n                yield true;\n            }\n            case "light" -> {\n                caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0, true, false));\n                ArcaneLightService.illuminate(caster, 1800);\n                yield true;\n            }''')
rep(first, '            case "sleep" -> sleepNpc(level, caster, designatedTarget, power);',
           '            case "sleep" -> sleepNpc(level, caster, range, power, snapshot.target());')
rep(first,
'''            case "mage_armor" -> {\n                caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 720, 0, true, false));\n                yield true;\n            }''',
'''            case "mage_armor" -> npcMageArmor(level, caster, power);''')

rep(first,
'''        tickGrease(level, now);\n        tickSleep(level, now);''',
'''        tickGrease(level, now);\n        tickSleep(level, now);\n        tickNpcWards(level, now);\n        tickNpcFeatherFall(level, now);\n        ArcaneLightService.tickNpc(level);''')

old_damage = '''    /** Any real hit wakes Sleep before the incoming damage is resolved. */\n    public static void onIncomingDamage(LivingIncomingDamageEvent event) {\n        if (event == null || event.getAmount() <= 0.0F) return;\n        SleepState state = SLEEP.remove(event.getEntity().getUUID());\n        if (state != null) restoreSleep(state);\n    }'''
new_damage = '''    /** Resolve 1C NPC wards late, then wake Sleep only when damage still survives interception. */\n    public static void onIncomingDamage(LivingIncomingDamageEvent event) {\n        if (event == null || event.getAmount() <= 0.0F || event.isCanceled()) return;\n        LivingEntity target = event.getEntity();\n        if (!(target instanceof ServerPlayer) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {\n            resolveNpcWard(target, event);\n            if (event.isCanceled() || event.getAmount() <= 0.0F) return;\n        }\n        SleepState state = SLEEP.remove(target.getUUID());\n        if (state != null) {\n            restoreSleep(state);\n            cancelSleepReleaseIfIdle(state.level, state.ownerId);\n        }\n    }'''
rep(first, old_damage, new_damage)

old_clear = '''    /** Dispel Magic wakes a sleeping subject without deleting fields that subject happens to own. */\n    public static boolean dispel(LivingEntity subject) {\n        if (subject == null) return false;\n        SleepState state = SLEEP.remove(subject.getUUID());\n        if (state == null) return false;\n        restoreSleep(state);\n        return true;\n    }\n\n    public static void clear(LivingEntity owner) {\n        if (owner != null) clear(owner.getUUID());\n    }\n\n    public static void clear(UUID ownerId) {\n        if (ownerId == null) return;\n        GREASE.removeIf(zone -> zone.ownerId.equals(ownerId));\n        Iterator<Map.Entry<UUID, SleepState>> iterator = SLEEP.entrySet().iterator();\n        while (iterator.hasNext()) {\n            SleepState state = iterator.next().getValue();\n            if (!state.ownerId.equals(ownerId)) continue;\n            restoreSleep(state);\n            iterator.remove();\n        }\n    }'''
new_clear = '''    /** Third-circle Dispel clears every maintained 1C authority owned by or attached to the subject. */\n    public static boolean dispel(LivingEntity subject) {\n        return clearMaintained(subject);\n    }\n\n    public static void clear(LivingEntity subject) {\n        clearMaintained(subject);\n    }\n\n    private static boolean clearMaintained(LivingEntity subject) {\n        if (subject == null) return false;\n        UUID id = subject.getUUID();\n        boolean changed = false;\n        Set<String> cancel = new java.util.HashSet<>();\n\n        if (GREASE.removeIf(zone -> zone.ownerId.equals(id))) { changed = true; cancel.add("grease"); }\n\n        Iterator<Map.Entry<UUID, SleepState>> ownedSleep = SLEEP.entrySet().iterator();\n        while (ownedSleep.hasNext()) {\n            SleepState state = ownedSleep.next().getValue();\n            if (!state.ownerId.equals(id)) continue;\n            restoreSleep(state);\n            ownedSleep.remove();\n            changed = true;\n            cancel.add("sleep");\n        }\n        SleepState received = SLEEP.remove(id);\n        if (received != null) {\n            restoreSleep(received);\n            cancelSleepReleaseIfIdle(received.level, received.ownerId);\n            changed = true;\n        }\n\n        if (NPC_SHIELD.remove(id) != null) { changed = true; cancel.add("shield"); }\n        if (NPC_MAGE_ARMOR.remove(id) != null) { changed = true; cancel.add("mage_armor"); }\n        if (NPC_FEATHER_FALL.remove(id) != null) {\n            subject.removeEffect(MobEffects.SLOW_FALLING);\n            changed = true;\n            cancel.add("feather_fall");\n        }\n\n        if (ArcaneBuffRuntime.clearSpell(subject, "shield")) changed = true;\n        if (ArcaneBuffRuntime.clearSpell(subject, "mage_armor")) changed = true;\n        if (subject instanceof ServerPlayer player && MageGearService.clearStableDescent(id)) {\n            player.removeEffect(MobEffects.SLOW_FALLING);\n            WorldMagicService.cancelRelease(player, "feather_fall");\n            changed = true;\n        }\n        if (ArcaneLightService.clear(subject)) { cancel.add("light"); changed = true; }\n\n        for (String spellId : cancel) WorldMagicService.cancelRelease(subject, spellId);\n        return changed;\n    }\n\n    public static void clear(UUID ownerId) {\n        if (ownerId == null) return;\n        GREASE.removeIf(zone -> zone.ownerId.equals(ownerId));\n        NPC_SHIELD.remove(ownerId);\n        NPC_MAGE_ARMOR.remove(ownerId);\n        NPC_FEATHER_FALL.remove(ownerId);\n        Iterator<Map.Entry<UUID, SleepState>> iterator = SLEEP.entrySet().iterator();\n        while (iterator.hasNext()) {\n            SleepState state = iterator.next().getValue();\n            if (!state.ownerId.equals(ownerId) && !state.target.getUUID().equals(ownerId)) continue;\n            restoreSleep(state);\n            iterator.remove();\n        }\n    }'''
rep(first, old_clear, new_clear)

rep(first,
'''        SLEEP.clear();\n        GREASE.clear();\n        LAST_TICK.clear();''',
'''        SLEEP.clear();\n        GREASE.clear();\n        NPC_SHIELD.clear();\n        NPC_MAGE_ARMOR.clear();\n        NPC_FEATHER_FALL.clear();\n        LAST_TICK.clear();''')

rep(first,
'''    private static boolean grease(ServerPlayer player, double range, CastTargetSnapshot snapshot) {\n        addGrease((ServerLevel) player.level(), player.getUUID(), snapshot.target(), SpellMetrics.effectRadius("grease", range, 1));\n        return true;\n    }\n\n    private static boolean sleep(ServerPlayer player, double range, double power, CastTargetSnapshot snapshot) {\n        ServerLevel level = (ServerLevel) player.level();\n        Vec3 center = snapshot.target();\n        double radius = Math.max(3.5, SpellMetrics.effectRadius("sleep", range, 1));\n        int affected = 0;\n        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,\n                new AABB(center, center).inflate(radius, Math.max(3.5, radius * .65), radius), value -> enemy(player, value))) {\n            if (!sleepEligible(target, power)) continue;\n            putToSleep(level, player.getUUID(), target, power);\n            affected++;\n        }\n        return affected > 0;\n    }\n\n    private static boolean sleepNpc(ServerLevel level, Mob caster, LivingEntity target, double power) {\n        if (!enemy(caster, target) || !sleepEligible(target, power)) return false;\n        putToSleep(level, caster.getUUID(), target, power);\n        return true;\n    }''',
'''    public static double greaseRadius(double range) {\n        return Math.max(2.5, Math.min(8.0, SpellMetrics.effectRadius("grease", range, 1)));\n    }\n\n    public static double sleepRadius(double range) {\n        return Math.max(3.5, SpellMetrics.effectRadius("sleep", range, 1));\n    }\n\n    private static boolean grease(ServerPlayer player, double range, CastTargetSnapshot snapshot) {\n        addGrease((ServerLevel) player.level(), player.getUUID(), snapshot.target(), greaseRadius(range));\n        return true;\n    }\n\n    private static boolean sleep(ServerPlayer player, double range, double power, CastTargetSnapshot snapshot) {\n        return sleepArea((ServerLevel) player.level(), player, range, power, snapshot.target());\n    }\n\n    private static boolean sleepNpc(ServerLevel level, Mob caster, double range, double power, Vec3 center) {\n        return sleepArea(level, caster, range, power, center);\n    }\n\n    private static boolean sleepArea(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {\n        double radius = sleepRadius(range);\n        int affected = 0;\n        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,\n                new AABB(center, center).inflate(radius, Math.max(3.5, radius * .65), radius), value -> enemy(caster, value))) {\n            double allowed = radius + target.getBbWidth() * .5;\n            double dx = target.getX() - center.x, dz = target.getZ() - center.z;\n            if (dx * dx + dz * dz > allowed * allowed || !sleepEligible(target, power)) continue;\n            putToSleep(level, caster.getUUID(), target, power);\n            affected++;\n        }\n        return affected > 0;\n    }''')

rep(first,
'''    private static void addGrease(ServerLevel level, UUID ownerId, Vec3 center, double radius) {\n        long now = level.getGameTime();\n        GREASE.add(new GreaseZone(level, ownerId, center, Math.max(2.5, Math.min(8.0, radius)), now + GREASE_TICKS, now));\n    }''',
'''    private static void addGrease(ServerLevel level, UUID ownerId, Vec3 center, double radius) {\n        long now = level.getGameTime();\n        GREASE.removeIf(zone -> zone.ownerId.equals(ownerId));\n        GREASE.add(new GreaseZone(level, ownerId, center, greaseRadius(radius), now + GREASE_TICKS, now));\n    }''')
# greaseRadius expects range; addGrease receives already-computed radius, so avoid re-running metrics.
rep(first, 'GREASE.add(new GreaseZone(level, ownerId, center, greaseRadius(radius), now + GREASE_TICKS, now));',
           'GREASE.add(new GreaseZone(level, ownerId, center, Math.max(2.5, Math.min(8.0, radius)), now + GREASE_TICKS, now));')
rep(first,
'''            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(owner, value))) {\n                double angle = Math.toRadians(Math.floorMod(target.getUUID().hashCode() + (int) now * 23, 360));''',
'''            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(owner, value))) {\n                double allowed = zone.radius + target.getBbWidth() * .5;\n                double dx = target.getX() - zone.center.x, dz = target.getZ() - zone.center.z;\n                if (dx * dx + dz * dz > allowed * allowed) continue;\n                double angle = Math.toRadians(Math.floorMod(target.getUUID().hashCode() + (int) now * 23, 360));''')

# Insert NPC ward runtime before frost().
rep(first,
'''    private static void frost(LivingEntity target, int slowTicks, int freezeBonus) {''',
'''    private static boolean npcShield(ServerLevel level, Mob caster, double power) {\n        NPC_SHIELD.put(caster.getUUID(), new NpcWardState(level, caster.getUUID(), level.getGameTime() + SHIELD_TICKS, power, 2, 0L));\n        return true;\n    }\n\n    private static boolean npcMageArmor(ServerLevel level, Mob caster, double power) {\n        long now = level.getGameTime();\n        NPC_MAGE_ARMOR.put(caster.getUUID(), new NpcWardState(level, caster.getUUID(), now + MAGE_ARMOR_TICKS, power, 4, now + MAGE_ARMOR_RECHARGE_TICKS));\n        return true;\n    }\n\n    private static void resolveNpcWard(LivingEntity target, LivingIncomingDamageEvent event) {\n        if (!(target.level() instanceof ServerLevel level)) return;\n        long now = level.getGameTime();\n        float amount = event.getAmount();\n        NpcWardState shield = NPC_SHIELD.get(target.getUUID());\n        if (shield != null && shield.level == level && shield.expiresAt > now && shield.charges > 0) {\n            shield.charges--;\n            amount -= (float) Math.max(3.0, 2.0 + shield.power * .18);\n            if (shield.charges <= 0) {\n                NPC_SHIELD.remove(target.getUUID());\n                WorldMagicService.cancelRelease(target, "shield");\n            }\n        }\n        NpcWardState armor = NPC_MAGE_ARMOR.get(target.getUUID());\n        if (armor != null && armor.level == level && armor.expiresAt > now) {\n            amount = (float) Math.max(0.0, amount * (armor.charges > 0 ? .68 : .84) - .8);\n            if (armor.charges > 0) {\n                armor.charges--;\n                if (armor.nextChargeAt <= now) armor.nextChargeAt = now + MAGE_ARMOR_RECHARGE_TICKS;\n            }\n        }\n        if (amount <= .05F) event.setCanceled(true); else event.setAmount(amount);\n    }\n\n    private static void tickNpcWards(ServerLevel level, long now) {\n        Iterator<Map.Entry<UUID, NpcWardState>> shields = NPC_SHIELD.entrySet().iterator();\n        while (shields.hasNext()) {\n            NpcWardState state = shields.next().getValue();\n            if (state.level != level) continue;\n            Entity raw = level.getEntity(state.entityId);\n            if (!(raw instanceof LivingEntity living) || !living.isAlive() || living.isRemoved() || now >= state.expiresAt) {\n                if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "shield");\n                shields.remove();\n            }\n        }\n        Iterator<Map.Entry<UUID, NpcWardState>> armor = NPC_MAGE_ARMOR.entrySet().iterator();\n        while (armor.hasNext()) {\n            NpcWardState state = armor.next().getValue();\n            if (state.level != level) continue;\n            Entity raw = level.getEntity(state.entityId);\n            if (!(raw instanceof LivingEntity living) || !living.isAlive() || living.isRemoved() || now >= state.expiresAt) {\n                if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "mage_armor");\n                armor.remove();\n                continue;\n            }\n            if (state.charges < 4 && now >= state.nextChargeAt) {\n                state.charges++;\n                state.nextChargeAt = now + MAGE_ARMOR_RECHARGE_TICKS;\n            }\n        }\n    }\n\n    private static void tickNpcFeatherFall(ServerLevel level, long now) {\n        Iterator<Map.Entry<UUID, NpcTimedState>> iterator = NPC_FEATHER_FALL.entrySet().iterator();\n        while (iterator.hasNext()) {\n            NpcTimedState state = iterator.next().getValue();\n            if (state.level != level) continue;\n            Entity raw = level.getEntity(state.entityId);\n            if (!(raw instanceof LivingEntity living) || !living.isAlive() || living.isRemoved() || now >= state.expiresAt) {\n                iterator.remove();\n            }\n        }\n    }\n\n    private static void cancelSleepReleaseIfIdle(ServerLevel level, UUID ownerId) {\n        if (SLEEP.values().stream().anyMatch(state -> state.ownerId.equals(ownerId))) return;\n        Entity raw = level.getEntity(ownerId);\n        if (raw instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, "sleep");\n    }\n\n    private static void frost(LivingEntity target, int slowTicks, int freezeBonus) {''')

# On timed sleep expiry, cancel the field if the last target woke/expired.
rep(first,
'''    private static void tickSleep(ServerLevel level, long now) {\n        Iterator<Map.Entry<UUID, SleepState>> iterator = SLEEP.entrySet().iterator();\n        while (iterator.hasNext()) {''',
'''    private static void tickSleep(ServerLevel level, long now) {\n        Set<UUID> finishedOwners = new java.util.HashSet<>();\n        Iterator<Map.Entry<UUID, SleepState>> iterator = SLEEP.entrySet().iterator();\n        while (iterator.hasNext()) {''')
rep(first,
'''                restoreSleep(state);\n                iterator.remove();\n                continue;''',
'''                restoreSleep(state);\n                iterator.remove();\n                finishedOwners.add(state.ownerId);\n                continue;''', 1)
rep(first,
'''            enforceSleep(target);\n        }\n    }\n\n    private static void enforceSleep''',
'''            enforceSleep(target);\n        }\n        for (UUID ownerId : finishedOwners) cancelSleepReleaseIfIdle(level, ownerId);\n    }\n\n    private static void enforceSleep''')

# Add state classes before GreaseZone.
rep(first,
'''    private static final class GreaseZone {''',
'''    private static final class NpcWardState {\n        final ServerLevel level; final UUID entityId; final long expiresAt; final double power; int charges; long nextChargeAt;\n        NpcWardState(ServerLevel level, UUID entityId, long expiresAt, double power, int charges, long nextChargeAt) {\n            this.level = level; this.entityId = entityId; this.expiresAt = expiresAt; this.power = power;\n            this.charges = charges; this.nextChargeAt = nextChargeAt;\n        }\n    }\n\n    private static final class NpcTimedState {\n        final ServerLevel level; final UUID entityId; final long expiresAt;\n        NpcTimedState(ServerLevel level, UUID entityId, long expiresAt) {\n            this.level = level; this.entityId = entityId; this.expiresAt = expiresAt;\n        }\n    }\n\n    private static final class GreaseZone {''')

# Arcane Light: generalize ownership to LivingEntity and add NPC ticking.
light = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneLightService.java'
rep(light,
'''import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.level.Level;''',
'''import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.entity.Entity;\nimport net.minecraft.world.entity.LivingEntity;\nimport net.minecraft.world.level.Level;''')
rep(light, '    public static void illuminate(ServerPlayer player, int durationTicks) {\n        ServerLevel level = (ServerLevel) player.level();\n        long now = level.getGameTime();\n        LightState state = ACTIVE.computeIfAbsent(player.getUUID(), ignored -> new LightState());\n        state.untilTick = Math.max(state.untilTick, now + Math.max(20, durationTicks));\n        refresh(player, state);\n    }',
'''    public static void illuminate(LivingEntity owner, int durationTicks) {\n        if (!(owner.level() instanceof ServerLevel level)) return;\n        long now = level.getGameTime();\n        LightState state = ACTIVE.computeIfAbsent(owner.getUUID(), ignored -> new LightState());\n        state.untilTick = Math.max(state.untilTick, now + Math.max(20, durationTicks));\n        refresh(owner, state);\n    }''')
rep(light,
'''    public static void clear(ServerPlayer player) {\n        LightState state = ACTIVE.remove(player.getUUID());\n        if (state != null) clear(player, state);\n    }''',
'''    public static boolean clear(LivingEntity owner) {\n        if (owner == null || !(owner.level() instanceof ServerLevel level)) return false;\n        LightState state = ACTIVE.remove(owner.getUUID());\n        if (state == null) return false;\n        clear(level.getServer(), state);\n        state.positions.clear();\n        state.dimension = null;\n        return true;\n    }\n\n    /** NPC Light is real world illumination too; players keep their direct per-player refresh path. */\n    public static void tickNpc(ServerLevel level) {\n        long now = level.getGameTime();\n        java.util.Iterator<Map.Entry<UUID, LightState>> iterator = ACTIVE.entrySet().iterator();\n        while (iterator.hasNext()) {\n            Map.Entry<UUID, LightState> entry = iterator.next();\n            LightState state = entry.getValue();\n            if (state.dimension != null && !state.dimension.equals(level.dimension())) continue;\n            Entity raw = level.getEntity(entry.getKey());\n            if (raw instanceof ServerPlayer) continue;\n            if (!(raw instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || now >= state.untilTick) {\n                clear(level.getServer(), state);\n                iterator.remove();\n                continue;\n            }\n            if (owner.tickCount % REFRESH_INTERVAL == 0) refresh(owner, state);\n        }\n    }''')
rep(light, '    private static void refresh(ServerPlayer player, LightState state) {\n        ServerLevel level = (ServerLevel) player.level();\n        MinecraftServer server = level.getServer();',
           '    private static void refresh(LivingEntity player, LightState state) {\n        ServerLevel level = (ServerLevel) player.level();\n        MinecraftServer server = level.getServer();')
# Existing private clear(ServerPlayer, state) still works for player tick; make it generic.
rep(light, '    private static void clear(ServerPlayer player, LightState state) {',
           '    private static void clear(LivingEntity player, LightState state) {')

# Feather Fall targeted cancellation without resetting unrelated gear state.
mage = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java'
rep(mage,
'''    public static void grantStableDescent(ServerPlayer player,int ticks){long now=((ServerLevel)player.level()).getServer().overworld().getGameTime();STABLE_DESCENT_UNTIL.merge(player.getUUID(),now+Math.max(1,ticks),Math::max);}\n''',
'''    public static void grantStableDescent(ServerPlayer player,int ticks){long now=((ServerLevel)player.level()).getServer().overworld().getGameTime();STABLE_DESCENT_UNTIL.merge(player.getUUID(),now+Math.max(1,ticks),Math::max);}\n    public static boolean clearStableDescent(UUID id){return id!=null&&STABLE_DESCENT_UNTIL.remove(id)!=null;}\n''')

# Shield visuals end with the second consumed barrier.
buff = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneBuffRuntime.java'
rep(buff,
'''            shield.charges--;\n            amount -= (float) Math.max(3.0, 2.0 + shield.power * .18);\n            chime(player, 1.35F + shield.charges * .12F);''',
'''            shield.charges--;\n            amount -= (float) Math.max(3.0, 2.0 + shield.power * .18);\n            chime(player, 1.35F + shield.charges * .12F);\n            if (shield.charges <= 0) {\n                STATES.remove(new BuffKey(player.getUUID(), "shield"));\n                WorldMagicService.cancelRelease(player, "shield");\n            }''')

# Accurate first-circle compendium.
summary = P / 'src/main/java/kr/moonseungjun/arcanecircle/magic/FirstCircleSpellSummary.java'
write(summary, '''package kr.moonseungjun.arcanecircle.magic;\n\n/** Alpha.74 exact first-circle gameplay contract used by the grimoire. */\npublic final class FirstCircleSpellSummary {\n    private FirstCircleSpellSummary() {}\n\n    public static String summary(String spellId) {\n        return switch (spellId) {\n            case "magic_missile" -> "조준한 생명체에 3발이 수렴하는 추적 비전 탄환 · 한 번의 합산 피해로 판정되어 저써클 정밀 필살 역할 유지";\n            case "fire_bolt" -> "비유도 화염탄 · 보이는 착탄점의 생명체에 단일 피해 + 화상 · 추적탄과 달리 실제 착탄 위치가 중요";\n            case "ray_of_frost" -> "단발 냉기 광선 피해 · 동결 + 강한 둔화 · 채널 다단히트 없이 즉시 냉각 제어";\n            case "shield" -> "약 8.5초 내 다음 충격 2회를 반응 방벽이 고정량 흡수 · 2장 소진 즉시 종료 · 플레이어/NPC 동일 규칙";\n            case "feather_fall" -> "6초 안정 낙하 · 즉시 누적 추락거리 초기화 · Dispel/사망/차원이동 시 해당 낙하 권능만 정리";\n            case "light" -> "90초 야간 시야 + 시전자 주변을 따라 이동하는 실제 임시 광원 5점 · 플레이어/NPC 모두 실제 월드 광원";\n            case "grease" -> "8초 단일 유지 미끄럼 영역 · 보이는 원형 반경 안에서만 약한 둔화 + 횡미끄러짐 · 재시전 시 이전 장판 교체";\n            case "sleep" -> "일반 체급 적만 최대 7초 원형 광역 수면 · 플레이어/NPC 동일 반경 · AI/이동/시전 정지 · 실제 피해가 남으면 즉시 각성";\n            case "thunderwave" -> "전방 부채꼴 충격파 피해 + 넉백 · 플레이어만 보이는 경로의 취약 블록을 조건부 파손하고 NPC는 지형 보존";\n            case "mage_armor" -> "36초 · 4장 재생형 아케인 플레이트가 피해를 분산하고 4.5초마다 재충전 · 플레이어/NPC 동일 규칙";\n            default -> "";\n        };\n    }\n}\n''')

# Exact authority overlays for the two 1C ground-area spells.
overlay = P / 'src/main/java/kr/moonseungjun/arcanecircle/client/FirstCircleAuthorityOverlay.java'
write(overlay, '''package kr.moonseungjun.arcanecircle.client;\n\nimport kr.moonseungjun.arcanecircle.magic.FirstCircleSpellService;\nimport kr.moonseungjun.arcanecircle.magic.SpellDefinition;\nimport net.minecraft.world.phys.Vec3;\n\n/** Alpha.74 exact-footprint overlay for first-circle ground authority. */\nfinal class FirstCircleAuthorityOverlay {\n    private FirstCircleAuthorityOverlay() {}\n\n    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 target,\n                                   double range, double elapsedSeconds, double durationSeconds) {\n        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(320);\n        if (spell == null || spell.circle() != 1) return m.build();\n        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();\n        double t = Math.max(0.0, elapsedSeconds);\n        if ("grease".equals(spell.id())) {\n            double radius = FirstCircleSpellService.greaseRadius(range);\n            Vec3 floor = target.add(0, .045, 0);\n            double slide = (t * .85) % 1.0;\n            m.circle(g, floor, radius, 52, .82F);\n            m.circle(g, floor.add(0, .018, 0), radius * (.32 + .50 * slide), 40, .24F);\n            for (int i = 0; i < 10; i++) {\n                double a = i * Math.PI * 2.0 / 10.0 + t * .16;\n                Vec3 a0 = floor.add(g.point(a, radius * .28));\n                Vec3 a1 = floor.add(g.point(a + .30, radius * .78));\n                m.line(a0, a1, .26F, .72F, .24F);\n            }\n            return m.build();\n        }\n        if ("sleep".equals(spell.id())) {\n            double radius = FirstCircleSpellService.sleepRadius(range);\n            Vec3 floor = target.add(0, .055, 0);\n            double pulse = .78 + .10 * Math.sin(t * 2.0);\n            m.circle(g, floor, radius, 52, .74F);\n            m.circle(g, floor.add(0, .02, 0), radius * pulse, 44, .22F);\n            for (int i = 0; i < 6; i++) {\n                double a = i * Math.PI * 2.0 / 6.0 + t * .08;\n                Vec3 p = floor.add(g.point(a, radius * .62)).add(0, .10 + .05 * Math.sin(t * 1.7 + i), 0);\n                m.diamond(g, p, Math.max(.18, radius * .055), -t * .10 + i, .56F, .28F);\n            }\n        }\n        return m.build();\n    }\n}\n''')

tracker = P / 'src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java'
rep(tracker,
'''                if(v.spell.circle()==2){\n                    ArcaneWorldMesh secondAuthority=SecondCircleAuthorityOverlay.release(v.spell,v.direction,targetOffset(v),''',
'''                if(v.spell.circle()==1){\n                    ArcaneWorldMesh firstAuthority=FirstCircleAuthorityOverlay.release(v.spell,v.direction,targetOffset(v),\n                            v.range,elapsedSeconds,durationSeconds);\n                    if(firstAuthority.size()>0)entries.add(new RenderEntry(center,firstAuthority,color,86,opacity));\n                }\n\n                if(v.spell.circle()==2){\n                    ArcaneWorldMesh secondAuthority=SecondCircleAuthorityOverlay.release(v.spell,v.direction,targetOffset(v),''')

# Metadata.
index_path = P / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
index = json.loads(index_path.read_text(encoding='utf-8'))
index['version'] = '0.12.1-alpha.74'
index['first_circle_deep_audit'] = [
    'magic_missile_locked_precision_burst',
    'fire_bolt_nonhoming_impact',
    'single_beam_ray_of_frost',
    'player_npc_two_charge_reactive_shield',
    'dispel_safe_feather_fall',
    'player_npc_refcounted_real_light',
    'single_owner_exact_radius_grease_slip',
    'player_npc_exact_radius_damage_wake_sleep',
    'physical_thunderwave_with_npc_terrain_safety',
    'player_npc_regenerating_mage_armor',
]
index['first_circle_preserved_authority'] = ['magic_missile','fire_bolt','ray_of_frost','feather_fall','thunderwave']
index['first_circle_value_pass_1'] = {
    'shield': '8.5s_two_reactive_barriers_player_npc_same_damage_contract',
    'light': '90s_five_point_refcounted_real_light_player_npc',
    'grease': '8s_single_owner_exact_radius_slip_field',
    'sleep': '7s_exact_radius_weak_target_aoe_player_npc_damage_wake',
    'mage_armor': '36s_four_regenerating_plates_player_npc_same_damage_contract',
}
index['first_circle_role_audit'] = {
    'magic_missile': 'locked_single_target_precision_burst',
    'fire_bolt': 'nonhoming_impact_burn_projectile',
    'ray_of_frost': 'instant_single_beam_freeze_slow',
    'shield': 'two_charge_reactive_flat_absorption',
    'feather_fall': 'temporary_safe_descent_and_fall_reset',
    'light': 'mobile_refcounted_real_world_illumination',
    'grease': 'single_fixed_slip_and_movement_disruption_zone',
    'sleep': 'weak_target_damage_break_area_incapacitation',
    'thunderwave': 'directional_damage_knockback_physical_wave',
    'mage_armor': 'long_regenerating_plate_damage_smoothing',
}
index['first_circle_dispel_scope'] = 'all_owned_or_attached_circle_1_maintenance'
index['first_circle_visual_hitbox_lifetime_sync'] = 'grease_sleep_exact_footprints_and_maintained_release_cleanup'
index['first_circle_npc_terrain_safety'] = 'thunderwave_keeps_combat_wave_but_skips_npc_world_edit'
index['first_circle_npc_parity'] = True
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Source verifier alpha.74 gates.
test = P / 'tools/test_current_source.py'
rep(test, "need(gradle, 'mod_version=0.12.1-alpha.73')", "need(gradle, 'mod_version=0.12.1-alpha.74')")
rep(test, "need(main, 'VERSION = \"0.12.1-alpha.73\"')", "need(main, 'VERSION = \"0.12.1-alpha.74\"')")
rep(test, "assert index['version'] == '0.12.1-alpha.73'", "assert index['version'] == '0.12.1-alpha.74'")
marker = "# Alpha.73 second-circle authority/value pass.\n"
alpha74 = '''# Alpha.74 first-circle authority/value pass.\nfirst = text(magic / 'FirstCircleSpellService.java')\nfirst_summary = text(magic / 'FirstCircleSpellSummary.java')\nfirst_authority = text(client / 'FirstCircleAuthorityOverlay.java')\nlight = text(magic / 'ArcaneLightService.java')\nbuff = text(magic / 'ArcaneBuffRuntime.java')\nmage_gear = text(magic / 'MageGearService.java')\ntracker_1 = text(client / 'WorldMagicTracker.java')\nneed(first,\n     'NPC_SHIELD = new HashMap<>()', 'NPC_MAGE_ARMOR = new HashMap<>()',\n     'case "shield" -> npcShield(level, caster, power);', 'case "mage_armor" -> npcMageArmor(level, caster, power);',\n     'ArcaneLightService.illuminate(caster, 1800)', 'sleepNpc(level, caster, range, power, snapshot.target())',\n     'public static double greaseRadius(double range)', 'public static double sleepRadius(double range)',\n     'GREASE.removeIf(zone -> zone.ownerId.equals(ownerId));',\n     'dx * dx + dz * dz > allowed * allowed',\n     'ArcaneBuffRuntime.clearSpell(subject, "shield")', 'ArcaneBuffRuntime.clearSpell(subject, "mage_armor")',\n     'MageGearService.clearStableDescent(id)', 'ArcaneLightService.clear(subject)',\n     'cancelSleepReleaseIfIdle', 'resolveNpcWard(target, event)')\nneed(light, 'public static void illuminate(LivingEntity owner, int durationTicks)',\n     'public static boolean clear(LivingEntity owner)', 'public static void tickNpc(ServerLevel level)')\nneed(buff, 'STATES.remove(new BuffKey(player.getUUID(), "shield"));',\n     'WorldMagicService.cancelRelease(player, "shield");')\nneed(mage_gear, 'public static boolean clearStableDescent(UUID id)')\nneed(first_summary, '플레이어/NPC 동일 규칙', '플레이어/NPC 모두 실제 월드 광원',\n     '재시전 시 이전 장판 교체', '플레이어/NPC 동일 반경')\nneed(first_authority, '"grease".equals(spell.id())', 'FirstCircleSpellService.greaseRadius(range)',\n     '"sleep".equals(spell.id())', 'FirstCircleSpellService.sleepRadius(range)')\nneed(tracker_1, 'if(v.spell.circle()==1){', 'FirstCircleAuthorityOverlay.release(')\nassert main.index('SpellGameplayService::onIncomingDamage') < main.index('FirstCircleSpellService::onIncomingDamage')\nexpected1 = {\n    'shield': '8.5s_two_reactive_barriers_player_npc_same_damage_contract',\n    'light': '90s_five_point_refcounted_real_light_player_npc',\n    'grease': '8s_single_owner_exact_radius_slip_field',\n    'sleep': '7s_exact_radius_weak_target_aoe_player_npc_damage_wake',\n    'mage_armor': '36s_four_regenerating_plates_player_npc_same_damage_contract',\n}\nassert index['first_circle_value_pass_1'] == expected1\nroles1 = index['first_circle_role_audit']\nassert set(roles1) == {'magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor'}\nassert len(set(roles1.values())) == 10\nassert index['first_circle_dispel_scope'] == 'all_owned_or_attached_circle_1_maintenance'\nassert index['first_circle_visual_hitbox_lifetime_sync'] == 'grease_sleep_exact_footprints_and_maintained_release_cleanup'\nassert index['first_circle_npc_terrain_safety'] == 'thunderwave_keeps_combat_wave_but_skips_npc_world_edit'\nassert index['first_circle_npc_parity'] is True\n\n'''
rep(test, marker, alpha74 + marker)
rep(test,
"need(verify,\n     '0.12.1-alpha.73', 'SecondCircleSpellSummary.class'",
"need(verify,\n     '0.12.1-alpha.74', 'FirstCircleAuthorityOverlay.class', 'SecondCircleSpellSummary.class'")
rep(test,
"print('Arcane Circle current-source audit: PASS')\nprint('catalog_90_direct_19_fusion=PASS')\nprint('all_109_explicit_effect_summaries=PASS')",
"print('Arcane Circle current-source audit: PASS')\nprint('catalog_90_direct_19_fusion=PASS')\nprint('all_109_explicit_effect_summaries=PASS')\nprint('alpha74_first_circle_player_npc_ward_parity=PASS')\nprint('alpha74_first_circle_light_npc_world_parity=PASS')\nprint('alpha74_first_circle_grease_sleep_exact_footprints=PASS')\nprint('alpha74_first_circle_dispel_lifecycle=PASS')\nprint('alpha74_first_circle_role_audit=PASS')\nprint('alpha74_first_circle_value_pass_1=PASS')")

# JAR verifier.
verify = P / 'tools/verify_jar.py'
rep(verify, "'kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.class',", "'kr/moonseungjun/arcanecircle/magic/FirstCircleSpellService.class',\n    'kr/moonseungjun/arcanecircle/client/FirstCircleAuthorityOverlay.class',")
rep(verify, "if version != '0.12.1-alpha.73':", "if version != '0.12.1-alpha.74':")
rep(verify, "raise SystemExit(f'unexpected alpha.73 package version: {version}')", "raise SystemExit(f'unexpected alpha.74 package version: {version}')")
insert = '''\n    expected1 = {\n        'shield': '8.5s_two_reactive_barriers_player_npc_same_damage_contract',\n        'light': '90s_five_point_refcounted_real_light_player_npc',\n        'grease': '8s_single_owner_exact_radius_slip_field',\n        'sleep': '7s_exact_radius_weak_target_aoe_player_npc_damage_wake',\n        'mage_armor': '36s_four_regenerating_plates_player_npc_same_damage_contract',\n    }\n    if index.get('first_circle_value_pass_1') != expected1:\n        raise SystemExit(f'alpha.74 first-circle value metadata mismatch: {index.get("first_circle_value_pass_1")}')\n    roles1 = index.get('first_circle_role_audit', {})\n    expected_roles1 = {'magic_missile','fire_bolt','ray_of_frost','shield','feather_fall','light','grease','sleep','thunderwave','mage_armor'}\n    if set(roles1) != expected_roles1 or len(set(roles1.values())) != 10:\n        raise SystemExit('alpha.74 first-circle role separation contract missing')\n    if index.get('first_circle_dispel_scope') != 'all_owned_or_attached_circle_1_maintenance':\n        raise SystemExit('alpha.74 first-circle dispel scope missing')\n    if index.get('first_circle_visual_hitbox_lifetime_sync') != 'grease_sleep_exact_footprints_and_maintained_release_cleanup':\n        raise SystemExit('alpha.74 first-circle visual/hitbox contract missing')\n    if index.get('first_circle_npc_terrain_safety') != 'thunderwave_keeps_combat_wave_but_skips_npc_world_edit':\n        raise SystemExit('alpha.74 first-circle NPC terrain safety missing')\n\n'''
rep(verify, "\n    expected2 = {\n", insert + "    expected2 = {\n")
rep(verify, "print('Arcane Circle alpha.73 JAR verification: PASS')", "print('Arcane Circle alpha.74 JAR verification: PASS')")
rep(verify,
"print('Arcane Circle alpha.74 JAR verification: PASS')\nprint('alpha73_second_circle_value_pass_1=PASS')",
"print('Arcane Circle alpha.74 JAR verification: PASS')\nprint('alpha74_first_circle_value_pass_1=PASS')\nprint('alpha74_first_circle_player_npc_ward_parity=PASS')\nprint('alpha74_first_circle_light_npc_world_parity=PASS')\nprint('alpha74_first_circle_grease_sleep_exact_footprints=PASS')\nprint('alpha74_first_circle_dispel_lifecycle=PASS')\nprint('alpha74_first_circle_role_separation=PASS')\nprint('alpha74_first_circle_npc_parity=PASS')\nprint('alpha73_second_circle_value_pass_1=PASS')")
