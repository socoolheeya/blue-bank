#!/bin/bash

echo "========================================"
echo "Blue Bank Scale 모드 시작"
echo "========================================"

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 기본 인스턴스 수
SCALE_COUNT=${1:-3}

echo -e "${YELLOW}각 서비스를 ${SCALE_COUNT}개 인스턴스로 실행합니다...${NC}"

# 기존 컨테이너 정리
echo -e "${YELLOW}기존 컨테이너 정리 중...${NC}"
docker-compose -f docker-compose-scale.yml down

# Eureka와 API Gateway 시작 (단일 인스턴스)
echo -e "${GREEN}Eureka Server와 API Gateway 시작...${NC}"
docker-compose -f docker-compose-scale.yml up -d --build eureka-server api-gateway

# 잠시 대기
echo -e "${YELLOW}Eureka Server 시작 대기 중 (15초)...${NC}"
sleep 15

# 서비스들 시작 및 스케일링
echo -e "${GREEN}서비스들을 ${SCALE_COUNT}개 인스턴스로 시작...${NC}"
docker-compose -f docker-compose-scale.yml up -d --build --scale account-service=${SCALE_COUNT} --scale deposit-service=${SCALE_COUNT} --scale loan-service=${SCALE_COUNT} --scale card-service=${SCALE_COUNT}

# 시작 대기
echo -e "${YELLOW}모든 서비스 시작 대기 중 (30초)...${NC}"
sleep 30

# 실행 중인 컨테이너 확인
echo -e "${GREEN}실행 중인 컨테이너:${NC}"
docker-compose -f docker-compose-scale.yml ps

echo ""
# Eureka 등록 확인
echo -e "${GREEN}Eureka 서비스 등록 상태:${NC}"
curl -s http://localhost:8761/eureka/apps | grep -o "<instanceId>.*</instanceId>" | wc -l | xargs echo "총 등록된 인스턴스 수:"
curl -s http://localhost:8761/eureka/apps | grep -o "<app>.*</app>" | sed 's/<[^>]*>//g' | sort | uniq -c

echo ""
echo "========================================"
echo -e "${GREEN}Scale 구성 완료!${NC}"
echo "========================================"
echo ""
echo "📊 서비스 접근 정보:"
echo "  - Eureka Dashboard: http://localhost:8761"
echo "  - API Gateway: http://localhost:8080"
echo ""
echo -e "${YELLOW}참고: 각 서비스 인스턴스는 랜덤 포트를 사용합니다${NC}"
echo -e "${YELLOW}API Gateway를 통해 로드밸런싱된 요청을 보낼 수 있습니다${NC}"
echo ""
echo "🔄 스케일 조정 명령:"
echo "  docker-compose -f docker-compose-scale.yml up -d --scale account-service=5"
echo ""