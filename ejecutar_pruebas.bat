@echo off
REM =====================================================================
REM  ejecutar_pruebas.bat - Compila y ejecuta las pruebas automaticas
REM  Uso (PowerShell o CMD):  .\ejecutar_pruebas.bat
REM =====================================================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"

call build.bat
if errorlevel 1 exit /b 1

echo.
echo Ejecutando calculadora.pruebas.PruebasCalculadora ...
echo.
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp build\classes calculadora.pruebas.PruebasCalculadora
set RESULTADO=%ERRORLEVEL%

echo.
if "%RESULTADO%"=="0" (
    echo Todas las pruebas pasaron.
) else (
    echo Hay pruebas fallidas. Codigo de salida: %RESULTADO%
)
endlocal & exit /b %RESULTADO%
