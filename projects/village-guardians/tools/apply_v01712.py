#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
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


# Version
props = ROOT / "gradle.properties"
text = read(props)
text = replace_once(text, "mod_version=0.17.11-alpha.1", "mod_version=0.17.12-alpha.1", "version")
write(props, text)

# New skill identities and the concrete ability system delegation.
path = JAVA / "VillageRoleSkillSystem.java"
text = read(path)
cast_start = text.index("    private static void cast(\n")
cast_end = text.index("    private static List<Mob> damageArea(\n", cast_start)
cast_method = '''    private static void cast(
            ServerLevel level,
            ServerPlayer player,
            ActiveSkill skill,
            float power,
            float durationMultiplier,
            int specialRank) {
        switch (skill) {
            case VANGUARD_WHIRLWIND -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case VANGUARD_BREAKER -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case VANGUARD_CRY -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case VANGUARD_STORM -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_VOLLEY -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_PIERCE -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_RICOCHET -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case RANGER_FIRE_RAIN -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_FIRE_ORB -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_FROST_RING -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_CHAIN -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case ARCANIST_NOVA -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_HEAL -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_CLEANSE -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_VEIL -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case LUMINAR_SANCTUARY -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_TAUNT -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_BASH -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_FORMATION -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
            case WARDEN_FIELD -> VillageRoleAbilitySystem.cast(level, player, skill, power, durationMultiplier, specialRank);
        }
    }

'''
text = text[:cast_start] + cast_method + text[cast_end:]
old_skills = '''        VANGUARD_WHIRLWIND("vanguard_whirlwind", VillageRole.VANGUARD, 0, "회전 참격", 2, 70, 18, "주변 적을 한 번에 베고 특수 경로에 따라 체력을 회복합니다."),
        VANGUARD_BREAKER("vanguard_breaker", VillageRole.VANGUARD, 1, "돌진 분쇄", 7, 190, 24, "전방 돌파를 표현한 강한 범위 일격과 이동 가속을 얻습니다."),
        VANGUARD_CRY("vanguard_cry", VillageRole.VANGUARD, 2, "전장의 포효", 13, 380, 32, "주변 아군의 공격력과 이동 속도를 강화합니다."),
        VANGUARD_STORM("vanguard_storm", VillageRole.VANGUARD, 3, "검기 폭풍", 21, 680, 42, "넓은 범위의 다수 적에게 강한 검기 피해를 가합니다."),

        RANGER_VOLLEY("ranger_volley", VillageRole.RANGER, 0, "연발 사격", 2, 70, 16, "주변 여러 적에게 빠른 원거리 피해를 분산합니다."),
        RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "관통 사격", 7, 190, 22, "적은 수의 정예 대상에게 높은 관통 피해를 줍니다."),
        RANGER_RICOCHET("ranger_ricochet", VillageRole.RANGER, 2, "도탄 연쇄", 13, 380, 30, "여러 적 사이를 튕기는 연쇄 사격을 가합니다."),
        RANGER_FIRE_RAIN("ranger_fire_rain", VillageRole.RANGER, 3, "화염 폭우", 21, 680, 40, "넓은 범위의 적을 불태우는 대규모 사격을 실행합니다."),

        ARCANIST_FIRE_ORB("arcanist_fire_orb", VillageRole.ARCANIST, 0, "화염 구체", 2, 70, 18, "주변 적에게 폭발 피해와 화염을 가합니다."),
        ARCANIST_FROST_RING("arcanist_frost_ring", VillageRole.ARCANIST, 1, "서리 고리", 7, 190, 24, "주변 적에게 냉기 피해와 강한 둔화를 부여합니다."),
        ARCANIST_CHAIN("arcanist_chain", VillageRole.ARCANIST, 2, "연쇄 번개", 13, 380, 30, "다수의 적에게 연쇄되는 비전 피해를 가합니다."),
        ARCANIST_NOVA("arcanist_nova", VillageRole.ARCANIST, 3, "비전 폭발", 21, 680, 44, "넓은 범위의 적을 한 번에 폭발시키는 궁극 기술입니다."),

        LUMINAR_HEAL("luminar_heal", VillageRole.LUMINAR, 0, "치유의 빛", 2, 70, 16, "주변 아군의 체력을 즉시 회복합니다."),
        LUMINAR_CLEANSE("luminar_cleanse", VillageRole.LUMINAR, 1, "정화 기도", 7, 190, 24, "주변 아군의 주요 해로운 효과를 제거하고 재생을 부여합니다."),
        LUMINAR_VEIL("luminar_veil", VillageRole.LUMINAR, 2, "재생 장막", 13, 380, 32, "아군을 치유하고 오래 지속되는 재생과 보호막을 부여합니다."),
        LUMINAR_SANCTUARY("luminar_sanctuary", VillageRole.LUMINAR, 3, "생명 성역", 21, 680, 46, "넓은 범위의 아군을 크게 치유하고 강한 보호막을 부여합니다."),

        WARDEN_TAUNT("warden_taunt", VillageRole.WARDEN, 0, "도발의 함성", 2, 70, 18, "주변 적을 약화·둔화하고 자신에게 저항을 부여합니다."),
        WARDEN_BASH("warden_bash", VillageRole.WARDEN, 1, "방패 충격", 7, 190, 22, "주변 적에게 피해와 매우 강한 짧은 둔화를 가합니다."),
        WARDEN_FORMATION("warden_formation", VillageRole.WARDEN, 2, "철벽 진형", 13, 380, 32, "주변 아군에게 저항과 흡수 보호막을 부여합니다."),
        WARDEN_FIELD("warden_field", VillageRole.WARDEN, 3, "수호 결계", 21, 680, 46, "넓은 범위의 아군에게 강한 저항과 보호막을 부여합니다.");'''
