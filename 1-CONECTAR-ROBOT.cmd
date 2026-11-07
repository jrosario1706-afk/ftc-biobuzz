@echo off
title BioBuzz - Conectar robot
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0herramientas\Conectar-Robot.ps1" %*
echo.
pause
