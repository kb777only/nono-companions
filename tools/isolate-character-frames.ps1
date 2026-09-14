$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$data=Get-Content -LiteralPath (Join-Path $root 'art/v2/isolation-layout.json') -Raw | ConvertFrom-Json
$dir=Join-Path $root 'app/src/main/assets/art/calibrated'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
foreach($name in $data.PSObject.Properties.Name) {
 $layout=$data.$name
 $src=[System.Drawing.Bitmap]::new((Join-Path $root "app/src/main/assets/art/$name.png"))
 $dst=[System.Drawing.Bitmap]::new([int]($layout.cellWidth*4),[int]($layout.cellHeight*$layout.rows),[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
 $i=0
 foreach($frame in $layout.frames) {
  $ox=($i%4)*$layout.cellWidth; $oy=[int][Math]::Floor($i/4)*$layout.cellHeight
  foreach($run in $frame.runs) {
   $y=$run[0]
   for($x=$run[1];$x -lt $run[2];$x++) { $dst.SetPixel([int]($ox+$x-$frame.crop[0]),[int]($oy+$y-$frame.crop[1]),$src.GetPixel($x,$y)) }
  }
  $i++
 }
 $dst.Save((Join-Path $dir "$name.png"),[System.Drawing.Imaging.ImageFormat]::Png);$dst.Dispose();$src.Dispose()
 Write-Output "Packed $name"
}
