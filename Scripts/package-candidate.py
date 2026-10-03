#!/usr/bin/env python3
"""Prepare a local owner-review package. Never publishes, pushes or uploads."""
import hashlib, json, os, re, shutil, struct, subprocess, zipfile
import xml.etree.ElementTree as ET
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "release-candidate"
VERSION = "1.0.0"
def sha(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""): h.update(block)
    return h.hexdigest()
def run(args):
    env = os.environ.copy()
    env["JAVA_HOME"] = str(ROOT / ".tools/jdk-17.0.20.1+1/Contents/Home")
    result = subprocess.run([str(a) for a in args], cwd=ROOT, env=env, capture_output=True, text=True)
    return result.returncode, result.stdout + result.stderr

def main():
    OUT.mkdir(exist_ok=True)
    unsigned = ROOT / "app/build/outputs/apk/release/app-release-unsigned.apk"
    if not unsigned.exists(): raise SystemExit("Release APK missing; complete the build first")
    final = OUT / f"Akito-Station-Android-v{VERSION}-UNSIGNED.apk"
    shutil.copyfile(unsigned, final)
    tools = ROOT / ".tools/sdk/build-tools/36.0.0"
    review = OUT / f"Akito-Station-Android-v{VERSION}-LOCAL-REVIEW.apk"
    key = ROOT / ".tools/android-user/debug.keystore"
    if key.exists():
        code, text = run([tools / "apksigner", "sign", "--ks", key, "--ks-pass", "pass:android", "--key-pass", "pass:android", "--ks-key-alias", "androiddebugkey", "--out", review, final])
        if code: raise SystemExit("Local review signing failed: " + text.replace(str(ROOT), "<worktree>"))
        review.with_suffix(".apk.idsig").unlink(missing_ok=True)
    if not review.exists(): raise SystemExit("Build the local review APK before removing the development key")
    with zipfile.ZipFile(final) as original, zipfile.ZipFile(review) as signed:
        for name in original.namelist():
            if original.read(name) != signed.read(name): raise SystemExit("Local review APK does not match this release build")
    reviewaligncode, _ = run([tools / "zipalign", "-c", "-P", "16", "4", review])
    if reviewaligncode: raise SystemExit("Signed review APK alignment failed")
    code, metadata = run([tools / "aapt2", "dump", "badging", final])
    if code: raise SystemExit("APK metadata inspection failed")
    metadata = metadata.replace(str(ROOT), "<worktree>")
    (OUT / "APK-METADATA.txt").write_text(metadata)
    aligncode, alignment = run([tools / "zipalign", "-c", "-P", "16", "-v", "4", final])
    (OUT / "APK-ALIGNMENT.txt").write_text(alignment.replace(str(ROOT), "<worktree>"))
    signcode, signature = run([tools / "apksigner", "verify", "--verbose", "--print-certs", review])
    (OUT / "LOCAL-REVIEW-SIGNATURE.txt").write_text(signature.replace(str(ROOT), "<worktree>"))
    allowed = ["README.md", "STATUS.md", "THIRD_PARTY_NOTICES.md", ".gitignore", "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "app/build.gradle.kts", "app/proguard-rules.pro"]
    sources = [ROOT / name for name in allowed]
    for directory in ["app/src", "gradle", "Scripts", "Documentation", "licenses"]:
        sources += [p for p in (ROOT / directory).rglob("*") if p.is_file() and "__pycache__" not in p.parts]
    sources = sorted(set(p for p in sources if p.is_file()))
    violations = []
    suspicious = re.compile(rb"(?i)(?:client_secret|api_secret|access_token|private_key)\s*[:=]\s*[\"\'][A-Za-z0-9_+/=-]{16,}")
    personal = re.compile(rb"/(?:Volumes|Users)/[^\\s]+|BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY")
    for p in sources:
        data = p.read_bytes()
        if personal.search(data) or suspicious.search(data): violations.append("source:" + str(p.relative_to(ROOT)))
        if p.suffix.lower() in {".jks", ".keystore", ".nes", ".gba", ".iso", ".chd", ".nsp", ".xci", ".dylib"}: violations.append("forbidden source asset:" + str(p.relative_to(ROOT)))
    natives = []
    with zipfile.ZipFile(final) as apk:
        if apk.testzip() is not None: raise SystemExit("Corrupt APK ZIP entry")
        for name in apk.namelist():
            data = apk.read(name)
            if personal.search(data) or suspicious.search(data): violations.append("APK:" + name)
            if name.endswith((".nes", ".gba", ".iso", ".chd", ".nsp", ".xci", ".jks", ".keystore", ".dylib")): violations.append("forbidden APK asset:" + name)
            if name.startswith("lib/") and name.endswith(".so"):
                if not name.startswith("lib/arm64-v8a/"): violations.append("non-ARM64 library:" + name)
                alignments = []
                if data[:5] == b"\x7fELF\x02":
                    phoff = struct.unpack_from("<Q", data, 32)[0]
                    entsize, count = struct.unpack_from("<HH", data, 54)
                    for i in range(count):
                        offset = phoff + i * entsize
                        if struct.unpack_from("<I", data, offset)[0] == 1: alignments.append(struct.unpack_from("<Q", data, offset + 48)[0])
                natives.append({"path": name, "sha256": hashlib.sha256(data).hexdigest(), "load_segment_alignments": alignments})
                if not alignments or min(alignments) < 16384: violations.append("native 16KB page alignment:" + name)
    archive = OUT / f"Akito-Station-Android-v{VERSION}-Source.zip"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
        for p in sources: z.write(p, "Akito-Station-Android/" + str(p.relative_to(ROOT)))
    suites = []
    for f in (ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"):
        t = ET.parse(f).getroot()
        suites.append({k: t.attrib.get(k) for k in ["name", "tests", "failures", "errors", "skipped"]})
    instruments = []
    for f in (ROOT / "app/build/outputs/androidTest-results/connected").rglob("TEST-*.xml"):
        t = ET.parse(f).getroot()
        instruments.append({k: t.attrib.get(k) for k in ["name", "tests", "failures", "errors", "skipped"]})
    lint = ROOT / "app/build/reports/lint-results-release.xml"
    if not suites or not instruments or not lint.exists(): raise SystemExit("Required JVM, emulator UI or lint evidence missing")
    for suite in suites + instruments:
        if any(int(suite[k] or 0) for k in ["failures", "errors", "skipped"]): raise SystemExit("Required tests failed or skipped")
    issues = ET.parse(lint).getroot().findall("issue")
    lint_errors = sum(i.attrib.get("severity") in ["Error", "Fatal"] for i in issues)
    audit = {"scope": "allowlisted source archive and unsigned release APK; toolchain, caches, debug key, device data and local logs excluded", "violations": violations,
        "source_files": len(sources), "native_libraries": natives, "zip_alignment_pass": aligncode == 0, "local_review_signature_pass": signcode == 0,
        "release_owner_signed": False, "release_debuggable": "application-debuggable" in metadata,
        "apk_min_sdk": re.search(r"minSdkVersion:'([^']+)'", metadata).group(1), "apk_target_sdk": re.search(r"targetSdkVersion:'([^']+)'", metadata).group(1),
        "lint_errors": lint_errors, "lint_warnings": sum(i.attrib.get("severity") == "Warning" for i in issues), "physical_device_tested": False, "private_signing_files_remaining": [str(p.relative_to(ROOT)) for p in ROOT.rglob("*") if p.is_file() and p.suffix.lower() in {".jks", ".keystore", ".p12", ".pfx"}]}
    (OUT / "PUBLIC-RELEASE-AUDIT.json").write_text(json.dumps(audit, indent=2) + "\n")
    tests = {"jvm_suites": suites, "instrumentation_suites": instruments, "lint_errors": lint_errors, "physical_device_tested": False}
    (OUT / "TEST-RESULTS.json").write_text(json.dumps(tests, indent=2) + "\n")
    info = {"name": "Akito Station for Android", "application_id": "app.akitostation.android", "version": VERSION, "version_code": 1, "compile_sdk": 36, "target_sdk": 36, "minimum_sdk": 26,
        "release_apk": final.name, "release_sha256": sha(final), "local_review_apk": review.name, "local_review_sha256": sha(review),
        "production_signed": False, "native_abi": "arm64-v8a", "git_tag_proposed": "android-v1.0.0", "status": "ANDROID RELEASE CANDIDATE: READY FOR PHYSICAL DEVICE ACCEPTANCE"}
    (OUT / "VERSION-BUILD.json").write_text(json.dumps(info, indent=2) + "\n")
    for source, name in [("Documentation/RUNTIME_SUPPORT.md", "RUNTIME-SUPPORT.md"), ("Documentation/PHYSICAL_DEVICE_ACCEPTANCE.md", "PHYSICAL-DEVICE-VERIFICATION.md"), ("Documentation/PRIVACY.md", "PRIVACY.md"), ("THIRD_PARTY_NOTICES.md", "THIRD-PARTY-NOTICES.md")]: shutil.copyfile(ROOT / source, OUT / name)
    for source in (ROOT / "Documentation/Release").glob("*.md"): shutil.copyfile(source, OUT / source.name)
    for extra in ["PRODUCTION_SIGNING.md", "ANDROID_UPDATE_CHANNEL.md", "PARITY_1_0.md"]: shutil.copyfile(ROOT / "Documentation" / extra, OUT / extra)
    checksums = []
    for p in sorted(OUT.iterdir()):
        if p.is_file() and p.name != "SHA256SUMS.txt": checksums.append(f"{sha(p)}  {p.name}")
    (OUT / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n")
    if violations or aligncode or signcode or lint_errors or audit["release_debuggable"]: raise SystemExit("Packaging audit FAILED; inspect PUBLIC-RELEASE-AUDIT.json")
    print(json.dumps(info, indent=2))
if __name__ == "__main__": main()
