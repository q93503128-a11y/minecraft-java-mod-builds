from __future__ import annotations

from pathlib import Path
from textwrap import dedent
import json

REPO = Path(__file__).resolve().parents[2]
ROOT = REPO / "projects" / "survival-ascension"
WORKFLOW = REPO / ".github" / "workflows" / "build-survival-ascension.yml"


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_once(rel: str, old: str, new: str) -> None:
    text = read(rel)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one anchor in {rel}: {old[:140]!r}; got {count}")
    write(rel, text.replace(old, new, 1))


def replace_all(rel: str, old: str, new: str) -> None:
    text = read(rel)
    if old not in text:
        raise SystemExit(f"missing replacement anchor in {rel}: {old[:140]!r}")
    write(rel, text.replace(old, new))


# Version + one-import content-preview lock. External mod versions remain unchanged.
replace_once("gradle.properties", "mod_version=0.55.0-alpha.1", "mod_version=0.56.0-alpha.1")
lock_path = ROOT / "modpack/content-lock.json"
lock = json.loads(lock_path.read_text(encoding="utf-8"))
if lock.get("version") != "0.55.0-alpha.1-content-preview.1":
    raise SystemExit(f"unexpected content lock version: {lock.get('version')!r}")
lock["version"] = "0.56.0-alpha.1-content-preview.1"
lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Ranged projectile snapshot now carries the firing player's UUID and can recover an online owner.
affix = "src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java"
replace_once(affix,
    "import java.util.List;\n",
    "import java.util.List;\nimport java.util.UUID;\n")
replace_once(affix,
    '    private static final String RANGED_PROJECTILE = "survivalascension_ranged_projectile";\n',
    '    private static final String RANGED_PROJECTILE = "survivalascension_ranged_projectile";\n    private static final String RANGED_OWNER = "survivalascension_ranged_owner";\n')
replace_once(affix,
    "    public static void snapshotRangedProjectile(Projectile projectile, ItemStack weapon, boolean precision) {\n        if (!isRangedWeapon(weapon)) return;\n        CompoundTag data = projectile.getPersistentData();\n        data.putBoolean(RANGED_PROJECTILE, true);\n        data.putBoolean(RANGED_PRECISION, precision);",
    "    public static void snapshotRangedProjectile(Projectile projectile, ServerPlayer player, ItemStack weapon, boolean precision) {\n        if (!isRangedWeapon(weapon)) return;\n        CompoundTag data = projectile.getPersistentData();\n        data.putBoolean(RANGED_PROJECTILE, true);\n        data.putString(RANGED_OWNER, player.getUUID().toString());\n        data.putBoolean(RANGED_PRECISION, precision);")
replace_once(affix,
    dedent('''\
    public static boolean isRangedProjectile(Entity direct) {
        return direct != null && direct.getPersistentData().getBooleanOr(RANGED_PROJECTILE, false);
    }

    public static boolean isPrecisionRangedProjectile(Entity direct) {
'''),
    dedent('''\
    public static boolean isRangedProjectile(Entity direct) {
        return direct != null && direct.getPersistentData().getBooleanOr(RANGED_PROJECTILE, false);
    }

    public static ServerPlayer rangedProjectileOwner(Entity direct, ServerLevel level) {
        if (!isRangedProjectile(direct)) return null;
        String raw = direct.getPersistentData().getStringOr(RANGED_OWNER, "");
        if (raw.isBlank()) return null;
        try {
            return level.getServer().getPlayerList().getPlayer(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isPrecisionRangedProjectile(Entity direct) {
'''))

# Affix elite drops use the same ranged-owner fallback when DamageSource no longer exposes the shooter.
replace_once(affix,
    dedent('''\
    public static void onEliteDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        int rankId = EliteMobSystem.rankId(mob);
'''),
    dedent('''\
    public static void onEliteDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        if (killer == null) return;
        int rankId = EliteMobSystem.rankId(mob);
'''))

