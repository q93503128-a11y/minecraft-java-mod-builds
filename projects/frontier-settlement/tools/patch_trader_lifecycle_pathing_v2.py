from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"
WATERFRONT = JAVA / "SettlementWaterfrontService.java"
MARKET = JAVA / "SettlementMarketService.java"
AUDIT = ROOT / "tools/test_alpha91_source.py"

waterfront = WATERFRONT.read_text(encoding="utf-8")
market = MARKET.read_text(encoding="utf-8")
audit = AUDIT.read_text(encoding="utf-8")

def replace_once(src, old, new, label):
    if new in src:
        return src
    count = src.count(old)
    if count != 1:
        raise SystemExit(f"{label} anchor count={count}")
    return src.replace(old, new, 1)

waterfront = replace_once(waterfront,
'''        if (worker.distanceToSqr(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            worker.getNavigation().moveTo(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D, 0.78D);
            return;
        }
''',
'''        if (worker.distanceToSqr(placement.pos().getX() + 0.5D, placement.pos().getY(), placement.pos().getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, worker, placement.pos(), 0.78D, INTERACTION_RANGE_SQR);
            return;
        }
''', "waterfront build path")

waterfront = replace_once(waterfront,
'''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            worker.getNavigation().moveTo(stock.getX() + 0.5D, stock.getY(), stock.getZ() + 0.5D, 0.8D);
            return;
        }
''',
'''        if (worker.distanceToSqr(stock.getX() + 0.5D, stock.getY() + 0.5D, stock.getZ() + 0.5D)
                > INTERACTION_RANGE_SQR) {
            SettlementWorkerStorageNavigation.moveToInteraction(
                    level, worker, stock, 0.8D, INTERACTION_RANGE_SQR);
            return;
        }
''', "waterfront stock path")

waterfront = replace_once(waterfront,
'''        if (!traders.isEmpty()) {
            FrontierWorkerEntity active = traders.getFirst();
            active.setNoAi(false);
            for (int i = 1; i < traders.size(); i++) {
                FrontierWorkerEntity duplicate = traders.get(i);
                duplicate.getNavigation().stop();
                duplicate.setNoAi(true);
                duplicate.setInvulnerable(true);
            }
            return active;
        }
''',
'''        if (!traders.isEmpty()) {
            FrontierWorkerEntity active = traders.getFirst();
            active.setNoAi(false);
            active.setInvulnerable(false);
            // One completed waterfront owns one visible trader. Extra loaded bodies with the exact
            // assignment tag are definitive duplicates; preserve any unexpected MAINHAND cargo and
            // discard them instead of retaining immortal NoAI statues in historical saves.
            for (int i = 1; i < traders.size(); i++) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, traders.get(i));
            }
            return active;
        }
''', "waterfront trader lifecycle")

waterfront = replace_once(waterfront,
'''        trader.setPersistenceRequired();
        trader.setNoAi(false);
        trader.setInvulnerable(true);
        trader.addTag(WATER_TRADER_TAG);
''',
'''        trader.setPersistenceRequired();
        trader.setNoAi(false);
        trader.setInvulnerable(false);
        trader.addTag(WATER_TRADER_TAG);
''', "waterfront trader spawn")

market = replace_once(market,
'''            if (trader.distanceToSqr(crate.getX() + 0.5D, crate.getY() + 0.5D, crate.getZ() + 0.5D) > CRATE_REACHED_SQR) {
                trader.getNavigation().moveTo(crate.getX() + 0.5D, crate.getY(), crate.getZ() + 0.5D, 0.7D);
                continue;
            }
''',
'''            if (trader.distanceToSqr(crate.getX() + 0.5D, crate.getY() + 0.5D, crate.getZ() + 0.5D) > CRATE_REACHED_SQR) {
                SettlementWorkerStorageNavigation.moveToInteraction(
                        level, trader, crate, 0.7D, CRATE_REACHED_SQR);
                continue;
            }
''', "market crate path")

