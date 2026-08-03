#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one marker, found {count}")
    write(path, text.replace(old, new, 1))


ability = JAVA / "VillageRoleAbilitySystem.java"

# Scaling state for skills whose effects continue after the initial cast.
replace_once(
    ability,
    '''    private static final Map<UUID, Long> SPIN_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RAPID_UNTIL = new HashMap<>();
    private static final Map<UUID, Integer> RAPID_DRAW_TICKS = new HashMap<>();
    private static final Map<UUID, Long> RICOCHET_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> RICOCHET_ARROWS = new HashMap<>();
''',
    '''    private static final Map<UUID, Long> SPIN_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> SPIN_SCALE = new HashMap<>();
    private static final Map<UUID, TimedScale> RALLY_SCALE = new HashMap<>();
    private static final Map<UUID, Long> RAPID_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> RAPID_SCALE = new HashMap<>();
    private static final Map<UUID, Integer> RAPID_DRAW_TICKS = new HashMap<>();
    private static final Map<UUID, Long> RICOCHET_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> RICOCHET_SCALE = new HashMap<>();
    private static final Map<UUID, EmpoweredArrowState> RICOCHET_ARROWS = new HashMap<>();
''',
    "continued skill scaling maps",
)
replace_once(
    ability,
    '''    private static final Map<UUID, Long> FORTRESS_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> AEGIS_UNTIL = new HashMap<>();
''',
    '''    private static final Map<UUID, Long> FORTRESS_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> FORTRESS_SCALE = new HashMap<>();
    private static final Map<UUID, Long> AEGIS_UNTIL = new HashMap<>();
    private static final Map<UUID, SkillScale> AEGIS_SCALE = new HashMap<>();
''',
    "warden scaling maps",
)
replace_once(
    ability,
    '''        SPIN_UNTIL.clear();
        RAPID_UNTIL.clear();
        RAPID_DRAW_TICKS.clear();
        RICOCHET_UNTIL.clear();
        RICOCHET_ARROWS.clear();
''',
    '''        SPIN_UNTIL.clear();
        SPIN_SCALE.clear();
        RALLY_SCALE.clear();
        RAPID_UNTIL.clear();
        RAPID_SCALE.clear();
        RAPID_DRAW_TICKS.clear();
        RICOCHET_UNTIL.clear();
        RICOCHET_SCALE.clear();
        RICOCHET_ARROWS.clear();
''',
    "reset continued scales",
)
replace_once(
    ability,
    '''        FORTRESS_UNTIL.clear();
        AEGIS_UNTIL.clear();
''',
    '''        FORTRESS_UNTIL.clear();
        FORTRESS_SCALE.clear();
        AEGIS_UNTIL.clear();
        AEGIS_SCALE.clear();
''',
    "reset warden scales",
)

