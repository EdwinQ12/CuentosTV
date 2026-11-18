# Genera las ilustraciones de los cuentos con GDI+ (sin dependencias externas).
# Uso:  powershell -ExecutionPolicy Bypass -File tools\generar-ilustraciones.ps1
#
# Salida:
#   app/src/main/assets/stories/images/*.jpg   (cuentos locales)
#   tv-content/images/*.jpg                    (cuentos remotos)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$outLocal = Join-Path $root "app\src\main\assets\stories\images"
$outRemote = Join-Path $root "tv-content\images"
New-Item -ItemType Directory -Force -Path $outLocal, $outRemote | Out-Null

$W = 1280
$H = 720

function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }

function New-Canvas {
    $bmp = New-Object System.Drawing.Bitmap($W, $H)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    return @($bmp, $g)
}

function Fill-Gradient($g, [string]$top, [string]$bottom) {
    $rect = New-Object System.Drawing.Rectangle(0, 0, $W, $H)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect, (C $top), (C $bottom), 90.0)
    $g.FillRectangle($brush, $rect)
    $brush.Dispose()
}

function Add-Stars($g, [int]$count, [int]$maxY, [int]$seed) {
    $rand = New-Object System.Random($seed)
    for ($i = 0; $i -lt $count; $i++) {
        $x = $rand.Next(0, $W)
        $y = $rand.Next(0, $maxY)
        $r = 0.8 + $rand.NextDouble() * 2.2
        $alpha = $rand.Next(70, 230)
        $brush = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb($alpha, 255, 246, 214))
        $g.FillEllipse($brush, [float]($x - $r), [float]($y - $r), [float]($r * 2), [float]($r * 2))
        $brush.Dispose()
    }
}

function Add-Moon($g, [float]$cx, [float]$cy, [float]$r, [string]$color) {
    for ($i = 7; $i -ge 1; $i--) {
        $glow = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb(16, (C $color)))
        $rr = $r + $i * 9
        $g.FillEllipse($glow, [float]($cx - $rr), [float]($cy - $rr), [float]($rr * 2), [float]($rr * 2))
        $glow.Dispose()
    }
    $moon = New-Object System.Drawing.SolidBrush((C $color))
    $g.FillEllipse($moon, [float]($cx - $r), [float]($cy - $r), [float]($r * 2), [float]($r * 2))
    $moon.Dispose()

    # cráteres suaves
    $rand = New-Object System.Random(21)
    for ($i = 0; $i -lt 7; $i++) {
        $ang = $rand.NextDouble() * 6.283
        $dist = $rand.NextDouble() * $r * 0.65
        $px = $cx + [Math]::Cos($ang) * $dist
        $py = $cy + [Math]::Sin($ang) * $dist
        $rr = 4 + $rand.NextDouble() * $r * 0.12
        $crater = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb(26, 120, 96, 40))
        $g.FillEllipse($crater, [float]($px - $rr), [float]($py - $rr), [float]($rr * 2), [float]($rr * 2))
        $crater.Dispose()
    }
}

function Add-Glow($g, [float]$cx, [float]$cy, [float]$r, [string]$color, [int]$alpha) {
    for ($i = 8; $i -ge 1; $i--) {
        $a = [Math]::Max(1, [int]($alpha / ($i * 1.6)))
        $brush = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb($a, (C $color)))
        $rr = $r * (1 + $i * 0.55)
        $g.FillEllipse($brush, [float]($cx - $rr), [float]($cy - $rr), [float]($rr * 2), [float]($rr * 2))
        $brush.Dispose()
    }
    $core = New-Object System.Drawing.SolidBrush((C $color))
    $g.FillEllipse($core, [float]($cx - $r), [float]($cy - $r), [float]($r * 2), [float]($r * 2))
    $core.Dispose()
}

function Add-Fireflies($g, [int]$count, [int]$seed, [float]$top, [float]$bottom) {
    $rand = New-Object System.Random($seed)
    for ($i = 0; $i -lt $count; $i++) {
        $x = $rand.Next(20, ($W - 20)) + $rand.NextDouble()
        $y = $top + $rand.NextDouble() * ($bottom - $top)
        $r = 1.5 + $rand.NextDouble() * 2.5
        $alpha = $rand.Next(90, 200)
        $brush = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb($alpha, 255, 209, 102))
        $g.FillEllipse($brush, [float]($x - $r), [float]($y - $r), [float]($r * 2), [float]($r * 2))
        $brush.Dispose()
    }
}

