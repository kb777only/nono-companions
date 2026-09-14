param([Parameter(Mandatory=$true)][ValidateSet('default','cold','hot','night')][string]$Outfit)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot=Split-Path -Parent $PSScriptRoot
$assetPath=Join-Path $projectRoot "app/src/main/assets/art/rain-$Outfit.png"
$layout=(Get-Content -LiteralPath (Join-Path $projectRoot 'art/v2/rain-layout.json') -Raw | ConvertFrom-Json).$Outfit
# Source row boundaries were measured from connected opaque figures. Generated rows
# drift a few pixels; crop complete figures before packing, instead of cutting at y%256.
$source=[System.Drawing.Bitmap]::new($assetPath)
$target=[System.Drawing.Bitmap]::new(1024,1536,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics=[System.Drawing.Graphics]::FromImage($target)
try {
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.CompositingMode=[System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    for($col=0;$col -lt 4;$col++) {
        for($row=0;$row -lt 6;$row++) {
            $top=[int]$layout[$col][$row][0]; $bottom=[int]$layout[$col][$row][1]
            $height=$bottom-$top
            $rect=[System.Drawing.Rectangle]::new(($col*256+8),($row*256+250-[int]($height*.94)),240,[int]($height*.94))
            $graphics.DrawImage($source,$rect,($col*256),$top,256,$height,[System.Drawing.GraphicsUnit]::Pixel)
        }
    }
} finally { $graphics.Dispose(); $source.Dispose() }
try { $target.Save($assetPath,[System.Drawing.Imaging.ImageFormat]::Png) } finally { $target.Dispose() }
Write-Output "Aligned 24 rain-$Outfit cells at a shared scale and foot anchor"
