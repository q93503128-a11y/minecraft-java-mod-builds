from pathlib import Path

path = Path('.github/scripts/patch_erden_living_economy.py')
source = path.read_text()
old = '''old = \'\'\'            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            switch (site.role()) {
\'\'\'
new = \'\'\'            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            if (site.metric("operating_today") <= 0L) continue;
            switch (site.role()) {
\'\'\'
if text.count(old) != 2:
    raise SystemExit(f"operating day anchors count={text.count(old)}")
text = text.replace(old, new)
'''
new = '''text = replace_once(text,
\'\'\'            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            switch (site.role()) {
\'\'\',
\'\'\'            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            if (site.metric("operating_today") <= 0L) continue;
            switch (site.role()) {
\'\'\', "production operating day")
text = replace_once(text,
\'\'\'            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            long serviceUnits = 0L;
\'\'\',
\'\'\'            ErdenPhysicalEconomySavedData.SiteState site = sites.get(original.id());
            if (site.metric("operating_today") <= 0L) continue;
            long serviceUnits = 0L;
\'\'\', "service operating day")
'''
if source.count(old) != 1:
    raise SystemExit(f'patch script operating block count={source.count(old)}')
source = source.replace(old, new)
exec(compile(source, str(path), 'exec'))
