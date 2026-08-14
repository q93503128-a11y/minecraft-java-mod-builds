from pathlib import Path

root = Path('projects/arcane-circle')
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'
helper = Path('.github/scripts/tmp_visual_helper.txt')
(client / 'ArcaneSpellVisualOverhaul.java').write_text(helper.read_text(encoding='utf-8'), encoding='utf-8')


def replace(path: Path, old: str, new: str, count: int = 1):
    body = path.read_text(encoding='utf-8')
    actual = body.count(old)
    if actual != count:
        raise SystemExit(f'{path}: expected {count} copies, found {actual}: {old[:120]!r}')
    path.write_text(body.replace(old, new), encoding='utf-8')


tracker = client / 'WorldMagicTracker.java'
replace(tracker,
        'private static final int MAX_FRAME = 9000;\n    private static final int MAX_ENTRY = 2800;',
        'private static final int MAX_FRAME = 12000;\n    private static final int MAX_ENTRY = 3400;')

old_charge = '''        for(Visual v:CHARGES.values()){
            int color=SpellCinematicDirector.color(v.spell);
            ArcaneWorldMesh sigilMesh=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSigilDirector.charge(v.spell,v.direction,targetOffset(v),v.range,v.progress,v.fusion,v.startedAt));
            ArcaneWorldMesh cinematicMesh=MeteorBarragePattern.withSeed(v.seed,
                    ()->SpellCinematicDirector.charge(v.spell,v.direction,targetOffset(v),v.range,v.power,v.progress,v.fusion,v.startedAt));
            entries.add(new RenderEntry(v.center,sigilMesh,color));
            entries.add(new RenderEntry(v.center,cinematicMesh,color));
        }
'''
new_charge = '''        for(Visual v:CHARGES.values()){
            int color=SpellCinematicDirector.color(v.spell);
            if(!ArcaneSpellVisualOverhaul.replacesBaseSigil(v.spell)){
                ArcaneWorldMesh sigilMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSigilDirector.charge(v.spell,v.direction,targetOffset(v),v.range,v.progress,v.fusion,v.startedAt));
                entries.add(new RenderEntry(v.center,sigilMesh,color));
            }
            ArcaneWorldMesh authoredSigil=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.chargeSigil(v.spell,v.direction,v.progress,v.range,v.startedAt));
            if(authoredSigil.size()>0)entries.add(new RenderEntry(v.center,authoredSigil,color));
            if(!ArcaneSpellVisualOverhaul.replacesBaseChargeBody(v.spell)){
                ArcaneWorldMesh cinematicMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->SpellCinematicDirector.charge(v.spell,v.direction,targetOffset(v),v.range,v.power,v.progress,v.fusion,v.startedAt));
                entries.add(new RenderEntry(v.center,cinematicMesh,color));
            }
            ArcaneWorldMesh authoredBody=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.chargeBody(v.spell,v.direction,targetOffset(v),v.progress,v.range,v.startedAt));
            if(authoredBody.size()>0)entries.add(new RenderEntry(v.center,authoredBody,color));
        }
'''
replace(tracker, old_charge, new_charge)

old_release = '''        for(Visual v:RELEASES){
            double age=clamp((now-v.startedAt)/(double)Math.max(1L,v.expiresAt-v.startedAt),0,1);
            int color=SpellCinematicDirector.color(v.spell);
            if(!"prismatic_wall".equals(v.spell.id())){
                ArcaneWorldMesh echo=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSigilDirector.releaseEcho(v.spell,v.direction,targetOffset(v),v.range,age,v.fusion,v.startedAt));
                if(echo.size()>0)entries.add(new RenderEntry(v.center,echo,ArcaneSigilDirector.releaseEchoColor(color,age)));
            }
            // Seven coloured panels are the wall. The old pure-white base mesh was only scaffolding.
            if(!"prismatic_wall".equals(v.spell.id())){
                ArcaneWorldMesh releaseMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->SpellCinematicDirector.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                                age,v.impactAge,v.fusion,v.ingredients));
                entries.add(new RenderEntry(v.center,releaseMesh,color));
            }
            if(SpellCinematicDirector.isPrismatic(v.spell)){
                for(int layer=0;layer<7;layer++)entries.add(new RenderEntry(v.center,
                        SpellCinematicDirector.prismaticAccent(v.spell,v.direction,targetOffset(v),v.range,age,layer),
                        SpellCinematicDirector.prismaticColor(layer)));
            }
        }
'''
new_release = '''        for(Visual v:RELEASES){
            double age=clamp((now-v.startedAt)/(double)Math.max(1L,v.expiresAt-v.startedAt),0,1);
            double durationSeconds=Math.max(.05,(v.expiresAt-v.startedAt)/1_000_000_000.0);
            double elapsedSeconds=age*durationSeconds;
            int color=SpellCinematicDirector.color(v.spell);
            if(!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseSigil(v.spell)){
                ArcaneWorldMesh echo=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSigilDirector.releaseEcho(v.spell,v.direction,targetOffset(v),v.range,age,v.fusion,v.startedAt));
                if(echo.size()>0)entries.add(new RenderEntry(v.center,echo,ArcaneSigilDirector.releaseEchoColor(color,age)));
            }
            if(!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseRelease(v.spell)){
                ArcaneWorldMesh releaseMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->SpellCinematicDirector.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                                age,v.impactAge,v.fusion,v.ingredients));
                entries.add(new RenderEntry(v.center,releaseMesh,color));
            }
            ArcaneWorldMesh authoredRelease=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                            age,elapsedSeconds,durationSeconds,v.seed));
            if(authoredRelease.size()>0)entries.add(new RenderEntry(v.center,authoredRelease,color));
            if("prismatic_wall".equals(v.spell.id())){
                for(int layer=0;layer<7;layer++)entries.add(new RenderEntry(v.center,
                        ArcaneSpellVisualOverhaul.prismaticWallLayer(v.spell,v.direction,targetOffset(v),v.range,
                                age,elapsedSeconds,layer),SpellCinematicDirector.prismaticColor(layer)));
            }else if("prismatic_spray".equals(v.spell.id())){
                for(int layer=0;layer<7;layer++)entries.add(new RenderEntry(v.center,
                        SpellCinematicDirector.prismaticAccent(v.spell,v.direction,targetOffset(v),v.range,age,layer),
                        SpellCinematicDirector.prismaticColor(layer)));
            }
        }
'''
replace(tracker, old_release, new_release)