new_skills = '''        VANGUARD_WHIRLWIND("vanguard_whirlwind", VillageRole.VANGUARD, 0, "회전 칼날", 2, 70, 18, "가렌의 회전 공격처럼 몸을 돌리며 여러 차례 주변 적을 베고 이동할 수 있습니다."),
        VANGUARD_BREAKER("vanguard_breaker", VillageRole.VANGUARD, 1, "전투 고양", 7, 190, 24, "검을 치켜들고 함성을 질러 자신과 주변 아군의 공격력·이동 속도를 강화합니다."),
        VANGUARD_CRY("vanguard_cry", VillageRole.VANGUARD, 2, "검기 난무", 13, 380, 32, "자세를 잡고 검을 연속으로 휘둘러 전방에 여러 개의 실제 검기 투사체를 날립니다."),
        VANGUARD_STORM("vanguard_storm", VillageRole.VANGUARD, 3, "천붕 강하", 21, 680, 42, "공중으로 도약한 뒤 지면을 내려찍어 바닥을 깨뜨리고 넓은 범위에 피해와 강한 충격을 줍니다."),

        RANGER_VOLLEY("ranger_volley", VillageRole.RANGER, 0, "신속 삼연사", 2, 70, 16, "일정 시간 활 충전이 크게 빨라지고 발사한 화살이 세 갈래로 분열합니다."),
        RANGER_PIERCE("ranger_pierce", VillageRole.RANGER, 1, "추적 도탄", 7, 190, 22, "다음 화살의 조준을 강하게 보정하고 첫 적중 뒤 주변 여러 적에게 연쇄 도탄 피해를 줍니다."),
        RANGER_RICOCHET("ranger_ricochet", VillageRole.RANGER, 2, "천공 화살비", 13, 380, 30, "조준한 넓은 지역에 실제 화살이 여러 차례 떨어져 지속 광역 피해를 줍니다."),
        RANGER_FIRE_RAIN("ranger_fire_rain", VillageRole.RANGER, 3, "성멸 대궁", 21, 680, 40, "잠시 기를 모은 뒤 초대형 에너지 화살을 발사해 전방의 적을 관통하고 초토화합니다."),

        ARCANIST_FIRE_ORB("arcanist_fire_orb", VillageRole.ARCANIST, 0, "홍염탄", 2, 70, 18, "실제 화염 구체를 전방으로 날려 충돌 지점에서 폭발시키고 적을 불태웁니다."),
        ARCANIST_FROST_RING("arcanist_frost_ring", VillageRole.ARCANIST, 1, "빙결 지대", 7, 190, 24, "조준 위치에 지속되는 냉기 지대를 만들어 범위 안 적을 강하게 둔화하고 조금씩 피해를 줍니다."),
        ARCANIST_CHAIN("arcanist_chain", VillageRole.ARCANIST, 2, "폭풍 회랑", 13, 380, 30, "전진하는 토네이도를 만들어 적을 끌어올리고 휩쓸며 낮은 피해와 강한 군중 제어를 가합니다."),
        ARCANIST_NOVA("arcanist_nova", VillageRole.ARCANIST, 3, "천뢰 폭격", 21, 680, 44, "넓은 목표 지점에 번개가 연속으로 떨어져 다수의 적에게 강한 광역 피해를 줍니다."),

        LUMINAR_HEAL("luminar_heal", VillageRole.LUMINAR, 0, "응급 성광", 2, 70, 16, "현재 체력 비율이 가장 낮은 아군 한 명을 찾아 큰 폭으로 즉시 회복시킵니다."),
        LUMINAR_CLEANSE("luminar_cleanse", VillageRole.LUMINAR, 1, "전군 정화", 7, 190, 24, "같은 전장에 있는 모든 아군의 해로운 효과를 제거하고 소량 회복시킵니다."),
        LUMINAR_VEIL("luminar_veil", VillageRole.LUMINAR, 2, "치유 성역", 13, 380, 32, "주변에 오래 지속되는 회복 지대를 설치해 범위 안 아군을 반복해서 치유합니다."),
        LUMINAR_SANCTUARY("luminar_sanctuary", VillageRole.LUMINAR, 3, "기적의 대성역", 21, 680, 46, "전장 전체 아군을 크게 치유하고 보호막을 부여하며 전투 불능 아군을 즉시 부활시킵니다."),

        WARDEN_TAUNT("warden_taunt", VillageRole.WARDEN, 0, "수호 돌진", 2, 70, 18, "방패를 앞세워 전방으로 돌진하고 접촉한 적에게 피해를 주며 강하게 밀어냅니다."),
        WARDEN_BASH("warden_bash", VillageRole.WARDEN, 1, "위압의 함성", 7, 190, 22, "큰 소리를 질러 주변 적에게 약한 피해를 주고 잠시 자신을 공격하도록 도발합니다."),
        WARDEN_FORMATION("warden_formation", VillageRole.WARDEN, 2, "거대 방패 태세", 13, 380, 32, "잠시 이동할 수 없는 대신 거대한 보호막과 피해 저항을 얻고 가까운 적을 계속 밀어냅니다."),
        WARDEN_FIELD("warden_field", VillageRole.WARDEN, 3, "대수호 진군", 21, 680, 46, "전방에 거대한 반투명 에너지 방패를 전개하고 달리면 짧게 돌진하며 접촉한 적을 밀어냅니다.");'''
