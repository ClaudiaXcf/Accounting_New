# 自动记账 (Accounting New) 开发指南

## 项目概述

这是一个 Android 自动记账应用，通过无障碍服务和通知监听自动捕获支付信息。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **数据库**: Room
- **架构**: MVVM + Clean Architecture

## 开发流程

### 1. 代码修改
每次修改代码后，需要：
1. 运行 `./gradlew.bat assembleDebug` 验证构建
2. 运行 `./gradlew.bat installDebug` 安装到设备测试

### 2. 提交规范
使用中文 commit message，格式：
```
<类型>: <简短描述>

- <详细变更>
- <测试结果>
```

类型：
- `ui`: UI 相关修改
- `fix`: Bug 修复
- `feat`: 新功能
- `refactor`: 重构
- `data`: 数据/数据库相关

### 3. 快速提交
```bash
# 在项目根目录运行
.\commit.bat
```

### 4. 安装测试
```bash
.\gradlew.bat installDebug
```

## 已识别的问题 (待修复)

| 优先级 | 问题 | 位置 |
|--------|------|------|
| 🔴 高 | 数据库无迁移策略 | AccountingDatabase.kt |
| 🔴 高 | AccessibilityService 可能内存泄漏 | PaymentAccessibilityService.kt |
| 🟡 中 | 报表柱状图颜色硬编码 | MainActivity.kt |
| 🟡 中 | 分类颜色未在 UI 使用 | - |