function Add-Trees($g, [int]$baseY, [float]$height, [string]$color, [int]$alpha, [int]$seed) {
    $rand = New-Object System.Random($seed)
    $brush = New-Object System.Drawing.SolidBrush(
        [System.Drawing.Color]::FromArgb($alpha, (C $color)))
    $x = -50
    while ($x -lt $W + 70) {
        $treeH = $height * (0.55 + $rand.NextDouble() * 0.7)
        $treeW = $treeH * (0.32 + $rand.NextDouble() * 0.12)
        $tiers = 3
        for ($t = 0; $t -lt $tiers; $t++) {
            $ty = $baseY - ($treeH * 0.26) * $t
            $tw = $treeW * (1.0 - $t * 0.24)
            $points = @(
                (New-Object System.Drawing.PointF([float]($x - $tw / 2), [float]$ty)),
                (New-Object System.Drawing.PointF([float]($x + $tw / 2), [float]$ty)),
                (New-Object System.Drawing.PointF([float]$x, [float]($ty - $treeH * 0.62)))
            )
            $g.FillPolygon($brush, $points)
        }
        $x += 55 + $rand.Next(0, 75)
    }
    $brush.Dispose()
}

function Add-Hills($g, [int]$baseY, [int]$amplitude, [string]$color, [int]$alpha, [int]$seed) {
    $rand = New-Object System.Random($seed)
    $points = New-Object "System.Collections.Generic.List[System.Drawing.PointF]"
    $points.Add((New-Object System.Drawing.PointF(0, $H)))
    $x = 0
    while ($x -le $W) {
        $y = $baseY - [Math]::Sin(($x / [double]$W) * 3.1416 * 2 + $rand.NextDouble() * 0.2) * $amplitude
        $points.Add((New-Object System.Drawing.PointF([float]$x, [float]$y)))
        $x += 40
    }
    $points.Add((New-Object System.Drawing.PointF($W, $H)))
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb($alpha, (C $color)))
    $g.FillPolygon($brush, $points.ToArray())
    $brush.Dispose()
}

function Add-Water($g, [int]$top, [string]$topColor, [string]$bottomColor, [int]$seed) {
    $rect = New-Object System.Drawing.Rectangle(0, $top, $W, ($H - $top))
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect, (C $topColor), (C $bottomColor), 90.0)
    $g.FillRectangle($brush, $rect)
    $brush.Dispose()

    $rand = New-Object System.Random($seed)
    for ($i = 0; $i -lt 70; $i++) {
        $y = $top + $rand.Next(0, ($H - $top))
        $len = $rand.Next(20, 140)
        $x = $rand.Next(-20, ($W - 20))
        $alpha = $rand.Next(10, 55)
        $pen = New-Object System.Drawing.Pen(
            [System.Drawing.Color]::FromArgb($alpha, 255, 236, 180), 2.0)
        $g.DrawLine($pen, [float]$x, [float]$y, [float]($x + $len), [float]$y)
        $pen.Dispose()
    }
}

