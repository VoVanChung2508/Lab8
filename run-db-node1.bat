@echo off
chcp 65001 > nul
echo ===================================================
echo   BÀI 2: KHỞI CHẠY DB SERVER NODE 1 (CỔNG 1099)
echo ===================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar db-server Server-1 1099 server1_db.sqlite
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar db-server Server-1 1099 server1_db.sqlite
)
pause
