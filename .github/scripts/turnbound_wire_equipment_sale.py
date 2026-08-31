from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "projects" / "turnbound"


def path(*parts: str) -> Path:
    return PROJECT.joinpath(*parts)


def replace_exact(file: Path, old: str, new: str, expected: int = 1) -> None:
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{file.relative_to(ROOT)}: expected {expected} match(es), found {count}")
    file.write_text(text.replace(old, new), encoding="utf-8")


store = path("src/main/java/io/github/q93503128/turnbound/world/CampaignProgressStore.java")
replace_exact(store,
'''    public static EquipmentInventory.Item buyEquipment(UUID playerId, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = EquipmentRules.buyNormal(progress.equipment, progress.profile, itemId, shopChapter(progress));
        progress.dirty = true;
        return item;
    }
''',
'''    public static EquipmentInventory.Item buyEquipment(UUID playerId, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = EquipmentRules.buyNormal(progress.equipment, progress.profile, itemId, shopChapter(progress));
        progress.dirty = true;
        return item;
    }

    public static int sellEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        int gold = progress.equipment.sell(instanceId, progress.profile);
        progress.dirty = true;
        return gold;
    }
''')

snapshot = path("src/main/java/io/github/q93503128/turnbound/world/MetaUiSnapshot.java")
replace_exact(snapshot,
'''    public record EquipmentRow(
            String instanceId, String itemId, String name, String tier, String slot, int enhancement,
            String equippedCharacterId, String mainType, double mainValue, String subType, double subValue,
            double mainAt20, double subAt20) {}
''',
'''    public record EquipmentRow(
            String instanceId, String itemId, String name, String tier, String slot, int enhancement,
            String equippedCharacterId, String mainType, double mainValue, String subType, double subValue,
            double mainAt20, double subAt20, int salePrice, boolean sellable) {}
''')

codec = path("src/main/java/io/github/q93503128/turnbound/world/MetaUiCodec.java")
replace_exact(codec,
'''                .append(row.equippedCharacterId()).append('|').append(row.mainType()).append('|').append(row.mainValue()).append('|')
                .append(row.subType()).append('|').append(row.subValue()).append('|').append(row.mainAt20()).append('|').append(row.subAt20()).append('\\n');
''',
'''                .append(row.equippedCharacterId()).append('|').append(row.mainType()).append('|').append(row.mainValue()).append('|')
                .append(row.subType()).append('|').append(row.subValue()).append('|').append(row.mainAt20()).append('|').append(row.subAt20()).append('|')
                .append(row.salePrice()).append('|').append(row.sellable()?1:0).append('\\n');
''')

service = path("src/main/java/io/github/q93503128/turnbound/world/MetaMenuService.java")
replace_exact(service,
'''            case "BUY" -> {
                if (parts.length < 2) return;
                mutate(player, "구매 실패", () -> CampaignProgressStore.buyEquipment(player.getUUID(), parts[1]));
            }
            case "ENHANCE" -> {
''',
'''            case "BUY" -> {
                if (parts.length < 2) return;
                mutate(player, "구매 실패", () -> CampaignProgressStore.buyEquipment(player.getUUID(), parts[1]));
            }
            case "SELL" -> {
                if (parts.length < 2) return;
                mutate(player, "판매 실패", () -> CampaignProgressStore.sellEquipment(player.getUUID(), parts[1]));
            }
            case "ENHANCE" -> {
''')
replace_exact(service,
'''                return new MetaUiSnapshot.EquipmentRow(item.instanceId(), item.itemId(), spec.name(), spec.tier(), spec.slot(),
                        item.enhancementLevel(), owners.getOrDefault(item.instanceId(), ""), spec.main().type(),
                        EquipmentInventory.scaledMain(spec.main().value(), item.enhancementLevel()), spec.sub().type(),
                        EquipmentInventory.scaledSub(spec.sub().value(), item.enhancementLevel()),
                        EquipmentInventory.scaledMain(spec.main().value(), 20), EquipmentInventory.scaledSub(spec.sub().value(), 20));
''',
'''                String owner = owners.getOrDefault(item.instanceId(), "");
                return new MetaUiSnapshot.EquipmentRow(item.instanceId(), item.itemId(), spec.name(), spec.tier(), spec.slot(),
                        item.enhancementLevel(), owner, spec.main().type(),
                        EquipmentInventory.scaledMain(spec.main().value(), item.enhancementLevel()), spec.sub().type(),
                        EquipmentInventory.scaledSub(spec.sub().value(), item.enhancementLevel()),
                        EquipmentInventory.scaledMain(spec.main().value(), 20), EquipmentInventory.scaledSub(spec.sub().value(), 20),
                        EquipmentRules.salePrice(spec.tier()), owner.isBlank());
''')
replace_exact(service,
'''                return new MetaUiSnapshot.EquipmentRow(item.instanceId(), item.itemId(), spec.name(), "SIGNATURE", "SIGNATURE",
                        item.enhancementLevel(), owners.getOrDefault(item.instanceId(), ""), spec.main().type(),
                        EquipmentInventory.scaledMain(spec.main().value(), item.enhancementLevel()), spec.sub().type(),
                        EquipmentInventory.scaledSub(spec.sub().value(), item.enhancementLevel()),
                        EquipmentInventory.scaledMain(spec.main().value(), 20), EquipmentInventory.scaledSub(spec.sub().value(), 20));
''',
'''                return new MetaUiSnapshot.EquipmentRow(item.instanceId(), item.itemId(), spec.name(), "SIGNATURE", "SIGNATURE",
                        item.enhancementLevel(), owners.getOrDefault(item.instanceId(), ""), spec.main().type(),
                        EquipmentInventory.scaledMain(spec.main().value(), item.enhancementLevel()), spec.sub().type(),
                        EquipmentInventory.scaledSub(spec.sub().value(), item.enhancementLevel()),
                        EquipmentInventory.scaledMain(spec.main().value(), 20), EquipmentInventory.scaledSub(spec.sub().value(), 20),
                        0, false);
''')