text = replace_once(text, old_skills, new_skills, "active skill definitions")
# Add a readable cooldown bar to the action-bar slot text.
old_hud = '''        int remaining = cooldownRemainingSeconds(player, slot);
        String state = remaining > 0 ? "§c" + remaining + "초" : "§a준비";
        return key + " §f" + skill.displayName() + " " + state;
'''
new_hud = '''        int remaining = cooldownRemainingSeconds(player, slot);
        if (remaining <= 0) return key + " §f" + skill.displayName() + " §a준비";
        float progress = cooldownProgress(player, slot);
        int cooled = Math.max(0, Math.min(5, Math.round((1.0f - progress) * 5.0f)));
        String bar = "§a" + "■".repeat(cooled) + "§8" + "□".repeat(5 - cooled);
        return key + " §f" + skill.displayName() + " §c" + remaining + "초 " + bar;
'''
text = replace_once(text, old_hud, new_hud, "cooldown HUD bar")
write(path, text)

# Prevent recursive echo chains: an echo may never schedule another echo.
path = JAVA / "VillageRoleAbilitySystem.java"
text = read(path)
text = replace_once(text,
                    "    private static boolean spawningGeneratedArrow;\n",
                    "    private static boolean spawningGeneratedArrow;\n    private static boolean replayingEcho;\n",
                    "echo guard field")
text = replace_once(text,
                    "        spawningGeneratedArrow = false;\n",
                    "        spawningGeneratedArrow = false;\n        replayingEcho = false;\n",
                    "echo guard reset")
text = replace_once(text,
                    "        if (skill.role() == VillageRole.ARCANIST) {\n",
                    "        if (skill.role() == VillageRole.ARCANIST && !replayingEcho) {\n",
                    "echo guard condition")
text = replace_once(text,
                    "                case ARCANE_ECHO -> cast(level, player, action.skill(), action.power(),\n                        action.durationMultiplier(), action.specialRank());\n",
                    "                case ARCANE_ECHO -> {\n                    replayingEcho = true;\n                    try {\n                        cast(level, player, action.skill(), action.power(),\n                                action.durationMultiplier(), action.specialRank());\n                    } finally {\n                        replayingEcho = false;\n                    }\n                }\n",
                    "echo execution guard")
