import unittest
import probe_drehmal_start_runtime as p


class SceneRuntimeTests(unittest.TestCase):
    def test_pinned_chain_scope(self):
        self.assertEqual(len(p.chain_positions()), 57)
        self.assertEqual(len(p.ROOTS), 6)
        self.assertTrue(set(p.seven.coordinate_roles()).issubset(p.chain_positions()))

    def test_state_signature_deterministic(self):
        x = {(1, 4, 7): ("minecraft:stone", None, False),
             (0, 0, 0): ("minecraft:air", None, False)}
        self.assertEqual(p.signature(x), p.signature(dict(reversed(list(x.items())))))
        x[(1, 4, 7)] = ("minecraft:grass_block", None, False)
        self.assertNotEqual(p.signature(x), p.signature({(0,0,0): ("minecraft:air",None,False)}))

    def test_dedicated_rcon_auth_packet_is_not_map_data(self):
        self.assertNotIn("TURNBOUND_DREHMAL_WORLD_V1", p.RUN_MARKER)
        self.assertEqual(p.game_time("The time is 1135083463"), 1135083463)
        with self.assertRaises(ValueError): p.game_time("unknown")

    def test_scope_rejects_unstaged_copy(self):
        with self.assertRaises((ValueError, FileNotFoundError)):
            p.verify_staged_copy(p.Path("/an/impossible/turnbound/copy"), "six")

if __name__ == "__main__":
    unittest.main()
