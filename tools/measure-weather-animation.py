"""Measure generated frames and select foreground pixel runs; never edits bitmap pixels."""
from pathlib import Path
import json,sys
import numpy as np
from PIL import Image
from importlib.machinery import SourceFileLoader
c=SourceFileLoader('calibration',str(Path(__file__).with_name('calibrate-characters.py'))).load_module()
ROOT=Path(__file__).resolve().parents[1];DIR=ROOT/'art/weather-v10'
old=json.loads((ROOT/'art/v2/isolated-landmarks.json').read_text(encoding='utf-8'))
frames=list(range(40))+list(range(48,64));layouts={};measures={}
for record in json.loads((DIR/'generation.json').read_text(encoding='utf-8-sig')):
 name=record['name'];who=0 if name.startswith('husband') else 1
 im=np.array(Image.open(DIR/'clean'/f'{name}.png').convert('RGBA'));H,W=im.shape[:2]
 entries=[]
 for slot,frame in enumerate(frames):
  x0=round(slot%8*W/8);x1=round((slot%8+1)*W/8)
  counts=(im[:,x0:x1,3]>160).sum(axis=1)
  def cut(row):
   if row==0:return 0
   if row==7:return H
   nominal=round(row*H/7);radius=round(H/7*.23)
   lo=max(0,nominal-radius);hi=min(H,nominal+radius)
   return lo+int(np.argmin(counts[lo:hi]+np.abs(np.arange(lo,hi)-nominal)*.01))
  y0=cut(slot//8);y1=cut(slot//8+1)
  cell=im[y0:y1,x0:x1];mask=cell[:,:,3]>160
  components=sorted(c.components(mask),key=len,reverse=True)
  if not components: raise ValueError(f'{name}:{frame} empty')
  keep=np.zeros(mask.shape,bool)
  # A detached canopy can be the second large component; discard small export debris.
  for part in components:
   if len(part)>max(80,len(components[0])*.025):keep[part[:,0],part[:,1]]=True
  padded=np.pad(keep,1);keep=np.logical_or.reduce([padded[y:y+mask.shape[0],x:x+mask.shape[1]] for y in range(3) for x in range(3)])&(cell[:,:,3]>16)
  ys,xs=np.where(keep);l,t,r,b=int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)
  # Restrict skin detection to the face expected from the pose, excluding knees/hands.
  if frame>=48:key=f'{("husband","wife")[who]}-idle:{frame-48}'
  elif frame>=36:key=f'kiss:{who*4+frame-36}'
  else:key=f'{("husband","wife")[who]}-{("motion","social","personal")[frame//12]}:{frame%12}'
  ref=old['frames'][key];rs=old['referenceFaceDp'][who]/ref['span']*2
  expectedX=(128-ref['source'][2]*rs/2+ref['face'][0]*rs)*W/2048
  expectedY=(300-ref['source'][3]*rs+ref['face'][1]*rs)*H/2240+slot//8*H/7-y0
  if '-rain-' in name: expectedY=max(t+35,expectedY)
  radius=(x1-x0)*.26
  roi=[max(0,int(expectedX-radius)),max(t,int(expectedY-radius)),min(x1-x0,int(expectedX+radius)),min(y1-y0,int(expectedY+radius))]
  try: center,span,box=c.face(cell,roi)
  except (IndexError,ValueError):center,span,box=c.face(cell)
  crop=[x0+l-2,y0+t-2,r-l+4,b-t+4]
  runs=[]
  for yy in range(t,b):
   row=keep[yy];edges=np.diff(np.r_[False,row,False].astype(int));starts=np.where(edges==1)[0];ends=np.where(edges==-1)[0]
   runs.extend([[int(yy+y0),int(a+x0),int(z+x0)] for a,z in zip(starts,ends)])
  entries.append(dict(index=frame,crop=crop,runs=runs,face=[float(center[0]-l+2),float(center[1]-t+2)],span=span,faceBox=box,who=who))
 median=float(np.median([e['span'] for e in entries]))
 cw=int(np.ceil(max(e['crop'][2] for e in entries)/4)*4);ch=int(np.ceil(max(e['crop'][3] for e in entries)/4)*4)
 for slot,e in enumerate(entries):
  # Generated sheets preserve head scale across slots. One robust sheet scale avoids
  # enlarging a figure when a hand/closed eye changes the visible skin measurement.
  measured=e['span'];e['span']=median
  measures[f'w10-{name}:{e["index"]}']=dict(asset=f'w10-{name}',index=e['index'],source=[slot%8*cw,slot//8*ch,e['crop'][2],e['crop'][3]],face=e['face'],span=e['span'],rawSpan=measured,who=who)
 layouts[name]=dict(cellWidth=cw,cellHeight=ch,rows=7,frames=entries)
 print(name,56,'frames, median face span',round(median,2))
(DIR/'layout.json').write_text(json.dumps(layouts,separators=(',',':')),encoding='utf-8')
(DIR/'measures.json').write_text(json.dumps(dict(referenceFaceDp=old['referenceFaceDp'],frames=measures),indent=2),encoding='utf-8')
