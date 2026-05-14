@echo off
setlocal enabledelayedexpansion
title Build Generic DAO Framework

echo ===================================================
echo     Compilation et Packaging du Generic DAO
echo ===================================================

REM --- Variables de configuration ---
set "SRC_DIR=src"
set "BUILD_DIR=build"
set "DIST_DIR=dist"
set "LIB_DIR=lib"
set "JAR_NAME=GenericDAO.jar"

REM --- 1. Nettoyage et preparation des dossiers ---
echo [1/4] Nettoyage de l'environnement...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%BUILD_DIR%"
mkdir "%DIST_DIR%"
if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"

REM --- 2. Recherche des fichiers sources ---
echo [2/4] Analyse des fichiers sources Java...
dir /s /B "%SRC_DIR%\*.java" > sources.txt 2>nul
for /F "usebackq" %%A in ('sources.txt') do set size=%%~zA
if %size% EQU 0 (
    echo Erreur : Aucun fichier .java trouve dans le dossier %SRC_DIR%.
    del sources.txt
    goto :finError
)

REM --- 3. Compilation ---
echo [3/4] Compilation des classes en cours...
REM On inclut toutes les librairies du dossier lib dans le classpath (utile pour le driver JDBC)
javac -encoding UTF-8 -d "%BUILD_DIR%" -cp "%LIB_DIR%\*" @sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERREUR] La compilation a echoue. Verifiez votre code source.
    del sources.txt
    goto :finError
)

REM --- 4. Creation de l'archive JAR ---
echo [4/4] Creation du fichier %JAR_NAME%...
cd "%BUILD_DIR%"
jar cvf "../%DIST_DIR%/%JAR_NAME%" . > nul
cd ..

REM --- Nettoyage des fichiers temporaires ---
del sources.txt

echo.
echo ===================================================
echo   SUCCES ! Build termine sans erreur.
echo   Fichier genere : %DIST_DIR%\%JAR_NAME%
echo ===================================================
pause
exit /b 0

:finError
echo ===================================================
echo   ECHEC DU BUILD.
echo ===================================================
pause
exit /b 1