# Core combat: stamp owner at launch, distinguish orphaned projectiles from environment damage,
# and recover the online shooter for hit scaling/burst and kill XP/major-target credit.
combat = "src/main/java/kr/moonseungjun/survivalascension/combat/CombatProgression.java"
replace_once(combat,
    "        AscensionAffixes.snapshotRangedProjectile(projectile, weapon, player.isShiftKeyDown());",
    "        AscensionAffixes.snapshotRangedProjectile(projectile, player, weapon, player.isShiftKeyDown());")
replace_once(combat,
    "            boolean environmental = event.getSource().getEntity() == null;",
    "            boolean environmental = event.getSource().getEntity() == null && event.getSource().getDirectEntity() == null;")
replace_once(combat,
    dedent('''\
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player || event.getAmount() <= 0.0F) return;
        UUID uuid = player.getUUID();
        if (CLEAVE_GUARD.contains(uuid) || SHOCKWAVE_GUARD.contains(uuid)) return;

        Entity direct = event.getSource().getDirectEntity();
        boolean rangedShot = AscensionAffixes.isRangedProjectile(direct);
'''),
    dedent('''\
        Entity direct = event.getSource().getDirectEntity();
        boolean rangedShot = AscensionAffixes.isRangedProjectile(direct);
        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer ? sourcePlayer : null;
        if (player == null && rangedShot && event.getEntity().level() instanceof ServerLevel hitLevel) {
            player = AscensionAffixes.rangedProjectileOwner(direct, hitLevel);
        }
        if (player == null || event.getEntity() == player || event.getAmount() <= 0.0F) return;
        UUID uuid = player.getUUID();
        if (CLEAVE_GUARD.contains(uuid) || SHOCKWAVE_GUARD.contains(uuid)) return;

'''))
replace_once(combat,
    dedent('''\
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        LivingEntity victim = event.getEntity();
        if (victim == player || victim instanceof Player || !ContentPackCompatibility.isCombatTarget(victim)) return;

        boolean majorTarget = ContentPackCompatibility.isMajorExpeditionTarget(victim);
        ExpeditionProgression.recordSkillAction(player, SkillType.COMBAT, 1);
        if (majorTarget) ExpeditionProgression.grantMajorTargetBonus(player, MAJOR_TARGET_EXPEDITION_BONUS);

        Entity direct = event.getSource().getDirectEntity();
'''),
    dedent('''\
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        LivingEntity victim = event.getEntity();
        Entity direct = event.getSource().getDirectEntity();
        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer ? sourcePlayer : null;
        if (player == null && victim.level() instanceof ServerLevel deathLevel) {
            player = AscensionAffixes.rangedProjectileOwner(direct, deathLevel);
        }
        if (player == null || victim == player || victim instanceof Player || !ContentPackCompatibility.isCombatTarget(victim)) return;

        boolean majorTarget = ContentPackCompatibility.isMajorExpeditionTarget(victim);
        ExpeditionProgression.recordSkillAction(player, SkillType.COMBAT, 1);
        if (majorTarget) ExpeditionProgression.grantMajorTargetBonus(player, MAJOR_TARGET_EXPEDITION_BONUS);

'''))

# Elite combat behavior/rewards also recover the same player for a Survival-snapshotted projectile.
elite = "src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java"
replace_once(elite,
    "import kr.moonseungjun.survivalascension.SurvivalAscension;\n",
    "import kr.moonseungjun.survivalascension.SurvivalAscension;\nimport kr.moonseungjun.survivalascension.equipment.AscensionAffixes;\n")
replace_once(elite,
    dedent('''\
        if (event.getEntity() instanceof Mob defender
                && event.getSource().getEntity() instanceof ServerPlayer player
                && isElite(defender)
                && defender.isAlive()
                && event.getHealthDamage() > 0.0F) {
            reactToPlayerHit(defender, player);
        }
'''),
    dedent('''\
        if (event.getEntity() instanceof Mob defender
                && defender.level() instanceof ServerLevel level
                && isElite(defender)
                && defender.isAlive()
                && event.getHealthDamage() > 0.0F) {
            ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                    ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
            if (player != null) reactToPlayerHit(defender, player);
        }
'''))
replace_once(elite,
    dedent('''\
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !isElite(event.getEntity())) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        Rank rank = rank(event.getEntity());
'''),
    dedent('''\
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !isElite(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        if (player == null) return;
        Rank rank = rank(event.getEntity());
'''))
replace_once(elite,
    "        if (event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level) {\n            dropRankReward(level, mob, rank);\n        }",
    "        if (event.getEntity() instanceof Mob mob) {\n            dropRankReward(level, mob, rank);\n        }")

