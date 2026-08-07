from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenExteriorResidenceBuilder.java')
text = path.read_text(encoding='utf-8')

old = '''    private static boolean collidesWithStorage(BlockPos position) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            BlockPos storage = ErdenKingdomExteriorBuilder.storagePosition(null, node);
            if (storage.getX() == position.getX()
                    && storage.getY() == position.getY()
                    && storage.getZ() == position.getZ()) return true;
        }
        return false;
    }
'''
new = '''    private static boolean collidesWithStorage(BlockPos position) {
        for (ErdenKingdomSupplyCatalog.SupplyNode node : ErdenKingdomSupplyCatalog.nodes()) {
            BlockPos storage = ErdenKingdomExteriorBuilder.storagePosition(null, node);
            if (storage.getY() != position.getY() || storage.getZ() != position.getZ()) continue;
            int dx = position.getX() - storage.getX();
            if (dx == 0 || dx == -2 || dx == 2) return true;
        }
        return false;
    }
'''
if old not in text:
    if new in text:
        raise SystemExit(0)
    raise SystemExit('storage fixture collision method missing')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
