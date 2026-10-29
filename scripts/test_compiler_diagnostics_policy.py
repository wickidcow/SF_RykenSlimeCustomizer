"""Offline contract tests for diagnostic capture, not Java/runtime acceptance."""
from pathlib import Path
import os
import re
import subprocess
import tempfile
import textwrap
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
LANES = {
    "Verify Paper 1.21.11 / Java 21 baseline": (
        "1.21.11-build.log", "clean build", "1.21.11-R0.1-SNAPSHOT", 21),
    "Verify Paper 26.2 compilation": (
        "26.2-compile.log", "clean compileJava test", "26.2.build.+", 25),
    "Verify Paper 26.3 compilation": (
        "26.3-compile.log", "clean compileJava test", "26.3.build.205-stable", 25),
}


class CompilerDiagnosticsPolicy(unittest.TestCase):
    def setUp(self):
        self.workflow = (ROOT / ".github/workflows/build.yml").read_text(encoding="utf-8")
        self.build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")

    def step(self, name):
        marker = "      - name: " + name + "\n"
        self.assertEqual(1, self.workflow.count(marker))
        body = self.workflow.split(marker, 1)[1]
        return re.split(r"^      - name: |^  publish:", body, maxsplit=1, flags=re.MULTILINE)[0]

    def script(self, name):
        return textwrap.dedent(self.step(name).split("        run: |\n", 1)[1])

    def test_actual_javac_warnings_enabled_without_suppression_or_target_change(self):
        compile_java = self.build.split("tasks.compileJava {", 1)[1].split("\n}", 1)[0]
        self.assertIn('options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))', compile_java)
        self.assertIn('options.release.set(targetJvm)', compile_java)
        self.assertIn('providers.gradleProperty("targetJvm").orElse("21")', self.build)
        self.assertNotRegex(compile_java, r"-nowarn|-Xlint:-|-Xlint:none|-Xlint:all,-")

    def test_original_tasks_api_pins_and_release_staging_order_are_preserved(self):
        for name, (log, tasks, api, target) in LANES.items():
            with self.subTest(lane=name):
                script = self.script(name)
                self.assertIn(f"./gradlew {tasks} ", script)
                self.assertIn(f"-PpaperVersion={api}", script)
                self.assertIn(f"-PtargetJvm={target}", script)
                self.assertIn("--warning-mode all", script)
                self.assertIn(f"2>&1 | tee ci-evidence/{log}", script)
                self.assertNotRegex(script, r"(?:-x\s+|skipTests|continue-on-error|\|\|\s*true)")
                self.assertIn("set -euo pipefail", script)
        staged = self.workflow.index("      - name: Stage universal release JAR")
        self.assertLess(self.workflow.index("      - name: Verify Paper 1.21.11 / Java 21 baseline"), staged)
        self.assertLess(staged, self.workflow.index("      - name: Verify Paper 26.2 compilation"))
        self.assertLess(staged, self.workflow.index("      - name: Verify Paper 26.3 compilation"))

    def exercise(self, name, result):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            wrapper = root / "gradlew"
            wrapper.write_text('#!/bin/sh\necho compiler-stdout\necho compiler-stderr >&2\nexit "$FIXTURE_EXIT"\n')
            wrapper.chmod(0o755)
            process = subprocess.run(
                ["bash", "-c", self.script(name)], cwd=root,
                env={**os.environ, "FIXTURE_EXIT": str(result)},
                text=True, capture_output=True, timeout=10,
            )
            log = (root / "ci-evidence" / LANES[name][0]).read_text()
            self.assertIn("compiler-stdout", log)
            self.assertIn("compiler-stderr", log)
            self.assertIn("compiler-stdout", process.stdout)
            return process.returncode

    def test_all_lanes_capture_successful_stdout_and_stderr(self):
        for name in LANES:
            with self.subTest(lane=name):
                self.assertEqual(0, self.exercise(name, 0))

    def test_all_lanes_preserve_real_build_failure_despite_successful_tee(self):
        for name in LANES:
            with self.subTest(lane=name):
                self.assertEqual(17, self.exercise(name, 17))

    def test_missing_pipefail_is_detected_by_behavioral_regression(self):
        broken = self.workflow.replace("set -euo pipefail", "set -eu")
        for name in LANES:
            with self.subTest(lane=name), patch.object(self, "workflow", broken):
                with self.assertRaises(AssertionError):
                    self.assertEqual(17, self.exercise(name, 17))

    def test_diagnostics_retained_on_failure_outside_gradle_clean_directory(self):
        step = self.step("Retain exact-head compiler and build warnings")
        self.assertIn("if: always()", step)
        self.assertIn("uses: actions/upload-artifact@v7", step)
        self.assertIn("path: ci-evidence/*.log", step)
        self.assertNotIn("path: build/", step)
        self.assertIn("name: ryken-compiler-diagnostics", step)

    def test_contract_runs_before_compiling_and_publisher_remains_manual(self):
        name = "Verify retained compiler diagnostics contract"
        self.assertIn("python3 -m unittest discover -s scripts -p 'test_compiler_diagnostics_policy.py' -v", self.step(name))
        self.assertLess(self.workflow.index("      - name: " + name),
                        self.workflow.index("      - name: Verify Paper 1.21.11 / Java 21 baseline"))
        publisher = self.workflow.split("\n  publish:\n", 1)[1]
        self.assertIn("needs: build", publisher)
        self.assertIn("github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main' && inputs.publish == true", publisher)
        self.assertIn('test "$CONFIRM_VERSION" = "$VERSION"', publisher)
        self.assertNotIn("--clobber", self.workflow)


if __name__ == "__main__":
    unittest.main()