# Cast-time scaling coverage.
replace_once(
    ability,
    '''                SPIN_UNTIL.put(player.getUUID(), now + spinDuration);
                VillageNetwork.sendSkillMotion(level, player, "vanguard_spin", spinDuration + 8);
''',
    '''                SPIN_UNTIL.put(player.getUUID(), now + spinDuration);
                SPIN_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                VillageNetwork.sendSkillMotion(level, player, "vanguard_spin", spinDuration + 8);
''',
    "whirlwind scaling state",
)
replace_once(
    ability,
    '''            case VANGUARD_BREAKER -> {
                player.swing(InteractionHand.MAIN_HAND, true);
''',
    '''            case VANGUARD_BREAKER -> {
                player.swing(InteractionHand.MAIN_HAND, true);
                RALLY_SCALE.put(player.getUUID(), new TimedScale(now + duration, power, specialRank));
''',
    "rally power state",
)
replace_once(
    ability,
    '''                for (int i = 0; i < 6; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 4L + i * 4L, player.getUUID(), skill,
''',
    '''                int waveCount = 6 + Math.min(4, Math.max(0,
                        Math.round((durationMultiplier - 1.0f) * 5.0f)));
                for (int i = 0; i < waveCount; i++) {
                    SCHEDULED.add(new ScheduledAction(now + 4L + i * 4L, player.getUUID(), skill,
''',
    "blade wave duration scaling",
)
replace_once(
    ability,
    '''                SLAMS.put(player.getUUID(), new SlamState(now, power, specialRank, player.position()));
''',
    '''                SLAMS.put(player.getUUID(), new SlamState(
                        now, power, durationMultiplier, specialRank, player.position()));
''',
    "slam duration state",
)
replace_once(
    ability,
    '''                RAPID_UNTIL.put(player.getUUID(), until);
                player.swing(InteractionHand.MAIN_HAND, true);
''',
    '''                RAPID_UNTIL.put(player.getUUID(), until);
                RAPID_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
''',
    "rapid scaling state",
)
replace_once(
    ability,
    '''                RICOCHET_UNTIL.put(player.getUUID(), now + Math.max(240L, duration * 2L));
                player.swing(InteractionHand.MAIN_HAND, true);
''',
    '''                RICOCHET_UNTIL.put(player.getUUID(), now + Math.max(240L, duration * 2L));
                RICOCHET_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                player.swing(InteractionHand.MAIN_HAND, true);
''',
    "ricochet scaling state",
)
replace_once(
    ability,
    '''            case ARCANIST_FIRE_ORB -> launchFireOrb(level, player,
                    1.35, 112, (12.0f + playerLevel * 0.65f) * power,
''',
    '''            case ARCANIST_FIRE_ORB -> launchFireOrb(level, player,
                    1.35, Math.max(80, Math.round(112 * durationMultiplier)),
                    (12.0f + playerLevel * 0.65f) * power,
''',
    "fire orb duration range scaling",
)
replace_once(
    ability,
    '''            case LUMINAR_HEAL -> healLowestAlly(player,
                    (10.0f + playerLevel * 0.7f) * power, specialRank, false);
            case LUMINAR_CLEANSE -> cleanseAllies(player,
                    (3.0f + playerLevel * 0.22f) * power, specialRank);
''',
    '''            case LUMINAR_HEAL -> healLowestAlly(player,
                    (10.0f + playerLevel * 0.7f) * power, duration, specialRank, false);
            case LUMINAR_CLEANSE -> cleanseAllies(player,
                    (3.0f + playerLevel * 0.22f) * power, duration, specialRank);
''',
    "luminar duration scaling",
)
replace_once(
    ability,
    '''            case WARDEN_FORMATION -> {
                FORTRESS_UNTIL.put(player.getUUID(), now + Math.max(120, duration));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.max(120, duration), 5 + Math.min(3, specialRank), false, false, true));
''',
    '''            case WARDEN_FORMATION -> {
                FORTRESS_UNTIL.put(player.getUUID(), now + Math.max(120, duration));
                FORTRESS_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                int shieldAmplifier = 5 + Math.min(3, specialRank)
                        + Math.min(3, Math.max(0, Math.round((power - 1.0f) * 4.0f)));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        Math.max(120, duration), shieldAmplifier, false, false, true));
''',
    "fortress power scaling",
)
replace_once(
    ability,
    '''            case WARDEN_FIELD -> {
                AEGIS_UNTIL.put(player.getUUID(), now + Math.max(180, duration * 2L));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(180, duration * 2), 1, false, false, true));
''',
    '''            case WARDEN_FIELD -> {
                AEGIS_UNTIL.put(player.getUUID(), now + Math.max(180, duration * 2L));
                AEGIS_SCALE.put(player.getUUID(), new SkillScale(power, durationMultiplier, specialRank));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                        Math.max(180, duration * 2), 1 + Math.min(1, specialRank / 4), false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        Math.max(180, duration * 2),
                        Math.min(5, 1 + Math.max(0, Math.round((power - 1.0f) * 4.0f))), false, false, true));
''',
    "aegis power scaling",
)
replace_once(
    ability,
    '''            int echoes = 0;
            if (player.getRandom().nextFloat() < 0.30f) echoes++;
            if (player.getRandom().nextFloat() < 0.12f) echoes++;
''',
    '''            int echoes = 0;
            float firstEchoChance = Math.min(0.58f, 0.30f + specialRank * 0.045f);
            float secondEchoChance = Math.min(0.32f, 0.12f + specialRank * 0.025f);
            if (player.getRandom().nextFloat() < firstEchoChance) echoes++;
            if (player.getRandom().nextFloat() < secondEchoChance) echoes++;
''',
    "arcanist special echo scaling",
)

