#!/usr/bin/env python3
"""Verify the existing owner's signed 1.0.3 APK. Never signs or publishes."""
import hashlib,json,os,subprocess,zipfile
from pathlib import Path
root=Path(__file__).resolve().parent.parent
signed=root/'production-candidate/Akito-Station-Android-v1.0.3.apk'
previous=root/'production-candidate/Akito-Station-Android-v1.0.2.apk'
unsigned=root/'release-candidate/Akito-Station-Android-v1.0.3-UNSIGNED.apk'
expected='babd07ffe949cdce48998f1b935778c3dba5285d69b2b48d20e67f5b6187b9b2'
if not signed.exists():raise SystemExit('Owner signing pending: run Scripts/sign-update.command privately in Terminal. No key is generated.')
env=os.environ.copy();env['JAVA_HOME']=str(root/'.tools/jdk-17.0.20.1+1/Contents/Home')
tools=root/'.tools/sdk/build-tools/36.0.0'
def run(args):return subprocess.run([str(x) for x in args],env=env,check=True,capture_output=True,text=True).stdout
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
assert sha(previous)=='dfad977724388e0c09de81e25be73ecc66e69fed8dc9ecdb76eb00801d49e3c0'
oldcert=run([tools/'apksigner','verify','--print-certs',previous])
cert=run([tools/'apksigner','verify','--verbose','--print-certs',signed])
assert 'certificate SHA-256 digest: '+expected in oldcert and 'certificate SHA-256 digest: '+expected in cert
assert 'Number of signers: 1' in cert and 'CN=Android Debug' not in cert
assert 'Verified using v2 scheme (APK Signature Scheme v2): true' in cert
assert 'Verified using v3 scheme (APK Signature Scheme v3): true' in cert
metadata=run([tools/'aapt2','dump','badging',signed])
assert all(x in metadata.splitlines()[0] for x in ["name='app.akitostation.android'","versionName='1.0.3'","versionCode='4'"])
assert 'application-debuggable' not in metadata
run([tools/'zipalign','-c','-P','16','4',signed])
with zipfile.ZipFile(unsigned) as before,zipfile.ZipFile(signed) as after:
 assert after.testzip() is None
 assert all(before.read(n)==after.read(n) for n in before.namelist())
result={'version':'1.0.3','version_code':4,'package':'app.akitostation.android','apk_sha256':sha(signed),'certificate_sha256':expected,'certificate_matches_102':True,'payload_matches_accepted_unsigned':True,'alignment_pass':True,'production_upgrade':'pending','publication':'pending'}
out=root/'production-candidate/v1.0.3';out.mkdir(parents=True,exist_ok=True)
(out/'PRODUCTION-SIGNATURE.txt').write_text(cert)
(out/'PRODUCTION-VERIFICATION.json').write_text(json.dumps(result,indent=2)+'\n')
(out/'SHA256SUMS.txt').write_text(sha(signed)+'  '+signed.name+'\n')
print(json.dumps(result,indent=2))
