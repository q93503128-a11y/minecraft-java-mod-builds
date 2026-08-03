package kr.moonseungjun.livingkingdoms.world;

import java.util.List;

/** One authoritative metre-scale catalog shared by supply simulation and exterior construction. */
public final class ErdenKingdomSupplyCatalog {
    public static final class SupplyNode {
        public final String id;
        public final int x;
        public final int z;
        public final String role;
        public final int radius;
        public final String buildingStyle;
        public final int facingQuarterTurns;

        public SupplyNode(
                String id,
                int x,
                int z,
                String role,
                int radius,
                String buildingStyle,
                int facingQuarterTurns) {
            this.id = id;
            this.x = x;
            this.z = z;
            this.role = role;
            this.radius = radius;
            this.buildingStyle = buildingStyle;
            this.facingQuarterTurns = Math.floorMod(facingQuarterTurns, 4);
        }

        public boolean producer() {
            return !role.equals("river_wharf");
        }
    }

    private static final List<SupplyNode> NODES = List.of(
            new SupplyNode("erden_grain_estate_01", -1_650, 1_050, "grain_estate", 72, "house", 1),
            new SupplyNode("erden_grain_estate_02", -550, 1_220, "grain_estate", 72, "manor", 2),
            new SupplyNode("erden_grain_estate_03", 550, 1_240, "grain_estate", 72, "house", 2),
            new SupplyNode("erden_grain_estate_04", 1_650, 1_060, "grain_estate", 72, "manor", 3),

            new SupplyNode("erden_ranch_01", -1_700, -1_050, "ranch", 68, "castle_house", 1),
            new SupplyNode("erden_ranch_02", -700, -1_180, "ranch", 68, "house", 2),
            new SupplyNode("erden_ranch_03", 400, -1_160, "ranch", 68, "castle_house", 2),

            new SupplyNode("erden_colliery_01", 1_450, -1_250, "colliery", 56, "castle_house", 0),
            new SupplyNode("erden_colliery_02", 1_750, -1_050, "colliery", 56, "house", 3),
            new SupplyNode("erden_colliery_03", 2_050, -850, "colliery", 56, "castle_house", 3),

            new SupplyNode("erden_iron_mine_01", 1_800, -1_450, "iron_mine", 60, "castle_house", 0),
            new SupplyNode("erden_iron_mine_02", 2_200, -1_200, "iron_mine", 60, "manor", 3),

            new SupplyNode("erden_paper_mill_01", -1_550, 250, "paper_mill", 52, "house", 1),
            new SupplyNode("erden_paper_mill_02", -1_750, -200, "paper_mill", 52, "manor", 1),
            new SupplyNode("erden_paper_mill_03", -1_950, 50, "paper_mill", 52, "house", 1),

            new SupplyNode("erden_west_wharf", -1_220, 250, "river_wharf", 48, "house", 1),
            new SupplyNode("erden_south_wharf", 0, 920, "river_wharf", 48, "castle_house", 2),
            new SupplyNode("erden_east_wharf", 1_220, -250, "river_wharf", 48, "house", 3)
    );

    private ErdenKingdomSupplyCatalog() {
    }

    public static List<SupplyNode> nodes() {
        return NODES;
    }

    public static SupplyNode node(String id) {
        for (SupplyNode node : NODES) {
            if (node.id.equals(id)) return node;
        }
        return null;
    }

    public static int producerCount() {
        int count = 0;
        for (SupplyNode node : NODES) if (node.producer()) count++;
        return count;
    }

    public static int wharfCount() {
        return NODES.size() - producerCount();
    }
}