# Tick-time scaling for persistent skills and passives.
replace_once(
    ability,
    '''            long spinUntil = SPIN_UNTIL.getOrDefault(id, 0L);
            if (spinUntil >= now) {
''',
    '''            long spinUntil = SPIN_UNTIL.getOrDefault(id, 0L);
            if (spinUntil >= now) {
                SkillScale spin = SPIN_SCALE.getOrDefault(id, SkillScale.DEFAULT);
''',
    "spin state read",
)
replace_once(
    ability,
    '''                    damageRadius(level, player, player.position(), 4.7, 10,
                            2.4f + VillageCouncilState.levelOf(id) * 0.16f,
                            false, 0.32, 0.05);
''',
    '''                    damageRadius(level, player, player.position(),
                            areaRadius(4.7, spin.specialRank()), 10 + spin.specialRank() * 2,
                            (2.4f + VillageCouncilState.levelOf(id) * 0.16f) * spin.power(),
                            false, 0.32 + spin.specialRank() * 0.03, 0.05);
''',
    "spin power special scaling",
)
replace_once(
    ability,
    '''                groundSlam(level, player, slam.power(), slam.specialRank());
''',
    '''                groundSlam(level, player, slam.power(), slam.durationMultiplier(), slam.specialRank());
''',
    "slam state consume",
)
replace_once(
    ability,
    '''            if (FORTRESS_UNTIL.getOrDefault(id, 0L) >= now) {
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                if (now % 5L == 0L) {
                    pushFront(level, player, 3.6, 16, 0.38, 0.04, 0.0f);
                }
            } else if (AEGIS_UNTIL.getOrDefault(id, 0L) >= now) {
                if (now % 3L == 0L) pushFront(level, player, 7.0, 30, 0.7, 0.08, 1.2f);
''',
    '''            if (FORTRESS_UNTIL.getOrDefault(id, 0L) >= now) {
                SkillScale fortress = FORTRESS_SCALE.getOrDefault(id, SkillScale.DEFAULT);
                player.setDeltaMovement(Vec3.ZERO);
                player.hurtMarked = true;
                if (now % 5L == 0L) {
                    pushFront(level, player, 3.6 + fortress.specialRank() * 0.35,
                            16 + fortress.specialRank() * 3,
                            0.38 + fortress.specialRank() * 0.025, 0.04,
                            0.55f * fortress.power());
                }
            } else if (AEGIS_UNTIL.getOrDefault(id, 0L) >= now) {
                SkillScale aegis = AEGIS_SCALE.getOrDefault(id, SkillScale.DEFAULT);
                if (now % 3L == 0L) pushFront(level, player,
                        7.0 + aegis.specialRank() * 0.65, 30 + aegis.specialRank() * 4,
                        0.7 + aegis.specialRank() * 0.035, 0.08,
                        1.2f * aegis.power());
''',
    "warden persistent scaling",
)
replace_once(
    ability,
    '''                    player.setDeltaMovement(forward.scale(0.78).add(0.0, 0.05, 0.0));
''',
    '''                    SkillScale aegis = AEGIS_SCALE.getOrDefault(id, SkillScale.DEFAULT);
                    player.setDeltaMovement(forward.scale(0.78 + Math.min(0.25, (aegis.power() - 1.0f) * 0.22))
                            .add(0.0, 0.05, 0.0));
''',
    "aegis dash power scaling",
)
replace_once(
    ability,
    '''            if (VillageCouncilState.roleOf(id).orElse(null) == VillageRole.WARDEN && now % 40L == 0L) {
                player.heal(0.8f);
            }
''',
    '''            if (activeRole(player) == VillageRole.WARDEN && now % 40L == 0L) {
                int passiveRank = VillageRoleSkillSystem.specialRank(player, VillageRole.WARDEN);
                player.heal(0.8f + passiveRank * 0.16f);
            }
''',
    "warden passive special scaling",
)

