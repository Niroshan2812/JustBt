@echo off
echo [BUILD] Compiling C++ USB Driver...

:: Configuration
set COMPILER=g++
set OUTPUT=usb_driver.dll
set SOURCE=src/CDor/usb_driver.cpp
set INCLUDES=-I"libs"
set LIBS="libs/libusb-1.0.a"

:: Compile Command
%COMPILER% -shared -o %OUTPUT% %SOURCE% %INCLUDES% %LIBS%

if %errorlevel% neq 0 (
    echo [ERROR] Compilation Failed!
    exit /b %errorlevel%
)

echo [SUCCESS] built %OUTPUT%
