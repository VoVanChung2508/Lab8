#!/bin/bash
# Script khởi chạy trên máy ảo Linux (Ubuntu, Debian, CentOS, v.v.)
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

if [ -f "target/lab8-1.0-SNAPSHOT.jar" ]; then
    java -jar target/lab8-1.0-SNAPSHOT.jar "$@"
else
    echo "Chưa tìm thấy file jar, đang biên dịch..."
    mvn package -DskipTests
    java -jar target/lab8-1.0-SNAPSHOT.jar "$@"
fi
