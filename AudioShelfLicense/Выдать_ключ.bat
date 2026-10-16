@echo off
chcp 65001 >nul
cd /d "%~dp0"
node license.mjs
if errorlevel 1 echo.
echo Файл licenses.txt лежит в этой же папке.
pause
