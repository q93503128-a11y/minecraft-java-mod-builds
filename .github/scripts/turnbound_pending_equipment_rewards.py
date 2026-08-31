from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROJECT = ROOT / "projects" / "turnbound"


def p(rel: str) -> Path:
    return PROJECT / rel


def replace_exact(path: Path, old: str, new: str, expected: int = 1) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{path.relative_to(ROOT)}: expected {expected} exact match(es), found {count}")
    path.write_text(text.replace(old, new), encoding="utf-8")


# -----------------------------------------------------------------------------
# EquipmentInventory: rewards that arrive at 300/300 live in a persisted pending
# reward queue outside the 300 inventory instances. Incoming rewards keep stable
# instance IDs so they can be claimed or sold without rerolling/recreating them.
# -----------------------------------------------------------------------------
inv = p("src/main/java/io/github/q93503128/turnbound/progression/EquipmentInventory.java")
replace_exact(inv, "import java.util.LinkedHashMap;\n", "import java.util.ArrayList;\nimport java.util.LinkedHashMap;\n")
replace_exact(inv,
'''    public record Snapshot(
            long nextSerial,
            Map<String, Item> items,
            Map<String, Loadout> loadouts,
            Map<String, Integer> choiceTokens) {
        public Snapshot {
            if (nextSerial < 1) throw new IllegalArgumentException("nextSerial must be positive");
            items = Map.copyOf(items);
            loadouts = Map.copyOf(loadouts);
            choiceTokens = Map.copyOf(choiceTokens);
            if (items.size() > MAX_INSTANCES) throw new IllegalArgumentException("Equipment inventory exceeds " + MAX_INSTANCES + " instances");
            choiceTokens.forEach((tier, count) -> {
                if (count < 0) throw new IllegalArgumentException("Negative equipment choice token");
            });
        }
        public static Snapshot empty() { return new Snapshot(1, Map.of(), Map.of(), Map.of()); }
    }
''',
'''    public record Snapshot(
            long nextSerial,
            Map<String, Item> items,
            Map<String, Loadout> loadouts,
            Map<String, Integer> choiceTokens,
            List<Item> pendingRewards) {
        public Snapshot(long nextSerial, Map<String, Item> items, Map<String, Loadout> loadouts, Map<String, Integer> choiceTokens) {
            this(nextSerial, items, loadouts, choiceTokens, List.of());
        }

        public Snapshot {
            if (nextSerial < 1) throw new IllegalArgumentException("nextSerial must be positive");
            items = Map.copyOf(items);
            loadouts = Map.copyOf(loadouts);
            choiceTokens = Map.copyOf(choiceTokens);
            pendingRewards = List.copyOf(pendingRewards == null ? List.of() : pendingRewards);
            if (items.size() > MAX_INSTANCES) throw new IllegalArgumentException("Equipment inventory exceeds " + MAX_INSTANCES + " instances");
            Set<String> identities = new LinkedHashSet<>(items.keySet());
            for (Item pending : pendingRewards) {
                if (!identities.add(pending.instanceId())) throw new IllegalArgumentException("Duplicate equipment identity " + pending.instanceId());
                if (pending.enhancementLevel() != 0) throw new IllegalArgumentException("Pending equipment reward must be unenhanced");
            }
            choiceTokens.forEach((tier, count) -> {
                if (count < 0) throw new IllegalArgumentException("Negative equipment choice token");
            });
        }
        public static Snapshot empty() { return new Snapshot(1, Map.of(), Map.of(), Map.of(), List.of()); }
    }
''')
replace_exact(inv,
'''    private long nextSerial = 1;
    private final Map<String, Item> items = new LinkedHashMap<>();
    private final Map<String, Loadout> loadouts = new LinkedHashMap<>();
    private final Map<String, Integer> choiceTokens = new LinkedHashMap<>();
''',
'''    private long nextSerial = 1;
    private final Map<String, Item> items = new LinkedHashMap<>();
    private final Map<String, Loadout> loadouts = new LinkedHashMap<>();
    private final Map<String, Integer> choiceTokens = new LinkedHashMap<>();
    private final List<Item> pendingRewards = new ArrayList<>();
''')
replace_exact(inv,
'''        inventory.items.putAll(snapshot.items());
        inventory.loadouts.putAll(snapshot.loadouts());
        inventory.choiceTokens.putAll(snapshot.choiceTokens());
        inventory.validateReferences();
''',
'''        inventory.items.putAll(snapshot.items());
        inventory.loadouts.putAll(snapshot.loadouts());
        inventory.choiceTokens.putAll(snapshot.choiceTokens());
        inventory.pendingRewards.addAll(snapshot.pendingRewards());
        inventory.validateReferences();
''')
replace_exact(inv,
'''    public Snapshot snapshot() {
        return new Snapshot(nextSerial, items, loadouts, choiceTokens);
    }
''',
'''    public Snapshot snapshot() {
        return new Snapshot(nextSerial, items, loadouts, choiceTokens, pendingRewards);
    }
''')
replace_exact(inv,
'''    public Set<String> unknownItemIds() {
        Set<String> out = new LinkedHashSet<>();
        for (Item item : items.values()) if (!isKnownItemId(item.itemId())) out.add(item.itemId());
        return Set.copyOf(out);
    }

    public Item grant(String itemId) {
        requireKnown(itemId);
        requireFreeSlot();
        String instanceId = "eq_" + String.format("%08d", nextSerial++);
        Item item = new Item(instanceId, itemId, 0);
        items.put(instanceId, item);
        return item;
    }
''',
'''    public Set<String> unknownItemIds() {
        Set<String> out = new LinkedHashSet<>();
        for (Item item : items.values()) if (!isKnownItemId(item.itemId())) out.add(item.itemId());
        for (Item item : pendingRewards) if (!isKnownItemId(item.itemId())) out.add(item.itemId());
        return Set.copyOf(out);
    }

    public Item grant(String itemId) {
        requireKnown(itemId);
        requireFreeSlot();
        Item item = newItem(itemId);
        items.put(item.instanceId(), item);
        return item;
    }

    /**
     * Canon §122: a newly earned equipment reward is never lost at 300/300.
     * It is held outside inventory until the player claims it or, for normal gear,
     * sells that incoming reward at the canonical tier sale value.
     */
    public Item grantReward(String itemId) {
        requireKnown(itemId);
        Item item = newItem(itemId);
        if (hasFreeSlot() && pendingRewards.isEmpty()) items.put(item.instanceId(), item);
        else pendingRewards.add(item);
        return item;
    }

    public List<Item> pendingRewards() { return List.copyOf(pendingRewards); }

    public Item claimPending(String instanceId) {
        requireFreeSlot();
        int index = pendingIndex(instanceId);
        Item item = pendingRewards.remove(index);
        items.put(item.instanceId(), item);
        return item;
    }

    public int sellPending(String instanceId, PlayerProfile profile) {
        int index = pendingIndex(instanceId);
        Item item = pendingRewards.get(index);
        ItemSpec spec = spec(item.itemId());
        if (spec.signature) throw new IllegalStateException("Signature equipment has no canonical sale price");
        int price = EquipmentRules.salePrice(spec.tier);
        profile.grant(PlayerProfile.Currency.GOLD, price);
        pendingRewards.remove(index);
        return price;
    }
''')
replace_exact(inv,
'''    public Item claimChoice(String tier, String itemId) {
        int count = choiceTokens(tier);
        if (count <= 0) throw new IllegalStateException("No " + tier + " equipment choice token");
        ItemSpec spec = spec(itemId);
        if (spec.signature || !spec.tier.equals(tier)) throw new IllegalArgumentException("Choice token tier mismatch");
        requireFreeSlot();
        if (count == 1) choiceTokens.remove(tier); else choiceTokens.put(tier, count - 1);
        return grant(itemId);
    }
''',
'''    public Item claimChoice(String tier, String itemId) {
        int count = choiceTokens(tier);
        if (count <= 0) throw new IllegalStateException("No " + tier + " equipment choice token");
        ItemSpec spec = spec(itemId);
        if (spec.signature || !spec.tier.equals(tier)) throw new IllegalArgumentException("Choice token tier mismatch");
        if (count == 1) choiceTokens.remove(tier); else choiceTokens.put(tier, count - 1);
        return grantReward(itemId);
    }
''')
replace_exact(inv,
'''    private void validateReferences() {
        Set<String> equipped = new LinkedHashSet<>();
        for (var entry : loadouts.entrySet()) {
''',
'''    private void validateReferences() {
        Set<String> identities = new LinkedHashSet<>(items.keySet());
        for (Item pending : pendingRewards) {
            if (!identities.add(pending.instanceId())) throw new IllegalStateException("Duplicate equipment identity " + pending.instanceId());
        }
        Set<String> equipped = new LinkedHashSet<>();
        for (var entry : loadouts.entrySet()) {
''')
replace_exact(inv,
'''    private void requireFreeSlot() {
        if (!hasFreeSlot()) throw new IllegalStateException("Equipment inventory is full (" + MAX_INSTANCES + "/" + MAX_INSTANCES + ")");
    }

    private static void merge(Map<String, Double> values, String type, double amount) {
''',
'''    private Item newItem(String itemId) {
        return new Item("eq_" + String.format("%08d", nextSerial++), itemId, 0);
    }

    private int pendingIndex(String instanceId) {
        for (int i = 0; i < pendingRewards.size(); i++) if (pendingRewards.get(i).instanceId().equals(instanceId)) return i;
        throw new IllegalArgumentException("Unknown pending equipment reward " + instanceId);
    }

    private void requireFreeSlot() {
        if (!hasFreeSlot()) throw new IllegalStateException("Equipment inventory is full (" + MAX_INSTANCES + "/" + MAX_INSTANCES + ")");
    }

    private static void merge(Map<String, Double> values, String type, double amount) {
''')