# End-stage mutated enemy reactions/rewards recover the shooter too.
mutation = "src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java"
replace_once(mutation,
    "package kr.moonseungjun.survivalascension.elite;\n\n",
    "package kr.moonseungjun.survivalascension.elite;\n\nimport kr.moonseungjun.survivalascension.equipment.AscensionAffixes;\n")
replace_once(mutation,
    dedent('''\
        if (event.getEntity() instanceof Zombie zombie
                && mutation(zombie) == Mutation.PHASE
                && event.getSource().getEntity() instanceof ServerPlayer player
                && zombie.isAlive()) {
            reactPhase(level, zombie, player);
        }
'''),
    dedent('''\
        if (event.getEntity() instanceof Zombie zombie
                && mutation(zombie) == Mutation.PHASE
                && zombie.isAlive()) {
            ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                    ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
            if (player != null) reactPhase(level, zombie, player);
        }
'''))
replace_once(mutation,
    dedent('''\
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) return;
        Mutation mutation = mutation(mob);
        if (mutation == Mutation.NONE || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        player.giveExperiencePoints(10);
        if (mob.level() instanceof ServerLevel level && level.getRandom().nextFloat() < 0.35F) {
'''),
    dedent('''\
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        Mutation mutation = mutation(mob);
        if (mutation == Mutation.NONE) return;
        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        if (player == null) return;
        player.giveExperiencePoints(10);
        if (level.getRandom().nextFloat() < 0.35F) {
'''))

# Warband leader rewards recover the shooter; squad rout itself remains kill-source independent.
warband = "src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java"
replace_once(warband,
    "import kr.moonseungjun.survivalascension.SurvivalAscension;\n",
    "import kr.moonseungjun.survivalascension.SurvivalAscension;\nimport kr.moonseungjun.survivalascension.equipment.AscensionAffixes;\n")
replace_once(warband,
    dedent('''\
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            int combat = SkillProgressData.get(player).level(player, SkillType.COMBAT);
            int shards = Math.max(1, Math.min(4, 1 + combat / 30));
            level.addFreshEntity(new ItemEntity(level, leader.getX(), leader.getY() + 0.5D, leader.getZ(), new ItemStack(Items.ECHO_SHARD, shards)));
            player.sendSystemMessage(Component.literal("§4[전단장 격파] §f적 분대가 붕괴합니다. §b메아리 조각 +" + shards));
        }
'''),
    dedent('''\
        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        if (player != null) {
            int combat = SkillProgressData.get(player).level(player, SkillType.COMBAT);
            int shards = Math.max(1, Math.min(4, 1 + combat / 30));
            level.addFreshEntity(new ItemEntity(level, leader.getX(), leader.getY() + 0.5D, leader.getZ(), new ItemStack(Items.ECHO_SHARD, shards)));
            player.sendSystemMessage(Component.literal("§4[전단장 격파] §f적 분대가 붕괴합니다. §b메아리 조각 +" + shards));
        }
'''))

# Runtime version/banner.
main = "src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java"
replace_once(main, 'public static final String VERSION = "0.55.0-alpha.1";', 'public static final String VERSION = "0.56.0-alpha.1";')
replace_once(main,
    "// 0.55: native 26.2 Sulfur Caves and spear identity join progression without replacing vanilla mechanics.",
    "// 0.56: Survival-snapshotted ranged projectiles retain online shooter attribution across reward/behavior layers.")
replace_once(main,
    "loaded: scaled mastery + spear momentum drive lines + mace outer impact rings + shield guard waves + ranged projectile snapshots/impact bursts",
    "loaded: scaled mastery + ranged shooter attribution + spear momentum drive lines + mace outer impact rings + shield guard waves + ranged projectile snapshots/impact bursts")

