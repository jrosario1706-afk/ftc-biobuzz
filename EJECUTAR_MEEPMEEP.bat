@echo off
title BioBuzz - Simulador MeepMeep
cd /d "%~dp0"
for /f "delims=" %%D in ('dir /b /ad /o-d "C:\Program Files\Android\Android Studio*" 2^>nul') do (
    if exist "C:\Program Files\Android\%%D\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Android\%%D\jbr"
        goto :found_java
    )
)
:found_java
echo [INFO] Iniciando el simulador MeepMeep en tu pantalla...
call gradlew.bat :meepmeep:run
pause
