package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.CharacterMenuCatalog;
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
    private static final int PANEL = 0xCC10141D;

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
            String text = (selected ? "● " : "○ ") + star + "  " + row.name() + "  Lv." + row.level() + "  CP " + row.cp();
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
            String text = row.name() + "  /  " + state + "  /  " + row.primaryRole();
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
            var awaken = new BattleHudButton(left + panelWidth - 140, actionY, 122, 23,
                    Component.literal(row.awakened() ? "각성 완료" : "각성"), row.awakened() ? GREEN : BLUE,
                    ignored -> send("AWAKEN|" + row.id()));
            awaken.active = !row.awakened();
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
            String owner = row.equippedCharacterId().isBlank() ? "" : " · " + row.equippedCharacterId();
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
                    Component.literal(selected.enhancement() >= 20 ? "+20 MAX" : "강화 +1"), GOLD,
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
            String text = row.tier() + "  " + row.name() + "  · " + row.slot() + "  · " + row.price() + "G";
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
            var starter = new BattleHudButton(left + 298, y, 176, 25, Component.literal("Starter 10회 · 3000"), GREEN, ignored -> send("STARTER"));
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
        int hardX = left + 18, y = top + 112;
        for (var row : rows.stream().filter(r -> "HARD".equals(r.kind())).toList()) {
            String status = row.cleared() ? "✓" : row.unlocked() ? "" : "LOCK";
            var button = new BattleHudButton(hardX, y, Math.min(250, panelWidth / 3), 25,
                    Component.literal(status + " " + row.label()), row.cleared() ? GREEN : row.unlocked() ? DANGER : MUTED,
                    ignored -> start(row));
            button.active = row.unlocked(); addRenderableWidget(button); y += 30;
        }
        int gridLeft = left + Math.min(290, panelWidth / 3 + 38), gridTop = top + 112;
        int available = left + panelWidth - 18 - gridLeft;
        int cols = available >= 500 ? 6 : available >= 360 ? 5 : 4;
        int cellW = Math.max(54, (available - (cols - 1) * 5) / cols), index = 0;
        for (var row : rows.stream().filter(r -> "RIFT".equals(r.kind())).toList()) {
            int xx = gridLeft + (index % cols) * (cellW + 5), yy = gridTop + (index / cols) * 31;
            String text = (row.cleared() ? "✓ " : "") + "F" + row.id().substring(row.id().length() - 2) + " Lv" + row.level();
            var button = new BattleHudButton(xx, yy, cellW, 25, Component.literal(text),
                    row.cleared() ? GREEN : row.hardPattern() ? GOLD : row.unlocked() ? BLUE : MUTED, ignored -> start(row));
            button.active = row.unlocked(); addRenderableWidget(button); index++;
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

    private void openCharacter(String id) { selectedCharacterId = id; detailTab = DetailTab.STATUS; page = 0; clearWidgets(); init(); }
    private void closeCharacter() { selectedCharacterId = ""; page = 0; clearWidgets(); init(); }
    private void switchDetail(DetailTab value) { detailTab = value; clearWidgets(); init(); }
    private void cycleOwnership() { ownershipFilter = OwnershipFilter.values()[(ownershipFilter.ordinal()+1)%OwnershipFilter.values().length]; page=0; clearWidgets(); init(); }
    private void cycleStar() { starFilter = (starFilter + 1) % 7; page=0; clearWidgets(); init(); }
    private void cycleLevel() { minimumLevel = minimumLevel == 0 ? 10 : minimumLevel >= 60 ? 0 : minimumLevel + 10; page=0; clearWidgets(); init(); }
    private void cycleRole() { roleFilter = RoleFilter.values()[(roleFilter.ordinal()+1)%RoleFilter.values().length]; page=0; clearWidgets(); init(); }
    private String ownershipLabel() { return switch(ownershipFilter){case ALL->"ALL";case OWNED->"보유";case UNOWNED->"미보유";}; }

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
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + 4, top + panelHeight, BLUE);
        graphics.text(font, Component.literal("TURNBOUND"), left + 18, top + 15, TEXT, true);
        var snapshot = ClientMetaState.snapshot();
        String resources = "Gold " + snapshot.gold() + "    Crystal " + snapshot.crystal() + "    Essence " + snapshot.essence()
                + "    Core " + snapshot.core() + "    Party CP " + snapshot.partyCp();
        graphics.text(font, Component.literal(resources), left + 18, top + 35, SECONDARY, false);
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
        graphics.text(font, Component.literal("4 Slot · 참가 100% XP · 미편성 보유 캐릭터 20% XP · 중복 편성 불가"), left + 160, top + 88, SECONDARY, false);
        int x=left+18, y=top+panelHeight-86;
        for(int i=0;i<3;i++){
            List<String> preset=snapshot.partyPresets().size()>i?snapshot.partyPresets().get(i):List.of();
            String summary=preset.isEmpty()?"비어 있음":String.join(" / ",preset);
            graphics.text(font, Component.literal("P"+(i+1)+"  "+summary), x+i*150, y, preset.isEmpty()?MUTED:SECONDARY, false);
        }
    }

    private void drawCharacters(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        if (selectedCharacterId.isBlank()) {
            long owned=snapshot.characters().stream().filter(ClientMetaState.CharacterRow::owned).count();
            graphics.text(font, Component.literal("전체 12 · 보유 " + owned + " · Star / Level / Role filter"), left+480, top+108, SECONDARY, false);
            return;
        }
        var row=character(selectedCharacterId);
        if(row==null)return;
        int x=left+18,y=top+145;
        graphics.text(font, Component.literal(row.name()+"  "+(row.owned()?(row.awakened()?"◆6":"★"+row.star())+" Lv."+row.level():"미보유 · 태생 ★"+row.nativeStar())), x,y,row.owned()?TEXT:MUTED,true);
        graphics.text(font, Component.literal(row.role()+" · "+row.difficulty()),x,y+16,SECONDARY,false);
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
        graphics.text(font,Component.literal("CP  "+row.cp()+"    태생 ★"+row.nativeStar()+"    현재 "+(row.awakened()?"◆6":"★"+row.star())),x,y+20,TEXT,false);
        if(!row.owned()) graphics.text(font,Component.literal("미보유 캐릭터의 수치는 Lv.1 태생 기준."),x,y+42,MUTED,false);
    }

    private void drawCharacterSkills(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        var definition=CanonicalData.definition(row.id(),Math.max(1,row.level()),row.star(),row.awakened());
        int yy=y;
        for(var skill:definition.skills()){
            String kind=skill.id().equals(definition.basicSkillId())?"Basic":"Active";
            graphics.text(font,Component.literal(kind+" · "+skill.name()+"  CD "+skill.cooldown()),x,yy,GOLD,false);
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
            String text=item==null?slot+"  ·  비어 있음":slot+"  ·  "+item.name()+" +"+item.enhancement();
            graphics.text(font,Component.literal(text),x,yy,item==null?MUTED:tierColor(item.tier()),false);yy+=21;
        }
        graphics.text(font,Component.literal("장착 변경은 Equipment 탭에서 수행."),x,yy+10,SECONDARY,false);
    }

    private void drawCharacterAwakening(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        var menu=CharacterMenuCatalog.profile(row.id());
        if(!row.owned()){graphics.text(font,Component.literal("캐릭터를 획득해야 성장할 수 있습니다."),x,y,MUTED,false);return;}
        graphics.text(font,Component.literal("현재 성급  "+(row.awakened()?"◆6":"★"+row.star())+"   /   Lv."+row.level()),x,y,TEXT,false);
        graphics.text(font,Component.literal("Awakening Package"),x,y+28,GOLD,true);
        graphics.text(font,Component.literal(shorten(menu.awakening(),115)),x+12,y+45,row.awakened()?GREEN:MUTED,false);
        if(!row.awakened()) graphics.text(font,Component.literal("🔒 Lv.60 / ★6 / Signature Trial / Awakening Core 조건을 서버가 검증"),x,y+70,MUTED,false);
    }

    private void drawCharacterProfile(GuiGraphicsExtractor graphics, ClientMetaState.CharacterRow row,int x,int y){
        var menu=CharacterMenuCatalog.profile(row.id());
        if(!row.profileUnlocked()){
            graphics.text(font,Component.literal("🔒 Character Quest 완료 후 Profile 해금"),x,y,MUTED,true);return;
        }
        graphics.text(font,Component.literal("Role  ·  "+menu.role()),x,y,TEXT,false);
        graphics.text(font,Component.literal("Weapon  ·  "+menu.weapon()),x,y+20,SECONDARY,false);
        graphics.text(font,Component.literal("채용 이유  ·  "+shorten(menu.reason(),100)),x,y+42,TEXT,false);
        graphics.text(font,Component.literal("성격  ·  "+shorten(menu.personality(),100)),x,y+64,SECONDARY,false);
    }

    private void drawEquipment(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        if(equipView==EquipView.MARKET){
            graphics.text(font,Component.literal("Market Row · T1/T2 확정 구매 · 강화 실패/파괴 없음"),left+600,top+108,SECONDARY,false);return;
        }
        graphics.text(font,Component.literal("Inventory  "+snapshot.equipment().size()+" / 300"),left+480,top+108,SECONDARY,false);
        var selected=equipment(selectedEquipmentId);
        if(selected==null){graphics.text(font,Component.literal("장비를 선택하면 Compare / +20 Preview / 강화 / 장착을 표시합니다."),left+500,top+150,MUTED,false);return;}
        int x=left+Math.min(480,panelWidth/2+18),y=top+150;
        graphics.text(font,Component.literal(selected.tier()+"  "+selected.name()+"  +"+selected.enhancement()),x,y,tierColor(selected.tier()),true);
        graphics.text(font,Component.literal(selected.mainType()+"  "+stat(selected.mainValue())+"    "+selected.subType()+"  "+stat(selected.subValue())),x,y+20,TEXT,false);
        graphics.text(font,Component.literal("+20 Preview  ·  "+selected.mainType()+" "+stat(selected.mainAt20())+" / "+selected.subType()+" "+stat(selected.subAt20())),x,y+40,GOLD,false);
        graphics.text(font,Component.literal("현재 장착  ·  "+(selected.equippedCharacterId().isBlank()?"없음":selected.equippedCharacterId())),x,y+62,SECONDARY,false);
        if(!equipmentTargetCharacterId.isBlank()){
            var current=snapshot.equipment().stream().filter(e->e.equippedCharacterId().equals(equipmentTargetCharacterId)&&e.slot().equals(selected.slot())).findFirst().orElse(null);
            String compare=current==null?"비교: 해당 슬롯 비어 있음":"비교: "+current.name()+" +"+current.enhancement()+"  →  "+selected.name()+" +"+selected.enhancement();
            graphics.text(font,Component.literal(compare),x,y+82,current==null?GREEN:BLUE,false);
        }
        String saleInfo=selected.tier().equals("SIGNATURE")?"판매 · 전용 장비는 정본 판매가 없음"
                : selected.equippedCharacterId().isBlank()?"판매 · "+selected.salePrice()+" Gold":"판매 · "+selected.salePrice()+" Gold · 장착 해제 필요";
        graphics.text(font,Component.literal(saleInfo),x,y+105,selected.sellable()?GOLD:MUTED,false);
    }

    private void drawArchive(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot s) {
        graphics.text(font, Component.literal("★5 Pity  " + s.fiveStarPity() + " / " + GachaCatalog.HARD_PITY + "    보유 Crystal " + s.crystal()), left + 500, top + 108, GOLD, true);
        graphics.text(font, Component.literal("Rate  ★5 3% · ★4 12% · ★3 35% · ★2 30% · ★1 20%"), left + 500, top + 124, SECONDARY, false);
        graphics.text(font, Component.literal("Soft Pity: 65부터 매회 +3%p · Hard Pity: 80 · 10회 최소 ★4"), left + 500, top + 140, SECONDARY, false);
        int perPage=12,start=page*perPage,end=Math.min(s.archiveHistory().size(),start+perPage),y=top+174;
        graphics.text(font,Component.literal("최근 획득 기록 "+s.archiveHistory().size()+" / 50"),left+18,y-18,TEXT,true);
        for(int i=start;i<end;i++){
            var row=s.archiveHistory().get(i);
            String text="★"+row.nativeStars()+"  "+row.name()+(row.newlyOwned()?"  NEW":"  → Essence +"+row.essenceGranted())+"  · Pity "+row.pityAfter();
            graphics.text(font,Component.literal(text),left+18,y,row.newlyOwned()?GREEN:SECONDARY,false);y+=18;
        }
    }

    private void drawQuests(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        int y=top+112;
        graphics.text(font,Component.literal("Main / Character / Region / Challenge · 동시에 최대 3 Track"),left+18,y,SECONDARY,false);
        y+=24;
        for(var q:snapshot.regionQuests()){
            graphics.text(font,Component.literal((q.completed()?"✓ ":"○ ")+q.id()+" · "+q.region()),left+18,y,q.completed()?GREEN:TEXT,false);y+=16;
            if(y>top+panelHeight-90)break;
        }
        int x=left+panelWidth/2+8,yy=top+136;
        for(var c:snapshot.challenges()){
            graphics.text(font,Component.literal((c.completed()?"✓ ":"○ ")+c.ordinal()+". "+c.label()),x,yy,c.completed()?GREEN:c.autoEvaluable()?TEXT:GOLD,false);yy+=16;
            if(yy>top+panelHeight-60)break;
        }
    }

    private void drawCodex(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        List<ClientMetaState.CodexRow> rows=snapshot.codex().stream().filter(row->row.category().equals(codexCategory)).toList();
        int perPage=18,start=page*perPage,end=Math.min(rows.size(),start+perPage),cols=panelWidth>=760?3:2;
        int gap=8,cardW=(panelWidth-36-gap*(cols-1))/cols,gridTop=top+140;
        if(codexCategory.equals("TUTORIAL")&&rows.isEmpty()){
            graphics.text(font,Component.literal("튜토리얼 도감 카테고리는 정본에 있으나 v0.4에는 개별 Entry ID/내용이 정의되지 않음."),left+18,gridTop,MUTED,false);return;
        }
        for(int i=start;i<end;i++){
            var row=rows.get(i);int local=i-start,x=left+18+(local%cols)*(cardW+gap),y=gridTop+(local/cols)*52;
            boolean silhouette=(row.category().equals("ENEMIES")||row.category().equals("BOSSES"))&&!row.discovered();
            String name=silhouette?"████  ???  ████":row.name();
            graphics.fill(x,y,x+cardW,y+42,silhouette?0xEE08090C:0xB0181D26);
            graphics.text(font,Component.literal(name),x+10,y+8,silhouette?MUTED:row.discovered()?TEXT:MUTED,false);
            String detail=silhouette?"미발견":row.detailUnlocked()?row.summary():row.discovered()?"상세 정보 잠김":"미보유";
            graphics.text(font,Component.literal(detail),x+10,y+24,row.detailUnlocked()?SECONDARY:MUTED,false);
        }
    }

    private void drawSystem(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        graphics.text(font,Component.literal(snapshot.riftUnlocked()?"Rift Gate 개방":"B05 클리어 후 Rift Gate 개방"),left+300,top+88,snapshot.riftUnlocked()?GREEN:MUTED,false);
        graphics.text(font,Component.literal("Hard/Rift 임시 관리면 · 최종 필드에서는 라디아 시설과 연결"),left+18,top+panelHeight-24,SECONDARY,false);
    }

    private ClientMetaState.CharacterRow character(String id){return ClientMetaState.snapshot().characters().stream().filter(row->row.id().equals(id)).findFirst().orElse(null);}
    private ClientMetaState.EquipmentRow equipment(String id){return ClientMetaState.snapshot().equipment().stream().filter(row->row.instanceId().equals(id)).findFirst().orElse(null);}

    private static int tierRank(String tier){return switch(tier){case "SIGNATURE"->5;case "T4"->4;case "T3"->3;case "T2"->2;case "T1"->1;default->0;};}
    private static int tierColor(String tier){return switch(tier){case "SIGNATURE"->0xFFC794FF;case "T4"->0xFFFFC857;case "T3"->0xFFB68CFF;case "T2"->0xFF6DC6FF;default->0xFFAEB7C6;};}
    private static String stat(double value){double abs=Math.abs(value);if(abs<=1.0)return String.format(Locale.ROOT,"%.1f%%",value*100.0);return String.format(Locale.ROOT,"%.1f",value);}
    private static String shorten(String value,int max){return value.length()<=max?value:value.substring(0,max-1)+"…";}
    private static String detailLabel(DetailTab tab){return switch(tab){case STATUS->"Status";case SKILLS->"Skills";case EQUIPMENT->"Equipment";case AWAKENING->"Awakening";case PROFILE->"Profile";};}
    private static String label(Tab tab){return switch(tab){case PARTY->"PARTY";case CHARACTERS->"CHARACTERS";case EQUIPMENT->"EQUIPMENT";case ARCHIVE->"ARCHIVE";case QUESTS->"QUESTS";case CODEX->"CODEX";case SYSTEM->"SYSTEM";};}
    private static String title(Tab tab){return switch(tab){case PARTY->"파티 편성";case CHARACTERS->"Characters";case EQUIPMENT->"Equipment / Market Row";case ARCHIVE->"Echo Archive";case QUESTS->"Quests";case CODEX->"Codex";case SYSTEM->"System / Endgame";};}
}
