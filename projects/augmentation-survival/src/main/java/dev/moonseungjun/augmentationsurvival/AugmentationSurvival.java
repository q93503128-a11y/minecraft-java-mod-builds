package dev.moonseungjun.augmentationsurvival;

import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.BossCoreRewardCatalog;
import dev.moonseungjun.augmentationsurvival.core.BossCoreRewardService;
import dev.moonseungjun.augmentationsurvival.core.CoreDistributionCatalog;
import dev.moonseungjun.augmentationsurvival.core.CoreRewardService;
import dev.moonseungjun.augmentationsurvival.core.CoreSourceResolver;
import dev.moonseungjun.augmentationsurvival.core.FishingCoreRewardService;
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
        AugmentationAttachments.initialize();
        WorldThreatAttachments.initialize();
        CoreDistributionCatalog.initialize();
        CoreSourceResolver.initialize();
        BossCoreRewardCatalog.initialize();
        CoreRewardService.initialize();
        BossCoreRewardService.initialize();
        FishingCoreRewardService.initialize();
        LOGGER.info("Augmentation Survival runtime spine initialized");
    }
}
