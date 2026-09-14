"""Build exact alpha-component crop masks; .NET performs lossless pixel isolation."""
from pathlib import Path
import json
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'app/src/main/assets/art'
data=json.loads((ROOT/'art/v2/character-landmarks.json').read_text(encoding='utf-8'))
result={}
for name in data['hashes']:
    im=np.array(Image.open(ART/(name+'.png')).convert('RGBA')); mask=im[:,:,3]>40
    h,w=mask.shape; labels=np.zeros((h,w),np.int32); count=0; boxes={}
    # Scan-line flood fill is linear in horizontal runs rather than individual pixels.
    for y in range(h):
      for x in np.where(mask[y] & (labels[y]==0))[0]:
        if labels[y,x]: continue
        count+=1; stack=[(int(x),y)]; l=w;t=h;r=b=0; area=0
        while stack:
            sx,sy=stack.pop()
            if labels[sy,sx] or not mask[sy,sx]: continue
            a=sx; z=sx
            while a>0 and mask[sy,a-1] and not labels[sy,a-1]: a-=1
            while z+1<w and mask[sy,z+1] and not labels[sy,z+1]: z+=1
            labels[sy,a:z+1]=count; area+=z-a+1
            l=min(l,a);r=max(r,z+1);t=min(t,sy);b=max(b,sy+1)
            for ny in (sy-1,sy+1):
                if not 0<=ny<h: continue
                pending=mask[ny,a:z+1] & (labels[ny,a:z+1]==0)
                starts=np.where(pending & ~np.r_[False,pending[:-1]])[0]
                stack.extend((int(a+xx),ny) for xx in starts)
        boxes[count]=(l,t,r,b,area)
    frames=[r for r in data['frames'].values() if r['asset']==name]
    primaries={}
    for entry in frames:
        sx,sy,sw,sh=entry['source']; fx,fy=entry['face']
        x=int(sx+fx);y=int(sy+fy)
        candidates=labels[max(0,y-12):min(h,y+13),max(0,x-12):min(w,x+13)]
        ids,counts=np.unique(candidates[candidates>0],return_counts=True)
        primary=int(ids[np.argmax(counts)])
        primaries[entry['index']]=primary
    used=set(primaries.values()); packed=[]
    for entry in frames:
        primary=primaries[entry['index']]; selected={primary}
        sx,sy,sw,sh=entry['source']
        for label,(l,t,r,b,area) in boxes.items():
            if label not in used and area>12 and sx<=(l+r)/2<sx+sw and sy<=(t+b)/2<sy+sh:
                selected.add(label)
        keep=np.isin(labels,list(selected))
        # Preserve original antialias colors adjacent to selected components.
        padded=np.pad(keep,1)
        grown=np.logical_or.reduce([padded[yy:yy+h,xx:xx+w] for yy in range(3) for xx in range(3)])
        keep=grown & ((labels==0)|np.isin(labels,list(selected))) & (im[:,:,3]>0)
        yy,xx=np.where(keep); l=max(0,int(xx.min())-2);t=max(0,int(yy.min())-2);r=min(w,int(xx.max())+3);b=min(h,int(yy.max())+3)
        runs=[]
        for y in range(t,b):
            row=keep[y,l:r]; transitions=np.diff(np.r_[False,row,False].astype(np.int8))
            for a,z in zip(np.where(transitions==1)[0],np.where(transitions==-1)[0]): runs.append([y,int(l+a),int(l+z)])
        packed.append(dict(index=entry['index'],crop=[l,t,r-l,b-t],runs=runs))
    cw=((max(p['crop'][2] for p in packed)+7)//4)*4; ch=((max(p['crop'][3] for p in packed)+7)//4)*4
    result[name]=dict(cellWidth=cw,cellHeight=ch,rows=(len(packed)+3)//4,frames=packed)
    for i,p in enumerate(packed):
        entry=data['frames'][f"{name}:{p['index']}"]; old=entry['source']; l,t,ww,hh=p['crop']
        entry['face']=[entry['face'][0]+old[0]-l,entry['face'][1]+old[1]-t]
        entry['source']=[(i%4)*cw,(i//4)*ch,ww,hh]
    print(name,len(packed),'isolated frames',flush=True)
(ROOT/'art/v2/isolation-layout.json').write_text(json.dumps(result,separators=(',',':')),encoding='utf-8')
(ROOT/'art/v2/isolated-landmarks.json').write_text(json.dumps(data,indent=2),encoding='utf-8')
