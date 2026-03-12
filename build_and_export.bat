@echo off
cd /d "E:\COURSES\01. GitHub Repo Projects\SaveStatus"
call gradlew.bat clean assembleDebug --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED >> build_v102_final.txt
    exit /b 1
)
echo BUILD SUCCESSFUL >> build_v102_final.txt
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "APK_Export\SaveStatus_PRO_v1.0.2.apk"
echo EXPORT DONE >> build_v102_final.txt

