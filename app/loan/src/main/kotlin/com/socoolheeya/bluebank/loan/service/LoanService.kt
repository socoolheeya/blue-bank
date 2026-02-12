package com.socoolheeya.bluebank.loan.service

import com.socoolheeya.bluebank.loan.client.AccountClient
import com.socoolheeya.bluebank.loan.data.domain.LoanEnums
import com.socoolheeya.bluebank.loan.data.domain.command.LoanCommand
import com.socoolheeya.bluebank.loan.data.service.LoanDataService
import com.socoolheeya.bluebank.loan.dto.LoanDto
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class LoanService(
    private val loanDataService: LoanDataService,
    private val accountClient: AccountClient
) {

    fun getLoan(loanId: Long): LoanDto.Response {
        val result = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다: $loanId")

        return LoanDto.Response.from(result)
    }

    fun getLoansByCustomerId(customerId: Long): List<LoanDto.Response> {
        val results = loanDataService.getLoansByCustomerId(customerId)
        return results.map { LoanDto.Response.from(it) }
    }

    fun executeLoan(loanId: Long): LoanDto.Response {
        // 1. 대출 정보 조회
        val loan = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다: $loanId")

        // 2. 계좌에 대출금 입금
        // TODO: AccountClient에 deposit 메서드 추가 필요
        // accountClient.deposit(
        //     accountId = loan.accountId,
        //     amount = loan.principalAmount,
        //     description = "대출금 입금 - ${loan.loanNumber}"
        // )

        // 3. 대출 실행 처리
        val command = LoanCommand.Execute(
            loanId = loanId,
            accountId = loan.accountId
        )
        val result = loanDataService.executeLoan(command)

        return LoanDto.Response.from(result)
    }

    fun repayLoan(loanId: Long, amount: BigDecimal): LoanDto.Response {
        // 1. 대출 정보 조회
        val loan = loanDataService.getLoan(loanId)
            ?: throw NoSuchElementException("대출을 찾을 수 없습니다: $loanId")

        // 2. 계좌에서 출금
        // TODO: AccountClient에 withdraw 메서드 추가 필요
        // accountClient.withdraw(
        //     accountId = loan.accountId,
        //     amount = amount,
        //     description = "대출 상환 - ${loan.loanNumber}"
        // )

        // 3. 대출 상환 처리
        val command = LoanCommand.Repay(
            loanId = loanId,
            amount = amount,
            repaymentType = LoanEnums.RepaymentType.EARLY
        )
        val result = loanDataService.repayLoan(command)

        return LoanDto.Response.from(result)
    }
}