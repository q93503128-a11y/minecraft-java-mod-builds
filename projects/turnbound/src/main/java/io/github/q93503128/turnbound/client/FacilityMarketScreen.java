package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Physical equipment merchant with inspect-before-commit buying and selling. */
final class FacilityMarketScreen extends FacilityScreen {
    private enum Mode { BUY, SELL }

    private Mode mode = Mode.BUY;
    private int page;
    private String selectedShopId = "";
    private String selectedSellId = "";

    FacilityMarketScreen() {
        super("장비 상점", 0xFFFFC857, 540, 278, 360, 220);
    }

    @Override
    protected String subtitle() {
        return "보유 골드 " + gold(ClientMetaState.snapshot().gold()) + " · 장비를 선택하면 상세 성능을 확인할 수 있습니다.";
    }

    @Override
    protected void buildFacility() {
        int tabY = bodyTop() + 3;
        int tabW = Math.max(72, (panelWidth - 30) / 4);
        addRenderableWidget(new BattleHudButton(
                left + 11, tabY, tabW, 18, Component.literal("구매"),
                mode == Mode.BUY ? TurnboundUiTokens.ACCENT : TurnboundUiTokens.TEXT_MUTED,
                ignored -> switchMode(Mode.BUY)));
        addRenderableWidget(new BattleHudButton(
                left + 15 + tabW, tabY, tabW, 18, Component.literal("판매"),
                mode == Mode.SELL ? TurnboundUiTokens.SUCCESS : TurnboundUiTokens.TEXT_MUTED,
                ignored -> switchMode(Mode.SELL)));

        int listW = Math.min(235, Math.max(150, panelWidth * 43 / 100));
        int listX = left + 10;
        int listTop = bodyTop() + 27;
        int listBottom = bodyBottom() - 22;
        int rowH = 18;
        int per = Math.max(3, (listBottom - listTop) / 20);

        if (mode == Mode.BUY) {
            List<ClientMetaState.ShopRow> rows = ClientMetaState.snapshot().shopItems();
            if (!selectedShopId.isBlank() && rows.stream().noneMatch(row -> row.itemId().equals(selectedShopId))) {
                selectedShopId = "";
            }
            if (selectedShopId.isBlank() && !rows.isEmpty()) selectedShopId = rows.getFirst().itemId();
            page = clampPage(page, rows.size(), per);
            int start = page * per;
            int end = Math.min(rows.size(), start + per);
            for (int i = start; i < end; i++) {
                var row = rows.get(i);
                int yy = listTop + (i - start) * 20;
                String label = tierLabel(row.tier()) + " · " + row.name() + " · " + gold(row.price());
                addRenderableWidget(new BattleHudButton(
                        listX + 4, yy, listW - 8, rowH, Component.literal(label),
                        row.itemId().equals(selectedShopId) ? TurnboundUiTokens.PRIMARY
                                : row.unlocked() ? TurnboundUiTokens.ACCENT : TurnboundUiTokens.TEXT_MUTED,
                        ignored -> selectShop(row.itemId())));
            }

            ClientMetaState.ShopRow selected = selectedShop();
            if (selected != null) {
                int actionX = listX + listW + 10;
                int actionW = left + panelWidth - 10 - actionX;
                boolean affordable = ClientMetaState.snapshot().gold() >= selected.price();
                var buy = new BattleHudButton(
                        actionX + 4, bodyBottom() - 24, Math.max(80, actionW - 8), 19,
                        Component.literal(selected.unlocked()
                                ? "구매 · " + gold(selected.price())
                                : "아직 구매할 수 없음"),
                        selected.unlocked() && affordable ? TurnboundUiTokens.ACCENT : TurnboundUiTokens.TEXT_MUTED,
                        ignored -> send("BUY|" + selected.itemId()));
                buy.active = selected.unlocked() && affordable;
                addRenderableWidget(buy);
            }
            buildPager(rows.size(), per, listX, listW);
        } else {
            List<ClientMetaState.EquipmentRow> rows = sellRows();
            if (!selectedSellId.isBlank() && rows.stream().noneMatch(row -> row.instanceId().equals(selectedSellId))) {
                selectedSellId = "";
            }
            if (selectedSellId.isBlank() && !rows.isEmpty()) selectedSellId = rows.getFirst().instanceId();
            page = clampPage(page, rows.size(), per);
            int start = page * per;
            int end = Math.min(rows.size(), start + per);
            for (int i = start; i < end; i++) {
                var row = rows.get(i);
                int yy = listTop + (i - start) * 20;
                String label = tierLabel(row.tier()) + " · " + row.name() + " +" + row.enhancement();
                addRenderableWidget(new BattleHudButton(
                        listX + 4, yy, listW - 8, rowH, Component.literal(label),
                        row.instanceId().equals(selectedSellId) ? TurnboundUiTokens.PRIMARY : TurnboundUiTokens.TEXT_SECONDARY,
                        ignored -> selectSell(row.instanceId())));
            }

            ClientMetaState.EquipmentRow selected = selectedSell();
            if (selected != null) {
                int actionX = listX + listW + 10;
                int actionW = left + panelWidth - 10 - actionX;
                addRenderableWidget(new BattleHudButton(
                        actionX + 4, bodyBottom() - 24, Math.max(80, actionW - 8), 19,
                        Component.literal("판매 · +" + gold(selected.salePrice())),
                        TurnboundUiTokens.SUCCESS,
                        ignored -> send("SELL|" + selected.instanceId())));
            }
            buildPager(rows.size(), per, listX, listW);
        }
    }

