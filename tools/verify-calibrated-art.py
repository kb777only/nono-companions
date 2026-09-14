"""Verify packaged pixels/metadata; JVM tests independently exercise the real selector."""
from pathlib import Path
import csv, json, hashlib, math
from PIL import Image
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'app/src/main/assets/art'
rows=list(csv.DictReader((ART/'character-measures.csv').open(encoding='utf-8')))
weather=any(r['asset'].startswith('w10-') for r in rows)
landmarks=json.loads((ROOT/('art/weather-v10/published.json' if weather else 'art/v2/isolated-landmarks.json')).read_text(encoding='utf-8'))
images={name:np.array(Image.open(ART/'calibrated'/f'{name}.png').convert('RGBA')) for name in landmarks['hashes']}
failures=[]; results=[]
for name,digest in landmarks['hashes'].items():
    if hashlib.sha256((ART/'calibrated'/f'{name}.png').read_bytes()).hexdigest()!=digest:
        failures.append(f'{name}: hash changed; repeat visual review')
for row in rows:
    key=f"{row['asset']}:{row['index']}"
    x,y,w,h=[int(row[k]) for k in ('x','y','width','height')]
    scale=float(row['referenceSpan'])/float(row['faceSpan'])
    alpha=images[row['asset']][y:y+h,x:x+w,3]
    if alpha.shape!=(h,w) or not np.any(alpha>40): failures.append(f'{key}: invalid/empty crop')
    if any(np.any(edge>40) for edge in (alpha[0],alpha[-1],alpha[:,0],alpha[:,-1])):
        failures.append(f'{key}: artwork touches crop boundary')
    top=-h*scale/2 if row['asset']=='peeking' else 50-h*scale
    if w*scale>160 or top < -120 or top+h*scale>120: failures.append(f'{key}: overflow clipping')
    original=landmarks['frames'][key]
    if original['source']!=[x,y,w,h] or abs(original['span']-float(row['faceSpan']))>1e-5:
        failures.append(f'{key}: metadata drift')
    results.append(dict(frame=key,scale=scale,widthDp=w*scale,heightDp=h*scale,topDp=top))
assert len(rows)==(910 if weather else 296) and len(images)==(33 if weather else 19)
report=dict(version='0.10.0' if weather else '0.9.1',frames=len(rows),atlases=len(images),failures=failures,
    scope='Packaged asset integrity, alpha crop margins, calibrated drawing bounds. Runtime routes/rotation/density are covered by CharacterGeometryTest. Perceived scale also requires visual review.',
    assetHashes=landmarks['hashes'],measurements=results)
(ROOT/('docs/scale-audit/weather-verification.json' if weather else 'docs/scale-audit/calibrated-verification.json')).write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(dict(frames=len(rows),atlases=len(images),failures=failures)))
raise SystemExit(bool(failures))
