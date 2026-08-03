#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def require(path, *tokens):
    text = read(path)
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f'{path}: missing {missing}')
    return text

require('gradle.properties', 'mod_version=0.12.1-alpha.10')
require('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
        '0.12.1-alpha.10', 'RpgScaleService::onIncomingDamage',
        'ArcaneMageService::onIncomingDamage', 'ArcaneEncounterService::onDeath',
        'ArcaneEncounterService.tick(player)')
require('src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneVitalityService.java',
        'RPG_POINTS_PER_VANILLA_HEALTH = 5.0',
        'effectiveAmount * RPG_POINTS_PER_VANILLA_HEALTH')
require('src/main/java/kr/moonseungjun/arcanecircle/magic/RpgScaleService.java',
        'SCALED_TAG', 'MAX_HEALTH', 'event.getAmount() * ArcaneVitalityService.RPG_POINTS_PER_VANILLA_HEALTH')
require('src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneMageService.java',
        'Math.pow(1.85', 'Math.pow(1.72', 'onIncomingDamage', 'registerNamedMage')
require('src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterData.java',
        'arcane_encounters_v1', 'dead_named', 'defeated_bosses', 'Champion')
require('src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneEncounterService.java',
        '균열 황야', '빙결 묘역', '별먹는 심연',
        'arcanecircle_boss_', 'markNamedDead', 'markBossDefeated')
require('src/main/java/kr/moonseungjun/arcanecircle/world/FactionProfile.java',
        '청람의 아르덴', '백은의 세라핀', '녹월의 미렐', '재왕 바르카스')
require('src/main/java/kr/moonseungjun/arcanecircle/world/ArcaneQuestData.java',
        'fixed-difficulty', 'difficultyRoll', 'baseReward', '재앙',
        'mageCircle is retained only for binary/source compatibility')
require('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java',
        '고정 난이도·고정 보상', 'inspectedTradition', 'traditionDetail',
        'questListY', '_difficulty_name', '최강 ')
require('src/main/java/kr/moonseungjun/arcanecircle/network/ArcaneNetwork.java',
        'alpha10', 'factionSnapshot', '_difficulty_name', 'ArcaneEncounterService.zoneSummary')
require('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellMetrics.java',
        'effectRadius', 'waveEndRadius')
require('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java',
        'SpellMetrics.effectRadius', 'SpellMetrics.waveEndRadius',
        'case FIRE ->', 'case FROST ->', 'case WIND ->', 'case SPACE ->')
require('src/main/java/kr/moonseungjun/arcanecircle/magic/CombatGrowthService.java',
        'Math.pow(threat, 2.18)', 'threatMastery', 'Math.min(5000')
require('src/main/java/kr/moonseungjun/arcanecircle/magic/MageGearService.java',
        'RIFT_BOOTS', 'mayfly=true', 'CINDER_ROBE', 'GLACIER_ROBE', 'TEMPEST_ROBE')
require('src/main/java/kr/moonseungjun/arcanecircle/registry/ModItems.java',
        'CINDER_HOOD', 'GLACIER_CIRCLET', 'TEMPEST_HOOD', 'RIFT_CROWN', 'RIFT_BOOTS')
require('src/main/java/kr/moonseungjun/arcanecircle/world/AcademyOfferCatalog.java',
        'cinder_hat', 'glacier_hat', 'tempest_hat', 'rift_hat', 'rift_boots')
lang = json.loads(read('src/main/resources/assets/arcanecircle/lang/ko_kr.json'))
for item in ('cinder_hat','cinder_robe','cinder_boots','glacier_hat','glacier_robe','glacier_boots',
             'tempest_hat','tempest_robe','tempest_boots','rift_hat','rift_robe','rift_boots'):
    if f'item.arcanecircle.{item}' not in lang:
        raise SystemExit(f'missing language key for {item}')
    for path in (f'src/main/resources/assets/arcanecircle/items/{item}.json',
                 f'src/main/resources/assets/arcanecircle/models/item/{item}.json',
                 f'src/main/resources/assets/arcanecircle/textures/item/{item}.png'):
        if not (ROOT / path).exists():
            raise SystemExit(f'missing generated asset: {path}')
print('Arcane Circle v0.12.1-alpha.10 world-scale, quest, faction, encounter, range and equipment contract: PASS')
