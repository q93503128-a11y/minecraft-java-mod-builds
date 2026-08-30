package io.github.q93503128.turnbound.client;

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
import java.util.List;

/** v0.4 world-backed RPG management menu. Server state remains authoritative. */
public final class MetaMenuScreen extends Screen {
    public enum Tab { PARTY, CHARACTERS, EQUIPMENT, ARCHIVE, QUESTS, CODEX, SYSTEM }

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

    public MetaMenuScreen(Tab tab) {
        super(Component.literal("TURNBOUND"));
        this.tab = tab == null ? Tab.PARTY : tab;
        draftParty.addAll(ClientMetaState.snapshot().activeParty());
    }

    public Tab tab() { return tab; }

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
            case EQUIPMENT -> buildEquipment();
            case ARCHIVE -> buildArchive();
            case SYSTEM -> buildEndgame();
            case CHARACTERS, QUESTS, CODEX -> { }
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
        int contentTop = top + 108;
        int gap = 8;
        int cardW = (panelWidth - 54 - gap) / 2;
        int cardH = 30;
        for (int i = 0; i < snapshot.characters().size(); i++) {
            var row = snapshot.characters().get(i);
            int column = i % 2, line = i / 2;
            int x = left + 18 + column * (cardW + gap), y = contentTop + line * (cardH + 6);
            boolean selected = draftParty.contains(row.id());
            String star = row.awakened() ? "◆6" : "★" + row.star();
            String text = (selected ? "● " : "○ ") + star + "  " + row.name() + "  Lv." + row.level() + "  CP " + row.cp();
            addRenderableWidget(new BattleHudButton(x, y, cardW, cardH, Component.literal(text), selected ? GREEN : MUTED,
                    ignored -> toggleParty(row.id())));
        }
        int y = top + panelHeight - 42;
        addRenderableWidget(new BattleHudButton(left + panelWidth - 140, y, 122, 24,
                Component.literal("편성 저장  " + draftParty.size() + "/4"), GREEN, ignored -> saveParty()));
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

