#!/bin/sh
# Compila todo el proyecto (Linux / Mac). Ejecutar desde la carpeta pos
rm -rf out && mkdir out
javac -encoding UTF-8 -cp lib/ojdbc11.jar -d out $(find src -name "*.java") && echo "Compilacion terminada correctamente."
