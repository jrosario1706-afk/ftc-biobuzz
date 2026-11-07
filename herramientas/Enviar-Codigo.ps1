<#
    Enviar-Codigo.ps1  -  BioBuzz FTC
    Compila TeamCode y lo instala en el Control Hub (robot).

    Uso:
        .\Enviar-Codigo.ps1                  -> compila e instala (debug)
        .\Enviar-Codigo.ps1 -CompilarSolamente
        .\Enviar-Codigo.ps1 -SinVerificarRobot
#>
param(
    [switch]$CompilarSolamente,
    [switch]$SinVerificarRobot
)

$ErrorActionPreference = 'Continue'
$Host.UI.RawUI.WindowTitle = 'BioBuzz - Enviar codigo al robot'

function Linea { Write-Host ('-' * 62) -ForegroundColor DarkGray }
function Paso($t) { Write-Host "`n>> $t" -ForegroundColor Cyan }
function Ok($t) { Write-Host "   [OK] $t" -ForegroundColor Green }
function Aviso($t) { Write-Host "   [!]  $t" -ForegroundColor Yellow }
function Fallo($t) { Write-Host "   [X]  $t" -ForegroundColor Red }

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz
Linea
Write-Host '   BIOBUZZ FTC  -  compilar y enviar al robot' -ForegroundColor White
Linea
Write-Host "   Proyecto: $raiz" -ForegroundColor DarkGray

# ---------------------------------------------------------------- Java
$jdk = $null
$candidatos = @(
    'C:\Program Files\Android\Android Studio2\jbr',
    'C:\Program Files\Android\Android Studio1\jbr',
    'C:\Program Files\Android\Android Studio\jbr',
    $env:JAVA_HOME
)
foreach ($c in $candidatos) {
    if ($c -and (Test-Path (Join-Path $c 'bin\java.exe'))) { $jdk = $c; break }
}
if (-not $jdk) {
    Fallo 'No encuentro un Java (JDK) valido.'
    Write-Host '   Revisa que Android Studio este instalado en:' -ForegroundColor Gray
    Write-Host '   C:\Program Files\Android\Android Studio2' -ForegroundColor Gray
    exit 1
}
$env:JAVA_HOME = $jdk
$env:PATH = (Join-Path $jdk 'bin') + ';' + $env:PATH
Ok "Java: $jdk"

# ---------------------------------------------------------------- robot
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb.exe' }

function Hay-Robot {
    $salida = & $adb devices 2>&1
    return (@($salida | Where-Object { $_ -match '^\S+\s+device$' }).Count -gt 0)
}

if (-not $CompilarSolamente -and -not $SinVerificarRobot) {
    if (-not (Hay-Robot)) {
        Paso 'El robot no aparece: intento conectarlo'
        & (Join-Path $PSScriptRoot 'Conectar-Robot.ps1')
        if (-not (Hay-Robot)) {
            Fallo 'Sin robot conectado, no se puede instalar.'
            Write-Host '   Conecta el WiFi FTC-xxxx o el cable USB-C y reintenta.' -ForegroundColor Gray
            exit 1
        }
    }
    Ok 'Robot conectado'
}

# ---------------------------------------------------------------- gradle
$tarea = ':TeamCode:assembleDebug'
if (-not $CompilarSolamente) { $tarea = ':TeamCode:installDebug' }

Paso "Ejecutando $tarea (puede tardar 1-3 minutos)"
& (Join-Path $raiz 'gradlew.bat') $tarea --console=plain
$codigo = $LASTEXITCODE

Linea
if ($codigo -eq 0) {
    if ($CompilarSolamente) {
        Ok 'Compilado. APK en TeamCode\build\outputs\apk\debug\TeamCode-debug.apk'
    }
    else {
        Ok 'Codigo instalado en el robot.'
        Write-Host '   Abre el Driver Station y elige tu OpMode.' -ForegroundColor White
    }
    Linea
    exit 0
}
else {
    Fallo "Gradle termino con error (codigo $codigo)."
    Write-Host '   Lee el primer "error:" de arriba: ahi esta el archivo y la linea.' -ForegroundColor Gray
    Linea
    exit $codigo
}
