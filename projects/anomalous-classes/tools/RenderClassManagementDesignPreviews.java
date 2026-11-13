import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Creates bilingual, offline-only UI composition previews for classes without server UI data. */
public final class RenderClassManagementDesignPreviews {
    private static final int W = 1600;
    private static final int H = 900;
    private static final Path REVIEW = Path.of("ui", "reviews", "2026-11-13", "class-management-design-review");
    private final Path assets;
    private final Path output;
    private final BufferedImage surface;
    private final BufferedImage titlePlate;
    private final BufferedImage buttons;
    private final Map<String, BufferedImage> corners = new LinkedHashMap<>();
    private final Map<String, BufferedImage> art = new LinkedHashMap<>();
    private final Map<String, BufferedImage> icons = new LinkedHashMap<>();
    private final Map<String, BufferedImage> rails = new LinkedHashMap<>();
    private final Font regular;
    private final Font medium;
    private final Font bold;

    private RenderClassManagementDesignPreviews(Path assets, Path output) throws IOException {
        this.assets = assets;
        this.output = output;
        Path menu = assets.resolve("menu_v1");
        Path parts = menu.resolve("frame_parts");
        surface = read(menu.resolve("surface_tile.png"));
        titlePlate = read(menu.resolve("title_plate.png"));
        buttons = read(menu.resolve("button_states.png"));
        for (String name : new String[]{"corner_tl", "corner_tr", "corner_bl", "corner_br"}) {
            corners.put(name, read(parts.resolve(name + ".png")));
        }
        for (String name : new String[]{"rail_top_left", "rail_top_right", "rail_bottom_left", "rail_bottom_right",
                "rail_side_left", "rail_side_right", "jewel_top", "jewel_bottom", "jewel_side_left", "jewel_side_right"}) {
            rails.put(name, read(parts.resolve(name + ".png")));
        }
        Path root = assets.getParent();
        Path gui = root.resolve("gui");
        for (String key : new String[]{"puppeteer", "parasite_morpher", "barrier_caster", "pact_caster", "swarm_caster"}) {
            art.put(key, read(gui.resolve("class_cards_v3").resolve(key + ".png")));
            icons.put(key, read(gui.resolve("class_icons_v2").resolve(key + ".png")));
        }
        String family = chooseKoreanFont();
        regular = new Font(family, Font.PLAIN, 20);
        medium = new Font(family, Font.PLAIN, 24);
        bold = new Font(family, Font.BOLD, 30);
    }

    public static void main(String[] args) throws Exception {
        Path assets = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Files.createDirectories(output);
        RenderClassManagementDesignPreviews renderer = new RenderClassManagementDesignPreviews(assets, output);
        renderer.renderAll();
    }

    private void renderAll() throws IOException {
        Map<String, String[]> classes = new LinkedHashMap<>();
        classes.put("puppeteer", new String[]{"인형술사", "Puppeteer"});
        classes.put("parasite_morpher", new String[]{"기생술사", "Parasite Morph"});
        classes.put("barrier_caster", new String[]{"결계술사", "Barrier Caster"});
        classes.put("pact_caster", new String[]{"계약술사", "Pact Caster"});
        classes.put("swarm_caster", new String[]{"군체술사", "Swarm Caster"});
        for (var entry : classes.entrySet()) {
            render(entry.getKey(), entry.getValue()[0], true);
            render(entry.getKey(), entry.getValue()[1], false);
        }
        renderContactSheet(classes);
    }

