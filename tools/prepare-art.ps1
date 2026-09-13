# Deterministic atlas extraction: remove only border-connected neutral checkerboard.
# Original source artwork is retained. No generative changes to character identity.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies @('System.Drawing.Common','System.Drawing.Primitives','System.Private.Windows.GdiPlus','System.Private.Windows.Core','System.Collections','System.Runtime','System.Console') -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
public static class AtlasPrep {
 public static void Run(string source, string output) {
  using(var src=new Bitmap(source)) using(var dst=new Bitmap(src.Width,src.Height,PixelFormat.Format32bppArgb)) {
   int w=src.Width,h=src.Height; bool[] seen=new bool[w*h]; var q=new Queue<int>();
   Func<int,bool> candidate=(i)=> { Color c=src.GetPixel(i%w,i/w); int max=Math.Max(c.R,Math.Max(c.G,c.B)), min=Math.Min(c.R,Math.Min(c.G,c.B)); return min>=105 && max-min<=22; };
   Action<int> add=(i)=> { if(!seen[i] && candidate(i)) { seen[i]=true; q.Enqueue(i); } };
   // Seed each cell boundary as all poses have explicitly empty margins.
   for(int y=0;y<h;y++) for(int x=0;x<w;x++) if(x%(w/4)==0 || x%(w/4)==w/4-1 || y%(h/2)==0 || y%(h/2)==h/2-1) add(y*w+x);
   while(q.Count>0) { int i=q.Dequeue(),x=i%w,y=i/w; if(x>0)add(i-1); if(x<w-1)add(i+1); if(y>0)add(i-w); if(y<h-1)add(i+w); }
   int transparent=0;
   for(int y=0;y<h;y++) for(int x=0;x<w;x++) { int i=y*w+x; var c=src.GetPixel(x,y); if(seen[i]) { dst.SetPixel(x,y,Color.Transparent); transparent++; } else dst.SetPixel(x,y,c); }
   if(transparent<w*h/5) throw new Exception("Extraction failed: insufficient transparent background");
   dst.Save(output,ImageFormat.Png);
   Console.WriteLine(output+": "+transparent+" transparent pixels / "+w*h);
  }
 }
}
'@
$projectRoot = Split-Path -Parent $PSScriptRoot
foreach ($sheet in @('a','b')) {
    [AtlasPrep]::Run((Join-Path $projectRoot "art/poses-$sheet-source.png"),(Join-Path $projectRoot "app/src/main/assets/art/poses-$sheet.png"))
}
