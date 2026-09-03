package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.CharacterMenuCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** v0.4 world-backed RPG management menu. Server state remains authoritative. */
public final class MetaMenuScreen extends Screen {
    public enum Tab { PARTY, CHARACTERS, EQUIPMENT, ARCHIVE, QUESTS, CODEX, SYSTEM }
    private enum DetailTab { STATUS, SKILLS, EQUIPMENT, AWAKENING, PROFILE }
    private enum OwnershipFilter { ALL, OWNED, UNOWNED }
    private enum RoleFilter { ALL, DPS, SUPPORT, TANK, SUMMON }
    private enum EquipView { INVENTORY, MARKET }
    private enum EquipSort { TIER, LEVEL, STAT }

    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int BLUE = 0xFF6DC6FF;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFFFC857;
    private static final int DANGER = 0xFFFF6B6B;

    private Tab tab;
    private final List<String> draftParty = new ArrayList<>();
    private int page;
    private int left, top, panelWidth, panelHeight;

    private OwnershipFilter ownershipFilter = OwnershipFilter.ALL;
    private RoleFilter roleFilter = RoleFilter.ALL;
    private int starFilter;
    private int minimumLevel;
    private String selectedCharacterId = "";
    private DetailTab detailTab = DetailTab.STATUS;

    private EquipView equipView = EquipView.INVENTORY;
    private EquipSort equipSort = EquipSort.TIER;
    private String equipSlotFilter = "ALL";
    private String selectedEquipmentId = "";
    private String equipmentTargetCharacterId = "";

    private String codexCategory = "CHARACTERS";
    private String selectedEndgameId = "";

    public MetaMenuScreen(Tab tab) {
        super(Component.literal("TURNBOUND"));
        this.tab = tab == null ? Tab.PARTY : tab;
        draftParty.addAll(ClientMetaState.snapshot().activeParty());
    }

    public Tab tab() { return tab; }

    public void refreshSnapshot() {
        if (tab == Tab.PARTY) {
            draftParty.clear();
            draftParty.addAll(ClientMetaState.snapshot().activeParty());
        }
        if (!selectedCharacterId.isBlank() && character(selectedCharacterId) == null) selectedCharacterId = "";
        if (!selectedEquipmentId.isBlank() && equipment(selectedEquipmentId) == null) selectedEquipmentId = "";
        if (!selectedEndgameId.isBlank() && endgame(selectedEndgameId) == null) selectedEndgameId = "";
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        super.init();
        panelWidth = Math.min(980, Math.max(420, width - 42));
        panelHeight = Math.min(600, Math.max(300, height - 42));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        buildTabs();
        switch (tab) {
            case PARTY -> buildParty();
            case CHARACTERS -> buildCharacters();
            case EQUIPMENT -> buildEquipment();
            case ARCHIVE -> buildArchive();
            case SYSTEM -> buildEndgame();
            case CODEX -> buildCodex();
            case QUESTS -> { }
        }
    }

    private void buildTabs() {
        int x = left + 16, y = top + 57;
        int gap = 4;
        int w = Math.max(54, (panelWidth - 32 - gap * (Tab.values().length - 1)) / Tab.values().length);
        for (Tab value : Tab.values()) {
            int accent = value == tab ? BLUE : MUTED;
            addRenderableWidget(new BattleHudButton(x, y, w, 21, Component.literal(label(value)), accent, ignored -> switchTab(value)));
            x += w + gap;
        }
    }

    private void buildParty() {
        var snapshot = ClientMetaState.snapshot();
        List<ClientMetaState.CharacterRow> owned = snapshot.characters().stream().filter(ClientMetaState.CharacterRow::owned).toList();
        int contentTop = top + 108;
        int gap = 8;
        int cardW = (panelWidth - 54 - gap) / 2;
        int cardH = 30;
        for (int i = 0; i < owned.size(); i++) {
            var row = owned.get(i);
            int column = i % 2, line = i / 2;
            int x = left + 18 + column * (cardW + gap), y = contentTop + line * (cardH + 6);
            boolean selected = draftParty.contains(row.id());
            String star = row.awakened() ? "◆6" : "★" + row.star();
            String text = (selected ? "● " : "○ ") + star + "  " + row.name() + "  Lv." + row.level() + "  전투력 " + row.cp();
            addRenderableWidget(new BattleHudButton(x, y, cardW, cardH, Component.literal(text), selected ? GREEN : MUTED,
                    ignored -> toggleParty(row.id())));
        }
        int presetY = top + panelHeight - 70;
        int px = left + 18;
        for (int slot = 1; slot <= 3; slot++) {
            final int preset = slot;
            List<String> saved = snapshot.partyPresets().size() >= slot ? snapshot.partyPresets().get(slot - 1) : List.of();
            var load = new BattleHudButton(px, presetY, 76, 21, Component.literal("P" + slot + " 불러오기"), saved.isEmpty() ? MUTED : BLUE,
                    ignored -> loadPreset(preset));
            load.active = !saved.isEmpty();
            addRenderableWidget(load);
            addRenderableWidget(new BattleHudButton(px + 80, presetY, 64, 21, Component.literal("저장"), GREEN,
                    ignored -> savePreset(preset)));
            px += 150;
        }
        int y = top + panelHeight - 42;
        addRenderableWidget(new BattleHudButton(left + panelWidth - 140, y, 122, 24,
                Component.literal("편성 저장  " + draftParty.size() + "/4"), GREEN, ignored -> saveParty()));
    }

