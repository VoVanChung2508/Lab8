@echo off
chcp 65001 > nul
echo ==================================================================
echo   KIỂM THỬ ĐỒNG BỘ HÓA RMI GIỮA 2 SERVER (SERVER 1 VÀ SERVER 2)
echo ==================================================================
echo.

if not exist "target\classes\com\lab8\TestTwoServerSync.class" (
    echo Đang biên dịch mã nguồn kiểm thử...
    call mvn compile
)

java -cp "target/classes;target/lab8-1.0-SNAPSHOT.jar" com.lab8.TestTwoServerSync
echo.
pause
