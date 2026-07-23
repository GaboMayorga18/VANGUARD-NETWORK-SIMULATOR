@echo off
chcp 65001 >nul 2>&1
title EVEA Server Dashboard

echo.
echo =======================================================
echo        EVEA Server Dashboard - Iniciando...           
echo =======================================================
echo.

setlocal enabledelayedexpansion

set "JAVA_CMD="
set "JAVAC_CMD="

REM 1. Verificar si javac y java estan disponibles en el PATH actual
where javac >nul 2>&1
if %errorlevel%==0 (
    where java >nul 2>&1
    if %errorlevel%==0 (
        set "JAVAC_CMD=javac"
        set "JAVA_CMD=java"
        goto :JAVA_DETECTED
    )
)

REM 2. Verificar variables JAVA_HOME o JDK_HOME si estan definidas
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC_CMD="%JAVA_HOME%\bin\javac.exe""
        set "JAVA_CMD="%JAVA_HOME%\bin\java.exe""
        goto :JAVA_DETECTED
    )
)
if defined JDK_HOME (
    if exist "%JDK_HOME%\bin\javac.exe" (
        set "JAVAC_CMD="%JDK_HOME%\bin\javac.exe""
        set "JAVA_CMD="%JDK_HOME%\bin\java.exe""
        goto :JAVA_DETECTED
    )
)

REM 3. Buscar en rutas habituales de instalacion de JDK / NetBeans / JRE en Windows
set "SEARCH_PATHS="C:\Program Files\Java" "C:\Program Files (x86)\Java" "C:\Program Files\Eclipse Adoptium" "C:\Program Files\AdoptOpenJDK" "C:\Program Files\Amazon Corretto" "C:\Program Files\Zulu" "C:\Program Files\LibericaJDK" "C:\Program Files\Microsoft" "C:\Program Files\NetBeans" "C:\Program Files\Apache NetBeans" "%LOCALAPPDATA%\Programs" "%APPDATA%\NetBeans""

REM Buscar primero un JDK completo (con javac.exe)
for %%P in (%SEARCH_PATHS%) do (
    if exist %%P (
        for /f "delims=" %%I in ('dir /b /s "%%~P\javac.exe" 2^>nul') do (
            if exist "%%~I" (
                set "JAVAC_CMD="%%~I""
                set "JAVA_BIN=%%~dpI"
                if exist "!JAVA_BIN!java.exe" (
                    set "JAVA_CMD="!JAVA_BIN!java.exe""
                    goto :JAVA_DETECTED
                )
            )
        )
    )
)

REM Si no se encontro javac, buscar java.exe (JRE o ejecucion directa Java 11+)
for %%P in (%SEARCH_PATHS%) do (
    if exist %%P (
        for /f "delims=" %%I in ('dir /b /s "%%~P\java.exe" 2^>nul') do (
            if exist "%%~I" (
                set "JAVA_CMD="%%~I""
                goto :JAVA_DETECTED
            )
        )
    )
)

REM Si tras la busqueda no se encuentra Java
if not defined JAVA_CMD (
    echo [ERROR] No se pudo encontrar una instalacion de Java/JDK en el sistema.
    echo.
    echo Buscamos en:
    echo  - PATH y variables de entorno JAVA_HOME / JDK_HOME
    echo  - C:\Program Files\Java\
    echo  - C:\Program Files (x86)\Java\
    echo  - Rutas de NetBeans, Adoptium, Corretto, Microsoft, Zulu, etc.
    echo.
    echo Por favor instala JDK 8 o superior desde: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:JAVA_DETECTED
echo [INFO] Java detectado: !JAVA_CMD!
if defined JAVAC_CMD (
    echo [INFO] Compilador Java detectado: !JAVAC_CMD!
)
echo.

if defined JAVAC_CMD (
    echo [1/2] Compilando ServidorWeb.java...
    !JAVAC_CMD! -encoding UTF-8 ServidorWeb.java
    if errorlevel 1 (
        echo [ERROR] Error de compilacion. Revise el codigo.
        pause
        exit /b 1
    )
    echo       Compilacion exitosa.
    echo.
    
    echo [2/2] Iniciando servidor HTTP en puerto 8080...
    echo.
    start /b cmd /c "timeout /t 2 /nobreak >nul && start http://localhost:8080"
    echo Abriendo navegador en http://localhost:8080 ...
    echo Presione Ctrl+C para detener el servidor.
    echo.
    !JAVA_CMD! ServidorWeb
) else (
    echo [1/1] Ejecutando ServidorWeb.java directamente...
    echo.
    start /b cmd /c "timeout /t 2 /nobreak >nul && start http://localhost:8080"
    echo Abriendo navegador en http://localhost:8080 ...
    echo Presione Ctrl+C para detener el servidor.
    echo.
    !JAVA_CMD! ServidorWeb.java
)

pause