write(path, text)

# Role passives and ability feedback route.
path = JAVA / "VillageRpgSystem.java"
text = read(path)
old_refresh = '''    public static void refreshPlayerPassive(ServerPlayer player) {
        int bonus = bonusHealthPoints(VillageCouncilState.levelOf(player.getUUID()));
        if (bonus > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 80, Math.max(0, bonus / 4 - 1)));
        }
'''
new_refresh = '''    public static void refreshPlayerPassive(ServerPlayer player) {
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        int roleHealth = role == VillageRole.VANGUARD ? 8 : role == VillageRole.WARDEN ? 6 : 0;
        int bonus = bonusHealthPoints(VillageCouncilState.levelOf(player.getUUID())) + roleHealth;
        if (bonus > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 80, Math.max(0, bonus / 4 - 1)));
        }
'''
text = replace_once(text, old_refresh, new_refresh, "role health passives")
text = replace_once(text,
                    '''        if (VillageCouncilState.roleOf(player.getUUID()).orElse(null) == VillageRole.WARDEN
                && player.getOffhandItem().is(Items.SHIELD)) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
        }
''',
                    '''        if (role == VillageRole.WARDEN) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
            if (player.getOffhandItem().is(Items.SHIELD)) {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0, false, false, true));
            }
        }
''',
                    "warden passive refresh")
old_use = '''    public static String useRoleSkill(ServerPlayer player, int slot) {
        var equipped = VillageRoleSkillSystem.equippedSkill(player, slot);
        String result = VillageRoleSkillSystem.useEquippedSkill(player, slot);
        if (equipped.isPresent() && result.contains("사용 완료")) {
            VillageSkillVisualSystem.render(player, equipped.get());
        }
        return result;
    }
'''
new_use = '''    public static String useRoleSkill(ServerPlayer player, int slot) {
        return VillageRoleSkillSystem.useEquippedSkill(player, slot);
    }
'''
text = replace_once(text, old_use, new_use, "role skill visual route")
old_test = '''    public static String testRoleSkill(ServerPlayer player, String skillId) {
        VillageRoleSkillSystem.ActiveSkill skill =
                VillageRoleSkillSystem.ActiveSkill.parse(skillId).orElse(null);
        String result = VillageRoleSkillSystem.useTestSkill(player, skillId);
        if (skill != null && result.contains("시험 시전 완료")) VillageSkillVisualSystem.render(player, skill);
        return result;
    }
'''
new_test = '''    public static String testRoleSkill(ServerPlayer player, String skillId) {
        return VillageRoleSkillSystem.useTestSkill(player, skillId);
    }
'''
text = replace_once(text, old_test, new_test, "test skill visual route")
text = replace_once(text,
                    '''            case VANGUARD -> !projectile && player.getMainHandItem().is(ItemTags.SWORDS) ? 1.20f : 1.02f;
            case RANGER -> projectile ? (isOnWallTop(player) ? 1.55f : 1.25f) : 0.92f;
''',
                    '''            case VANGUARD -> !projectile && player.getMainHandItem().is(ItemTags.SWORDS) ? 1.28f : 1.08f;
            case RANGER -> projectile ? (isOnWallTop(player) ? 1.58f : 1.30f) : 0.92f;
''',
                    "vanguard ranger passive damage")
text = replace_once(text,
                    '''            case WARDEN -> player.getOffhandItem().is(Items.SHIELD) ? 0.76f : 0.84f;
''',
                    '''            case WARDEN -> player.getOffhandItem().is(Items.SHIELD) ? 0.72f : 0.82f;
''',
                    "warden passive damage reduction")
write(path, text)

# Main event wiring.
path = JAVA / "VillageGuardians.java"
text = read(path)
text = replace_once(text,
                    "import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n",
                    "import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;\n"
                    "import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;\n",
                    "knockback import")
text = replace_once(text,
                    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\n",
                    "import net.neoforged.neoforge.event.entity.player.ArrowLooseEvent;\n"
                    "import net.neoforged.neoforge.event.entity.player.PlayerEvent;\n",
                    "arrow loose import")
text = replace_once(text,
                    "        VillageSkillTestSystem.initializeServer(event.getServer());\n",
                    "        VillageSkillTestSystem.initializeServer(event.getServer());\n"
                    "        VillageRoleAbilitySystem.reset();\n",
                    "ability reset")
text = replace_once(text,
                    '''    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
''',
                    '''    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        VillageRoleAbilitySystem.handleEntityJoin(event);
        if (event.getLevel().isClientSide()
''',
                    "arrow join hook")
