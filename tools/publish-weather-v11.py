from pathlib import Path
from importlib.machinery import SourceFileLoader
import json,csv,hashlib
ROOT=Path(__file__).resolve().parents[1];ART=ROOT/'app/src/main/assets/art';DIR=ROOT/'art/weather-v11'
old=json.loads((ROOT/'art/v2/isolated-landmarks.json').read_text())
new=json.loads((DIR/'measures.json').read_text())
override=DIR/'reviewed-spans.json'
reviewed=json.loads(override.read_text()) if override.exists() else {}
for key,record in new['frames'].items():
 record['span']=reviewed.get(key,reviewed.get(record['asset'],record['span']))
audit=SourceFileLoader('audit',str(Path(__file__).with_name('audit-character-scale.py'))).load_module()
records={};routes=[]
for who in range(2):
 for outfit in ('default','cold','hot','night'):
  for rain in (False,True):
   for frame in range(64):
    person=('husband','wife')[who];group=('motion' if frame<12 else 'social' if frame<24 else 'personal' if frame<36 else 'extras')
    if frame not in range(40,48) and rain and frame not in range(8,12):key=f'w11-{person}-rain-{outfit}-{group}:{frame}'
    elif frame not in range(40,48) and outfit!='default':key=f'w11-{person}-{outfit}-{group}:{frame}'
    else:
     name,cols,rows,index,mode=audit.route(who,outfit,rain,frame);key=f'{name}:{index}'
    records[key]=(new['frames'] if key.startswith('w11-') else old['frames'])[key]
    routes.append(dict(who=who,outfit=outfit,rain=rain,frame=frame,asset=key))
hashes={name:hashlib.sha256((ART/'calibrated'/f'{name}.png').read_bytes()).hexdigest() for name in {r['asset'] for r in records.values()}}
assert len(records)==910 and len(hashes)==75
with (ART/'character-measures.csv').open('w',newline='',encoding='utf-8') as f:
 w=csv.writer(f);w.writerow(['asset','index','x','y','width','height','faceX','faceY','faceSpan','referenceSpan'])
 for r in records.values():w.writerow([r['asset'],r['index'],*r['source'],*r['face'],r['span'],new['referenceFaceDp'][r['who']]])
(DIR/'published.json').write_text(json.dumps(dict(version='0.11.0',referenceFaceDp=new['referenceFaceDp'],frames=records,hashes=hashes,routes=routes),indent=2))
print(len(records),'unique runtime frames',len(hashes),'atlases',len(routes),'routes')