# In-game guide: explain the player-visible guarantee and the intentional offline boundary.
guide = "src/main/java/kr/moonseungjun/survivalascension/client/GuideScreen.java"
replace_once(guide,
    'h("원거리 전투 파급"), p("표준 활/쇠뇌의 발사체는 발사 순간 장비 affix와 Shift 정밀 상태를 기록합니다. 전투 Lv30=2.5블록/2체, Lv60=3.5/4, Lv90=4.25/6, Lv100=5/8, 현장 숙련=6블록/10체의 기본 충돌 파급이 열립니다. Shift 발사는 파급 없는 단일 정밀 타격이며 산개·연쇄·충격 affix가 반경·대상·파급 피해를 제한적으로 확장합니다."),',
    'h("원거리 전투 파급"), p("표준 활/쇠뇌의 발사체는 발사 순간 장비 affix·Shift 정밀 상태와 발사자 귀속을 함께 기록합니다. 전투 Lv30=2.5블록/2체, Lv60=3.5/4, Lv90=4.25/6, Lv100=5/8, 현장 숙련=6블록/10체의 기본 충돌 파급이 열립니다. Shift 발사는 파급 없는 단일 정밀 타격이며 산개·연쇄·충격 affix가 반경·대상·파급 피해를 제한적으로 확장합니다. 충돌 시 공격자 참조가 끊겨도 발사자가 온라인이면 전투 성장·처치 보상·특수 적 보상이 같은 발사자에게 유지되며, 오프라인 보상 큐는 만들지 않습니다."),')

# README / PROJECT / CHANGELOG.
replace_once("README.md", "## 0.55.0-alpha.1 — Native 26.2 Spear + Sulfur Integration", dedent('''\
## 0.56.0-alpha.1 — Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화
Survival-recognized player-fired bow/crossbow projectiles now snapshot the firing player's UUID beside the existing affix/Shift launch state. The live `DamageSource` player remains authoritative when present; only when that source-player reference is missing does Survival resolve the still-online shooter from the same physical projectile.

That fallback now covers Combat damage scaling/impact burst, Combat kill XP and major-target expedition credit, Ascension elite-affix drops, Elite rank rewards/reactions, endgame-mutation rewards/reactions, and Warband leader rewards. An orphaned projectile is also no longer classified as environmental damage merely because its attacking-entity reference disappeared: environmental armor logic now requires both attacking entity and direct entity to be absent.

The fallback is deliberately bounded. It trusts only Survival-snapshotted ranged projectiles, resolves only a currently online player, queues no offline reward, and adds no new SavedData, packet/protocol, entity, force-load or background simulation. Network protocol remains `8` and the six locked external content-mod versions are unchanged.

## 0.55.0-alpha.1 — Native 26.2 Spear + Sulfur Integration'''))
replace_once("PROJECT.md", "- Mod version: `0.55.0-alpha.1`", "- Mod version: `0.56.0-alpha.1`")
replace_once("PROJECT.md", "## 0.55 Native 26.2 Spear + Sulfur Integration", dedent('''\
## 0.56 Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화
- A Survival-snapshotted ranged projectile stores the firing ServerPlayer UUID beside its existing affix/precision launch state.
- `DamageSource#getEntity()` remains authoritative when it is a ServerPlayer. Fallback lookup happens only for a Survival-marked ranged projectile and only resolves a currently online player through the server PlayerList.
- The recovered owner is used consistently for Combat scaling/burst, Combat kill XP + major-target credit, Ascension elite-affix drops, Elite reaction/rank rewards, endgame-mutation reaction/rewards, and Warband leader rewards.
- Environmental armor classification now requires both attacking entity and direct entity to be absent, so an orphaned physical projectile is not treated as environmental damage.
- Offline shooters are not queued for later rewards. No new SavedData, packet/protocol, custom projectile/entity, force-load or background simulation is introduced.

## 0.55 Native 26.2 Spear + Sulfur Integration'''))
replace_once("CHANGELOG.md", "## 0.55.0-alpha.1", dedent('''\
## 0.56.0-alpha.1
- Added firing-player UUID to the existing Survival ranged projectile launch snapshot.
- Added bounded online-owner fallback when a Survival-snapshotted projectile's `DamageSource` no longer exposes its ServerPlayer shooter.
- Routed fallback attribution through Combat damage/burst, Combat kill XP + major-target credit, Ascension elite-affix drops, Elite reactions/rank rewards, endgame-mutation reactions/rewards, and Warband leader rewards.
- Fixed armor `보호` environmental classification so a direct projectile with a missing attacking-entity reference is not treated as environmental damage.
- Kept live `DamageSource` ServerPlayer authority first; fallback is only for Survival-marked ranged projectiles and never queues offline rewards.
- Added no SavedData, packet/protocol, custom projectile/entity, force-load or background simulation. Network protocol remains8.
- Bumped content-preview lock to `0.56.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

## 0.55.0-alpha.1'''))