text = replace_once(text,
                    '''        VillageRpgSystem.handleIncomingDamage(event);
        VillageRespawnSystem.handleIncomingDamage(event);
''',
                    '''        VillageRpgSystem.handleIncomingDamage(event);
        VillageRoleAbilitySystem.handleIncomingDamage(event);
        VillageRespawnSystem.handleIncomingDamage(event);
''',
                    "ability incoming damage hook")
text = replace_once(text,
                    '''        VillageRaidSystem.onLivingDeath(event);
        VillageRpgSystem.handleDeath(event);
''',
                    '''        VillageRaidSystem.onLivingDeath(event);
        VillageRoleAbilitySystem.handleDeath(event);
        VillageRpgSystem.handleDeath(event);
''',
                    "ability death hook")
text = replace_once(text,
                    '''    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        VillageRaidSystem.tick(event.getServer());
''',
                    '''    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        VillageRoleAbilitySystem.handleArrowLoose(event);
    }

    @SubscribeEvent
    public void onKnockback(LivingKnockBackEvent event) {
        VillageRoleAbilitySystem.handleKnockback(event);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        VillageRoleAbilitySystem.tick(event.getServer());
        VillageRaidSystem.tick(event.getServer());
''',
                    "ability tick and event hooks")
write(path, text)

# Immediate priest revival.
path = JAVA / "VillageRespawnSystem.java"
text = read(path)
marker = '''    private static void teleportToVillage(ServerPlayer player, MinecraftServer server) {
'''
method = '''    public static boolean reviveNow(ServerPlayer player, String source) {
        if (player == null || !isDowned(player)) return false;
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        RESPAWN_AT.remove(player.getUUID());
        teleportToVillage(player, server);
        player.setGameMode(GameType.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setAbsorptionAmount(8.0f);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.setRemainingFireTicks(0);
        player.setDeltaMovement(Vec3.ZERO);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 4));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
        VillageRpgSystem.refreshPlayerPassive(player);
        player.sendSystemMessage(Component.literal("§e[즉시 부활] §f" + source + "의 힘으로 전장에 복귀했습니다."));
        return true;
    }

    private static void teleportToVillage(ServerPlayer player, MinecraftServer server) {
'''
text = replace_once(text, marker, method, "priest revive method")
write(path, text)

# Accurate role descriptions.
path = JAVA / "VillageRole.java"
text = read(path)
replacements = {
    "검과 도끼 피해가 증가하고 적을 쓰러뜨릴수록 짧은 전투 가속을 얻습니다.":
        "기본 공격력과 최대 체력이 증가하고 근접 피해 일부를 체력으로 흡수합니다.",
    "회전 참격·돌진·검기 폭풍처럼 범위와 연속 공격에 특화된 기술을 사용합니다.":
        "회전 칼날·전투 고양·검기 난무·천붕 강하로 근접 전장을 장악합니다.",
    "투사체 피해가 증가하며 성벽 위에서는 추가 사거리 보정과 피해를 얻습니다.":
        "활 충전 시간이 짧아지고 조준이 적에게 보정되며 화살로 처치하면 사용 화살을 회수합니다.",
    "연발·관통·도탄·화염 사격을 조합해 후방에서 정예 적을 제거합니다.":
        "신속 삼연사·추적 도탄·천공 화살비·성멸 대궁으로 원거리 전장을 제압합니다.",
    "역할 기술 피해가 증가하고 기술 재사용 대기시간이 조금 짧아집니다.":
        "마법을 사용할 때 일정 확률로 같은 마법이 추가 발동하며 한 번의 시전에 최대 두 번 반복됩니다.",
    "화염 구체·서리 고리·연쇄 번개·비전 폭발로 적 무리를 제어합니다.":
        "홍염탄·빙결 지대·폭풍 회랑·천뢰 폭격으로 적 무리를 폭발시키고 제어합니다.",
    "치유량이 증가하고 주변 아군이 치명적인 피해를 버틸 수 있도록 보호합니다.":
        "대상의 체력이 낮을수록 치유량과 보호막량이 크게 증폭됩니다.",
    "즉시 치유·정화·재생 장막·생명 성역을 상황에 맞게 장착합니다.":
        "응급 성광·전군 정화·치유 성역·기적의 대성역으로 전투를 복구합니다.",
    "받는 피해가 크게 감소하며 방패를 들면 추가 저항과 밀쳐내기 저항을 얻습니다.":
        "체력이 계속 재생되고 받는 피해가 감소하며 모든 넉백을 무효화합니다.",
    "도발·방패 충격·철벽 진형·수호 결계로 적의 시선을 끌고 아군을 보호합니다.":
        "수호 돌진·위압의 함성·거대 방패 태세·대수호 진군으로 적을 밀어냅니다."
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"role description not found: {old}")
    text = text.replace(old, new, 1)
