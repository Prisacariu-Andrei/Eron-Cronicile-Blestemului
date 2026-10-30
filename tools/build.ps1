# Construiește jocul ca folder autonom pentru Windows: dist\Eron\Eron.exe (cu Java inclus).
# Folosire (din rădăcina proiectului):   powershell -ExecutionPolicy Bypass -File tools\build.ps1 [-Smoke]
#   -Smoke : după build pornește Eron.exe ~5 secunde, verifică procesul și %APPDATA%\Eron, apoi îl închide.
param([switch]$Smoke)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Fail($msg) { Write-Host "`nEROARE: $msg" -ForegroundColor Red; exit 1 }

# --- versiunea ---
if (-not (Test-Path 'VERSION')) { Fail 'Lipsește fișierul VERSION din rădăcina proiectului.' }
$version = (Get-Content 'VERSION' -Raw).Trim()
if ($version -notmatch '^\d+(\.\d+){0,2}$') { Fail "VERSION trebuie să fie de forma 1.2.3 (acum: '$version')." }

# --- JDK: JAVA_HOME, PATH, apoi JDK-urile din IntelliJ (~/.jdks) ---
function Find-Tool($name) {
    $cands = @()
    if ($env:JAVA_HOME) { $cands += (Join-Path $env:JAVA_HOME "bin\$name.exe") }
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { $cands += $cmd.Source }
    $jdks = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path $jdks) { Get-ChildItem $jdks -Directory | Sort-Object Name -Descending | ForEach-Object { $cands += (Join-Path $_.FullName "bin\$name.exe") } }
    foreach ($c in $cands) { if ($c -and (Test-Path $c)) { return $c } }
    return $null
}
$javac = Find-Tool 'javac'; $jar = Find-Tool 'jar'; $jpackage = Find-Tool 'jpackage'; $jdeps = Find-Tool 'jdeps'
foreach ($t in @(@('javac', $javac), @('jar', $jar), @('jpackage', $jpackage))) {
    if (-not $t[1]) { Fail "Nu găsesc '$($t[0])'. Instalează un JDK complet (17+), setează JAVA_HOME și rulează din nou." }
}
Write-Host "JDK: $(Split-Path -Parent (Split-Path -Parent $javac))" -ForegroundColor DarkGray

