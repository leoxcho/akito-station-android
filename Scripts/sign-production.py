#!/usr/bin/env python3
"""Owner-operated offline APK signing. Never generates keys or uploads files."""
import argparse, getpass, os, subprocess, tempfile
from pathlib import Path
root = Path(__file__).resolve().parent.parent
p = argparse.ArgumentParser()
p.add_argument('--keystore', type=Path, required=True)
p.add_argument('--alias', default='akito-android')
p.add_argument('--fingerprint', required=True, help='Expected SHA-256 certificate fingerprint from owner records')
a = p.parse_args()
source = root / 'release-candidate/Akito-Station-Android-v1.0.0-UNSIGNED.apk'
tools = root / '.tools/sdk/build-tools/36.0.0'
env = os.environ.copy()
env['JAVA_HOME'] = str(root / '.tools/jdk-17.0.20.1+1/Contents/Home')
env['AKITO_OWNER_STORE_PASS'] = env.get('AKITO_OWNER_STORE_PASS') or getpass.getpass('Keystore password: ')
env['AKITO_OWNER_KEY_PASS'] = env.get('AKITO_OWNER_KEY_PASS') or getpass.getpass('Key password: ')
expected = a.fingerprint.replace(':', '').lower()
if len(expected) != 64 or any(c not in '0123456789abcdef' for c in expected): p.error('Expected fingerprint must be 64 hexadecimal digits')
if not source.exists() or not a.keystore.is_file(): p.error('Unsigned candidate or owner keystore missing')
out = root / 'production-candidate'; out.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(dir=out) as temp:
 signed = Path(temp) / 'signed.apk'
 subprocess.run([str(tools/'apksigner'), 'sign', '--ks', str(a.keystore.resolve()), '--ks-key-alias', a.alias, '--ks-pass', 'env:AKITO_OWNER_STORE_PASS', '--key-pass', 'env:AKITO_OWNER_KEY_PASS', '--out', str(signed), str(source)], env=env, check=True)
 result = subprocess.run([str(tools/'apksigner'), 'verify', '--verbose', '--print-certs', str(signed)], env=env, capture_output=True, text=True, check=True)
 if 'CN=Android Debug' in result.stdout or f'certificate SHA-256 digest: {expected}' not in result.stdout: raise SystemExit('Rejected: development certificate or unexpected owner fingerprint')
 subprocess.run([str(tools/'zipalign'), '-c', '-P', '16', '4', str(signed)], check=True)
 final = out/'Akito-Station-Android-v1.0.0.apk'
 if final.exists(): raise SystemExit('Output already exists; preserve/review it before another signing attempt')
 signed.rename(final)
 final.chmod(0o600)
 (out/'SIGNATURE.txt').write_text(result.stdout)
 print('Owner-signed candidate prepared locally. Device acceptance and publication review remain required.')