# Modpack compatibility/plan: generic tags stay the bridge; only the already-owned projectile NBT gains attribution.
replace_once("MODPACK_COMPAT_MATRIX.md",
    "- 원거리 장비: NeoForge 공용 `c:tools/bow` / `c:tools/crossbow` 태그를 쓰는 장비는 승천 각인에 합류한다. 발사체는 발사 순간 Survival affix/Shift 정밀 상태만 자체 persistent NBT에 스냅샷하며, 외부 활/쇠뇌 구현 클래스를 직접 참조하지 않는다.",
    "- 원거리 장비: NeoForge 공용 `c:tools/bow` / `c:tools/crossbow` 태그를 쓰는 장비는 승천 각인에 합류한다. 발사체는 발사 순간 Survival affix/Shift 정밀 상태와 발사자 UUID를 자체 persistent NBT에 스냅샷한다. 충돌 시 공격자 참조가 끊겨도 Survival이 이미 표시한 발사체라면 현재 온라인인 발사자를 복구해 전투/특수보상 귀속을 유지하며, 외부 활/쇠뇌 구현 클래스를 직접 참조하지 않는다.")
replace_once("MODPACK_COMPAT_MATRIX.md",
    "표준 활/쇠뇌 원거리 affix/발사체 스냅샷",
    "표준 활/쇠뇌 원거리 affix/발사체 스냅샷·온라인 발사자 귀속 복구")
replace_once("MODPACK_PLAN_DRAFT.md", "## 현재 0.55 네이티브 26.2 통합 상태", dedent('''\
## 현재 0.56 원거리 귀속 안정화 상태
- Survival이 발사 순간 인식한 표준 활/쇠뇌 projectile은 기존 affix/Shift 상태와 함께 발사자 UUID를 같은 물리 발사체 NBT에 기록한다.
- 이후 `DamageSource`의 공격자 참조가 사라져도 발사자가 온라인이면 전투 성장·처치/강적 보상·엘리트/변이/전단장 보상을 같은 발사자에게 복구한다.
- 오프라인 보상을 별도 저장하거나 나중에 지급하는 시스템은 만들지 않으며 새 SavedData/패킷/강제청크도 추가하지 않는다.

## 현재 0.55 네이티브 26.2 통합 상태'''))

# Release source audit: preserve all prior contracts and add explicit 0.56 attribution coverage.
src_test = "tools/test_release_source.py"
replace_once(src_test, 'REQUIRED_VERSION = "0.55.0-alpha.1"', 'REQUIRED_VERSION = "0.56.0-alpha.1"')
replace_all(src_test, r'VERSION = \"0.55.0-alpha.1\"', r'VERSION = \"0.56.0-alpha.1\"')
replace_once(src_test,
    '"snapshotRangedProjectile(Projectile projectile, ItemStack weapon, boolean precision)",',
    '"snapshotRangedProjectile(Projectile projectile, ServerPlayer player, ItemStack weapon, boolean precision)",')
replace_once(src_test,
    '"snapshotRangedProjectile(projectile, weapon, player.isShiftKeyDown())", "tryRangedBurst",',
    '"snapshotRangedProjectile(projectile, player, weapon, player.isShiftKeyDown())", "tryRangedBurst",')
