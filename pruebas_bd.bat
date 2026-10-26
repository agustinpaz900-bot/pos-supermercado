@echo off
REM Pruebas CON base de datos (usa el usuario pos_test, NO ensucia los datos reales)
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Ddb.usuario=pos_test -cp out;lib\ojdbc11.jar com.supermercado.pos.PruebasBD