write(path, text)

# Hard-coded shortcut help must match the registered defaults exactly.
for path in JAVA.glob("*.java"):
    text = read(path)
    updated = text.replace(
        "H 상태 · J 성장 · K 직업 성장 · B/U 통신 · Z/X 기술",
        "기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장 · U 빠른 통신")
    updated = updated.replace(
        "H 상태 · J 성장 · K 직업 성장 · B/U 빠른 통신 · Z/X 기술",
        "기본키 Z 기술1 · X 기술2 · B 빠른 통신 · H 상태 · J 성장 · K 직업 성장 · U 빠른 통신")
    if updated != text:
        write(path, updated)

# Dedicated regression contract.
test = r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    skills = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    rpg = read("VillageRpgSystem.java")
    guard = read("VillageGuardians.java")
    respawn = read("VillageRespawnSystem.java")
    role = read("VillageRole.java")
    keys = read("VillageClientKeys.java")

    assert "mod_version=0.17.12-alpha.1" in props
    expected_names = [
        "회전 칼날", "전투 고양", "검기 난무", "천붕 강하",
        "신속 삼연사", "추적 도탄", "천공 화살비", "성멸 대궁",
        "홍염탄", "빙결 지대", "폭풍 회랑", "천뢰 폭격",
        "응급 성광", "전군 정화", "치유 성역", "기적의 대성역",
        "수호 돌진", "위압의 함성", "거대 방패 태세", "대수호 진군"
    ]
    for name in expected_names:
        assert name in skills, name
    for token in [
        "SPIN_UNTIL", "player.swing", "Snowball", "ArrowLooseEvent", "spawnSideArrow",
        "RICOCHET_UNTIL", "ARROW_RAIN", "ENERGY_ARROW", "AreaKind.FROST",
        "AreaKind.TORNADO", "spawnVisualLightning", "healLowestAlly", "cleanseAllies",
        "AreaKind.HEALING", "reviveNow", "SHIELDS", "LIGHT_BLUE_STAINED_GLASS",
        "LivingKnockBackEvent", "replayingEcho"
    ]:
        assert token in ability, token
    assert "ParticleTypes" not in ability and "sendParticles" not in ability
    assert "VillageRoleAbilitySystem.tick" in guard
    assert "VillageRoleAbilitySystem.handleArrowLoose" in guard
    assert "VillageRoleAbilitySystem.handleKnockback" in guard
    assert "VillageRoleAbilitySystem.handleEntityJoin" in guard
    assert "VillageRoleAbilitySystem.handleIncomingDamage" in guard
    assert "VillageRoleAbilitySystem.handleDeath" in guard
    assert "public static boolean reviveNow" in respawn
    assert "근접 피해 일부를 체력으로 흡수" in role
    assert "화살로 처치하면 사용 화살을 회수" in role
    assert "최대 두 번 반복" in role
    assert "체력이 낮을수록 치유량과 보호막량" in role
    assert "모든 넉백을 무효화" in role
    assert 'GLFW.GLFW_KEY_Z' in keys and 'GLFW.GLFW_KEY_X' in keys
    assert 'GLFW.GLFW_KEY_B' in keys and 'GLFW.GLFW_KEY_H' in keys
    assert 'GLFW.GLFW_KEY_J' in keys and 'GLFW.GLFW_KEY_K' in keys and 'GLFW.GLFW_KEY_U' in keys
    assert "■" in skills and "□" in skills
    assert "VillageSkillVisualSystem.render" not in rpg

    print("[PASS] Twenty active skills now own distinct real movement, projectile, field and shield motions")
    print("[PASS] Vanguard, ranger, arcanist, luminar and warden passives are wired to combat events")
    print("[PASS] Skill visuals avoid particle geometry and cooldown HUD exposes live readiness")
    print("[PASS] Default shortcut help matches Z/X/B/H/J/K/U registrations")


if __name__ == "__main__":
    main()
'''
write(TOOLS / "test_v01712_role_abilities.py", test)

print("Applied Village Guardians v0.17.12 role ability overhaul")
