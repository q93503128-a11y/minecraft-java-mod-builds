from pathlib import Path
import subprocess
import time

ROOT = Path('projects/arcane-circle')
CLIENT = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/client'
MAGIC = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/magic'
WORLD = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/world'
WORKFLOW = Path('.github/workflows/build-arcane-circle.yml')
SELF = Path('.github/scripts/manual-vfx45.py')


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly 1 anchor, found {count}')
    return text.replace(old, new, 1)


def replace_all_exact(text, old, new, expected, label):
    count = text.count(old)
    if count != expected:
        raise SystemExit(f'{label}: expected {expected} anchors, found {count}')
    return text.replace(old, new)

# Version contract ------------------------------------------------------------------------------
p = ROOT / 'gradle.properties'
s = read(p)
s = replace_once(s, 'mod_version=0.12.1-alpha.44', 'mod_version=0.12.1-alpha.45', 'gradle version')
# Remove the temporary alpha.44 ignition wording if it is still the tail comment.
lines = s.splitlines()
if lines and lines[-1].startswith('# alpha.44'):
    lines[-1] = '# alpha.45 manual full-audit lifecycle consistency release'
s = '\n'.join(lines) + '\n'
write(p, s)

p = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java'
s = read(p)
s = replace_once(s, 'VERSION = "0.12.1-alpha.44"', 'VERSION = "0.12.1-alpha.45"', 'java version')
# Lifecycle cleanup must remove gameplay state and every lingering client release, not just charge VFX.
s = replace_all_exact(s, 'SpellGameplayService.clear(player.getUUID());', 'SpellGameplayService.clear(player);', 3, 'lifecycle gameplay clear')
s = replace_all_exact(s, '            WorldMagicService.stop(player);\n',
'''            WorldMagicService.stop(player);\n            WorldMagicService.clearVisuals(player);\n''', 3, 'lifecycle visual clear')
write(p, s)

p = ROOT / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
s = read(p)
s = replace_once(s, '"version": "0.12.1-alpha.44"', '"version": "0.12.1-alpha.45"', 'catalog version')
write(p, s)

# WorldMagicService: explicit release cancellation/clear and Etherealness lifetime parity. ----------
p = MAGIC / 'WorldMagicService.java'
s = read(p)
s = replace_once(s,
'''        int duration = "meteor_swarm".equals(spell.id())\n                ? MeteorBarragePattern.durationTicks(snapshot.barrageSeed())\n                : SpellPresentationProfile.releaseDurationTicks(spell, travelDistance);\n        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients().size(), center, target,\n''',
'''        int duration = "meteor_swarm".equals(spell.id())\n                ? MeteorBarragePattern.durationTicks(snapshot.barrageSeed())\n                : SpellPresentationProfile.releaseDurationTicks(spell, travelDistance);\n        // Etherealness is a true maintained 7C self-state. Its server duration scales with power,\n        // so the release geometry must live for that same authored duration instead of AURA's 28 ticks.\n        if ("etherealness".equals(spell.id())) duration = Math.max(duration, 360 + (int) cast.power());\n        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients().size(), center, target,\n''', 'ethereal visual lifetime')
s = replace_once(s,
'''    public static void stop(ServerPlayer player) { stop((LivingEntity) player); }\n\n    public static void stop(LivingEntity caster) {\n        NPC_RELEASES.remove(caster.getUUID());\n        PLAYER_CHARGE_SEEDS.remove(caster.getUUID());\n        NPC_CHARGE_SEEDS.remove(caster.getUUID());\n        send(caster, "kind=stop;caster=" + caster.getUUID());\n    }\n\n    public static void clearAll() {\n''',
'''    public static void stop(ServerPlayer player) { stop((LivingEntity) player); }\n\n    public static void stop(LivingEntity caster) {\n        NPC_RELEASES.remove(caster.getUUID());\n        PLAYER_CHARGE_SEEDS.remove(caster.getUUID());\n        NPC_CHARGE_SEEDS.remove(caster.getUUID());\n        send(caster, "kind=stop;caster=" + caster.getUUID());\n    }\n\n    /** Cancels one already-released spell without killing unrelated maintained magic. */\n    public static void cancelRelease(LivingEntity caster, String spellId) {\n        if (caster == null || spellId == null || spellId.isBlank()) return;\n        send(caster, "kind=cancel;caster=" + caster.getUUID() + ";spell=" + spellId);\n    }\n\n    /** Hard lifecycle boundary used on logout/respawn/dimension change. */\n    public static void clearVisuals(LivingEntity caster) {\n        if (caster == null) return;\n        send(caster, "kind=clear;caster=" + caster.getUUID());\n    }\n\n    public static void clearAll() {\n''', 'world visual lifecycle API')
write(p, s)

