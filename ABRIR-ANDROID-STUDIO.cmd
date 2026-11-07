@echo off
title Abrir Android Studio (FTC BioBuzz)
set "STUDIO=C:\Program Files\Android\Android Studio2\bin\studio64.exe"
if not exist "%STUDIO%" set "STUDIO=C:\Program Files\Android\Android Studio1\bin\studio64.exe"
if not exist "%STUDIO%" set "STUDIO=C:\Program Files\Android\Android Studio\bin\studio64.exe"
if not exist "%STUDIO%" (
    echo No encuentro Android Studio.
    echo Revisa C:\Program Files\Android\
    pause
    exit /b 1
)
start "" "%STUDIO%" "%~dp0."
