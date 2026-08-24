#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A55=ROOT/'tools/test_alpha55_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
a=text(A55).replace("print('Frontier Settlement alpha.23-55 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.55','0.1.0-alpha.56'); ns={'__file__':str(A55),'__name__':'__main__'}; exec(compile(a,str(A55),'exec'),ns,ns)
biome=text(JAVA/'settlement/SettlementOutpostBiomeService.java'); out=text(JAVA/'settlement/SettlementOutpostService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(biome,('FOREST_LOG_BONUS = 8','OPEN_FIELD_BONUS = 24','MOUNTAIN_STONE_BONUS = 8','MOUNTAIN_ORE_BONUS = 1','DRY_STONE_BONUS = 6','level.hasChunkAt(center)','level.getBiome(center)','Tags.Biomes.IS_FOREST','Tags.Biomes.IS_DENSE_VEGETATION','Tags.Biomes.IS_PLAINS','Tags.Biomes.IS_SAVANNA','Tags.Biomes.IS_MOUNTAIN','Tags.Biomes.IS_HILL','Tags.Biomes.IS_BADLANDS','Tags.Biomes.IS_SANDY'),'alpha.56 common biome soft seam')
forbid(biome,('terralith','Terralith','getChunk(','forceChunk','setChunkForced','ItemStack','setBlock(','Identifier.fromNamespaceAndPath'),'alpha.56 biome helper authority')
must(out,('SettlementOutpostBiomeService.bias(level, center)','ores += biomeBias.ore()','logs += biomeBias.logs()','fieldGround += biomeBias.field()','exposedStone += biomeBias.stone()','후보 · 환경 " + biomeBias.label()'),'alpha.56 existing specialization integration')
must(props,('mod_version=0.1.0-alpha.56','soft common-biome-tag evidence'),'alpha.56 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.56"','Alpha.56 reads only NeoForge common biome tags','no Terralith class/id hard dependency','"status": "candidate_runtime_lock"'),'alpha.56 lock')
print('Frontier Settlement alpha.23-56 cumulative source audit: PASS')