    private void buildPager(int total, int per, int listX, int listW) {
        int pages = Math.max(1, (total + per - 1) / per);
        if (pages <= 1) return;
        int y = bodyBottom() - 18;
        if (page > 0) {
            addRenderableWidget(new BattleHudButton(
                    listX + 4, y, 36, 15, Component.literal("<"), TurnboundUiTokens.TEXT_MUTED,
                    ignored -> { page--; refreshSnapshot(); }));
        }
        if (page + 1 < pages) {
            addRenderableWidget(new BattleHudButton(
                    listX + listW - 40, y, 36, 15, Component.literal(">"), TurnboundUiTokens.TEXT_MUTED,
                    ignored -> { page++; refreshSnapshot(); }));
        }
    }

    @Override
    protected void renderFacility(@NotNull GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int listW = Math.min(235, Math.max(150, panelWidth * 43 / 100));
        int listX = left + 10;
        int detailX = listX + listW + 10;
        int detailW = left + panelWidth - 10 - detailX;
        int y = bodyTop() + 27;
        int h = bodyBottom() - y;

        inset(g, listX, y - 3, listW, h + 3);
        inset(g, detailX, y - 3, detailW, h + 3);
        g.text(font, Component.literal(mode == Mode.BUY ? "구매 목록" : "보유 장비"),
                listX + 7, y + 5, TurnboundUiTokens.TEXT_SECONDARY, true);

        if (mode == Mode.BUY) drawBuyDetail(g, detailX, y, detailW);
        else drawSellDetail(g, detailX, y, detailW);
    }

    private void drawBuyDetail(GuiGraphicsExtractor g, int x, int y, int w) {
        ClientMetaState.ShopRow row = selectedShop();
        if (row == null) {
            g.text(font, Component.literal("구매할 장비가 없습니다."), x + 8, y + 8, TurnboundUiTokens.TEXT_SECONDARY, false);
            return;
        }
        V04Catalogs.EquipmentSpec spec = V04Catalogs.equipment(row.itemId());
        int cy = y + 7;
        g.text(font, Component.literal(UiTextLayout.fit(row.name(), w - 16)), x + 8, cy, TurnboundUiTokens.TEXT_PRIMARY, true);
        cy += 16;
        g.text(font, Component.literal(tierLabel(row.tier()) + " · " + slotLabel(row.slot())),
                x + 8, cy, TurnboundUiTokens.ACCENT, false);
        cy += 16;
        g.text(font, Component.literal(UiTextLayout.fit("주 능력치 · " + statText(spec.main().type(), spec.main().value()), w - 16)),
                x + 8, cy, TurnboundUiTokens.SUCCESS, false);
        cy += 15;
        g.text(font, Component.literal(UiTextLayout.fit("보조 능력치 · " + statText(spec.sub().type(), spec.sub().value()), w - 16)),
                x + 8, cy, TurnboundUiTokens.TEXT_SECONDARY, false);
        cy += 15;
        g.text(font, Component.literal(UiTextLayout.fit("고유 효과 · " + effectText(spec.fixedEffect()), w - 16)),
                x + 8, cy, TurnboundUiTokens.TEXT_SECONDARY, false);
        cy += 18;
        String price = "가격 " + gold(row.price()) + " · 보유 " + gold(ClientMetaState.snapshot().gold());
        g.text(font, Component.literal(UiTextLayout.fit(price, w - 16)), x + 8, cy,
                ClientMetaState.snapshot().gold() >= row.price() ? TurnboundUiTokens.ACCENT : TurnboundUiTokens.DANGER, true);
    }

    private void drawSellDetail(GuiGraphicsExtractor g, int x, int y, int w) {
        ClientMetaState.EquipmentRow row = selectedSell();
        if (row == null) {
            g.text(font, Component.literal("판매할 수 있는 장비가 없습니다."), x + 8, y + 8, TurnboundUiTokens.TEXT_SECONDARY, false);
            return;
        }
        V04Catalogs.EquipmentSpec spec = V04Catalogs.equipment(row.itemId());
        int cy = y + 7;
        g.text(font, Component.literal(UiTextLayout.fit(row.name() + " +" + row.enhancement(), w - 16)),
                x + 8, cy, TurnboundUiTokens.TEXT_PRIMARY, true);
        cy += 16;
        g.text(font, Component.literal(tierLabel(row.tier()) + " · " + slotLabel(row.slot())),
                x + 8, cy, TurnboundUiTokens.SUCCESS, false);
        cy += 16;
        g.text(font, Component.literal(UiTextLayout.fit("주 능력치 · " + statText(row.mainType(), row.mainValue()), w - 16)),
                x + 8, cy, TurnboundUiTokens.TEXT_PRIMARY, false);
        cy += 15;
        g.text(font, Component.literal(UiTextLayout.fit("보조 능력치 · " + statText(row.subType(), row.subValue()), w - 16)),
                x + 8, cy, TurnboundUiTokens.TEXT_SECONDARY, false);
        cy += 15;
        g.text(font, Component.literal(UiTextLayout.fit("고유 효과 · " + effectText(spec.fixedEffect()), w - 16)),
                x + 8, cy, TurnboundUiTokens.TEXT_SECONDARY, false);
        cy += 18;
        g.text(font, Component.literal("판매 금액 · +" + gold(row.salePrice())),
                x + 8, cy, TurnboundUiTokens.SUCCESS, true);
    }

