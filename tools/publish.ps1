# Trimite folderul dist\Eron pe itch.io cu butler.
# Prima dată: `butler login` (o singură dată, de mână). Apoi:   powershell -ExecutionPolicy Bypass -File tools\publish.ps1
$ItchUser = 'andreiaig'          # contul itch.io
$ItchGame = 'eron-chronicles'    # pagina jocului: https://andreiaig.itch.io/eron-chronicles
$Channel  = 'windows'

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Fail($msg) { Write-Host "`nEROARE: $msg" -ForegroundColor Red; exit 1 }

if (-not (Get-Command butler -ErrorAction SilentlyContinue)) {
    Fail "Nu găsesc 'butler' în PATH. Instalează-l (https://itch.io/docs/butler/) și rulează 'butler login'."
}
if (-not (Test-Path 'dist\Eron\Eron.exe')) { Fail 'Nu există dist\Eron\Eron.exe. Rulează întâi tools\build.ps1.' }
if (-not (Test-Path 'VERSION')) { Fail 'Lipsește fișierul VERSION.' }
$version = (Get-Content 'VERSION' -Raw).Trim()

Write-Host "Trimit versiunea $version la ${ItchUser}/${ItchGame}:${Channel} ..." -ForegroundColor Cyan
butler push dist\Eron "${ItchUser}/${ItchGame}:${Channel}" --userversion $version
if ($LASTEXITCODE -ne 0) { Fail 'butler push a eșuat.' }
Write-Host "Gata. Pagina: https://${ItchUser}.itch.io/${ItchGame}" -ForegroundColor Green
