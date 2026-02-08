# Account Feign Client 사용 가이드

## 개요

`AccountClient`는 다른 마이크로서비스(card, deposit, loan 등)에서 Account 서비스의 계좌 정보를 조회하기 위한 Feign Client입니다.

## 설정

### 1. 의존성 추가

다른 서비스의 `build.gradle.kts`에 다음 의존성을 추가:

```kotlin
dependencies {
    implementation(project(":app:account"))
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
}
```

### 2. @EnableFeignClients 활성화

Application 클래스에 `@EnableFeignClients` 추가:

```kotlin
@SpringBootApplication
@EnableFeignClients(basePackages = ["com.socoolheeya.bluebank.account.adapter"])
class YourApplication

fun main(args: Array<String>) {
    runApplication<YourApplication>(*args)
}
```

### 3. application.yml 설정

```yaml
feign:
  account:
    url: http://localhost:8080  # Account 서비스 URL (환경별로 변경)
  client:
    config:
      account-service:
        connectTimeout: 3000
        readTimeout: 5000
        loggerLevel: basic
```

## API 사용 예제

### 1. 계좌 정보 조회 (by ID)

```kotlin
@Service
class CardService(
    private val accountClient: AccountClient
) {
    fun validateAccountForCard(accountId: Long) {
        val account = accountClient.getAccount(accountId)

        if (account.status != AccountEnums.AccountStatus.ACTIVE) {
            throw IllegalStateException("비활성 계좌입니다")
        }

        println("계좌번호: ${account.accountNumber}")
        println("계좌 유형: ${account.productType}")
    }
}
```

### 2. 계좌 정보 조회 (by 계좌번호)

```kotlin
fun getAccountInfo(accountNumber: String) {
    val account = accountClient.getAccountByNumber(accountNumber)
    println("계좌 ID: ${account.id}")
    println("개설일: ${account.openedAt}")
}
```

### 3. 고객의 계좌 목록 조회

```kotlin
fun getCustomerAccounts(customerId: Long) {
    val accounts = accountClient.getAccountsByCustomerId(customerId)

    accounts.forEach { account ->
        println("계좌: ${account.accountNumber} - ${account.name}")
    }
}
```

### 4. 계좌 잔액 조회

```kotlin
fun checkBalance(accountId: Long) {
    val balance = accountClient.getBalance(accountId)

    println("총 잔액: ${balance.ledgerBalance}원")
    println("사용 가능 잔액: ${balance.availableBalance}원")
    println("보류 금액: ${balance.holdBalance}원")
}
```

### 5. 계좌 유효성 검증

```kotlin
fun validateAccount(accountId: Long): Boolean {
    val validation = accountClient.validateAccount(accountId)

    if (!validation.exists) {
        println("계좌가 존재하지 않습니다")
        return false
    }

    if (!validation.isActive) {
        println("비활성 계좌입니다: ${validation.message}")
        return false
    }

    return validation.isValid
}
```

## 에러 처리

`AccountFeignConfiguration`에서 정의된 `ErrorDecoder`가 HTTP 에러를 Kotlin 예외로 변환:

```kotlin
try {
    val account = accountClient.getAccount(accountId)
} catch (e: NoSuchElementException) {
    // 404: 계좌를 찾을 수 없음
    println("계좌가 존재하지 않습니다: ${e.message}")
} catch (e: IllegalArgumentException) {
    // 400: 잘못된 요청
    println("잘못된 요청입니다: ${e.message}")
} catch (e: IllegalStateException) {
    // 403: 접근 거부
    println("접근이 거부되었습니다: ${e.message}")
} catch (e: RuntimeException) {
    // 500 or others: 서버 오류
    println("서버 오류: ${e.message}")
}
```

## 내부 API 엔드포인트

Account 서비스가 제공하는 내부 API:

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/internal/accounts/{accountId}` | 계좌 ID로 조회 |
| GET | `/internal/accounts/by-number/{accountNumber}` | 계좌번호로 조회 |
| GET | `/internal/accounts/by-customer/{customerId}` | 고객의 계좌 목록 조회 |
| GET | `/internal/accounts/{accountId}/balance` | 계좌 잔액 조회 |
| GET | `/internal/accounts/{accountId}/validate` | 계좌 유효성 검증 |

## 주의사항

1. **내부 통신 전용**: `/internal/**` API는 마이크로서비스 간 내부 통신용이며, 외부에 노출되지 않아야 합니다.

2. **타임아웃 설정**: 네트워크 지연을 고려하여 적절한 타임아웃을 설정하세요.

3. **서킷 브레이커**: 프로덕션 환경에서는 Resilience4j 등을 사용한 서킷 브레이커 패턴 적용을 권장합니다.

4. **모니터링**: Feign Client 호출은 로그에 기록되며, 필요시 분산 추적(Distributed Tracing) 설정을 추가할 수 있습니다.

## 환경별 설정 예시

### Local 환경
```yaml
feign:
  account:
    url: http://localhost:8080
```

### Dev 환경
```yaml
feign:
  account:
    url: http://account-service.dev.svc.cluster.local:8080
```

### Production 환경
```yaml
feign:
  account:
    url: http://account-service.prod.svc.cluster.local:8080
```

## 추가 개선 사항

향후 추가 가능한 기능:

1. **분산 추적**: Spring Cloud Sleuth를 사용한 trace id 전파
2. **서킷 브레이커**: Resilience4j를 사용한 장애 격리
3. **재시도 로직**: Feign Retryer 설정
4. **인증/인가**: JWT 토큰 전달
5. **로드 밸런싱**: Spring Cloud LoadBalancer 통합