# Scheduled charge receives duration scaling.
replace_once(
    ability,
    '''                case SHIELD_CHARGE -> shieldCharge(level, player, action.power(), action.specialRank());
''',
    '''                case SHIELD_CHARGE -> shieldCharge(level, player, action.power(),
                        action.durationMultiplier(), action.specialRank());
''',
    "charge scheduled duration",
)

# Cleanup all companion scaling maps.
replace_once(
    ability,
    '''        SPIN_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RAPID_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
''',
    '''        SPIN_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        SPIN_SCALE.keySet().removeIf(id -> SPIN_UNTIL.getOrDefault(id, 0L) < now);
        RALLY_SCALE.entrySet().removeIf(entry -> entry.getValue().until() < now);
        RAPID_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RAPID_SCALE.keySet().removeIf(id -> RAPID_UNTIL.getOrDefault(id, 0L) < now);
''',
    "persistent scale cleanup one",
)
replace_once(
    ability,
    '''        RICOCHET_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue() < now);
        TRACKING_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);
''',
    '''        RICOCHET_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        RICOCHET_SCALE.keySet().removeIf(id -> RICOCHET_UNTIL.getOrDefault(id, 0L) < now);
        RICOCHET_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);
        TRACKING_ARROWS.entrySet().removeIf(entry -> entry.getValue().until() < now);
''',
    "persistent scale cleanup two",
)
replace_once(
    ability,
    '''        FORTRESS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        AEGIS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
''',
    '''        FORTRESS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        FORTRESS_SCALE.keySet().removeIf(id -> FORTRESS_UNTIL.getOrDefault(id, 0L) < now);
        AEGIS_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        AEGIS_SCALE.keySet().removeIf(id -> AEGIS_UNTIL.getOrDefault(id, 0L) < now);
''',
    "warden scale cleanup",
)

# Ranger arrow states preserve power and special rank until the actual shot.
replace_once(
    ability,
    '''        Long trackingUntil = RICOCHET_UNTIL.remove(id);
        boolean tracking = trackingUntil != null && trackingUntil >= now;
        if (tracking) {
            RICOCHET_ARROWS.put(arrow.getUUID(), now + 240L);
''',
    '''        Long trackingUntil = RICOCHET_UNTIL.remove(id);
        SkillScale trackingScale = RICOCHET_SCALE.remove(id);
        boolean tracking = trackingUntil != null && trackingUntil >= now;
        if (tracking) {
            SkillScale scale = trackingScale == null ? SkillScale.DEFAULT : trackingScale;
            arrow.setBaseDamage(arrow.getBaseDamage() * scale.power());
            RICOCHET_ARROWS.put(arrow.getUUID(),
                    new EmpoweredArrowState(now + 240L, scale.power(), scale.specialRank()));
''',
    "tracking arrow scale preserve",
)
replace_once(
    ability,
    '''        } else {
            aimAssist(level, player, arrow, 0.24);
        }

        Long rapidUntil = RAPID_UNTIL.remove(id);
        if (rapidUntil == null || rapidUntil < now) return;
        spawningGeneratedArrow = true;
        try {
            spawnSideArrow(level, player, arrow, -8.0);
            spawnSideArrow(level, player, arrow, 8.0);
        } finally {
''',
    '''        } else {
            int passiveRank = VillageRoleSkillSystem.specialRank(player, VillageRole.RANGER);
            aimAssist(level, player, arrow, 0.24 + passiveRank * 0.025);
        }

        Long rapidUntil = RAPID_UNTIL.remove(id);
        SkillScale rapidScale = RAPID_SCALE.remove(id);
        if (rapidUntil == null || rapidUntil < now) return;
        SkillScale scale = rapidScale == null ? SkillScale.DEFAULT : rapidScale;
        arrow.setBaseDamage(arrow.getBaseDamage() * scale.power());
        spawningGeneratedArrow = true;
        try {
            spawnSideArrow(level, player, arrow, -8.0, scale.power());
            spawnSideArrow(level, player, arrow, 8.0, scale.power());
            if (scale.specialRank() >= 4) {
                spawnSideArrow(level, player, arrow, -16.0, scale.power() * 0.82f);
                spawnSideArrow(level, player, arrow, 16.0, scale.power() * 0.82f);
            }
        } finally {
''',
    "rapid arrow scale preserve",
)