replace_once(src_test, "# User-facing docs are part of the release contract, not an uncommitted CI-side patch.", dedent('''\
# 0.56 ranged projectile attribution hardening.
need(affix, [
    'RANGED_OWNER = "survivalascension_ranged_owner"',
    "snapshotRangedProjectile(Projectile projectile, ServerPlayer player, ItemStack weapon, boolean precision)",
    "data.putString(RANGED_OWNER, player.getUUID().toString())",
    "rangedProjectileOwner(Entity direct, ServerLevel level)", "UUID.fromString(raw)", "getPlayer(UUID.fromString(raw))"
], "0.56 ranged shooter snapshot/recovery")
need(combat, [
    "snapshotRangedProjectile(projectile, player, weapon, player.isShiftKeyDown())",
    "event.getSource().getEntity() == null && event.getSource().getDirectEntity() == null",
    "player = AscensionAffixes.rangedProjectileOwner(direct, hitLevel)",
    "player = AscensionAffixes.rangedProjectileOwner(direct, deathLevel)"
], "0.56 combat ranged attribution")
elite_src = read("src/main/java/kr/moonseungjun/survivalascension/elite/EliteMobSystem.java")
mutation_src = read("src/main/java/kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.java")
warband_src = read("src/main/java/kr/moonseungjun/survivalascension/elite/WarbandDirector.java")
need(affix, ["rangedProjectileOwner(event.getSource().getDirectEntity(), level)"], "0.56 affix elite-drop attribution")
need(elite_src, ["AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level)"], "0.56 elite attribution")
need(mutation_src, ["AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level)"], "0.56 mutation attribution")
need(warband_src, ["AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level)"], "0.56 warband attribution")
forbid(affix + combat + elite_src + mutation_src + warband_src,
       ["setChunkForced", "addRegionTicket", "getChunk("], "0.56 ranged attribution world-loading policy")

# User-facing docs are part of the release contract, not an uncommitted CI-side patch.'''))
replace_once(src_test,
    '"Mod version: `0.55.0-alpha.1`",',
    '"Mod version: `0.56.0-alpha.1`",')
replace_once(src_test,
    'need(guide, [\'h("스피어 돌파선")\', "minecraft:spears", "피해·숙련 XP 없이", "바닐라 Jab/Charge"], "0.55 in-game guide")',
    'need(guide, [\'h("스피어 돌파선")\', "minecraft:spears", "피해·숙련 XP 없이", "바닐라 Jab/Charge"], "0.55 in-game guide")\nneed(project_doc, ["## 0.56 Ranged Projectile Attribution Hardening", "Offline shooters are not queued"], "0.56 PROJECT docs")\nneed(readme, ["## 0.56.0-alpha.1 — Ranged Projectile Attribution Hardening", "firing player\'s UUID", "currently online player"], "0.56 README docs")\nneed(changelog, ["## 0.56.0-alpha.1", "ranged projectile launch snapshot", "never queues offline rewards", "0.56.0-alpha.1-content-preview.1"], "0.56 CHANGELOG docs")\nneed(guide, [\'h("원거리 전투 파급")\', "발사자 귀속", "발사자가 온라인이면", "오프라인 보상 큐"], "0.56 in-game guide")')
replace_once(src_test,
    'print("- 0.55 Sulfur Caves and dedicated spear momentum drive-line integration are bounded and regression-checked")\nprint("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.55")',
    'print("- 0.55 Sulfur Caves and dedicated spear momentum drive-line integration are bounded and regression-checked")\nprint("- 0.56 Survival-snapshotted ranged projectiles retain bounded online shooter attribution across combat/reward layers")\nprint("- README / PROJECT / CHANGELOG / in-game guide are committed and synchronized to 0.56")')

# Release content-pack audit.
pack_test = "tools/test_release_content_pack.py"
replace_once(pack_test, 'REQUIRED_LOCK_VERSION = "0.55.0-alpha.1-content-preview.1"', 'REQUIRED_LOCK_VERSION = "0.56.0-alpha.1-content-preview.1"')
replace_once(pack_test,
    "baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.55.0-alpha.1`')",
    "baseline.replace('Mod version: `0.48.0-alpha.1`', 'Mod version: `0.56.0-alpha.1`')")