function Add-Lighthouse($g, [float]$x, [float]$baseY, [float]$height, [string]$body, [string]$top, [string]$beam) {
    $wBase = $height * 0.26
    $wTop = $height * 0.13
    $topY = $baseY - $height

    $points = @(
        (New-Object System.Drawing.PointF([float]($x - $wBase / 2), [float]$baseY)),
        (New-Object System.Drawing.PointF([float]($x + $wBase / 2), [float]$baseY)),
        (New-Object System.Drawing.PointF([float]($x + $wTop / 2), [float]$topY)),
        (New-Object System.Drawing.PointF([float]($x - $wTop / 2), [float]$topY))
    )
    $brush = New-Object System.Drawing.SolidBrush((C $body))
    $g.FillPolygon($brush, $points)
    $brush.Dispose()

    # franjas
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(70, (C $top)))
    $g.FillRectangle($brush, [float]($x - $wBase / 2), [float]($baseY - $height * 0.32), [float]$wBase, [float]($height * 0.10))
    $g.FillRectangle($brush, [float]($x - $wBase / 2.4), [float]($baseY - $height * 0.58), [float]($wBase / 1.2), [float]($height * 0.08))
    $brush.Dispose()

    # galería
    $brush = New-Object System.Drawing.SolidBrush((C $top))
    $g.FillRectangle($brush, [float]($x - $wTop * 0.9), [float]($topY - 10), [float]($wTop * 1.8), 12)
    $g.FillRectangle($brush, [float]($x - $wTop * 0.62), [float]($topY - 46), [float]($wTop * 1.24), 38)
    $brush.Dispose()

    # luz
    $beamBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(34, (C $beam)))
    $beamPoints = @(
        (New-Object System.Drawing.PointF([float]$x, [float]($topY - 26))),
        (New-Object System.Drawing.PointF([float]($W + 40), [float]($topY - 130))),
        (New-Object System.Drawing.PointF([float]($W + 40), [float]($topY + 90)))
    )
    $g.FillPolygon($beamBrush, $beamPoints)
    $beamBrush.Dispose()

    Add-Glow $g $x ($topY - 26) 12 $beam 120

    # techo
    $brush = New-Object System.Drawing.SolidBrush((C $top))
    $roof = @(
        (New-Object System.Drawing.PointF([float]($x - $wTop * 0.8), [float]($topY - 50))),
        (New-Object System.Drawing.PointF([float]($x + $wTop * 0.8), [float]($topY - 50))),
        (New-Object System.Drawing.PointF([float]$x, [float]($topY - 82)))
    )
    $g.FillPolygon($brush, $roof)
    $brush.Dispose()
}

function Add-Clock($g, [float]$cx, [float]$cy, [float]$r, [string]$face, [string]$rim) {
    $rimBrush = New-Object System.Drawing.SolidBrush((C $rim))
    $g.FillEllipse($rimBrush, [float]($cx - $r), [float]($cy - $r), [float]($r * 2), [float]($r * 2))
    $rimBrush.Dispose()

    $faceBrush = New-Object System.Drawing.SolidBrush((C $face))
    $g.FillEllipse($faceBrush, [float]($cx - $r * 0.9), [float]($cy - $r * 0.9), [float]($r * 1.8), [float]($r * 1.8))
    $faceBrush.Dispose()

    $pen = New-Object System.Drawing.Pen((C $rim), 5.0)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    for ($i = 0; $i -lt 12; $i++) {
        $ang = $i * [Math]::PI / 6
        $x1 = $cx + [Math]::Cos($ang) * $r * 0.74
        $y1 = $cy + [Math]::Sin($ang) * $r * 0.74
        $x2 = $cx + [Math]::Cos($ang) * $r * 0.84
        $y2 = $cy + [Math]::Sin($ang) * $r * 0.84
        $g.DrawLine($pen, [float]$x1, [float]$y1, [float]$x2, [float]$y2)
    }
    # manecillas a las 7:15
    $g.DrawLine($pen, [float]$cx, [float]$cy, [float]($cx + [Math]::Cos(1.9) * $r * 0.45), [float]($cy - [Math]::Sin(1.9) * $r * 0.45))
    $g.DrawLine($pen, [float]$cx, [float]$cy, [float]($cx + [Math]::Cos(-1.3) * $r * 0.66), [float]($cy - [Math]::Sin(-1.3) * $r * 0.66))
    $pen.Dispose()

    $dot = New-Object System.Drawing.SolidBrush((C $rim))
    $g.FillEllipse($dot, [float]($cx - 9), [float]($cy - 9), 18, 18)
    $dot.Dispose()
}

function Add-Gears($g, [float]$cx, [float]$cy, [float]$r, [string]$color, [int]$teeth, [float]$angle) {
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(120, (C $color)))
    for ($i = 0; $i -lt $teeth; $i++) {
        $a = $angle + $i * 2 * [Math]::PI / $teeth
        $x = $cx + [Math]::Cos($a) * $r
        $y = $cy + [Math]::Sin($a) * $r
        $g.FillEllipse($brush, [float]($x - $r * 0.14), [float]($y - $r * 0.14), [float]($r * 0.28), [float]($r * 0.28))
    }
    $g.FillEllipse($brush, [float]($cx - $r * 0.82), [float]($cy - $r * 0.82), [float]($r * 1.64), [float]($r * 1.64))
    $brush.Dispose()
    $hole = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(200, 12, 8, 22))
    $g.FillEllipse($hole, [float]($cx - $r * 0.3), [float]($cy - $r * 0.3), [float]($r * 0.6), [float]($r * 0.6))
    $hole.Dispose()
}

