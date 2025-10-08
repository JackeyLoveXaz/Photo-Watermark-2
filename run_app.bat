@echo off
REM 设置编码为UTF-8，解决中文显示乱码问题
chcp 65001 > nul

REM 图片水印工具启动脚本

REM 构建项目
echo 正在构建项目，请稍候...
mvn clean package

REM 检查构建是否成功
if %errorlevel% neq 0 (
    echo 构建失败！请确保已正确安装JDK 17和Maven并配置环境变量
    pause
    exit /b 1
)

REM 运行应用程序，添加JavaFX模块参数
echo 构建成功！正在启动应用程序...
java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.swing,javafx.web -jar target/Photo-Watermark-2-1.0-SNAPSHOT-jar-with-dependencies.jar

REM 如果上面的命令失败，尝试使用Maven直接运行（这会自动处理JavaFX依赖）
if %errorlevel% neq 0 (
    echo JavaFX启动失败，尝试使用Maven直接运行...
    mvn javafx:run
)

pause