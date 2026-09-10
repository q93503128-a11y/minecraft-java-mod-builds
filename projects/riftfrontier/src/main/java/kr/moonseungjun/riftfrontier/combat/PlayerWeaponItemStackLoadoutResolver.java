package kr.moonseungjun.riftfrontier.combat;

import kr.moonseungjun.riftfrontier.content.ContentId;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves the server-owned main-hand stack into the first production weapon-family loadout.
 *
 * <p>The ItemStack data component is identity metadata, not a second combat registry. Every decoded
 * id is restricted to the locked first-slice production set and then resolved again through the
 * current validated {@link CombatRuntimeCatalog} before it can reach execution.</p>
 */
public final class PlayerWeaponItemStackLoadoutResolver implements MinecraftPlayerWeaponCombatAdapter.LoadoutResolver {
    public static final ContentId MOBILE_PRESSURE = ContentId.rift("weapon_family/mobile_pressure");
    public static final ContentId REACH_COMMITMENT = ContentId.rift("weapon_family/reach_commitment");
    public static final ContentId RECOVERY_PIVOT = ContentId.rift("weapon_module/recovery_pivot");
    private static final Set<ContentId> PRODUCTION_FAMILIES = Set.of(MOBILE_PRESSURE, REACH_COMMITMENT);

    private final CombatRuntimeCatalog catalog;

    public PlayerWeaponItemStackLoadoutResolver(CombatRuntimeCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public Optional<MinecraftPlayerWeaponCombatAdapter.Loadout> resolve(LivingEntity actor) {
        Objects.requireNonNull(actor, "actor");
        return resolve(actor.getMainHandItem());
    }

    public Optional<MinecraftPlayerWeaponCombatAdapter.Loadout> resolve(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");
        if (stack.isEmpty()) return Optional.empty();

        PlayerWeaponLoadoutComponent component = stack.get(RiftfrontierCombatDataComponents.PLAYER_WEAPON_LOADOUT.value());
        if (component == null) return Optional.empty();

        try {
            ContentId familyId = ContentId.parse(component.familyId());
            if (!PRODUCTION_FAMILIES.contains(familyId)) return Optional.empty();

            Optional<ContentId> moduleId = component.moduleId().map(ContentId::parse);
            if (moduleId.isPresent() && !moduleId.orElseThrow().equals(RECOVERY_PIVOT)) return Optional.empty();

            MinecraftPlayerWeaponCombatAdapter.Loadout loadout = new MinecraftPlayerWeaponCombatAdapter.Loadout(familyId, moduleId);
            // Re-assemble against the currently published graph so stale/invalid component metadata fails closed.
            catalog.playerWeaponProfile(loadout.familyId(), loadout.moduleId());
            return Optional.of(loadout);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Optional.empty();
        }
    }
}
