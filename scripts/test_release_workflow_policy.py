"""Offline structural regressions for the known legacy release bypass; never publish."""
from pathlib import Path
import re
import subprocess
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[1]


def require_policy(legacy: str, maintained: str) -> None:
    if not re.search(r"(?m)^permissions:\n  contents: read$", legacy):
        raise ValueError("The retired entrypoint must be read-only")
    for forbidden in ("contents: write", "gh release", "GH_TOKEN", "GITHUB_TOKEN", "github.token", "uses:"):
        if forbidden in legacy:
            raise ValueError("The retired entrypoint must not run actions or access a publishing credential")
    if "exit 1" not in legacy or ".github/workflows/build.yml" not in legacy:
        raise ValueError("The retired entrypoint must fail with the maintained workflow location")
    if "--clobber" in maintained or "gh release upload" in maintained:
        raise ValueError("The maintained publisher must create a new release, never overwrite")
    publisher = maintained.split("\n  publish:\n")
    if len(publisher) != 2:
        raise ValueError("Expected exactly one maintained publisher job")
    required = (
        "needs: build",
        "github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main' && inputs.publish == true",
        'test "$CURRENT_MAIN" = "$GITHUB_SHA"',
        'test "$CONFIRM_VERSION" = "$VERSION"',
        'if gh release view "$TAG" --repo "$GITHUB_REPOSITORY"',
        'if git ls-remote --exit-code --tags',
        'Refusing to replace existing immutable release',
        'Refusing to replace existing immutable Git tag',
        'skip-decompress: true',
        'gh release create "$TAG"',
        'cmp "dist/$OUTPUT_NAME" "verified/$OUTPUT_NAME"',
    )
    for fragment in required:
        if fragment not in publisher[1]:
            raise ValueError("Required immutable-release check is missing: " + fragment)


class ReleaseWorkflowPolicyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.legacy = (ROOT / ".github/workflows/release.yml").read_text(encoding="utf-8")
        cls.maintained = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")

    def test_actual_workflows_keep_the_protected_entrypoint(self):
        require_policy(self.legacy, self.maintained)

    def test_retired_script_fails_with_the_replacement_location(self):
        require_policy(self.legacy, self.maintained)
        script = textwrap.dedent(self.legacy.split("        run: |\n", 1)[1])
        result = subprocess.run(["/bin/bash", "-c", script], capture_output=True, text=True,
                                check=False, timeout=5, env={"PATH": "/usr/bin:/bin"})
        self.assertEqual(1, result.returncode)
        self.assertIn(".github/workflows/build.yml", result.stdout)
        self.assertIn("cannot publish or replace assets", result.stdout)

    def test_old_write_permission_is_rejected(self):
        with self.assertRaises(ValueError):
            require_policy(self.legacy.replace("contents: read", "contents: write"), self.maintained)

    def test_original_upload_and_clobber_bypass_is_rejected(self):
        with self.assertRaises(ValueError):
            require_policy(self.legacy + '\n          gh release upload "$TAG" "$JAR" --clobber\n', self.maintained)

    def test_retired_workflow_cannot_receive_credentials_or_actions(self):
        for text in ("GH_TOKEN", "GITHUB_TOKEN", "github.token", "uses:"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                require_policy(self.legacy + "\n" + text, self.maintained)

    def test_retired_workflow_cannot_report_false_success(self):
        with self.assertRaises(ValueError):
            require_policy(self.legacy.replace("exit 1", "exit 0"), self.maintained)

    def test_main_dispatch_and_explicit_publish_conditions_remain_required(self):
        for text in ("github.event_name == 'workflow_dispatch'", "github.ref == 'refs/heads/main'", "inputs.publish == true"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                require_policy(self.legacy, self.maintained.replace(text, "true"))

    def test_current_source_and_version_confirmation_remain_required(self):
        for text in ('test "$CURRENT_MAIN" = "$GITHUB_SHA"', 'test "$CONFIRM_VERSION" = "$VERSION"'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                require_policy(self.legacy, self.maintained.replace(text, "true"))

    def test_existing_release_and_tag_rejection_remain_required(self):
        for text in ('if gh release view "$TAG" --repo "$GITHUB_REPOSITORY"', 'if git ls-remote --exit-code --tags'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                require_policy(self.legacy, self.maintained.replace(text, "if false"))

    def test_new_publisher_cannot_reintroduce_upload_or_clobber(self):
        for text in ('gh release upload "$TAG"', '--clobber'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                require_policy(self.legacy, self.maintained + "\n" + text)

    def test_build_raw_artifact_and_byte_verification_remain_required(self):
        for text in ('needs: build', 'skip-decompress: true', 'cmp "dist/$OUTPUT_NAME" "verified/$OUTPUT_NAME"'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                require_policy(self.legacy, self.maintained.replace(text, ""))


if __name__ == "__main__":
    unittest.main()
