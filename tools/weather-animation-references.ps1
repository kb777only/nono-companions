$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$dir=Join-Path $root 'art/weather-v10'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
$rows=Import-Csv (Join-Path $root 'app/src/main/assets/art/character-measures.csv')
foreach($who in @('husband','wife')) {
 $dst=[System.Drawing.Bitmap]::new(2048,2240)
 $g=[System.Drawing.Graphics]::FromImage($dst); $g.Clear([System.Drawing.Color]::Transparent)
 $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
 $frames=@(0..39)+@(48..63); $slot=0
 foreach($frame in $frames) {
  if($frame -ge 48) { $name="$who-idle"; $index=$frame-48 }
  elseif($frame -ge 36) { $name='kiss'; $index=(@('husband','wife').IndexOf($who))*4+$frame-36 }
  else { $name="$who-$(@('motion','social','personal')[[int][Math]::Floor($frame/12)])"; $index=$frame%12 }
  $m=$rows | Where-Object { $_.asset -eq $name -and [int]$_.index -eq $index }
  $src=[System.Drawing.Bitmap]::new((Join-Path $root "app/src/main/assets/art/calibrated/$name.png"))
  $scale=[double]$m.referenceSpan/[double]$m.faceSpan*2
  $w=[int]([double]$m.width*$scale);$h=[int]([double]$m.height*$scale)
  $x=($slot%8)*256+128-$w/2; $y=[Math]::Floor($slot/8)*320+300-$h
  $g.DrawImage($src,[System.Drawing.Rectangle]::new([int]$x,[int]$y,$w,$h),[int]$m.x,[int]$m.y,[int]$m.width,[int]$m.height,[System.Drawing.GraphicsUnit]::Pixel)
  $src.Dispose();$slot++
 }
 $g.Dispose();$dst.Save((Join-Path $dir "$who-reference.png"));$dst.Dispose()
}
