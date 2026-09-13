param([string]$Only='')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies @('System.Drawing.Common','System.Drawing.Primitives','System.Private.Windows.GdiPlus','System.Private.Windows.Core','System.Collections','System.Runtime','System.Console') -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
public static class ChromaAtlas {
 static int Clamp(double v) { return (int)Math.Max(0,Math.Min(255,v)); }
 public static void Run(string input,string output,int cols,int rows,int targetWidth,int targetHeight) {
  using(var src=new Bitmap(input)) using(var keyed=new Bitmap(src.Width,src.Height,PixelFormat.Format32bppArgb)) {
   int w=src.Width,h=src.Height, count=0;
   for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
    var c=src.GetPixel(x,y); int difference=Math.Min(c.R,c.B)-c.G;
    if(difference>120 && c.R>160 && c.B>160 && c.G<110) { keyed.SetPixel(x,y,Color.Transparent); count++; }
    else if(difference>50 && c.R>c.G*1.6 && c.B>c.G*1.6) {
     double a=1-difference/255.0;
     keyed.SetPixel(x,y,Color.FromArgb(Clamp(a*255),Clamp((c.R-(1-a)*255)/a),Clamp(c.G/a),Clamp((c.B-(1-a)*255)/a)));
    } else keyed.SetPixel(x,y,c);
   }
   // Remove generated grid separators without touching character interior pixels.
   if(cols>1) {
    bool[] clearY=new bool[h],clearX=new bool[w];
    for(int y=0;y<h;y++) { int n=0; for(int x=0;x<w;x++) if(keyed.GetPixel(x,y).A>10)n++; if(n>w*.975)clearY[y]=true; }
    for(int x=0;x<w;x++) { int n=0; for(int y=0;y<h;y++) if(keyed.GetPixel(x,y).A>10)n++; if(n>h*.975)clearX[x]=true; }
    for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(clearY[y] || clearY[Math.Max(0,y-1)] || clearY[Math.Min(h-1,y+1)] || clearX[x] || clearX[Math.Max(0,x-1)] || clearX[Math.Min(w-1,x+1)]) keyed.SetPixel(x,y,Color.Transparent);
   }
   if(count<w*h*.15) throw new Exception("Not enough background removed: "+input);
   using(var dst=new Bitmap(targetWidth,targetHeight,PixelFormat.Format32bppArgb)) {
    using(var g=Graphics.FromImage(dst)) { g.CompositingMode=CompositingMode.SourceCopy; g.InterpolationMode=InterpolationMode.HighQualityBicubic; g.PixelOffsetMode=PixelOffsetMode.HighQuality; g.DrawImage(keyed,new Rectangle(0,0,targetWidth,targetHeight),0,0,w,h,GraphicsUnit.Pixel); }
    dst.Save(output,ImageFormat.Png);
   }
   Console.WriteLine(output+": keyed "+count+" background pixels");
  }
 }
}
'@
$projectRoot=Split-Path -Parent $PSScriptRoot
foreach($character in @('husband','wife')) {
    foreach($kind in @('motion','social','personal')) {
        $name="$character-$kind"
        if($Only -eq '' -or $Only -eq $name) { [ChromaAtlas]::Run((Join-Path $projectRoot "art/v2/$name-source.png"),(Join-Path $projectRoot "app/src/main/assets/art/$name.png"),4,3,1024,1023) }
    }
}
if($Only -eq '' -or $Only -eq 'bubbles') { [ChromaAtlas]::Run((Join-Path $projectRoot 'art/v2/bubbles-source.png'),(Join-Path $projectRoot 'app/src/main/assets/art/bubbles.png'),1,2,1024,682) }
if(($Only -eq '' -or $Only -eq 'props') -and (Test-Path (Join-Path $projectRoot 'art/v2/props-source.png'))) { [ChromaAtlas]::Run((Join-Path $projectRoot 'art/v2/props-source.png'),(Join-Path $projectRoot 'app/src/main/assets/art/props.png'),3,2,768,512) }

if($Only -eq '' -or $Only -eq 'kiss') { [ChromaAtlas]::Run((Join-Path $projectRoot 'art/v2/kiss-source.png'),(Join-Path $projectRoot 'app/src/main/assets/art/kiss.png'),4,2,1024,682) }

if($Only -eq '' -or $Only -eq 'menus') { [ChromaAtlas]::Run((Join-Path $projectRoot 'art/v2/menus-source.png'),(Join-Path $projectRoot 'app/src/main/assets/art/menus.png'),2,1,1024,512) }

if($Only -eq '' -or $Only -eq 'parachutes') { [ChromaAtlas]::Run((Join-Path $projectRoot 'art/v2/parachutes-source.png'),(Join-Path $projectRoot 'app/src/main/assets/art/parachutes.png'),3,2,1536,1024) }
