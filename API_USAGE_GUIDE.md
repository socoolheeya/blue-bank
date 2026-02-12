# Blue Bank API 사용 가이드

## 1. 서비스 시작하기

### 전체 서비스 실행
```bash
# 모든 서비스 빌드
./build-all.sh

# Docker Compose로 전체 실행
docker-compose up -d
```

### 서비스 상태 확인
```bash
# 실행 중인 컨테이너 확인
docker-compose ps

# Eureka Dashboard 확인
open http://localhost:8761
```

## 2. API 엔드포인트

### 🏦 Account Service (계좌 관리)

#### 계좌 생성
```bash
curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountType": "CHECKING",
    "accountName": "월급통장",
    "currency": "KRW",
    "initialDeposit": 100000
  }'
```

#### 계좌 조회
```bash
# 특정 계좌 조회
curl http://localhost:8081/api/accounts/{accountId}

# 고객의 모든 계좌 조회
curl http://localhost:8081/api/accounts/customer/{customerId}
```

#### 계좌 잔액 조회
```bash
curl http://localhost:8081/api/accounts/{accountId}/balance
```

#### 입금
```bash
curl -X POST http://localhost:8081/api/accounts/{accountId}/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500000,
    "description": "월급"
  }'
```

#### 출금
```bash
curl -X POST http://localhost:8081/api/accounts/{accountId}/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50000,
    "description": "ATM 출금"
  }'
```

#### 계좌 이체
```bash
curl -X POST http://localhost:8081/api/accounts/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 100000,
    "description": "용돈"
  }'
```

---

### 💰 Deposit Service (예금/적금)

#### 예금 상품 가입
```bash
# 정기예금 가입
curl -X POST http://localhost:8084/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountId": 1,
    "productType": "FIXED_DEPOSIT",
    "principalAmount": 10000000,
    "baseRate": 2.95,
    "contractPeriod": 12,
    "periodUnit": "MONTH",
    "startDate": "2024-01-01",
    "maturityDate": "2025-01-01"
  }'

# 자유적금 가입
curl -X POST http://localhost:8084/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountId": 1,
    "productType": "FREE_SAVINGS",
    "principalAmount": 0,
    "baseRate": 3.15,
    "contractPeriod": 12,
    "periodUnit": "MONTH",
    "startDate": "2024-01-01",
    "maturityDate": "2025-01-01",
    "minMonthlyPayment": 100000,
    "maxMonthlyPayment": 500000,
    "autoTransferEnabled": true,
    "autoTransferDay": 25,
    "autoTransferAmount": 300000
  }'
```

#### 예금 활성화
```bash
curl -X POST http://localhost:8084/api/deposits/{depositId}/activate?customerId=1
```

#### 적금 입금
```bash
curl -X POST http://localhost:8084/api/deposits/{depositId}/deposit?customerId=1 \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 300000,
    "description": "1월 정기납입"
  }'
```

#### 예금/적금 조회
```bash
# 특정 예금 조회
curl http://localhost:8084/api/deposits/{depositId}

# 고객의 모든 예금/적금 조회
curl http://localhost:8084/api/deposits/customer/{customerId}
```

#### 중도 해지
```bash
curl -X POST http://localhost:8084/api/deposits/{depositId}/terminate?customerId=1
```

---

### 💳 Card Service (카드 관리)

#### 카드 신청
```bash
curl -X POST http://localhost:8083/api/card-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountId": 1,
    "requestedCardType": "DEBIT",
    "requestedProductType": "CHECK_CARD",
    "applicantName": "홍길동",
    "applicantPhone": "010-1234-5678",
    "applicantEmail": "hong@email.com",
    "annualIncome": 50000000,
    "employmentStatus": "직장인",
    "companyName": "블루뱅크",
    "requestedLimit": 5000000
  }'
```

#### 카드 활성화
```bash
curl -X POST http://localhost:8083/api/cards/{cardId}/activate?customerId=1
```

#### 카드 조회
```bash
# 특정 카드 조회
curl http://localhost:8083/api/cards/{cardId}

# 고객의 모든 카드 조회
curl http://localhost:8083/api/cards/customer/{customerId}
```

#### 카드 사용 설정
```bash
curl -X PUT http://localhost:8083/api/cards/{cardId}/toggle?customerId=1 \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": false
  }'
```

#### 카드 분실 신고
```bash
curl -X POST http://localhost:8083/api/cards/{cardId}/report-lost?customerId=1
```

---

### 🏠 Loan Service (대출)

