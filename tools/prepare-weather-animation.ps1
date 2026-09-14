$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies @('System.Drawing.Common','System.Drawing.Primitives','System.Private.Windows.GdiPlus','System.Private.Windows.Core','System.Collections','System.Runtime','System.Console') -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
public static class WeatherAtlasPrep {
 public static void Run(string source,string output) {
  using(var src=new Bitmap(source)) using(var dst=new Bitmap(src.Width,src.Height,PixelFormat.Format32bppArgb)) {
   int w=src.Width,h=src.Height; bool[] bg=new bool[w*h];var q=new Queue<int>();
   bool opaque=src.GetPixel(0,0).A>200;
   bool magenta=src.GetPixel(0,0).R>180 && src.GetPixel(0,0).B>180 && src.GetPixel(0,0).G<100;
   Func<int,bool> candidate=i=> { var c=src.GetPixel(i%w,i/w); int max=Math.Max(c.R,Math.Max(c.G,c.B)),min=Math.Min(c.R,Math.Min(c.G,c.B));return c.A<16 || (magenta ? c.R>180 && c.B>180 && c.G<125 : opaque && min>=105 && max-min<=8); };
   Action<int> add=i=> {if(!bg[i] && candidate(i)){bg[i]=true;q.Enqueue(i);}};
   for(int y=0;y<h;y++)for(int x=0;x<w;x++) if(x==0||y==0||x==w-1||y==h-1) add(y*w+x);
   while(q.Count>0){int i=q.Dequeue(),x=i%w,y=i/w;if(x>0)add(i-1);if(x<w-1)add(i+1);if(y>0)add(i-w);if(y<h-1)add(i+w);}
   for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
    var c=src.GetPixel(x,y);
    // Reject transparent-export chroma noise; preserve original colours/alpha otherwise.
    bool noise=!opaque && ((c.R>230 && c.G<45 && c.B<45)||(c.R>230 && c.G>230 && c.B<45));
    if(!bg[y*w+x]&&!noise && !(magenta && c.R>200 && c.B>200 && c.G<100)) dst.SetPixel(x,y,c);
   }
   dst.Save(output,ImageFormat.Png);
  }
 }
}
'@
$root=Split-Path -Parent $PSScriptRoot
$dir=Join-Path $root 'art/weather-v10/clean'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
$records=Get-Content (Join-Path $root 'art/weather-v10/generation.json') -Raw | ConvertFrom-Json
foreach($r in $records) {
 $name=$r.name
 $source=if(Test-Path (Join-Path $root "art/weather-v10/$name-matte.png")) {"$name-matte"} elseif($name -eq 'husband-cold') {'husband-cold-v2'} else {$name}
 [WeatherAtlasPrep]::Run((Join-Path $root "art/weather-v10/$source.png"),(Join-Path $dir "$name.png"))
 Write-Output "Prepared $name"
}

