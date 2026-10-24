package dev.moonseungjun.augmentationsurvival;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatTuning;
import dev.moonseungjun.augmentationsurvival.augment.combat.ProtectionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.stat.PlayerStatAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.stat.StatAugmentCatalog;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.BossCoreRewardCatalog;
import dev.moonseungjun.augmentationsurvival.core.BossCoreRewardService;
import dev.moonseungjun.augmentationsurvival.core.CoreDistributionCatalog;
import dev.moonseungjun.augmentationsurvival.core.CoreRewardService;
import dev.moonseungjun.augmentationsurvival.core.CoreSourceResolver;
import dev.moonseungjun.augmentationsurvival.core.FishingCoreRewardService;
import dev.moonseungjun.augmentationsurvival.core.StructureCoreRewardService;
import dev.moonseungjun.augmentationsurvival.device.AugmentDeviceBlocks;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenus;
import dev.moonseungjun.augmentationsurvival.state.AugmentationAttachments;
import dev.moonseungjun.augmentationsurvival.threat.WorldThreatAttachments;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AugmentationSurvival implements ModInitializer {
    public static final String MOD_ID = "augmentation_survival";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AugmentCoreItems.initialize();
        AugmentDeviceMenus.initialize();
        AugmentDeviceBlocks.initialize();
        AugmentationAttachments.initialize();
        WorldThreatAttachments.initialize();
        AugmentCatalog.initialize();
        StatAugmentCatalog.initialize();
        PlayerStatAugmentService.initialize();
        CombatTuning.initialize();
        ProtectionAugmentService.initialize();
        CombatAugmentService.initialize();
        CoreDistributionCatalog.initialize();
        CoreSourceResolver.initialize();
        BossCoreRewardCatalog.initialize();
        CoreRewardService.initialize();
        BossCoreRewardService.initialize();
        FishingCoreRewardService.initialize();
        StructureCoreRewardService.initialize();
        LOGGER.info("Augmentation Survival runtime spine initialized");
    }
}
