# Git Commit & Push 技能

## 功能
自动提交本地代码变更并推送到远程 Git 仓库。该技能会：
- 添加所有修改的文件到暂存区
- 根据修改类型自动生成提交信息
- 执行提交
- 推送到远程仓库
- 适用于遵循项目 CLAUDE.md 规范的代码提交

## 使用方法
```bash
/commit-push
```

## 执行流程
1. 检查是否在 Git 仓库中
2. 读取项目名称（从 CLAUDE.md）
3. 获取当前分支信息
4. 添加所有文件到暂存区 (`git add -A`)
5. 检查是否有更改需要提交
6. 分析修改内容，自动判断提交类型：
   - `build`: Maven/POM.xml 相关修改
   - `chore`: 配置文件修改
   - `feat`: 新功能（Java代码）
   - `docs`: 文档更新
   - `fix`: 修复错误
7. 自动生成提交信息
8. 执行提交
9. 推送到远程仓库

## 输出示例
```
🔧 Git Commit & Push Skill
===============================
Current branch: main

📦 Adding files to staging...

📋 Changes to commit:
 M src/main/java/com/example/demo/controller/TestController.java
 A README.md

💬 Default commit message: "feat: add new features"

✨ Committing changes...
✅ Committed successfully! Hash: abc123

🚀 Pushing to remote...
🎉 Successfully pushed to origin/main

Current status: ## main...origin/main
```

## 注意事项
- 需要已配置远程仓库
- 确保提交前已解决所有冲突
- 推送失败时会显示具体错误信息