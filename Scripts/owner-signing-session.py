#!/usr/bin/env python3
"""Local terminal only: owner credentials stay in memory; no publishing."""
import getpass, hashlib, json, os, subprocess, sys
from pathlib import Path
root = Path(__file__).resolve().parent.parent
os.chdir(root)
os.umask(0o077)
if not sys.stdin.isatty() or not sys.stdout.isatty():
 raise SystemExit('Run this script directly in your local Terminal. Do not pipe or record input.')
private = root / 'signing'
if private.is_symlink(): raise SystemExit('Private signing directory must not be a symlink')
private.mkdir(exist_ok=True); private.chmod(0o700)
key = private / 'akito-android-release.jks'
if key.is_symlink(): raise SystemExit('Keystore must not be a symlink')
for location in [key, root/'production-candidate/Akito-Station-Android-v1.0.0.apk']:
 subprocess.run(['git','check-ignore','--quiet',str(location.relative_to(root))],check=True)
tracked = subprocess.run(['git','ls-files','--','signing'],capture_output=True,text=True,check=True).stdout
if tracked.strip(): raise SystemExit('Private signing material is tracked; stop and correct before signing')
java = root / '.tools/jdk-17.0.20.1+1/Contents/Home'
keytool = java / 'bin/keytool'
env = os.environ.copy(); env['JAVA_HOME'] = str(java)
print('Permanent Akito Android signing key. No uploads or publishing. Password input is hidden.')
print('Certificate identity becomes public in the APK. Enter your chosen release identity locally.')
password = getpass.getpass('Permanent keystore password (choose and retain securely): ')
if not password or len(password) < 6: raise SystemExit('keytool requires a password of at least 6 characters; choose a strong password locally.')
if not key.exists() and password != getpass.getpass('Confirm permanent password: '): raise SystemExit('Passwords differ; no key created')
env['AKITO_OWNER_STORE_PASS'] = password; env['AKITO_OWNER_KEY_PASS'] = password
try:
 if not key.exists():
  subprocess.run([str(keytool),'-genkeypair','-keystore',str(key),'-storetype','PKCS12','-alias','akito-android','-keyalg','RSA','-keysize','4096','-validity','10000','-storepass:env','AKITO_OWNER_STORE_PASS','-keypass:env','AKITO_OWNER_KEY_PASS'],env=env,check=True)
 key.chmod(0o600)
 cert = subprocess.run([str(keytool),'-exportcert','-keystore',str(key),'-alias','akito-android','-storepass:env','AKITO_OWNER_STORE_PASS'],env=env,capture_output=True,check=True).stdout
 fingerprint = hashlib.sha256(cert).hexdigest()
 record = private/'certificate-sha256.txt'
 if record.exists() and record.read_text().strip() != fingerprint: raise SystemExit('Permanent certificate fingerprint changed; stop for owner review')
 record.write_text(fingerprint+'\n'); record.chmod(0o600)
 print('Public certificate SHA-256:', fingerprint)
 final=root/'production-candidate/Akito-Station-Android-v1.0.0.apk'
 if final.exists(): raise SystemExit('Production APK already exists; preserve it and ask Codex to verify it. No output overwritten.')
 subprocess.run([sys.executable,str(root/'Scripts/sign-production.py'),'--keystore',str(key),'--alias','akito-android','--fingerprint',fingerprint],env=env,check=True)
 env.pop('AKITO_OWNER_STORE_PASS',None); env.pop('AKITO_OWNER_KEY_PASS',None); password=None
 subprocess.run([sys.executable,str(root/'Scripts/package-production.py')],env=env,check=True)
 print('Return to Codex for final review. Nothing published.')
finally:
 env.pop('AKITO_OWNER_STORE_PASS',None); env.pop('AKITO_OWNER_KEY_PASS',None); password=None