    private void buildCharacters() {
        if (!selectedCharacterId.isBlank()) {
            buildCharacterDetail();
            return;
        }
        int y = top + 104;
        int x = left + 18;
        addRenderableWidget(new BattleHudButton(x, y, 112, 22, Component.literal("보유: " + ownershipLabel()), BLUE, ignored -> cycleOwnership()));
        addRenderableWidget(new BattleHudButton(x + 118, y, 94, 22, Component.literal("Star: " + (starFilter == 0 ? "ALL" : "★" + starFilter)), GOLD, ignored -> cycleStar()));
        addRenderableWidget(new BattleHudButton(x + 218, y, 112, 22, Component.literal("Level ≥ " + (minimumLevel == 0 ? "ALL" : minimumLevel)), GREEN, ignored -> cycleLevel()));
        addRenderableWidget(new BattleHudButton(x + 336, y, 126, 22, Component.literal("Role: " + roleFilter.name()), MUTED, ignored -> cycleRole()));

        List<ClientMetaState.CharacterRow> rows = filteredCharacters();
        int gridTop = y + 34;
        int gap = 7;
        int cols = panelWidth >= 760 ? 3 : 2;
        int cardW = (panelWidth - 36 - gap * (cols - 1)) / cols;
        int perPage = cols * 4;
        int start = Math.min(page * perPage, Math.max(0, rows.size() - 1));
        int end = Math.min(rows.size(), start + perPage);
        for (int i = start; i < end; i++) {
            var row = rows.get(i);
            int local = i - start;
            int xx = left + 18 + (local % cols) * (cardW + gap);
            int yy = gridTop + (local / cols) * 43;
            String state = row.owned() ? (row.awakened() ? "◆6" : "★" + row.star()) + " Lv." + row.level() : "미보유 · 태생 ★" + row.nativeStar();
            String text = row.name() + "  /  " + state + "  /  " + primaryRoleLabel(row.primaryRole());
            addRenderableWidget(new BattleHudButton(xx, yy, cardW, 36, Component.literal(text), row.owned() ? BLUE : MUTED,
                    ignored -> openCharacter(row.id())));
        }
        buildPager(rows.size(), perPage);
    }

    private void buildCharacterDetail() {
        ClientMetaState.CharacterRow row = character(selectedCharacterId);
        if (row == null) { selectedCharacterId = ""; return; }
        int y = top + 103;
        addRenderableWidget(new BattleHudButton(left + 18, y, 74, 22, Component.literal("← 목록"), MUTED, ignored -> closeCharacter()));
        int x = left + 104;
        int w = Math.max(72, Math.min(112, (panelWidth - 140) / DetailTab.values().length));
        for (DetailTab value : DetailTab.values()) {
            addRenderableWidget(new BattleHudButton(x, y, w, 22, Component.literal(detailLabel(value)), value == detailTab ? BLUE : MUTED,
                    ignored -> switchDetail(value)));
            x += w + 4;
        }
        if (detailTab == DetailTab.AWAKENING && row.owned()) {
            int actionY = top + panelHeight - 42;
            if (row.star() < 6) {
                addRenderableWidget(new BattleHudButton(left + panelWidth - 272, actionY, 122, 23,
                        Component.literal("★ 승급"), GOLD, ignored -> send("PROMOTE|" + row.id())));
            }
            var trial = ClientSignatureTrialState.forCharacter(row.id());
            boolean awakeningReady = trial != null && trial.awakeningReady();
            String awakenLabel = row.awakened() ? "각성 완료" : awakeningReady ? "각성" : "각성 잠김";
            var awaken = new BattleHudButton(left + panelWidth - 140, actionY, 122, 23,
                    Component.literal(awakenLabel), row.awakened() ? GREEN : awakeningReady ? BLUE : MUTED,
                    ignored -> send("AWAKEN|" + row.id()));
            awaken.active = !row.awakened() && awakeningReady;
            addRenderableWidget(awaken);
        }
    }

    private void buildEquipment() {
        int y = top + 103;
        addRenderableWidget(new BattleHudButton(left + 18, y, 104, 22, Component.literal(equipView == EquipView.INVENTORY ? "INVENTORY" : "MARKET"), BLUE,
                ignored -> toggleEquipView()));
        if (equipView == EquipView.MARKET) {
            buildMarket();
            return;
        }
        addRenderableWidget(new BattleHudButton(left + 128, y, 112, 22, Component.literal("Slot: " + equipSlotFilter), MUTED, ignored -> cycleEquipSlot()));
        addRenderableWidget(new BattleHudButton(left + 246, y, 112, 22, Component.literal("Sort: " + equipSort.name()), MUTED, ignored -> cycleEquipSort()));

        List<ClientMetaState.EquipmentRow> rows = filteredEquipment();
        int listTop = y + 33;
        int listW = Math.min(440, Math.max(280, panelWidth / 2));
        int perPage = 10;
        int start = page * perPage, end = Math.min(rows.size(), start + perPage);
        for (int i = start; i < end; i++) {
            var row = rows.get(i);
            int yy = listTop + (i - start) * 31;
            String owner = row.equippedCharacterId().isBlank() ? "" : " · " + characterName(row.equippedCharacterId());
            String text = row.tier() + "  " + row.name() + "  +" + row.enhancement() + owner;
            addRenderableWidget(new BattleHudButton(left + 18, yy, listW, 26, Component.literal(text),
                    row.instanceId().equals(selectedEquipmentId) ? BLUE : tierColor(row.tier()), ignored -> selectEquipment(row.instanceId())));
        }
        buildPager(rows.size(), perPage);

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
        if (selected != null) {
            int rx = left + listW + 36;
            int rw = Math.max(210, left + panelWidth - 18 - rx);
            int targetY = top + 236;
            List<ClientMetaState.CharacterRow> owned = ClientMetaState.snapshot().characters().stream().filter(ClientMetaState.CharacterRow::owned).toList();
            int cols = Math.max(2, Math.min(4, rw / 96));
            int bw = Math.max(72, (rw - (cols - 1) * 4) / cols);
            for (int i = 0; i < owned.size(); i++) {
                var c = owned.get(i);
                int xx = rx + (i % cols) * (bw + 4), yy = targetY + (i / cols) * 26;
                addRenderableWidget(new BattleHudButton(xx, yy, bw, 22, Component.literal(c.name()),
                        c.id().equals(equipmentTargetCharacterId) ? GREEN : MUTED, ignored -> selectEquipmentTarget(c.id())));
            }
            int actionY = top + panelHeight - 70;
            var enhance = new BattleHudButton(rx, actionY, Math.min(112, rw / 3), 23,
                    Component.literal(selected.enhancement() >= 20 ? "+20 완료" : "강화 +1"), GOLD,
                    ignored -> send("ENHANCE|" + selected.instanceId()));
            enhance.active = selected.enhancement() < 20;
            addRenderableWidget(enhance);
            var equipButton = new BattleHudButton(rx + Math.min(118, rw / 3 + 6), actionY, Math.min(112, rw / 3), 23,
                    Component.literal("장착"), GREEN, ignored -> equipSelected());
            equipButton.active = !equipmentTargetCharacterId.isBlank();
            addRenderableWidget(equipButton);
            String saleLabel = selected.tier().equals("SIGNATURE") ? "전용 장비 · 판매 불가"
                    : selected.equippedCharacterId().isBlank() ? "판매 · " + selected.salePrice() + "G"
                    : "장착 해제 후 판매 · " + selected.salePrice() + "G";
            var sell = new BattleHudButton(rx, actionY + 29, Math.min(230, rw), 22,
                    Component.literal(saleLabel), selected.sellable() ? GOLD : MUTED, ignored -> sellSelected());
            sell.active = selected.sellable();
            addRenderableWidget(sell);
        }
    }

