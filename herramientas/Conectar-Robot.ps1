<#
    Conectar-Robot.ps1  -  BioBuzz FTC
    Deja el Control Hub (robot) visible para Android Studio.

    Uso:
        .\Conectar-Robot.ps1              -> prueba USB y WiFi automaticamente
        .\Conectar-Robot.ps1 -Modo wifi   -> solo WiFi (192.168.43.1:5555)
        .\Conectar-Robot.ps1 -Modo usb    -> solo cable USB-C
#>
param(
    [ValidateSet('auto', 'wifi', 'usb')]
    [string]$Modo = 'auto',
    [string]$IpRobot = '192.168.43.1',
    [int]$Puerto = 5555
)

$ErrorActionPreference = 'Continue'
$Host.UI.RawUI.WindowTitle = 'BioBuzz - Conectar robot'

function Linea { Write-Host ('-' * 62) -ForegroundColor DarkGray }
function Paso($t) { Write-Host "`n>> $t" -ForegroundColor Cyan }
function Ok($t) { Write-Host "   [OK] $t" -ForegroundColor Green }
function Aviso($t) { Write-Host "   [!]  $t" -ForegroundColor Yellow }
function Fallo($t) { Write-Host "   [X]  $t" -ForegroundColor Red }
function Dato($t) { Write-Host "        $t" -ForegroundColor Gray }