# -----------------------------------------------------------------------------
# Persist pending rewards inside the existing v4 equipment object. The field is
# optional on decode, so existing schema-v4 saves remain backward compatible.
# -----------------------------------------------------------------------------
save = p("src/main/java/io/github/q93503128/turnbound/world/CampaignSaveCodec.java")
replace_exact(save,
'''        out.add("items", items);
        JsonObject loadouts = new JsonObject();
''',
'''        out.add("items", items);
        JsonArray pendingRewards = new JsonArray();
        for (EquipmentInventory.Item item : snapshot.pendingRewards()) {
            JsonObject row = new JsonObject();
            row.addProperty("instanceId", item.instanceId());
            row.addProperty("itemId", item.itemId());
            row.addProperty("enhancementLevel", item.enhancementLevel());
            pendingRewards.add(row);
        }
        out.add("pendingRewards", pendingRewards);
        JsonObject loadouts = new JsonObject();
''')
replace_exact(save,
'''        Map<String, EquipmentInventory.Loadout> loadouts = new LinkedHashMap<>();
        for (var entry : optionalObject(raw, "loadouts").entrySet()) {
''',
'''        List<EquipmentInventory.Item> pendingRewards = new ArrayList<>();
        for (JsonElement element : optionalArray(raw, "pendingRewards")) {
            JsonObject row = element.getAsJsonObject();
            String itemId = requiredString(row, "itemId");
            if (!knownEquipment(itemId)) { orphaned.add(itemId); continue; }
            pendingRewards.add(new EquipmentInventory.Item(requiredString(row, "instanceId"), itemId,
                    optionalInt(row, "enhancementLevel", 0)));
        }
        Map<String, EquipmentInventory.Loadout> loadouts = new LinkedHashMap<>();
        for (var entry : optionalObject(raw, "loadouts").entrySet()) {
''')
replace_exact(save,
'''        return new EquipmentInventory.Snapshot(nextSerial, items, loadouts, choices);
''',
'''        return new EquipmentInventory.Snapshot(nextSerial, items, loadouts, choices, pendingRewards);
''')