    private void render(String key, String className, boolean korean) throws IOException {
        BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        quality(g);
        drawBackground(g, key);
        drawOuterFrame(g);
        drawTitle(g, korean ? "직업 관리" : "Class Management");
        drawButton(g, 58, 43, 140, 43, korean ? "프로필" : "Profile");
        drawButton(g, 207, 43, 140, 43, korean ? "매개체" : "Focus");
        drawClassIdentity(g, key, className, korean);
        drawReviewBadge(g, korean ? "디자인 검토용 · 인게임 화면 아님" : "DESIGN REVIEW · NOT IN GAME");

        switch (key) {
            case "puppeteer" -> drawPuppeteer(g, korean);
            case "parasite_morpher" -> drawParasite(g, korean);
            case "barrier_caster" -> drawBarrier(g, korean);
            case "pact_caster" -> drawPact(g, korean);
            case "swarm_caster" -> drawSwarm(g, korean);
            default -> throw new IllegalArgumentException(key);
        }
        drawButton(g, 1382, 778, 148, 36, korean ? "직업 변경" : "Change Class");
        g.dispose();
        String suffix = korean ? "ko" : "en";
        Path target = output.resolve(key + "_" + suffix + ".jpg");
        BufferedImage rgb = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D out = rgb.createGraphics();
        out.drawImage(canvas, 0, 0, null);
        out.dispose();
        ImageIO.write(rgb, "jpg", target.toFile());
    }

