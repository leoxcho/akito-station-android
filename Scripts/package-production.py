#!/usr/bin/env python3
"""Verify and stage owner-signed Android assets locally. Never publishes."""
import hashlib, json, os, re, shutil, subprocess, sys, zipfile
from pathlib import Path
root=Path(__file__).resolve().parent.parent
os.chdir(root)
name='Akito-Station-Android-v1.0.0.apk'
apk=root/'production-candidate'/name
fingerprint=(root/'signing/certificate-sha256.txt').read_text().strip()
if not re.fullmatch('[a-f0-9]{64}',fingerprint): raise SystemExit('Owner fingerprint record invalid')
env=os.environ.copy()
for variable in ['AKITO_OWNER_STORE_PASS','AKITO_OWNER_KEY_PASS']:env.pop(variable,None)
env['JAVA_HOME']=str(root/'.tools/jdk-17.0.20.1+1/Contents/Home')
tools=root/'.tools/sdk/build-tools/36.0.0'
def run(args):
 result=subprocess.run([str(x) for x in args],env=env,capture_output=True,text=True,check=True)
 return result.stdout+result.stderr
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
signature=run([tools/'apksigner','verify','--verbose','--print-certs',apk])
if 'CN=Android Debug' in signature or f'certificate SHA-256 digest: {fingerprint}' not in signature:raise SystemExit('Production certificate verification failed')
if 'Number of signers: 1' not in signature:raise SystemExit('Unexpected signer count')
alignment=run([tools/'zipalign','-c','-P','16','-v','4',apk])
metadata=run([tools/'aapt2','dump','badging',apk])
for expected in ["name='app.akitostation.android'","versionCode='1'","versionName='1.0.0'"]:
 if expected not in metadata.splitlines()[0]:raise SystemExit('APK identity mismatch')
if 'application-debuggable' in metadata:raise SystemExit('Release is debuggable')
unsigned=root/'release-candidate/Akito-Station-Android-v1.0.0-UNSIGNED.apk'
with zipfile.ZipFile(unsigned) as original,zipfile.ZipFile(apk) as signed:
 if signed.testzip():raise SystemExit('Corrupt signed APK')
 for entry in original.namelist():
  if signed.read(entry)!=original.read(entry):raise SystemExit('Signed payload differs from accepted unsigned build')
 native=[n for n in signed.namelist() if n.startswith('lib/') and n.endswith('.so')]
 if not native or any(not n.startswith('lib/arm64-v8a/') for n in native):raise SystemExit('ARM64 verification failed')
