@echo off
setlocal enabledelayedexpansion
title Java Quick Run (Compile + Run)

REM --- Configuration des dossiers ---
set "SRC_DIR=src"
set "LIB_DIR=lib"
set "BIN_DIR=build"

echo ===================================================
echo     Compilateur et Lanceur Java Automatique
echo ===================================================

REM 1. Demander la classe principale si non fournie en argument
set "MAIN_CLASS=%~1"
if "%MAIN_CLASS%"=="" (
    echo [?] Quelle est la classe principale a lancer ?
    set /p MAIN_CLASS="(ex: com.test.Main) : "
)

REM 2. Nettoyage et preparation du dossier build
if exist "%BIN_DIR%" rd /s /q "%BIN_DIR%"
mkdir "%BIN_DIR%"

REM 3. Construction du Classpath de compilation (tous les jars dans lib)
set "CP_LIB=."
if exist "%LIB_DIR%" (
    set "CP_LIB=%LIB_DIR%\*"
)

REM 4. Recherche de tous les fichiers .java dans src
echo [1/3] Analyse des fichiers sources...
dir /s /b "%SRC_DIR%\*.java" > sources.txt

REM 5. Compilation
echo [2/3] Compilation en cours vers %BIN_DIR%...
javac -d "%BIN_DIR%" -cp "%CP_LIB%" @sources.txt

if %ERRORLEVEL% neq 0 (
    echo:
    echo [ERREUR] La compilation a echoue. Verifiez votre code.
    del sources.txt
    pause
    exit /b 1
)
del sources.txt

REM 6. Execution
echo [3/3] Lancement de %MAIN_CLASS%...
echo ---------------------------------------------------
echo:

REM Le classpath d'execution inclut build (tes classes), lib (tes jars) et . (pour le .env)
java -cp "%BIN_DIR%;%CP_LIB%;." %MAIN_CLASS%

if %ERRORLEVEL% neq 0 (
    echo:
    echo [INFO] Le programme s'est arrete avec le code %ERRORLEVEL%
)

echo:
echo ---------------------------------------------------
echo Fin d'execution.
pause