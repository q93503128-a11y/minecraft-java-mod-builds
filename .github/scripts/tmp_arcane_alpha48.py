from pathlib import Path

root=Path('projects/arcane-circle')
java=root/'src/main/java/kr/moonseungjun/arcanecircle'
magic=java/'magic'
client=java/'client'

def read(path): return path.read_text(encoding='utf-8')
def write(path,s): path.write_text(s,encoding='utf-8')
def once(s,old,new,label):
    count=s.count(old)
    if count!=1: raise SystemExit(f'{label}: expected 1, got {count}')
    return s.replace(old,new,1)

# Fix the newly introduced explicit high-utility runtime before wiring it in.
p=magic/'HighUtilitySpellService.java'; s=read(p)
s=once(s,'if (source == null || source instanceof ServerPlayer) return false;','if (source == null) return false;','clone target guard')
s=once(s,'        ArcaneDamage.hurt(level, player, proxy, (float) Math.max(0.0, power * .08));\n','', 'polymorph gratuitous damage')
# Stashed originals stay at their real coordinates but become non-colliding/untargetable instead of
# being moved below the world where void damage could destroy the authoritative original.
s=once(s,'                original.isNoAi());','                original.isNoAi(), original.noPhysics);','polymorph state old nophysics')
s=once(s,'                target.isInvisible(), target.isInvulnerable(), target.isNoGravity(), target.isSilent(), target.isNoAi());','                target.isInvisible(), target.isInvulnerable(), target.isNoGravity(), target.isSilent(), target.isNoAi(), target.noPhysics);','maze state old nophysics')
s=once(s,'        restoreMobFlags(original, state.oldInvisible, state.oldInvulnerable, state.oldNoGravity,\n                state.oldSilent, state.oldNoAi);','        restoreMobFlags(original, state.oldInvisible, state.oldInvulnerable, state.oldNoGravity,\n                state.oldSilent, state.oldNoAi, state.oldNoPhysics);','restore polymorph flags')
s=once(s,'        restoreMobFlags(target, state.oldInvisible, state.oldInvulnerable, state.oldNoGravity,\n                state.oldSilent, state.oldNoAi);','        restoreMobFlags(target, state.oldInvisible, state.oldInvulnerable, state.oldNoGravity,\n                state.oldSilent, state.oldNoAi, state.oldNoPhysics);','restore maze flags')
s=once(s,'        mob.setNoAi(true);\n        mob.setInvisible(true);','        mob.setNoAi(true);\n        mob.noPhysics = true;\n        mob.addTag("arcanecircle_high_utility_stashed");\n        mob.setInvisible(true);','stash nophysics')
s=once(s,'        mob.setPersistenceRequired();\n        mob.snapTo(mob.getX(), -512.0, mob.getZ(), mob.getYRot(), mob.getXRot());','        mob.setPersistenceRequired();','remove void stash')
s=once(s,'    private static void restoreMobFlags(Mob mob, boolean invisible, boolean invulnerable,\n                                        boolean noGravity, boolean silent, boolean noAi) {\n        mob.setInvisible(invisible);','    private static void restoreMobFlags(Mob mob, boolean invisible, boolean invulnerable,\n                                        boolean noGravity, boolean silent, boolean noAi, boolean noPhysics) {\n        mob.noPhysics = noPhysics;\n        mob.removeTag("arcanecircle_high_utility_stashed");\n        mob.setInvisible(invisible);','restore nophysics signature')
s=once(s,'        return target instanceof Mob mob && mob.isAlive() && !mob.isRemoved() ? mob : null;','        return target instanceof Mob mob && mob.isAlive() && !mob.isRemoved()\n                && !mob.getTags().contains("arcanecircle_high_utility_stashed") ? mob : null;','target excludes stash')
# Store old noPhysics in both restoration records.
s=once(s,'        private final boolean oldNoAi;\n        private Vec3 lastPosition;','        private final boolean oldNoAi;\n        private final boolean oldNoPhysics;\n        private Vec3 lastPosition;','poly old nophysics field')
s=once(s,'                               boolean oldSilent, boolean oldNoAi) {','                               boolean oldSilent, boolean oldNoAi, boolean oldNoPhysics) {','poly ctor nophysics')
s=once(s,'            this.oldNoAi = oldNoAi;\n            this.lastPosition = anchor;','            this.oldNoAi = oldNoAi;\n            this.oldNoPhysics = oldNoPhysics;\n            this.lastPosition = anchor;','poly assign nophysics')
s=once(s,'        private final boolean oldNoAi;\n\n        private MazeState','        private final boolean oldNoAi;\n        private final boolean oldNoPhysics;\n\n        private MazeState','maze old nophysics field')
s=once(s,'                          boolean oldNoGravity, boolean oldSilent, boolean oldNoAi) {','                          boolean oldNoGravity, boolean oldSilent, boolean oldNoAi, boolean oldNoPhysics) {','maze ctor nophysics')
s=once(s,'            this.oldNoAi = oldNoAi;\n        }\n    }\n}','            this.oldNoAi = oldNoAi;\n            this.oldNoPhysics = oldNoPhysics;\n        }\n    }\n}','maze assign nophysics')
write(p,s)

