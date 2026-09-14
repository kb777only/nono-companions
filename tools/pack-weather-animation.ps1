$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$data=Get-Content (Join-Path $root 'art/weather-v10/layout.json') -Raw | ConvertFrom-Json
foreach($name in $data.PSObject.Properties.Name) {
 $layout=$data.$name
 $src=[System.Drawing.Bitmap]::new((Join-Path $root "art/weather-v10/clean/$name.png"))
 $dst=[System.Drawing.Bitmap]::new([int]($layout.cellWidth*8),[int]($layout.cellHeight*7),[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $slot=0
 foreach($frame in $layout.frames) {
  $ox=($slot%8)*$layout.cellWidth;$oy=[int][Math]::Floor($slot/8)*$layout.cellHeight
  foreach($run in $frame.runs) {
   $y=[int]$run[0]
   for($x=[int]$run[1];$x -lt $run[2];$x++) { $dst.SetPixel([int]($ox+$x-$frame.crop[0]),[int]($oy+$y-$frame.crop[1]),$src.GetPixel($x,$y)) }
  }
  $slot++
 }
 $dst.Save((Join-Path $root "app/src/main/assets/art/calibrated/w10-$name.png"));$dst.Dispose();$src.Dispose()
 Write-Output "Packed $name"
}