# -----------------------------------------------------------------------------
# Campaign authority: actual rewards queue instead of failing at 300/300; buying
# stays strict because a shop purchase is voluntary and already guards free space.
# -----------------------------------------------------------------------------
store = p("src/main/java/io/github/q93503128/turnbound/world/CampaignProgressStore.java")
replace_exact(store, 'EquipmentInventory.Item reward = progress.equipment.grant(signatureId);',
                   'EquipmentInventory.Item reward = progress.equipment.grantReward(signatureId);')
replace_exact(store,
'''    public static EquipmentInventory.Item grantEquipment(UUID playerId, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = progress.equipment.grant(itemId);
        progress.dirty = true;
        return item;
    }
''',
'''    public static EquipmentInventory.Item grantEquipment(UUID playerId, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = progress.equipment.grantReward(itemId);
        progress.dirty = true;
        return item;
    }
''')
replace_exact(store,
'''    public static int sellEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        int gold = progress.equipment.sell(instanceId, progress.profile);
        progress.dirty = true;
        return gold;
    }
''',
'''    public static int sellEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        int gold = progress.equipment.sell(instanceId, progress.profile);
        progress.dirty = true;
        return gold;
    }

    public static EquipmentInventory.Item claimPendingEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = progress.equipment.claimPending(instanceId);
        progress.dirty = true;
        return item;
    }

    public static int sellPendingEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        int gold = progress.equipment.sellPending(instanceId, progress.profile);
        progress.dirty = true;
        return gold;
    }
''')

