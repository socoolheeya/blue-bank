# Gradle Wrapper 9.6.1 일괄 업그레이드 설계

## 목표

루트 프로젝트의 Gradle Wrapper를 정식 릴리스인 Gradle 9.6.1로 통일하고, `app/*`, `data/*` 아래의 중복 Wrapper를 제거한다. 모든 모듈은 루트 멀티 프로젝트의 프로젝트 경로를 통해 빌드한다.

## 변경 범위

- 루트의 `gradle/wrapper/gradle-wrapper.properties`, `gradle-wrapper.jar`, `gradlew`, `gradlew.bat`를 Gradle 9.6.1 기준으로 갱신한다.
- `app/*`, `data/*` 아래의 `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`를 제거한다.
- 기존에 작업 중인 애플리케이션 코드와 빌드 설정 변경은 보존한다.
- 각 모듈의 `build.gradle.kts`와 루트 `settings.gradle.kts` 기반 멀티 프로젝트 구조는 유지한다.

## 구현 방식

루트에서 Gradle의 `wrapper` 태스크를 실행해 생성 파일 전체를 갱신한다. 속성 파일의 URL만 손으로 바꾸는 방식은 wrapper JAR 및 실행 스크립트와의 버전 불일치를 남길 수 있으므로 사용하지 않는다.

Gradle 9.6.1 wrapper 생성 후 루트 `gradle-wrapper.properties`에 다음 설정이 존재하는지 검사한다.

- `distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.1-bin.zip`
- 배포 URL 검증 활성화
- 현재 프로젝트에서 사용하는 네트워크 재시도 설정 유지

모듈별 빌드는 하위 디렉터리에서 wrapper를 실행하지 않고 루트에서 프로젝트 경로를 지정한다.

- 전체 빌드: `./gradlew build`
- 애플리케이션 모듈: `./gradlew :app:deposit:build`
- 데이터 모듈: `./gradlew :data:deposit-data:test`

## 오류 처리와 안전성

- 기존 미커밋 변경을 되돌리거나 덮어쓰지 않는다.
- wrapper 태스크와 하위 wrapper 제거가 예상 범위 밖의 파일을 변경하지 않는지 차이를 검토한다.
- 다운로드 또는 도구체인 문제로 빌드가 실패하면 wrapper 업그레이드 문제와 환경 문제를 구분해 보고한다.

## 검증

1. 루트 wrapper 속성 파일이 정확히 9.6.1을 가리키는지 정적 검사한다.
2. 하위 모듈에 wrapper 파일이 남아 있지 않은지 검사한다.
3. 루트에서 `./gradlew --version`을 실행해 실제 Gradle 버전이 9.6.1인지 확인한다.
4. 루트 멀티 프로젝트의 compile/test 또는 build를 실행한다.
5. 루트 wrapper와 프로젝트 경로를 사용해 각 모듈이 빌드되는지 확인한다.
6. 최종 `git diff`로 wrapper 관련 변경과 기존 사용자 변경이 섞이거나 손상되지 않았는지 확인한다.

## 완료 기준

- 저장소에는 루트 Gradle 9.6.1 wrapper만 존재한다.
- 모든 모듈이 루트 wrapper의 프로젝트 경로로 빌드된다.
- 전체 프로젝트가 성공적으로 컴파일되고 관련 테스트가 통과한다.
- 기존 미커밋 변경이 보존된다.
