@echo off
REM 图片水印工具启动脚本

REM 构建项目
mvn clean package

REM 检查构建是否成功
if %errorlevel% neq 0 (
    echo 构建失败！
    pause
    exit /b 1
)

REM 运行应用程序
java -jar target/Photo-Watermark-2-1.0-SNAPSHOT-jar-with-dependencies.jar

pause