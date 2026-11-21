@echo off
setlocal
cd /d "%~dp0"
title Galactic Purpose 0.1.0-alpha.1 - WIP Playtest Client

echo ============================================================================
echo   Galactic Purpose (0.1.0-alpha.1 - Erken Gelistirme / WIP Test Surumu)
echo   Minecraft 1.21.1 Fabric Test Istemcisi Baslatiliyor...
echo ============================================================================
echo.
echo   Oyun acildiginda hizli deneme komutu (Hileler acik Dunyada):
echo     /galacticpurpose wormhole build colossal stable hephaestus_iv
echo     /give @s galacticpurpose:flight_director
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo [HATA] Bilgisayarda Java bulunamadi!
    echo Lutfen Java 21 ^(JDK 21+^) kurup tekrar deneyin: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

call "%~dp0gradlew.bat" runClient
set "EXIT_CODE=%errorlevel%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo ============================================================================
    echo   [HATA] Istemci hata koduyla kapandi ^(%EXIT_CODE%^). Yukaridaki ciktiyi kontrol edin.
    echo ============================================================================
    pause
)

exit /b %EXIT_CODE%