    private void buildMarket() {
        var rows = ClientMetaState.snapshot().shopItems();
        int perPage = 10;
        int start = page * perPage, end = Math.min(rows.size(), start + perPage);
        int y = top + 138;
        for (int i = start; i < end; i++) {
            var row = rows.get(i);
            String text = row.tier() + "  " + row.name() + "  · " + slotLabel(row.slot()) + "  · " + row.price() + "G";
            var button = new BattleHudButton(left + 18, y, Math.min(panelWidth - 36, 560), 27,
                    Component.literal(text), row.unlocked() ? GOLD : MUTED, ignored -> buy(row));
            button.active = row.unlocked() && ClientMetaState.snapshot().gold() >= row.price();
            addRenderableWidget(button);
            y += 32;
        }
        buildPager(rows.size(), perPage);
    }

    private void buildArchive() {
        var s = ClientMetaState.snapshot();
        int y = top + 104;
        var single = new BattleHudButton(left + 18, y, 126, 25, Component.literal("1회 소환 · 300"), BLUE, ignored -> send("SUMMON1"));
        single.active = s.crystal() >= GachaCatalog.SINGLE_COST;
        addRenderableWidget(single);
        var ten = new BattleHudButton(left + 152, y, 138, 25, Component.literal("10회 소환 · 3000"), GOLD, ignored -> send("SUMMON10"));
        ten.active = s.crystal() >= GachaCatalog.TEN_COST;
        addRenderableWidget(ten);
        if (s.starterArchiveAvailable()) {
            var starter = new BattleHudButton(left + 298, y, 176, 25, Component.literal("초기 10회 · 3000"), GREEN, ignored -> send("STARTER"));
            starter.active = s.crystal() >= GachaCatalog.TEN_COST;
            addRenderableWidget(starter);
        }
        buildPager(s.archiveHistory().size(), 12);
    }

    private void buildCodex() {
        int x = left + 18, y = top + 104;
        for (String category : List.of("CHARACTERS", "ENEMIES", "BOSSES", "EQUIPMENT", "TUTORIAL")) {
            int w = category.equals("CHARACTERS") ? 112 : category.equals("EQUIPMENT") ? 112 : 96;
            addRenderableWidget(new BattleHudButton(x, y, w, 22, Component.literal(category), category.equals(codexCategory) ? BLUE : MUTED,
                    ignored -> selectCodex(category)));
            x += w + 5;
        }
        int size = ClientMetaState.snapshot().codex().stream().filter(row -> row.category().equals(codexCategory)).toList().size();
        buildPager(size, 18);
    }

    private void buildEndgame() {
        var rows = ClientMetaState.snapshot().endgame();
        if (selectedEndgameId.isBlank() || endgame(selectedEndgameId) == null) {
            selectedEndgameId = rows.stream().filter(r -> r.unlocked() && !r.cleared()).map(ClientMetaState.EndgameRow::id).findFirst()
                    .orElse(rows.stream().filter(ClientMetaState.EndgameRow::unlocked).map(ClientMetaState.EndgameRow::id).findFirst().orElse(""));
        }

        int hardX = left + 18, y = top + 120;
        int hardW = Math.min(252, Math.max(180, panelWidth / 3));
        for (var row : rows.stream().filter(r -> "HARD".equals(r.kind())).toList()) {
            boolean selected = row.id().equals(selectedEndgameId);
            String status = row.cleared() ? "✓ " : row.unlocked() ? "" : "LOCK · ";
            int color = selected ? BLUE : row.cleared() ? GREEN : row.unlocked() ? DANGER : MUTED;
            var button = new BattleHudButton(hardX, y, hardW, 27,
                    Component.literal(status + row.label()), color, ignored -> selectEndgame(row.id()));
            button.active = row.unlocked(); addRenderableWidget(button); y += 32;
        }

        int gridLeft = left + hardW + 38, gridTop = top + 120;
        int available = left + panelWidth - 18 - gridLeft;
        int cols = available >= 500 ? 6 : available >= 360 ? 5 : 4;
        int cellW = Math.max(54, (available - (cols - 1) * 5) / cols), index = 0;
        for (var row : rows.stream().filter(r -> "RIFT".equals(r.kind())).toList()) {
            int xx = gridLeft + (index % cols) * (cellW + 5), yy = gridTop + (index / cols) * 31;
            boolean selected = row.id().equals(selectedEndgameId);
            String text = (row.cleared() ? "✓ " : "") + "F" + row.id().substring(row.id().length() - 2) + " · " + row.level();
            int color = selected ? BLUE : row.cleared() ? GREEN : row.hardPattern() ? GOLD : row.unlocked() ? SECONDARY : MUTED;
            var button = new BattleHudButton(xx, yy, cellW, 25, Component.literal(text), color, ignored -> selectEndgame(row.id()));
            button.active = row.unlocked(); addRenderableWidget(button); index++;
        }

        ClientMetaState.EndgameRow selected = endgame(selectedEndgameId);
        if (selected != null && selected.unlocked()) {
            int by = top + panelHeight - 42;
            addRenderableWidget(new BattleHudButton(left + panelWidth - 176, by, 158, 24,
                    Component.literal("브리핑 / 출전"), "HARD".equals(selected.kind()) ? DANGER : BLUE, ignored -> start(selected)));
        }
    }

