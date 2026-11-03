@echo off
chcp 65001 > nul
echo ==================================================================
echo   BÀI 2: BẢNG ĐIỀU KHIỂN GIÁM SÁT ĐỒNG BỘ 3 CSDL (DASHBOARD)
echo ==================================================================

if exist "target\lab8-1.0-SNAPSHOT.jar" (
    java -jar target\lab8-1.0-SNAPSHOT.jar db-dashboard %*
) else (
    echo Đang đóng gói chương trình...
    mvn package -DskipTests
    java -jar target\lab8-1.0-SNAPSHOT.jar db-dashboard %*
)
pause