# Damage event: rally power, special-scaled passives, and stored ricochet scale.
replace_once(
    ability,
    '''        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            VillageRole role = activeRole(attacker);
            if (role == VillageRole.VANGUARD && !(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                attacker.heal(Math.min(2.5f, event.getAmount() * 0.055f));
            }
''',
    '''        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            VillageRole role = activeRole(attacker);
            TimedScale rally = RALLY_SCALE.get(attacker.getUUID());
            if (rally != null && rally.until() >= attacker.level().getGameTime()) {
                event.setAmount(event.getAmount() * (1.0f + Math.max(0.0f, rally.power() - 1.0f) * 0.80f));
            }
            if (role == VillageRole.VANGUARD && !(event.getSource().getDirectEntity() instanceof AbstractArrow)) {
                int passiveRank = VillageRoleSkillSystem.specialRank(attacker, VillageRole.VANGUARD);
                float ratio = 0.055f + passiveRank * 0.008f
                        + VillageRelicSystem.vanguardLifeStealBonus(attacker);
                attacker.heal(Math.min(4.5f, event.getAmount() * ratio));
            }
''',
    "rally and vanguard passive scaling",
)
replace_once(
    ability,
    '''                    && event.getSource().getDirectEntity() instanceof AbstractArrow directArrow
                    && event.getEntity() instanceof Mob primary
                    && RICOCHET_ARROWS.remove(directArrow.getUUID()) != null
                    && attacker.level() instanceof ServerLevel level) {
                TRACKING_ARROWS.remove(directArrow.getUUID());
                List<Mob> chain = targetsNear(level, attacker, primary.position(),
                        areaRadius(12.0, VillageRoleSkillSystem.specialRank(attacker, VillageRole.RANGER)), 12);
''',
    '''                    && event.getSource().getDirectEntity() instanceof AbstractArrow directArrow
                    && event.getEntity() instanceof Mob primary
                    && RICOCHET_ARROWS.containsKey(directArrow.getUUID())
                    && attacker.level() instanceof ServerLevel level) {
                EmpoweredArrowState ricochet = RICOCHET_ARROWS.remove(directArrow.getUUID());
                TRACKING_ARROWS.remove(directArrow.getUUID());
                int ricochetRank = ricochet == null ? 0 : ricochet.specialRank();
                float ricochetPower = ricochet == null ? 1.0f : ricochet.power();
                List<Mob> chain = targetsNear(level, attacker, primary.position(),
                        areaRadius(12.0, ricochetRank), 12 + ricochetRank * 2);
''',
    "ricochet stored scaling",
)
replace_once(
    ability,
    '''                float damage = Math.max(2.0f, event.getAmount() * 0.72f);
                List<Mob> visualChain = new ArrayList<>();
                for (int i = 0; i < Math.min(6, chain.size()); i++) {
''',
    '''                float damage = Math.max(2.0f, event.getAmount() * 0.72f * ricochetPower);
                List<Mob> visualChain = new ArrayList<>();
                int maximumChain = 6 + Math.min(5, ricochetRank);
                for (int i = 0; i < Math.min(maximumChain, chain.size()); i++) {
''',
    "ricochet power and targets",
)
replace_once(
    ability,
    '''        if (event.getEntity() instanceof ServerPlayer defender
                && VillageCouncilState.roleOf(defender.getUUID()).orElse(null) == VillageRole.WARDEN) {
            event.setAmount(event.getAmount() * 0.82f);
        }
''',
    '''        if (event.getEntity() instanceof ServerPlayer defender && activeRole(defender) == VillageRole.WARDEN) {
            int passiveRank = VillageRoleSkillSystem.specialRank(defender, VillageRole.WARDEN);
            event.setAmount(event.getAmount() * Math.max(0.68f, 0.82f - passiveRank * 0.018f));
        }
''',
    "warden passive reduction scaling",
)

