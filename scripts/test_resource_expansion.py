"""Offline failure controls for the real resource-byte verifier."""
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile

from verify_resource_expansion import verify_resources


class ResourceExpansionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source, self.processed = self.root / 'source', self.root / 'processed'
        self.source.mkdir()
        self.processed.mkdir()
        (self.source / 'plugin.yml').write_bytes(b"name: Example\nversion: '${version}'\n")
        (self.source / 'config.yml').write_bytes(b'amount: 42\nname: unchanged\n')
        self.payload = {
            'plugin.yml': b"name: Example\nversion: '3.1.11'\n",
            'config.yml': b'amount: 42\nname: unchanged\n',
        }
        for name, data in self.payload.items():
            (self.processed / name).write_bytes(data)
        self.jar = self.root / 'candidate.jar'
        self.write_jar(self.payload)

    def write_jar(self, payload):
        with zipfile.ZipFile(self.jar, 'w') as archive:
            for name, data in payload.items():
                archive.writestr(name, data)

    def verify(self, version='3.1.11'):
        return verify_resources(self.source, self.processed, self.jar, version)

    def test_exact_expansion_and_unchanged_other_resource_pass(self):
        result = self.verify()
        self.assertEqual('PASS', result['status'])
        self.assertEqual(2, result['resource_count'])
        self.assertEqual({'plugin.yml', 'config.yml'}, set(result['sha256']))

    def test_wrong_version_fails(self):
        with self.assertRaisesRegex(ValueError, 'Processed resource bytes differ'):
            self.verify('3.1.12')

    def test_changed_processed_bytes_fail(self):
        (self.processed / 'config.yml').write_bytes(b'amount: 99\n')
        with self.assertRaisesRegex(ValueError, 'Processed resource bytes differ'):
            self.verify()

    def test_changed_packaged_bytes_fail(self):
        self.write_jar({**self.payload, 'config.yml': b'changed'})
        with self.assertRaisesRegex(ValueError, 'Packaged resource bytes differ'):
            self.verify()

    def test_missing_or_extra_processed_files_fail(self):
        (self.processed / 'extra.yml').write_bytes(b'extra')
        with self.assertRaisesRegex(ValueError, 'file set differs'):
            self.verify()
        (self.processed / 'extra.yml').unlink()
        (self.processed / 'config.yml').unlink()
        with self.assertRaisesRegex(ValueError, 'file set differs'):
            self.verify()

    def test_missing_or_duplicate_packaged_resource_fails(self):
        self.write_jar({'plugin.yml': self.payload['plugin.yml']})
        with self.assertRaisesRegex(ValueError, 'Missing or duplicate'):
            self.verify()
        self.write_jar(self.payload)
        with warnings.catch_warnings(), zipfile.ZipFile(self.jar, 'a') as archive:
            warnings.simplefilter('ignore', UserWarning)
            archive.writestr('plugin.yml', self.payload['plugin.yml'])
        with self.assertRaisesRegex(ValueError, 'Missing or duplicate'):
            self.verify()

    def test_empty_source_or_version_fails(self):
        with self.assertRaises(ValueError):
            self.verify('')
        (self.source / 'plugin.yml').unlink()
        with self.assertRaisesRegex(ValueError, 'Missing source'):
            self.verify()

    def test_build_capture_and_strict_actual_resource_gate_are_present(self):
        root = Path(__file__).resolve().parents[1]
        build = (root / 'build.gradle.kts').read_text()
        action = build.split('tasks.named<ProcessResources>("processResources")', 1)[1].split('\ntasks.', 1)[0]
        self.assertIn('val resourceVersion = project.version.toString()', build)
        self.assertNotIn('project.version', action)
        self.assertIn('inputs.property("resourceVersion", resourceVersion)', action)
        self.assertIn('expand(mapOf("version" to resourceVersion))', action)
        workflow = (root / '.github/workflows/build.yml').read_text()
        self.assertIn('processResources --rerun-tasks --no-daemon --no-configuration-cache --warning-mode fail', workflow)
        self.assertIn('python3 scripts/verify_resource_expansion.py --version "$VERSION" --jar "dist/${OUTPUT_NAME}"', workflow)


if __name__ == '__main__':
    unittest.main()
