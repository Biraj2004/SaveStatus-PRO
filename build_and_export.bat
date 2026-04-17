@echo off
cd /d "E:\COURSES\01. GitHub Repo Projects\SaveStatus"
call gradlew.bat clean assembleRelease --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED >> build_v100_final.txt
    exit /b 1
)
echo BUILD SUCCESSFUL >> build_v100_final.txt
copy /Y "app\build\outputs\apk\release\app-release.apk" "APK_Export\SaveStatus_PRO.apk"
echo EXPORT DONE >> build_v100_final.txt

