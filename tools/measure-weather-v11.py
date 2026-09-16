"""Analyze generated sprites and emit transparent packing instructions; does not edit bitmaps."""
from pathlib import Path
import json
import numpy as np
from PIL import Image
from importlib.machinery import SourceFileLoader
c=SourceFileLoader('calibration',str(Path(__file__).with_name('calibrate-characters.py'))).load_module()
ROOT=Path(__file__).resolve().parents[1]; DIR=ROOT/'art/weather-v11'
old=json.loads((ROOT/'art/v2/isolated-landmarks.json').read_text())
plan=json.loads((DIR/'generation-plan.json').read_text()); layouts={};measures={}
groups={'motion':list(range(12)),'social':list(range(12,24)),'personal':list(range(24,36)),'extras':list(range(36,40))+list(range(48,64))}
for item in plan:
 name=item['name']; path=DIR/'clean'/f'{name}.png'
 if not path.exists():continue
 who=0 if item['who']=='husband' else 1
 im=np.array(Image.open(path).convert('RGBA'));H,W=im.shape[:2];rows=item['rows'];entries=[]
 for slot,frame in enumerate(groups[item['group']]):
  x0=round(slot%4*W/4);x1=round((slot%4+1)*W/4)
  counts=(im[:,x0:x1,3]>160).sum(axis=1)
  def cut(row):
   if row==0:return 0
   if row==rows:return H
   nominal=round(row*H/rows);radius=round(H/rows*.22)
   lo=max(0,nominal-radius);hi=min(H,nominal+radius)
   return lo+int(np.argmin(counts[lo:hi]+np.abs(np.arange(lo,hi)-nominal)*.01))
  y0=cut(slot//4);y1=cut(slot//4+1);cell=im[y0:y1,x0:x1]; mask=cell[:,:,3]>160
  parts=sorted(c.components(mask),key=len,reverse=True)
  if not parts:raise ValueError(f'{name}:{frame} empty')
  keep=np.zeros(mask.shape,bool)
  for part in parts:
   if len(part)>max(100,len(parts[0])*.018):keep[part[:,0],part[:,1]]=True
  padded=np.pad(keep,1);keep=np.logical_or.reduce([padded[y:y+mask.shape[0],x:x+mask.shape[1]] for y in range(3) for x in range(3)])&(cell[:,:,3]>16)
  ys,xs=np.where(keep);l,t,r,b=int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)
  # Most visible skin in the upper figure is the face. The lower half contains bare knees.
  roi=[l,t,r,min(b,int(t+(b-t)*(.70 if item['rain'] else .62)))]
  center,span,box=c.face(cell,roi)
  crop=[x0+l-2,y0+t-2,r-l+4,b-t+4];runs=[]
  for yy in range(t,b):
   edges=np.diff(np.r_[False,keep[yy],False].astype(int));starts=np.where(edges==1)[0];ends=np.where(edges==-1)[0]
   runs.extend([[int(yy+y0),int(a+x0),int(z+x0)] for a,z in zip(starts,ends)])
  entries.append(dict(index=frame,crop=crop,runs=runs,face=[float(center[0]-l+2),float(center[1]-t+2)],span=span,faceBox=box,who=who))
 # Match a neutral standing pose's face-to-sole distance to the original character.
 # Including umbrellas/hoods in total bounds, or cream fabric in a skin span, shrinks the body.
 neutral={'motion':4,'social':15,'personal':26,'extras':52}[item['group']]
 base=old['frames'][f'{item["who"]}-motion:4']
 anchor=next(e for e in entries if e['index']==neutral)
 target=(base['source'][3]-base['face'][1])*old['referenceFaceDp'][who]/base['span']
 median=(anchor['crop'][3]-anchor['face'][1])*old['referenceFaceDp'][who]/target
 cw=int(np.ceil(max(e['crop'][2] for e in entries)/4)*4);ch=int(np.ceil(max(e['crop'][3] for e in entries)/4)*4)
 for slot,e in enumerate(entries):
  measures[f'w11-{name}:{e["index"]}']=dict(asset=f'w11-{name}',index=e['index'],source=[slot%4*cw,slot//4*ch,e['crop'][2],e['crop'][3]],face=e['face'],span=median,rawSpan=e['span'],who=who)
 layouts[name]=dict(cellWidth=cw,cellHeight=ch,rows=rows,frames=entries)
 print(name,len(entries),'span',round(median,1),flush=True)
(DIR/'layout.json').write_text(json.dumps(layouts,separators=(',',':')))
(DIR/'measures.json').write_text(json.dumps(dict(referenceFaceDp=old['referenceFaceDp'],frames=measures),indent=2))
