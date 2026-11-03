@echo off
chcp 65001 > nul
echo ===================================================
echo   BÀI 1: KHỞI CHẠY RMI CHAT SERVER (MÁY THỰC)
echo ===================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar chat-server %*
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar chat-server %*
)
pause