    private void drawBackground(Graphics2D g, String key) {
        BufferedImage image = art.get(key);
        drawCover(g, image, 0, 0, W, H);
        g.setComposite(AlphaComposite.SrcOver.derive(0.32f));
        g.setColor(new Color(12, 7, 20));
        g.fillRect(0, 0, W, H);
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new GradientPaint(0, 0, new Color(18, 8, 27, 38), W, H, new Color(7, 7, 14, 82)));
        g.fillRect(0, 0, W, H);
    }

    private void drawOuterFrame(Graphics2D g) {
        int x = 30, y = 26, width = 1540, height = 850, corner = 68, rail = 13;
        tile(g, surface, x + corner, y + rail, width - corner * 2, height - rail * 2, 104);
        g.setColor(new Color(19, 7, 27, 114));
        g.fillRect(x + corner, y + rail, width - corner * 2, height - rail * 2);
        drawHorizontalRail(g, rails.get("rail_top_left"), x + corner - 1, y + 14, W / 2 - 42, rail, 12);
        drawHorizontalRail(g, rails.get("rail_top_right"), W / 2 + 42, y + 14, x + width - corner + 1, rail, 12);
        drawHorizontalRail(g, rails.get("rail_bottom_left"), x + corner - 1, y + height - corner + 14, W / 2 - 42, rail, 12);
        drawHorizontalRail(g, rails.get("rail_bottom_right"), W / 2 + 42, y + height - corner + 14, x + width - corner + 1, rail, 12);
        drawVerticalRail(g, rails.get("rail_side_left"), x + 14, y + corner - 1, y + height - corner + 1, rail, 12);
        drawVerticalRail(g, rails.get("rail_side_right"), x + width - corner + 14, y + corner - 1, y + height - corner + 1, rail, 12);
        g.drawImage(corners.get("corner_tl"), x, y, x + corner, y + corner, 0, 0, 160, 160, null);
        g.drawImage(corners.get("corner_tr"), x + width - corner, y, x + width, y + corner, 0, 0, 160, 160, null);
        g.drawImage(corners.get("corner_bl"), x, y + height - corner, x + corner, y + height, 0, 0, 160, 160, null);
        g.drawImage(corners.get("corner_br"), x + width - corner, y + height - corner, x + width, y + height, 0, 0, 160, 160, null);
        drawSprite(g, rails.get("jewel_side_left"), x + 7, H / 2 - 34, 19, 68);
        drawSprite(g, rails.get("jewel_side_right"), x + width - 26, H / 2 - 34, 19, 68);
        drawSprite(g, rails.get("jewel_top"), W / 2 - 21, y - 7, 42, 62);
        drawSprite(g, rails.get("jewel_bottom"), W / 2 - 21, y + height - 52, 42, 62);
    }

    private void drawTitle(Graphics2D g, String text) {
        int x = 522, y = 10, width = 556, height = 62, cap = 24, jewel = 20;
        g.drawImage(titlePlate, x, y, x + cap, y + height, 0, 0, 132, 342, null);
        g.drawImage(titlePlate, x + cap, y, x + width / 2 - jewel / 2, y + height, 132, 0, 456, 342, null);
        g.drawImage(titlePlate, W / 2 - jewel / 2, y, W / 2 + jewel / 2, y + height, 456, 0, 568, 342, null);
        g.drawImage(titlePlate, W / 2 + jewel / 2, y, x + width - cap, y + height, 568, 0, 892, 342, null);
        g.drawImage(titlePlate, x + width - cap, y, x + width, y + height, 892, 0, 1024, 342, null);
        centered(g, text, W / 2, y + 20, bold, new Color(255, 241, 215), width - 84);
    }

    private void drawClassIdentity(Graphics2D g, String key, String name, boolean korean) {
        int x = 66, y = 104;
        g.drawImage(icons.get(key), x, y, 48, 48, null);
        text(g, name, x + 62, y + 33, bold, new Color(246, 232, 207));
        text(g, korean ? "관리" : "MANAGEMENT", x + 63, y + 56, regular, new Color(197, 171, 209));
    }

    private void drawReviewBadge(Graphics2D g, String text) {
        int x = 1132, y = 106, w = 404, h = 35;
        g.setColor(new Color(40, 20, 31, 220));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 16, 16));
        g.setColor(new Color(218, 164, 97, 220));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(x, y, w, h, 16, 16));
        centered(g, text, x + w / 2, y + 8, regular, new Color(255, 219, 166), w - 22);
    }

    private void drawPuppeteer(Graphics2D g, boolean ko) {
        drawArtPanel(g, 70, 170, 488, 274, "puppeteer");
        panel(g, 70, 464, 488, 303, "puppeteer");
        panelHeading(g, 98, 486, ko ? "보유 인형" : "Puppet Roster", "puppeteer");
        emptyState(g, 112, 557, 400, ko ? "목록 데이터 영역" : "Roster data area",
                ko ? "실제 보유 인형이 표시되는 자리" : "Owned puppets appear here");
        panel(g, 586, 170, 944, 597, "puppeteer");
        panelHeading(g, 616, 195, ko ? "선택 인형" : "Selected Puppet", "puppeteer");
        drawField(g, 620, 250, 265, ko ? "전개 상태" : "Deployment", "—");
        drawField(g, 900, 250, 265, ko ? "체력" : "Health", "—");
        drawField(g, 1180, 250, 315, ko ? "손상" : "Condition", "—");
        line(g, 616, 318, 1496, 318, accent("puppeteer"));
        panelHeading(g, 616, 344, ko ? "부품 구성" : "Assembly", "puppeteer");
        String[] parts = ko ? new String[]{"골격", "행동 장치", "구동핵"} : new String[]{"Frame", "Action Unit", "Core"};
        for (int i = 0; i < 3; i++) drawSlot(g, 620 + i * 290, 405, 255, 126, parts[i]);
        panelHeading(g, 616, 574, ko ? "정비 정보" : "Maintenance", "puppeteer");
        drawField(g, 620, 626, 410, ko ? "필요 재료" : "Required materials", "—");
        drawField(g, 1050, 626, 430, ko ? "제작대 상태" : "Workbench", "—");
    }

    private void drawParasite(Graphics2D g, boolean ko) {
        drawArtPanel(g, 70, 170, 500, 282, "parasite_morpher");
        panel(g, 70, 472, 500, 295, "parasite_morpher");
        panelHeading(g, 98, 494, ko ? "생체 기록" : "Biological Record", "parasite_morpher");
        drawField(g, 104, 554, 195, ko ? "생체 데이터" : "BD", "—");
        drawField(g, 317, 554, 222, ko ? "현재 숙주" : "Current Host", "—");
        panel(g, 598, 170, 932, 597, "parasite_morpher");
        panelHeading(g, 628, 195, ko ? "숙주 상태" : "Host Status", "parasite_morpher");
        drawField(g, 632, 250, 255, ko ? "종류" : "Species", "—");
        drawField(g, 902, 250, 255, ko ? "상태" : "State", "—");
        drawField(g, 1172, 250, 324, ko ? "종별 숙련" : "Species Mastery", "—");
        line(g, 628, 318, 1496, 318, accent("parasite_morpher"));
        panelHeading(g, 628, 344, ko ? "영구 계승" : "Inherited Traits", "parasite_morpher");
        emptyState(g, 650, 398, 826, ko ? "계승 능력 목록" : "Inherited abilities",
                ko ? "직접 발동 · 상시 · 반응형 분류" : "Direct · passive · reactive groups");
        panelHeading(g, 628, 570, ko ? "기술 배치" : "Ability Layout", "parasite_morpher");
        String[] keys = ko ? new String[]{"V", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}
                : new String[]{"V", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        for (int i = 0; i < keys.length; i++) drawKey(g, 638 + i * 76, 621, keys[i], i == 0 ? "MODE" : "—", "parasite_morpher");
    }

    private void drawBarrier(Graphics2D g, boolean ko) {
        panel(g, 70, 170, 535, 597, "barrier_caster");
        panelHeading(g, 100, 194, ko ? "활성 결계" : "Active Barriers", "barrier_caster");
        emptyState(g, 117, 266, 440, ko ? "설치된 결계 목록" : "Installed barriers",
                ko ? "실제 전개 상태가 여기에 표시됩니다" : "Live barrier states appear here");
        panel(g, 631, 170, 899, 278, "barrier_caster");
        panelHeading(g, 660, 194, ko ? "현재 편집" : "Current Edit", "barrier_caster");
        drawField(g, 665, 252, 248, ko ? "종류" : "Type", "—");
        drawField(g, 930, 252, 248, ko ? "범위" : "Range", "—");
        drawField(g, 1195, 252, 290, ko ? "배치" : "Placement", "—");
        panel(g, 631, 466, 899, 301, "barrier_caster");
        panelHeading(g, 660, 490, ko ? "최근 성공 설정" : "Last Successful Setup", "barrier_caster");
        drawField(g, 665, 550, 248, ko ? "종류" : "Type", "—");
        drawField(g, 930, 550, 248, ko ? "범위" : "Range", "—");
        drawField(g, 1195, 550, 290, ko ? "내구" : "Durability", "—");
        line(g, 665, 617, 1485, 617, accent("barrier_caster"));
        emptyState(g, 690, 648, 770, ko ? "현재 편집값과 최근 성공값은 별도로 유지" : "Edit and successful values stay separate",
                ko ? "두 설정을 혼동하지 않도록 구분" : "Kept visually distinct");
    }

    private void drawPact(Graphics2D g, boolean ko) {
        panel(g, 70, 170, 445, 597, "pact_caster");
        panelHeading(g, 100, 194, ko ? "체결한 계약" : "Active Contracts", "pact_caster");
        emptyState(g, 112, 266, 362, ko ? "계약 목록" : "Contract list",
                ko ? "체결된 계약만 표시" : "Only signed contracts appear");
        panel(g, 541, 170, 989, 597, "pact_caster");
        parchment(g, 574, 195, 922, 535, "pact_caster");
        panelHeading(g, 614, 225, ko ? "계약서" : "Contract Record", "pact_caster");
        String[] labels = ko
                ? new String[]{"상대 · 위계", "가호", "권능", "고유 패시브", "금기 · 의무", "관계 · 채무", "심화 조건"}
                : new String[]{"Entity · Rank", "Favor", "Authority", "Unique Passive", "Taboo · Duty", "Bond · Debt", "Deepening"};
        for (int i = 0; i < labels.length; i++) {
            int y = 292 + i * 54;
            text(g, labels[i], 628, y, medium, new Color(243, 224, 185));
            text(g, "—", 1080, y, medium, new Color(216, 198, 177));
            line(g, 628, y + 33, 1438, y + 33, new Color(126, 83, 100, 105));
        }
    }

    private void drawSwarm(Graphics2D g, boolean ko) {
        panel(g, 70, 170, 1460, 174, "swarm_caster");
        panelHeading(g, 100, 194, ko ? "군체 현황" : "Colony Status", "swarm_caster");
        String[] roles = ko ? new String[]{"현재 군체", "최대 군체", "공격", "엄호", "운반", "재결집"}
                : new String[]{"Current", "Maximum", "Attack", "Guard", "Carry", "Regroup"};
        for (int i = 0; i < roles.length; i++) drawField(g, 104 + i * 232, 246, 208, roles[i], "—");
        panel(g, 70, 364, 650, 403, "swarm_caster");
        panelHeading(g, 100, 389, ko ? "전술 비율 편집" : "Tactic Ratio", "swarm_caster");
        text(g, ko ? "공격" : "Attack", 108, 468, medium, new Color(242, 224, 193));
        ratioTrack(g, 205, 462, 450, "swarm_caster");
        text(g, ko ? "엄호" : "Guard", 108, 550, medium, new Color(242, 224, 193));
        ratioTrack(g, 205, 544, 450, "swarm_caster");
        emptyState(g, 110, 635, 565, ko ? "공격·엄호 비율을 직접 설정" : "Set the attack and guard ratio yourself",
                ko ? "기본 전술은 제공하지 않음" : "No built-in tactics");
        panel(g, 748, 364, 782, 403, "swarm_caster");
        panelHeading(g, 778, 389, ko ? "저장 전술" : "Saved Tactics", "swarm_caster");
        emptyState(g, 800, 475, 674, ko ? "저장한 전술이 없습니다" : "No saved tactics",
                ko ? "전술은 플레이어가 직접 만듭니다" : "Tactics are created by the player");
        drawButton(g, 790, 670, 210, 48, ko ? "전술 만들기" : "Create Tactic");
    }

    private void drawArtPanel(Graphics2D g, int x, int y, int w, int h, String key) {
        panel(g, x, y, w, h, key);
        int inset = 10;
        drawCover(g, art.get(key), x + inset, y + inset, w - inset * 2, h - inset * 2);
        g.setColor(new Color(12, 7, 15, 28));
        g.fillRect(x + inset, y + inset, w - inset * 2, h - inset * 2);
        g.setColor(new Color(225, 191, 139, 210));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(x + inset, y + inset, w - inset * 2, h - inset * 2);
    }

    private void panel(Graphics2D g, int x, int y, int w, int h, String key) {
        tile(g, surface, x, y, w, h, 88);
        g.setColor(new Color(15, 8, 22, 104));
        g.fillRect(x, y, w, h);
        Color accent = accent(key);
        g.setStroke(new BasicStroke(1.35f));
        g.setColor(new Color(193, 151, 102, 155));
        g.draw(new RoundRectangle2D.Float(x, y, w, h, 15, 15));
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g.drawLine(x + 15, y + 3, x + Math.min(w - 15, 130), y + 3);
        g.drawLine(x + 3, y + 15, x + 3, y + Math.min(h - 15, 76));
    }

    private void panelHeading(Graphics2D g, int x, int y, String label, String key) {
        g.setColor(new Color(240, 211, 160));
        g.fill(new RoundRectangle2D.Float(x, y + 8, 5, 22, 4, 4));
        text(g, label, x + 18, y, medium, new Color(246, 229, 202));
        Color accent = accent(key);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
        g.fillRect(x + 18, y + 35, 530, 1);
    }

    private void drawField(Graphics2D g, int x, int y, int w, String label, String value) {
        g.setColor(new Color(9, 6, 16, 100));
        g.fill(new RoundRectangle2D.Float(x, y, w, 68, 10, 10));
        g.setColor(new Color(183, 146, 114, 100));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(x, y, w, 68, 10, 10));
        text(g, label, x + 15, y + 11, regular, new Color(199, 182, 200));
        text(g, value, x + 15, y + 36, medium, new Color(241, 223, 197));
    }

    private void drawSlot(Graphics2D g, int x, int y, int w, int h, String label) {
        g.setColor(new Color(9, 5, 15, 104));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        g.setColor(new Color(205, 168, 122, 135));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        centered(g, label, x + w / 2, y + 18, regular, new Color(229, 208, 177), w - 16);
        centered(g, "—", x + w / 2, y + 69, bold, new Color(183, 150, 185), w - 20);
    }

    private void drawKey(Graphics2D g, int x, int y, String key, String value, String style) {
        int w = 66, h = 78;
        g.setColor(new Color(11, 7, 18, 140));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 10, 10));
        g.setColor(new Color(accent(style).getRed(), accent(style).getGreen(), accent(style).getBlue(), 155));
        g.draw(new RoundRectangle2D.Float(x, y, w, h, 10, 10));
        centered(g, key, x + w / 2, y + 8, medium, new Color(250, 228, 195), w - 8);
        centered(g, value, x + w / 2, y + 47, regular, new Color(195, 177, 197), w - 8);
    }

    private void ratioTrack(Graphics2D g, int x, int y, int w, String key) {
        g.setColor(new Color(8, 5, 15, 170));
        g.fill(new RoundRectangle2D.Float(x, y, w, 27, 13, 13));
        g.setColor(new Color(accent(key).getRed(), accent(key).getGreen(), accent(key).getBlue(), 170));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(x, y, w, 27, 13, 13));
    }

    private void emptyState(Graphics2D g, int x, int y, int w, String title, String subtitle) {
        centered(g, "◇", x + w / 2, y, bold, new Color(217, 176, 124), 48);
        centered(g, title, x + w / 2, y + 34, medium, new Color(238, 220, 193), w - 24);
        centered(g, subtitle, x + w / 2, y + 68, regular, new Color(185, 167, 190), w - 28);
    }

    private void parchment(Graphics2D g, int x, int y, int w, int h, String key) {
        g.setPaint(new GradientPaint(x, y, new Color(85, 47, 55, 230), x + w, y + h, new Color(37, 18, 34, 225)));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 18, 18));
        g.setColor(new Color(224, 180, 119, 185));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(new RoundRectangle2D.Float(x, y, w, h, 18, 18));
        g.setColor(new Color(accent(key).getRed(), accent(key).getGreen(), accent(key).getBlue(), 110));
        g.drawRect(x + 9, y + 9, w - 18, h - 18);
    }

    private void drawButton(Graphics2D g, int x, int y, int w, int h, String label) {
        int artOffsetY = Math.round((128 - 165) * h / 256.0f);
        int drawY = y + artOffsetY;
        int cap = Math.min(h, w / 2);
        g.drawImage(buttons, x, drawY, x + cap, drawY + h, 0, 0, 256, 256, null);
        g.drawImage(buttons, x + cap, drawY, x + w - cap, drawY + h, 256, 0, 768, 256, null);
        g.drawImage(buttons, x + w - cap, drawY, x + w, drawY + h, 768, 0, 1024, 256, null);
        Font font = w < 160 ? regular : medium;
        int textTop = drawY + Math.max(0, (h - g.getFontMetrics(font).getHeight()) / 2);
        centered(g, label, x + w / 2, textTop, font, new Color(246, 231, 207), w - 18);
    }

    private void drawHorizontalRail(Graphics2D g, BufferedImage image, int x, int y, int endX, int height, int sampleX) {
        if (endX <= x) return;
        g.drawImage(image, x, y, endX, y + height, sampleX, 0, sampleX + 1, image.getHeight(), null);
    }

    private void drawVerticalRail(Graphics2D g, BufferedImage image, int x, int y, int endY, int width, int sampleY) {
        if (endY <= y) return;
        g.drawImage(image, x, y, x + width, endY, 0, sampleY, image.getWidth(), sampleY + 1, null);
    }

    private void drawSprite(Graphics2D g, BufferedImage image, int x, int y, int w, int h) {
        g.drawImage(image, x, y, x + w, y + h, 0, 0, image.getWidth(), image.getHeight(), null);
    }

    private void drawCover(Graphics2D g, BufferedImage image, int x, int y, int w, int h) {
        double scale = Math.max(w / (double) image.getWidth(), h / (double) image.getHeight());
        int sw = (int) Math.round(w / scale);
        int sh = (int) Math.round(h / scale);
        int sx = (image.getWidth() - sw) / 2;
        int sy = (image.getHeight() - sh) / 2;
        g.drawImage(image, x, y, x + w, y + h, sx, sy, sx + sw, sy + sh, null);
    }

    private void tile(Graphics2D g, BufferedImage image, int x, int y, int w, int h, int tileSize) {
        for (int py = y; py < y + h; py += tileSize) {
            for (int px = x; px < x + w; px += tileSize) {
                int dw = Math.min(tileSize, x + w - px);
                int dh = Math.min(tileSize, y + h - py);
                g.drawImage(image, px, py, px + dw, py + dh, 0, 0,
                        Math.max(1, dw * image.getWidth() / tileSize),
                        Math.max(1, dh * image.getHeight() / tileSize), null);
            }
        }
    }

    private void line(Graphics2D g, int x1, int y1, int x2, int y2, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(x1, y1, x2, y2);
    }

    private void text(Graphics2D g, String value, int x, int y, Font font, Color color) {
        g.setFont(font);
        g.setColor(color);
        g.drawString(value, x, y + g.getFontMetrics().getAscent());
    }

    private void centered(Graphics2D g, String value, int centerX, int y, Font font, Color color, int maxWidth) {
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String fitted = value;
        while (fm.stringWidth(fitted) > maxWidth && fitted.length() > 1) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }
        int x = centerX - fm.stringWidth(fitted) / 2;
        g.setColor(color);
        g.drawString(fitted, x, y + fm.getAscent());
    }

    private void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static Color accent(String key) {
        return switch (key) {
            case "puppeteer" -> new Color(218, 157, 97);
            case "parasite_morpher" -> new Color(131, 181, 108);
            case "barrier_caster" -> new Color(105, 178, 210);
            case "pact_caster" -> new Color(204, 170, 104);
            case "swarm_caster" -> new Color(193, 194, 105);
            default -> new Color(198, 154, 233);
        };
    }

    private static BufferedImage read(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) throw new IOException("Could not read image: " + path);
        return image;
    }

    private static String chooseKoreanFont() {
        for (String family : new String[]{"Malgun Gothic", "Noto Sans CJK KR", "Gulim", "Dialog"}) {
            if (java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames().length > 0) {
                return family;
            }
        }
        return "Dialog";
    }

    private void renderContactSheet(Map<String, String[]> classes) throws IOException {
        int cw = 520, ch = 293, gap = 20;
        BufferedImage sheet = new BufferedImage(3 * cw + 4 * gap, 2 * ch + 3 * gap + 44,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        quality(g);
        g.setColor(new Color(18, 11, 26));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        text(g, "CLASS MANAGEMENT · DESIGN REVIEW / NOT IN GAME", gap, 10, medium, new Color(242, 221, 190));
        int index = 0;
        for (String key : classes.keySet()) {
            BufferedImage preview = ImageIO.read(output.resolve(key + "_ko.jpg").toFile());
            int col = index % 3;
            int row = index / 3;
            int x = gap + col * (cw + gap);
            int y = gap + 35 + row * (ch + gap);
            g.drawImage(preview, x, y, cw, ch, null);
            g.setColor(new Color(226, 189, 136));
            g.setStroke(new BasicStroke(1.4f));
            g.drawRect(x, y, cw, ch);
            index++;
        }
        g.dispose();
        ImageIO.write(sheet, "jpg", output.resolve("class_management_review_contact_sheet.jpg").toFile());
    }
}