replace_once(pack_test, "if errors:\n", dedent('''\
need(affix, ["survivalascension_ranged_owner", "rangedProjectileOwner", "getPlayer(UUID.fromString(raw))"], "0.56 ranged-owner content bridge")
need(combat, ["rangedProjectileOwner(direct, hitLevel)", "rangedProjectileOwner(direct, deathLevel)"], "0.56 ranged-owner runtime bridge")
need(matrix, ["발사자 UUID", "현재 온라인인 발사자를 복구"], "0.56 generic ranged attribution docs")

if errors:
'''))
replace_once(pack_test,
    'print("spear_affix_drive_bridge=PASS")\nprint("RELEASE CONTENT-PACK AUDIT PASS")',
    'print("spear_affix_drive_bridge=PASS")\nprint("ranged_projectile_owner_attribution=PASS")\nprint("RELEASE CONTENT-PACK AUDIT PASS")')

# Packaged JAR verifier must see the owner key + recovery helper and all consuming reward/behavior classes.
verify_release = "tools/verify_release_jar.py"
replace_once(verify_release, 'print("frontline_freight_manifest_runtime=present")', dedent('''\
with zipfile.ZipFile(jar) as zf:
    affix056 = zf.read("kr/moonseungjun/survivalascension/equipment/AscensionAffixes.class")
    combat056 = zf.read("kr/moonseungjun/survivalascension/combat/CombatProgression.class")
    elite056 = zf.read("kr/moonseungjun/survivalascension/elite/EliteMobSystem.class")
    mutation056 = zf.read("kr/moonseungjun/survivalascension/elite/EndgameMutationSystem.class")
    warband056 = zf.read("kr/moonseungjun/survivalascension/elite/WarbandDirector.class")
    for token in [b"survivalascension_ranged_owner", b"rangedProjectileOwner", b"UUID", b"getPlayer"]:
        if token not in affix056:
            raise SystemExit(f"0.56 compiled ranged-owner token missing: {token!r}")
    for blob, label in [(combat056, "combat"), (elite056, "elite"), (mutation056, "mutation"), (warband056, "warband")]:
        if b"rangedProjectileOwner" not in blob:
            raise SystemExit(f"0.56 compiled ranged-owner consumer missing: {label}")

print("frontline_freight_manifest_runtime=present")'''))
replace_once(verify_release,
    'print("native_26_2_release_verify=PASS")',
    'print("native_26_2_release_verify=PASS")\nprint("ranged_projectile_owner_attribution_runtime=present")\nprint("ranged_projectile_owner_attribution_release_verify=PASS")')

# Generic JAR verifier also checks the new compiled bridge without relying only on release wrapper output.
verify_jar = "tools/verify_jar.py"
replace_once(verify_jar,
    '    if b"trySpearDrive" not in combat_class:\n        raise SystemExit("compiled spear drive-line bridge missing")',
    '    if b"trySpearDrive" not in combat_class:\n        raise SystemExit("compiled spear drive-line bridge missing")\n    if b"survivalascension_ranged_owner" not in affix_class or b"rangedProjectileOwner" not in affix_class:\n        raise SystemExit("compiled ranged projectile owner attribution bridge missing")\n    if b"rangedProjectileOwner" not in combat_class:\n        raise SystemExit("compiled combat ranged owner fallback missing")')
replace_once(verify_jar,
    'print("field_recovery_runtime=present")',
    'print("field_recovery_runtime=present")\nprint("ranged_projectile_owner_attribution_runtime=present")')

# Remove this one-shot staging block from the workflow before the verified project commit is published.
workflow_text = WORKFLOW.read_text(encoding="utf-8")
start_marker = "      - name: Apply and fully verify staged 0.56 patch once\n"
end_marker = "      - name: Resolve version\n"
start = workflow_text.find(start_marker)
end = workflow_text.find(end_marker, start if start >= 0 else 0)
if start < 0 or end < 0 or end <= start:
    raise SystemExit("0.56 staging workflow block missing")
WORKFLOW.write_text(workflow_text[:start] + workflow_text[end:], encoding="utf-8")

# Self-delete so the verified release commit contains only canonical project/workflow state.
Path(__file__).unlink()
print("Survival Ascension 0.56 ranged attribution patch applied; staging hook removed")
