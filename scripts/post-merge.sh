#!/bin/bash
set -e

cd audit-ui && npm install --no-fund --no-audit 2>/dev/null
cd ..

rm -rf ~/.m2/repository/com/energy/audit/
mvn clean install -DskipTests -q
