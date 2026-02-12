# Blue Bank 고가용성(HA) 구성 가이드

## 🚀 삼중화(High Availability) 구성

Blue Bank 서비스를 삼중화하여 고가용성을 제공하는 두 가지 방법을 제공합니다.

### 방법 1: 고정 포트 방식 (docker-compose-ha.yml)

각 서비스 인스턴스에 고정된 포트를 할당하는 방식입니다.

#### 실행 방법
```bash
# 실행 스크립트 사용
./start-ha.sh

# 또는 docker-compose 직접 사용
docker-compose -f docker-compose-ha.yml up -d
```

#### 포트 할당 구조
| 서비스 | Instance 1 | Instance 2 | Instance 3 |
|--------|------------|------------|------------|
| Account Service | 8081 | 8091 | 8101 |
| Deposit Service | 8084 | 8094 | 8104 |
| Loan Service | 8082 | 8092 | 8102 |
| Card Service | 8083 | 8093 | 8103 |

#### 장점
- 각 인스턴스에 직접 접근 가능
- 디버깅과 모니터링이 용이
- 포트 번호가 고정되어 예측 가능

#### 단점
- 포트 관리가 복잡
- 스케일 조정이 어려움

---

### 방법 2: 동적 스케일링 방식 (docker-compose-scale.yml)

Docker Compose의 scale 기능을 사용하는 방식입니다.

#### 실행 방법
```bash
# 기본 3개 인스턴스로 실행
./start-scale.sh

# 5개 인스턴스로 실행
./start-scale.sh 5

# 또는 docker-compose scale 명령 사용
docker-compose -f docker-compose-scale.yml up -d
docker-compose -f docker-compose-scale.yml up -d --scale account-service=3 --scale deposit-service=3
```

#### 런타임 스케일 조정
```bash
# Account 서비스를 5개로 스케일 업
docker-compose -f docker-compose-scale.yml up -d --scale account-service=5

# Loan 서비스를 2개로 스케일 다운
docker-compose -f docker-compose-scale.yml up -d --scale loan-service=2
```

#### 장점
- 동적 스케일링 가능
- 관리가 간편
- 클라우드 환경에 적합

#### 단점
- 개별 인스턴스 포트가 랜덤
- 직접 접근이 어려움 (API Gateway 통해서만 접근)

---

## 🔄 로드밸런싱

### Eureka + Ribbon 기반 클라이언트 사이드 로드밸런싱

Spring Cloud Netflix Eureka와 Ribbon을 사용하여 자동 로드밸런싱이 적용됩니다.

1. **서비스 등록**: 각 서비스 인스턴스가 Eureka에 자동 등록
2. **서비스 발견**: 클라이언트가 Eureka에서 서비스 목록 조회
3. **로드밸런싱**: Ribbon이 Round-Robin 방식으로 요청 분산

### API Gateway를 통한 로드밸런싱

모든 외부 요청은 API Gateway(포트 8080)를 통해 라우팅됩니다.

```bash
# API Gateway를 통한 요청 (자동 로드밸런싱)
curl http://localhost:8080/account/api/accounts/1
curl http://localhost:8080/deposit/api/deposits/1
curl http://localhost:8080/loan/api/loans/1
curl http://localhost:8080/card/api/cards/1
```

---

## 📊 모니터링

### Eureka Dashboard
http://localhost:8761 에서 모든 서비스 인스턴스 상태 확인

### 서비스 상태 확인
```bash
# 등록된 서비스 확인
curl -s http://localhost:8761/eureka/apps | grep -o "<app>.*</app>"

# 실행 중인 컨테이너 확인
docker-compose -f docker-compose-ha.yml ps
```

### 로그 확인
```bash
# 특정 서비스의 모든 인스턴스 로그
docker-compose -f docker-compose-ha.yml logs account-service-1 account-service-2 account-service-3

# 실시간 로그 스트리밍
docker-compose -f docker-compose-ha.yml logs -f
```

---

## 🛠 트러블슈팅

### 서비스가 Eureka에 등록되지 않을 때
```bash
# Eureka Server 재시작
docker-compose -f docker-compose-ha.yml restart eureka-server

# 서비스 재시작
docker-compose -f docker-compose-ha.yml restart account-service-1
```

### 포트 충돌 발생 시
```bash
# 사용 중인 포트 확인
lsof -i :8081

# 기존 컨테이너 정리
docker-compose -f docker-compose-ha.yml down
docker system prune -a
```

### 메모리 부족 시
```bash
# Docker 리소스 제한 설정
docker-compose -f docker-compose-ha.yml up -d \
  --scale account-service=2 \
  --scale deposit-service=2
```

---

## 🔍 테스트 시나리오

### 1. 로드밸런싱 테스트
```bash
# 반복 요청으로 로드밸런싱 확인
for i in {1..10}; do
  echo "Request $i:"
  curl -s http://localhost:8080/account/actuator/health | jq '.status'
done
```

### 2. 장애 복구 테스트
```bash
# 인스턴스 하나 종료
docker stop account-service-1

# 서비스 가용성 확인 (다른 인스턴스가 처리)
curl http://localhost:8080/account/api/accounts/1

# 인스턴스 재시작
docker start account-service-1
```

### 3. 스케일 업/다운 테스트
```bash
# 부하 증가 시뮬레이션 후 스케일 업
docker-compose -f docker-compose-scale.yml up -d --scale account-service=5

# 부하 감소 후 스케일 다운
docker-compose -f docker-compose-scale.yml up -d --scale account-service=2
```

---

## 💡 권장사항

### 개발 환경
- **docker-compose-ha.yml** 사용 (고정 포트로 디버깅 용이)
- 각 인스턴스 개별 모니터링 가능

### 테스트 환경
- **docker-compose-scale.yml** 사용 (동적 스케일링 테스트)
- 부하 테스트 시 인스턴스 수 조정

### 프로덕션 환경
- Kubernetes 사용 권장
- HPA(Horizontal Pod Autoscaler)로 자동 스케일링
- 실제 데이터베이스 클러스터 사용 (H2 메모리 DB 대신)

---

## 📝 참고사항

1. **데이터베이스**: 현재 각 인스턴스가 독립적인 H2 메모리 DB를 사용합니다. 프로덕션에서는 공유 데이터베이스 필요.

2. **세션 관리**: 스테이트리스 설계로 세션 공유 문제 없음.

3. **캐싱**: Redis 등 분산 캐시 도입 시 더 나은 성능 제공.

4. **모니터링**: Prometheus + Grafana 등 전문 모니터링 도구 추가 권장.

5. **로깅**: ELK Stack (Elasticsearch, Logstash, Kibana) 도입으로 중앙 로그 관리.