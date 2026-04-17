#!/bin/bash
set -e

###############################################
#  EnergyAudit 腾讯云同步脚本
#  用法: bash sync.sh [--skip-migrations]
#
#  功能:
#  1. 从 GitHub master 拉取最新代码
#  2. 合并到 deploy/tencent-cloud 分支
#  3. 重新构建并重启 Docker 容器
#  4. 执行待运行的 SQL 迁移
###############################################

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SKIP_MIGRATIONS=false

for arg in "$@"; do
    case $arg in
        --skip-migrations) SKIP_MIGRATIONS=true ;;
    esac
done

echo "========================================="
echo "  EnergyAudit 腾讯云同步更新"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="

# Step 0: Ensure Git identity is configured (needed for merge commits)
cd "$PROJECT_DIR"
if [ -z "$(git config user.email)" ]; then
    echo "[准备] 配置 Git 身份..."
    git config user.email "deploy@tencent-cloud"
    git config user.name "Tencent Cloud Deploy"
fi

# Step 1: Pull latest code
echo ""
echo "[1/4] 拉取最新代码..."

# Stash any local changes to avoid merge conflicts
if [ -n "$(git status --porcelain)" ]; then
    echo "检测到本地未提交的改动，自动暂存..."
    git stash --include-untracked -m "sync-auto-stash-$(date +%Y%m%d%H%M%S)"
    STASHED=true
else
    STASHED=false
fi

BEFORE_COMMIT=$(git rev-parse HEAD)

git fetch origin master
git fetch origin deploy/tencent-cloud
git checkout deploy/tencent-cloud 2>/dev/null || git checkout -b deploy/tencent-cloud origin/deploy/tencent-cloud

# Fast-forward deploy branch to remote if behind
git merge origin/deploy/tencent-cloud --no-edit 2>/dev/null || true

git merge origin/master --no-edit

AFTER_COMMIT=$(git rev-parse HEAD)

if [ "$BEFORE_COMMIT" = "$AFTER_COMMIT" ]; then
    echo "代码已是最新，无需更新"
    echo ""
    read -p "是否仍要重新构建容器? (y/N): " REBUILD
    if [ "$REBUILD" != "y" ] && [ "$REBUILD" != "Y" ]; then
        echo "跳过构建。如需运行迁移，执行: bash run-migrations.sh"
        exit 0
    fi
else
    echo "代码已更新: $BEFORE_COMMIT -> $AFTER_COMMIT"
    echo ""
    echo "新增的提交:"
    git log --oneline "$BEFORE_COMMIT".."$AFTER_COMMIT" | head -20
fi

# Restore stashed changes if any
if [ "$STASHED" = true ]; then
    echo "恢复之前暂存的本地改动..."
    git stash pop || echo "警告: 恢复暂存失败，请手动执行 git stash pop"
fi

# Step 2: Rebuild and restart containers
echo ""
echo "[2/4] 重新构建并重启服务 (约 5-10 分钟)..."
cd "$SCRIPT_DIR"

# Rebuild backend and frontend only (MySQL keeps running)
docker compose build backend frontend
docker compose up -d backend frontend

# Wait for backend to be ready
echo "等待后端启动..."
for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -q "200"; then
        echo "后端已就绪"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "警告: 后端启动超时，请检查日志: docker compose logs backend"
    fi
    sleep 3
done

# Step 3: Run SQL migrations
echo ""
echo "[3/4] 检查 SQL 迁移..."
if [ "$SKIP_MIGRATIONS" = true ]; then
    echo "跳过迁移 (--skip-migrations)"
else
    bash "$SCRIPT_DIR/run-migrations.sh"
fi

# Step 4: Verify
echo ""
echo "[4/4] 验证部署..."
echo ""
echo "容器状态:"
docker compose ps
echo ""

# Quick health check
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://localhost/api/auth/info -k 2>/dev/null || echo "000")
echo "API 健康检查: HTTP $HTTP_CODE"

echo ""
echo "========================================="
echo "  同步完成! $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="
echo ""
echo "  如果有新的 SQL 迁移需要手动执行:"
echo "  cd $SCRIPT_DIR && bash run-migrations.sh"
echo ""