# WorldMagicTracker: generic living-entity attachment, singleton maintained releases, lifecycle reset. -
p = CLIENT / 'WorldMagicTracker.java'
s = read(p)
s = replace_once(s, 'import net.minecraft.world.entity.player.Player;', 'import net.minecraft.world.entity.LivingEntity;', 'tracker entity import')
s = replace_once(s, 'private static final int MAX_VISUALS = 10;', 'private static final int MAX_VISUALS = 32;', 'tracker capacity')
s = replace_once(s, '    private static final long CHARGE_TTL = 2_250_000_000L;\n',
'''    private static final long CHARGE_TTL = 2_250_000_000L;\n    private static Object LAST_LEVEL;\n''', 'tracker level identity field')
s = replace_once(s,
'''    public static void accept(WorldMagicPayload payload) {\n        Map<String,String> values=parse(payload.state());\n        String kind=values.getOrDefault("kind","");\n        UUID caster;\n        try{caster=UUID.fromString(values.getOrDefault("caster",""));}catch(Exception ignored){return;}\n        if("stop".equals(kind)){CHARGES.remove(caster);return;}\n\n        SpellDefinition spell=SpellCatalog.spell(values.getOrDefault("spell","")).orElse(null);\n''',
'''    public static void accept(WorldMagicPayload payload) {\n        syncLevelIdentity();\n        Map<String,String> values=parse(payload.state());\n        String kind=values.getOrDefault("kind","");\n        UUID caster;\n        try{caster=UUID.fromString(values.getOrDefault("caster",""));}catch(Exception ignored){return;}\n        if("stop".equals(kind)){CHARGES.remove(caster);return;}\n        if("clear".equals(kind)){\n            CHARGES.remove(caster);\n            RELEASES.removeIf(v->v.caster.equals(caster));\n            return;\n        }\n        if("cancel".equals(kind)){\n            String spellId=values.getOrDefault("spell","");\n            RELEASES.removeIf(v->v.caster.equals(caster)&&v.spell.id().equals(spellId));\n            return;\n        }\n\n        SpellDefinition spell=SpellCatalog.spell(values.getOrDefault("spell","")).orElse(null);\n''', 'tracker lifecycle packet handling')
s = replace_once(s,
'''        if("release".equals(kind)){\n            while(RELEASES.size()>=MAX_VISUALS)RELEASES.removeFirst();\n            boolean attached=followsCaster(spell);\n''',
'''        if("release".equals(kind)){\n            if(singletonRelease(spell))\n                RELEASES.removeIf(v->v.caster.equals(caster)&&v.spell.id().equals(spell.id()));\n            evictForCapacity();\n            boolean attached=followsCaster(spell);\n''', 'tracker release replacement')
s = replace_once(s,
'''    public static void onExtract(ExtractLevelRenderStateEvent event) {\n        long now=System.nanoTime();\n        CHARGES.values().removeIf(v->v.expiresAt<now);\n        RELEASES.removeIf(v->v.expiresAt<now);\n''',
'''    public static void onExtract(ExtractLevelRenderStateEvent event) {\n        syncLevelIdentity();\n        long now=System.nanoTime();\n        CHARGES.values().removeIf(v->v.expiresAt<now);\n        RELEASES.removeIf(v->v.expiresAt<now\n                ||(v.attached&&findLiving(v.caster)==null&&now-v.startedAt>500_000_000L));\n''', 'tracker extract lifecycle')
start = s.index('    private static boolean followsCaster(SpellDefinition spell){')
end = s.index('    private static float releaseOpacity(', start)
replacement = '''    private static boolean followsCaster(SpellDefinition spell){\n        if("time_stop".equals(spell.id()))return false;\n        if("antimagic_field".equals(spell.id())||"control_weather".equals(spell.id()))return true;\n        SpellPresentationProfile.SigilStyle sigil=SpellPresentationProfile.profile(spell).sigil();\n        return sigil==SpellPresentationProfile.SigilStyle.BODY_HALO\n                ||sigil==SpellPresentationProfile.SigilStyle.FEET_RUNE;\n    }\n\n    private static boolean singletonRelease(SpellDefinition spell){\n        if(followsCaster(spell)||"time_stop".equals(spell.id()))return true;\n        return switch(spell.id()){\n            case "grease","web","slow","sleet_storm","cloudkill","insect_plague",\n                    "incendiary_cloud","winter_domain","wall_of_fire","wall_of_force",\n                    "wind_wall","wall_of_ice","prismatic_wall" -> true;\n            default -> false;\n        };\n    }\n\n    private static void evictForCapacity(){\n        while(RELEASES.size()>=MAX_VISUALS){\n            int victim=-1;\n            for(int i=0;i<RELEASES.size();i++){\n                Visual v=RELEASES.get(i);\n                if(v.expiresAt-v.startedAt<4_000_000_000L){victim=i;break;}\n            }\n            if(victim<0)victim=0;\n            RELEASES.remove(victim);\n        }\n    }\n\n    private static Vec3 attachmentOffset(UUID caster,SpellDefinition spell,Vec3 originalCenter){\n        SpellPresentationProfile.Profile profile=SpellPresentationProfile.profile(spell);\n        if("control_weather".equals(spell.id()))return new Vec3(0,profile.skyHeight(),0);\n        LivingEntity entity=findLiving(caster);\n        if(entity!=null)return originalCenter.subtract(entity.position());\n        SpellPresentationProfile.SigilStyle sigil=profile.sigil();\n        if(sigil==SpellPresentationProfile.SigilStyle.BODY_HALO)return new Vec3(0,1.0,0);\n        if(sigil==SpellPresentationProfile.SigilStyle.FEET_RUNE||"antimagic_field".equals(spell.id()))return new Vec3(0,.055,0);\n        return Vec3.ZERO;\n    }\n\n    private static Vec3 renderCenter(Visual visual){\n        if(visual.attached){\n            LivingEntity entity=findLiving(visual.caster);\n            if(entity!=null)return entity.position().add(visual.attachOffset);\n        }\n        return visual.center;\n    }\n\n    private static LivingEntity findLiving(UUID id){\n        Minecraft minecraft=Minecraft.getInstance();\n        if(minecraft.player!=null&&minecraft.player.getUUID().equals(id))return minecraft.player;\n        if(minecraft.level==null||minecraft.player==null)return null;\n        return minecraft.level.getEntitiesOfClass(LivingEntity.class,\n                        minecraft.player.getBoundingBox().inflate(224.0), value->value.getUUID().equals(id)).stream()\n                .findFirst().orElse(null);\n    }\n\n    private static void syncLevelIdentity(){\n        Object current=Minecraft.getInstance().level;\n        if(current==LAST_LEVEL)return;\n        CHARGES.clear();\n        RELEASES.clear();\n        LAST_LEVEL=current;\n    }\n\n'''
s = s[:start] + replacement + s[end:]
write(p, s)