function Add-Vignette($g) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(-$W * 0.25, -$H * 0.35, $W * 1.5, $H * 1.7)
    $brush = New-Object System.Drawing.Drawing2D.PathGradientBrush($path)
    $brush.CenterColor = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
    $brush.SurroundColors = @([System.Drawing.Color]::FromArgb(150, 5, 3, 12))
    $g.FillRectangle($brush, 0, 0, $W, $H)
    $brush.Dispose()
    $path.Dispose()
}

function Save-Scene($bmp, [string]$name) {
    $encoder = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() |
        Where-Object { $_.MimeType -eq "image/jpeg" }
    $params = New-Object System.Drawing.Imaging.EncoderParameters(1)
    $params.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter(
        [System.Drawing.Imaging.Encoder]::Quality, 88L)
    $bmp.Save((Join-Path $outLocal "$name.jpg"), $encoder, $params)
    $bmp.Save((Join-Path $outRemote "$name.jpg"), $encoder, $params)
    $params.Dispose()
    "  $name.jpg"
}

# ---------------------------------------------------------------- bosque
$scene = {
    param($name, $script)
    $pair = New-Canvas
    $bmp = $pair[0]; $g = $pair[1]
    & $script $g
    Add-Vignette $g
    Save-Scene $bmp $name
    $g.Dispose(); $bmp.Dispose()
}

& $scene "bosque" {
    param($g)
    Fill-Gradient $g "#03120F" "#0B2B24"
    Add-Stars $g 120 430 3
    Add-Hills $g 580 40 "#061A16" 255 5
    Add-Trees $g 640 340 "#04100D" 235 8
    Add-Trees $g 690 250 "#020B09" 245 15
    Add-Glow $g 790 330 15 "#7FD8FF" 150
    Add-Fireflies $g 40 11 380 640
}

& $scene "bosque_puente" {
    param($g)
    Fill-Gradient $g "#040A1C" "#123055"
    Add-Stars $g 150 380 12
    Add-Moon $g 1030 140 52 "#FFE9B0"
    Add-Hills $g 470 55 "#081529" 255 21
    Add-Water $g 470 "#0A2340" "#05121F" 9
    # puente de raíces
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(230, 30, 22, 18), 16.0)
    $g.DrawArc($pen, 250, 360, 780, 260, 190, 160)
    $pen.Dispose()
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(180, 52, 38, 28), 6.0)
    $g.DrawArc($pen, 270, 380, 740, 230, 192, 156)
    $pen.Dispose()
    # pueblo de luciérnagas
    $rand = New-Object System.Random(31)
    for ($i = 0; $i -lt 26; $i++) {
        $x = $rand.Next(120, 1160)
        $y = $rand.Next(360, 455)
        Add-Glow $g $x $y 2.5 "#FFD166" 90
    }
    Add-Fireflies $g 70 17 250 640
}

& $scene "bosque_cueva" {
    param($g)
    Fill-Gradient $g "#04060F" "#101B33"
    # bóveda
    $dark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(250, 3, 4, 10))
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddBezier(0, 0, 300, 210, 980, 210, 1280, 0)
    $path.AddLine(1280, 0, 0, 0)
    $g.FillPath($dark, $path)
    $path.Dispose()
    # estalactitas
    $rand = New-Object System.Random(44)
    for ($i = 0; $i -lt 16; $i++) {
        $x = $rand.Next(0, $W)
        $len = $rand.Next(40, 150)
        $shardW = $rand.Next(16, 40)
        $points = @(
            (New-Object System.Drawing.PointF([float]($x - $shardW / 2), 0)),
            (New-Object System.Drawing.PointF([float]($x + $shardW / 2), 0)),
            (New-Object System.Drawing.PointF([float]$x, [float]$len))
        )
        $g.FillPolygon($dark, $points)
    }
    Add-Water $g 470 "#0B1C3A" "#050A16" 5
    # río de estrellas
    for ($i = 0; $i -lt 90; $i++) {
        $x = $rand.Next(20, ($W - 20))
        $y = $rand.Next(480, 715)
        $r = 0.8 + $rand.NextDouble() * 2.2
        $brush = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb((100 + $rand.Next(0, 120)), 190, 225, 255))
        $g.FillEllipse($brush, [float]($x - $r), [float]($y - $r), [float]($r * 2), [float]($r * 2))
        $brush.Dispose()
    }
    $dark.Dispose()
}

