from pathlib import Path

repo=Path(__file__).resolve().parents[2]
original=repo/'.github/scripts/arcane_alpha32_destruction_barrage_20260813.py'
source=original.read_text(encoding='utf-8')
old="r'    private static void meteorSwarm\\(ArcaneWorldMesh\\.Builder m,Vec3 target,double age,double impact,double scale\\)\\{.*?\\n    \\}\\n\\n    private static void executionWord'"
new="r'    private static void meteorSwarm\\(ArcaneWorldMesh\\.Builder m,Vec3 target,double age,double impact,double scale\\)\\{.*?\\n    \\}\\n    private static void executionWord'"
if source.count(old)!=1:
    raise SystemExit(f'expected one meteor regex token, found {source.count(old)}')
source=source.replace(old,new,1)
exec(compile(source,str(original),'exec'),{'__file__':str(original),'__name__':'__main__'})
for path in [repo/'.github/scripts/arcane_alpha32_retry_20260813.py',repo/'.github/workflows/maintenance-arcane32-retry-20260813.yml']:
    if path.exists(): path.unlink()
print('alpha.32 retry wrapper complete')
