# Gradle Wrapper 9.6.1 일괄 업그레이드 설계

## 목표

루트 프로젝트와 `app/*`, `data/*` 아래의 모든 독립 Gradle Wrapper를 정식 릴리스인 Gradle 9.6.1로 통일한다. 신규 `app/batch` 모듈도 동일한 기준을 적용한다.

## 변경 범위

- 루트와 각 모듈의 `gradle/wrapper/gradle-wrapper.properties`를 Gradle 9.6.1 배포본으로 맞춘다.
- 각 wrapper의 `gradle-wrapper.jar`, `gradlew`, `gradlew.bat`를 Gradle 9.6.1이 생성한 파일로 갱신한다.
- 기존에 작업 중인 애플리케이션 코드와 빌드 설정 변경은 보존한다.
- 하위 wrapper를 제거하거나 멀티 프로젝트 구조를 재편하지 않는다.

## 구현 방식

루트 wrapper와 각 독립 모듈 wrapper에서 Gradle의 `wrapper` 태스크를 실행해 생성 파일 전체를 갱신한다. 속성 파일의 URL만 손으로 바꾸는 방식은 wrapper JAR 및 실행 스크립트와의 버전 불일치를 남길 수 있으므로 사용하지 않는다.

Gradle 9.6.1 wrapper 생성 후 모든 `gradle-wrapper.properties`에 다음 공통 설정이 존재하는지 검사한다.

- `distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip`
- 배포 URL 검증 활성화
- 현재 프로젝트에서 사용하는 네트워크 재시도 설정 유지

## 오류 처리와 안전성

- 기존 미커밋 변경을 되돌리거나 덮어쓰지 않는다.
- wrapper 태스크가 예상 범위 밖의 파일을 변경하면 해당 변경을 적용하지 않고 차이를 검토한다.
- 다운로드 또는 도구체인 문제로 빌드가 실패하면 wrapper 업그레이드 문제와 환경 문제를 구분해 보고한다.

## 검증

1. 모든 wrapper 속성 파일이 정확히 9.6.1을 가리키는지 정적 검사한다.
2. 루트와 각 독립 모듈에서 `./gradlew --version`을 실행해 실제 Gradle 버전이 9.6.1인지 확인한다.
3. 루트 멀티 프로젝트의 compile/test 또는 build를 실행한다.
4. 독립 모듈 wrapper에서도 필요한 compile/build 검증을 수행한다.
5. 최종 `git diff`로 wrapper 관련 변경과 기존 사용자 변경이 섞이거나 손상되지 않았는지 확인한다.

## 완료 기준

- 모든 wrapper가 Gradle 9.6.1로 실행된다.
- 전체 프로젝트가 성공적으로 컴파일되고 관련 테스트가 통과한다.
- 기존 미커밋 변경이 보존된다.