checksum=sha(apk)
release=root/'Documentation/Release'
(release/'INSTALLATION.md').write_text('''# Install Akito Station Android 1.0.0\n\nUse Akito-Station-Android-v1.0.0.apk, the owner production-signed build for Android 8+ ARM64. Verify SHA256SUMS.txt and the certificate fingerprint in VERSION-BUILD.json. Allow installation from your chosen file manager/browser and follow Android's installer. Production updates must use the same permanent signing certificate and an increasing versionCode.\n\nDevelopment installs use another certificate and cannot upgrade directly to production. Uninstalling a development install removes private metadata/imported covers; preserve original game folders and reimport. Do not uninstall an existing production installation to test a development APK.\n\nChoose library folders using Android's document picker. Install compatible runtimes independently and select them explicitly in Consoles. No games, BIOS, firmware, keys or emulator binaries are included. See RUNTIME-SUPPORT.md and PRIVACY.md.\n''')
(release/'PROPOSED-GITHUB-RELEASE.md').write_text(f'''# Prepared GitHub release — awaiting owner approval\n\nRepository: leoxcho/akito-station-android\nTag: android-v1.0.0\nTitle: Akito Station Android 1.0.0\nProduction APK: {name}\nAPK SHA-256: {checksum}\nProduction certificate SHA-256: {fingerprint}\n\nNative Android library and external-runtime frontend: SAF import, editable artwork/title/system, favorites/search/filtering, persistent settings and controller navigation. Twelve compatible app adapters; no emulator/game/BIOS/key payload bundled. Physical hardware acceptance confirmed by owner; no individual runtime gameplay results inferred from that general confirmation.\n\nPublish only files listed in PUBLICATION-MANIFEST.json after owner approval. LOCAL-REVIEW, UNSIGNED, signing material, toolchain and local logs are excluded. Nothing has been published by the preparation scripts.\n''')
report=f'''# Android production release verification\n\nVersion 1.0.0; versionCode 1; package app.akitostation.android; Android 8+ ARM64.\n\nProduction APK: {name}\nAPK SHA-256: {checksum}\nProduction certificate SHA-256: {fingerprint}\n\nOwner confirmed physical-device acceptance in this session. This records the owner's general acceptance, not independent per-runtime gameplay verification. Signed payload exactly matches the previously tested unsigned release build. Prior validation: clean build, 85 JVM tests, phone 3/3 and tablet 3/3 Compose UI tests; lint zero errors and 20 non-blocking warnings.\n\nFinal production verification: APK signature and expected certificate PASS; no Android Debug certificate; single signer; 16 KB ZIP alignment PASS; ARM64-only packaging PASS; version/package/non-debuggable PASS; source and release content audit PASS. Secrets/signing/cache/game payloads excluded by allowlist and .gitignore. Permanent private key remains only in ignored signing/akito-android-release.jks. Keep an encrypted offline backup under owner control. LOCAL-REVIEW is internal only and excluded from public staging.\n\nNo macOS project writes were issued. No publishing, uploads, remote changes or GitHub releases were performed. The official Android channel is prepared for leoxcho/akito-station-android. Public availability/update checks require later owner-approved publication. PRO and broader desktop/runtime parity remain optional post-1.0.\n\nSee PUBLICATION-MANIFEST.json for the exact repository files and release assets. Existing third-party license texts and owner-controlled branding/application notices are preserved; no new license grant is invented. Final owner approval is required before publication.\n\nANDROID PRODUCTION RELEASE: READY TO PUBLISH\n'''
(release/'RELEASE-REPORT.md').write_text(report)
(release/'KNOWN-LIMITATIONS.md').write_text('''# Known limitations\n\nOwner-confirmed physical acceptance and production signing are recorded. Individual adapter gameplay/save/controller matrices were not independently verified by Codex. Multiple-file disc dependencies, runtime-owned saves/BIOS setup and SAF provider behavior remain runtime/device-specific. PRO, online art scraping, cloud backup, embedded rendering and desktop save/profile management are post-1.0. App uninstall removes private metadata/covers while original games and external emulator saves are preserved. Updates open Android release review pages and do not automatically download/install. Publication awaits owner approval.\n''')
(root/'STATUS.md').write_text('''# Android production release\n\nProduction key/signature and physical owner acceptance completed. Public staging is local under production-candidate/public-release. No publication authorized yet. No macOS writes. See Documentation/Release/RELEASE-REPORT.md.\n''')
p=root/'Documentation/PRODUCTION_SIGNING.md'
s=p.read_text().replace('No production key has been created.','The permanent production key is stored privately in ignored signing/akito-android-release.jks. Its public fingerprint is recorded in the production release metadata.')
p.write_text(s)
p=root/'README.md';s=p.read_text();s=s.replace('The temporary development signing key used for local review was removed after validation.', 'The final public APK is owner production-signed; the permanent private key is never distributed. Physical-device acceptance was confirmed by the owner. The temporary development signing key used for local review was removed after validation.');p.write_text(s)
# Reuse the existing allowlisted source/ELF/secret/test audit. Never replace signed APK with LOCAL-REVIEW.
subprocess.run([sys.executable,str(root/'Scripts/package-candidate.py')],env=env,stdout=subprocess.DEVNULL,check=True)
audit=json.loads((root/'release-candidate/PUBLIC-RELEASE-AUDIT.json').read_text())
if audit['violations'] or audit['lint_errors'] or not audit['zip_alignment_pass']:raise SystemExit('Underlying public-content audit failed')
archive=root/'release-candidate/Akito-Station-Android-v1.0.0-Source.zip'
staging=root/'production-candidate/public-release'
if staging.exists():raise SystemExit('Public staging already exists; preserve/review before replacing it')
staging.mkdir(mode=0o700)
repo=staging/'repository';assets=staging/'release-assets';repo.mkdir();assets.mkdir()
with zipfile.ZipFile(archive) as z:
 for entry in z.infolist():
  relative=Path(entry.filename).relative_to('Akito-Station-Android')
  if relative.is_absolute() or '..' in relative.parts:raise SystemExit('Unsafe source archive path')
  target=repo/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(entry))