# -----------------------------------------------------------------------------
# Server-authored meta snapshot/codec: client receives pending reward state and
# canonical sale value. No client-side tier-price duplication.
# -----------------------------------------------------------------------------
snap = p("src/main/java/io/github/q93503128/turnbound/world/MetaUiSnapshot.java")
replace_exact(snap,
'''        List<CharacterRow> characters, List<EquipmentRow> equipment,
        List<EndgameRow> endgame, List<ChallengeRow> challenges, List<RegionQuestRow> regionQuests,
        List<ArchiveRow> archiveHistory, List<ShopRow> shopItems, List<CodexRow> codex) {
    public MetaUiSnapshot {
''',
'''        List<CharacterRow> characters, List<EquipmentRow> equipment,
        List<EndgameRow> endgame, List<ChallengeRow> challenges, List<RegionQuestRow> regionQuests,
        List<ArchiveRow> archiveHistory, List<ShopRow> shopItems, List<CodexRow> codex,
        List<PendingEquipmentRow> pendingEquipment) {
    public MetaUiSnapshot(
            long gold, long crystal, long starEssence, long awakeningCore, int partyCp, boolean riftUnlocked,
            int fiveStarPity, boolean starterArchiveAvailable,
            List<String> activeParty, List<List<String>> partyPresets,
            List<CharacterRow> characters, List<EquipmentRow> equipment,
            List<EndgameRow> endgame, List<ChallengeRow> challenges, List<RegionQuestRow> regionQuests,
            List<ArchiveRow> archiveHistory, List<ShopRow> shopItems, List<CodexRow> codex) {
        this(gold, crystal, starEssence, awakeningCore, partyCp, riftUnlocked, fiveStarPity, starterArchiveAvailable,
                activeParty, partyPresets, characters, equipment, endgame, challenges, regionQuests,
                archiveHistory, shopItems, codex, List.of());
    }

    public MetaUiSnapshot {
''')
replace_exact(snap,
'''        shopItems = List.copyOf(shopItems);
        codex = List.copyOf(codex);
''',
'''        shopItems = List.copyOf(shopItems);
        codex = List.copyOf(codex);
        pendingEquipment = List.copyOf(pendingEquipment == null ? List.of() : pendingEquipment);
''')
replace_exact(snap,
'''    public record EquipmentRow(
            String instanceId, String itemId, String name, String tier, String slot, int enhancement,
            String equippedCharacterId, String mainType, double mainValue, String subType, double subValue,
            double mainAt20, double subAt20, int salePrice, boolean sellable) {}

    public record EndgameRow''',
'''    public record EquipmentRow(
            String instanceId, String itemId, String name, String tier, String slot, int enhancement,
            String equippedCharacterId, String mainType, double mainValue, String subType, double subValue,
            double mainAt20, double subAt20, int salePrice, boolean sellable) {}

    public record PendingEquipmentRow(
            String instanceId, String itemId, String name, String tier, String slot,
            int salePrice, boolean claimable, boolean immediateSellable) {}

    public record EndgameRow''')

codec = p("src/main/java/io/github/q93503128/turnbound/world/MetaUiCodec.java")
replace_exact(codec,
'''                .append(row.salePrice()).append('|').append(row.sellable()?1:0).append('\\n');
        for (var row : snapshot.endgame())''',
'''                .append(row.salePrice()).append('|').append(row.sellable()?1:0).append('\\n');
        for (var row : snapshot.pendingEquipment()) out.append("IR|").append(row.instanceId()).append('|').append(row.itemId()).append('|')
                .append(safe(row.name())).append('|').append(row.tier()).append('|').append(row.slot()).append('|').append(row.salePrice()).append('|')
                .append(row.claimable()?1:0).append('|').append(row.immediateSellable()?1:0).append('\\n');
        for (var row : snapshot.endgame())''')