# ArcaneBuffRuntime: consumed or forcibly-cleared maintained buffs immediately end their visuals. ---
p = MAGIC / 'ArcaneBuffRuntime.java'
s = read(p)
s = replace_once(s,
'''            event.setCanceled(true);\n            chime(player, 1.55F);\n            return true;\n''',
'''            event.setCanceled(true);\n            chime(player, 1.55F);\n            WorldMagicService.cancelRelease(player, "invisibility");\n            return true;\n''', 'invisibility consumed visual')
s = replace_once(s,
'''    public static void clear(UUID playerId) {\n        STATES.keySet().removeIf(key -> key.playerId().equals(playerId));\n    }\n''',
'''    public static void clear(UUID playerId) {\n        Iterator<Map.Entry<BuffKey, State>> iterator = STATES.entrySet().iterator();\n        while (iterator.hasNext()) {\n            Map.Entry<BuffKey, State> entry = iterator.next();\n            if (!entry.getKey().playerId().equals(playerId)) continue;\n            State state = entry.getValue();\n            Entity raw = state.level.getEntity(playerId);\n            if (raw instanceof LivingEntity living) WorldMagicService.cancelRelease(living, state.spellId);\n            iterator.remove();\n        }\n    }\n''', 'buff clear visual lifecycle')
write(p, s)

