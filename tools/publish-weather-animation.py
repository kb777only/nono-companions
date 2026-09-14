from pathlib import Path
import json,csv,hashlib
from importlib.machinery import SourceFileLoader
ROOT=Path(__file__).resolve().parents[1];ART=ROOT/'app/src/main/assets/art'
old=json.loads((ROOT/'art/v2/isolated-landmarks.json').read_text(encoding='utf-8'))
new=json.loads((ROOT/'art/weather-v10/measures.json').read_text(encoding='utf-8'))
reviewed=json.loads((ROOT/'art/weather-v10/reviewed-sheet-spans.json').read_text(encoding='utf-8'))
for record in new['frames'].values():
 if record['asset'][4:] in reviewed: record['span']=reviewed[record['asset'][4:]]
audit=SourceFileLoader('audit',str(Path(__file__).with_name('audit-character-scale.py'))).load_module()
records={};routes=[]
for who in range(2):
 for outfit in ('default','cold','hot','night'):
  for rain in (False,True):
   for frame in range(64):
    person=('husband','wife')[who]
    if frame not in range(40,48) and rain and frame not in range(8,12):key=f'w10-{person}-rain-{outfit}:{frame}'
    elif frame not in range(40,48) and outfit!='default':key=f'w10-{person}-{outfit}:{frame}'
    else:
     name,cols,rows,index,mode=audit.route(who,outfit,rain,frame);key=f'{name}:{index}'
    records[key]=(new['frames'] if key.startswith('w10-') else old['frames'])[key]
    routes.append(dict(who=who,outfit=outfit,rain=rain,frame=frame,asset=key))
hashes={name:hashlib.sha256((ART/'calibrated'/f'{name}.png').read_bytes()).hexdigest() for name in {r['asset'] for r in records.values()}}
with (ART/'character-measures.csv').open('w',newline='',encoding='utf-8') as f:
 w=csv.writer(f);w.writerow(['asset','index','x','y','width','height','faceX','faceY','faceSpan','referenceSpan'])
 for r in records.values():w.writerow([r['asset'],r['index'],*r['source'],*r['face'],r['span'],new['referenceFaceDp'][r['who']]])
report=dict(version='0.10.0',referenceFaceDp=new['referenceFaceDp'],frames=records,hashes=hashes,routes=routes)
(ROOT/'art/weather-v10/published.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(len(records),'unique runtime frames',len(hashes),'atlases',len(routes),'routes')
