from pathlib import Path
import json,csv,hashlib
ROOT=Path(__file__).resolve().parents[1]; ART=ROOT/'app/src/main/assets/art'
raw=json.loads((ROOT/'art/v2/character-landmarks.json').read_text(encoding='utf-8'))
isolated=json.loads((ROOT/'art/v2/isolated-landmarks.json').read_text(encoding='utf-8'))
layout=json.loads((ROOT/'art/v2/isolation-layout.json').read_text(encoding='utf-8'))
for key,r in isolated['frames'].items():
    original=raw['frames'][key]
    crop=next(f['crop'] for f in layout[r['asset']]['frames'] if f['index']==r['index'])
    r['face']=[original['source'][0]+original['face'][0]-crop[0],original['source'][1]+original['face'][1]-crop[1]]
    r['span']=original['span']
isolated['referenceFaceDp']=raw['referenceFaceDp']
isolated['hashes']={n:hashlib.sha256((ART/'calibrated'/f'{n}.png').read_bytes()).hexdigest() for n in raw['hashes']}
(ROOT/'art/v2/isolated-landmarks.json').write_text(json.dumps(isolated,indent=2),encoding='utf-8')
with (ART/'character-measures.csv').open('w',newline='',encoding='utf-8') as f:
    writer=csv.writer(f);writer.writerow(['asset','index','x','y','width','height','faceX','faceY','faceSpan','referenceSpan'])
    for r in isolated['frames'].values(): writer.writerow([r['asset'],r['index'],*r['source'],*r['face'],r['span'],isolated['referenceFaceDp'][r['who']]])
print('Published all 296 calibrated frame records')
