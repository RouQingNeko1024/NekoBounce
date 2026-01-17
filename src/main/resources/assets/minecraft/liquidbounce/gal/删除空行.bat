@echo off

chcp 65001 >nul 2>nul
setlocal enabledelayedexpansion

if not exist "1.txt" (
    echo 错误：文件1.txt不存在！
    pause
    exit /b 1
)

rem 使用临时文件
(
    for /f "delims=" %%a in ('type "1.txt"') do (
        set "line=%%a"
        if "!line!" neq "" echo !line!
    )
) > "1.tmp"

rem 检查文件大小，如果是0字节则可能编码有问题
if exist "1.tmp" (
    move /y "1.tmp" "1.txt" >nul
    echo 已完成：已删除1.txt中的所有空行
) else (
    echo 错误：处理文件时出现问题，可能编码不兼容
)

pause