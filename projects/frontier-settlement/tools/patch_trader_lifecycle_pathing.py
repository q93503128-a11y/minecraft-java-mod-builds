from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
WATERFRONT = JAVA / "SettlementWaterfrontService.java"
MARKET = JAVA / "SettlementMarketService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

waterfront = WATERFRONT.read_text(encoding="utf-8")
market = MARKET.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

old_build_move = '''        if (worker.distanceToSqr(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            worker.getNavigation().moveTo(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D, 0.78D);\n            return;\n        }\n'''
new_build_move = '''        if (worker.distanceToSqr(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, worker, placement.pos(), 0.78D, INTERACTION_RANGE_SQR);\n            return;\n        }\n'''
if new_build_move not in waterfront:
    if waterfront.count(old_build_move) != 1:
        raise SystemExit(f"waterfront build path anchor count={waterfront.count(old_build_move)}")
    waterfront = waterfront.replace(old_build_move, new_build_move, 1)

old_wood_move = '''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            worker.getNavigation().moveTo(stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D, 0.8D);\n            return;\n        }\n'''
new_wood_move = '''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D)\n                > INTERACTION_RANGE_SQR) {\n            SettlementWorkerStorageNavigation.moveToInteraction(\n                    level, worker, stock, 0.8D, INTERACTION_RANGE_SQR);\n            return;\n        }\n'''
if new_wood_move not in waterfront:
    if waterfront.count(old_wood_move) != 1:
        raise SystemExit(f"waterfront stock path anchor count={waterfront.count(old_wood_move)}")
    waterfront = waterfront.replace(old_wood_move, new_wood_move, 1)

old_water_trader = '''        if (!traders.isEmpty()) {\n            FrontierWorkerEntity active = traders.getFirst();\n            active.setNoAi(false);\n            for (int i = 1; i < traders.size(); i++) {\n                FrontierWorkerEntity duplicate = traders.get(i);\n                duplicate.getNavigation().stop();\n                duplicate.setNoAi(true);\n                duplicate.setInvulnerable(true);\n            }\n            return active;\n        }\n'''
new_water_trader = '''        if (!traders.isEmpty()) {\n            FrontierWorkerEntity active = traders.getFirst();\n            active.setNoAi(false);\n            active.setInvulnerable(false);\n            // One completed waterfront owns one visible trader. Extra loaded bodies with the exact\n            // assignment tag are definitive duplicates; preserve any unexpected MAINHAND cargo and\n            // discard them instead of retaining immortal NoAI statues in historical saves.\n            for (int i = 1; i < traders.size(); i++) {\n                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, traders.get(i));\n            }\n            return active;\n        }\n'''
if new_water_trader not in waterfront:
    if waterfront.count(old_water_trader) != 1:
        raise SystemExit(f"waterfront trader lifecycle anchor count={waterfront.count(old_water_trader)}")
    waterfront = waterfront.replace(old_water_trader, new_water_trader, 1)

old_water_spawn = '''        trader.setPersistenceRequired();\n        trader.setNoAi(false);\n        trader.setInvulnerable(true);\n        trader.addTag(WATER_TRADER_TAG);\n'''
new_water_spawn = '''        trader.setPersistenceRequired();\n        trader.setNoAi(false);\n        trader.setInvulnerable(false);\n        trader.addTag(WATER_TRADER_TAG);\n'''
if new_water_spawn not in waterfront:
    if waterfront.count(old_water_spawn) != 1:
        raise SystemExit(f"waterfront trader spawn anchor count={waterfront.count(old_water_spawn)}")
    waterfront = waterfront.replace(old_water_spawn, new_water_spawn, 1)

old_market_move = '''            if (trader.distanceToSqr(crate.getX() + 0.5D, crate.getY() + 0.5D, crate.getZ() + 0.5D) > CRATE_REACHED_SQR) {\n                trader.getNavigation().moveTo(crate.getX() + 0.5D, crate.getY(), crate.getZ() + 0.5D, 0.7D);\n                continue;\n            }\n'''
new_market_move = '''            if (trader.distanceToSqr(crate.getX() + 0.5D, crate.getY() + 0.5D, crate.getZ() + 0.5D) > CRATE_REACHED_SQR) {\n                SettlementWorkerStorageNavigation.moveToInteraction(\n                        level, trader, crate, 0.7D, CRATE_REACHED_SQR);\n                continue;\n            }\n'''
if new_market_move not in market:
    if market.count(old_market_move) != 1:
        raise SystemExit(f"market crate path anchor count={market.count(old_market_move)}")
    market = market.replace(old_market_move, new_market_move, 1)

old_market_trader = '''        if (!assigned.isEmpty()) {\n            FrontierWorkerEntity active = assigned.getFirst();\n            active.setNoAi(false);\n            for (int i = 1; i < assigned.size(); i++) {\n                FrontierWorkerEntity duplicate = assigned.get(i);\n                duplicate.getNavigation().stop();\n                duplicate.setNoAi(true);\n                duplicate.setInvulnerable(true);\n            }\n            return active;\n        }\n'''
new_market_trader = '''        if (!assigned.isEmpty()) {\n            FrontierWorkerEntity active = assigned.getFirst();\n            active.setNoAi(false);\n            active.setInvulnerable(false);\n            for (int i = 1; i < assigned.size(); i++) {\n                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i));\n            }\n            return active;\n        }\n'''
if new_market_trader not in market:
    if market.count(old_market_trader) != 1:
        raise SystemExit(f"market trader lifecycle anchor count={market.count(old_market_trader)}")
    market = market.replace(old_market_trader, new_market_trader, 1)

