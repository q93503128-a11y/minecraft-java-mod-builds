#!/usr/bin/env python3
import json, os, re, subprocess, urllib.request, zipfile
from pathlib import Path

VID='xls8dTZv'
req=urllib.request.Request(f'https://api.modrinth.com/v2/version/{VID}',headers={'User-Agent':'SurvivalAscension-archive-audit/0.47'})
with urllib.request.urlopen(req,timeout=45) as r: meta=json.load(r)
files=meta.get('files') or []
f=next((x for x in files if x.get('primary') and 'neoforge' in x.get('filename','').lower()),None) or next(x for x in files if 'neoforge' in x.get('filename','').lower())
out=Path(os.environ.get('RUNNER_TEMP','/tmp'))/'sa047-archive'; out.mkdir(parents=True,exist_ok=True)
jar=out/f['filename']
req=urllib.request.Request(f['url'],headers={'User-Agent':'SurvivalAscension-archive-audit/0.47'})
with urllib.request.urlopen(req,timeout=90) as r, jar.open('wb') as w: w.write(r.read())
with zipfile.ZipFile(jar) as z:
    names=z.namelist()
    match=[n for n in names if re.search(r'(fractur|archive|dimension|biome|waystone|portal)',n,re.I)]
    print('MATCHING_ENTRIES')
    for n in match: print(n)
    print('TEXT_MATCHES')
    for n in names:
        if not n.endswith(('.json','.toml','.mcmeta')): continue
        try: txt=z.read(n).decode('utf-8')
        except Exception: continue
        if re.search(r'fractur|archive|waystone|dimension|biome|portal',txt,re.I):
            print('---',n,'---')
            for line in txt.splitlines():
                if re.search(r'fractur|archive|waystone|dimension|biome|portal',line,re.I): print(line[:500])
    classes=[n[:-6].replace('/','.') for n in names if n.endswith('.class') and re.search(r'(fractur|archive|dimension|waystone|portal)',n,re.I)]
print('FOCUSED_CLASSES')
for c in classes:
    print('===',c,'===')
    p=subprocess.run(['javap','-classpath',str(jar),'-c','-p','-verbose',c],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    for line in p.stdout.splitlines():
        if re.search(r'fractur|archive|waystone|dimension|biome|portal|ResourceKey|LevelStem|DIMENSION|LEVEL_STEM',line,re.I): print(line.strip()[:600])