    private void buildEquipment() {
        var rows = ClientMetaState.snapshot().shopItems();
        int perPage = 10;
        int start = page * perPage;
        int end = Math.min(rows.size(), start + perPage);
        int y = top + 112;
        for (int i = start; i < end; i++) {
            var row = rows.get(i);
            String text = row.tier() + "  " + row.name() + "  · " + row.slot() + "  · " + row.price() + "G";
            var button = new BattleHudButton(left + 18, y, Math.min(panelWidth - 36, 520), 27,
                    Component.literal(text), row.unlocked() ? GOLD : MUTED, ignored -> buy(row));
            button.active = row.unlocked() && ClientMetaState.snapshot().gold() >= row.price();
            addRenderableWidget(button);
            y += 32;
        }
        buildPager(rows.size(), perPage);
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
            int x = gridLeft + (index % cols) * (cellW + 5), yy = gridTop + (index / cols) * 31;
            String text = (row.cleared() ? "✓ " : "") + "F" + row.id().substring(row.id().length() - 2) + " Lv" + row.level();
            var button = new BattleHudButton(x, yy, cellW, 25, Component.literal(text),
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

    private void switchTab(Tab value) {
        if (value == tab) return;
        tab = value; page = 0;
        clearWidgets(); init();
    }

    private void movePage(int delta) { page = Math.max(0, page + delta); clearWidgets(); init(); }
    private void toggleParty(String id) {
        if (draftParty.contains(id)) { if (draftParty.size() > 1) draftParty.remove(id); }
        else if (draftParty.size() < 4) draftParty.add(id);
        clearWidgets(); init();
    }
    private void saveParty() { send("PARTY|" + String.join(",", draftParty)); }
    private void buy(ClientMetaState.ShopRow row) { if (row.unlocked()) send("BUY|" + row.itemId()); }
    private void start(ClientMetaState.EndgameRow row) { if (row.unlocked()) send("START|" + row.id()); }
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
            case PARTY -> drawPartyInfo(graphics);
            case CHARACTERS -> drawCharacters(graphics, snapshot);
            case EQUIPMENT -> drawEquipment(graphics, snapshot);
            case ARCHIVE -> drawArchive(graphics, snapshot);
            case QUESTS -> drawQuests(graphics, snapshot);
            case CODEX -> drawCodex(graphics, snapshot);
            case SYSTEM -> drawSystem(graphics, snapshot);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPartyInfo(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.literal("4 Slot · 참가 100% XP · 미편성 보유 캐릭터 20% XP · 중복 편성 불가"), left + 160, top + 88, SECONDARY, false);
        graphics.text(font, Component.literal("Preset 3개는 다음 저장 상태 확장에서 연결 예정"), left + 18, top + panelHeight - 22, GOLD, false);
    }

    private void drawCharacters(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        int y = top + 112;
        graphics.text(font, Component.literal("보유 캐릭터 " + snapshot.characters().size() + " · 전체 도감/필터/상세 5탭 연결 진행 중"), left + 18, y, SECONDARY, false);
        y += 20;
        for (var row : snapshot.characters()) {
            graphics.text(font, Component.literal((row.awakened()?"◆6":"★"+row.star()) + "  " + row.name() + "  Lv." + row.level() + "  CP " + row.cp()), left + 18, y, TEXT, false);
            y += 15;
        }
    }

    private void drawEquipment(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        graphics.text(font, Component.literal("Market Row · T1/T2 확정 구매 · 강화 실패/파괴 없음"), left + 570, top + 112, SECONDARY, false);
        graphics.text(font, Component.literal("인벤토리 Equip/Compare/Sort/Sell/+20 Preview는 다음 장비 화면 묶음에서 연결"), left + 570, top + 128, GOLD, false);
    }

    private void drawArchive(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot s) {
        graphics.text(font, Component.literal("★5 Pity  " + s.fiveStarPity() + " / " + GachaCatalog.HARD_PITY + "    보유 Crystal " + s.crystal()), left + 500, top + 108, GOLD, true);
        graphics.text(font, Component.literal("Rate  ★5 3% · ★4 12% · ★3 35% · ★2 30% · ★1 20%"), left + 500, top + 124, SECONDARY, false);
        graphics.text(font, Component.literal("Soft Pity: 65부터 매회 +3%p · Hard Pity: 80 · 10회 최소 ★4"), left + 500, top + 140, SECONDARY, false);
        int perPage=12, start=page*perPage, end=Math.min(s.archiveHistory().size(), start+perPage), y=top+174;
        graphics.text(font, Component.literal("최근 획득 기록 " + s.archiveHistory().size() + " / 50"), left+18, y-18, TEXT, true);
        for(int i=start;i<end;i++){
            var row=s.archiveHistory().get(i);
            String text="★"+row.nativeStars()+"  "+row.name()+"  "+(row.newlyOwned()?"NEW":"중복 +"+row.essenceGranted()+" Essence")+"  · Pity "+row.pityAfter();
            graphics.text(font, Component.literal(text), left+18, y, row.newlyOwned()?GREEN:TEXT, false); y+=17;
        }
    }

    private void drawQuests(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        int y=top+112;
        graphics.text(font, Component.literal("Main / Character / Region / Challenge · 동시 Track 최대 3"), left+18, y, SECONDARY, false); y+=20;
        int complete=(int)snapshot.challenges().stream().filter(ClientMetaState.ChallengeRow::completed).count();
        graphics.text(font, Component.literal("Challenge  "+complete+" / "+snapshot.challenges().size()+"    · Region Quest "+snapshot.regionQuests().size()+""), left+18, y, TEXT, false); y+=20;
        for(var c:snapshot.challenges().stream().limit(12).toList()){
            graphics.text(font, Component.literal((c.completed()?"✓ ":c.autoEvaluable()?"○ ":"◇ ")+c.ordinal()+". "+c.label()), left+18, y, c.completed()?GREEN:c.autoEvaluable()?TEXT:GOLD, false); y+=15;
        }
    }

    private void drawCodex(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        int y=top+112;
        graphics.text(font, Component.literal("Characters · Enemies · Bosses · Equipment · Tutorial"), left+18, y, TEXT, true); y+=22;
        graphics.text(font, Component.literal("발견 상태/미발견 적 실루엣 판정은 필드 Encounter 발견 플래그와 연결 예정"), left+18, y, GOLD, false);
    }

    private void drawSystem(GuiGraphicsExtractor graphics, ClientMetaState.Snapshot snapshot) {
        graphics.text(font, Component.literal(snapshot.riftUnlocked()?"Rift Gate 개방":"B05 클리어 후 Rift Gate 개방"), left+18, top+88, snapshot.riftUnlocked()?GREEN:MUTED, false);
        graphics.text(font, Component.literal("현재 Hard/Rift 선택은 시설 UI 완성 전 임시 접근면이며 라디아에서만 서버가 허용"), left+18, top+panelHeight-22, SECONDARY, false);
    }

    private static String label(Tab tab) { return switch (tab) {
        case PARTY -> "PARTY"; case CHARACTERS -> "CHAR"; case EQUIPMENT -> "EQUIP"; case ARCHIVE -> "ARCHIVE";
        case QUESTS -> "QUESTS"; case CODEX -> "CODEX"; case SYSTEM -> "SYSTEM"; };
    }
    private static String title(Tab tab) { return switch (tab) {
        case PARTY -> "파티 편성"; case CHARACTERS -> "캐릭터"; case EQUIPMENT -> "장비 / Market Row"; case ARCHIVE -> "Archive";
        case QUESTS -> "퀘스트"; case CODEX -> "도감"; case SYSTEM -> "시스템 / Endgame Access"; };
    }
}
