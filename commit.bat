@echo off
chcp 65001 >nul
echo ========================================
echo     自动记账 - 代码提交工具
echo ========================================
echo.

git status --short

echo.
echo 请输入本次更新的简介:
set /p MESSAGE=

if "%MESSAGE%"=="" (
    echo 错误: 请输入更新简介
    pause
    exit /b 1
)

echo.
echo 正在提交更改...

git add -A
git commit -m "%MESSAGE%"

echo.
echo ========================================
echo     提交完成!
echo ========================================
pause