@echo off
chcp 65001 > nul
echo ===================================================
echo   BÀI 1: KHỞI CHẠY RMI CHAT CLIENT (MÁY ẢO / MÁY THỰC)
echo ===================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar chat-client %*
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar chat-client %*
)
pause
