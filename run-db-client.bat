@echo off
chcp 65001 > nul
echo ===================================================
echo   BÀI 2: KHỞI CHẠY DB CLIENT (MÁY ẢO)
echo ===================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar db-client %*
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar db-client %*
)
pause
