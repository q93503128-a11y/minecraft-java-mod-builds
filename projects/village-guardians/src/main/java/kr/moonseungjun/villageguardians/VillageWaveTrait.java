package kr.moonseungjun.villageguardians;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

public enum VillageWaveTrait {
    STANDARD("standard", "정규 진군", "균형 잡힌 병력이 순서대로 밀고 들어옵니다.",
            "특정 약점이 없습니다. 성벽과 플레이어 화력을 고르게 운용하세요.",
            1.00f, 0, 0, 0, 1.00f, 1.00f),
    SWARM("swarm", "물량 공세", "체력이 낮은 돌격병과 척후병이 대량으로 합류합니다.",
            "화염탑·빙결탑·광역 기술이 특히 효과적입니다.",
            1.42f, -1, 0, 1, 0.90f, 0.82f),
    IRONCLAD("ironclad", "철갑 대열", "방패병과 중장갑 병력이 느리지만 단단하게 전진합니다.",
            "노포의 관통 분기, 마법 피해와 방어 약화가 유리합니다.",
            0.84f, 2, 0, -1, 0.92f, 1.24f),
    SIEGE("siege", "공성 대열", "폭파병과 성벽 파쇄병이 시설을 집중 공격합니다.",
            "우선 표적을 빠르게 제거하고 북문에 용병을 집중하세요.",
            0.96f, 1, 1, 0, 1.72f, 1.08f),
    HUNTERS("hunters", "사냥꾼 부대", "사수와 탑 사냥꾼이 후방에서 포탑과 수호자를 노립니다.",
            "성루사수와 근접 돌격으로 원거리 대열을 먼저 끊으세요.",
            0.92f, 0, 1, 1, 1.12f, 0.98f),
    HEXED("hexed", "저주 의식", "주술사와 사령술사가 약화·회복·증원을 반복합니다.",
            "주술사를 우선 처치하고 정화 기술과 비전탑을 준비하세요.",
            0.90f, 1, 0, 0, 1.18f, 1.10f),
    FRENZY("frenzy", "광란 돌격", "모든 적의 이동과 공격이 빨라집니다.",
            "빙결·둔화와 철벽수호자의 저항 기술이 핵심입니다.",
            1.08f, 0, 1, 2, 1.28f, 0.94f),
    REGENERATING("regenerating", "불사 행렬", "적이 진입 직후 짧게 재생하고 치유병이 제한된 회복으로 전열을 보조합니다.",
            "치유병을 먼저 끊으면 적의 총 회복량에는 명확한 한계가 있습니다.",
            0.88f, 1, 0, 0, 1.08f, 1.16f),
    PHALANX("phalanx", "방진 행군", "방패병·파쇄병·전쟁 고수가 밀집 대형으로 전진합니다.",
            "광역 마법과 측후방 공격으로 대형을 무너뜨리세요.",
            0.82f, 2, 0, -1, 1.24f, 1.30f),
    BLOOD_MOON("blood_moon", "혈월 습격", "광전사들이 강한 공격력과 짧은 진입 재생을 지닌 채 몰려옵니다.",
            "초기 재생이 끝난 뒤에는 지속 회복이 없으므로 전열을 끊어내세요.",
            1.12f, 0, 2, 1, 1.30f, 1.02f),
    STORMFRONT("stormfront", "폭풍 전선", "고속 원거리 병력과 주술사가 끊임없이 진형을 바꿉니다.",
            "엄폐와 추적 기술을 활용하고 후방 사수를 먼저 제거하세요.",
            0.98f, 0, 1, 2, 1.18f, 1.04f),
    RIFTED("rifted", "균열 군세", "사령술사와 정예병이 보호막을 두르고 혼합 편성으로 진군합니다.",
            "정화·폭발·관통 피해를 함께 운용해 보호막과 지원병을 동시에 끊으세요.",
            0.86f, 1, 1, 1, 1.38f, 1.22f),
    BREACH_STORM("breach_storm", "파성 집중전", "폭파병과 파쇄병이 한 전선에 힘을 모아 방어구역을 끊어냅니다.",
            "공병을 표시 즉시 끊고 수호자·빙결·집중 포격으로 접촉 시간을 줄이세요.",
            0.88f, 1, 0, 0, 1.55f, 1.14f),
    SKY_SIEGE("sky_siege", "천공 공성", "공중 병력 비중이 크게 늘고 지상 사수가 대공 대응을 방해합니다.",
            "대공 발사대와 성루사수를 분산 배치하고 폭격 경고 지점을 즉시 비우세요.",
            0.92f, 0, 0, 1, 1.16f, 1.06f),
    HUNTER_NET("hunter_net", "사냥망", "탑 사냥꾼·사수·주술사가 방어 화력과 플레이어를 동시에 압박합니다.",
            "포탑 교란병을 직접 끊고 원거리 대열에 돌진·관통 기술을 집중하세요.",
            0.90f, 0, 1, 0, 1.20f, 1.08f),
    DEATH_CHORUS("death_chorus", "사령 합창", "사령술사·전쟁 고수·주술사가 서로를 지원하며 전선을 오래 유지합니다.",
            "회복과 강화의 연결고리를 먼저 끊고 한 구역을 빠르게 붕괴시키세요.",
            0.82f, 1, 0, 0, 1.22f, 1.18f),
    IRON_TIDE("iron_tide", "철의 파도", "중장갑과 파쇄병이 넓은 전선에서 느리지만 끊임없이 압박합니다.",
            "관통·비전·후방 공격으로 방패 전열을 무너뜨리고 시설 접촉을 막으세요.",
            0.78f, 2, 0, 0, 1.30f, 1.26f),
    CATACLYSM("cataclysm", "재앙 혼성군", "공중·공성·지원 병과가 같은 웨이브에 섞여 우선순위를 흔듭니다.",
            "한 종류의 포탑에 의존하지 말고 대공·관통·억제 화력을 함께 준비하세요.",
            0.90f, 1, 1, 0, 1.38f, 1.20f),
    FINAL_HOST("final_host", "종말 군세", "최후 전쟁의 정예 편성이 모든 전선을 동시에 시험합니다.",
            "정찰 정보대로 병력을 나누고 지원병→공성병→전열 순으로 위협을 끊으세요.",
            0.84f, 2, 1, 0, 1.48f, 1.26f);

