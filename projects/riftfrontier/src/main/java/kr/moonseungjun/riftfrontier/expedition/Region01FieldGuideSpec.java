package kr.moonseungjun.riftfrontier.expedition;

import java.util.List;

/** Pure-data contract for the first-slice Region 01 field guide. */
final class Region01FieldGuideSpec {
    static final String TITLE = "Riftfrontier Field Guide";
    static final String AUTHOR = "Riftfrontier";
    static final List<String> PAGE_KEYS = List.of(
        "riftfrontier.guide.region_01.page.overview",
        "riftfrontier.guide.region_01.page.patrol",
        "riftfrontier.guide.region_01.page.salvage",
        "riftfrontier.guide.region_01.page.logistics"
    );

    private Region01FieldGuideSpec() {}
}