old_market_spawn = '''        trader.setCustomNameVisible(true);\n        trader.setPersistenceRequired();\n        trader.setNoAi(false);\n        trader.addTag(MARKET_TRADER_TAG);\n'''
new_market_spawn = '''        trader.setCustomNameVisible(true);\n        trader.setPersistenceRequired();\n        trader.setNoAi(false);\n        trader.setInvulnerable(false);\n        trader.addTag(MARKET_TRADER_TAG);\n'''
if new_market_spawn not in market:
    if market.count(old_market_spawn) != 1:
        raise SystemExit(f"market trader spawn anchor count={market.count(old_market_spawn)}")
    market = market.replace(old_market_spawn, new_market_spawn, 1)

old_vars = '''barracks = text(JAVA / "settlement/SettlementBarracksService.java")\noffice = text(JAVA / "settlement/SettlementConstructionOfficeService.java")\n'''
new_vars = '''barracks = text(JAVA / "settlement/SettlementBarracksService.java")\nwaterfront = text(JAVA / "settlement/SettlementWaterfrontService.java")\nmarket = text(JAVA / "settlement/SettlementMarketService.java")\noffice = text(JAVA / "settlement/SettlementConstructionOfficeService.java")\n'''
if new_vars not in audit:
    if audit.count(old_vars) != 1:
        raise SystemExit(f"audit trader vars anchor count={audit.count(old_vars)}")
    audit = audit.replace(old_vars, new_vars, 1)

audit_anchor = '''forbid(barracks, (\n    "duplicate.setNoAi(true);",\n    "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"\n), "legacy barracks duplicate containment")\n'''
audit_block = '''forbid(barracks, (\n    "duplicate.setNoAi(true);",\n    "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"\n), "legacy barracks duplicate containment")\nmust(waterfront, (\n    "active.setInvulnerable(false);",\n    "trader.setInvulnerable(false);",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, traders.get(i))",\n    "level, worker, placement.pos(), 0.78D, INTERACTION_RANGE_SQR",\n    "level, worker, stock, 0.8D, INTERACTION_RANGE_SQR"\n), "waterfront trader/build path hardening")\nforbid(waterfront, (\n    "duplicate.setNoAi(true);",\n    "duplicate.setInvulnerable(true);",\n    "trader.setInvulnerable(true);",\n    "worker.getNavigation().moveTo(placement.pos().getX() + 0.5D",\n    "worker.getNavigation().moveTo(stock.getX() + 0.5D"\n), "legacy waterfront lifecycle/pathing")\nmust(market, (\n    "active.setInvulnerable(false);",\n    "trader.setInvulnerable(false);",\n    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))",\n    "level, trader, crate, 0.7D, CRATE_REACHED_SQR"\n), "market trader/path hardening")\nforbid(market, (\n    "duplicate.setNoAi(true);",\n    "duplicate.setInvulnerable(true);",\n    "trader.getNavigation().moveTo(crate.getX() + 0.5D"\n), "legacy market lifecycle/pathing")\n'''
if "waterfront trader/build path hardening" not in audit:
    if audit.count(audit_anchor) != 1:
        raise SystemExit(f"audit trader block anchor count={audit.count(audit_anchor)}")
    audit = audit.replace(audit_anchor, audit_block, 1)

WATERFRONT.write_text(waterfront, encoding="utf-8")
MARKET.write_text(market, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for label, src, required in (
    ("waterfront", waterfront, (
        "active.setInvulnerable(false);",
        "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, traders.get(i))",
        "level, worker, placement.pos(), 0.78D, INTERACTION_RANGE_SQR",
        "level, worker, stock, 0.8D, INTERACTION_RANGE_SQR",
    )),
    ("market", market, (
        "active.setInvulnerable(false);",
        "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))",
        "level, trader, crate, 0.7D, CRATE_REACHED_SQR",
    )),
):
    for token in required:
        if token not in src:
            raise SystemExit(f"{label} invariant missing: {token}")

for forbidden in ("duplicate.setNoAi(true);", "duplicate.setInvulnerable(true);", "trader.setInvulnerable(true);"):
    if forbidden in waterfront:
        raise SystemExit(f"legacy waterfront invariant remains: {forbidden}")
for forbidden in ("duplicate.setNoAi(true);", "duplicate.setInvulnerable(true);", "trader.getNavigation().moveTo(crate.getX() + 0.5D"):
    if forbidden in market:
        raise SystemExit(f"legacy market invariant remains: {forbidden}")
if "waterfront trader/build path hardening" not in audit or "market trader/path hardening" not in audit:
    raise SystemExit("trader persistent audit missing")

print("TRADER LIFECYCLE PATHING PATCH PASS")
