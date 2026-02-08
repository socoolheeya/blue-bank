package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.IntegrationTestBase
import com.socoolheeya.bluebank.account.data.domain.Account
import com.socoolheeya.bluebank.account.data.domain.AccountEnums
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("AccountRepository 테스트")
class AccountRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Test
    @DisplayName("계좌 저장 및 조회 테스트")
    fun `should save and find account by account number`() {
        // Given
        val account = Account(
            accountNumber = "1002-3456-7890",
            name = "테스트 계좌",
            accountType = AccountEnums.AccountType.CHECKING,
            productType = AccountEnums.ProductType.BASIC_CHECKING,
            status = AccountEnums.AccountStatus.ACTIVE,
            interestRate = BigDecimal("0.01"),
            openedAt = LocalDateTime.now()
        )

        // When
        val savedAccount = accountRepository.save(account)
        flushAndClear()

        val foundAccount = accountRepository.findByAccountNumber("1002-3456-7890")

        // Then
        assertThat(foundAccount).isNotNull
        assertThat(foundAccount?.id).isEqualTo(savedAccount.id)
        assertThat(foundAccount?.accountNumber).isEqualTo("1002-3456-7890")
        assertThat(foundAccount?.name).isEqualTo("테스트 계좌")
        assertThat(foundAccount?.productType).isEqualTo(AccountEnums.ProductType.BASIC_CHECKING)
    }

    @Test
    @DisplayName("ProductType으로 계좌 목록 조회")
    fun `should find accounts by product type`() {
        // Given
        val account1 = Account(
            accountNumber = "1000-0001",
            accountType = AccountEnums.AccountType.CHECKING,
            productType = AccountEnums.ProductType.BASIC_CHECKING,
            status = AccountEnums.AccountStatus.ACTIVE,
            interestRate = BigDecimal.ZERO,
            openedAt = LocalDateTime.now()
        )

        val account2 = Account(
            accountNumber = "1000-0002",
            accountType = AccountEnums.AccountType.CHECKING,
            productType = AccountEnums.ProductType.GROUP_MEETING,
            status = AccountEnums.AccountStatus.ACTIVE,
            interestRate = BigDecimal.ZERO,
            openedAt = LocalDateTime.now()
        )

        accountRepository.saveAll(listOf(account1, account2))
        flushAndClear()

        // When
        val basicCheckingAccounts = accountRepository.findByProductType(AccountEnums.ProductType.BASIC_CHECKING)

        // Then
        assertThat(basicCheckingAccounts).hasSize(1)
        assertThat(basicCheckingAccounts[0].accountNumber).isEqualTo("1000-0001")
    }

    @Test
    @DisplayName("부모 계좌로 자식 계좌 조회")
    fun `should find child accounts by parent account id`() {
        // Given
        val parentAccount = Account(
            accountNumber = "1000-0000",
            accountType = AccountEnums.AccountType.CHECKING,
            productType = AccountEnums.ProductType.BASIC_CHECKING,
            status = AccountEnums.AccountStatus.ACTIVE,
            interestRate = BigDecimal.ZERO,
            openedAt = LocalDateTime.now()
        )
        val savedParent = accountRepository.save(parentAccount)
        flushAndClear()

        val childAccount = Account(
            accountNumber = "1000-0001",
            accountType = AccountEnums.AccountType.SAVINGS,
            productType = AccountEnums.ProductType.CHILD_ACCOUNT,
            status = AccountEnums.AccountStatus.ACTIVE,
            interestRate = BigDecimal("0.02"),
            openedAt = LocalDateTime.now(),
            parentAccountId = savedParent.id
        )
        accountRepository.save(childAccount)
        flushAndClear()

        // When
        val childAccounts = accountRepository.findByParentAccountId(savedParent.id!!)

        // Then
        assertThat(childAccounts).hasSize(1)
        assertThat(childAccounts[0].accountNumber).isEqualTo("1000-0001")
        assertThat(childAccounts[0].productType).isEqualTo(AccountEnums.ProductType.CHILD_ACCOUNT)
    }
}