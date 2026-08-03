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
    REGENERATING("regenerating", "불사 행렬", "적이 지속적으로 회복하고 치유병이 전열을 유지합니다.",
            "한 대상을 집중 공격하고 화염·처형 효과로 회복을 끊으세요.",
            0.88f, 1, 0, 0, 1.08f, 1.16f),
    PHALANX("phalanx", "방진 행군", "방패병·파쇄병·전쟁 고수가 밀집 대형으로 전진합니다.",
            "광역 마법과 측후방 공격으로 대형을 무너뜨리세요.",
            0.82f, 2, 0, -1, 1.24f, 1.30f),
    BLOOD_MOON("blood_moon", "혈월 습격", "광전사들이 강한 공격력과 재생을 지닌 채 몰려옵니다.",
            "짧은 시간에 집중 화력을 쏟아 회복 전에 마무리하세요.",
            1.12f, 0, 2, 1, 1.30f, 1.02f),
    STORMFRONT("stormfront", "폭풍 전선", "고속 원거리 병력과 주술사가 끊임없이 진형을 바꿉니다.",
            "엄폐와 추적 기술을 활용하고 후방 사수를 먼저 제거하세요.",
            0.98f, 0, 1, 2, 1.18f, 1.04f),
    RIFTED("rifted", "균열 군세", "사령술사와 정예병이 보호막을 두르고 혼합 편성으로 진군합니다.",
            "정화·폭발·관통 피해를 함께 운용해 보호막과 지원병을 동시에 끊으세요.",
            0.86f, 1, 1, 1, 1.38f, 1.22f);

    private static final int LONG_EFFECT_TICKS = 20 * 60 * 30;

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
        if (this == REGENERATING || this == BLOOD_MOON) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, LONG_EFFECT_TICKS, this == BLOOD_MOON ? 1 : 0));
        }
        if (this == IRONCLAD || this == PHALANX) {
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, this == PHALANX ? 1 : 0));
        }
        if (this == STORMFRONT) mob.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, LONG_EFFECT_TICKS, 1));
        if (this == RIFTED) mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, LONG_EFFECT_TICKS, 2));
    }

    public static VillageWaveTrait select(int day, int wave) {
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

    public static VillageWaveTrait fromId(String id) {
        if (id == null) return STANDARD;
        for (VillageWaveTrait trait : values()) if (trait.id.equals(id)) return trait;
        return STANDARD;
    }
}
