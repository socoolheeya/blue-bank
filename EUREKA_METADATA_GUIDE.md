# Eureka AMIs와 Availability Zones 설정 가이드

## 개념 설명

### AMIs (Amazon Machine Images)
- AWS EC2 인스턴스를 생성할 때 사용하는 템플릿
- 로컬 환경에서는 "n/a"로 표시됨
- AWS 환경에서만 실제 AMI ID가 표시됨 (예: ami-12345678)

### Availability Zones (가용 영역)
- AWS 데이터센터 내의 물리적으로 분리된 영역
- 로컬 환경에서는 기본값 1로 표시
- AWS에서는 여러 AZ에 걸쳐 서비스 분산 가능

## 현재 설정 상태

### docker-compose-scale.yml 업데이트
각 서비스에 메타데이터 추가:
- **ACCOUNT**: zone-a (Zone A에 배치)
- **DEPOSIT**: zone-b (Zone B에 배치)
- **LOAN**: zone-c (Zone C에 배치)
- **CARD**: zone-a (Zone A에 배치)

```yaml
environment:
  - EUREKA_INSTANCE_METADATA_MAP_ZONE=zone-a
  - EUREKA_INSTANCE_METADATA_MAP_DATACENTER=Docker
  - EUREKA_INSTANCE_METADATA_MAP_ENVIRONMENT=development
```

## 설정 방법

### 1. 로컬 환경 (Docker)
```yaml
# docker-compose.yml에 환경변수 추가
environment:
  - EUREKA_INSTANCE_METADATA_MAP_ZONE=zone-a
  - EUREKA_INSTANCE_METADATA_MAP_DATACENTER=Docker
  - EUREKA_INSTANCE_METADATA_MAP_ENVIRONMENT=development
```

### 2. AWS 환경
```yaml
# application-aws.yml
eureka:
  instance:
    metadata-map:
      zone: ${AWS_ZONE:us-east-1a}
      ami-id: ${AWS_AMI_ID:ami-12345678}
      instance-type: ${AWS_INSTANCE_TYPE:t2.micro}
    datacenter: Amazon
    environment: production
```

### 3. 멀티 AZ 구성 (AWS)
```yaml
eureka:
  client:
    # 여러 AZ의 Eureka 서버
    service-url:
      us-east-1a: http://eureka-1a.example.com:8761/eureka/
      us-east-1b: http://eureka-1b.example.com:8761/eureka/
      us-east-1c: http://eureka-1c.example.com:8761/eureka/

    # 가용 영역 설정
    availability-zones:
      us-east-1: us-east-1a,us-east-1b,us-east-1c

    region: us-east-1
    prefer-same-zone-eureka: true  # 같은 AZ 우선
```

## 메타데이터 활용

### 1. Zone-aware 로드밸런싱
```java
// 같은 zone의 서비스 우선 호출
@Configuration
public class RibbonConfig {
    @Bean
    public IRule ribbonRule() {
        return new ZoneAvoidanceRule();
    }
}
```

### 2. 커스텀 메타데이터 활용
```java
@Component
public class ServiceDiscovery {
    @Autowired
    private DiscoveryClient discoveryClient;

    public List<ServiceInstance> getInstancesInZone(String zone) {
        return discoveryClient.getInstances("account")
            .stream()
            .filter(instance ->
                zone.equals(instance.getMetadata().get("zone")))
            .collect(Collectors.toList());
    }
}
```

## 확인 방법

### 1. Eureka Dashboard
- http://localhost:8761 접속
- 각 서비스의 인스턴스 정보에서 메타데이터 확인

### 2. REST API
```bash
# 특정 서비스의 메타데이터 확인
curl http://localhost:8761/eureka/apps/ACCOUNT | grep metadata

# 모든 서비스 정보
curl http://localhost:8761/eureka/apps
```

### 3. 확인 스크립트
```bash
./check-eureka-instances.sh
```

## 실제 AWS 배포 시 고려사항

### 1. EC2 메타데이터 자동 감지
EC2에서 실행 시 자동으로 감지되는 정보:
- Instance ID
- Availability Zone
- Public/Private IP
- AMI ID

### 2. Spring Cloud AWS 통합
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-aws</artifactId>
</dependency>
```

### 3. Auto Scaling Group 태그 활용
```yaml
eureka:
  instance:
    metadata-map:
      asg: ${AWS_AUTOSCALING_GROUP}
      deployment: blue-green
```

## 트러블슈팅

### 메타데이터가 표시되지 않을 때
1. 서비스 재시작 필요
2. Eureka 캐시 갱신 대기 (30초)
3. 환경변수 확인: `docker inspect <container-id>`

### Zone 정보가 활용되지 않을 때
1. Ribbon의 ZoneAvoidanceRule 설정 확인
2. prefer-same-zone-eureka 속성 확인
3. 각 zone에 충분한 인스턴스가 있는지 확인

## 참고 자료
- [Spring Cloud Netflix Documentation](https://cloud.spring.io/spring-cloud-netflix/reference/html/)
- [AWS Availability Zones](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html)
- [Eureka Wiki](https://github.com/Netflix/eureka/wiki)