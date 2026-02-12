#!/bin/bash

# Blue Bank API 테스트 스크립트

echo "========================================="
echo "Blue Bank API 테스트 시작"
echo "========================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# API 엔드포인트
ACCOUNT_API="http://localhost:8081"
DEPOSIT_API="http://localhost:8084"
CARD_API="http://localhost:8083"
LOAN_API="http://localhost:8082"

# 테스트 데이터
CUSTOMER_ID=1
ACCOUNT_NAME="테스트계좌"

echo ""
echo -e "${YELLOW}1. Account Service 테스트${NC}"
echo "----------------------------------------"

# 계좌 생성
echo -e "${GREEN}[POST]${NC} 계좌 생성 요청..."
ACCOUNT_RESPONSE=$(curl -s -X POST ${ACCOUNT_API}/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": '${CUSTOMER_ID}',
    "accountType": "CHECKING",
    "accountName": "'${ACCOUNT_NAME}'",
    "currency": "KRW",
    "initialDeposit": 1000000
  }')

if [ $? -eq 0 ]; then
    echo "✅ 계좌 생성 성공"
    echo "Response: ${ACCOUNT_RESPONSE}" | jq '.' 2>/dev/null || echo "${ACCOUNT_RESPONSE}"
    ACCOUNT_ID=$(echo "${ACCOUNT_RESPONSE}" | jq -r '.id' 2>/dev/null || echo "1")
else
    echo "❌ 계좌 생성 실패"
    ACCOUNT_ID=1
fi

echo ""
# 계좌 조회
echo -e "${GREEN}[GET]${NC} 계좌 조회 요청..."
curl -s ${ACCOUNT_API}/api/accounts/${ACCOUNT_ID} | jq '.' 2>/dev/null || echo "계좌 조회 실패"

echo ""
echo ""
echo -e "${YELLOW}2. Deposit Service 테스트${NC}"
echo "----------------------------------------"

# 적금 가입
echo -e "${GREEN}[POST]${NC} 자유적금 가입 요청..."
DEPOSIT_RESPONSE=$(curl -s -X POST ${DEPOSIT_API}/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": '${CUSTOMER_ID}',
    "accountId": '${ACCOUNT_ID:-1}',
    "productType": "FREE_SAVINGS",
    "principalAmount": 0,
    "baseRate": 3.15,
    "contractPeriod": 12,
    "periodUnit": "MONTH",
    "startDate": "2024-01-01",
    "maturityDate": "2025-01-01",
    "minMonthlyPayment": 100000,
    "maxMonthlyPayment": 500000
  }')

if [ $? -eq 0 ]; then
    echo "✅ 적금 가입 성공"
    echo "Response: ${DEPOSIT_RESPONSE}" | jq '.' 2>/dev/null || echo "${DEPOSIT_RESPONSE}"
    DEPOSIT_ID=$(echo "${DEPOSIT_RESPONSE}" | jq -r '.id' 2>/dev/null || echo "1")
else
    echo "❌ 적금 가입 실패"
fi

echo ""
echo ""
echo -e "${YELLOW}3. Card Service 테스트${NC}"
echo "----------------------------------------"

# 카드 신청
echo -e "${GREEN}[POST]${NC} 체크카드 신청 요청..."
CARD_APP_RESPONSE=$(curl -s -X POST ${CARD_API}/api/card-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": '${CUSTOMER_ID}',
    "accountId": '${ACCOUNT_ID:-1}',
    "requestedCardType": "DEBIT",
    "requestedProductType": "CHECK_CARD",
    "applicantName": "홍길동",
    "applicantPhone": "010-1234-5678",
    "applicantEmail": "test@bluebank.com"
  }')

if [ $? -eq 0 ]; then
    echo "✅ 카드 신청 성공"
    echo "Response: ${CARD_APP_RESPONSE}" | jq '.' 2>/dev/null || echo "${CARD_APP_RESPONSE}"
else
    echo "❌ 카드 신청 실패"
fi

echo ""
echo ""
echo -e "${YELLOW}4. Loan Service 테스트${NC}"
echo "----------------------------------------"

# 대출 신청
echo -e "${GREEN}[POST]${NC} 신용대출 신청 요청..."
LOAN_APP_RESPONSE=$(curl -s -X POST ${LOAN_API}/api/loan-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": '${CUSTOMER_ID}',
    "accountId": '${ACCOUNT_ID:-1}',
    "loanProductType": "PERSONAL_CREDIT",
    "requestedAmount": 10000000,
    "loanTerm": 12,
    "purpose": "생활자금",
    "monthlyIncome": 3000000,
    "employmentType": "정규직",
    "employmentYears": 3
  }')

if [ $? -eq 0 ]; then
    echo "✅ 대출 신청 성공"
    echo "Response: ${LOAN_APP_RESPONSE}" | jq '.' 2>/dev/null || echo "${LOAN_APP_RESPONSE}"
else
    echo "❌ 대출 신청 실패"
fi

echo ""
echo ""
echo "========================================="
echo -e "${GREEN}테스트 완료!${NC}"
echo "========================================="
echo ""
echo "🔍 서비스 상태 확인:"
echo "   - Eureka Dashboard: http://localhost:8761"
echo "   - Account Service: ${ACCOUNT_API}"
echo "   - Deposit Service: ${DEPOSIT_API}"
echo "   - Card Service: ${CARD_API}"
echo "   - Loan Service: ${LOAN_API}"
echo ""