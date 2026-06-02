# 自动提交脚本
# 每次代码修改后自动提交

param(
    [string]$Message = "代码更新"
)

$ErrorActionPreference = "Stop"

# 获取项目根目录
$ProjectRoot = Split-Path -Parent $PSScriptRoot

# 切换到项目目录
Set-Location $ProjectRoot

# 检查是否有更改
$status = git status --porcelain
if ($status -eq "") {
    Write-Host "没有更改需要提交"
    exit 0
}

# 获取更改的文件列表
$changedFiles = git diff --name-only --cached
if ($changedFiles -eq "") {
    $changedFiles = git diff --name-only
}

# 添加所有更改
git add -A

# 生成 commit message
$commitMessage = "$Message`n`n修改文件:`n$changedFiles"

# 提交
git commit -m $commitMessage

Write-Host "已自动提交更改: $Message"
Write-Host "修改的文件: $changedFiles"