@echo off
chcp 65001 > nul
echo ===================================================
echo   LAB 8: MAIN LAUNCHER - TRÌNH ĐIỀU KHIỂN CHÍNH
echo ===================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar
)
pause
