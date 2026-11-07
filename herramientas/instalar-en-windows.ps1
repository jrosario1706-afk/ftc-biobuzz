<#
    instalar-en-windows.ps1  -  BioBuzz FTC
    Deja el acceso directo bueno, JAVA_HOME y las herramientas a mano.
    (Requiere permisos para escribir en el escritorio y en las variables de usuario.)
#>
$ErrorActionPreference = 'Continue'

$proyecto = Split-Path -Parent $PSScriptRoot
$studio = 'C:\Program Files\Android\Android Studio2\bin\studio64.exe'
$jdk = 'C:\Program Files\Android\Android Studio2\jbr'

function Ok($t) { Write-Host "   [OK] $t" -ForegroundColor Green }
function Aviso($t) { Write-Host "   [!]  $t" -ForegroundColor Yellow }
function Fallo($t) { Write-Host "   [X]  $t" -ForegroundColor Red }

Write-Host "`n=== 1. JAVA_HOME ===" -ForegroundColor Cyan
if (Test-Path (Join-Path $jdk 'bin\java.exe')) {
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'User')
    Ok "JAVA_HOME (usuario) = $jdk"
}
else {
    Fallo "No existe $jdk"
}

Write-Host "`n=== 2. Accesos directos ===" -ForegroundColor Cyan
$shell = New-Object -ComObject WScript.Shell

function Nuevo-Acceso($carpeta, $nombre, $destino, $argumentos, $descripcion) {
    try {
        if (-not (Test-Path $carpeta)) { New-Item -ItemType Directory -Path $carpeta -Force | Out-Null }
        $ruta = Join-Path $carpeta "$nombre.lnk"
        $lnk = $shell.CreateShortcut($ruta)
        $lnk.TargetPath = $destino
        if ($argumentos) { $lnk.Arguments = $argumentos }
        $lnk.WorkingDirectory = $proyecto
        $lnk.Description = $descripcion
        $lnk.IconLocation = "$studio,0"
        $lnk.Save()
        Ok $ruta
    }
    catch { Fallo "No se pudo crear $nombre en $carpeta : $($_.Exception.Message)" }
}

$escritorio = [Environment]::GetFolderPath('Desktop')
$inicio = Join-Path ([Environment]::GetFolderPath('Programs')) 'BioBuzz FTC'

Nuevo-Acceso $escritorio 'Android Studio BioBuzz' $studio "`"$proyecto`"" 'Abre Android Studio con el proyecto FTC'
Nuevo-Acceso $escritorio '1 Conectar robot' (Join-Path $proyecto '1-CONECTAR-ROBOT.cmd') '' 'Conecta el Control Hub (USB o WiFi)'
Nuevo-Acceso $escritorio '2 Enviar codigo al robot' (Join-Path $proyecto '2-ENVIAR-CODIGO.cmd') '' 'Compila e instala TeamCode en el robot'

Nuevo-Acceso $inicio 'Android Studio BioBuzz' $studio "`"$proyecto`"" 'Abre Android Studio con el proyecto FTC'
Nuevo-Acceso $inicio '1 Conectar robot' (Join-Path $proyecto '1-CONECTAR-ROBOT.cmd') '' 'Conecta el Control Hub'
Nuevo-Acceso $inicio '2 Enviar codigo al robot' (Join-Path $proyecto '2-ENVIAR-CODIGO.cmd') '' 'Compila e instala TeamCode'
Nuevo-Acceso $inicio 'Guia conectar y enviar' (Join-Path $proyecto 'CONECTAR-Y-ENVIAR.md') '' 'Guia paso a paso'

Write-Host "`nListo." -ForegroundColor Green
