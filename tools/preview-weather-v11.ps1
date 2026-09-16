$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$data=Get-Content (Join-Path $root 'art/weather-v11/published.json') -Raw | ConvertFrom-Json
$dir=Join-Path $root 'docs/weather-v11'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
$font=[System.Drawing.Font]::new('Arial',9)
function Draw-Sample($graphics,$entry,$cx,$baseline,$label) {
 $s=$entry.source;$scale=$data.referenceFaceDp[$entry.who]/$entry.span*2
 $src=[System.Drawing.Bitmap]::new((Join-Path $root "app/src/main/assets/art/calibrated/$($entry.asset).png"))
 $w=$s[2]*$scale;$h=$s[3]*$scale
 $rect=[System.Drawing.Rectangle]::new([int]($cx-$w/2),[int]($baseline-$h),[int]$w,[int]$h)
 $graphics.DrawImage($src,$rect,[int]$s[0],[int]$s[1],[int]$s[2],[int]$s[3],[System.Drawing.GraphicsUnit]::Pixel)
 $graphics.DrawString($label,$font,[System.Drawing.Brushes]::Black,[single]($cx-100),[single]($baseline+5))
 $src.Dispose()
}
foreach($name in $data.hashes.PSObject.Properties.Name | Where-Object {$_ -like 'w11-*'}) {
 $entries=@($data.frames.PSObject.Properties.Value | Where-Object asset -eq $name | Sort-Object index)
 $dst=[System.Drawing.Bitmap]::new(1024,([int][Math]::Ceiling($entries.Count/4)*360))
 $g=[System.Drawing.Graphics]::FromImage($dst);$g.Clear([System.Drawing.Color]::FromArgb(246,245,242));$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
 $i=0
 foreach($entry in $entries) { Draw-Sample $g $entry (($i%4)*256+128) ([Math]::Floor($i/4)*360+330) "$($entry.index)";$i++ }
 $g.Dispose();$dst.Save((Join-Path $dir "$name.png"));$dst.Dispose()
}
$dst=[System.Drawing.Bitmap]::new(2048,720);$g=[System.Drawing.Graphics]::FromImage($dst);$g.Clear([System.Drawing.Color]::FromArgb(246,245,242));$g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$row=0
foreach($who in @('husband','wife')) {
 $keys=@("${who}-motion:4")+@('cold','hot','night','rain-default','rain-cold','rain-hot','rain-night' | ForEach-Object {"w11-${who}-${_}-motion:4"})
 $i=0
 foreach($key in $keys) {Draw-Sample $g $data.frames.$key ($i*256+128) ($row*360+330) $(if($i -eq 0){'Original'}else{$key.Replace("w11-${who}-",'').Replace('-motion:4','')});$i++}
 $row++
}
$g.Dispose();$dst.Save((Join-Path $dir 'original-and-weather.png'));$dst.Dispose();$font.Dispose()