# ---------------------------------------------------------------- luna
& $scene "luna" {
    param($g)
    Fill-Gradient $g "#060818" "#2B1B55"
    Add-Stars $g 170 520 7
    Add-Moon $g 880 250 95 "#FFE9B0"
    Add-Trees $g 620 300 "#050A16" 235 3
    Add-Trees $g 690 210 "#03060F" 245 19
    Add-Hills $g 640 30 "#0A1430" 255 23
    # ventanas del pueblo
    $rand = New-Object System.Random(51)
    for ($i = 0; $i -lt 14; $i++) {
        $x = $rand.Next(60, 420)
        $y = $rand.Next(600, 690)
        $brush = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb(210, 255, 209, 102))
        $g.FillRectangle($brush, $x, $y, 9, 12)
        $brush.Dispose()
    }
    Add-Fireflies $g 20 33 450 660
}

& $scene "luna_cielo" {
    param($g)
    Fill-Gradient $g "#081128" "#3A2A6B"
    Add-Stars $g 220 560 14
    Add-Moon $g 240 160 70 "#FFF1C4"
    # cadena de estrellas
    for ($i = 0; $i -lt 9; $i++) {
        $x = 320 + $i * 92
        $y = 210 + [Math]::Sin($i * 0.7) * 60
        Add-Glow $g $x $y 5 "#FFF4D0" 120
        if ($i -lt 8) {
            $pen = New-Object System.Drawing.Pen(
                [System.Drawing.Color]::FromArgb(90, 255, 240, 190), 2.0)
            $g.DrawLine($pen, [float]$x, [float]$y, [float]($x + 92), [float]($y + [Math]::Sin(($i + 1) * 0.7) * 60))
            $pen.Dispose()
        }
    }
    Add-Hills $g 560 70 "#0C1A38" 255 27
    Add-Trees $g 640 180 "#060C1C" 230 6
}

# ---------------------------------------------------------------- mar
& $scene "mar" {
    param($g)
    Fill-Gradient $g "#03121F" "#0B3D5C"
    Add-Stars $g 90 300 18
    Add-Moon $g 1080 120 46 "#FFE9B0"
    Add-Hills $g 470 35 "#041826" 255 4
    Add-Water $g 460 "#083049" "#03101C" 11
    Add-Lighthouse $g 240 560 300 "#14202C" "#0B141C" "#FFE3A3"
    # rocas
    $rock = New-Object System.Drawing.SolidBrush((C "#050D14"))
    $points = @(
        (New-Object System.Drawing.PointF(30, 720)),
        (New-Object System.Drawing.PointF(120, 560)),
        (New-Object System.Drawing.PointF(360, 580)),
        (New-Object System.Drawing.PointF(470, 720))
    )
    $g.FillPolygon($rock, $points)
    $rock.Dispose()
    Add-Fireflies $g 10 41 300 460
}