world = magic / 'WorldMagicService.java'
replace(world,
        'case PORTAL_GATE -> target.add(0.0, Math.max(1.1, profile.radius() * 0.52), 0.0);',
        'case PORTAL_GATE -> caster.position().add(0.0, 0.055, 0.0);')

gameplay = magic / 'SpellGameplayService.java'
replace(gameplay, 'case "prismatic_wall" -> 280;', 'case "prismatic_wall" -> 600;')

summary = magic / 'SpellEffectSummary.java'
replace(summary,
        'case "prismatic_wall" -> "14초 지속 7색 장벽 · 반복 피해·상태이상·통과 저지";',
        'case "prismatic_wall" -> "30초 지속 7색 장벽 · 반복 피해·상태이상·통과 저지";')

profile = magic / 'SpellPresentationProfile.java'
replace(profile,
        'put("prismatic_wall", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 10.80, 6, 7, 0, 0, 2.10, 4);',
        'put("prismatic_wall", SigilStyle.WALL_MATRIX, MotionStyle.WALL, 12.40, 6, 7, 0, 0, 2.38, 4);')
replace(profile,
        'put("gate", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 10.80, 6, 8, 0, 0, 2.25, 0);',
        'put("gate", SigilStyle.PORTAL_GATE, MotionStyle.PORTAL, 11.80, 6, 8, 0, 0, 2.35, 0);')
replace(profile,
        'put("control_weather", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 16.00, 6, 8, 0, 24, 1.88, 8);',
        'put("control_weather", SigilStyle.SKY_RITUAL, MotionStyle.STORM, 18.00, 6, 10, 0, 26, 2.02, 8);')
replace(profile,
        'put("world_sunder", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 14.50, 6, 12, 0, 0, 2.32, 5);',
        'put("world_sunder", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 16.00, 6, 12, 0, 0, 2.45, 5);')

# Bump the canonical alpha version everywhere it is intentionally asserted.
for path in [
    root / 'gradle.properties',
    root / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
    root / 'src/main/resources/data/arcanecircle/spell_catalog/index.json',
]:
    body = path.read_text(encoding='utf-8')
    if '0.12.1-alpha.35' not in body:
        raise SystemExit(f'missing alpha.35 version in {path}')
    path.write_text(body.replace('0.12.1-alpha.35', '0.12.1-alpha.36'), encoding='utf-8')

audit = root / 'tools/test_current_source.py'
body = audit.read_text(encoding='utf-8')
body = body.replace('0.12.1-alpha.35', '0.12.1-alpha.36')
body = body.replace("'MAX_FRAME = 9000','MAX_ENTRY = 2800'", "'MAX_FRAME = 12000','MAX_ENTRY = 3400'")
body = body.replace("'case \"prismatic_wall\"','14초 지속'", "'case \"prismatic_wall\"','30초 지속'")
body = body.replace("'case \"prismatic_wall\" -> 280'", "'case \"prismatic_wall\" -> 600'")
marker = '# Destruction budgets/classification and explicit World Sunder fissure.\n'
extra = '''# Alpha.36 presentation contract: spell-authored layers, upward portals/prisons and durable prism wall.\noverhaul=text(client/'ArcaneSpellVisualOverhaul.java')\nfor token in ['replacesBaseSigil','portalPair','risingPortal','risingPrison','prismaticWallLayer',\n              'temporalAstrolabe','wishCrown','executionFormula','tectonicFormula','materialWall',\n              'fieldAtmosphere','skyConvergence','impactFormula','terrainLift']:\n    assert token in overhaul, token\nfor token in ['ArcaneSpellVisualOverhaul.chargeSigil','ArcaneSpellVisualOverhaul.chargeBody',\n              'ArcaneSpellVisualOverhaul.release','ArcaneSpellVisualOverhaul.prismaticWallLayer',\n              'MAX_FRAME = 12000','MAX_ENTRY = 3400']:\n    assert token in tracker, token\nassert 'case PORTAL_GATE -> caster.position().add(0.0, 0.055, 0.0);' in world_magic\nassert 'case \"prismatic_wall\" -> 600;' in gameplay\nassert '30초 지속 7색 장벽' in summary\nassert 'age < .90' in overhaul and 'elapsedSeconds / .30' in overhaul\n\n'''
if marker not in body:
    raise SystemExit('audit insertion marker missing')
body = body.replace(marker, extra + marker)
audit.write_text(body, encoding='utf-8')

print('visual overhaul migration applied')