service = p("src/main/java/io/github/q93503128/turnbound/world/MetaMenuService.java")
replace_exact(service,
'''        List<MetaUiSnapshot.EquipmentRow> equipment = equipmentRows(campaign.equipment());

        List<MetaUiSnapshot.EndgameRow> endgame = new ArrayList<>();
''',
'''        List<MetaUiSnapshot.EquipmentRow> equipment = equipmentRows(campaign.equipment());
        List<MetaUiSnapshot.PendingEquipmentRow> pendingEquipment = pendingEquipmentRows(campaign.equipment());

        List<MetaUiSnapshot.EndgameRow> endgame = new ArrayList<>();
''')
replace_exact(service,
'''                campaign.profile().fiveStarPity(), CampaignProgressStore.starterArchiveAvailable(id),
                party, campaign.profile().partyPresets(), characters, equipment, endgame, challenges, regionQuests, archive, shop, codex);
''',
'''                campaign.profile().fiveStarPity(), CampaignProgressStore.starterArchiveAvailable(id),
                party, campaign.profile().partyPresets(), characters, equipment, endgame, challenges, regionQuests, archive, shop, codex,
                pendingEquipment);
''')
replace_exact(service,
'''            case "SELL" -> {
                if (parts.length < 2) return;
                mutate(player, "판매 실패", () -> CampaignProgressStore.sellEquipment(player.getUUID(), parts[1]));
            }
            case "ENHANCE" -> {
''',
'''            case "SELL" -> {
                if (parts.length < 2) return;
                mutate(player, "판매 실패", () -> CampaignProgressStore.sellEquipment(player.getUUID(), parts[1]));
            }
            case "REWARD_CLAIM" -> {
                if (parts.length < 2) return;
                mutate(player, "장비 보상 수령 실패", () -> CampaignProgressStore.claimPendingEquipment(player.getUUID(), parts[1]));
            }
            case "REWARD_SELL" -> {
                if (parts.length < 2) return;
                mutate(player, "장비 보상 판매 실패", () -> CampaignProgressStore.sellPendingEquipment(player.getUUID(), parts[1]));
            }
            case "ENHANCE" -> {
''')
replace_exact(service,
'''    private static List<MetaUiSnapshot.CodexRow> codexRows(CampaignProgressStore.Snapshot campaign,
''',
'''    private static List<MetaUiSnapshot.PendingEquipmentRow> pendingEquipmentRows(EquipmentInventory.Snapshot inventory) {
        boolean claimable = inventory.items().size() < EquipmentInventory.MAX_INSTANCES;
        return inventory.pendingRewards().stream().map(item -> {
            try {
                var spec = V04Catalogs.equipment(item.itemId());
                return new MetaUiSnapshot.PendingEquipmentRow(item.instanceId(), item.itemId(), spec.name(), spec.tier(), spec.slot(),
                        EquipmentRules.salePrice(spec.tier()), claimable, true);
            } catch (RuntimeException ignored) {
                var spec = V04Catalogs.signature(item.itemId());
                return new MetaUiSnapshot.PendingEquipmentRow(item.instanceId(), item.itemId(), spec.name(), "SIGNATURE", "SIGNATURE",
                        0, claimable, false);
            }
        }).toList();
    }

    private static List<MetaUiSnapshot.CodexRow> codexRows(CampaignProgressStore.Snapshot campaign,
''')
replace_exact(service,
'''        Set<String> ownedEquipment = campaign.equipment().items().values().stream().map(EquipmentInventory.Item::itemId)
                .collect(java.util.stream.Collectors.toSet());
''',
'''        Set<String> ownedEquipment = new java.util.LinkedHashSet<>();
        campaign.equipment().items().values().stream().map(EquipmentInventory.Item::itemId).forEach(ownedEquipment::add);
        campaign.equipment().pendingRewards().stream().map(EquipmentInventory.Item::itemId).forEach(ownedEquipment::add);
''')

# -----------------------------------------------------------------------------
# Client state + Equipment screen: first pending reward is resolved in FIFO order.
# The normal inventory list remains usable at 300/300 so the player can sell an
# existing item, sync, then claim the reward. Signature incoming sale stays blocked.
# -----------------------------------------------------------------------------
client = p("src/main/java/io/github/q93503128/turnbound/client/ClientMetaState.java")
replace_exact(client,
'''    public record EquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int enhancement,String equippedCharacterId,
                               String mainType,double mainValue,String subType,double subValue,double mainAt20,double subAt20,
                               int salePrice,boolean sellable) {}
    public record EndgameRow''',
'''    public record EquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int enhancement,String equippedCharacterId,
                               String mainType,double mainValue,String subType,double subValue,double mainAt20,double subAt20,
                               int salePrice,boolean sellable) {}
    public record PendingEquipmentRow(String instanceId,String itemId,String name,String tier,String slot,int salePrice,boolean claimable,boolean immediateSellable) {}
    public record EndgameRow''')
