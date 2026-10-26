@echo off
REM Pruebas SIN base de datos
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out;lib\ojdbc11.jar com.supermercado.pos.Pruebas
