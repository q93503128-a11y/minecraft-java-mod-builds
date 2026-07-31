package kr.moonseungjun.livingkingdoms.skill;

import kr.moonseungjun.livingkingdoms.crime.CrimeSavedData;
import kr.moonseungjun.livingkingdoms.world.RealmJurisdiction;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Society skill effects that operate on persistent warrants. */
public final class SkillCrimeHooks {
    private SkillCrimeHooks() {
    }

    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)
                || !level.dimension().equals(StarterRealmManager.REALM_KEY)
                || level.getGameTime() % 600L != 0L
                || !SkillProgressionManager.has(player, "society_escape_routes")) {
            return;
        }
        CrimeSavedData data = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE);
        CrimeSavedData.CrimeRecord record = data.record(player.getUUID());
        String local = RealmJurisdiction.at(level, player.blockPosition());
        if (record.wanted() <= 0 || record.wantedHere(local == null ? "wilderness" : local)) return;
        CrimeSavedData.CrimeRecord reduced = data.reduceWanted(player.getUUID(), 1);
        player.sendSystemMessage(Component.literal(
                "§7[탈출 경로] §f관할 밖에 숨어 수배도가 §e" + reduced.wanted() + "§f로 감소했습니다."
        ));
    }
}