replace_exact(client,
'''                           List<ChallengeRow> challenges,List<RegionQuestRow> regionQuests,
                           List<ArchiveRow> archiveHistory,List<ShopRow> shopItems,List<CodexRow> codex) {
        public Snapshot {
''',
'''                           List<ChallengeRow> challenges,List<RegionQuestRow> regionQuests,
                           List<ArchiveRow> archiveHistory,List<ShopRow> shopItems,List<CodexRow> codex,List<PendingEquipmentRow> pendingEquipment) {
        public Snapshot {
''')
replace_exact(client,
'''            archiveHistory=List.copyOf(archiveHistory); shopItems=List.copyOf(shopItems); codex=List.copyOf(codex);
        }
        public static Snapshot empty(){return new Snapshot(0,0,0,0,0,false,0,false,List.of(),List.of(List.of(),List.of(),List.of()),
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
''',
'''            archiveHistory=List.copyOf(archiveHistory); shopItems=List.copyOf(shopItems); codex=List.copyOf(codex);
            pendingEquipment=List.copyOf(pendingEquipment);
        }
        public static Snapshot empty(){return new Snapshot(0,0,0,0,0,false,0,false,List.of(),List.of(List.of(),List.of(),List.of()),
                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of());}
''')
replace_exact(client,
'''        List<ChallengeRow> challenges=new ArrayList<>(); List<RegionQuestRow> regions=new ArrayList<>();
        List<ArchiveRow> archive=new ArrayList<>(); List<ShopRow> shop=new ArrayList<>(); List<CodexRow> codex=new ArrayList<>();
''',
'''        List<ChallengeRow> challenges=new ArrayList<>(); List<RegionQuestRow> regions=new ArrayList<>();
        List<ArchiveRow> archive=new ArrayList<>(); List<ShopRow> shop=new ArrayList<>(); List<CodexRow> codex=new ArrayList<>();
        List<PendingEquipmentRow> pendingEquipment=new ArrayList<>();
''')
replace_exact(client,
'''                    case "I"->equipment.add(new EquipmentRow(p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),p[7],p[8],Double.parseDouble(p[9]),p[10],Double.parseDouble(p[11]),Double.parseDouble(p[12]),Double.parseDouble(p[13]),p.length>14?Integer.parseInt(p[14]):0,p.length>15&&"1".equals(p[15])));
                    case "E"->endgame.add''',
'''                    case "I"->equipment.add(new EquipmentRow(p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),p[7],p[8],Double.parseDouble(p[9]),p[10],Double.parseDouble(p[11]),Double.parseDouble(p[12]),Double.parseDouble(p[13]),p.length>14?Integer.parseInt(p[14]):0,p.length>15&&"1".equals(p[15])));
                    case "IR"->pendingEquipment.add(new PendingEquipmentRow(p[1],p[2],p[3],p[4],p[5],Integer.parseInt(p[6]),"1".equals(p[7]),"1".equals(p[8])));
                    case "E"->endgame.add''')
replace_exact(client,
'''        snapshot=new Snapshot(gold,crystal,essence,core,cp,rift,pity,starter,party,presets,chars,equipment,endgame,challenges,regions,archive,shop,codex);
''',
'''        snapshot=new Snapshot(gold,crystal,essence,core,cp,rift,pity,starter,party,presets,chars,equipment,endgame,challenges,regions,archive,shop,codex,pendingEquipment);
''')

screen = p("src/main/java/io/github/q93503128/turnbound/client/MetaMenuScreen.java")
replace_exact(screen,
'''        buildPager(rows.size(), perPage);

        ClientMetaState.EquipmentRow selected = equipment(selectedEquipmentId);
''',
'''        buildPager(rows.size(), perPage);

        ClientMetaState.PendingEquipmentRow pending = ClientMetaState.snapshot().pendingEquipment().stream().findFirst().orElse(null);
        if (pending != null) {
            int rewardX = left + listW + 36;
            int rewardW = Math.max(210, left + panelWidth - 18 - rewardX);
            int rewardY = top + 138;
            String queue = ClientMetaState.snapshot().pendingEquipment().size() > 1
                    ? " · 대기 " + ClientMetaState.snapshot().pendingEquipment().size() + "개" : "";
            var label = new BattleHudButton(rewardX, rewardY, rewardW, 23,
                    Component.literal("미수령 장비 보상" + queue + " · " + pending.tier() + " " + pending.name()), GOLD, ignored -> { });
            label.active = false;
            addRenderableWidget(label);
            var claim = new BattleHudButton(rewardX, rewardY + 28, Math.min(230, rewardW), 22,
                    Component.literal(pending.claimable() ? "보상 수령" : "300/300 · 기존 장비 판매 필요"),
                    pending.claimable() ? GREEN : MUTED, ignored -> send("REWARD_CLAIM|" + pending.instanceId()));
            claim.active = pending.claimable();
            addRenderableWidget(claim);
            String sellText = pending.immediateSellable() ? "새 보상 즉시 판매 · " + pending.salePrice() + "G" : "전용 장비 · 즉시 판매 불가";
            var sellReward = new BattleHudButton(rewardX, rewardY + 55, Math.min(230, rewardW), 22,
                    Component.literal(sellText), pending.immediateSellable() ? GOLD : MUTED,
                    ignored -> send("REWARD_SELL|" + pending.instanceId()));
            sellReward.active = pending.immediateSellable();
            addRenderableWidget(sellReward);
        }

        ClientMetaState.EquipmentRow selected = equipment(selectedEquipmentId);
''')

