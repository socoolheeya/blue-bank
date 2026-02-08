package com.socoolheeya.bluebank.account.data

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * Integration Test Base Class
 *
 * 모든 Repository 테스트는 이 클래스를 상속하여 사용
 * - H2 in-memory database 사용
 * - @SpringBootTest 를 통한 전체 애플리케이션 컨텍스트 로드
 * - @ActiveProfiles("test") 로 test 프로파일 활성화
 * - @Transactional 로 각 테스트 후 롤백
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestBase {

    @PersistenceContext
    protected lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        // 각 테스트 전에 영속성 컨텍스트를 초기화
        entityManager.clear()
    }

    /**
     * 영속성 컨텍스트를 플러시하고 클리어
     * 쿼리 실행 시점을 명확히 하기 위해 사용
     */
    protected fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
