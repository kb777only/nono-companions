"""Offline landmark extraction and atlas sampling metadata; does not rewrite artwork."""
from pathlib import Path
import sys, json, hashlib, csv
import numpy as np
from PIL import Image
sys.path.insert(0,str(Path(__file__).parent))
from importlib.machinery import SourceFileLoader
audit=SourceFileLoader('audit',str(Path(__file__).with_name('audit-character-scale.py'))).load_module()
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'app/src/main/assets/art'

def components(mask):
    seen=mask.copy(); result=[]
    for y,x in zip(*np.where(mask)):
        if not seen[y,x]: continue
        todo=[(int(y),int(x))]; seen[y,x]=False; coords=[]
        while todo:
            yy,xx=todo.pop(); coords.append((yy,xx))
            for ny,nx in ((yy-1,xx),(yy+1,xx),(yy,xx-1),(yy,xx+1)):
                if 0<=ny<seen.shape[0] and 0<=nx<seen.shape[1] and seen[ny,nx]:
                    seen[ny,nx]=False; todo.append((ny,nx))
        if len(coords)>12: result.append(np.array(coords))
    return result

def face(pixels,roi=None):
    r,g,b,a=np.moveaxis(pixels.astype(np.int16),2,0)
    mask=(a>180)&(r>165)&(g>75)&(b>35)&(r-g>35)&(g-b>15)
    if roi:
        keep=np.zeros(mask.shape,dtype=bool); l,t,rr,bb=roi; keep[t:bb,l:rr]=True; mask &= keep
    # Close small gaps made by eyes/eyebrows without joining distant hands.
    padded=np.pad(mask,2)
    dilated=np.logical_or.reduce([padded[y:y+mask.shape[0],x:x+mask.shape[1]] for y in range(5) for x in range(5)])
    candidates=components(dilated)
    candidates.sort(key=lambda c:len(c),reverse=True)
    c=candidates[0]
    points=c[:,::-1].astype(float)
    center=points.mean(axis=0)
    covariance=np.cov(points.T); values,vectors=np.linalg.eigh(covariance)
    projection=(points-center)@vectors[:,-1]
    span=float(np.quantile(projection,.995)-np.quantile(projection,.005))
    return center,span,[int(c[:,1].min()),int(c[:,0].min()),int(c[:,1].max()+1),int(c[:,0].max()+1)]

def run():
    records={}; images={}
    for who in range(2):
      for outfit in ('default','cold','hot','night'):
       for rain in (False,True):
        for frame in range(64):
          name,cols,rows,index,mode=audit.route(who,outfit,rain,frame)
          key=f'{name}:{index}'
          if key in records: continue
          if name not in images: images[name]=np.array(Image.open(ART/(name+'.png')).convert('RGBA'))
          im=images[name]; h,w=im.shape[:2]; cw=w//cols; ch=h//rows
          col=index%cols; row=index//cols
          # Find the least occupied horizontal cut near each nominal boundary.
          # This captures complete generated heads/boots when rows drift.
          strip=im[:,col*cw:(col+1)*cw]
          counts=(strip[:,:,3]>40).sum(axis=1)
          def cut(n):
              if n==0: return 0
              if n==rows: return h
              expected=n*ch; radius=int(ch*.16)
              lo=max(0,expected-radius); hi=min(h,expected+radius)
              score=counts[lo:hi].astype(float)+np.abs(np.arange(lo,hi)-expected)*.015
              return lo+int(np.argmin(score))
          top=cut(row); bottom=cut(row+1)
          cell=strip[top:bottom]
          ys,xs=np.where(cell[:,:,3]>40)
          left,right=int(xs.min()),int(xs.max()+1); low,high=int(ys.min()),int(ys.max()+1)
          center,span,box=face(cell)
          override_path=ROOT/'art/v2/face-regions.json'
          overrides=json.loads(override_path.read_text(encoding='utf-8')) if override_path.exists() else {}
          if key in overrides:
              x0,y0,x1,y1=overrides[key]
              center,span,box=face(cell,[x0-col*cw,max(0,y0-top),x1-col*cw,min(bottom-top,y1-top)])
          scale_path=ROOT/'art/v2/reviewed-face-spans.json'
          reviewed=json.loads(scale_path.read_text(encoding='utf-8')) if scale_path.exists() else {}
          if key in reviewed: span=reviewed[key]
          records[key]=dict(asset=name,index=index,who=who,source=[col*cw+left,top+low,right-left,high-low],
                            face=[float(center[0]-left),float(center[1]-low)],span=span,
                            faceBox=[box[0]-left,box[1]-low,box[2]-left,box[3]-low])
    refs=[records[f'{who}-motion:4']['span'] for who in ('husband','wife')]
    targets=[refs[0]*104/341*.97,refs[1]*104/341*.9]
    data=dict(version=1,referenceFaceDp=targets,frames=records,
              hashes={n:hashlib.sha256((ART/(n+'.png')).read_bytes()).hexdigest() for n in images})
    (ROOT/'art/v2/character-landmarks.json').write_text(json.dumps(data,indent=2),encoding='utf-8')
    with (ART/'character-measures.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.writer(f); writer.writerow(['asset','index','x','y','width','height','faceX','faceY','faceSpan','referenceSpan'])
        for r in records.values(): writer.writerow([r['asset'],r['index'],*r['source'],*r['face'],r['span'],targets[r['who']]])
    print('Frames:',len(records),'reference face dp:',targets)
    for name in images:
        rr=[r for r in records.values() if r['asset']==name]
        print(name, 'span',round(min(r['span'] for r in rr),1),round(max(r['span'] for r in rr),1))

if __name__=='__main__': run()

