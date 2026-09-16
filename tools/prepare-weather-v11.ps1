param([switch]$PackOnly)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies @('System.Drawing.Common','System.Drawing.Primitives','System.Private.Windows.GdiPlus','System.Private.Windows.Core','System.Collections','System.Runtime','System.Runtime.InteropServices') -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public static class CyanSprites {
 static byte Clamp(double x) { return (byte)Math.Max(0,Math.Min(255,Math.Round(x))); }
 public static void Key(string input,string output) {
  using(var raw=new Bitmap(input)) using(var src=new Bitmap(raw.Width,raw.Height,PixelFormat.Format32bppArgb)) {
   using(var g=Graphics.FromImage(src)) g.DrawImageUnscaled(raw,0,0);
   var rect=new Rectangle(0,0,src.Width,src.Height);
   var data=src.LockBits(rect,ImageLockMode.ReadWrite,PixelFormat.Format32bppArgb);
   var bytes=new byte[data.Stride*src.Height];Marshal.Copy(data.Scan0,bytes,0,bytes.Length);
   for(int y=0;y<src.Height;y++)for(int x=0;x<src.Width;x++) {
    int i=y*data.Stride+x*4;double b=bytes[i],g=bytes[i+1],r=bytes[i+2];
    double spill=Math.Max(0,Math.Min(g,b)-r);
    if(spill>220 && r<40) {bytes[i+3]=0;continue;}
    if(spill>20) {
     double a=Math.Max(.01,1-spill/255);
     bytes[i]=Clamp((b-(1-a)*255)/a);bytes[i+1]=Clamp((g-(1-a)*255)/a);bytes[i+2]=Clamp(r/a);bytes[i+3]=Clamp(bytes[i+3]*a);
    }
   }
   Marshal.Copy(bytes,0,data.Scan0,bytes.Length);src.UnlockBits(data);src.Save(output,ImageFormat.Png);
  }
 }
}
'@
$root=Split-Path -Parent $PSScriptRoot
$dir=Join-Path $root 'art/weather-v11'
New-Item -ItemType Directory -Path (Join-Path $dir 'clean') -Force | Out-Null
if(!$PackOnly) {
 $plan=Get-Content (Join-Path $dir 'generation-plan.json') -Raw | ConvertFrom-Json
 foreach($item in $plan) {
  $inputFile=Join-Path $dir "$($item.name).png"
  $repair=Join-Path $dir "$($item.name)-repair.png"
  if(Test-Path $repair) {$inputFile=$repair}
  if(!(Test-Path $inputFile)) {continue}
  [CyanSprites]::Key($inputFile,(Join-Path $dir "clean/$($item.name).png"))
  Write-Output "Prepared $($item.name)"
 }
} else {
 $layouts=Get-Content (Join-Path $dir 'layout.json') -Raw | ConvertFrom-Json
 foreach($property in $layouts.PSObject.Properties) {
  $name=$property.Name;$layout=$property.Value
  $src=[System.Drawing.Bitmap]::new((Join-Path $dir "clean/$name.png"))
  $dst=[System.Drawing.Bitmap]::new([int]($layout.cellWidth*4),[int]($layout.cellHeight*$layout.rows))
  $slot=0
  foreach($frame in $layout.frames) {
   $ox=($slot%4)*$layout.cellWidth;$oy=[int][Math]::Floor($slot/4)*$layout.cellHeight
   foreach($run in $frame.runs) {
    $y=[int]$run[0]
    for($x=[int]$run[1];$x -lt $run[2];$x++) { $dst.SetPixel([int]($ox+$x-$frame.crop[0]),[int]($oy+$y-$frame.crop[1]),$src.GetPixel($x,$y)) }
   }
   $slot++
  }
  $dst.Save((Join-Path $root "app/src/main/assets/art/calibrated/w11-$name.png"));$dst.Dispose();$src.Dispose()
  Write-Output "Packed $name"
 }
}