    private static final int LONG_EFFECT_TICKS = 20 * 60 * 30;
    private static final VillageWaveTrait[] BLACK_WAVE = {
            BREACH_STORM, RIFTED, BLOOD_MOON, SIEGE, HEXED, PHALANX
    };
    private static final VillageWaveTrait[] SKY_INVASION = {
            SKY_SIEGE, STORMFRONT, HUNTERS, BREACH_STORM, RIFTED, FRENZY
    };
    private static final VillageWaveTrait[] NECRO_SIEGE = {
            DEATH_CHORUS, RIFTED, HEXED, REGENERATING, HUNTER_NET, BREACH_STORM
    };
    private static final VillageWaveTrait[] IRON_ECLIPSE = {
            IRON_TIDE, PHALANX, IRONCLAD, BREACH_STORM, DEATH_CHORUS, HUNTER_NET
    };
    private static final VillageWaveTrait[] STORM_LEGION = {
            STORMFRONT, SKY_SIEGE, HUNTER_NET, IRON_TIDE, BREACH_STORM, DEATH_CHORUS
    };
    private static final VillageWaveTrait[] ABYSS_MARCH = {
            RIFTED, DEATH_CHORUS, CATACLYSM, IRON_TIDE, HUNTER_NET, SKY_SIEGE
    };
    private static final VillageWaveTrait[] DOOM_OFFENSIVE = {
            CATACLYSM, DEATH_CHORUS, IRON_TIDE, SKY_SIEGE, BREACH_STORM, HUNTER_NET, RIFTED
    };
    private static final VillageWaveTrait[] FINAL_WAR = {
            FINAL_HOST, CATACLYSM, DEATH_CHORUS, IRON_TIDE, SKY_SIEGE, BREACH_STORM, HUNTER_NET
    };
    private static final VillageWaveTrait[] FINAL_SIEGE = {
            BREACH_STORM, SKY_SIEGE, HUNTER_NET, DEATH_CHORUS, IRON_TIDE, CATACLYSM, FINAL_HOST
    };
    private static final VillageWaveTrait[] ENDLESS_WAR = {
            FINAL_HOST, CATACLYSM, IRON_TIDE, DEATH_CHORUS, SKY_SIEGE,
            BREACH_STORM, HUNTER_NET, RIFTED, PHALANX, BLOOD_MOON
    };

    private final String id;
    private final String displayName;
    private final String description;
    private final String counterHint;
    private final float countMultiplier;
    private final int healthBonus;
    private final int strengthBonus;
    private final int speedBonus;
    private final float structureDamageMultiplier;
    private final float healthScale;

