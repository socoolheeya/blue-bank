package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.TestConfig
import com.socoolheeya.bluebank.account.data.domain.Account
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import jakarta.persistence.EntityManager
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

private inline fun withRepository(crossinline block: (AccountRepository, EntityManager) -> Unit) {
    val context = SpringApplicationBuilder(TestConfig::class.java)
        .web(WebApplicationType.NONE)
        .profiles("test")
        .run()
    try {
        val repository = context.getBean(AccountRepository::class.java)
        val entityManager = context.getBean(EntityManager::class.java)
        val transactionManager = context.getBean(org.springframework.transaction.PlatformTransactionManager::class.java)
        TransactionTemplate(transactionManager).executeWithoutResult {
            block(repository, entityManager)
            it.setRollbackOnly()
        }
    } finally {
        context.close()
    }
}

private fun AccountRepository.flush(entityManager: EntityManager) {
    entityManager.flush()
    entityManager.clear()
}

val accountRepositorySuite by testSuite(
    compartment = { TestCompartment.Sequential }
) {
    test("계좌 저장 및 조회") {
        withRepository { repository, entityManager ->
            val account = Account(
                accountNumber = "1002-3456-7890", name = "테스트 계좌",
                accountType = AccountEnums.AccountType.CHECKING,
                productType = AccountEnums.ProductType.BASIC_CHECKING,
                status = AccountEnums.AccountStatus.ACTIVE, interestRate = BigDecimal("0.01"),
                openedAt = LocalDateTime.now()
            )
            val saved = repository.save(account)
            repository.flush(entityManager)
            val found = repository.findByAccountNumber(account.accountNumber)
            check(found != null)
            check(saved.id == found.id)
            check(account.productType == found.productType)
        }
    }

    test("ProductType으로 계좌 목록 조회") {
        withRepository { repository, entityManager ->
            repository.saveAll(listOf(
                Account(accountNumber = "1000-0001", accountType = AccountEnums.AccountType.CHECKING,
                    productType = AccountEnums.ProductType.BASIC_CHECKING, status = AccountEnums.AccountStatus.ACTIVE,
                    interestRate = BigDecimal.ZERO, openedAt = LocalDateTime.now()),
                Account(accountNumber = "1000-0002", accountType = AccountEnums.AccountType.CHECKING,
                    productType = AccountEnums.ProductType.GROUP_MEETING, status = AccountEnums.AccountStatus.ACTIVE,
                    interestRate = BigDecimal.ZERO, openedAt = LocalDateTime.now())
            ))
            repository.flush(entityManager)
            val found = repository.findByProductType(AccountEnums.ProductType.BASIC_CHECKING)
            check(found.size == 1)
            check(found.single().accountNumber == "1000-0001")
        }
    }

    test("부모 계좌로 자식 계좌 조회") {
        withRepository { repository, entityManager ->
            val parent = repository.save(Account(accountNumber = "1000-0000", accountType = AccountEnums.AccountType.CHECKING,
                productType = AccountEnums.ProductType.BASIC_CHECKING, status = AccountEnums.AccountStatus.ACTIVE,
                interestRate = BigDecimal.ZERO, openedAt = LocalDateTime.now()))
            repository.flush(entityManager)
            repository.save(Account(accountNumber = "1000-0001", accountType = AccountEnums.AccountType.SAVINGS,
                productType = AccountEnums.ProductType.CHILD_ACCOUNT, status = AccountEnums.AccountStatus.ACTIVE,
                interestRate = BigDecimal("0.02"), openedAt = LocalDateTime.now(), parentAccountId = parent.id))
            repository.flush(entityManager)
            val found = repository.findByParentAccountId(parent.id!!)
            check(found.size == 1)
            check(found.single().productType == AccountEnums.ProductType.CHILD_ACCOUNT)
        }
    }
}