# -----------------------------------------------------------------------------
# Tests: 300/300 preservation, incoming sale, Signature protection/claim,
# save round-trip, and server->client pending reward protocol.
# -----------------------------------------------------------------------------
overflow_test = p("src/test/java/io/github/q93503128/turnbound/progression/EquipmentPendingRewardTest.java")
if overflow_test.exists():
    raise SystemExit("EquipmentPendingRewardTest already exists")
overflow_test.write_text('''package io.github.q93503128.turnbound.progression;\n\nimport org.junit.jupiter.api.Test;\n\nimport java.util.Set;\n\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass EquipmentPendingRewardTest {\n    @Test\n    void fullInventoryPreservesIncomingNormalRewardAndAllowsImmediateSale() {\n        EquipmentInventory inventory = fullInventory();\n        PlayerProfile profile = profileWithGold(0);\n\n        EquipmentInventory.Item reward = inventory.grantReward("W05");\n        assertEquals(EquipmentInventory.MAX_INSTANCES, inventory.size());\n        assertEquals(List.of(reward), inventory.pendingRewards());\n        assertEquals(15_000, inventory.sellPending(reward.instanceId(), profile));\n        assertTrue(inventory.pendingRewards().isEmpty());\n        assertEquals(15_000, profile.currency(PlayerProfile.Currency.GOLD));\n        assertEquals(EquipmentInventory.MAX_INSTANCES, inventory.size());\n    }\n\n    @Test\n    void signatureRewardCannotBeSoldAndCanBeClaimedAfterExistingSaleFreesSpace() {\n        EquipmentInventory inventory = fullInventory();\n        PlayerProfile profile = profileWithGold(0);\n        EquipmentInventory.Item signature = inventory.grantReward("sig_p01_unending_vow");\n\n        assertThrows(IllegalStateException.class, () -> inventory.sellPending(signature.instanceId(), profile));\n        String existing = inventory.items().keySet().stream().sorted().findFirst().orElseThrow();\n        inventory.sell(existing, profile);\n        EquipmentInventory.Item claimed = inventory.claimPending(signature.instanceId());\n\n        assertEquals(signature, claimed);\n        assertEquals(EquipmentInventory.MAX_INSTANCES, inventory.size());\n        assertTrue(inventory.pendingRewards().isEmpty());\n        assertEquals(signature.itemId(), inventory.item(signature.instanceId()).itemId());\n    }\n\n    private static EquipmentInventory fullInventory() {\n        EquipmentInventory inventory = EquipmentInventory.empty();\n        for (int i = 0; i < EquipmentInventory.MAX_INSTANCES; i++) inventory.grant("W01");\n        return inventory;\n    }\n\n    private static PlayerProfile profileWithGold(long gold) {\n        return PlayerProfile.restore(new PlayerProfile.Snapshot(gold, 0, 0, 0, Set.of("P01"), 0, false, false));\n    }\n}\n''', encoding="utf-8")
# add missing List import intentionally via exact patch below to keep generated test simple
text = overflow_test.read_text(encoding="utf-8")
text = text.replace('import java.util.Set;\n', 'import java.util.List;\nimport java.util.Set;\n')
overflow_test.write_text(text, encoding="utf-8")

save_test = p("src/test/java/io/github/q93503128/turnbound/world/PendingEquipmentSaveTest.java")
if save_test.exists():
    raise SystemExit("PendingEquipmentSaveTest already exists")
save_test.write_text('''package io.github.q93503128.turnbound.world;\n\nimport io.github.q93503128.turnbound.progression.CharacterGrowthRules;\nimport io.github.q93503128.turnbound.progression.EquipmentInventory;\nimport io.github.q93503128.turnbound.progression.PlayerProfile;\nimport io.github.q93503128.turnbound.progression.QuestProgress;\nimport org.junit.jupiter.api.Test;\n\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Set;\n\nimport static org.junit.jupiter.api.Assertions.assertEquals;\n\nclass PendingEquipmentSaveTest {\n    @Test\n    void pendingEquipmentRewardSurvivesSchemaFourSaveRoundTrip() {\n        var pending = new EquipmentInventory.Item("eq_00000002", "W05", 0);\n        var equipment = new EquipmentInventory.Snapshot(3,\n                Map.of("eq_00000001", new EquipmentInventory.Item("eq_00000001", "W01", 0)),\n                Map.of(), Map.of(), List.of(pending));\n        var snapshot = new CampaignProgressStore.Snapshot(\n                new PlayerProfile.Snapshot(5_000,0,0,0,Set.of("P01"),0,false,false),\n                Map.of("P01", new CharacterProgression.State(1,0)),\n                Map.of("P01", CharacterGrowthRules.initial("P01")), equipment, QuestProgress.Snapshot.empty(),\n                Set.of(), Set.of(), Set.of());\n\n        var decoded = CampaignSaveCodec.decode(CampaignSaveCodec.encode(snapshot));\n        assertEquals(List.of(pending), decoded.equipment().pendingRewards());\n        assertEquals(1, decoded.equipment().items().size());\n    }\n}\n''', encoding="utf-8")

