@echo off
REM Compila todo el proyecto. Ejecutar desde la carpeta pos
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 -cp lib\ojdbc11.jar -d out @fuentes.txt
if errorlevel 1 (echo ERROR al compilar & exit /b 1)
del fuentes.txt
echo Compilacion terminada correctamente.
