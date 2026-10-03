#!/usr/bin/env python3
"""Audit/stage Android 1.0.1 locally. Never signs, generates keys, commits or publishes."""
import hashlib,json,re,shutil,struct,subprocess,zipfile,os
import xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parent.parent
out=root/'release-candidate/v1.0.1';out.mkdir(parents=True,exist_ok=True)
source=root/'app/build/outputs/apk/release/app-release-unsigned.apk'
env=os.environ.copy();env['JAVA_HOME']=str(root/'.tools/jdk-17.0.20.1+1/Contents/Home')
tools=root/'.tools/sdk/build-tools/36.0.0'
def run(args): return subprocess.run([str(x) for x in args],env=env,capture_output=True,text=True,check=True).stdout
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
metadata=run([tools/'aapt2','dump','badging',source])
assert all(x in metadata.splitlines()[0] for x in ["name='app.akitostation.android'","versionCode='2'","versionName='1.0.1'"])
assert 'application-debuggable' not in metadata
run([tools/'zipalign','-c','-P','16','4',source])
suites=[]
for f in (root/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
 t=ET.parse(f).getroot();suites.append({k:t.get(k) for k in ['name','tests','failures','errors','skipped']})
assert suites and any('CoverSearchTest' in s['name'] for s in suites)
assert all(int(s.get(k) or 0)==0 for s in suites for k in ['failures','errors','skipped'])
lint=ET.parse(root/'app/build/reports/lint-results-release.xml').getroot().findall('issue')
assert not any(i.get('severity') in ['Error','Fatal'] for i in lint)
allowed=['README.md','STATUS.md','THIRD_PARTY_NOTICES.md','.gitignore','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat','app/build.gradle.kts','app/proguard-rules.pro']
files=[root/p for p in allowed if (root/p).is_file()]
for directory in ['app/src','gradle','Scripts','Documentation','licenses']:
 files += [p for p in (root/directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts]
files=sorted(set(files))
secret=re.compile(rb'BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY|/(?:Volumes|Users)/[^\s]+|(?i:client_secret|api_secret|access_token)\s*[:=]\s*["\'][A-Za-z0-9_+/=-]{16,}')
forbidden={'.jks','.keystore','.p12','.pfx','.pem','.nes','.sfc','.smc','.gba','.gb','.gbc','.iso','.chd','.nsp','.xci','.dylib'}
violations=[];natives=[]
for p in files:
 if p.suffix.lower() in forbidden or secret.search(p.read_bytes()): violations.append('source:'+str(p.relative_to(root)))
with zipfile.ZipFile(source) as apk:
 assert not apk.testzip()
 for name in apk.namelist():
  data=apk.read(name)
  if Path(name).suffix.lower() in forbidden or secret.search(data):violations.append('apk:'+name)
  if name.startswith('lib/') and name.endswith('.so'):
   assert name.startswith('lib/arm64-v8a/') and data[:5]==b'\x7fELF\x02'
   phoff=struct.unpack_from('<Q',data,32)[0];size,count=struct.unpack_from('<HH',data,54)
   aligns=[struct.unpack_from('<Q',data,phoff+i*size+48)[0] for i in range(count) if struct.unpack_from('<I',data,phoff+i*size)[0]==1]
   assert aligns and min(aligns)>=16384
   natives.append({'path':name,'load_alignment':aligns})
assert natives and not violations,violations
unsigned=root/'release-candidate/Akito-Station-Android-v1.0.1-UNSIGNED.apk';shutil.copyfile(source,unsigned)
archive=out/'Akito-Station-Android-v1.0.1-Source.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in files:z.write(p,'Akito-Station-Android/'+str(p.relative_to(root)))
 for name in ['LICENSE','LICENSE_SCOPE.json']:
  p=root/name
  if not p.exists():p=root/name
  if not p.exists():p=root/'production-candidate/public-release/repository'/name
  if p.exists():z.write(p,'Akito-Station-Android/'+name)
audit={'version':'1.0.1','version_code':2,'package':'app.akitostation.android','unsigned_apk_sha256':sha(unsigned),'source_sha256':sha(archive),'source_files':len(files),'violations':violations,'native_libraries':natives,'alignment_pass':True,'debuggable':False,'jvm_tests':sum(int(s['tests']) for s in suites),'jvm_suites':suites,'lint_errors':0,'lint_warnings':sum(i.get('severity')=='Warning' for i in lint),'production_signature':'pending','production_upgrade':'pending','publication':'blocked until all gates pass'}
(out/'PUBLIC-RELEASE-AUDIT.json').write_text(json.dumps(audit,indent=2)+'\n')
(out/'SHA256SUMS.txt').write_text(f'{sha(unsigned)}  {unsigned.name}\n{sha(archive)}  {archive.name}\n')
print(json.dumps(audit,indent=2))
