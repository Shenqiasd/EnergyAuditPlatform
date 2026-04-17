#!/bin/bash
set -e

###############################################
#  EnergyAudit SQL 迁移执行器
#  用法: bash run-migrations.sh [migration_number]
#        bash run-migrations.sh --seed
#
#  功能:
#  - 无参数: 检查并执行所有待运行的迁移
#  - 指定编号: 只执行指定编号的迁移 (如: bash run-migrations.sh 22)
#  - --seed: 将所有现有迁移标记为已执行(首次部署后使用)
#
#  迁移记录保存在 MySQL 的 _migration_history 表中
###############################################

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SQL_DIR="$PROJECT_DIR/sql"

# MySQL connection via Docker
MYSQL_CMD="docker exec -i energy-audit-mysql mysql -pEnergyAudit2026!Secure#DB energy_audit"

echo "========================================="
echo "  SQL 迁移检查"
echo "========================================="

# Create migration history table if not exists
echo "CREATE TABLE IF NOT EXISTS _migration_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    migration_file VARCHAR(255) NOT NULL UNIQUE,
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SUCCESS'
);" | $MYSQL_CMD 2>/dev/null

# Handle --seed mode: mark all existing migrations as already executed
if [ "$1" = "--seed" ]; then
    echo ""
    echo "种子模式: 将所有现有迁移标记为已执行..."
    SEED_FILES=$(ls "$SQL_DIR"/*.sql 2>/dev/null | xargs -I{} basename {} | grep -E '^[0-9]+-' | sort -t'-' -k1 -n)
    SEED_COUNT=0
    for file in $SEED_FILES; do
        echo "INSERT IGNORE INTO _migration_history (migration_file, status) VALUES ('$file', 'SUCCESS');" | $MYSQL_CMD 2>/dev/null
        SEED_COUNT=$((SEED_COUNT + 1))
    done
    echo "已标记 $SEED_COUNT 个迁移为已执行"
    exit 0
fi

# Check if this is the first time running (no history records exist)
HISTORY_COUNT=$(echo "SELECT COUNT(*) FROM _migration_history;" | $MYSQL_CMD 2>/dev/null | tail -1)

if [ "$HISTORY_COUNT" = "0" ] && [ -z "$1" ]; then
    echo ""
    echo "检测到首次运行迁移工具 (历史记录为空)"
    echo "当前数据库可能已经包含了历史迁移的变更。"
    echo ""
    echo "请选择:"
    echo "  1) 标记所有现有迁移为已执行 (推荐 - 已有数据库)"
    echo "  2) 执行所有迁移 (全新空数据库)"
    echo "  3) 退出"
    echo ""
    read -p "请选择 [1/2/3]: " CHOICE
    case $CHOICE in
        1)
            echo ""
            echo "标记所有现有迁移为已执行..."
            SEED_FILES=$(ls "$SQL_DIR"/*.sql 2>/dev/null | xargs -I{} basename {} | grep -E '^[0-9]+-' | sort -t'-' -k1 -n)
            for file in $SEED_FILES; do
                echo "INSERT IGNORE INTO _migration_history (migration_file, status) VALUES ('$file', 'SUCCESS');" | $MYSQL_CMD 2>/dev/null
            done
            echo "完成! 后续新增的迁移文件将自动检测并执行。"
            exit 0
            ;;
        2)
            echo "将执行所有迁移..."
            ;;
        3)
            echo "已退出。如需标记历史迁移，执行: bash run-migrations.sh --seed"
            exit 0
            ;;
        *)
            echo "无效选择，已退出"
            exit 1
            ;;
    esac
fi

# Get list of migration files (numbered ones only, sorted)
MIGRATION_FILES=$(ls "$SQL_DIR"/*.sql 2>/dev/null | xargs -I{} basename {} | grep -E '^[0-9]+-' | sort -t'-' -k1 -n)

if [ -z "$MIGRATION_FILES" ]; then
    echo "没有找到迁移文件"
    exit 0
fi

# Filter by specific migration number if provided
if [ -n "$1" ]; then
    MIGRATION_FILES=$(echo "$MIGRATION_FILES" | grep "^$1-" || true)
    if [ -z "$MIGRATION_FILES" ]; then
        echo "未找到编号 $1 的迁移文件"
        exit 1
    fi
fi

# Check which migrations have been executed
EXECUTED=$(echo "SELECT migration_file FROM _migration_history WHERE status='SUCCESS';" | $MYSQL_CMD 2>/dev/null | tail -n +2)

PENDING_COUNT=0
PENDING_LIST=""

for file in $MIGRATION_FILES; do
    if echo "$EXECUTED" | grep -q "^${file}$"; then
        continue
    fi
    PENDING_COUNT=$((PENDING_COUNT + 1))
    PENDING_LIST="$PENDING_LIST $file"
done

if [ $PENDING_COUNT -eq 0 ]; then
    echo "所有迁移已执行完毕，无待运行的迁移"
    echo ""
    TOTAL=$(echo "SELECT COUNT(*) FROM _migration_history WHERE status='SUCCESS';" | $MYSQL_CMD 2>/dev/null | tail -1)
    echo "已执行迁移总数: $TOTAL"
    exit 0
fi

echo ""
echo "待执行的迁移 ($PENDING_COUNT 个):"
for file in $PENDING_LIST; do
    echo "  - $file"
done
echo ""

# Ask for confirmation unless running specific migration
if [ -z "$1" ]; then
    read -p "是否执行以上所有迁移? (y/N): " CONFIRM
    if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
        echo "已取消"
        exit 0
    fi
fi

# Execute pending migrations
SUCCESS_COUNT=0
FAIL_COUNT=0

for file in $PENDING_LIST; do
    echo ""
    echo "--- 执行: $file ---"
    
    if $MYSQL_CMD < "$SQL_DIR/$file" 2>&1; then
        echo "INSERT INTO _migration_history (migration_file, status) VALUES ('$file', 'SUCCESS')
            ON DUPLICATE KEY UPDATE status='SUCCESS', executed_at=NOW();" | $MYSQL_CMD 2>/dev/null
        echo "  [成功] $file"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "INSERT INTO _migration_history (migration_file, status) VALUES ('$file', 'FAILED')
            ON DUPLICATE KEY UPDATE status='FAILED', executed_at=NOW();" | $MYSQL_CMD 2>/dev/null
        echo "  [失败] $file"
        FAIL_COUNT=$((FAIL_COUNT + 1))
        echo "  警告: 迁移失败，继续执行下一个..."
    fi
done

echo ""
echo "========================================="
echo "  迁移执行完成"
echo "  成功: $SUCCESS_COUNT  失败: $FAIL_COUNT"
echo "========================================="

if [ $FAIL_COUNT -gt 0 ]; then
    echo ""
    echo "失败的迁移可能需要手动处理:"
    echo "SELECT migration_file, status, executed_at FROM _migration_history WHERE status='FAILED';" | $MYSQL_CMD 2>/dev/null
    exit 1
fi