shutil.copyfile(apk,assets/name);shutil.copyfile(archive,assets/archive.name)
for p in release.glob('*.md'):shutil.copyfile(p,assets/p.name)
for source,target in [('Documentation/PRIVACY.md','PRIVACY.md'),('THIRD_PARTY_NOTICES.md','THIRD-PARTY-NOTICES.md'),('Documentation/RUNTIME_SUPPORT.md','RUNTIME-SUPPORT.md')]:shutil.copyfile(root/source,assets/target)
info={'application_id':'app.akitostation.android','version':'1.0.0','version_code':1,'production_signed':True,'apk':name,'apk_sha256':checksum,'certificate_sha256':fingerprint,'native_abi':'arm64-v8a','physical_acceptance':'owner-confirmed','published':False}
(assets/'VERSION-BUILD.json').write_text(json.dumps(info,indent=2)+'\n')
(assets/'PRODUCTION-SIGNATURE.txt').write_text(signature)
(assets/'APK-ALIGNMENT.txt').write_text(alignment.replace(str(root),'<worktree>'))
(assets/'APK-METADATA.txt').write_text(metadata.replace(str(root),'<worktree>'))
audit.update(release_owner_signed=True,expected_certificate_sha256=fingerprint,signature_pass=True,development_certificate=False,accepted_unsigned_payload_match=True,physical_device_acceptance='owner-confirmed',private_material_excluded=True,published=False)
audit.pop('private_signing_files_remaining',None)
(assets/'PUBLIC-RELEASE-AUDIT.json').write_text(json.dumps(audit,indent=2)+'\n')
for p in [root/'release-candidate/TEST-RESULTS.json',root/'release-candidate/UI-ACCEPTANCE.json']:shutil.copyfile(p,assets/p.name)
forbidden={'.jks','.keystore','.p12','.pfx','.pem','.nes','.sfc','.smc','.gba','.gb','.gbc','.iso','.chd','.nsp','.xci','.dylib'}
secret=re.compile(rb'BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY|/(?:Volumes|Users)/[^\s]+|(?i:client_secret|api_secret|access_token)\s*[:=]\s*["\'][A-Za-z0-9_+/=-]{16,}')
for p in staging.rglob('*'):
 if not p.is_file():continue
 if p.suffix.lower() in forbidden or 'LOCAL-REVIEW' in p.name or 'UNSIGNED' in p.name:raise SystemExit('Forbidden public file: '+str(p.relative_to(staging)))
 if p.suffix.lower() not in {'.apk','.zip','.png'} and secret.search(p.read_bytes()):raise SystemExit('Private content in public file: '+str(p.relative_to(staging)))
manifest={'repository':'leoxcho/akito-station-android','tag':'android-v1.0.0','published':False,'repository_files':sorted(str(p.relative_to(repo)) for p in repo.rglob('*') if p.is_file()),'release_assets':sorted(str(p.relative_to(assets)) for p in assets.rglob('*') if p.is_file())+['PUBLICATION-MANIFEST.json','SHA256SUMS.txt']}
(assets/'PUBLICATION-MANIFEST.json').write_text(json.dumps(manifest,indent=2)+'\n')
(assets/'SHA256SUMS.txt').write_text(''.join(f'{sha(p)}  {p.relative_to(assets)}\n' for p in sorted(assets.rglob('*')) if p.is_file() and p.name!='SHA256SUMS.txt'))
(root/'production-candidate/FINAL-VERIFICATION.json').write_text(json.dumps(info|{'audit_pass':True,'staging':'public-release','status':'ANDROID PRODUCTION RELEASE: READY TO PUBLISH'},indent=2)+'\n')
print('Production verification and public staging complete. Nothing published.')
print('Public APK SHA-256:',checksum)
print('Public certificate SHA-256:',fingerprint)
