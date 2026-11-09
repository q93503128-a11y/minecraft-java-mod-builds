import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path

SRC = Path(__file__).with_name("download_audit_official_drehmal.py")
spec = importlib.util.spec_from_file_location("official_audit", SRC)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class OfficialAuditSafeExtractionTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)

    def test_safe_zip_extraction_and_duplicate_identical(self):
        zip_path = self.root / "shard.zip"
        with zipfile.ZipFile(zip_path, "w") as z:
            z.writestr("level.dat", b"test level")
            z.writestr("datapacks/game/data/minecraft/tags/functions/tick.json",
                       b'{"values": ["sample:main"]}')
        dest = self.root / "world"
        dest.mkdir()
        self.assertEqual(mod.safe_extract(zip_path, dest)[0], 2)
        self.assertEqual((dest / "level.dat").read_bytes(), b"test level")
        self.assertEqual(mod.safe_extract(zip_path, dest)[0], 0)

    def test_zip_traversal_rejected(self):
        zip_path = self.root / "shard.zip"
        with zipfile.ZipFile(zip_path, "w") as z:
            z.writestr("../escape.txt", b"bad")
        dest = self.root / "world"
        dest.mkdir()
        with self.assertRaisesRegex(ValueError, "Unsafe ZIP"):
            mod.safe_extract(zip_path, dest)
        self.assertFalse((self.root / "escape.txt").exists())

    def test_conflicting_shard_rejected_without_overwrite(self):
        zip_path = self.root / "shard.zip"
        with zipfile.ZipFile(zip_path, "w") as z:
            z.writestr("level.dat", b"wrong")
        dest = self.root / "world"
        dest.mkdir()
        (dest / "level.dat").write_bytes(b"original")
        with self.assertRaisesRegex(ValueError, "Conflicting"):
            mod.safe_extract(zip_path, dest)
        self.assertEqual((dest / "level.dat").read_bytes(), b"original")

    def test_download_retries_gateway_timeout_and_verifies_full_size(self):
        import io
        from unittest.mock import patch
        from urllib.error import HTTPError

        class Response(io.BytesIO):
            headers = {}
            def getcode(self): return 200

        dest = self.root / "download.zip"
        timeout = HTTPError("https://github.com", 504, "Gateway Timeout", None, None)
        with patch.object(mod.urllib.request, "urlopen", side_effect=[timeout, Response(b"abcd")]) as opener:
            with patch.object(mod.time, "sleep"):
                mod.download("https://github.com/example/file.zip", dest, 4, retries=2)
        self.assertEqual(dest.read_bytes(), b"abcd")
        self.assertEqual(opener.call_count, 2)

if __name__ == "__main__":
    unittest.main()