market = replace_once(market,
'''        if (!assigned.isEmpty()) {
            FrontierWorkerEntity active = assigned.getFirst();
            active.setNoAi(false);
            for (int i = 1; i < assigned.size(); i++) {
                FrontierWorkerEntity duplicate = assigned.get(i);
                duplicate.getNavigation().stop();
                duplicate.setNoAi(true);
                duplicate.setInvulnerable(true);
            }
            return active;
        }
''',
'''        if (!assigned.isEmpty()) {
            FrontierWorkerEntity active = assigned.getFirst();
            active.setNoAi(false);
            active.setInvulnerable(false);
            for (int i = 1; i < assigned.size(); i++) {
                SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i));
            }
            return active;
        }
''', "market trader lifecycle")

market = replace_once(market,
'''        trader.setCustomNameVisible(true);
        trader.setPersistenceRequired();
        trader.setNoAi(false);
        trader.addTag(MARKET_TRADER_TAG);
''',
'''        trader.setCustomNameVisible(true);
        trader.setPersistenceRequired();
        trader.setNoAi(false);
        trader.setInvulnerable(false);
        trader.addTag(MARKET_TRADER_TAG);
''', "market trader spawn")

audit = replace_once(audit,
'''barracks = text(JAVA / "settlement/SettlementBarracksService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
''',
'''barracks = text(JAVA / "settlement/SettlementBarracksService.java")
waterfront = text(JAVA / "settlement/SettlementWaterfrontService.java")
market = text(JAVA / "settlement/SettlementMarketService.java")
office = text(JAVA / "settlement/SettlementConstructionOfficeService.java")
''', "audit trader vars")

audit = replace_once(audit,
'''forbid(barracks, (
    "duplicate.setNoAi(true);",
    "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"
), "legacy barracks duplicate containment")
''',
'''forbid(barracks, (
    "duplicate.setNoAi(true);",
    "return legacy.isEmpty() ? null : migrateLegacySoldier(level, legacy.getFirst());"
), "legacy barracks duplicate containment")
must(waterfront, (
    "active.setInvulnerable(false);",
    "trader.setInvulnerable(false);",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, traders.get(i))",
    "level, worker, placement.pos(), 0.78D, INTERACTION_RANGE_SQR",
    "level, worker, stock, 0.8D, INTERACTION_RANGE_SQR"
), "waterfront trader/build path hardening")
forbid(waterfront, (
    "duplicate.setNoAi(true);",
    "duplicate.setInvulnerable(true);",
    "trader.setInvulnerable(true);",
    "worker.getNavigation().moveTo(placement.pos().getX() + 0.5D",
    "worker.getNavigation().moveTo(stock.getX() + 0.5D"
), "legacy waterfront lifecycle/pathing")
must(market, (
    "active.setInvulnerable(false);",
    "trader.setInvulnerable(false);",
    "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, assigned.get(i))",
    "level, trader, crate, 0.7D, CRATE_REACHED_SQR"
), "market trader/path hardening")
forbid(market, (
    "duplicate.setNoAi(true);",
    "duplicate.setInvulnerable(true);",
    "trader.getNavigation().moveTo(crate.getX() + 0.5D"
), "legacy market lifecycle/pathing")
''', "audit trader invariants")

WATERFRONT.write_text(waterfront, encoding="utf-8")
MARKET.write_text(market, encoding="utf-8")
AUDIT.write_text(audit, encoding="utf-8")

for label, src, required in (
    ("waterfront", waterfront, (
        "active.setInvulnerable(false);",
        "trader.setInvulnerable(false);",
        "SettlementWorkerService.removeDuplicateWorkerPreservingCargo(level, traders.get(i))",
        "level, worker, placement.pos(), 0.78D, INTERACTION_RANGE_SQR",
        "level, worker, stock, 0.8D, INTERACTION_RANGE_SQR",
    )),
    ("market", market, (
        "active.setInvulnerable(false);",
        "trader.setInvulnerable(false);",
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

print("TRADER LIFECYCLE PATHING V2 PATCH PASS")