#### 대출 신청
```bash
# 신용대출 신청
curl -X POST http://localhost:8082/api/loan-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountId": 1,
    "loanProductType": "PERSONAL_CREDIT",
    "requestedAmount": 30000000,
    "loanTerm": 36,
    "purpose": "생활자금",
    "monthlyIncome": 5000000,
    "employmentType": "정규직",
    "employmentYears": 5
  }'
```

#### 대출 승인
```bash
curl -X POST http://localhost:8082/api/loan-applications/{applicationId}/approve?approverId=admin
```

#### 대출 실행
```bash
curl -X POST http://localhost:8082/api/loans/{loanId}/disburse
```

#### 대출 상환
```bash
curl -X POST http://localhost:8082/api/loans/{loanId}/repay \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1500000,
    "repaymentType": "REGULAR"
  }'
```

#### 대출 조회
```bash
# 특정 대출 조회
curl http://localhost:8082/api/loans/{loanId}

# 고객의 모든 대출 조회
curl http://localhost:8082/api/loans/customer/{customerId}
```

---

## 3. API Gateway를 통한 통합 접근

모든 서비스는 API Gateway(포트 8080)를 통해서도 접근 가능합니다:

```bash
# Account Service via Gateway
curl http://localhost:8080/api/accounts/{accountId}

# Deposit Service via Gateway
curl http://localhost:8080/api/deposits/{depositId}

# Card Service via Gateway
curl http://localhost:8080/api/cards/{cardId}

# Loan Service via Gateway
curl http://localhost:8080/api/loans/{loanId}
```

---

## 4. 시나리오별 사용 예시

### 시나리오 1: 신규 고객 가입 및 계좌 개설
```bash
# 1. 계좌 생성
ACCOUNT_ID=$(curl -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountType": "CHECKING",
    "accountName": "주거래통장",
    "currency": "KRW",
    "initialDeposit": 100000
  }' | jq -r '.id')

# 2. 체크카드 신청
CARD_APP_ID=$(curl -X POST http://localhost:8083/api/card-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountId": '$ACCOUNT_ID',
    "requestedCardType": "DEBIT",
    "requestedProductType": "CHECK_CARD",
    "applicantName": "홍길동"
  }' | jq -r '.id')

# 3. 적금 가입
DEPOSIT_ID=$(curl -X POST http://localhost:8084/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountId": '$ACCOUNT_ID',
    "productType": "FREE_SAVINGS",
    "baseRate": 3.15,
    "contractPeriod": 12
  }' | jq -r '.id')
```

### 시나리오 2: 월급날 자동 처리
```bash
# 1. 월급 입금
curl -X POST http://localhost:8081/api/accounts/1/deposit \
  -H "Content-Type: application/json" \
  -d '{"amount": 3000000, "description": "1월 급여"}'

# 2. 적금 자동이체
curl -X POST http://localhost:8084/api/deposits/1/deposit?customerId=1 \
  -H "Content-Type: application/json" \
  -d '{"amount": 500000, "description": "1월 적금"}'

# 3. 대출 상환
curl -X POST http://localhost:8082/api/loans/1/repay \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000000, "repaymentType": "REGULAR"}'
```

---

## 5. 테스트 도구

### Postman Collection
`postman/` 디렉토리에 Postman Collection 파일이 있습니다.

### Swagger UI (개발 중)
- Account: http://localhost:8081/swagger-ui.html
- Deposit: http://localhost:8084/swagger-ui.html
- Card: http://localhost:8083/swagger-ui.html
- Loan: http://localhost:8082/swagger-ui.html

---

## 6. 모니터링

### Eureka Dashboard
http://localhost:8761 에서 모든 서비스의 상태를 확인할 수 있습니다.

### Health Check
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8082/actuator/health
```

---

## 7. 트러블슈팅

### 서비스가 시작되지 않을 때
```bash
# 로그 확인
docker-compose logs -f [service-name]

# 서비스 재시작
docker-compose restart [service-name]

# 전체 재빌드
docker-compose down
./build-all.sh
docker-compose up -d --build
```

### 포트 충돌
이미 사용 중인 포트가 있다면 `docker-compose.yml`에서 포트를 변경하세요.

---

## 8. 주의사항

1. **H2 Database**: 현재 메모리 DB를 사용하므로 서비스 재시작 시 데이터가 초기화됩니다.
2. **인증/인가**: 현재는 보안 기능이 없으므로 프로덕션 환경에서는 JWT 등의 인증을 추가해야 합니다.
3. **트랜잭션**: 서비스 간 분산 트랜잭션은 구현되지 않았습니다. Saga 패턴 등을 고려하세요.