# Route these four spells to the high-utility runtime exactly once.
p=magic/'SpellKineticsService.java'; s=read(p)
s=once(s,'        if (SpellGameplayService.handles(cast.spell().id())) {','        if (HighUtilitySpellService.handles(cast.spell().id()) || SpellGameplayService.handles(cast.spell().id())) {','utility single execution branch')
old='''        boolean gameplayOwned = SpellGameplayService.handles(spellId);\n        boolean executed = targetSnapshot.executeLocked(player, () -> gameplayOwned\n                ? SpellGameplayService.execute(player, spellId, range, power, targetSnapshot)\n                : SpellCastingService.executeResolved(player, spellId, range, power));\n        if (executed && !gameplayOwned) {\n            DestructiveMagicService.applyPhysicalAftermath(player, spellId, targetSnapshot, range, power);\n        }'''
new='''        boolean utilityOwned = HighUtilitySpellService.handles(spellId);\n        boolean gameplayOwned = !utilityOwned && SpellGameplayService.handles(spellId);\n        boolean executed = targetSnapshot.executeLocked(player, () -> utilityOwned\n                ? HighUtilitySpellService.execute(player, spellId, range, power, targetSnapshot)\n                : gameplayOwned ? SpellGameplayService.execute(player, spellId, range, power, targetSnapshot)\n                : SpellCastingService.executeResolved(player, spellId, range, power));\n        if (executed && !utilityOwned && !gameplayOwned) {\n            DestructiveMagicService.applyPhysicalAftermath(player, spellId, targetSnapshot, range, power);\n        }'''
s=once(s,old,new,'utility execute routing')
write(p,s)

# Remove obsolete ownership for clone/maze/true polymorph. Etherealness was not owned here.
p=magic/'SpellGameplayService.java'; s=read(p)
s=once(s,'            "clone", "control_weather", "dominate_monster", "earthquake", "incendiary_cloud", "maze", "sunburst",\n            "prismatic_wall", "shapechange", "true_polymorph", "weird", "foresight",','            "control_weather", "dominate_monster", "earthquake", "incendiary_cloud", "sunburst",\n            "prismatic_wall", "shapechange", "weird", "foresight",','gameplay handled utility removal')
s=once(s,'            case "maze" -> controlSingle(player, spellId, power, snapshot, 360);\n            case "true_polymorph" -> controlSingle(player, spellId, power, snapshot, 480);\n','', 'remove utility control switch')
s=once(s,'            case "clone" -> cloneWard(player, power);\n','', 'remove clone ward switch')
s=once(s,'            case "clone" -> 1800;','            case "clone" -> 80;','clone visual duration')
s=once(s,'            case "true_polymorph" -> ArcaneDamage.hurt((ServerLevel) player.level(), player, target, (float) (power * .50));\n','', 'remove true polymorph generic damage')
# Remove true-polymorph scale mutation from generic control engine; no high utility spell uses it anymore.
s=s.replace('            if (Double.isNaN(oldScale) && "true_polymorph".equals(kind)) { AttributeInstance scale = target.getAttribute(Attributes.SCALE); if (scale != null) { oldScale = scale.getBaseValue(); scale.setBaseValue(Math.max(.35, oldScale * .58)); } }\n','')
s=s.replace('            if ("true_polymorph".equals(kind)) { AttributeInstance scale = target.getAttribute(Attributes.SCALE); if (scale != null) { oldScale = scale.getBaseValue(); scale.setBaseValue(Math.max(.35, oldScale * .58)); } }\n','')
# Legacy cloneWard must not survive as a second semantic definition.
start='    private static boolean cloneWard(ServerPlayer player, double power) {'
if start not in s: raise SystemExit('cloneWard method missing')
a=s.index(start); b=s.index('    private static boolean controlWeather',a)
s=s[:a]+s[b:]
write(p,s)

