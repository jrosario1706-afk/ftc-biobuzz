@echo off
title Subir codigo a GitHub (FTC BioBuzz 24096)
cd /d "%~dp0"
echo ========================================================
echo       SUBIENDO CODIGO BIOBUZZ A GITHUB
echo       Repositorio: https://github.com/jrosario1706-afk/ftc-biobuzz
echo ========================================================
echo.
git push origin main
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [AVISO] Permiso denegado con la cuenta actual del sistema.
    echo Abriendo ventana para iniciar sesion como 'jrosario1706-afk'...
    echo.
    "C:\Users\jhoan\AppData\Local\hermes\git\mingw64\bin\git-credential-manager.exe" github login --username jrosario1706-afk --web
    echo.
    echo Reintentando push a origin main...
    git push origin main
)
echo.
if %ERRORLEVEL% EQU 0 (
    echo ========================================================
    echo  TODO EL CODIGO SE HA SUBIDO EXITOSAMENTE A GITHUB!
    echo ========================================================
) else (
    echo ========================================================
    echo Si no pudiste iniciar sesion, tambien puedes agregar
    echo a 'jhoandrypolanco3-debug' como colaborador en:
    echo https://github.com/jrosario1706-afk/ftc-biobuzz/settings/access
    echo ========================================================
)
echo.
pause