# ---------------------------------------------------------------- adb
$adb = $null
$rutas = @(
    (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'),
    'C:\Android\Sdk\platform-tools\adb.exe'
)
foreach ($r in $rutas) { if (Test-Path $r) { $adb = $r; break } }
if (-not $adb) {
    $cmd = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($cmd) { $adb = $cmd.Source }
}
if (-not $adb) {
    Fallo 'No encuentro adb.exe. Revisa que el Android SDK este en:'
    Dato "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
    exit 1
}

function Lista-Dispositivos {
    $salida = & $adb devices -l 2>&1
    return @($salida | Where-Object { $_ -match '^\S+\s+(device|offline|unauthorized|recovery)' })
}

function Mostrar-Dispositivos($lista) {
    if (-not $lista -or $lista.Count -eq 0) {
        Write-Host '   (adb no ve ningun dispositivo)' -ForegroundColor DarkGray
        return $false
    }
    foreach ($d in $lista) {
        if ($d -match '\sdevice\s') { Write-Host "   $d" -ForegroundColor Green }
        elseif ($d -match 'unauthorized') { Write-Host "   $d" -ForegroundColor Yellow }
        else { Write-Host "   $d" -ForegroundColor DarkYellow }
    }
    return (($lista | Where-Object { $_ -match '\sdevice\s' }).Count -gt 0)
}

function Info-Red {
    $ips = @()
    try {
        $ips = [System.Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces() |
            Where-Object { $_.OperationalStatus -eq 'Up' -and $_.NetworkInterfaceType -ne 'Loopback' } |
            ForEach-Object { $_.GetIPProperties().UnicastAddresses } |
            Where-Object { $_.Address.AddressFamily -eq 'InterNetwork' } |
            ForEach-Object { $_.Address.IPAddressToString }
    }
    catch { }
    return @($ips)
}

# Comprobacion rapida (2.5 s) de si el puerto del robot responde.
function Puerto-Abierto($host_, $puerto) {
    $cliente = New-Object System.Net.Sockets.TcpClient
    try {
        $tarea = $cliente.ConnectAsync($host_, $puerto)
        if ($tarea.Wait(2500) -and $cliente.Connected) { return $true }
        return $false
    }
    catch { return $false }
    finally { $cliente.Close() }
}

function Intentar-Wifi {
    $ips = Info-Red
    foreach ($ip in $ips) { Dato "IP de la laptop: $ip" }

    $enRedRobot = @($ips | Where-Object { $_ -like '192.168.43.*' })
    $enRedTel = @($ips | Where-Object { $_ -like '192.168.49.*' })

    if ($enRedRobot.Count -eq 0 -and $enRedTel.Count -eq 0) {
        Aviso 'La laptop NO esta en la red del robot (192.168.43.x o 192.168.49.x).'
        Dato '1) Enciende el robot y el Driver Station.'
        Dato '2) En Windows, conecta el WiFi que se llama FTC-xxxx.'
        Dato '3) Vuelve a ejecutar este programa.'
        return $false
    }

    $objetivos = @($IpRobot)
    if ($enRedTel.Count -gt 0 -and $objetivos -notcontains '192.168.49.1') {
        $objetivos += '192.168.49.1'
    }

    foreach ($ip in $objetivos) {
        $objetivo = "${ip}:$Puerto"
        Paso "Conectando por WiFi a $objetivo"

        if (-not (Puerto-Abierto $ip $Puerto)) {
            Aviso "El puerto $Puerto de $ip no responde."
            Dato 'El Control Hub esta apagado, reiniciandose, o el ADB inalambrico esta desactivado.'
            Dato 'Prueba el cable USB-C, o apaga/enciende el Control Hub y reintenta.'
            continue
        }

        & $adb disconnect $objetivo 2>&1 | Out-Null
        for ($i = 1; $i -le 2; $i++) {
            Write-Host "   Intento $i de 2..." -ForegroundColor Gray
            $r = & $adb connect $objetivo 2>&1
            Write-Host "   $r" -ForegroundColor DarkGray
            Start-Sleep -Seconds 2
            $lista = Lista-Dispositivos
            if (Mostrar-Dispositivos $lista) { return $true }
            if ($i -lt 2) { Start-Sleep -Seconds 2 }
        }
        Aviso "El robot responde en $ip pero adb dice \"offline\"."
        Dato 'Desconecta y vuelve a conectar el WiFi, o usa el cable USB-C.'
    }
    return $false
}

function Intentar-Usb {
    Paso 'Buscando el robot por cable USB'
    $lista = Lista-Dispositivos
    if (Mostrar-Dispositivos $lista) { return $true }
    Aviso 'No se ve ningun celular/Control Hub por USB.'
    Dato 'Revisa: cable USB-C de datos (no solo de carga), robot encendido,'
    Dato 'y en el Administrador de dispositivos que no salga "desconocido".'
    return $false
}

# ---------------------------------------------------------------- inicio
Linea
Write-Host '   BIOBUZZ FTC  -  conexion con el robot' -ForegroundColor White
Linea

Paso 'Reiniciando el servicio adb (quita el error "device offline")'
& $adb kill-server 2>&1 | Out-Null
& $adb start-server 2>&1 | Out-Null
Ok "adb listo ($adb)"

$conectado = $false

if ($Modo -eq 'auto' -or $Modo -eq 'usb') {
    if (Intentar-Usb) { $conectado = $true }
}

if (-not $conectado -and ($Modo -eq 'auto' -or $Modo -eq 'wifi')) {
    if (Intentar-Wifi) { $conectado = $true }
}

Linea
if ($conectado) {
    Write-Host '   ROBOT CONECTADO' -ForegroundColor Green
    Write-Host '   Ya puedes darle Run (triangulo verde) en Android Studio.' -ForegroundColor White
    Write-Host '   Arriba a la derecha debe decir: REV Robotics Control Hub' -ForegroundColor Gray
    Linea
    exit 0
}
else {
    Write-Host '   EL ROBOT NO APARECE TODAVIA' -ForegroundColor Red
    Write-Host '   Lo que suele ser:' -ForegroundColor White
    Dato '- El robot esta apagado, o la laptop no esta en el WiFi FTC-xxxx.'
    Dato '- El cable USB-C es solo de carga.'
    Dato '- Hay que aceptar "Permitir depuracion USB" en el Driver Station.'
    Write-Host '   Detalle paso a paso en CONECTAR-Y-ENVIAR.md' -ForegroundColor Gray
    Linea
    exit 1
}