# SpellKineticsService: cancelled authoritative impacts explicitly cancel already-released VFX. ------
p = MAGIC / 'SpellKineticsService.java'
s = read(p)
s = replace_once(s, 'import java.util.HashMap;\n', 'import java.util.HashMap;\nimport java.util.HashSet;\n', 'kinetics HashSet import')
s = replace_once(s, 'import java.util.Map;\n', 'import java.util.Map;\nimport java.util.Set;\n', 'kinetics Set import')
s = replace_once(s,
'''        while (queue.size() >= MAX_PENDING_PER_PLAYER) {\n            PendingCast dropped = queue.removeFirst();\n            SpellCastingService.finishKineticCast(player, dropped.cast(), dropped.growthSnapshot(),\n                    dropped.anyExecuted());\n        }\n''',
'''        while (queue.size() >= MAX_PENDING_PER_PLAYER) {\n            PendingCast dropped = queue.removeFirst();\n            WorldMagicService.cancelRelease(player, dropped.cast().spell().id());\n            SpellCastingService.finishKineticCast(player, dropped.cast(), dropped.growthSnapshot(),\n                    dropped.anyExecuted());\n        }\n''', 'kinetics overflow visual cancel')
s = replace_once(s,
'''        if (!player.isAlive() || player.isSpectator() || ArcaneFieldService.blocksCasting(player)) {\n            casts.clear();\n            PENDING.remove(player.getUUID());\n            WorldMagicService.stop(player);\n            return;\n        }\n''',
'''        if (!player.isAlive() || player.isSpectator() || ArcaneFieldService.blocksCasting(player)) {\n            cancel(player);\n            return;\n        }\n''', 'kinetics blocked cancel')
s = replace_once(s,
'''            if (!pending.targetSnapshot().validFor(player)) {\n                iterator.remove();\n                continue;\n            }\n''',
'''            if (!pending.targetSnapshot().validFor(player)) {\n                WorldMagicService.cancelRelease(player, pending.cast().spell().id());\n                iterator.remove();\n                continue;\n            }\n''', 'kinetics invalid target visual cancel')
s = replace_once(s,
'''    public static void clear(UUID playerId) {\n        PENDING.remove(playerId);\n    }\n\n    public static void clearAll() {\n''',
'''    public static void cancel(ServerPlayer player) {\n        List<PendingCast> casts=PENDING.remove(player.getUUID());\n        if(casts!=null&&!casts.isEmpty()){\n            Set<String> spellIds=new HashSet<>();\n            for(PendingCast pending:casts)spellIds.add(pending.cast().spell().id());\n            for(String spellId:spellIds)WorldMagicService.cancelRelease(player,spellId);\n        }\n        WorldMagicService.stop(player);\n    }\n\n    public static void clear(UUID playerId) {\n        PENDING.remove(playerId);\n    }\n\n    public static void clearAll() {\n''', 'kinetics cancel API')
write(p, s)