# --- bibliotecile (sqlite-jdbc + slf4j-api), din .idea/libraries/xerial_sqlite_jdbc.xml ---
$m2 = if ($env:MAVEN_REPOSITORY) { $env:MAVEN_REPOSITORY } else { Join-Path $env:USERPROFILE '.m2\repository' }
$xml = Get-Content '.idea\libraries\xerial_sqlite_jdbc.xml' -Raw
$libs = [regex]::Matches($xml, 'jar://\$MAVEN_REPOSITORY\$/([^!]+)!') | ForEach-Object { Join-Path $m2 ($_.Groups[1].Value -replace '/', '\') }
if (-not $libs) { Fail 'Nu am găsit biblioteca sqlite-jdbc în .idea\libraries\xerial_sqlite_jdbc.xml.' }
foreach ($l in $libs) { if (-not (Test-Path $l)) { Fail "Lipsește $l. Deschide proiectul în IntelliJ ca să descarce bibliotecile Maven." } }

# --- curățenie ---
Remove-Item build, dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory build\classes, build\lib, dist | Out-Null

# --- compilare ---
Write-Host "Compilez sursele..." -ForegroundColor Cyan
$sources = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$cp = ($libs -join ';')
& $javac -encoding UTF-8 -cp $cp -d build\classes $sources
if ($LASTEXITCODE -ne 0) { Fail 'javac a eșuat (vezi erorile de mai sus).' }

# --- resursele (getResource("/Data/..."), "/lang/...", imagini) ---
Copy-Item res\* build\classes -Recurse -Force

# --- clasa principală, citită din Main.java ---
$mainSrc = Get-Content src\Main\Main.java -Raw
$pkg = [regex]::Match($mainSrc, '(?m)^\s*package\s+([\w.]+)\s*;').Groups[1].Value
$cls = [regex]::Match($mainSrc, 'public\s+class\s+(\w+)').Groups[1].Value
$mainClass = if ($pkg) { "$pkg.$cls" } else { $cls }

# --- Eron.jar + bibliotecile alături (Class-Path) ---
foreach ($l in $libs) { Copy-Item $l build\lib }
$libNames = ($libs | ForEach-Object { Split-Path -Leaf $_ }) -join ' '
@("Main-Class: $mainClass", "Class-Path: $libNames", "Implementation-Version: $version") | Set-Content build\manifest.txt -Encoding ASCII
Write-Host "Creez Eron.jar ($mainClass)..." -ForegroundColor Cyan
& $jar --create --file build\lib\Eron.jar --manifest build\manifest.txt -C build\classes .
if ($LASTEXITCODE -ne 0) { Fail 'jar a eșuat.' }

# --- modulele Java necesare: jdeps + lista de rezervă ---
$modules = [System.Collections.Generic.HashSet[string]]::new()
foreach ($m in 'java.desktop', 'java.sql', 'java.naming', 'jdk.unsupported', 'java.logging') { [void]$modules.Add($m) }
if ($jdeps) {
    try {
        $out = & $jdeps --print-module-deps --ignore-missing-deps --multi-release base --class-path 'build\lib\*' build\lib\Eron.jar 2>$null
        if ($LASTEXITCODE -eq 0 -and $out) { foreach ($m in ($out -join ',').Split(',')) { if ($m.Trim()) { [void]$modules.Add($m.Trim()) } } }
    } catch { Write-Host 'jdeps a eșuat; folosesc lista de rezervă.' -ForegroundColor Yellow }
} else { Write-Host 'jdeps nu e disponibil; folosesc lista de rezervă.' -ForegroundColor Yellow }
$modList = ($modules | Sort-Object) -join ','
Write-Host "Module Java: $modList" -ForegroundColor DarkGray

# --- jpackage: folder autonom (fără consolă; iconiță doar dacă există un .ico în res/) ---
$jp = @('--type', 'app-image', '--name', 'Eron', '--app-version', $version, '--input', 'build\lib',
        '--main-jar', 'Eron.jar', '--main-class', $mainClass, '--add-modules', $modList,
        '--java-options', '-Dfile.encoding=UTF-8', '--dest', 'dist')
$ico = Get-ChildItem res -Recurse -Filter *.ico -ErrorAction SilentlyContinue | Select-Object -First 1
if ($ico) { $jp += @('--icon', $ico.FullName) }
Write-Host "Rulez jpackage..." -ForegroundColor Cyan
& $jpackage @jp
if ($LASTEXITCODE -ne 0) { Fail 'jpackage a eșuat.' }

$exe = 'dist\Eron\Eron.exe'
if (-not (Test-Path $exe)) { Fail "jpackage a terminat, dar $exe nu există." }
$mb = [math]::Round(((Get-ChildItem dist\Eron -Recurse -File | Measure-Object Length -Sum).Sum) / 1MB, 1)
Write-Host "`nGata: $exe  (v$version, $mb MB)" -ForegroundColor Green

# --- test de fum ---
# Rulează cu un %APPDATA% temporar, ca testul să fie concludent (folderul trebuie creat de joc) și să nu atingă salvările tale.
if ($Smoke) {
    Write-Host "Test de fum: pornesc Eron.exe 5 secunde (APPDATA temporar)..." -ForegroundColor Cyan
    $fakeAppData = Join-Path $env:TEMP ("eron_smoke_" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory $fakeAppData | Out-Null
    $realAppData = $env:APPDATA
    $env:APPDATA = $fakeAppData
    try {
        $p = Start-Process -FilePath (Resolve-Path $exe) -PassThru
        Start-Sleep -Seconds 5
        $alive = -not $p.HasExited
        Get-Process -Name Eron -ErrorAction SilentlyContinue | Stop-Process -Force
    } finally { $env:APPDATA = $realAppData }
    Start-Sleep -Milliseconds 500
    $data = Join-Path $fakeAppData 'Eron'
    $dirOk = Test-Path $data
    $dbOk = Test-Path (Join-Path $data 'eron_save.db')
    Write-Host ("  procesul rulează după 5 s                 : {0}" -f $alive)
    Write-Host ("  folderul de date creat (APPDATA\Eron)      : {0}" -f $dirOk)
    Write-Host ("  eron_save.db creat acolo                  : {0}" -f $dbOk)
    Remove-Item $fakeAppData -Recurse -Force -ErrorAction SilentlyContinue
    if (-not ($alive -and $dirOk -and $dbOk)) { Fail 'Testul de fum a eșuat.' }
    Write-Host 'Testul de fum a trecut.' -ForegroundColor Green
}
