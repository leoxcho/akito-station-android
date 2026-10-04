#!/usr/bin/env python3
"""Black-box logo regression against an installed, optimized APK; no app internals."""
import argparse, re, subprocess, xml.etree.ElementTree as ET
from pathlib import Path

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument('--package', default='app.akitostation.android.releasecheck')
args = parser.parse_args()
adb = root / '.tools/sdk/platform-tools/adb'
labels = ['Aurora', 'Classic', 'Chrome', 'Neon', 'Ice', 'Gold', 'Blueprint']
def run(*argv):
    return subprocess.run([str(adb), *argv], check=True, capture_output=True, text=True).stdout
def window():
    run('shell', 'uiautomator', 'dump', '/sdcard/akito-logo-window.xml')
    return ET.fromstring(run('exec-out', 'cat', '/sdcard/akito-logo-window.xml'))
def tap(label):
    nodes = [n for n in window().iter('node') if n.get('text') in (label, label + ' ✓')]
    assert nodes, 'Missing visible control: ' + label
    # Navigation is the final Settings node; a heading can share its label.
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', nodes[-1].get('bounds')))
    run('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
def launch():
    run('shell', 'am', 'start', '-W', '-n', args.package + '/app.akitostation.android.MainActivity')
    tap('Settings')
for label in labels:
    launch()
    tap(label)
    assert any(n.get('text') == label + ' ✓' for n in window().iter('node')), 'Selection failed: ' + label
    result = run('shell', 'cmd', 'package', 'query-activities', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER', '-p', args.package)
    aliases = re.findall(r'app\.akitostation\.android\.Logo[A-Z]+', result)
    assert aliases == ['app.akitostation.android.Logo' + label.upper()], result
    run('shell', 'am', 'force-stop', args.package)
    launch()
    assert any(n.get('text') == label + ' ✓' for n in window().iter('node')), 'Persistence failed: ' + label
    print(label + ': selection, one launcher alias and cold-start persistence PASS', flush=True)
run('shell', 'rm', '/sdcard/akito-logo-window.xml')
print('All seven optimized-release logo choices PASS')
