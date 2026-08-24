from pathlib import Path

ROOT = Path('projects/arcane-circle')


def replace_once(path, old, new):
    p = Path(path)
    s = p.read_text(encoding='utf-8')
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected 1 match, got {count}: {old[:120]!r}')
    p.write_text(s.replace(old, new, 1), encoding='utf-8')


# 1) Every real grimoire page uses the same responsive shell.
handlers = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/client/ClientNetworkHandlers.java'
replace_once(
    handlers,
    '''        if ("atlas".equals(payload.page()) || "academy".equals(payload.page())) {
            Minecraft.getInstance().gui.setScreen(new PrimaryGrimoireScreen(payload.page()));
        } else {
            Minecraft.getInstance().gui.setScreen(new GrimoireScreen(payload.page()));
        }
''',
    '''        Minecraft.getInstance().gui.setScreen(new GrimoireScreen(payload.page()));
'''
)

# 2) Earthquake terrain faults now bridge the epicentre to 96% of the authoritative radius.
destructive = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/magic/DestructiveMagicService.java'
replace_once(
    destructive,
    '''        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0 * i / 12.0 + Math.sin(i * 1.91) * .15;
            double d = field * (.38 + .038 * (i % 4));
            Vec3 at = center.add(Math.cos(a) * d, -.22 - .08 * (i % 3), Math.sin(a) * d);
            queue(level, new RuptureTask(player.getUUID(), "earthquake", at,
                    Math.min(9.5, Math.max(4.0, field * (.16 + .012 * (i % 3)))),
                    power * (.68 + .045 * (i % 4)), level.getGameTime() + 1 + i / 2));
        }
''',
    '''        // six bounded zig-zag radial faults bridge core to authoritative edge
        int branches = 6;
        int steps = 4;
        for (int branch = 0; branch < branches; branch++) {
            double angle = Math.PI * 2.0 * branch / branches
                    + Math.sin(branch * 1.91 + player.getUUID().hashCode() * .001) * .12;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            for (int step = 1; step <= steps; step++) {
                double fraction = .18 + .78 * (step / (double) steps);
                double side = ((branch + step) & 1) == 0 ? -1.0 : 1.0;
                double lateral = side * Math.min(1.8, Math.max(.55, field * .04));
                Vec3 at = center.add(dx * field * fraction - dz * lateral,
                        -.24 - .09 * (step % 3), dz * field * fraction + dx * lateral);
                double localRadius = Math.min(5.2,
                        Math.max(2.6, field * (.095 + .006 * (step & 1))));
                double localPower = power * (.78 - .16 * (step / (double) steps));
                int serial = branch * steps + step - 1;
                queue(level, new RuptureTask(player.getUUID(), "earthquake", at, localRadius,
                        localPower, level.getGameTime() + 1 + serial / 5));
            }
        }
'''
)

# Version surfaces.
replace_once(ROOT / 'gradle.properties',
             'mod_version=0.12.1-alpha.75', 'mod_version=0.12.1-alpha.76')
replace_once(
    ROOT / 'gradle.properties',
    '# alpha.75 integrated progression audit: successful-use mastery floor, hostile-combat insight/economy, server purchase circle gate, high-circle spellbook rarity',
    '# alpha.76 live-play fix: unified responsive grimoire shell, restored academy shop visibility, 1-9 circle access, edge-reaching Earthquake faults'
)
replace_once(ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
             'VERSION = "0.12.1-alpha.75"', 'VERSION = "0.12.1-alpha.76"')

# Canonical metadata.
index_path = ROOT / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
replace_once(index_path,
             '"version": "0.12.1-alpha.75"', '"version": "0.12.1-alpha.76"')
replace_once(
    index_path,
    '  "catastrophe_destruction": "visible_footprint_tiled_across_bounded_ticks",\n',
    '''  "catastrophe_destruction": "visible_footprint_tiled_across_bounded_ticks",
  "liveplay_alpha76_hotfix": {
    "grimoire_shell": "all_non_sync_pages_use_responsive_grimoire_screen",
    "circle_selector": "all_1_to_9_entries_share_available_height_and_click_geometry",
    "academy_spellbook_shop": "responsive_offer_rows_restored_with_existing_server_purchase_gate",
    "earthquake_terrain": "six_radial_faults_bridge_core_to_96_percent_authoritative_radius_plus_tiled_footprint",
    "npc_terrain_safety": "unchanged_no_npc_world_edit"
  },
'''
)