client_state = path("src/main/java/io/github/q93503128/turnbound/client/ClientMetaState.java")
replace_exact(client_state,
'''    public record EquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int enhancement,String equippedCharacterId,
                               String mainType,double mainValue,String subType,double subValue,double mainAt20,double subAt20) {}
''',
'''    public record EquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int enhancement,String equippedCharacterId,
                               String mainType,double mainValue,String subType,double subValue,double mainAt20,double subAt20,
                               int salePrice,boolean sellable) {}
''')
replace_exact(client_state,
'''                    case "I"->equipment.add(new EquipmentRow(p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),p[7],p[8],Double.parseDouble(p[9]),p[10],Double.parseDouble(p[11]),Double.parseDouble(p[12]),Double.parseDouble(p[13])));
''',
'''                    case "I"->equipment.add(new EquipmentRow(p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),p[7],p[8],Double.parseDouble(p[9]),p[10],Double.parseDouble(p[11]),Double.parseDouble(p[12]),Double.parseDouble(p[13]),p.length>14?Integer.parseInt(p[14]):0,p.length>15&&"1".equals(p[15])));
''')

screen = path("src/main/java/io/github/q93503128/turnbound/client/MetaMenuScreen.java")
replace_exact(screen,
'''            var sell = new BattleHudButton(rx, actionY + 29, Math.min(230, rw), 22,
                    Component.literal("판매 · 가격 정본 미정"), MUTED, ignored -> { });
            sell.active = false;
            addRenderableWidget(sell);
''',
'''            String saleLabel = selected.tier().equals("SIGNATURE") ? "전용 장비 · 판매 불가"
                    : selected.equippedCharacterId().isBlank() ? "판매 · " + selected.salePrice() + "G"
                    : "장착 해제 후 판매 · " + selected.salePrice() + "G";
            var sell = new BattleHudButton(rx, actionY + 29, Math.min(230, rw), 22,
                    Component.literal(saleLabel), selected.sellable() ? GOLD : MUTED, ignored -> sellSelected());
            sell.active = selected.sellable();
            addRenderableWidget(sell);
''')
replace_exact(screen,
'''    private void equipSelected() { if(!selectedEquipmentId.isBlank()&&!equipmentTargetCharacterId.isBlank()) send("EQUIP|"+equipmentTargetCharacterId+"|"+selectedEquipmentId); }
    private void selectCodex(String category) { codexCategory=category;page=0;clearWidgets();init(); }
''',
'''    private void equipSelected() { if(!selectedEquipmentId.isBlank()&&!equipmentTargetCharacterId.isBlank()) send("EQUIP|"+equipmentTargetCharacterId+"|"+selectedEquipmentId); }
    private void sellSelected() { var selected=equipment(selectedEquipmentId); if(selected!=null&&selected.sellable()) send("SELL|"+selected.instanceId()); }
    private void selectCodex(String category) { codexCategory=category;page=0;clearWidgets();init(); }
''')
replace_exact(screen,
'''        graphics.text(font,Component.literal("판매 가격은 v0.4 정본에 수치가 없어 비활성."),x,y+105,MUTED,false);
''',
'''        String saleInfo=selected.tier().equals("SIGNATURE")?"판매 · 전용 장비는 정본 판매가 없음"
                : selected.equippedCharacterId().isBlank()?"판매 · "+selected.salePrice()+" Gold":"판매 · "+selected.salePrice()+" Gold · 장착 해제 필요";
        graphics.text(font,Component.literal(saleInfo),x,y+105,selected.sellable()?GOLD:MUTED,false);
''')

