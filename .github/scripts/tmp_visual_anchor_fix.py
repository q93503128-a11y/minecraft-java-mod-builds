from pathlib import Path

root=Path('projects/arcane-circle')
tracker=root/'src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java'
audit=root/'tools/test_current_source.py'

body=tracker.read_text(encoding='utf-8')
old='''    private static Vec3 targetOffset(Visual visual){
        Vec3 d=visual.target.subtract(visual.center);
        return d.lengthSqr()<1.0E-8?visual.direction.scale(Math.max(1,visual.range)):d;
    }
'''
new='''    private static Vec3 targetOffset(Visual visual){
        // A zero offset is meaningful: TARGET/BODY/FEET spells are rendered around the same
        // authoritative point that owns the effect.  The old fallback displaced those visuals
        // forward by the entire range and was especially obvious on prisons, self auras and marks.
        return visual.target.subtract(visual.center);
    }
'''
if body.count(old)!=1: raise SystemExit('targetOffset contract not found exactly once')
tracker.write_text(body.replace(old,new),encoding='utf-8')

body=audit.read_text(encoding='utf-8')
needle="assert 'age < .90' in overhaul and 'elapsedSeconds / .30' in overhaul\n"
extra="assert 'return visual.target.subtract(visual.center);' in tracker\nassert 'visual.direction.scale(Math.max(1,visual.range))' not in tracker\n"
if needle not in body: raise SystemExit('alpha36 audit marker missing')
audit.write_text(body.replace(needle,needle+extra),encoding='utf-8')