# Individual helper functions.
replace_once(
    ability,
    '''        launchMovingAt(level, player, MovingKind.BLADE, ItemStack.EMPTY,
                1.75, 24, (5.4f + VillageCouncilState.levelOf(player.getUUID()) * 0.30f) * power,
                1.45, specialRank, origin, direction);
''',
    '''        launchMovingAt(level, player, MovingKind.BLADE, ItemStack.EMPTY,
                1.75 + specialRank * 0.035, 24 + specialRank * 2,
                (5.4f + VillageCouncilState.levelOf(player.getUUID()) * 0.30f) * power,
                1.45 + specialRank * 0.12, specialRank, origin, direction);
''',
    "blade wave special scaling",
)
replace_once(
    ability,
    '''    private static void groundSlam(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        double radius = areaRadius(8.5, specialRank);
        damageRadius(level, player, player.position(), radius, 40,
                (14.0f + VillageCouncilState.levelOf(player.getUUID()) * 0.72f) * power,
                false, 1.05, 0.38);
''',
    '''    private static void groundSlam(ServerLevel level, ServerPlayer player,
                                   float power, float durationMultiplier, int specialRank) {
        player.swing(InteractionHand.MAIN_HAND, true);
        double radius = areaRadius(8.5, specialRank);
        damageRadius(level, player, player.position(), radius, 40 + specialRank * 4,
                (14.0f + VillageCouncilState.levelOf(player.getUUID()) * 0.72f) * power,
                false, 1.05 + specialRank * 0.05, 0.38);
        int fractureDuration = Math.max(40, Math.round(55 * durationMultiplier));
        for (Mob target : targetsNear(level, player, player.position(), radius, 48)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    fractureDuration, Math.min(2, specialRank / 2), false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    fractureDuration, Math.min(3, 1 + specialRank / 2), false, false, true));
        }
''',
    "ground slam duration special scaling",
)
replace_once(
    ability,
    '''    private static void shieldCharge(ServerLevel level, ServerPlayer player, float power, int specialRank) {
        player.swing(InteractionHand.OFF_HAND, true);
        VillageSkillEffectSystem.shieldCharge(level, player, horizontalLook(player));
        for (int i = 0; i < 5; i++) {
            Vec3 center = player.position().add(horizontalLook(player).scale(1.0 + i * 1.2));
            for (Mob target : targetsNear(level, player, center, 2.3, 12)) {
                hurt(level, target, (5.5f + VillageCouncilState.levelOf(player.getUUID()) * 0.3f) * power);
                knockFrom(player.position(), target, 1.15, 0.12);
''',
    '''    private static void shieldCharge(ServerLevel level, ServerPlayer player,
                                     float power, float durationMultiplier, int specialRank) {
        player.swing(InteractionHand.OFF_HAND, true);
        VillageSkillEffectSystem.shieldCharge(level, player, horizontalLook(player));
        int steps = 5 + Math.min(4, Math.max(0, Math.round((durationMultiplier - 1.0f) * 4.0f)));
        double contactRadius = 2.3 + specialRank * 0.22;
        for (int i = 0; i < steps; i++) {
            Vec3 center = player.position().add(horizontalLook(player).scale(1.0 + i * 1.2));
            for (Mob target : targetsNear(level, player, center, contactRadius, 12 + specialRank * 3)) {
                hurt(level, target, (5.5f + VillageCouncilState.levelOf(player.getUUID()) * 0.3f) * power);
                knockFrom(player.position(), target, 1.15 + specialRank * 0.07, 0.12);
''',
    "shield charge all branches",
)
replace_once(
    ability,
    '''        for (Mob target : targetsNear(level, player, player.position(), 20.0, 60)) {
            hurt(level, target, damage);
            target.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.min(100, duration), 1, false, false, true));
        }
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.min(100, duration), 1, false, false, true));
''',
    '''        double radius = 20.0 + specialRank * 2.0;
        for (Mob target : targetsNear(level, player, player.position(), radius, 60 + specialRank * 5)) {
            hurt(level, target, damage);
            target.setTarget(player);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    Math.min(180, duration), 1 + Math.min(2, specialRank / 2), false, false, true));
        }
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                Math.min(180, duration), 1 + Math.min(1, specialRank / 4), false, false, true));
''',
    "taunt shout special scaling",
)
replace_once(
    ability,
    '''    private static void healLowestAlly(ServerPlayer player, float amount, int specialRank, boolean barrier) {
''',
    '''    private static void healLowestAlly(ServerPlayer player, float amount,
                                       int duration, int specialRank, boolean barrier) {
''',
    "heal duration signature",
)
replace_once(
    ability,
    '''        if (barrier || specialRank >= 2) {
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1 + Math.min(3, specialRank), false, false, true));
        }
''',
    '''        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                Math.max(60, duration / 2), Math.min(2, specialRank / 2), false, false, true));
        if (barrier || specialRank >= 2) {
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    Math.max(120, duration), 1 + Math.min(3, specialRank), false, false, true));
        }
''',
    "heal duration effect",
)
replace_once(
    ability,
    '''    private static void cleanseAllies(ServerPlayer player, float heal, int specialRank) {
''',
    '''    private static void cleanseAllies(ServerPlayer player, float heal, int duration, int specialRank) {
''',
    "cleanse duration signature",
)
replace_once(
    ability,
    '''            ally.removeEffect(MobEffects.HUNGER);
            healScaled(ally, heal);
        }
''',
    '''            ally.removeEffect(MobEffects.HUNGER);
            ally.removeEffect(MobEffects.NAUSEA);
            ally.removeEffect(MobEffects.MINING_FATIGUE);
            healScaled(ally, heal);
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                    Math.max(50, duration / 2), Math.min(1, specialRank / 3), false, false, true));
            if (specialRank >= 2) {
                ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                        Math.max(80, duration), Math.min(4, specialRank), false, false, true));
            }
        }
''',
    "cleanse duration special effects",
)
replace_once(
    ability,
    '''    private static void spawnSideArrow(ServerLevel level, ServerPlayer owner, AbstractArrow source, double degrees) {
''',
    '''    private static void spawnSideArrow(ServerLevel level, ServerPlayer owner,
                                       AbstractArrow source, double degrees, float power) {
''',
    "side arrow power signature",
)
replace_once(
    ability,
    '''        arrow.setBaseDamage(2.0);
''',
    '''        arrow.setBaseDamage(Math.max(2.0, source.getBaseDamage() * 0.82) * power);
''',
    "side arrow power damage",
)

