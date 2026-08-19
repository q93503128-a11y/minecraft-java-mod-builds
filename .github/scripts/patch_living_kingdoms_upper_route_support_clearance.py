from pathlib import Path

path = Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredUpperRouteManager.java')
text = path.read_text(encoding='utf-8')

old_support = '''            if (sourceFloorAir && level.getBlockState(floorPos).isAir()) {\n                level.setBlock(floorPos, support.defaultBlockState(), UPDATE_FLAGS);\n            }\n'''
new_support = '''            // A stair block is itself the walkable surface. Adding a synthetic full support\n            // beneath every stair can occupy the head cell of a lower switchback segment when\n            // the source-air route passes under itself two metres below. Keep stair undersides\n            // open; only flat air-foot nodes need an authored support floor.\n            if (stair == null && sourceFloorAir && level.getBlockState(floorPos).isAir()) {\n                level.setBlock(floorPos, support.defaultBlockState(), UPDATE_FLAGS);\n            }\n'''
if new_support not in text:
    if old_support not in text:
        raise SystemExit('upper-route support insertion block not found')
    text = text.replace(old_support, new_support, 1)

old_verify = '''            BlockState floor = level.getBlockState(new BlockPos(\n                    node.world().x(), node.world().y() - 1, node.world().z()));\n            if (floor.isAir() || !floor.getFluidState().isEmpty()) return false;\n'''
new_verify = '''            BlockState floor = level.getBlockState(new BlockPos(\n                    node.world().x(), node.world().y() - 1, node.world().z()));\n            // Flat air-foot nodes require a real floor. A stair node does not: the stair in the\n            // feet cell is already its collision/walking surface, and forcing a block below it\n            // can destroy the headroom of a lower switchback segment. Fluids below either form\n            // are still rejected.\n            if (!floor.getFluidState().isEmpty()) return false;\n            if (stair == null && floor.isAir()) return false;\n'''
if new_verify not in text:
    if old_verify not in text:
        raise SystemExit('upper-route floor verification block not found')
    text = text.replace(old_verify, new_verify, 1)

if 'if (stair == null && sourceFloorAir' not in text:
    raise SystemExit('stair support-clearance rule missing')
if 'if (stair == null && floor.isAir()) return false;' not in text:
    raise SystemExit('stair-aware floor verification rule missing')
if 'setChunkForced' in text:
    raise SystemExit('upper-route manager unexpectedly gained persistent forced chunk loading')

path.write_text(text, encoding='utf-8')
print('Living Kingdoms upper-route stair undersides now preserve lower switchback headroom')