# Add integration coverage for server store and snapshot protocol.
store_test = path("src/test/java/io/github/q93503128/turnbound/world/CampaignEquipmentSaleIntegrationTest.java")
if store_test.exists():
    raise SystemExit(f"Unexpected existing test: {store_test.relative_to(ROOT)}")
store_test.write_text('''package io.github.q93503128.turnbound.world;\n\nimport io.github.q93503128.turnbound.progression.PlayerProfile;\nimport org.junit.jupiter.api.Test;\n\nimport java.util.UUID;\n\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass CampaignEquipmentSaleIntegrationTest {\n    @Test\n    void campaignSaleCreditsGoldRemovesInstanceAndMarksSaveDirty() {\n        UUID playerId = UUID.randomUUID();\n        try {\n            CampaignProgressStore.ensureNewGame(playerId);\n            long beforeGold = CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD);\n            var item = CampaignProgressStore.grantEquipment(playerId, "W05");\n            CampaignProgressStore.markClean(playerId);\n\n            assertEquals(15_000, CampaignProgressStore.sellEquipment(playerId, item.instanceId()));\n            assertEquals(beforeGold + 15_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD));\n            assertFalse(CampaignProgressStore.equipment(playerId).items().containsKey(item.instanceId()));\n            assertTrue(CampaignProgressStore.isDirty(playerId));\n        } finally {\n            CampaignProgressStore.resetForTests(playerId);\n        }\n    }\n}\n''', encoding="utf-8")

codec_test = path("src/test/java/io/github/q93503128/turnbound/client/MetaEquipmentSaleCodecTest.java")
if codec_test.exists():
    raise SystemExit(f"Unexpected existing test: {codec_test.relative_to(ROOT)}")
codec_test.write_text('''package io.github.q93503128.turnbound.client;\n\nimport io.github.q93503128.turnbound.world.MetaUiCodec;\nimport io.github.q93503128.turnbound.world.MetaUiSnapshot;\nimport org.junit.jupiter.api.Test;\n\nimport java.util.List;\n\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass MetaEquipmentSaleCodecTest {\n    @Test\n    void equipmentSaleAuthorityReachesClientWithoutClientPriceDuplication() {\n        var normal = new MetaUiSnapshot.EquipmentRow("eq_1", "W05", "결투자의 문장", "T3", "WEAPON", 7, "",\n                "ATK_PCT", 0.10, "SPD_FLAT", 3.0, 0.162, 6.0, 15_000, true);\n        var equipped = new MetaUiSnapshot.EquipmentRow("eq_2", "A01", "훈련 방어각인", "T1", "ARMOR", 0, "P01",\n                "HP_PCT", 0.06, "DEF_PCT", 0.03, 0.108, 0.06, 2_000, false);\n        var signature = new MetaUiSnapshot.EquipmentRow("eq_3", "sig_p01_unending_vow", "끝나지 않는 서약", "SIGNATURE", "SIGNATURE", 0, "",\n                "ATK_PCT", 0.12, "SPD_FLAT", 4.0, 0.216, 8.0, 0, false);\n        var snapshot = new MetaUiSnapshot(0,0,0,0,0,false,0,false, List.of(),List.of(),List.of(),\n                List.of(normal,equipped,signature),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());\n\n        ClientMetaState.update(MetaUiCodec.encode(snapshot));\n        var rows = ClientMetaState.snapshot().equipment();\n        assertEquals(3, rows.size());\n        assertEquals(15_000, rows.get(0).salePrice());\n        assertTrue(rows.get(0).sellable());\n        assertEquals(2_000, rows.get(1).salePrice());\n        assertFalse(rows.get(1).sellable());\n        assertEquals(0, rows.get(2).salePrice());\n        assertFalse(rows.get(2).sellable());\n    }\n}\n''', encoding="utf-8")

# Guard against stale UI text and missing server command after migration.
for file, token in [
    (screen, "가격 정본 미정"),
    (screen, "판매 가격은 v0.4 정본에 수치가 없어 비활성"),
]:
    if token in file.read_text(encoding="utf-8"):
        raise SystemExit(f"Stale sale placeholder remains in {file.relative_to(ROOT)}: {token}")
if 'case "SELL"' not in service.read_text(encoding="utf-8"):
    raise SystemExit("SELL command was not wired")
if 'public static int sellEquipment' not in store.read_text(encoding="utf-8"):
    raise SystemExit("Campaign sale endpoint was not wired")

print("Equipment sale UI/network migration complete.")