    private void switchMode(Mode next) {
        if (mode == next) return;
        mode = next;
        page = 0;
        refreshSnapshot();
    }

    private void selectShop(String itemId) {
        selectedShopId = itemId == null ? "" : itemId;
        refreshSnapshot();
    }

    private void selectSell(String instanceId) {
        selectedSellId = instanceId == null ? "" : instanceId;
        refreshSnapshot();
    }

    private ClientMetaState.ShopRow selectedShop() {
        return ClientMetaState.snapshot().shopItems().stream()
                .filter(row -> row.itemId().equals(selectedShopId)).findFirst().orElse(null);
    }

    private ClientMetaState.EquipmentRow selectedSell() {
        return ClientMetaState.snapshot().equipment().stream()
                .filter(ClientMetaState.EquipmentRow::sellable)
                .filter(row -> row.instanceId().equals(selectedSellId)).findFirst().orElse(null);
    }

    private static List<ClientMetaState.EquipmentRow> sellRows() {
        return ClientMetaState.snapshot().equipment().stream()
                .filter(ClientMetaState.EquipmentRow::sellable)
                .sorted(Comparator.comparingInt(ClientMetaState.EquipmentRow::salePrice).reversed()
                        .thenComparing(ClientMetaState.EquipmentRow::name))
                .toList();
    }

    private static int clampPage(int page, int total, int per) {
        int pages = Math.max(1, (total + per - 1) / per);
        return Math.max(0, Math.min(page, pages - 1));
    }

    private static String gold(long amount) {
        return String.format(Locale.ROOT, "%,dG", amount);
    }

    private static String tierLabel(String tier) {
        return switch (tier) {
            case "T1" -> "일반";
            case "T2" -> "희귀";
            case "T3" -> "영웅";
            case "T4" -> "유물";
            default -> tier;
        };
    }

    private static String slotLabel(String slot) {
        return switch (slot) {
            case "WEAPON" -> "무기";
            case "ARMOR" -> "방어구";
            case "ACCESSORY" -> "장신구";
            default -> slot;
        };
    }

    private static String statText(String type, double value) {
        return switch (type) {
            case "ATK_PCT" -> "공격력 +" + Math.round(value * 100.0D) + "%";
            case "HP_PCT" -> "최대 생명력 +" + Math.round(value * 100.0D) + "%";
            case "DEF_PCT" -> "방어력 +" + Math.round(value * 100.0D) + "%";
            case "SPD_FLAT" -> "속도 +" + Math.round(value);
            default -> type + " " + value;
        };
    }

    private static String effectText(String effect) {
        if (effect == null || effect.isBlank()) return "없음";
        return switch (effect) {
            case "START_GAUGE_30" -> "전투 시작 시 행동 게이지 +30";
            case "START_GAUGE_50" -> "전투 시작 시 행동 게이지 +50";
            case "START_GAUGE_80" -> "전투 시작 시 행동 게이지 +80";
            case "SINGLE_DIRECT_DAMAGE_3" -> "단일 대상 직접 피해 +3%";
            case "HEAL_DONE_4" -> "회복량 +4%";
            case "HEAL_DONE_5" -> "회복량 +5%";
            case "EXECUTE_DIRECT_DAMAGE_8" -> "처치권 대상 직접 피해 +8%";
            case "DIRECT_HIT_GAUGE_20" -> "직접 피격 후 행동 게이지 +20";
            case "HEAL_RECEIVED_6" -> "받는 회복량 +6%";
            case "BARRIER_RECEIVED_10" -> "받는 보호막 +10%";
            case "REVIVE_HP_PLUS_10P" -> "부활 시 생명력 +10%p";
            case "REACTION_DAMAGE_6" -> "반응 피해 +6%";
            case "ALLY_GAUGE_GRANT_PLUS_20" -> "아군 행동 게이지 부여량 +20";
            case "HIGH_HP_DR_5" -> "높은 생명력 구간에서 받는 피해 -5%";
            default -> "특수 효과";
        };
    }

    private static void send(String raw) {
        ClientPacketDistributor.sendToServer(new MetaCommandPayload(raw));
    }
}
