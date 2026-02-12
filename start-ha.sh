#!/bin/bash

echo "========================================"
echo "Blue Bank HA 모드 시작 (삼중화)"
echo "========================================"

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 기존 컨테이너 정리
echo -e "${YELLOW}기존 컨테이너 정리 중...${NC}"
docker-compose -f docker-compose-ha.yml down

# 서비스 빌드 및 시작
echo -e "${GREEN}서비스 빌드 및 시작...${NC}"
docker-compose -f docker-compose-ha.yml up -d --build

# 시작 대기
echo -e "${YELLOW}서비스 시작 대기 중 (30초)...${NC}"
sleep 30

# Eureka 등록 확인
echo -e "${GREEN}Eureka 서비스 등록 상태:${NC}"
curl -s http://localhost:8761/eureka/apps | grep -o "<app>.*</app>" | sed 's/<[^>]*>//g' | sort | uniq -c

echo ""
echo "========================================"
echo -e "${GREEN}HA 구성 완료!${NC}"
echo "========================================"
echo ""
echo "📊 서비스 포트 정보:"
echo "  - Eureka Server: http://localhost:8761"
echo "  - API Gateway: http://localhost:8080"
echo ""
echo "  - Account Service:"
echo "    - Instance 1: http://localhost:8081"
echo "    - Instance 2: http://localhost:8091"
echo "    - Instance 3: http://localhost:8101"
echo ""
echo "  - Deposit Service:"
echo "    - Instance 1: http://localhost:8084"
echo "    - Instance 2: http://localhost:8094"
echo "    - Instance 3: http://localhost:8104"
echo ""
echo "  - Loan Service:"
echo "    - Instance 1: http://localhost:8082"
echo "    - Instance 2: http://localhost:8092"
echo "    - Instance 3: http://localhost:8102"
echo ""
echo "  - Card Service:"
echo "    - Instance 1: http://localhost:8083"
echo "    - Instance 2: http://localhost:8093"
echo "    - Instance 3: http://localhost:8103"
echo ""