# SpellGameplayService: consumed wards and all cleared authoritative states end matching visuals. -----
p = MAGIC / 'SpellGameplayService.java'
s = read(p)
s = replace_once(s, 'import java.util.HashMap;\n', 'import java.util.HashMap;\nimport java.util.HashSet;\n', 'gameplay HashSet import')
s = replace_once(s,
'''            if (remaining <= 0) MIRRORS.remove(id); else MIRRORS.put(id, new MirrorState(remaining, mirror.expiresAt()));\n            event.setCanceled(true);\n''',
'''            if (remaining <= 0) {\n                MIRRORS.remove(id);\n                WorldMagicService.cancelRelease(player, "mirror_image");\n            } else MIRRORS.put(id, new MirrorState(remaining, mirror.expiresAt()));\n            event.setCanceled(true);\n''', 'mirror consumed visual')
s = replace_once(s,
'''            DEATH_WARDS.remove(id);\n            event.setCanceled(true);\n''',
'''            DEATH_WARDS.remove(id);\n            WorldMagicService.cancelRelease(player, death.kind());\n            event.setCanceled(true);\n''', 'death ward consumed visual')
old_clear = '''    public static void clear(UUID id) {\n        FlightState flight = FLIGHT.remove(id);\n        if (flight != null) revokeFlight(flight);\n        MIRRORS.remove(id); REDUCTION.remove(id); FIRE_SHIELDS.remove(id); DEATH_WARDS.remove(id);\n        ArcaneBuffRuntime.clear(id);\n        WeatherState weather = WEATHER.remove(id);\n        if (weather != null && WEATHER.values().stream().noneMatch(state -> state.level() == weather.level() && state.active())) {\n            setWeather(weather.level(), false, 100);\n        }\n        Iterator<ControlState> control = CONTROLS.values().iterator();\n        while (control.hasNext()) {\n            ControlState state = control.next();\n            if (!state.ownerId().equals(id) && !state.targetId().equals(id)) continue;\n            restoreControl(state); control.remove();\n        }\n        ZONES.removeIf(zone -> zone.ownerId.equals(id));\n    }\n'''
new_clear = '''    /** Clears gameplay state and cancels the matching maintained world-geometry releases. */\n    public static void clear(LivingEntity subject) {\n        if(subject==null)return;\n        UUID id=subject.getUUID();\n        Set<String> own=new HashSet<>();\n        if(FLIGHT.containsKey(id))own.add("fly");\n        if(MIRRORS.containsKey(id))own.add("mirror_image");\n        ReductionWard reduction=REDUCTION.get(id); if(reduction!=null)own.add(reduction.kind());\n        if(FIRE_SHIELDS.containsKey(id))own.add("fire_shield");\n        DeathWard death=DEATH_WARDS.get(id); if(death!=null)own.add(death.kind());\n        if(WEATHER.containsKey(id))own.add("control_weather");\n        for(ZoneState zone:ZONES)if(zone.ownerId.equals(id))own.add(zone.spellId);\n        for(ControlState state:CONTROLS.values()){\n            if(state.ownerId().equals(id))own.add(state.kind());\n            else if(state.targetId().equals(id)){\n                Entity rawOwner=state.level().getEntity(state.ownerId());\n                if(rawOwner instanceof LivingEntity livingOwner)WorldMagicService.cancelRelease(livingOwner,state.kind());\n            }\n        }\n        clear(id);\n        for(String spellId:own)WorldMagicService.cancelRelease(subject,spellId);\n    }\n\n    public static void clear(UUID id) {\n        FlightState flight = FLIGHT.remove(id);\n        if (flight != null) revokeFlight(flight);\n        MIRRORS.remove(id); REDUCTION.remove(id); FIRE_SHIELDS.remove(id); DEATH_WARDS.remove(id);\n        ArcaneBuffRuntime.clear(id);\n        WeatherState weather = WEATHER.remove(id);\n        if (weather != null && WEATHER.values().stream().noneMatch(state -> state.level() == weather.level() && state.active())) {\n            setWeather(weather.level(), false, 100);\n        }\n        Iterator<ControlState> control = CONTROLS.values().iterator();\n        while (control.hasNext()) {\n            ControlState state = control.next();\n            if (!state.ownerId().equals(id) && !state.targetId().equals(id)) continue;\n            restoreControl(state); control.remove();\n        }\n        ZONES.removeIf(zone -> zone.ownerId.equals(id));\n    }\n'''
s = replace_once(s, old_clear, new_clear, 'gameplay clear lifecycle')
s = replace_once(s,
'''if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false); SpellKineticsService.clear(player.getUUID()); WorldMagicService.stop(player);''',
'''if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false); SpellKineticsService.cancel(player);''',
'controlled player kinetic cancel')
write(p, s)

