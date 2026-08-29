package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

final class BattleScreenLayout {
    static final int SKILL_COUNT = 5;
    static final int ALLY_TARGET_COUNT = 4;
    static final int ENEMY_TARGET_COUNT = 5;

    private BattleScreenLayout() {
    }

    record Rect(int x, int y, int width, int height) {
        Rect {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Battle UI rectangles must have positive size");
            }
        }

        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }
    }

    record Layout(
            int screenWidth,
            int screenHeight,
            List<Rect> skillButtons,
            List<Rect> targetButtons,
            Rect autoButton,
            Rect speedButton,
            Rect fleeButton,
            Rect leftPanel,
            Rect rightPanel,
            Rect topInset,
            Rect bottomInset,
            int hintY,
            int messageY
    ) {
        Layout {
            skillButtons = List.copyOf(skillButtons);
            targetButtons = List.copyOf(targetButtons);
        }
    }

    static Layout calculate(int requestedWidth, int requestedHeight) {
        int screenWidth = Math.max(1, requestedWidth);
        int screenHeight = Math.max(1, requestedHeight);
        int margin = Math.max(2, Math.min(8, screenWidth / 40));
        int gap = 4;

        int controlHeight = Math.min(32, Math.max(20, screenHeight / 7));
        int controlWidth = Math.max(1, Math.min(66, (screenWidth - 2 * margin - 2 * gap) / 3));
        int controlY = Math.max(0, Math.min(12, screenHeight - controlHeight));
        int controlGroupWidth = controlWidth * 3 + gap * 2;
        int controlStartX = Math.max(0, screenWidth - margin - controlGroupWidth);
        Rect autoButton = rectInside(screenWidth, screenHeight, controlStartX, controlY, controlWidth, controlHeight);
        Rect speedButton = rectInside(screenWidth, screenHeight, controlStartX + controlWidth + gap, controlY, controlWidth, controlHeight);
        Rect fleeButton = rectInside(screenWidth, screenHeight, controlStartX + (controlWidth + gap) * 2, controlY, controlWidth, controlHeight);

        int skillHeight = Math.min(42, Math.max(24, screenHeight / 6));
        int skillY = Math.max(0, screenHeight - margin - skillHeight);
        int skillAvailableWidth = Math.max(SKILL_COUNT, screenWidth - 2 * margin - gap * (SKILL_COUNT - 1));
        int skillWidth = Math.max(1, Math.min(150, skillAvailableWidth / SKILL_COUNT));
        int skillGroupWidth = skillWidth * SKILL_COUNT + gap * (SKILL_COUNT - 1);
        int skillStartX = Math.max(0, (screenWidth - skillGroupWidth) / 2);
        List<Rect> skills = new ArrayList<>(SKILL_COUNT);
        for (int i = 0; i < SKILL_COUNT; i++) {
            skills.add(rectInside(
                    screenWidth,
                    screenHeight,
                    skillStartX + i * (skillWidth + gap),
                    skillY,
                    skillWidth,
                    skillHeight
            ));
        }

        int targetTop = Math.max(
                controlY + controlHeight + 6,
                Math.min(58, Math.max(42, screenHeight / 5))
        );
        targetTop = Math.min(targetTop, Math.max(0, skillY - 1));
        int targetBottomLimit = Math.max(targetTop + 1, skillY - 8);
        targetBottomLimit = Math.min(screenHeight, targetBottomLimit);
        int targetAvailableHeight = Math.max(1, targetBottomLimit - targetTop);

        int singleColumnGap = 8;
        int singleColumnNeededHeight = ENEMY_TARGET_COUNT * 34 + (ENEMY_TARGET_COUNT - 1) * singleColumnGap;
        int columns = targetAvailableHeight >= singleColumnNeededHeight && screenWidth >= 520 ? 1 : 2;
        int rows = (ENEMY_TARGET_COUNT + columns - 1) / columns;
        int targetGap = columns == 1 ? singleColumnGap : 4;
        int targetHeight = Math.max(
                1,
                Math.min(42, (targetAvailableHeight - targetGap * (rows - 1)) / rows)
        );

        int centerGap = Math.max(56, Math.min(180, screenWidth / 4));
        int leftStart = margin;
        int leftEnd = Math.max(leftStart + 1, screenWidth / 2 - centerGap / 2 - 2);
        int rightStart = Math.min(screenWidth - margin - 1, screenWidth / 2 + (centerGap + 1) / 2 + 2);
        int rightEnd = Math.max(rightStart + 1, screenWidth - margin);
        int sideWidth = Math.max(1, Math.min(leftEnd - leftStart, rightEnd - rightStart));
        int targetWidth = Math.max(
                1,
                Math.min(138, (sideWidth - targetGap * (columns - 1)) / columns)
        );

        List<Rect> targets = new ArrayList<>(ALLY_TARGET_COUNT + ENEMY_TARGET_COUNT);
        for (int i = 0; i < ALLY_TARGET_COUNT; i++) {
            int row = i / columns;
            int column = i % columns;
            targets.add(rectInside(
                    screenWidth,
                    screenHeight,
                    leftStart + column * (targetWidth + targetGap),
                    targetTop + row * (targetHeight + targetGap),
                    targetWidth,
                    targetHeight
            ));
        }

        int rightGridWidth = columns * targetWidth + (columns - 1) * targetGap;
        int rightGridStart = Math.max(0, rightEnd - rightGridWidth);
        for (int i = 0; i < ENEMY_TARGET_COUNT; i++) {
            int row = i / columns;
            int column = i % columns;
            targets.add(rectInside(
                    screenWidth,
                    screenHeight,
                    rightGridStart + column * (targetWidth + targetGap),
                    targetTop + row * (targetHeight + targetGap),
                    targetWidth,
                    targetHeight
            ));
        }

        int targetRows = (ENEMY_TARGET_COUNT + columns - 1) / columns;
        int targetGridBottom = Math.min(
                screenHeight,
                targetTop + targetRows * targetHeight + Math.max(0, targetRows - 1) * targetGap
        );
        int panelY = Math.max(0, targetTop - 6);
        int panelHeight = Math.max(1, Math.min(screenHeight - panelY, targetGridBottom - panelY + 6));
        int leftGridWidth = columns * targetWidth + (columns - 1) * targetGap;
        Rect leftPanel = rectInside(
                screenWidth,
                screenHeight,
                Math.max(0, leftStart - 6),
                panelY,
                Math.min(screenWidth, leftGridWidth + 12),
                panelHeight
        );
        Rect rightPanel = rectInside(
                screenWidth,
                screenHeight,
                Math.max(0, rightGridStart - 6),
                panelY,
                Math.min(screenWidth, rightGridWidth + 12),
                panelHeight
        );

        int topInsetWidth = Math.max(1, Math.min(460, screenWidth - 2 * margin));
        int topInsetHeight = Math.max(1, Math.min(42, screenHeight));
        Rect topInset = rectInside(
                screenWidth,
                screenHeight,
                Math.max(0, (screenWidth - topInsetWidth) / 2),
                Math.min(8, Math.max(0, screenHeight - topInsetHeight)),
                topInsetWidth,
                topInsetHeight
        );

        int bottomInsetY = Math.max(0, skillY - 10);
        int bottomInsetWidth = Math.max(1, Math.min(780, screenWidth - 2 * margin));
        Rect bottomInset = rectInside(
                screenWidth,
                screenHeight,
                Math.max(0, (screenWidth - bottomInsetWidth) / 2),
                bottomInsetY,
                bottomInsetWidth,
                Math.max(1, screenHeight - margin - bottomInsetY)
        );

        int hintY = Math.max(0, Math.min(targetTop - 10, screenHeight - 1));
        int messageY = Math.max(0, Math.min(skillY - 18, screenHeight - 1));
        return new Layout(
                screenWidth,
                screenHeight,
                skills,
                targets,
                autoButton,
                speedButton,
                fleeButton,
                leftPanel,
                rightPanel,
                topInset,
                bottomInset,
                hintY,
                messageY
        );
    }

    private static Rect rectInside(int screenWidth, int screenHeight, int x, int y, int width, int height) {
        int safeX = Math.max(0, Math.min(x, screenWidth - 1));
        int safeY = Math.max(0, Math.min(y, screenHeight - 1));
        int safeWidth = Math.max(1, Math.min(width, screenWidth - safeX));
        int safeHeight = Math.max(1, Math.min(height, screenHeight - safeY));
        return new Rect(safeX, safeY, safeWidth, safeHeight);
    }
}
