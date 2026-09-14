"""Read-only audit of v0.9 PetView geometry. Never alters source or runtime PNGs.

This verifies asset coverage, sampling, aspect ratio and clipping, not semantic
head/body uniformity. The HTML renders every case for perceptual review.
"""
from pathlib import Path
import argparse, base64, csv, hashlib, json, math
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'app/src/main/assets/art'
OUT=ROOT/'docs/scale-audit'

def dressed(f):
    for frames, index in [([48,51],9),([49,50],6),([52,55],0),([53,54],5),([56],0),([57,58],10),([59],1),([60,62],2),([61,63],3),([0,2],2),([1,3],3),([5,6],1),([8,9,10,11,23],8),([12,13,14],5),([20,21,22],10),([24,25,26,27],4),([28,29,30,31],6),([32,33,34,35],11),([36,37,38],7),([40,41],9)]:
        if f in frames: return index
    return 0

def rainframe(f):
    return 5 if f in (44,45) else 11 if f==46 else 9 if f==47 else dressed(f)

def route(who,outfit,rain,f):
    if f in (42,43): return 'peeking',2,2,who*2+f-42,'peek'
    if rain and f not in (8,9,10,11,40,41,42,43): return 'rain-'+outfit,4,6,who*12+rainframe(f),'rain'
    if f in range(44,48): return 'cooling',4,2,who*4+f-44,'full'
    if outfit!='default': return 'weather-'+outfit,4,6,who*12+dressed(f),'full'
    if f>=48: return ('husband','wife')[who]+'-idle',4,4,f-48,'full'
    if f>=40: return 'parachutes',3,2,who*3+f-39,'full'
    if f>=36: return 'kiss',4,2,who*4+f-36,'full'
    return ('husband','wife')[who]+'-'+('motion','social','personal')[f//12],4,3,f%12,'trimmed'

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--strict',action='store_true'); args=parser.parse_args()
    renderer=(ROOT/'app/src/main/java/dev/nono/companions/PetView.kt').read_text(encoding='utf-8')
    if hashlib.sha256(renderer.encode()).hexdigest()!='3207f46d5b8a97f5cb13127b74bc9c3d3806b8b13d2bf210e85fd82f211829fd':
        raise SystemExit('Renderer changed: update the audit geometry model before running it.')
    OUT.mkdir(exist_ok=True)
    cache={}; records=[]
    for who in range(2):
      for outfit in ('default','cold','hot','night'):
       for rain in (False,True):
        for frame in range(64):
          name,cols,rows,index,mode=route(who,outfit,rain,frame)
          if name not in cache: cache[name]=np.array(Image.open(ART/(name+'.png')).convert('RGBA'))
          im=cache[name]; height,width=im.shape[:2]; cw=width//cols; ch=height//rows
          sx=index%cols*cw; sy=index//cols*ch
          cell=im[sy:sy+ch,sx:sx+cw]; ys,xs=np.where(cell[:,:,3]>40)
          assert len(xs), (name,index,'empty')
          l,t,r,b=int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)
          if mode=='trimmed': sx+=l; sy+=t; sw=r-l; sh=b-t; px=xs-l; py=ys-t
          else: sw=cw; sh=ch; px=xs; py=ys
          if mode=='peek': scale_x=38/sw; scale_y=44/sh; view_w,view_h=38,44; dx=dy=0
          else:
            scale_x=scale_y=min(72/sw,104/sh) if mode=='rain' else 104/ch*(.97 if who==0 else .9)
            view_w,view_h=72,104; dx=(72-sw*scale_x)/2; dy=102-sh*scale_y
          x=dx+(px+.5)*scale_x; y=dy+(py+.5)*scale_y
          clipped=int(np.count_nonzero((x<0)|(x>=view_w)|(y<0)|(y>=view_h)))
          rec=dict(who=('husband','wife')[who],outfit=outfit,rain=rain,frame=frame,asset=name,index=index,
                   sx=sx,sy=sy,sw=sw,sh=sh,dx=dx,dy=dy,dw=sw*scale_x,dh=sh*scale_y,view_w=view_w,view_h=view_h,
                   scale_x=scale_x,scale_y=scale_y,opaque_width=(r-l)*scale_x,opaque_height=(b-t)*scale_y,
                   opaque_pixels=len(xs),clipped_pixels=clipped,clip_percent=100*clipped/len(xs),anisotropic=abs(scale_x-scale_y)>1e-6)
          records.append(rec)
    unique={(r['asset'],r['index']) for r in records}
    summary=dict(render_cases=len(records),unique_character_cells=len(unique),atlases=len(cache),
                 distorted_cases=sum(r['anisotropic'] for r in records),clipped_cases=sum(r['clipped_pixels']>0 for r in records),
                 material_clipping_cases=sum(r['clip_percent']>=1 for r in records),
                 verdict='FAIL: geometric defects and visually confirmed character size changes; not perceptually certified',
                 note='Opaque dimensions include hair, clothes, extended limbs and umbrellas. They are not head/body size measurements. No frame is exempt from inventory; inventory coverage is not perceptual approval.')
    summary['standing_comparisons']=[{k:r[k] for k in ('who','outfit','rain','frame','opaque_width','opaque_height','clip_percent')} for r in records if (r['outfit']=='default' and r['frame'] in (4,52)) or (r['outfit']!='default' and r['frame']==4 and not r['rain'])]
    (OUT/'measurements.json').write_text(json.dumps(dict(summary=summary,records=records),indent=2),encoding='utf-8')
    with (OUT/'measurements.csv').open('w',encoding='utf-8',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(records[0])); w.writeheader(); w.writerows(records)
    assets={name:'data:image/png;base64,'+base64.b64encode((ART/(name+'.png')).read_bytes()).decode() for name in cache}
    hashes={name:hashlib.sha256((ART/(name+'.png')).read_bytes()).hexdigest() for name in cache}
    (OUT/'asset-hashes.json').write_text(json.dumps(hashes,indent=2),encoding='utf-8')
    html='''<!doctype html><meta charset="utf-8"><title>NoNo — Character scale audit</title>
<style>body{font:15px system-ui;background:#f4f3ef;color:#29262c;margin:24px}header{position:sticky;top:0;background:#f4f3ef;padding:12px;z-index:2}h1{margin:0}button,select{padding:8px;margin:5px}#grid{display:grid;grid-template-columns:repeat(auto-fill,200px);gap:16px}.card{background:white;border:1px solid #ddd;padding:10px}.bad{border:2px solid #bc3948}canvas{width:144px;height:208px;background:linear-gradient(white,#eee)}small{display:block}b{color:#ac293d}</style>
<header><h1>Character scale audit — FAIL</h1><p>All 1,024 routes / 296 character cells. Display enlarged 2×. Baseline is red; window boundary is blue. Original artwork, actual v0.9 scaling. Different pose heights are expected; compare faces and body proportions.</p>
<select id="who"><option>husband</option><option>wife</option></select><select id="outfit"><option>default</option><option>cold</option><option>hot</option><option>night</option></select><label><input id="rain" type="checkbox">Rain</label><button id="compare">Compare standing poses across outfits</button><p id="summary"></p></header><main id="grid"></main>
<script>const data=DATA, sources=SOURCES; const images={}; let serial=0;
async function draw(records){const id=++serial; const grid=document.querySelector('#grid');grid.replaceChildren();for(const r of records){if(!images[r.asset]){const im=new Image();im.src=sources[r.asset];await im.decode();images[r.asset]=im;}if(id!==serial)return;const div=document.createElement('div');div.className='card'+(r.clipped_pixels||r.anisotropic?' bad':'');const c=document.createElement('canvas');c.width=288;c.height=416;const ctx=c.getContext('2d');ctx.scale(4,4);ctx.strokeStyle='#638ed0';ctx.strokeRect(.2,.2,r.view_w-.4,r.view_h-.4);ctx.save();ctx.beginPath();ctx.rect(0,0,r.view_w,r.view_h);ctx.clip();ctx.drawImage(images[r.asset],r.sx,r.sy,r.sw,r.sh,r.dx,r.dy,r.dw,r.dh);ctx.restore();ctx.strokeStyle='#dd746a';ctx.beginPath();ctx.moveTo(0,102);ctx.lineTo(72,102);ctx.stroke();div.append(c);const text=document.createElement('small');text.textContent=`${r.who} ${r.outfit}${r.rain?' rain':''} • frame ${r.frame} • ${r.asset}[${r.index}] • opaque ${r.opaque_width.toFixed(1)}×${r.opaque_height.toFixed(1)} dp • clipped ${r.clip_percent.toFixed(2)}%${r.anisotropic?' • ASPECT DISTORTION':''}`;div.append(text);grid.append(div);}}
function show(){draw(data.records.filter(r=>r.who===who.value&&r.outfit===outfit.value&&r.rain===rain.checked));}const who=document.querySelector('#who'),outfit=document.querySelector('#outfit'),rain=document.querySelector('#rain');[who,outfit,rain].forEach(e=>e.onchange=show);document.querySelector('#compare').onclick=()=>draw(data.records.filter(r=>r.who===who.value&&(r.frame===4||(r.frame===52&&r.outfit==='default'&&!r.rain))));document.querySelector('#summary').textContent=JSON.stringify(data.summary);show();</script>'''
    html=html.replace('DATA',json.dumps(dict(summary=summary,records=records))).replace('SOURCES',json.dumps(assets))
    (OUT/'index.html').write_text(html,encoding='utf-8')
    print(json.dumps(summary,indent=2))
    if args.strict:
        # Geometry failures are sufficient to reject; their absence is not a
        # semantic face/body approval. That still requires landmark review.
        raise SystemExit(1 if summary['distorted_cases'] or summary['clipped_cases'] else 2)

if __name__=='__main__': main()