# ArcaneFieldService: visual-aware gameplay clear, authoritative pending cancel, overlapping Time Stop. --
p = MAGIC / 'ArcaneFieldService.java'
s = read(p)
s = replace_once(s,
'''        TimeField removed = TIME_FIELDS.remove(ownerId);\n        if (removed != null) restoreFrozenLevel(removed.level());\n''',
'''        TimeField removed = TIME_FIELDS.remove(ownerId);\n        // Recompute all remaining fields immediately. Restoring the whole level here gave a second\n        // active Time Stop a one-tick hole whenever another owner left or changed dimension.\n        if (removed != null) applyTimeStop(removed.level());\n''', 'overlapping time stop clear')
s = replace_once(s, '            SpellGameplayService.clear(entity.getUUID());', '            SpellGameplayService.clear(entity);', 'antimagic visual-aware clear')
s = replace_once(s,
'''        SpellKineticsService.clear(player.getUUID());\n        WorldMagicService.stop(player);\n''',
'''        SpellKineticsService.cancel(player);\n''', 'field player cancel')
write(p, s)

# ArcaneMageService: blocked NPCs stop at charge/pending-release stage, not only final impact. --------
p = WORLD / 'ArcaneMageService.java'
s = read(p)
s = replace_once(s, 'import kr.moonseungjun.arcanecircle.magic.ArcaneDamage;\n',
                 'import kr.moonseungjun.arcanecircle.magic.ArcaneDamage;\nimport kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;\n', 'npc field import')
s = replace_once(s,
'''        NpcCast cast = CASTS.get(caster.getUUID());\n        if (cast == null) return false;\n        Entity rawTarget = level.getEntity(cast.targetId());\n''',
'''        NpcCast cast = CASTS.get(caster.getUUID());\n        if (cast == null) return false;\n        if (ArcaneFieldService.blocksCasting(caster)) {\n            CASTS.remove(caster.getUUID());\n            if (cast.released()) WorldMagicService.cancelRelease(caster, cast.spellId());\n            WorldMagicService.stop(caster);\n            return false;\n        }\n        Entity rawTarget = level.getEntity(cast.targetId());\n''', 'npc charge block')
write(p, s)

# NPC Meteor Swarm: removing a blocked/dead barrage also removes the authoritative release visual. --
p = WORLD / 'NpcMeteorBarrageService.java'
s = read(p)
s = replace_once(s, 'import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;\n',
                 'import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;\nimport kr.moonseungjun.arcanecircle.magic.WorldMagicService;\n', 'npc meteor world magic import')
s = replace_once(s,
'''            if (!(rawCaster instanceof Mob caster) || !caster.isAlive()\n                    || ArcaneFieldService.blocksCasting(caster)\n                    || !barrage.targetSnapshot().validFor(caster)) {\n                iterator.remove();\n                continue;\n            }\n''',
'''            if (!(rawCaster instanceof Mob caster) || !caster.isAlive()\n                    || ArcaneFieldService.blocksCasting(caster)\n                    || !barrage.targetSnapshot().validFor(caster)) {\n                if (rawCaster instanceof LivingEntity livingCaster)\n                    WorldMagicService.cancelRelease(livingCaster, "meteor_swarm");\n                iterator.remove();\n                continue;\n            }\n''', 'npc meteor visual cancel')
write(p, s)

# Etherealness joins maintained high-circle buff grammar. -----------------------------------------
p = CLIENT / 'ArcaneSpellVisualOverhaul.java'
s = read(p)
s = replace_once(s,
'''            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "simulacrum", "clone",\n            "fire_shield", "solar_guard", "shapechange", "foresight");\n''',
'''            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "simulacrum", "clone",\n            "etherealness", "fire_shield", "solar_guard", "shapechange", "foresight");\n''', 'etherealness persistent grammar')
write(p, s)

