#!/usr/bin/env node

/**
 * Git 提交和推送技能
 * 用于提交代码并推送到远程仓库
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// 当前工作目录
const cwd = process.cwd();

// 读取项目配置
const claudeMdPath = path.join(cwd, 'CLAUDE.md');
const gitConfigPath = path.join(cwd, '.git');

function readClaudeMd() {
  try {
    return fs.readFileSync(claudeMdPath, 'utf8');
  } catch (err) {
    return '';
  }
}

function formatMessage(message) {
  // 移除多余的空白行
  return message.trim().replace(/\n\s*\n/g, '\n');
}

function runGitCommand(command, options = {}) {
  try {
    const fullCommand = `git ${command}`;
    const output = execSync(fullCommand, {
      cwd,
      encoding: 'utf8',
      ...options
    });
    return output.trim();
  } catch (error) {
    throw new Error(`Git command failed: ${command}\n${error.message}`);
  }
}

// 主函数
function main() {
  console.log('🔧 Git Commit & Push Skill');
  console.log('===============================');

  try {
    // 检查是否在 git 仓库中
    if (!fs.existsSync(gitConfigPath)) {
      throw new Error('Not a git repository. Please initialize git first.');
    }

    // 读取 CLAUDE.md 获取项目信息
    const claudeMd = readClaudeMd();
    const projectName = claudeMd.match(/项目简介[^\n]+/)?.[0]?.replace(/项目简介\s*/, '').trim() || 'AI Test Platform';

    // 获取当前分支
    const currentBranch = runGitCommand('branch --show-current');
    console.log(`Current branch: ${currentBranch}`);

    // 检查远程分支是否存在
    try {
      runGitCommand(`rev-parse --verify origin/${currentBranch}`, { stdio: 'pipe' });
    } catch {
      console.log(`⚠️ Remote branch origin/${currentBranch} does not exist`);
    }

    // 获取最近的提交信息用于参考
    try {
      const lastCommit = runGitCommand('log -1 --pretty=format:%s', { stdio: 'pipe' });
      console.log(`Last commit: ${lastCommit}`);
    } catch {
      // 忽略
    }

    // 1. 添加所有文件到暂存区
    console.log('\n📦 Adding files to staging...');
    runGitCommand('add -A');

    // 2. 检查是否有更改
    const statusOutput = runGitCommand('status --porcelain', { stdio: 'pipe' });
    if (!statusOutput) {
      console.log('\n📭 Nothing to commit. All files are up to date.');
      return 0;
    }
    console.log('\n📋 Changes to commit:');
    console.log(statusOutput);

    // 3. 读取 git diff 以确定更改类型
    const diff = runGitCommand('diff --cached', { stdio: 'pipe' });
    let commitType = 'docs';
    let commitMessage = '';

    if (diff) {
      const hasChanges = diff.toLowerCase();

      if (hasChanges.includes('pom.xml') || hasChanges.includes('.maven')) {
        commitType = 'build';
      } else if (hasChanges.includes('application.yml') || hasChanges.includes('application.properties')) {
        commitType = 'chore';
      } else if (hasChanges.includes('.java')) {
        commitType = 'feat';
        commitMessage = 'feat: add new features';
      } else if (hasChanges.includes('.md') || hasChanges.includes('.txt') || hasChanges.includes('README')) {
        commitType = 'docs';
        commitMessage = 'docs: update documentation';
      } else if (hasChanges.includes('.yml') || hasChanges.includes('.yaml') || hasChanges.includes('.json')) {
        commitType = 'fix';
        commitMessage = 'fix: configuration fixes';
      }
    }

    // 4. 构建提交信息
    const defaultCommitMessage = `${commitType}: ${commitMessage || 'code changes'}`;
    console.log(`\n💬 Default commit message: "${defaultCommitMessage}"`);

    // 5. 提交更改
    console.log('\n✨ Committing changes...');
    const fullCommitMessage = `🚀 ${defaultCommitMessage}`;
    runGitCommand(`commit -m "${fullCommitMessage}"`);

    // 6. 获取最新的提交信息
    const commitHash = runGitCommand('rev-parse --short HEAD');
    console.log(`\n✅ Committed successfully! Hash: ${commitHash}`);

    // 7. 推送到远程仓库
    console.log('\n🚀 Pushing to remote...');

    // 检查远程仓库配置
    const remoteUrl = runGitCommand('remote get-url origin', { stdio: 'pipe' });
    console.log(`Remote URL: ${remoteUrl ? remoteUrl.substring(0, 40) + '...' : 'Not configured'}`);

    // 推送代码
    runGitCommand(`push origin ${currentBranch}`);
    console.log(`\n🎉 Successfully pushed to origin/${currentBranch}`);

    // 显示推送后的状态
    const pushStatus = runGitCommand('status -sb', { stdio: 'pipe' });
    console.log(`\nCurrent status: ${pushStatus}`);

    return 0;

  } catch (error) {
    console.error('\n❌ Error:');
    console.error(error.message);
    return 1;
  }
}

// 运行
process.exitCode = main();