# Clone is no longer a maintained self-buff; the persistent visual belongs to the spawned body, not caster armor.
p=client/'PersistentBuffRegalia.java'; s=read(p)
s=once(s,'            "globe_of_invulnerability", "simulacrum", "clone", "fire_shield", "solar_guard",','            "globe_of_invulnerability", "simulacrum", "fire_shield", "solar_guard",','remove clone regalia ownership')
s=once(s,'            case "clone" -> reserveBody(b, forward, right, up, back, t, true);\n','', 'remove clone reserve visual')
write(p,s)

# Lifecycle and damage event integration.
p=java/'ArcaneCircle.java'; s=read(p)
s=once(s,'import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;','import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;\nimport kr.moonseungjun.arcanecircle.magic.HighUtilitySpellService;','utility import')
s=once(s,'    public static final String VERSION = "0.12.1-alpha.47";','    public static final String VERSION = "0.12.1-alpha.48";','version main')
s=once(s,'        NeoForge.EVENT_BUS.addListener(ArcaneVitalityService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(SpellGameplayService::onIncomingDamage);','        NeoForge.EVENT_BUS.addListener(ArcaneVitalityService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(HighUtilitySpellService::onIncomingDamage);\n        NeoForge.EVENT_BUS.addListener(SpellGameplayService::onIncomingDamage);','damage listener')
s=s.replace('            ArcaneFieldService.clear(player.getUUID());\n            SpellGameplayService.clear(player);','            ArcaneFieldService.clear(player.getUUID());\n            HighUtilitySpellService.clear(player);\n            SpellGameplayService.clear(player);')
s=s.replace('        ArcaneFieldService.clear(player.getUUID());\n        SpellGameplayService.clear(player);','        ArcaneFieldService.clear(player.getUUID());\n        HighUtilitySpellService.clear(player);\n        SpellGameplayService.clear(player);')
s=once(s,'        SpellGameplayService.tick((ServerLevel) player.level());\n        // Run field suppression last','        SpellGameplayService.tick((ServerLevel) player.level());\n        HighUtilitySpellService.tick((ServerLevel) player.level());\n        // Run field suppression last','utility tick')
s=once(s,'        SpellGameplayService.clearAll();\n        ArcaneFieldService.clearAll();','        SpellGameplayService.clearAll();\n        HighUtilitySpellService.clearAll();\n        ArcaneFieldService.clearAll();','utility clear all')
write(p,s)

# Mechanical grimoire text must describe the actual runtime, including alpha.47 weather authority.
p=magic/'SpellEffectSummary.java'; s=read(p)
s=once(s,'            case "etherealness" -> "저항·투명화·느린 낙하로 생존력 대폭 강화";','            case "etherealness" -> "물질 충돌 위상화 + 자유 비행 · 일반 피해 88% 경감 · 종료 시 안전 낙하";','ethereal summary')
s=once(s,'            case "clone" -> "체력 완전 회복 · 90초 내 다음 치명상을 클론이 대신 받음";','            case "clone" -> "조준한 비플레이어 생명체의 실제 복제본 생성 · 장비/기초 전투체급 복제 · 시전자 소유 아님";','clone summary')
s=once(s,'            case "control_weather" -> "30초 실제 폭우·뇌우 지배 · 최대 6대상 연속 낙뢰 피해/강한 둔화";','            case "control_weather" -> "45초 실제 폭우·뇌우 지배 · G키로 바라본 지점 12연속 낙뢰 명령 · 재사용 2.5초";','weather summary')
s=once(s,'            case "maze" -> "조준 대상을 18초간 완전 격리 상태 + 실명·혼란";','            case "maze" -> "조준 생명체를 18초간 전장에서 실제 추방 · 종료 시 원래 전장으로 귀환";','maze summary')
s=once(s,'            case "true_polymorph" -> "대상 피해 · 24초 축소 변형 + AI·이동·Arcane 시전 완전 봉쇄";','            case "true_polymorph" -> "조준 생명체의 실제 몸체를 24초간 다른 생물로 교체 · 변신체가 쓰러지면 원형이 부상 상태로 복귀";','poly summary')
write(p,s)

# Version metadata.
p=root/'gradle.properties'; s=read(p)
s=once(s,'mod_version=0.12.1-alpha.47','mod_version=0.12.1-alpha.48','gradle version')
if '# alpha.47' in s:
    lines=s.splitlines()
    lines=[('# alpha.48 real clone, true polymorph, maze exile and ethereal phase runtime' if line.startswith('# alpha.47') else line) for line in lines]
    s='\n'.join(lines)+'\n'
