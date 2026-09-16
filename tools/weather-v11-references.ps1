$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$dir=Join-Path $root 'art/weather-v11'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
$data=Get-Content (Join-Path $root 'art/v2/isolated-landmarks.json') -Raw | ConvertFrom-Json
foreach($who in @('husband','wife')) {
 foreach($group in @('motion','social','personal','extras')) {
  $frames= switch($group) { 'motion' {@(0..11)} 'social' {@(12..23)} 'personal' {@(24..35)} 'extras' {@(36..39)+@(48..63)} }
  $rows=[int][Math]::Ceiling($frames.Count/4)
  $dst=[System.Drawing.Bitmap]::new(2048,($rows*520));$g=[System.Drawing.Graphics]::FromImage($dst)
  $g.Clear([System.Drawing.Color]::Transparent);$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $i=0
  foreach($frame in $frames) {
   if($frame -ge 48) {$name="$who-idle";$index=$frame-48}
   elseif($frame -ge 36) {$name='kiss';$index=@('husband','wife').IndexOf($who)*4+$frame-36}
   else {$name="$who-$group";$index=$frame%12}
   $m=$data.frames."${name}:$index";$s=$m.source
   $src=[System.Drawing.Bitmap]::new((Join-Path $root "app/src/main/assets/art/calibrated/$name.png"))
   $scale=$data.referenceFaceDp[$m.who]/$m.span*4
   $w=[int]($s[2]*$scale);$h=[int]($s[3]*$scale)
   $rect=[System.Drawing.Rectangle]::new([int](($i%4)*512+256-$w/2),[int]([Math]::Floor($i/4)*520+490-$h),$w,$h)
   $g.DrawImage($src,$rect,[int]$s[0],[int]$s[1],[int]$s[2],[int]$s[3],[System.Drawing.GraphicsUnit]::Pixel)
   $src.Dispose();$i++
  }
  $g.Dispose();$dst.Save((Join-Path $dir "$who-$group-reference.png"));$dst.Dispose()
 }
}
