@echo off
chcp 65001 > nul
echo ===================================================
echo   BÀI 2: KHỞI CHẠY DB SERVER NODE 2 (CỔNG 1100)
echo ===================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar db-server Server-2 1100 server2_db.sqlite
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar db-server Server-2 1100 server2_db.sqlite
)
pause