write(p,s)
p=root/'src/main/resources/data/arcanecircle/spell_catalog/index.json'; s=read(p)
s=once(s,'"version": "0.12.1-alpha.47"','"version": "0.12.1-alpha.48"','index version')
write(p,s)

# Source audit: preserve every prior gate and add alpha.48 ownership/semantics checks.
p=root/'tools/test_current_source.py'; s=read(p)
s=s.replace("mod_version=0.12.1-alpha.47","mod_version=0.12.1-alpha.48")
s=s.replace('VERSION = "0.12.1-alpha.47"','VERSION = "0.12.1-alpha.48"')
s=s.replace('"version": "0.12.1-alpha.47"','"version": "0.12.1-alpha.48"')
s=s.replace('case "clone" -> 1800','case "clone" -> 80')
# Alpha.46 exact ownership expectations were superseded, not weakened.
s=s.replace("'case \"clone\" -> 1800'", "'case \"clone\" -> 80'")
s=s.replace("'case \"maze\" -> controlSingle(player, spellId, power, snapshot, 360)',", "")
s=s.replace("'case \"true_polymorph\" -> controlSingle(player, spellId, power, snapshot, 480)',", "")
append=r'''

# Alpha.48 high-circle utility identity: real bodies/state transitions, not potion/control aliases.
utility_path=magic/'HighUtilitySpellService.java'; assert utility_path.exists(); utility=text(utility_path)
for token in ['Set.of("clone", "true_polymorph", "maze", "etherealness")',
              'source.getType().create(level, EntitySpawnReason.EVENT)','copyCombatBody(source, clone)',
              'arcanecircle_clone_source_','복제체는 시전자 소유가 아니며 독립적으로 행동',
              'TRUE_POLYMORPH_TICKS = 480','EntityTypes.RABBIT.create','EntityTypes.CHICKEN.create',
              'EntityTypes.PIG.create','EntityTypes.SHEEP.create','restorePolymorph',
              'MAZE_TICKS = 360','arcanecircle_maze_exile','restoreMaze',
              'player.noPhysics = true','getAbilities().mayfly = true','getAbilities().flying = true',
              'event.getAmount() * 0.12F','arcanecircle_high_utility_stashed']:
    assert token in utility, token
assert 'snapTo(mob.getX(), -512.0' not in utility
assert 'ArcaneDamage.hurt(level, player, proxy' not in utility
kinetics=text(magic/'SpellKineticsService.java')
for token in ['HighUtilitySpellService.handles(cast.spell().id()) || SpellGameplayService.handles',
              'boolean utilityOwned = HighUtilitySpellService.handles(spellId)',
              'utilityOwned ? HighUtilitySpellService.execute']:
    assert token in kinetics, token
gameplay=text(magic/'SpellGameplayService.java')
handled_block=gameplay[gameplay.index('private static final Set<String> HANDLED'):gameplay.index('private static final Map<UUID, FlightState>')]
for retired_owner in ['"clone"','"true_polymorph"','"maze"']:
    assert retired_owner not in handled_block, retired_owner
assert 'case "clone" -> cloneWard' not in gameplay
assert 'case "maze" -> controlSingle' not in gameplay
assert 'case "true_polymorph" -> controlSingle' not in gameplay
assert 'case "clone" -> 80' in gameplay
regalia=text(client/'PersistentBuffRegalia.java')
maintained_block=regalia[regalia.index('MAINTAINED = Set.of'):regalia.index('private PersistentBuffRegalia')]
assert '"clone"' not in maintained_block and '"etherealness"' in maintained_block
assert 'case "clone" -> reserveBody' not in regalia
main=text(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java')
for token in ['HighUtilitySpellService::onIncomingDamage','HighUtilitySpellService.tick',
              'HighUtilitySpellService.clear(player)','HighUtilitySpellService.clearAll()']:
    assert token in main, token
summary=text(magic/'SpellEffectSummary.java')
for token in ['case "clone"','실제 복제본','시전자 소유 아님','case "true_polymorph"','실제 몸체',
              'case "maze"','전장에서 실제 추방','case "etherealness"','물질 충돌 위상화',
              'case "control_weather"','G키로 바라본 지점 12연속 낙뢰']:
    assert token in summary, token
print('alpha48_real_clone=PASS')
print('alpha48_true_polymorph_body_swap=PASS')
print('alpha48_maze_battle_exile=PASS')
print('alpha48_ethereal_phase_runtime=PASS')
print('alpha48_weather_description_parity=PASS')
'''
if 'alpha48_real_clone=PASS' not in s: s += append
write(p,s)

print('alpha48 integration applied')
