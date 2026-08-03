package kr.moonseungjun.livingkingdoms.world;

/** Stable audit contract shared by build verification and future economy migrations. */
public final class ErdenPhysicalEconomyAudit {
    public static final int EXPECTED_SITES = ErdenAuthoritativeEconomyManager.EXPECTED_SITES;
    public static final int EXPECTED_WAREHOUSES = ErdenAuthoritativeEconomyManager.EXPECTED_WAREHOUSES;
    public static final int EXPECTED_WALLETS = ErdenAuthoritativeEconomyManager.EXPECTED_WALLETS;
    public static final int ECONOMY_SCHEMA = ErdenPhysicalEconomySavedData.SCHEMA_VERSION;

    private ErdenPhysicalEconomyAudit() {
    }
}