# Audit: preserve all old gates, update version and require the manually-found lifecycle fixes. ----
p = ROOT / 'tools/test_current_source.py'
s = read(p)
s = s.replace('0.12.1-alpha.44', '0.12.1-alpha.45')
s = replace_once(s, "'SpellGameplayService.clear(entity.getUUID())'", "'SpellGameplayService.clear(entity)'", 'audit antimagic clear token')
append = r'''

# Alpha.45 manual full-audit lifecycle consistency.
tracker=text(client/'WorldMagicTracker.java')
for token in ['MAX_VISUALS = 32','LAST_LEVEL','syncLevelIdentity','singletonRelease','evictForCapacity',
              '"cancel".equals(kind)','"clear".equals(kind)','getEntitiesOfClass(LivingEntity.class',
              'SigilStyle.BODY_HALO','SigilStyle.FEET_RUNE','"control_weather".equals(spell.id())',
              'DETAIL_DISTANCE_SQR = 96.0 * 96.0','SILHOUETTE_DISTANCE_SQR = 160.0 * 160.0']:
    assert token in tracker, token
world_magic=text(magic/'WorldMagicService.java')
for token in ['cancelRelease(LivingEntity caster, String spellId)','clearVisuals(LivingEntity caster)',
              '"kind=cancel;caster="','"kind=clear;caster="','"etherealness".equals(spell.id())',
              '360 + (int) cast.power()']:
    assert token in world_magic, token
buff=text(magic/'ArcaneBuffRuntime.java')
for token in ['WorldMagicService.cancelRelease(player, "invisibility")',
              'WorldMagicService.cancelRelease(living, state.spellId)']:
    assert token in buff, token
field=text(magic/'ArcaneFieldService.java')
for token in ['if (removed != null) applyTimeStop(removed.level())','SpellGameplayService.clear(entity)',
              'SpellKineticsService.cancel(player)']:
    assert token in field, token
kinetics=text(magic/'SpellKineticsService.java')
for token in ['public static void cancel(ServerPlayer player)','Set<String> spellIds=new HashSet<>()',
              'WorldMagicService.cancelRelease(player, pending.cast().spell().id())']:
    assert token in kinetics, token
gameplay=text(magic/'SpellGameplayService.java')
for token in ['public static void clear(LivingEntity subject)','Set<String> own=new HashSet<>()',
              'WorldMagicService.cancelRelease(player, "mirror_image")','WorldMagicService.cancelRelease(player, death.kind())',
              'SpellKineticsService.cancel(player)']:
    assert token in gameplay, token
npc=text(world/'ArcaneMageService.java'); npc_barrage=text(world/'NpcMeteorBarrageService.java')
assert 'ArcaneFieldService.blocksCasting(caster)' in npc and 'WorldMagicService.cancelRelease(caster, cast.spellId())' in npc
assert 'WorldMagicService.cancelRelease(livingCaster, "meteor_swarm")' in npc_barrage
overhaul=text(client/'ArcaneSpellVisualOverhaul.java')
assert '"etherealness", "fire_shield"' in overhaul
assert main.count('SpellGameplayService.clear(player);') >= 3
assert main.count('WorldMagicService.clearVisuals(player);') >= 3
print('alpha45_manual_lifecycle_audit=PASS')
print('persistent_visual_singleton=PASS')
print('blocked_release_cancel=PASS')
print('overlapping_time_stop=PASS')
print('npc_charge_block=PASS')
print('etherealness_visual_lifetime=PASS')
'''
if 'alpha45_manual_lifecycle_audit=PASS' in s:
    raise SystemExit('alpha45 audit block already present')
s += append
write(p, s)

# Restore the canonical workflow from the parent commit, then remove this one-shot script. ----------
canonical = subprocess.check_output(
    ['git', 'show', 'HEAD^:.github/workflows/build-arcane-circle.yml'], text=True
)
write(WORKFLOW, canonical)
SELF.unlink()

# Commit/push the atomic alpha.45 source migration. ------------------------------------------------
subprocess.run(['git', 'config', 'user.name', 'github-actions[bot]'], check=True)
subprocess.run(['git', 'config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com'], check=True)
subprocess.run(['git', 'add', '-A', 'projects/arcane-circle', str(WORKFLOW), str(SELF)], check=True)
subprocess.run(['git', 'diff', '--cached', '--check'], check=True)
subprocess.run(['git', 'commit', '-m', 'fix(arcane-circle): stabilize alpha.45 maintained magic lifecycle'], check=True)
for attempt in range(1, 7):
    pushed = subprocess.run(['git', 'push', 'origin', 'HEAD:main']).returncode == 0
    if pushed:
        break
    subprocess.run(['git', 'pull', '--rebase', 'origin', 'main'], check=True)
    time.sleep(attempt * 2)
else:
    raise SystemExit('failed to push alpha.45 migration')