ui_test = p("src/test/java/io/github/q93503128/turnbound/client/PendingEquipmentUiCodecTest.java")
if ui_test.exists():
    raise SystemExit("PendingEquipmentUiCodecTest already exists")
ui_test.write_text('''package io.github.q93503128.turnbound.client;\n\nimport io.github.q93503128.turnbound.world.MetaUiCodec;\nimport io.github.q93503128.turnbound.world.MetaUiSnapshot;\nimport org.junit.jupiter.api.Test;\n\nimport java.util.List;\n\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass PendingEquipmentUiCodecTest {\n    @Test\n    void pendingRewardAuthorityReachesClient() {\n        var pending = new MetaUiSnapshot.PendingEquipmentRow("eq_301", "W05", "결투자의 문장", "T3", "WEAPON",\n                15_000, false, true);\n        var snapshot = new MetaUiSnapshot(0,0,0,0,0,false,0,false, List.of(),List.of(),List.of(),List.of(),\n                List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(pending));\n\n        ClientMetaState.update(MetaUiCodec.encode(snapshot));\n        var row = ClientMetaState.snapshot().pendingEquipment().getFirst();\n        assertEquals("eq_301", row.instanceId());\n        assertEquals(15_000, row.salePrice());\n        assertFalse(row.claimable());\n        assertTrue(row.immediateSellable());\n    }\n}\n''', encoding="utf-8")

campaign_test = p("src/test/java/io/github/q93503128/turnbound/world/CampaignEquipmentOverflowIntegrationTest.java")
if campaign_test.exists():
    raise SystemExit("CampaignEquipmentOverflowIntegrationTest already exists")
campaign_test.write_text('''package io.github.q93503128.turnbound.world;\n\nimport io.github.q93503128.turnbound.progression.EquipmentInventory;\nimport io.github.q93503128.turnbound.progression.PlayerProfile;\nimport org.junit.jupiter.api.Test;\n\nimport java.util.UUID;\n\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass CampaignEquipmentOverflowIntegrationTest {\n    @Test\n    void campaignRewardAtThreeHundredQueuesAndIncomingSaleIsPersistableMutation() {\n        UUID playerId = UUID.randomUUID();\n        try {\n            CampaignProgressStore.ensureNewGame(playerId);\n            for (int i = 0; i < EquipmentInventory.MAX_INSTANCES; i++) CampaignProgressStore.grantEquipment(playerId, "W01");\n            var reward = CampaignProgressStore.grantEquipment(playerId, "W05");\n            var before = CampaignProgressStore.snapshot(playerId);\n            assertEquals(EquipmentInventory.MAX_INSTANCES, before.equipment().items().size());\n            assertEquals(reward, before.equipment().pendingRewards().getFirst());\n\n            long gold = CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD);\n            CampaignProgressStore.markClean(playerId);\n            assertEquals(15_000, CampaignProgressStore.sellPendingEquipment(playerId, reward.instanceId()));\n            assertEquals(gold + 15_000, CampaignProgressStore.currency(playerId, PlayerProfile.Currency.GOLD));\n            assertTrue(CampaignProgressStore.snapshot(playerId).equipment().pendingRewards().isEmpty());\n            assertTrue(CampaignProgressStore.isDirty(playerId));\n        } finally {\n            CampaignProgressStore.resetForTests(playerId);\n        }\n    }\n}\n''', encoding="utf-8")

# Guards against regressions that would silently discard/reject earned rewards.
for file in (inv, store, save, snap, codec, service, client, screen):
    text = file.read_text(encoding="utf-8")
    if not text.strip():
        raise SystemExit(f"Unexpected empty file {file.relative_to(ROOT)}")
if 'grantReward(signatureId)' not in store.read_text(encoding="utf-8"):
    raise SystemExit("Signature Trial reward is not overflow-safe")
if 'pendingRewards' not in save.read_text(encoding="utf-8"):
    raise SystemExit("Pending rewards are not persisted")
if 'REWARD_CLAIM' not in service.read_text(encoding="utf-8") or 'REWARD_SELL' not in service.read_text(encoding="utf-8"):
    raise SystemExit("Pending reward commands are missing")
if '300/300 · 기존 장비 판매 필요' not in screen.read_text(encoding="utf-8"):
    raise SystemExit("Full-inventory resolution UI is missing")

print("TURNBOUND full-inventory pending reward migration complete.")
