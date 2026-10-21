@echo off
REM =====================================================================
REM  build.bat - Compila el proyecto y genera dist\CalculadoraCalculo.jar
REM  Uso (PowerShell o CMD, desde la carpeta del proyecto):  .\build.bat
REM  Requisitos: JDK 11 o superior (javac y jar en el PATH)
REM =====================================================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] No se encontro javac. Instale un JDK 11 o superior y agreguelo al PATH.
    exit /b 1
)

echo [1/3] Limpiando la carpeta build...
if exist build rmdir /s /q build
mkdir build\classes

echo [2/3] Compilando las fuentes (javac --release 11)...
REM Con -sourcepath, javac compila automaticamente todas las clases que usan
REM Main y PruebasCalculadora (es decir, todo el proyecto).
javac -encoding UTF-8 --release 11 -d build\classes -sourcepath src src\calculadora\Main.java src\calculadora\pruebas\PruebasCalculadora.java
if errorlevel 1 (
    echo [ERROR] La compilacion fallo. Revise los mensajes anteriores.
    exit /b 1
)

echo [3/3] Empaquetando dist\CalculadoraCalculo.jar...
if not exist dist mkdir dist
jar --create --file dist\CalculadoraCalculo.jar --main-class calculadora.Main -C build\classes .
if errorlevel 1 (
    echo [ERROR] No se pudo crear el archivo JAR.
    exit /b 1
)

echo.
echo Listo. Para ejecutar la calculadora:  java -jar dist\CalculadoraCalculo.jar
echo        o haga doble clic en ejecutar.bat
endlocal
exit /b 0
