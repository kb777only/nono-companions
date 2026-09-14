$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Split-Path -Parent $PSScriptRoot
# Measured gaps between the complete generated figures, not nominal grid lines.
$layouts=@{ husband=@(0,345,727,1135,1536); wife=@(0,345,770,1174,1536) }
foreach($who in @('husband','wife')) {
    $source=[System.Drawing.Bitmap]::new((Join-Path $root "art/v2/$who-idle-source.png"))
    $keyed=[System.Drawing.Bitmap]::new(1024,1536,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for($y=0;$y -lt 1536;$y++) { for($x=0;$x -lt 1024;$x++) {
        $c=$source.GetPixel($x,$y)
        $difference=[Math]::Min([int]$c.R,[int]$c.B)-[int]$c.G
        if($difference -gt 90 -and $c.R -gt 150 -and $c.B -gt 150) { continue }
        if($difference -gt 40 -and $c.R -gt $c.G*1.5 -and $c.B -gt $c.G*1.5) {
            # Decontaminate only the magenta antialias fringe.
            $a=1-$difference/255.0
            $r=[int][Math]::Clamp(($c.R-(1-$a)*255)/$a,0,255)
            $g=[int][Math]::Clamp($c.G/$a,0,255)
            $b=[int][Math]::Clamp(($c.B-(1-$a)*255)/$a,0,255)
            $c=[System.Drawing.Color]::FromArgb([int]($a*255),$r,$g,$b)
        }
        $keyed.SetPixel($x,$y,$c)
    } }
    $target=[System.Drawing.Bitmap]::new(1024,1536,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g=[System.Drawing.Graphics]::FromImage($target)
    $g.CompositingMode=[System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    for($row=0;$row -lt 4;$row++) { for($col=0;$col -lt 4;$col++) {
        $top=$layouts[$who][$row]; $bottom=$layouts[$who][$row+1]
        $last=$top
        for($y=$top;$y -lt $bottom;$y++) { for($x=$col*256;$x -lt ($col+1)*256;$x++) { if($keyed.GetPixel($x,$y).A -gt 40) { $last=$y; break } } }
        $h=$last+1-$top
        # Common uniform scale, no independent resizing of seated heads or limbs.
        $scale=0.84
        $rect=[System.Drawing.Rectangle]::new(($col*256+20),($row*384+370-[int]($h*$scale)),215,[int]($h*$scale))
        $g.DrawImage($keyed,$rect,($col*256),$top,256,$h,[System.Drawing.GraphicsUnit]::Pixel)
    } }
    $g.Dispose(); $source.Dispose(); $keyed.Dispose()
    $target.Save((Join-Path $root "app/src/main/assets/art/$who-idle.png"),[System.Drawing.Imaging.ImageFormat]::Png)
    $target.Dispose()
    Write-Output "Prepared $who : 16 transparent, aligned frames"
}
