#!/bin/bash
set -e

echo "========================================="
echo "  EnergyAudit 腾讯云部署脚本"
echo "========================================="

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$SCRIPT_DIR"

# Step 1: Check Docker
echo ""
echo "[1/6] 检查 Docker..."
if ! command -v docker &> /dev/null; then
    echo "Docker 未安装，正在安装..."
    curl -fsSL https://get.docker.com | sh
    systemctl start docker
    systemctl enable docker
    echo "Docker 安装完成"
fi
docker --version

# Step 2: Check Docker Compose
echo ""
echo "[2/6] 检查 Docker Compose..."
if ! docker compose version &> /dev/null; then
    echo "安装 Docker Compose plugin..."
    apt-get update && apt-get install -y docker-compose-plugin
fi
docker compose version

# Step 3: Check SSL certificate
echo ""
echo "[3/6] 检查 SSL 证书..."
mkdir -p ssl
if [ -f ssl/fullchain.pem ] && [ -f ssl/privkey.pem ]; then
    echo "SSL 证书已就绪 (energyaudit.ben-china.org.cn)"
    openssl x509 -in ssl/fullchain.pem -noout -subject -enddate 2>/dev/null || true
else
    echo "警告: SSL 证书不存在，生成临时自签名证书..."
    openssl req -x509 -nodes -days 365 \
        -newkey rsa:2048 \
        -keyout ssl/privkey.pem \
        -out ssl/fullchain.pem \
        -subj "/CN=energyaudit.ben-china.org.cn" \
        2>/dev/null
    echo "临时自签名证书已生成（浏览器会提示不安全）"
fi

# Step 4: Import MySQL data
echo ""
echo "[4/6] 启动 MySQL 并导入数据..."
docker compose up -d mysql
echo "等待 MySQL 启动..."
sleep 15

# Check if MySQL is ready
for i in $(seq 1 30); do
    if docker exec energy-audit-mysql mysqladmin ping -h localhost -pEnergyAudit2026!Secure#DB 2>/dev/null | grep -q "alive"; then
        echo "MySQL 已就绪"
        break
    fi
    echo "等待 MySQL... ($i/30)"
    sleep 3
done

# Check if data already imported
TABLE_COUNT=$(docker exec energy-audit-mysql mysql -pEnergyAudit2026!Secure#DB energy_audit -e "SHOW TABLES;" 2>/dev/null | wc -l)
if [ "$TABLE_COUNT" -lt 10 ]; then
    echo "正在导入数据库 (约 30MB)..."
    if [ -f railway-dump.sql.gz ]; then
        gunzip -c railway-dump.sql.gz | docker exec -i energy-audit-mysql mysql -pEnergyAudit2026!Secure#DB energy_audit
        echo "数据库导入完成"
    else
        echo "错误: railway-dump.sql.gz 不存在!"
        exit 1
    fi
else
    echo "数据库已有 $TABLE_COUNT 个表，跳过导入"
fi

# Step 5: Build and start all services
echo ""
echo "[5/6] 构建并启动所有服务 (首次构建约 10-15 分钟)..."
docker compose up -d --build

# Step 6: Verify
echo ""
echo "[6/6] 验证部署..."
sleep 10

echo ""
echo "容器状态:"
docker compose ps

echo ""
echo "========================================="
echo "  部署完成!"
echo "========================================="
echo ""
echo "  域名: https://energyaudit.ben-china.org.cn"
echo "  IP:   https://43.247.89.187"
echo ""
echo "  请确保 DNS 已配置:"
echo "  energyaudit.ben-china.org.cn  A  43.247.89.187"
echo "========================================="
