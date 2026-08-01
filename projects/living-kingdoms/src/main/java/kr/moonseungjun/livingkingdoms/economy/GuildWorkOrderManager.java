package kr.moonseungjun.livingkingdoms.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Institution-based production contracts. Residents earn abstract silver by delivering real inputs
 * to a guild specialist; there is no magical money from repeatedly clicking or vanilla recipe grid.
 */
public final class GuildWorkOrderManager {
    private GuildWorkOrderManager() {
    }

    public static boolean isGuildRepresentative(String npcId) {
        return contractFor(npcId) != null;
    }

    public static void explain(ServerPlayer player, String npcId) {
        Contract contract = contractFor(npcId);
        if (contract == null) return;
        RealmEconomySavedData.Account account = RealmEconomyManager.account(player);
        if ("unregistered".equals(account.profession())) {
            player.sendSystemMessage(Component.literal(
                    "§8[조합 안내] §f웅크린 채 다시 대화하면 §e" + contract.displayProfession()
                            + "§f으로 등록합니다. 직업은 일감·임금·평판에 영향을 줍니다."
            ));
        } else if (account.profession().equals(contract.profession())) {
            player.sendSystemMessage(Component.literal(
                    "§8[오늘의 작업] §f" + contract.requirementText()
                            + "을 준비해 웅크린 채 제출하십시오. 하루에 한 번 정산됩니다."
            ));
        }
    }

    public static void interact(ServerPlayer player, String npcId) {
        Contract contract = contractFor(npcId);
        if (contract == null) return;
        RealmEconomySavedData.Account account = RealmEconomyManager.account(player);

        if ("unregistered".equals(account.profession())) {
            RealmEconomyManager.setProfession(player, contract.profession());
            RealmEconomyManager.credit(player, 6L, 1);
            player.sendSystemMessage(Component.literal(
                    "§6[조합 등록] §f" + contract.displayProfession()
                            + "으로 등록되었습니다. 등록 지원금 §e은화 6§f을 받았습니다."
            ));
            return;
        }

        if (!account.profession().equals(contract.profession())) {
            player.sendSystemMessage(Component.literal(
                    "§c[조합 관할] §f현재 등록 직업은 §e" + account.profession()
                            + "§f입니다. 직업 전환은 행정청에서 처리해야 합니다."
            ));
            return;
        }

        if (!hasAll(player, contract.inputs())) {
            player.sendSystemMessage(Component.literal(
                    "§c[작업 재료 부족] §f필요: " + contract.requirementText()
            ));
            return;
        }
        if (!RealmEconomyManager.beginDailyContract(player)) {
            player.sendSystemMessage(Component.literal(
                    "§7[작업 정산] 오늘 몫은 이미 정산했습니다. 다음 날 다시 방문하십시오."
            ));
            return;
        }

        consumeAll(player, contract.inputs());
        long wage = RealmEconomyManager.price(player, contract.baseWage(),
                RealmEconomySavedData.MarketCategory.LABOR);
        RealmEconomyManager.credit(player, wage, contract.renown());
        player.sendSystemMessage(Component.literal(
                "§a[작업 완료] §f" + contract.taskName() + "을 마쳤습니다. §e은화 "
                        + wage + "§f · 명망 " + contract.renown() + "을 받았습니다."
        ));
    }

    private static Contract contractFor(String npcId) {
        if (npcId == null) return null;
        if (containsAny(npcId, "smith", "forge", "quartermaster", "bowyer")) {
            return new Contract("smith", "금속·제작 조합원", "도구용 소재 검수",
                    14L, 2, List.of(new Ingredient(Items.IRON_INGOT, 1, "철괴"),
                    new Ingredient(Items.COAL, 2, "석탄")));
        }
        if (containsAny(npcId, "herbalist", "apothecary", "brewer")) {
            return new Contract("herbalist", "약초·치유 조합원", "치유 약재 선별",
                    12L, 2, List.of(new Ingredient(Items.DANDELION, 3, "민들레"),
                    new Ingredient(Items.BROWN_MUSHROOM, 2, "갈색 버섯")));
        }
        if (containsAny(npcId, "fisher", "river", "waterkeeper")) {
            return new Contract("fisher", "수운·어업 조합원", "수산물 납품",
                    11L, 1, List.of(new Ingredient(Items.COD, 3, "대구")));
        }
        if (containsAny(npcId, "warden", "gatekeeper", "sergeant", "guard")) {
            return new Contract("warden", "수비대 계약자", "성외 위협 증표 제출",
                    15L, 2, List.of(new Ingredient(Items.BONE, 3, "뼈"),
                    new Ingredient(Items.STRING, 2, "실")));
        }
        if (containsAny(npcId, "guide", "archivist", "lorekeeper", "clerk", "scholar")) {
            return new Contract("scholar", "기록원 수습생", "장부 필사",
                    12L, 2, List.of(new Ingredient(Items.PAPER, 6, "종이")));
        }
        if (containsAny(npcId, "carter", "provisioner", "pathfinder")) {
            return new Contract("carter", "운송 조합원", "운송 포장재 납품",
                    13L, 1, List.of(new Ingredient(Items.LEATHER, 2, "가죽"),
                    new Ingredient(Items.STRING, 3, "실")));
        }
        if (containsAny(npcId, "neighbor", "miner", "laborer", "surveyor")) {
            return new Contract("laborer", "도시 노동 조합원", "공공 보수 자재 납품",
                    10L, 1, List.of(new Ingredient(Items.COBBLESTONE, 8, "조약돌")));
        }
        return null;
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }

    private static boolean hasAll(ServerPlayer player, List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            int count = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(ingredient.item())) count += stack.getCount();
            }
            if (count < ingredient.count()) return false;
        }
        return true;
    }

    private static void consumeAll(ServerPlayer player, List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            int remaining = ingredient.count();
            for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!stack.is(ingredient.item())) continue;
                int remove = Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
        player.inventoryMenu.broadcastChanges();
    }

    private record Contract(String profession, String displayProfession, String taskName,
                            long baseWage, int renown, List<Ingredient> inputs) {
        String requirementText() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < inputs.size(); i++) {
                if (i > 0) builder.append(" · ");
                Ingredient ingredient = inputs.get(i);
                builder.append(ingredient.name()).append(' ').append(ingredient.count()).append("개");
            }
            return builder.toString();
        }
    }

    private record Ingredient(Item item, int count, String name) {
    }
}
