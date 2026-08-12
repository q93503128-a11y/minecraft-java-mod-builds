#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"
REPO = ROOT.parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one match in {path}: {count}\n--- OLD ---\n{old[:500]}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_all_existing(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    # Version and existing version-aware regression tests.
    replace_once(ROOT / "gradle.properties", "mod_version=0.18.10-alpha.1", "mod_version=0.18.11-alpha.1")
    for test in (ROOT / "tools").glob("test_*.py"):
        replace_all_existing(test, "mod_version=0.18.10-alpha.1", "mod_version=0.18.11-alpha.1")

    # Shared block-collision line-of-sight for static defenses and non-player ranged allies.
    (JAVA / "VillageDefenseLineOfSight.java").write_text('''package kr.moonseungjun.villageguardians;\n\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.world.entity.Mob;\nimport net.minecraft.world.level.ClipContext;\nimport net.minecraft.world.phys.HitResult;\nimport net.minecraft.world.phys.Vec3;\n\n/** Shared block-collision LOS for defense fire that has no player camera/entity eye ray. */\npublic final class VillageDefenseLineOfSight {\n    private VillageDefenseLineOfSight() {}\n\n    public static boolean hasLine(ServerLevel level, Vec3 start, Mob target) {\n        if (level == null || start == null || target == null || !target.isAlive()) return false;\n        Vec3 end = target.position().add(0, Math.max(0.35, target.getBbHeight() * 0.55), 0);\n        HitResult hit = level.clip(new ClipContext(\n                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));\n        return hit.getType() == HitResult.Type.MISS;\n    }\n}\n''', encoding="utf-8")

    turret = JAVA / "VillagePlacedTurretSystem.java"
    replace_once(turret,
'''        double range = state.type().range() + (state.level() - 1) * 2.5;\n        List<Mob> candidates = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), range, 12, null);\n        if (candidates.isEmpty()) return;''',
'''        double range = state.type().range() + (state.level() - 1) * 2.5;\n        Vec3 muzzle = Vec3.atCenterOf(state.pos().above());\n        List<Mob> candidates = VillageRaidSystem.activeEnemiesNear(level, Vec3.atCenterOf(state.pos()), range, 12, null)\n                .stream().filter(mob -> VillageDefenseLineOfSight.hasLine(level, muzzle, mob)).toList();\n        if (candidates.isEmpty()) return;''')

    replace_once(turret,
'''        switch (state.type()) {\n            case CHAIN -> {''',
'''        switch (state.type()) {\n            case PIERCER -> hit(level, state, target, damage * piercingMultiplier(target), ParticleTypes.CRIT);\n            case CHAIN -> {''')

    replace_once(turret,
'''    private static void hit(ServerLevel level, TurretState state, Mob target, float damage,\n                            net.minecraft.core.particles.ParticleOptions particle) {\n        Vec3 start = Vec3.atCenterOf(state.pos().above());\n        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);''',
'''    private static float piercingMultiplier(Mob target) {\n        VillageEnemyArchetypeSystem.Archetype type = VillageRaidSystem.archetypeOf(target);\n        if (type == null) return target.hasEffect(MobEffects.RESISTANCE) ? 1.30f : 1.05f;\n        return switch (type) {\n            case BULWARK, SHIELDBREAKER, SIEGE_BEAST, IRON_WARLORD -> 1.55f;\n            case DREAD_KNIGHT -> 1.35f;\n            default -> target.hasEffect(MobEffects.RESISTANCE) ? 1.30f : 1.05f;\n        };\n    }\n\n    private static void hit(ServerLevel level, TurretState state, Mob target, float damage,\n                            net.minecraft.core.particles.ParticleOptions particle) {\n        Vec3 start = Vec3.atCenterOf(state.pos().above());\n        if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;\n        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);''')

    replace_once(turret,
'''            if (type == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER && mob.distanceToSqr(Vec3.atCenterOf(state.pos())) <= 36.0 * 36.0) {\n                damage = Math.max(damage, 18 + VillageCouncilState.currentDay());\n                mob.getNavigation().moveTo(state.pos().getX() + 0.5, state.pos().getY(), state.pos().getZ() + 0.5, 1.08);\n            } else if (type == VillageEnemyArchetypeSystem.Archetype.SAPPER\n                    && mob.distanceToSqr(Vec3.atCenterOf(state.pos())) <= 36.0) {''',
'''            double distanceSquared = mob.distanceToSqr(Vec3.atCenterOf(state.pos()));\n            if (type == VillageEnemyArchetypeSystem.Archetype.TOWER_HUNTER && distanceSquared <= 36.0 * 36.0) {\n                mob.getNavigation().moveTo(state.pos().getX() + 0.5, state.pos().getY(), state.pos().getZ() + 0.5, 1.08);\n                if (distanceSquared <= 7.5 * 7.5) {\n                    damage = Math.max(damage, 18 + VillageCouncilState.currentDay());\n                }\n            } else if (type == VillageEnemyArchetypeSystem.Archetype.SAPPER\n                    && distanceSquared <= 6.0 * 6.0) {''')

    replace_once(turret,
'''            } else if (type != null && VillageEnemyArchetypeSystem.isBoss(type)\n                    && mob.distanceToSqr(Vec3.atCenterOf(state.pos())) <= 64.0) {''',
'''            } else if (type != null && VillageEnemyArchetypeSystem.isBoss(type)\n                    && distanceSquared <= 8.0 * 8.0) {''')

    replace_once(turret,
'''        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return "포탑 공간 2블록이 비어 있어야 합니다.";''',
'''        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()\n                || !level.getBlockState(pos.above(2)).isAir()) return "포탑 공간 3블록이 비어 있어야 합니다.";''')

    replace_once(turret,
'''    private static void buildVisual(ServerLevel level, TurretState state) {\n        if (!state.active()) { buildWreck(level, state); return; }\n        VillageFortressTerrain.set(level, state.pos(), Blocks.STONE_BRICK_WALL);\n        VillageFortressTerrain.set(level, state.pos().above(), state.type().visual());\n    }\n    private static void buildWreck(ServerLevel level, TurretState state) {\n        VillageFortressTerrain.set(level, state.pos(), Blocks.CRACKED_STONE_BRICKS);\n        VillageFortressTerrain.set(level, state.pos().above(), Blocks.AIR);\n    }\n    private static void clearVisual(ServerLevel level, BlockPos pos) {\n        VillageFortressTerrain.set(level, pos, Blocks.AIR);\n        VillageFortressTerrain.set(level, pos.above(), Blocks.AIR);\n    }''',
'''    private static void buildVisual(ServerLevel level, TurretState state) {\n        if (!state.active()) { buildWreck(level, state); return; }\n        Block base = state.level() >= 4 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL : Blocks.STONE_BRICK_WALL;\n        VillageFortressTerrain.set(level, state.pos(), base);\n        VillageFortressTerrain.set(level, state.pos().above(), state.type().visual());\n        VillageFortressTerrain.set(level, state.pos().above(2), turretCap(state.type()));\n    }\n    private static Block turretCap(TurretType type) {\n        return switch (type) {\n            case BALLISTA, REPEATER, PIERCER -> Blocks.IRON_BARS;\n            case FLAME -> Blocks.SOUL_LANTERN;\n            case FROST -> Blocks.BLUE_ICE;\n            case CHAIN -> Blocks.LIGHTNING_ROD;\n            case BOMBARD -> Blocks.HEAVY_CORE;\n            case NULLIFIER -> Blocks.END_ROD;\n            case ANTI_AIR -> Blocks.COPPER_BULB;\n            case BEACON -> Blocks.SEA_LANTERN;\n        };\n    }\n    private static void buildWreck(ServerLevel level, TurretState state) {\n        VillageFortressTerrain.set(level, state.pos(), Blocks.CRACKED_STONE_BRICKS);\n        VillageFortressTerrain.set(level, state.pos().above(), Blocks.AIR);\n        VillageFortressTerrain.set(level, state.pos().above(2), Blocks.AIR);\n    }\n    private static void clearVisual(ServerLevel level, BlockPos pos) {\n        VillageFortressTerrain.set(level, pos, Blocks.AIR);\n        VillageFortressTerrain.set(level, pos.above(), Blocks.AIR);\n        VillageFortressTerrain.set(level, pos.above(2), Blocks.AIR);\n    }''')

    merc = JAVA / "VillageMercenarySystem.java"
    replace_once(merc,
'''            if (!VillageRaidSystem.isActive()) continue;\n            if (kind == MercenaryClass.RANGER) rangedAttack(level, mercenary, rank);\n            else if (kind == MercenaryClass.MEDIC) healAllies(level, server, mercenary, rank);''',
'''            if (!VillageRaidSystem.isActive()) continue;\n            if (kind == MercenaryClass.BASTION) bastionControl(level, mercenary, rank);\n            else if (kind == MercenaryClass.STRIKER) strikerPressure(level, mercenary, rank);\n            else if (kind == MercenaryClass.RANGER) rangedAttack(level, mercenary, rank);\n            else if (kind == MercenaryClass.MEDIC) healAllies(level, server, mercenary, rank);''')

    old_award = '''    public static synchronized void awardKillExperience(MinecraftServer server, Vec3 deathPosition) {\n        if (server == null || deathPosition == null) return;\n        ServerLevel level = server.overworld();\n        AABB area = new AABB(deathPosition, deathPosition).inflate(48.0);\n        boolean changed = false;\n        for (IronGolem mercenary : level.getEntitiesOfClass(IronGolem.class, area,\n                entity -> isMercenary(entity.getUUID()) && entity.isAlive())) {\n            UUID uuid = mercenary.getUUID();\n            int kills = KILLS.getOrDefault(uuid, 0) + 1;\n            int currentRank = LEVELS.getOrDefault(uuid, 1);\n            int nextRank = Math.min(5, 1 + kills / 8);\n            KILLS.put(uuid, kills);\n            changed = true;\n            if (nextRank > currentRank) {\n                LEVELS.put(uuid, nextRank);\n                applyClassPassives(mercenary, mercenaryClass(mercenary), nextRank);\n                refreshName(mercenary);\n                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,\n                        mercenary.getX(), mercenary.getY() + 1.3, mercenary.getZ(),\n                        16, 0.45, 0.7, 0.45, 0.05);\n            }\n        }\n        if (changed) persist();\n    }'''
    new_award = '''    public static synchronized void awardKillExperience(Mob killer) {\n        if (!(killer instanceof IronGolem mercenary) || !isMercenary(mercenary.getUUID())\n                || !(mercenary.level() instanceof ServerLevel level)) return;\n        UUID uuid = mercenary.getUUID();\n        int kills = KILLS.getOrDefault(uuid, 0) + 1;\n        int currentRank = LEVELS.getOrDefault(uuid, 1);\n        int nextRank = Math.min(5, 1 + kills / 8);\n        KILLS.put(uuid, kills);\n        if (nextRank > currentRank) {\n            LEVELS.put(uuid, nextRank);\n            applyClassPassives(mercenary, mercenaryClass(mercenary), nextRank);\n            refreshName(mercenary);\n            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,\n                    mercenary.getX(), mercenary.getY() + 1.3, mercenary.getZ(),\n                    16, 0.45, 0.7, 0.45, 0.05);\n        }\n        persist();\n    }'''
    replace_once(merc, old_award, new_award)

    old_ranged = '''    private static void rangedAttack(ServerLevel level, IronGolem mercenary, int rank) {\n        Mob target = VillageRaidSystem.nearestActiveEnemy(level, mercenary.blockPosition(), 42.0 + rank * 3.0);\n        if (target == null) return;\n        float damage = (3.0f + rank * 1.3f) * VillageDefenseResearchSystem.mercenaryDamageMultiplier();\n        Vec3 start = mercenary.position().add(0, 1.8, 0);\n        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);\n        for (int i = 0; i <= 10; i++) {\n            Vec3 point = start.lerp(end, i / 10.0);\n            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);\n        }\n        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);\n    }'''
    new_ranged = '''    private static void bastionControl(ServerLevel level, IronGolem mercenary, int rank) {\n        double radius = 4.5 + rank * 0.55;\n        Vec3 eye = mercenary.position().add(0, 1.8, 0);\n        for (Mob enemy : VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), radius, 5 + rank, null)) {\n            if (!VillageDefenseLineOfSight.hasLine(level, eye, enemy)) continue;\n            enemy.setTarget(mercenary);\n            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 28 + rank * 5, 0));\n        }\n    }\n\n    private static void strikerPressure(ServerLevel level, IronGolem mercenary, int rank) {\n        Mob target = VillageRaidSystem.nearestActiveEnemy(level, mercenary.blockPosition(), 22.0 + rank * 2.0);\n        if (target == null || !VillageDefenseLineOfSight.hasLine(level, mercenary.position().add(0, 1.8, 0), target)) return;\n        mercenary.setTarget(target);\n        mercenary.getNavigation().moveTo(target, 1.18 + rank * 0.025);\n    }\n\n    private static void rangedAttack(ServerLevel level, IronGolem mercenary, int rank) {\n        Vec3 start = mercenary.position().add(0, 1.8, 0);\n        Mob target = VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), 42.0 + rank * 3.0, 18, null)\n                .stream().filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))\n                .min(java.util.Comparator.comparingDouble(mercenary::distanceToSqr)).orElse(null);\n        mercenary.setTarget(null);\n        if (target == null) return;\n        float damage = (3.0f + rank * 1.3f) * VillageDefenseResearchSystem.mercenaryDamageMultiplier();\n        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);\n        for (int i = 0; i <= 10; i++) {\n            Vec3 point = start.lerp(end, i / 10.0);\n            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);\n        }\n        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);\n    }'''
    replace_once(merc, old_ranged, new_ranged)

    deployment = JAVA / "VillageMercenaryDeploymentSystem.java"
    replace_once(deployment,
'''            if (force || !VillageRaidSystem.isActive() || golem.blockPosition().distSqr(rally) > leash * leash) {\n                golem.getNavigation().moveTo(rally.getX() + 0.5, rally.getY(), rally.getZ() + 0.5,\n                        kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 1.18 : 1.02);\n            }''',
'''            if (force || !VillageRaidSystem.isActive() || golem.blockPosition().distSqr(rally) > leash * leash) {\n                boolean accepted = golem.getNavigation().moveTo(rally.getX() + 0.5, rally.getY(), rally.getZ() + 0.5,\n                        kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 1.18 : 1.02);\n                if (!accepted && zone == Deployment.WALL) {\n                    BlockPos fallback = rallyPoint(center, Deployment.INNER, kind);\n                    golem.getNavigation().moveTo(fallback.getX() + 0.5, fallback.getY(), fallback.getZ() + 0.5, 1.0);\n                }\n            }''')
    replace_once(deployment,
'''            } else if (kind == VillageMercenarySystem.MercenaryClass.STRIKER) {\n                Mob target = VillageRaidSystem.nearestActiveEnemy(level, golem.blockPosition(), 42.0);\n                if (target != null) golem.setTarget(target);\n            } else if (kind == VillageMercenarySystem.MercenaryClass.MEDIC) {\n                golem.setTarget(null);\n            }''',
'''            } else if (kind == VillageMercenarySystem.MercenaryClass.STRIKER) {\n                Mob target = VillageRaidSystem.nearestActiveEnemy(level, golem.blockPosition(), 42.0);\n                if (target != null) golem.setTarget(target);\n            } else if (kind == VillageMercenarySystem.MercenaryClass.RANGER\n                    || kind == VillageMercenarySystem.MercenaryClass.MEDIC) {\n                golem.setTarget(null);\n            }''')

    guardians = JAVA / "VillageGuardians.java"
    replace_once(guardians,
'''        if (raidEnemy && server != null) {\n            VillageMercenarySystem.awardKillExperience(server, event.getEntity().position());\n            if (boss) VillageRelicSystem.offerToParty(server);\n        }''',
'''        if (raidEnemy && server != null) {\n            if (event.getSource().getEntity() instanceof Mob killer) {\n                VillageMercenarySystem.awardKillExperience(killer);\n            }\n            if (boss) VillageRelicSystem.offerToParty(server);\n        }''')

    # New deterministic contract.
    (ROOT / "tools/test_v01811_defense_polish.py").write_text(r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    turret = read("VillagePlacedTurretSystem.java")
    los = read("VillageDefenseLineOfSight.java")
    merc = read("VillageMercenarySystem.java")
    deploy = read("VillageMercenaryDeploymentSystem.java")
    guardians = read("VillageGuardians.java")

    assert "mod_version=0.18.11-alpha.1" in props

    # Static defenses and ranger mercenaries must not acquire/fire through blocks.
    assert "ClipContext.Block.COLLIDER" in los and "HitResult.Type.MISS" in los
    assert ".filter(mob -> VillageDefenseLineOfSight.hasLine(level, muzzle, mob))" in turret
    assert "if (!VillageDefenseLineOfSight.hasLine(level, start, target)) return;" in turret
    assert ".filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))" in merc

    # Tower hunters can approach from their search radius, but physical HP damage is short-range.
    assert "distanceSquared <= 36.0 * 36.0" in turret
    assert "distanceSquared <= 7.5 * 7.5" in turret
    assert "distanceSquared <= 6.0 * 6.0" in turret
    assert "distanceSquared <= 8.0 * 8.0" in turret

    # Piercer is now mechanically differentiated against armored / resistant targets.
    assert "case PIERCER -> hit" in turret
    assert "piercingMultiplier" in turret
    for token in ("BULWARK", "SHIELDBREAKER", "SIEGE_BEAST", "IRON_WARLORD"):
        assert token in turret
    assert "1.55f" in turret

    # Turrets have a compact three-block silhouette and matching placement/cleanup contract.
    assert "pos.above(2)" in turret and "포탑 공간 3블록" in turret
    assert "turretCap" in turret and "POLISHED_BLACKSTONE_BRICK_WALL" in turret

    # Mercenary progression belongs only to the actual killing mercenary.
    assert "awardKillExperience(Mob killer)" in merc
    assert "event.getSource().getEntity() instanceof Mob killer" in guardians
    assert "new AABB(deathPosition, deathPosition).inflate(48.0)" not in merc

    # All four classes now have active battlefield identity; ranged/medic do not drift into vanilla melee AI.
    assert "bastionControl" in merc and "strikerPressure" in merc and "rangedAttack" in merc and "healAllies" in merc
    assert "MercenaryClass.RANGER\n                    || kind == VillageMercenarySystem.MercenaryClass.MEDIC" in deploy
    assert "if (!accepted && zone == Deployment.WALL)" in deploy

    # Existing breadth is retained.
    for token in ("BALLISTA", "REPEATER", "PIERCER", "FLAME", "FROST", "CHAIN", "BOMBARD", "NULLIFIER", "ANTI_AIR", "BEACON"):
        assert token in turret
    for token in ("BASTION", "STRIKER", "RANGER", "MEDIC"):
        assert token in merc

    print("[PASS] turret and ranger LOS blocks wall-through acquisition and damage")
    print("[PASS] tower hunters approach at range but damage turrets only at physical attack distance")
    print("[PASS] piercer has a real armored-target damage niche")
    print("[PASS] turret placement/visual/cleanup owns a matching three-block footprint")
    print("[PASS] only the actual mercenary killer receives mercenary progression")
    print("[PASS] all four mercenary classes retain distinct active battlefield behavior")


if __name__ == "__main__":
    main()
''', encoding="utf-8")

    # Permanent Java 25 acceptance workflow for this version.
    workflow = REPO / ".github/workflows/build-village-guardians-v01811.yml"
    workflow.write_text('''name: Build Village Guardians v0.18.11 Defense Polish\n\non:\n  workflow_dispatch:\n  push:\n    branches: [main]\n    paths:\n      - 'projects/village-guardians/.build-trigger-v01811'\n\npermissions:\n  contents: read\n\nenv:\n  PROJECT_DIR: projects/village-guardians\n  BUILD_VERSION: 0.18.11-alpha.1\n  EXPECTED_JAR: villageguardians-0.18.11-alpha.1.jar\n\njobs:\n  build:\n    name: Java 25 / NeoForge 26.2 defense polish build\n    runs-on: ubuntu-latest\n    timeout-minutes: 90\n    steps:\n      - name: Checkout repository\n        uses: actions/checkout@v4\n      - name: Set up Java 25\n        uses: actions/setup-java@v4\n        with:\n          distribution: temurin\n          java-version: '25'\n      - name: Set up Gradle 9.2.1\n        uses: gradle/actions/setup-gradle@v4\n        with:\n          gradle-version: '9.2.1'\n          cache-read-only: false\n      - name: Run deterministic contract tests\n        working-directory: ${{ env.PROJECT_DIR }}\n        shell: bash\n        run: |\n          set -euo pipefail\n          for test in \\\n            tools/test_rpg_balance.py \\\n            tools/test_village_zone.py \\\n            tools/test_progression_loop.py \\\n            tools/test_progression_depth.py \\\n            tools/test_fortress_layout.py \\\n            tools/test_runtime_safety.py \\\n            tools/test_ui_layout_contract.py \\\n            tools/test_interaction_contract.py \\\n            tools/test_action_layout.py \\\n            tools/test_enemy_content.py \\\n            tools/test_v0188_risk_ui_cleanup.py \\\n            tools/test_v0189_siege_phase2.py \\\n            tools/test_v01810_ranger_ricochet.py \\\n            tools/test_v01811_defense_polish.py; do\n            if test -f "$test"; then python3 "$test"; fi\n          done\n      - name: Clean NeoForge build\n        working-directory: ${{ env.PROJECT_DIR }}\n        shell: bash\n        run: |\n          set -o pipefail\n          gradle --no-daemon --no-configuration-cache -Pmod_version="$BUILD_VERSION" clean build --stacktrace --console=plain 2>&1 | tee "$GITHUB_WORKSPACE/village-guardians-v01811-gradle.log"\n      - name: Verify runtime JAR\n        working-directory: ${{ env.PROJECT_DIR }}\n        shell: bash\n        run: |\n          set -euo pipefail\n          test -f "build/libs/$EXPECTED_JAR"\n          python3 tools/verify_jar.py "build/libs/$EXPECTED_JAR"\n          mkdir -p "$GITHUB_WORKSPACE/deliverables"\n          cp "build/libs/$EXPECTED_JAR" "$GITHUB_WORKSPACE/deliverables/"\n          sha256sum "build/libs/$EXPECTED_JAR" > "$GITHUB_WORKSPACE/deliverables/$EXPECTED_JAR.sha256"\n      - name: Upload verified JAR\n        uses: actions/upload-artifact@v4\n        with:\n          name: villageguardians-0.18.11-alpha.1\n          path: deliverables/\n          if-no-files-found: error\n          retention-days: 30\n      - name: Upload Gradle log on failure\n        if: failure()\n        uses: actions/upload-artifact@v4\n        with:\n          name: villageguardians-v01811-build-log\n          path: village-guardians-v01811-gradle.log\n          if-no-files-found: ignore\n          retention-days: 7\n''', encoding="utf-8")

    (ROOT / ".build-trigger-v01811").write_text(
        "Village Guardians v0.18.11-alpha.1 defense polish acceptance trigger\n", encoding="utf-8")

    print("[OK] v0.18.11 defense polish patch applied")


if __name__ == "__main__":
    main()
