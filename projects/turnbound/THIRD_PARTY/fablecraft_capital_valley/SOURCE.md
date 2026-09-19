# FableCraft Capital Valley humanoid production family

- Asset ids: `cv_b_road_cutthroat`, `cv_c_hill_marksman`
- Upstream: https://github.com/hipstereclipse/FableCraft
- Upstream commit: `68bd5f37d7d41753488633071318ea58c9ad6c47`
- License: Apache-2.0
- Repository NOTICE file: none at inspected commit
- Classification: editable_base / direct_asset

Direct sources:
- bandit geometry: `7eac22bb44f7aeca18ad6a96ae38733792580c7c`
- bandit archer geometry: `54c7f3d3140c3b185f479778e1619ec8c64c6088`
- bandit texture: `d895ba845dd1d6b939945f3c1534e57deb8f93bb`
- bandit archer texture: `d3c0fda9a3e14ec0d9e6aa02895bbf43b9ac2f6d`
- iron cleaver texture: `7381cbf72eade430ce868b7add300b94379b1858`
- oak longbow texture: `feacad51682d215a66c1ef5e898a762e2826ddda`
- shared biped motion source: `b42ca32dc439a6572eafef48269a36235d5d55e1`

TURNBOUND modifications:
- identifiers/visible bounds normalized;
- empty `RightHandItem` / `LeftHandItem` bones added solely as GeckoLib held-item anchors;
- FableCraft biped idle/walk/melee/bow motion language retargeted to TURNBOUND clip names;
- TURNBOUND combat hit/death/revive/victory transitions authored on the same final rigs;
- FableCraft iron cleaver and oak longbow are the final visible weapon designs;
- modified files are production derivatives, not placeholders.
