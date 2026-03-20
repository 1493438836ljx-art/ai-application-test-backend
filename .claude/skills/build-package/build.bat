@echo off
REM Build Package Skill Script for Windows
REM 用于构建和打包 Maven 项目

setlocal enabledelayedexpansion

REM 获取参数
set SKIP_TESTS=%1
if "%SKIP_TESTS%"=="" set SKIP_TESTS=true
set RUN_TESTS=%2
if "%RUN_TESTS%"=="" set RUN_TESTS=false

echo [INFO] 开始构建项目...
echo [INFO] 跳过测试: %SKIP_TESTS%

REM 检查 Maven 是否安装
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Maven 未安装或未在 PATH 中
    exit /b 1
)

REM 创建 releases 目录
if not exist releases mkdir releases

REM 清理项目
echo [INFO] 执行 mvn clean...
call mvn clean -q

REM 编译项目
echo [INFO] 执行 mvn compile...
call mvn compile -q

REM 运行测试（如果指定）
if "%RUN_TESTS%"=="true" (
    echo [INFO] 执行 mvn test...
    call mvn test
)

REM 打包项目
echo [INFO] 执行 mvn package...
if "%SKIP_TESTS%"=="true" (
    call mvn package -DskipTests -q
) else (
    call mvn package -q
)

REM 获取版本号
for /f "tokens=*" %%i in ('call mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2^>nul') do set VERSION=%%i
set JAR_NAME=demo-%VERSION%.jar
set ORIGINAL_JAR_NAME=demo-%VERSION%-original.jar

echo [INFO] 构建完成！
echo [INFO] 版本: %VERSION%

REM 复制 JAR 到 releases 目录
if exist "target\%JAR_NAME%" (
    copy /Y "target\%JAR_NAME%" "releases\" >nul
    echo [INFO] JAR 文件已复制到 releases\%JAR_NAME%
) else (
    echo [ERROR] 未找到 JAR 文件: target\%JAR_NAME%
    exit /b 1
)

if exist "target\%ORIGINAL_JAR_NAME%" (
    copy /Y "target\%ORIGINAL_JAR_NAME%" "releases\" >nul
    echo [INFO] 原始 JAR 文件已复制到 releases\%ORIGINAL_JAR_NAME%
)

echo [INFO] ========================================
echo [INFO] 构建成功！
echo [INFO] ========================================
echo.
echo [INFO] JAR 文件位置:
echo   - releases\%JAR_NAME% (可执行)
if exist "releases\%ORIGINAL_JAR_NAME%" (
    echo   - releases\%ORIGINAL_JAR_NAME%
)

endlocal
