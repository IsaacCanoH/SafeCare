param(
    [string]$OutputDirectory = (
        Join-Path $PSScriptRoot "..\tv\src\main\res\raw"
    )
)

$sampleRate = 22050
$amplitude = 0.42
$fadeSamples = [int]($sampleRate * 0.012)

$patterns = @(
    @(@(880, 180), @(0, 90), @(880, 260)),
    @(@(659, 240), @(784, 260), @(988, 420)),
    @(@(1047, 100), @(0, 55), @(1047, 100), @(0, 55), @(1319, 280)),
    @(@(440, 130), @(554, 130), @(659, 130), @(880, 320)),
    @(@(523, 110), @(784, 110), @(1047, 110), @(784, 110), @(1047, 300)),
    @(@(698, 190), @(0, 80), @(932, 190), @(0, 80), @(698, 300)),
    @(@(620, 170), @(930, 170), @(620, 170), @(930, 170), @(620, 340)),
    @(@(784, 180), @(0, 90), @(988, 180), @(0, 90), @(1175, 360))
)

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

for ($toneIndex = 0; $toneIndex -lt $patterns.Count; $toneIndex++) {
    $samples = [System.Collections.Generic.List[int16]]::new()

    foreach ($segment in $patterns[$toneIndex]) {
        $frequency = [double]$segment[0]
        $durationMs = [int]$segment[1]
        $segmentSamples = [int]($sampleRate * $durationMs / 1000)

        for ($sampleIndex = 0; $sampleIndex -lt $segmentSamples; $sampleIndex++) {
            if ($frequency -eq 0) {
                $samples.Add(0)
                continue
            }

            $fadeIn = [Math]::Min(1.0, $sampleIndex / [double]$fadeSamples)
            $fadeOut = [Math]::Min(
                1.0,
                ($segmentSamples - $sampleIndex - 1) / [double]$fadeSamples
            )
            $envelope = [Math]::Min($fadeIn, $fadeOut)
            $phase = 2.0 * [Math]::PI * $frequency * $sampleIndex / $sampleRate
            $fundamental = [Math]::Sin($phase)
            $harmonic = 0.22 * [Math]::Sin($phase * 2.0)
            $value = [int16](
                [Math]::Round(
                    [int16]::MaxValue * $amplitude * $envelope * ($fundamental + $harmonic)
                )
            )
            $samples.Add($value)
        }
    }

    $fileName = "alert_tone_{0}.wav" -f ($toneIndex + 1)
    $filePath = Join-Path $OutputDirectory $fileName
    $stream = [System.IO.File]::Create($filePath)
    $writer = [System.IO.BinaryWriter]::new($stream)

    try {
        $dataSize = $samples.Count * 2
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
        $writer.Write([int](36 + $dataSize))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("WAVE"))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
        $writer.Write([int]16)
        $writer.Write([int16]1)
        $writer.Write([int16]1)
        $writer.Write([int]$sampleRate)
        $writer.Write([int]($sampleRate * 2))
        $writer.Write([int16]2)
        $writer.Write([int16]16)
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
        $writer.Write([int]$dataSize)

        foreach ($sample in $samples) {
            $writer.Write($sample)
        }
    } finally {
        $writer.Dispose()
        $stream.Dispose()
    }
}

Write-Host "Generated $($patterns.Count) TV alert tones in $OutputDirectory"
