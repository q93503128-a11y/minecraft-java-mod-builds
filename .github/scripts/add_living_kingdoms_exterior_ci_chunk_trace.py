from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
text = path.read_text(encoding="utf-8")

old = '''            if (RETAINED.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
            CI_LOADING.add(packed);'''
new = '''            if (RETAINED.add(packed)) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_EXTERIOR_CHUNK_REQUEST chunk={},{} exterior_needed={} residence_needed={} retained={} queue_remaining={}",
                        chunkX, chunkZ, exteriorNeeded,
                        residences.needsChunk(chunkX, chunkZ,
                                ErdenExteriorResidenceBuilder.RESIDENCE_REVISION),
                        RETAINED.size(), CI_REQUESTS.size());
                level.getChunkSource().addTicketAndLoadWithRadius(
                        TicketType.PORTAL, new ChunkPos(chunkX, chunkZ), 0);
            }
            CI_LOADING.add(packed);'''
if old not in text:
    raise SystemExit("request trace anchor missing")
text = text.replace(old, new, 1)

old = '''            active = new ActiveChunk(
                    packed, chunkX, chunkZ,
                    buildExterior, buildResidences, plan);
            LivingKingdoms.LOGGER.debug(
                    "Prepared Erden exterior chunk {},{} writes={} operations={} exterior={} residences={}",
                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                    buildExterior, buildResidences);'''
new = '''            active = new ActiveChunk(
                    packed, chunkX, chunkZ,
                    buildExterior, buildResidences, plan);
            if (isCi()) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_EXTERIOR_CHUNK_START chunk={},{} writes={} operations={} exterior={} residences={} plots={}",
                        chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                        buildExterior, buildResidences,
                        ErdenExteriorResidenceCatalog.forChunk(chunkX, chunkZ).size());
            }
            LivingKingdoms.LOGGER.debug(
                    "Prepared Erden exterior chunk {},{} writes={} operations={} exterior={} residences={}",
                    chunkX, chunkZ, plan.estimatedWrites(), plan.operationCount(),
                    buildExterior, buildResidences);'''
if old not in text:
    raise SystemExit("start trace anchor missing")
text = text.replace(old, new, 1)

old = '''        QUEUED.remove(active.packed);
        release(level, active.packed);
        active = null;
        verifyCi(level);'''
new = '''        if (isCi()) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_EXTERIOR_CHUNK_COMPLETE chunk={},{} applied_writes={} exterior={} residences={}",
                    active.chunkX, active.chunkZ, active.plan.appliedWrites(),
                    active.buildExterior, active.buildResidences);
        }
        QUEUED.remove(active.packed);
        release(level, active.packed);
        active = null;
        verifyCi(level);'''
if old not in text:
    raise SystemExit("complete trace anchor missing")
text = text.replace(old, new, 1)

path.write_text(text, encoding="utf-8")
