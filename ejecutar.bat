@echo off
REM =====================================================================
REM  ejecutar.bat - Inicia la Calculadora de Calculo (compila si hace falta)
REM  Uso: doble clic, o en PowerShell:  .\ejecutar.bat
REM =====================================================================
setlocal
cd /d "%~dp0"

if not exist dist\CalculadoraCalculo.jar (
    call build.bat
    if errorlevel 1 (
        pause
        exit /b 1
    )
)

REM Se ejecuta desde esta carpeta para que se lea config.properties
java -Dfile.encoding=UTF-8 -jar dist\CalculadoraCalculo.jar
if errorlevel 1 pause
endlocal
