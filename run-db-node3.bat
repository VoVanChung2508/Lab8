@echo off
chcp 65001 > nul
echo ===================================================
echo   BÀI 2: KHỞI CHẠY DB SERVER NODE 3 (CỔNG 1101)
echo ===================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar db-server Server-3 1101 server3_db.sqlite
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar db-server Server-3 1101 server3_db.sqlite
)
pause