    private void buildPager(int total, int perPage) {
        int pages = Math.max(1, (total + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int y = top + panelHeight - 40;
        if (page > 0) addRenderableWidget(new BattleHudButton(left + 18, y, 74, 22, Component.literal("< 이전"), MUTED, ignored -> movePage(-1)));
        if (page + 1 < pages) addRenderableWidget(new BattleHudButton(left + 98, y, 74, 22, Component.literal("다음 >"), MUTED, ignored -> movePage(1)));
    }

    private List<ClientMetaState.CharacterRow> filteredCharacters() {
        Comparator<ClientMetaState.CharacterRow> comparator = Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar).reversed()
                .thenComparing(ClientMetaState.CharacterRow::id);
        return ClientMetaState.snapshot().characters().stream()
                .filter(row -> ownershipFilter == OwnershipFilter.ALL || (ownershipFilter == OwnershipFilter.OWNED) == row.owned())
                .filter(row -> starFilter == 0 || (row.owned() ? row.star() : row.nativeStar()) == starFilter)
                .filter(row -> minimumLevel == 0 || (row.owned() && row.level() >= minimumLevel))
                .filter(row -> roleFilter == RoleFilter.ALL || row.primaryRole().equals(roleFilter.name()))
                .sorted(comparator).toList();
    }

    private List<ClientMetaState.EquipmentRow> filteredEquipment() {
        Comparator<ClientMetaState.EquipmentRow> comparator = switch (equipSort) {
            case LEVEL -> Comparator.comparingInt(ClientMetaState.EquipmentRow::enhancement).reversed().thenComparing(ClientMetaState.EquipmentRow::name);
            case STAT -> Comparator.comparing(ClientMetaState.EquipmentRow::mainType).thenComparing(ClientMetaState.EquipmentRow::subType).thenComparing(ClientMetaState.EquipmentRow::name);
            case TIER -> Comparator.comparingInt((ClientMetaState.EquipmentRow row) -> tierRank(row.tier())).reversed().thenComparing(ClientMetaState.EquipmentRow::name);
        };
        return ClientMetaState.snapshot().equipment().stream()
                .filter(row -> equipSlotFilter.equals("ALL") || row.slot().equals(equipSlotFilter))
                .sorted(comparator).toList();
    }

    private void switchTab(Tab value) {
        if (value == tab) return;
        tab = value; page = 0; selectedCharacterId = ""; selectedEquipmentId = "";
        clearWidgets(); init();
    }
    private void movePage(int delta) { page = Math.max(0, page + delta); clearWidgets(); init(); }
    private void toggleParty(String id) {
        if (draftParty.contains(id)) { if (draftParty.size() > 1) draftParty.remove(id); }
        else if (draftParty.size() < 4) draftParty.add(id);
        clearWidgets(); init();
    }
    private void saveParty() { send("PARTY|" + String.join(",", draftParty)); }
    private void savePreset(int slot) { send("PRESET_SAVE|" + slot); }
    private void loadPreset(int slot) { send("PRESET_LOAD|" + slot); }
    private void buy(ClientMetaState.ShopRow row) { if (row.unlocked()) send("BUY|" + row.itemId()); }
    private void start(ClientMetaState.EndgameRow row) { if (row.unlocked()) send("START|" + row.id()); }
    private void selectEndgame(String id) { selectedEndgameId=id; clearWidgets(); init(); }

    private void openCharacter(String id) { selectedCharacterId = id; detailTab = DetailTab.STATUS; page = 0; clearWidgets(); init(); }
    private void closeCharacter() { selectedCharacterId = ""; page = 0; clearWidgets(); init(); }
    private void switchDetail(DetailTab value) { detailTab = value; clearWidgets(); init(); }
    private void cycleOwnership() { ownershipFilter = OwnershipFilter.values()[(ownershipFilter.ordinal()+1)%OwnershipFilter.values().length]; page=0; clearWidgets(); init(); }
    private void cycleStar() { starFilter = (starFilter + 1) % 7; page=0; clearWidgets(); init(); }
    private void cycleLevel() { minimumLevel = minimumLevel == 0 ? 10 : minimumLevel >= 60 ? 0 : minimumLevel + 10; page=0; clearWidgets(); init(); }
    private void cycleRole() { roleFilter = RoleFilter.values()[(roleFilter.ordinal()+1)%RoleFilter.values().length]; page=0; clearWidgets(); init(); }
    private String ownershipLabel() { return switch(ownershipFilter){case ALL->"전체";case OWNED->"보유";case UNOWNED->"미보유";}; }

    private void toggleEquipView() { equipView = equipView == EquipView.INVENTORY ? EquipView.MARKET : EquipView.INVENTORY; page=0; selectedEquipmentId=""; clearWidgets(); init(); }
    private void cycleEquipSlot() { List<String> v=List.of("ALL","WEAPON","ARMOR","ACCESSORY","SIGNATURE"); equipSlotFilter=v.get((v.indexOf(equipSlotFilter)+1)%v.size());page=0;clearWidgets();init(); }
    private void cycleEquipSort() { equipSort=EquipSort.values()[(equipSort.ordinal()+1)%EquipSort.values().length];page=0;clearWidgets();init(); }
    private void selectEquipment(String id) { selectedEquipmentId=id; if(equipmentTargetCharacterId.isBlank()) equipmentTargetCharacterId=ClientMetaState.snapshot().activeParty().stream().findFirst().orElse(""); clearWidgets();init(); }
    private void selectEquipmentTarget(String id) { equipmentTargetCharacterId=id; clearWidgets();init(); }
    private void equipSelected() { if(!selectedEquipmentId.isBlank()&&!equipmentTargetCharacterId.isBlank()) send("EQUIP|"+equipmentTargetCharacterId+"|"+selectedEquipmentId); }
    private void sellSelected() { var selected=equipment(selectedEquipmentId); if(selected!=null&&selected.sellable()) send("SELL|"+selected.instanceId()); }
    private void selectCodex(String category) { codexCategory=category;page=0;clearWidgets();init(); }

    private static void send(String command) { ClientPacketDistributor.sendToServer(new MetaCommandPayload(command)); }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_E || event.key() == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TurnboundFrameStyle.frame(graphics, left, top, panelWidth, panelHeight, BLUE);
        TurnboundFrameStyle.inset(graphics, left + 14, top + 29, panelWidth - 28, 23);
        graphics.text(font, Component.literal("TURNBOUND"), left + 18, top + 15, TEXT, true);
        var snapshot = ClientMetaState.snapshot();
        String resources = "골드 " + snapshot.gold() + "    크리스탈 " + snapshot.crystal() + "    별의 정수 " + snapshot.essence()
                + "    각성 코어 " + snapshot.core() + "    파티 전투력 " + snapshot.partyCp();
        graphics.text(font, Component.literal(resources), left + 22, top + 36, SECONDARY, false);
        graphics.text(font, Component.literal(title(tab)), left + 18, top + 88, TEXT, true);
        switch (tab) {
            case PARTY -> drawPartyInfo(graphics, snapshot);
            case CHARACTERS -> drawCharacters(graphics, snapshot);
            case EQUIPMENT -> drawEquipment(graphics, snapshot);
            case ARCHIVE -> drawArchive(graphics, snapshot);
            case QUESTS -> drawQuests(graphics, snapshot);
            case CODEX -> drawCodex(graphics, snapshot);
            case SYSTEM -> drawSystem(graphics, snapshot);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPartyInfo(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        graphics.text(font, Component.literal("4인 편성 · 참가 100% 경험치 · 미편성 보유 캐릭터 20% 경험치 · 중복 편성 불가"), left + 160, top + 88, SECONDARY, false);
        int x=left+18, y=top+panelHeight-86;
        for(int i=0;i<3;i++){
            List<String> preset=snapshot.partyPresets().size()>i?snapshot.partyPresets().get(i):List.of();
            String summary=preset.isEmpty()?"비어 있음":preset.stream().map(MetaMenuScreen::characterName).reduce((a,b)->a+" / "+b).orElse("");
            graphics.text(font, Component.literal("P"+(i+1)+"  "+summary), x+i*150, y, preset.isEmpty()?MUTED:SECONDARY, false);
        }
    }

    private void drawCharacters(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        if (selectedCharacterId.isBlank()) {
            long owned=snapshot.characters().stream().filter(ClientMetaState.CharacterRow::owned).count();
            graphics.text(font, Component.literal("전체 12 · 보유 " + owned + " · 별 등급 / 레벨 / 역할 필터"), left+480, top+108, SECONDARY, false);
            return;
        }
        var row=character(selectedCharacterId);
        if(row==null)return;
        int x=left+18,y=top+145;
        graphics.text(font, Component.literal(row.name()+"  "+(row.owned()?(row.awakened()?"◆6":"★"+row.star())+" Lv."+row.level():"미보유 · 태생 ★"+row.nativeStar())), x,y,row.owned()?TEXT:MUTED,true);
        graphics.text(font, Component.literal(roleDescription(row.role())+" · "+row.difficulty()),x,y+16,SECONDARY,false);
        switch(detailTab){
            case STATUS->drawCharacterStatus(graphics,row,x,y+45);
            case SKILLS->drawCharacterSkills(graphics,row,x,y+45);
            case EQUIPMENT->drawCharacterEquipment(graphics,row,x,y+45);
            case AWAKENING->drawCharacterAwakening(graphics,row,x,y+45);
            case PROFILE->drawCharacterProfile(graphics,row,x,y+45);
        }
    }

    private void drawCharacterStatus(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        graphics.text(font,Component.literal("HP  "+row.hp()+"    ATK  "+row.attack()+"    DEF  "+row.defense()+"    SPD  "+row.speed()),x,y,GREEN,false);
        graphics.text(font,Component.literal("전투력  "+row.cp()+"    태생 ★"+row.nativeStar()+"    현재 "+(row.awakened()?"◆6":"★"+row.star())),x,y+20,TEXT,false);
        if(!row.owned()) graphics.text(font,Component.literal("미보유 캐릭터의 수치는 Lv.1 태생 기준."),x,y+42,MUTED,false);
    }

    private void drawCharacterSkills(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        var definition=CanonicalData.definition(row.id(),Math.max(1,row.level()),row.star(),row.awakened());
        int yy=y;
        for(var skill:definition.skills()){
            String kind=skill.id().equals(definition.basicSkillId())?"기본":"액티브";
            graphics.text(font,Component.literal(kind+" · "+skill.name()+"  쿨타임 "+skill.cooldown()),x,yy,GOLD,false);
            graphics.text(font,Component.literal(shorten(skill.description(),100)),x+12,yy+14,TEXT,false);
            yy+=36;
        }
        var menu=CharacterMenuCatalog.profile(row.id());
        graphics.text(font,Component.literal((row.awakened()?"◆ ":"🔒 ")+"각성 추가효과"),x,yy+3,row.awakened()?GREEN:MUTED,false);
        graphics.text(font,Component.literal(shorten(menu.awakening(),110)),x+12,yy+18,row.awakened()?TEXT:MUTED,false);
    }

    private void drawCharacterEquipment(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        if(!row.owned()){graphics.text(font,Component.literal("캐릭터 미보유"),x,y,MUTED,false);return;}
        List<ClientMetaState.EquipmentRow> equipped=ClientMetaState.snapshot().equipment().stream().filter(e->e.equippedCharacterId().equals(row.id())).toList();
        int yy=y;
        for(String slot:List.of("WEAPON","ARMOR","ACCESSORY","SIGNATURE")){
            var item=equipped.stream().filter(e->e.slot().equals(slot)).findFirst().orElse(null);
            String text=item==null?slotLabel(slot)+"  ·  비어 있음":slotLabel(slot)+"  ·  "+item.name()+" +"+item.enhancement();
            graphics.text(font,Component.literal(text),x,yy,item==null?MUTED:tierColor(item.tier()),false);yy+=21;
        }
        graphics.text(font,Component.literal("장착 변경은 장비 탭에서 수행합니다."),x,yy+10,SECONDARY,false);
    }

    private void drawCharacterAwakening(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        var menu=CharacterMenuCatalog.profile(row.id());
        if(!row.owned()){graphics.text(font,Component.literal("캐릭터를 획득해야 성장할 수 있습니다."),x,y,MUTED,false);return;}
        graphics.text(font,Component.literal("현재 성급  "+(row.awakened()?"◆6":"★"+row.star())+"   /   Lv."+row.level()),x,y,TEXT,false);
        graphics.text(font,Component.literal("각성 패키지"),x,y+28,GOLD,true);
        graphics.text(font,Component.literal(shorten(menu.awakening(),115)),x+12,y+45,row.awakened()?GREEN:MUTED,false);

        var trial=ClientSignatureTrialState.forCharacter(row.id());
        int yy=y+72;
        if(trial==null){
            graphics.text(font,Component.literal("전용 장비 시련 상태를 동기화하는 중입니다."),x,yy,MUTED,false);
            return;
        }

        String trialState;
        int trialColor;
        if(trial.firstClearClaimed()){
            trialState="클리어"; trialColor=GREEN;
        }else if(trial.canEnter()){
            trialState="입장 가능"; trialColor=BLUE;
        }else if(trial.progressionReady()&&!trial.encounterCanonReady()){
            trialState="CANON GAP"; trialColor=GOLD;
        }else{
            trialState="잠김"; trialColor=MUTED;
        }
        graphics.text(font,Component.literal("전용 장비 시련 · "+trial.title()+" · "+trialState),x,yy,trialColor,true);
        graphics.text(font,Component.literal("목표 · "+shorten(trial.objective(),110)),x+12,yy+18,TEXT,false);
        String prerequisites=(trial.endgameUnlocked()?"✓":"○")+" B05   "+(trial.characterQuestComplete()?"✓":"○")+" 개인 퀘스트   "
                +(trial.level()==60?"✓":"○")+" Lv60   "+(trial.currentStar()==6?"✓":"○")+" ★6";
        graphics.text(font,Component.literal("선행 · "+prerequisites),x+12,yy+36,SECONDARY,false);

        int statusY=yy+58;
        if(trial.firstClearClaimed()){
            String equipmentState=trial.signaturePending()?"전용 장비 수령 대기":trial.signatureGranted()?"전용 장비 획득 완료":"전용 장비 보상 기록 확인 필요";
            graphics.text(font,Component.literal("✓ 첫 클리어 · "+equipmentState),x,statusY,trial.signatureGranted()?GREEN:GOLD,false);
        }else if(trial.canEnter()){
            graphics.text(font,Component.literal("입장 가능 · 라디아 Signature Trial Hall에서 시련을 시작할 수 있습니다."),x,statusY,BLUE,false);
        }else if(trial.progressionReady()&&!trial.encounterCanonReady()){
            graphics.text(font,Component.literal("◇ CANON GAP · "+shorten(trial.blockReason(),100)),x,statusY,GOLD,false);
        }else{
            graphics.text(font,Component.literal("잠김 · "+shorten(trial.blockReason(),100)),x,statusY,MUTED,false);
        }

        int awakeningY=statusY+22;
        if(row.awakened()){
            graphics.text(font,Component.literal("◆ 각성 완료 · 전용 장비 시련 및 각성 코어 소비 완료"),x,awakeningY,GREEN,true);
        }else if(trial.awakeningReady()){
            graphics.text(font,Component.literal("◆ 각성 가능 · 각성 코어 "+trial.awakeningCore()+"개 보유"),x,awakeningY,BLUE,true);
        }else{
            graphics.text(font,Component.literal("각성 잠김 · 시련 첫 클리어 + 각성 코어 필요 · 현재 코어 "+trial.awakeningCore()),x,awakeningY,MUTED,false);
        }
    }

    private void drawCharacterProfile(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        var menu=CharacterMenuCatalog.profile(row.id());
        if(!row.profileUnlocked()){
            graphics.text(font,Component.literal("🔒 캐릭터 퀘스트 완료 후 프로필 해금"),x,y,MUTED,true);return;
        }
        graphics.text(font,Component.literal("역할  ·  "+roleDescription(menu.role())),x,y,TEXT,false);
        graphics.text(font,Component.literal("무기  ·  "+menu.weapon()),x,y+20,SECONDARY,false);
        graphics.text(font,Component.literal("채용 이유  ·  "+shorten(menu.reason(),100)),x,y+42,TEXT,false);
        graphics.text(font,Component.literal("성격  ·  "+shorten(menu.personality(),100)),x,y+64,SECONDARY,false);
    }

    private void drawEquipment(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        if(equipView==EquipView.MARKET){
            graphics.text(font,Component.literal("상점 · T1/T2 확정 구매 · 강화 실패/파괴 없음"),left+600,top+108,SECONDARY,false);return;
        }
        graphics.text(font,Component.literal("인벤토리  "+snapshot.equipment().size()+" / 300"),left+480,top+108,SECONDARY,false);
        var selected=equipment(selectedEquipmentId);
        if(selected==null){graphics.text(font,Component.literal("장비를 선택하면 비교 / +20 미리보기 / 강화 / 장착 정보를 볼 수 있습니다."),left+500,top+150,MUTED,false);return;}
        int x=left+Math.min(480,panelWidth/2+18),y=top+150;
        graphics.text(font,Component.literal(selected.tier()+"  "+selected.name()+"  +"+selected.enhancement()),x,y,tierColor(selected.tier()),true);
        graphics.text(font,Component.literal(statTypeLabel(selected.mainType())+"  "+stat(selected.mainValue())+"    "+statTypeLabel(selected.subType())+"  "+stat(selected.subValue())),x,y+20,TEXT,false);
        graphics.text(font,Component.literal("+20 미리보기  ·  "+statTypeLabel(selected.mainType())+" "+stat(selected.mainAt20())+" / "+statTypeLabel(selected.subType())+" "+stat(selected.subAt20())),x,y+40,GOLD,false);
        graphics.text(font,Component.literal("현재 장착  ·  "+(selected.equippedCharacterId().isBlank()?"없음":characterName(selected.equippedCharacterId()))),x,y+62,SECONDARY,false);
        if(!equipmentTargetCharacterId.isBlank()){
            var current=snapshot.equipment().stream().filter(e->e.equippedCharacterId().equals(equipmentTargetCharacterId)&&e.slot().equals(selected.slot())).findFirst().orElse(null);
            String compare=current==null?"비교: 해당 부위 비어 있음":"비교: "+current.name()+" +"+current.enhancement()+"  →  "+selected.name()+" +"+selected.enhancement();
            graphics.text(font,Component.literal(compare),x,y+82,current==null?GREEN:BLUE,false);
        }
        String saleInfo=selected.tier().equals("SIGNATURE")?"전용 장비 · 판매 불가"
                : selected.equippedCharacterId().isBlank()?"판매 · "+selected.salePrice()+" 골드":"판매 · "+selected.salePrice()+" 골드 · 장착 해제 필요";
        graphics.text(font,Component.literal(saleInfo),x,y+105,selected.sellable()?GOLD:MUTED,false);
    }

    private void drawArchive(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot s) {
        graphics.text(font, Component.literal("★5 천장  " + s.fiveStarPity() + " / " + GachaCatalog.HARD_PITY + "    보유 크리스탈 " + s.crystal()), left + 500, top + 108, GOLD, true);
        graphics.text(font, Component.literal("확률  ★5 3% · ★4 12% · ★3 35% · ★2 30% · ★1 20%"), left + 500, top + 124, SECONDARY, false);
        graphics.text(font, Component.literal("소프트 천장: 65회부터 매회 +3%p · 80회 ★5 확정 · 10회 최소 ★4"), left + 500, top + 140, SECONDARY, false);
        int perPage=12,start=page*perPage,end=Math.min(s.archiveHistory().size(),start+perPage),y=top+174;
        graphics.text(font,Component.literal("최근 획득 기록 "+s.archiveHistory().size()+" / 50"),left+18,y-18,TEXT,true);
        for(int i=start;i<end;i++){
            var row=s.archiveHistory().get(i);
            String text="★"+row.nativeStars()+"  "+row.name()+(row.newlyOwned()?"  신규":"  → 별의 정수 +"+row.essenceGranted())+"  · 천장 "+row.pityAfter();
            graphics.text(font,Component.literal(text),left+18,y,row.newlyOwned()?GREEN:SECONDARY,false);y+=18;
        }
    }

    private void drawQuests(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        int y=top+112;
        graphics.text(font,Component.literal("메인 / 캐릭터 / 지역 / 도전 · 동시에 최대 3개 추적"),left+18,y,SECONDARY,false);
        y+=24;
        for(int i=0;i<snapshot.regionQuests().size();i++){
            var q=snapshot.regionQuests().get(i);
            graphics.text(font,Component.literal((q.completed()?"✓ ":"○ ")+"지역 탐사 "+(i+1)+" · "+regionLabel(q.region())),left+18,y,q.completed()?GREEN:TEXT,false);y+=16;
            if(y>top+panelHeight-90)break;
        }
        int x=left+panelWidth/2+8,yy=top+136;
        for(var c:snapshot.challenges()){
            String mark=c.completed()?"✓ ":c.autoEvaluable()?"○ ":"◇ ";
            graphics.text(font,Component.literal(mark+c.ordinal()+". "+c.label()),x,yy,c.completed()?GREEN:c.autoEvaluable()?TEXT:GOLD,false);yy+=16;
            if(yy>top+panelHeight-60)break;
        }
    }

    private void drawCodex(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        List<ClientMetaState.CodexRow> rows=snapshot.codex().stream().filter(row->row.category().equals(codexCategory)).toList();
        int perPage=18,start=page*perPage,end=Math.min(rows.size(),start+perPage),cols=panelWidth>=760?3:2;
        int gap=8,cardW=(panelWidth-36-gap*(cols-1))/cols,gridTop=top+140;
        if(codexCategory.equals("TUTORIAL")&&rows.isEmpty()){
            graphics.text(font,Component.literal("아직 기록되지 않은 튜토리얼 항목입니다."),left+18,gridTop,MUTED,false);return;
        }
        for(int i=start;i<end;i++){
            var row=rows.get(i);int local=i-start,x=left+18+(local%cols)*(cardW+gap),y=gridTop+(local/cols)*52;
            boolean silhouette=(row.category().equals("ENEMIES")||row.category().equals("BOSSES"))&&!row.discovered();
            String name=silhouette?"████  ???  ████":row.name();
            TurnboundFrameStyle.inset(graphics,x,y,cardW,42);
            graphics.text(font,Component.literal(name),x+10,y+8,silhouette?MUTED:row.discovered()?TEXT:MUTED,false);
            String detail=silhouette?"미발견":row.detailUnlocked()?codexSummary(row.summary()):row.discovered()?"상세 정보 잠김":"미보유";
            graphics.text(font,Component.literal(detail),x+10,y+24,row.detailUnlocked()?SECONDARY:MUTED,false);
        }
    }

    private void drawSystem(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        graphics.text(font,Component.literal(snapshot.riftUnlocked()?"균열 관문 · 개방":"균열 관문 · B05 클리어 후 개방"),left+300,top+88,snapshot.riftUnlocked()?GREEN:MUTED,false);
        ClientMetaState.EndgameRow row=endgame(selectedEndgameId);
        if(row==null){graphics.text(font,Component.literal("도전할 콘텐츠를 선택하세요."),left+18,top+330,MUTED,false);return;}
        int x=left+18,y=Math.min(top+335,top+panelHeight-190);
        TurnboundFrameStyle.inset(graphics,x,y,panelWidth-36,92);
        int accent="HARD".equals(row.kind())?DANGER:row.hardPattern()?GOLD:BLUE;
        graphics.fill(x+2,y+4,x+5,y+88,accent);
        graphics.text(font,Component.literal(endgameLabel(row)),x+12,y+10,TEXT,true);
        String kind="HARD".equals(row.kind())?"하드":"균열";
        graphics.text(font,Component.literal((row.cleared()?"클리어 · ":"")+kind+" · Lv."+row.level()+" · 파티 전투력 "+snapshot.partyCp()),x+12,y+27,row.cleared()?GREEN:SECONDARY,false);
        if("HARD".equals(row.kind())){
            graphics.text(font,Component.literal("하드 · HP ×1.65 · ATK ×1.25 · DEF ×1.15 · SPD +8"),x+12,y+46,DANGER,false);
            graphics.text(font,Component.literal(row.cleared()?"반복 보상 · 골드":"첫 클리어 · 크리스탈 600 · T4 장비 선택권 ×1"),x+12,y+64,row.cleared()?SECONDARY:GOLD,false);
        }else{
            int floor=Integer.parseInt(row.id().substring(row.id().length()-2));
            var spec=V04Catalogs.riftFloor(floor);
            String enemies=spec.enemies().stream().map(id->CanonicalData.definition(id).name()).reduce((a,b)->a+" · "+b).orElse("-");
            graphics.text(font,Component.literal(shorten("적 · "+enemies,100)),x+12,y+46,row.hardPattern()?GOLD:SECONDARY,false);
            String reward=row.cleared()?"반복 보상 · 골드 "+V04Catalogs.riftGold(floor)
                    :"첫 클리어 · 골드 "+V04Catalogs.riftGold(floor)+" · 크리스탈 60 · 별의 정수 25";
            graphics.text(font,Component.literal(reward),x+12,y+64,row.cleared()?SECONDARY:GOLD,false);
            int rec=riftRecommendedCp(floor);
            if(rec>0) graphics.text(font,Component.literal("권장 전투력 "+rec+" · 미달이어도 입장 가능"),left+panelWidth-226,y+27,snapshot.partyCp()<rec?GOLD:GREEN,false);
        }
        graphics.text(font,Component.literal("라디아 도전 회랑에서도 같은 콘텐츠를 선택할 수 있습니다."),left+18,top+panelHeight-24,SECONDARY,false);
    }

    private ClientMetaState.CharacterRow character(String id){return ClientMetaState.snapshot().characters().stream().filter(row->row.id().equals(id)).findFirst().orElse(null);}
    private ClientMetaState.EquipmentRow equipment(String id){return ClientMetaState.snapshot().equipment().stream().filter(row->row.instanceId().equals(id)).findFirst().orElse(null);}
    private ClientMetaState.EndgameRow endgame(String id){return ClientMetaState.snapshot().endgame().stream().filter(row->row.id().equals(id)).findFirst().orElse(null);}

    private static int riftRecommendedCp(int floor){return switch(floor){case 10->18_000;case 20->27_000;case 30->38_000;default->0;};}
    private static int tierRank(String tier){return switch(tier){case "SIGNATURE"->5;case "T4"->4;case "T3"->3;case "T2"->2;case "T1"->1;default->0;};}
    private static int tierColor(String tier){return switch(tier){case "SIGNATURE"->0xFFC794FF;case "T4"->0xFFFFC857;case "T3"->0xFFB68CFF;case "T2"->0xFF6DC6FF;default->0xFFAEB7C6;};}
    private static String stat(double value){double abs=Math.abs(value);if(abs<=1.0)return String.format(Locale.ROOT,"%.1f%%",value*100.0);return String.format(Locale.ROOT,"%.1f",value);}
    private static String shorten(String value,int max){return value.length()<=max?value:value.substring(0,max-1)+"…";}
    private static String detailLabel(DetailTab tab){return switch(tab){case STATUS->"능력치";case SKILLS->"스킬";case EQUIPMENT->"장비";case AWAKENING->"각성";case PROFILE->"프로필";};}
    private static String label(Tab tab){return switch(tab){case PARTY->"파티";case CHARACTERS->"캐릭터";case EQUIPMENT->"장비";case ARCHIVE->"소환";case QUESTS->"퀘스트";case CODEX->"도감";case SYSTEM->"도전";};}
    private static String title(Tab tab){return switch(tab){case PARTY->"파티 편성";case CHARACTERS->"캐릭터";case EQUIPMENT->"장비 / 상점";case ARCHIVE->"소환 기록";case QUESTS->"퀘스트";case CODEX->"도감";case SYSTEM->"도전 콘텐츠";};}
    private static String primaryRoleLabel(String role){return switch(role){case "DPS"->"공격";case "SUPPORT"->"지원";case "TANK"->"수호";case "SUMMON"->"소환";default->role;};}
    private static String roleDescription(String role){return switch(role){
        case "Single DPS / Combo / Boss Focus"->"단일 공격 / 연계 / 보스 집중";
        case "Gauge Control / Tempo Support"->"게이지 조작 / 템포 지원";
        case "Tank / Redirect / Counter"->"수호 / 피해 전환 / 반격";
        case "Heal / Revive / Safety"->"회복 / 부활 / 안정";
        case "Follow-up / Single DPS / Team Synergy"->"추격 / 단일 공격 / 파티 연계";
        case "Death Trigger / Burst / Comeback"->"사망 연계 / 폭발력 / 역전";
        case "Summon / Utility / Extra Body"->"소환 / 지원 / 추가 행동체";
        case "Low HP / Burst / High Risk"->"저체력 / 폭발력 / 고위험";
        case "Basic Melee"->"기본 근접";case "Basic Heal"->"기본 회복";case "Ranged DPS"->"원거리 공격";case "Basic Defense"->"기본 방어";
        default->role;};}
    private static String slotLabel(String slot){return switch(slot){case "WEAPON"->"무기";case "ARMOR"->"방어구";case "ACCESSORY"->"장신구";case "SIGNATURE"->"전용 장비";case "ALL"->"전체";default->slot;};}
    private static String statTypeLabel(String type){return switch(type){
        case "HP_FLAT"->"HP";case "HP_PERCENT"->"HP%";case "ATK_FLAT"->"ATK";case "ATK_PERCENT"->"ATK%";
        case "DEF_FLAT"->"DEF";case "DEF_PERCENT"->"DEF%";case "SPD_FLAT"->"SPD";case "SPD_PERCENT"->"SPD%";
        default->type;};}
    private static String regionLabel(String region){return switch(region){case "MEADOW"->"남부 초원";case "GLOAMWOOD"->"글룸우드";case "AQUEDUCT"->"붕괴 수로";case "QUARRY"->"잿불 채석장";default->region;};}
    private static String codexSummary(String summary){
        if(summary==null)return "";
        if(summary.startsWith("SIGNATURE"))return "전용 장비";
        return switch(summary){case "Boss"->"보스";case "Elite"->"엘리트";case "Enemy"->"일반 적";default->summary.replace("WEAPON","무기").replace("ARMOR","방어구").replace("ACCESSORY","장신구");};
    }
    private static String endgameLabel(ClientMetaState.EndgameRow row){
        if("HARD".equals(row.kind()))return row.label().replace(" Hard"," · 하드");
        if("RIFT".equals(row.kind()))return "균열 관문 F"+row.id().substring(row.id().length()-2);
        return row.label();
    }
    private static String characterName(String id){
        if(id==null||id.isBlank())return "";
        return ClientMetaState.snapshot().characters().stream().filter(row->row.id().equals(id)).map(ClientMetaState.CharacterRow::name).findFirst().orElse(id);
    }
}
