@echo off
REM 图片水印工具启动脚本

REM 检查Java是否安装
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到Java运行环境。请先安装JDK 17或更高版本。
    pause
    exit /b 1
)

REM 检查Maven是否安装
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo 警告: 未找到Maven。如果这是第一次运行，请先安装Maven并配置环境变量。
    pause
)

REM 构建项目
if exist "pom.xml" (
    echo 正在构建项目...
    mvn clean package
    if %errorlevel% neq 0 (
        echo 构建失败！
        pause
        exit /b 1
    )
)

REM 运行应用程序
java -jar target/Photo-Watermark-2-1.0-SNAPSHOT-jar-with-dependencies.jar

REM 如果运行失败，尝试在当前目录查找jar文件
if %errorlevel% neq 0 (
    echo 无法找到构建的jar文件。正在尝试在当前目录查找...
    for %%f in (*-jar-with-dependencies.jar) do (
        java -jar "%%f"
        exit /b 0
    )
    
    echo 错误: 未找到可运行的jar文件。请确保项目已成功构建。
    pause
    exit /b 1
)

pause