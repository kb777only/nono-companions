$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
$data=Get-Content -LiteralPath (Join-Path $root 'art/v2/character-landmarks.json') -Raw | ConvertFrom-Json
$dir=Join-Path $root 'docs/scale-audit/landmarks'
New-Item -ItemType Directory -Path $dir -Force | Out-Null
foreach($name in $data.hashes.PSObject.Properties.Name) {
 $src=[System.Drawing.Bitmap]::new((Join-Path $root "app/src/main/assets/art/$name.png"))
 $dst=[System.Drawing.Bitmap]::new($src.Width,$src.Height)
 $g=[System.Drawing.Graphics]::FromImage($dst); $g.Clear([System.Drawing.Color]::White); $g.DrawImageUnscaled($src,0,0)
 $pen=[System.Drawing.Pen]::new([System.Drawing.Color]::Red,2)
 foreach($entry in $data.frames.PSObject.Properties.Value | Where-Object asset -eq $name) {
  $s=$entry.source; $b=$entry.faceBox
  $g.DrawRectangle($pen,[single]($s[0]+$b[0]),[single]($s[1]+$b[1]),[single]($b[2]-$b[0]),[single]($b[3]-$b[1]))
 }
 $g.Dispose();$pen.Dispose();$src.Dispose()
 $dst.Save((Join-Path $dir "$name.png"));$dst.Dispose()
}