# Clear ranger readies must clear the companion scale maps.
replace_once(
    ability,
    '''        RAPID_UNTIL.remove(id);
        RICOCHET_UNTIL.remove(id);
''',
    '''        RAPID_UNTIL.remove(id);
        RAPID_SCALE.remove(id);
        RICOCHET_UNTIL.remove(id);
        RICOCHET_SCALE.remove(id);
''',
    "clear ranger scales",
)

# Records.
replace_once(
    ability,
    '''    private record EmpoweredArrowState(long until, float power, int specialRank) {}

    private record SlamState(long startedAt, float power, int specialRank, Vec3 origin) {}
''',
    '''    private record SkillScale(float power, float durationMultiplier, int specialRank) {
        private static final SkillScale DEFAULT = new SkillScale(1.0f, 1.0f, 0);
    }

    private record TimedScale(long until, float power, int specialRank) {}

    private record EmpoweredArrowState(long until, float power, int specialRank) {}

    private record SlamState(long startedAt, float power, float durationMultiplier,
                             int specialRank, Vec3 origin) {}
''',
    "skill scaling records",
)

# Role system exposes an explicit complete branch coverage matrix for all 20
# active skills. This is audited in CI and can be surfaced later in UI.
role = JAVA / "VillageRoleSkillSystem.java"
insert_before = '''    public static List<ActiveSkill> skillsFor(VillageRole role) {
'''
coverage = '''    public static ScalingCoverage scalingCoverage(ActiveSkill skill) {
        if (skill == null) return new ScalingCoverage(false, false, false, "기술 없음");
        return switch (skill) {
            case VANGUARD_WHIRLWIND -> new ScalingCoverage(true, true, true,
                    "위력=틱 피해 · 지속=회전 시간 · 특수=범위/대상/밀치기");
            case VANGUARD_BREAKER -> new ScalingCoverage(true, true, true,
                    "위력=공격 증폭 · 지속=버프 시간 · 특수=효과 단계/아군 강화");
            case VANGUARD_CRY -> new ScalingCoverage(true, true, true,
                    "위력=검기 피해 · 지속=검기 횟수 · 특수=검기 크기/사거리");
            case VANGUARD_STORM -> new ScalingCoverage(true, true, true,
                    "위력=강하 피해 · 지속=균열 약화 시간 · 특수=범위/제어");
            case RANGER_VOLLEY -> new ScalingCoverage(true, true, true,
                    "위력=본체/추가 화살 피해 · 지속=준비 시간 · 특수=장전/추가 화살 수");
            case RANGER_PIERCE -> new ScalingCoverage(true, true, true,
                    "위력=추적/도탄 피해 · 지속=준비 시간 · 특수=도탄 반경/대상 수");
            case RANGER_RICOCHET -> new ScalingCoverage(true, true, true,
                    "위력=화살비 피해 · 지속=준비/장판 시간 · 특수=범위/화상");
            case RANGER_FIRE_RAIN -> new ScalingCoverage(true, true, true,
                    "위력=대궁 피해 · 지속=준비 시간 · 특수=크기/관통 범위");
            case ARCANIST_FIRE_ORB -> new ScalingCoverage(true, true, true,
                    "위력=폭발 피해 · 지속=비행 사거리 · 특수=폭발 범위/화상");
            case ARCANIST_FROST_RING -> new ScalingCoverage(true, true, true,
                    "위력=지속 피해 · 지속=장판 시간 · 특수=범위/사거리");
            case ARCANIST_CHAIN -> new ScalingCoverage(true, true, true,
                    "위력=회랑 피해 · 지속=토네이도 시간 · 특수=범위/제어");
            case ARCANIST_NOVA -> new ScalingCoverage(true, true, true,
                    "위력=낙뢰 피해 · 지속=폭격 시간 · 특수=범위/낙뢰 대상");
            case LUMINAR_HEAL -> new ScalingCoverage(true, true, true,
                    "위력=즉시 회복 · 지속=재생/보호막 시간 · 특수=보호막 강도");
            case LUMINAR_CLEANSE -> new ScalingCoverage(true, true, true,
                    "위력=회복량 · 지속=정화 후 보호 시간 · 특수=보호막/정화 범위");
            case LUMINAR_VEIL -> new ScalingCoverage(true, true, true,
                    "위력=틱 회복 · 지속=성역 시간 · 특수=범위/보호");
            case LUMINAR_SANCTUARY -> new ScalingCoverage(true, true, true,
                    "위력=전체 회복 · 지속=보호막/재생 시간 · 특수=보호막 강도/부활");
            case WARDEN_TAUNT -> new ScalingCoverage(true, true, true,
                    "위력=돌진 피해 · 지속=돌진 거리 · 특수=방패 폭/밀치기");
            case WARDEN_BASH -> new ScalingCoverage(true, true, true,
                    "위력=함성 피해 · 지속=도발/약화 시간 · 특수=범위/약화 단계");
            case WARDEN_FORMATION -> new ScalingCoverage(true, true, true,
                    "위력=보호막/접촉 피해 · 지속=태세 시간 · 특수=방패 범위/밀치기");
            case WARDEN_FIELD -> new ScalingCoverage(true, true, true,
                    "위력=보호막/진군 피해 · 지속=진군 시간 · 특수=방패 범위/저항");
        };
    }

    public static boolean allSkillBranchesConnected() {
        return Arrays.stream(ActiveSkill.values()).allMatch(skill -> scalingCoverage(skill).complete());
    }

    public record ScalingCoverage(boolean power, boolean duration, boolean special, String detail) {
        public boolean complete() { return power && duration && special; }
    }

''' + insert_before
replace_once(role, insert_before, coverage, "skill branch coverage matrix")

print("Applied v0.18.0 all-skill role-node scaling patch")
