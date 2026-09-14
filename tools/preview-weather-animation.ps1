$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$data=Get-Content -LiteralPath (Join-Path $root 'art/weather-v10/published.json') -Raw | ConvertFrom-Json
$dir=Join-Path $root 'docs/weather-v10'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
foreach($name in $data.hashes.PSObject.Properties.Name | Where-Object { $_ -like 'w10-*' }) {
 $entries=@($data.frames.PSObject.Properties.Value | Where-Object asset -eq $name | Sort-Object index)
 $src=[System.Drawing.Bitmap]::new((Join-Path $root "app/src/main/assets/art/calibrated/$name.png"))
 $rows=[int][Math]::Ceiling($entries.Count/8)
 $dst=[System.Drawing.Bitmap]::new(2048,($rows*300))
 $g=[System.Drawing.Graphics]::FromImage($dst); $g.Clear([System.Drawing.Color]::FromArgb(245,244,240))
 $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
 $pen=[System.Drawing.Pen]::new([System.Drawing.Color]::LightGray,1)
 $font=[System.Drawing.Font]::new('Arial',10)
 $i=0
 foreach($entry in $entries) {
  $s=$entry.source; $scale=$data.referenceFaceDp[$entry.who]/$entry.span
  $w=$s[2]*$scale*2; $h=$s[3]*$scale*2
  $cx=($i%8)*256+128; $baseline=[Math]::Floor($i/8)*300+265
  $rect=[System.Drawing.Rectangle]::new([int]($cx-$w/2),[int]($baseline-$h),[int]$w,[int]$h)
  $g.DrawImage($src,$rect,[int]$s[0],[int]$s[1],[int]$s[2],[int]$s[3],[System.Drawing.GraphicsUnit]::Pixel)
  $g.DrawLine($pen,[single]($cx-130),[single]$baseline,[single]($cx+130),[single]$baseline)
  $g.DrawString("$($entry.asset):$($entry.index)",$font,[System.Drawing.Brushes]::Black,[single]($cx-120),[single]($baseline+8))
  $i++
 }
 $g.Dispose();$pen.Dispose();$font.Dispose();$src.Dispose()
 $dst.Save((Join-Path $dir "$name.png"));$dst.Dispose()
}