& $scene "mar_amanecer" {
    param($g)
    Fill-Gradient $g "#2A1436" "#F2A65A"
    Add-Moon $g 980 360 90 "#FFD9A0"
    Add-Hills $g 420 45 "#4A2440" 255 8
    Add-Water $g 420 "#C97C4E" "#5A2A3A" 13
    # reflejo del sol
    $rand = New-Object System.Random(61)
    for ($i = 0; $i -lt 40; $i++) {
        $y = 430 + $rand.Next(0, 280)
        $len = 30 + $rand.Next(0, 150)
        $pen = New-Object System.Drawing.Pen(
            [System.Drawing.Color]::FromArgb((40 + $rand.Next(0, 60)), 255, 220, 160), 3.0)
        $x = 980 - $len / 2 + $rand.Next(-60, 60)
        $g.DrawLine($pen, [float]$x, [float]$y, [float]($x + $len), [float]$y)
        $pen.Dispose()
    }
    # ballena
    $whale = New-Object System.Drawing.SolidBrush((C "#1B2A3C"))
    $g.FillEllipse($whale, 330, 470, 300, 110)
    $g.FillPolygon($whale, @(
        (New-Object System.Drawing.PointF(620, 500)),
        (New-Object System.Drawing.PointF(730, 450)),
        (New-Object System.Drawing.PointF(710, 535))
    ))
    $whale.Dispose()
    $spout = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(160, 255, 245, 220), 5.0)
    $g.DrawArc($spout, 400, 390, 70, 90, 200, 120)
    $spout.Dispose()
}

& $scene "mar_faro" {
    param($g)
    Fill-Gradient $g "#041826" "#0E4A63"
    Add-Stars $g 80 280 22
    Add-Water $g 500 "#0A3B52" "#04121E" 17
    Add-Lighthouse $g 900 600 360 "#E8D8B8" "#8A6A3A" "#FFE3A3"
    # conchas en la base
    $rand = New-Object System.Random(71)
    for ($i = 0; $i -lt 22; $i++) {
        $x = $rand.Next(760, 1060)
        $y = $rand.Next(560, 640)
        $brush = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb(190, 255, 228, 200))
        $g.FillEllipse($brush, $x, $y, 15, 11)
        $brush.Dispose()
    }
    Add-Hills $g 520 25 "#03121E" 255 29
}

# ---------------------------------------------------------------- reloj
& $scene "reloj" {
    param($g)
    Fill-Gradient $g "#170A2A" "#3D1B55"
    # ventana con luz
    $window = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(230, 255, 220, 150))
    $g.FillRectangle($window, 90, 110, 250, 320)
    $window.Dispose()
    $beam = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(26, 255, 220, 150))
    $g.FillPolygon($beam, @(
        (New-Object System.Drawing.PointF(90, 430)),
        (New-Object System.Drawing.PointF(340, 430)),
        (New-Object System.Drawing.PointF(760, 720)),
        (New-Object System.Drawing.PointF(0, 720))
    ))
    $beam.Dispose()
    $frame = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(220, 20, 12, 30), 18.0)
    $g.DrawRectangle($frame, 90, 110, 250, 320)
    $g.DrawLine($frame, 215, 110, 215, 430)
    $g.DrawLine($frame, 90, 270, 340, 270)
    $frame.Dispose()

    # reloj de pie
    $body = New-Object System.Drawing.SolidBrush((C "#241033"))
    $g.FillRectangle($body, 800, 250, 250, 440)
    $g.FillRectangle($body, 760, 210, 330, 60)
    $g.FillEllipse($body, 790, 180, 270, 90)
    $body.Dispose()

    Add-Clock $g 925 200 120 "#F4E1B5" "#5A3A22"

    $g.FillRectangle((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(180, 12, 7, 20))), 845, 360, 160, 250)
    $pend = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(200, 255, 209, 102), 5.0)
    $g.DrawLine($pend, 925, 360, 925, 540)
    $pend.Dispose()
    Add-Glow $g 925 555 18 "#FFD166" 120

    Add-Gears $g 1120 470 70 "#C9A227" 10 0.2
    Add-Gears $g 1200 300 44 "#C9A227" 8 0.6
    Add-Gears $g 690 600 40 "#C9A227" 8 0.1

    # motas de polvo
    $rand = New-Object System.Random(83)
    for ($i = 0; $i -lt 60; $i++) {
        $x = $rand.Next(0, $W)
        $y = $rand.Next(0, $H)
        $r = 0.6 + $rand.NextDouble() * 1.8
        $brush = New-Object System.Drawing.SolidBrush(
            [System.Drawing.Color]::FromArgb($rand.Next(40, 130), 255, 236, 190))
        $g.FillEllipse($brush, [float]($x - $r), [float]($y - $r), [float]($r * 2), [float]($r * 2))
        $brush.Dispose()
    }
}

Write-Output "Ilustraciones generadas en:"
Write-Output "  $outLocal"
Write-Output "  $outRemote"