    VillageWaveTrait(String id, String displayName, String description, String counterHint,
                     float countMultiplier, int healthBonus, int strengthBonus, int speedBonus,
                     float structureDamageMultiplier, float healthScale) {
        this.id = id; this.displayName = displayName; this.description = description;
        this.counterHint = counterHint; this.countMultiplier = countMultiplier;
        this.healthBonus = healthBonus; this.strengthBonus = strengthBonus; this.speedBonus = speedBonus;
        this.structureDamageMultiplier = structureDamageMultiplier; this.healthScale = healthScale;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public String counterHint() { return counterHint; }
    public float structureDamageMultiplier() { return structureDamageMultiplier; }
    public float healthScale() { return healthScale; }

    public int adjustedCount(int baseCount) {
        return Math.max(1, Math.round(Math.max(1, baseCount) * countMultiplier));
    }

    public void applyLongEffects(Mob mob) {
        if (healthBonus > 0) mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, LONG_EFFECT_TICKS, healthBonus - 1));
        if (strengthBonus > 0) mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG_EFFECT_TICKS, strengthBonus - 1));
        if (speedBonus > 0) mob.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG_EFFECT_TICKS, speedBonus - 1));
        if (this == REGENERATING) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0));
        } else if (this == BLOOD_MOON) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 4, 1));
        }
        if (this == IRONCLAD || this == PHALANX || this == IRON_TIDE) {
            int amplifier = this == PHALANX || this == IRON_TIDE ? 1 : 0;
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, amplifier));
        }
        if (this == STORMFRONT || this == SKY_SIEGE) {
            mob.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, LONG_EFFECT_TICKS, 1));
        }
        if (this == RIFTED) mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, LONG_EFFECT_TICKS, 2));
        if (this == FINAL_HOST) mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, LONG_EFFECT_TICKS, 1));
    }

    public static VillageWaveTrait select(int day, int wave) {
        int safeDay = Math.max(1, day);
        int safeWave = Math.max(1, wave);
        if (safeDay <= 19) return selectOpening(safeDay, safeWave);
        if (safeDay == VillageCampaignProgression.CAMPAIGN_END_DAY) return finalSiegeTrait(safeWave);

        VillageWaveTrait[] pool = safeDay > VillageCampaignProgression.CAMPAIGN_END_DAY ? ENDLESS_WAR
                : safeDay >= 90 ? FINAL_WAR
                : safeDay >= 80 ? DOOM_OFFENSIVE
                : safeDay >= 70 ? ABYSS_MARCH
                : safeDay >= 60 ? STORM_LEGION
                : safeDay >= 50 ? IRON_ECLIPSE
                : safeDay >= 40 ? NECRO_SIEGE
                : safeDay >= 30 ? SKY_INVASION
                : BLACK_WAVE;
        return selectFromPool(pool, safeDay, safeWave);
    }

    private static VillageWaveTrait selectOpening(int day, int wave) {
        if (day <= 1 && wave <= 1) return STANDARD;
        List<VillageWaveTrait> unlocked = new ArrayList<>();
        unlocked.add(STANDARD);
        if (day >= 2) unlocked.add(SWARM);
        if (day >= 3) unlocked.add(IRONCLAD);
        if (day >= 4) unlocked.add(SIEGE);
        if (day >= 5) unlocked.add(HUNTERS);
        if (day >= 6) unlocked.add(HEXED);
        if (day >= 7) unlocked.add(FRENZY);
        if (day >= 8) unlocked.add(REGENERATING);
        if (day >= 9) unlocked.add(PHALANX);
        if (day >= 10) unlocked.add(BLOOD_MOON);
        if (day >= 11) unlocked.add(STORMFRONT);
        if (day >= 12) unlocked.add(RIFTED);
        int index = Math.floorMod(day * 37 + wave * 19 + day * wave * 3, unlocked.size());
        return unlocked.get(index);
    }

    private static VillageWaveTrait selectFromPool(VillageWaveTrait[] pool, int day, int wave) {
        long seed = day * 31L + wave * 17L + (long) wave * wave * 7L + (long) day * wave * 3L;
        int index = (int) Math.floorMod(seed, (long) pool.length);
        return pool[index];
    }

    private static VillageWaveTrait finalSiegeTrait(int wave) {
        int index = Math.max(0, Math.min(FINAL_SIEGE.length - 1, wave - 1));
        return FINAL_SIEGE[index];
    }

    public static VillageWaveTrait fromId(String id) {
        if (id == null) return STANDARD;
        for (VillageWaveTrait trait : values()) if (trait.id.equals(id)) return trait;
        return STANDARD;
    }
}
