@echo off
title BioBuzz - Enviar codigo al robot
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0herramientas\Enviar-Codigo.ps1" %*
echo.
pause