# Permanent source audit regressions.
test_path = ROOT / 'tools/test_current_source.py'
t = test_path.read_text(encoding='utf-8')
t = t.replace('0.12.1-alpha.75', '0.12.1-alpha.76')
anchor = "# Alpha.75 integrated progression / economy authority pass.\n"
block = '''# Alpha.76 live-play grimoire / Earthquake regression pass.
handlers = text(client / 'ClientNetworkHandlers.java')
grimoire = text(client / 'GrimoireScreen.java')
destructive = text(magic / 'DestructiveMagicService.java')
need(handlers, 'Minecraft.getInstance().gui.setScreen(new GrimoireScreen(payload.page()));')
assert 'new PrimaryGrimoireScreen' not in handlers
need(grimoire,
     'for(int c=1;c<=9;c++)if(inside(e.x(),e.y(),l.circleIndex(c)))',
     'int circleStep(){int available=Math.max(9,contentBottom()-(body().y()+28));return Math.max(1,available/9);}',
     'AcademyOfferCatalog.forCircle(academyCircle)', 'l.offerRow(i,scroll)')
need(destructive,
     'int branches = 6;', 'int steps = 4;',
     'double fraction = .18 + .78 * (step / (double) steps);',
     'six bounded zig-zag radial faults')
need(main, 'DestructiveMagicService.tick(level);')
assert index['liveplay_alpha76_hotfix'] == {
    'grimoire_shell': 'all_non_sync_pages_use_responsive_grimoire_screen',
    'circle_selector': 'all_1_to_9_entries_share_available_height_and_click_geometry',
    'academy_spellbook_shop': 'responsive_offer_rows_restored_with_existing_server_purchase_gate',
    'earthquake_terrain': 'six_radial_faults_bridge_core_to_96_percent_authoritative_radius_plus_tiled_footprint',
    'npc_terrain_safety': 'unchanged_no_npc_world_edit',
}

'''
if t.count(anchor) != 1:
    raise SystemExit('test_current_source alpha75 anchor mismatch')
t = t.replace(anchor, block + anchor, 1)
print_anchor = "print('alpha75_successful_use_mastery_floor=PASS')\n"
prints = (
    "print('alpha76_single_responsive_grimoire_shell=PASS')\n"
    "print('alpha76_all_nine_circle_selector_accessible=PASS')\n"
    "print('alpha76_academy_spellbook_shop_restored=PASS')\n"
    "print('alpha76_earthquake_full_radius_fault_terrain=PASS')\n"
)
if t.count(print_anchor) != 1:
    raise SystemExit('test_current_source print anchor mismatch')
t = t.replace(print_anchor, prints + print_anchor, 1)
test_path.write_text(t, encoding='utf-8')

# JAR verifier identity / metadata / required runtime classes.
verify_path = ROOT / 'tools/verify_jar.py'
v = verify_path.read_text(encoding='utf-8').replace('0.12.1-alpha.75', '0.12.1-alpha.76')
required_anchor = "    'kr/moonseungjun/arcanecircle/client/PrimaryGrimoireScreen.class',\n"
required_new = (
    required_anchor
    + "    'kr/moonseungjun/arcanecircle/client/GrimoireScreen.class',\n"
    + "    'kr/moonseungjun/arcanecircle/client/ClientNetworkHandlers.class',\n"
    + "    'kr/moonseungjun/arcanecircle/magic/DestructiveMagicService.class',\n"
)
if v.count(required_anchor) != 1:
    raise SystemExit('verify required class anchor mismatch')
v = v.replace(required_anchor, required_new, 1)
verify_anchor = (
    "    if index.get('spell_contract_audit') != '109_explicit_summaries_and_runtime_routes':\n"
    "        raise SystemExit('109-spell audit metadata missing')\n"
)
verify_block = verify_anchor + (
    "    expected76 = {\n"
    "        'grimoire_shell': 'all_non_sync_pages_use_responsive_grimoire_screen',\n"
    "        'circle_selector': 'all_1_to_9_entries_share_available_height_and_click_geometry',\n"
    "        'academy_spellbook_shop': 'responsive_offer_rows_restored_with_existing_server_purchase_gate',\n"
    "        'earthquake_terrain': 'six_radial_faults_bridge_core_to_96_percent_authoritative_radius_plus_tiled_footprint',\n"
    "        'npc_terrain_safety': 'unchanged_no_npc_world_edit',\n"
    "    }\n"
    "    if index.get('liveplay_alpha76_hotfix') != expected76:\n"
    "        raise SystemExit('alpha.76 live-play hotfix metadata mismatch')\n"
)
if v.count(verify_anchor) != 1:
    raise SystemExit('verify metadata anchor mismatch')
v = v.replace(verify_anchor, verify_block, 1)
pass_anchor = "print('alpha75_successful_use_mastery_floor=PASS')\n"
pass_block = (
    "print('alpha76_single_responsive_grimoire_shell=PASS')\n"
    "print('alpha76_all_nine_circle_selector_accessible=PASS')\n"
    "print('alpha76_academy_spellbook_shop_restored=PASS')\n"
    "print('alpha76_earthquake_full_radius_fault_terrain=PASS')\n"
)
if v.count(pass_anchor) != 1:
    raise SystemExit('verify print anchor mismatch')
v = v.replace(pass_anchor, pass_block + pass_anchor, 1)
verify_path.write_text(v, encoding='utf-8')
