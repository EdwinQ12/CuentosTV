# Genera la narración de un cuento usando las voces instaladas en Windows (SAPI)
# y la convierte a MP3 con ffmpeg si está disponible.
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File tools\generar-audio.ps1 `
#     -Story app\src\main\assets\stories\bosque.json `
#     -Out app\src\main\assets\stories\audio
#
#   powershell -ExecutionPolicy Bypass -File tools\generar-audio.ps1 `
#     -Story tv-content\stories\reloj.json -Out tv-content\audio
#
# Los archivos se nombran <idCuento>_<idEscena>.mp3 (o .wav si no hay ffmpeg).

param(
    [Parameter(Mandatory = $true)][string]$Story,
    [Parameter(Mandatory = $true)][string]$Out,
    [string]$Voice = "Microsoft Sabina Desktop",
    [int]$Rate = -1,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Speech

if (-not (Test-Path $Story)) { throw "No existe el cuento: $Story" }
New-Item -ItemType Directory -Force -Path $Out | Out-Null

$json = Get-Content $Story -Raw | ConvertFrom-Json
$storyId = $json.id
if (-not $storyId) { throw "El cuento no tiene id" }

$ffmpeg = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ffmpeg) {
    $candidate = & python -c "import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())" 2>$null
    if ($candidate -and (Test-Path $candidate)) { $ffmpeg = $candidate }
}
$extension = if ($ffmpeg) { "mp3" } else { "wav" }

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$voiceInfo = $synth.GetInstalledVoices() |
    Where-Object { $_.VoiceInfo.Name -eq $Voice } |
    Select-Object -First 1
if (-not $voiceInfo) {
    $available = ($synth.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo.Name }) -join ", "
    throw "No se encontró la voz '$Voice'. Disponibles: $available"
}
$synth.SelectVoice($Voice)
$synth.Rate = $Rate

$temp = Join-Path $env:TEMP "cuentostv_audio"
New-Item -ItemType Directory -Force -Path $temp | Out-Null

Write-Output "Cuento: $storyId · Voz: $Voice · Formato: $extension"

$properties = $json.nodes.PSObject.Properties
foreach ($node in $properties) {
    $nodeId = $node.Name
    $data = $node.Value
    $text = [string]$data.text
    if ($data.endingTitle) { $text = "$($data.endingTitle). $text" }
    if ([string]::IsNullOrWhiteSpace($text)) { continue }

    $target = Join-Path $Out ("{0}_{1}.{2}" -f $storyId, $nodeId, $extension)
    if ((Test-Path $target) -and -not $Force) {
        Write-Output "  = $([System.IO.Path]::GetFileName($target)) (ya existe)"
        continue
    }

    $wav = Join-Path $temp ("{0}_{1}.wav" -f $storyId, $nodeId)
    $synth.SetOutputToWaveFile($wav)
    $synth.Speak($text)
    $synth.SetOutputToNull()

    if ($ffmpeg) {
        & $ffmpeg -y -loglevel error -i $wav -ac 1 -ar 22050 -b:a 64k $target
        if ($LASTEXITCODE -ne 0) { throw "ffmpeg falló con $nodeId" }
        Remove-Item $wav -Force
    } else {
        Move-Item $wav $target -Force
    }

    $kb = [Math]::Round((Get-Item $target).Length / 1KB)
    Write-Output "  + $([System.IO.Path]::GetFileName($target)) ($kb KB)"
}

$synth.Dispose()
Write-Output ""
Write-Output "Listo. Agrega el campo `"audio`" a cada escena, por ejemplo:"
Write-Output "  `"audio`": `"${storyId}_inicio.$extension`""
