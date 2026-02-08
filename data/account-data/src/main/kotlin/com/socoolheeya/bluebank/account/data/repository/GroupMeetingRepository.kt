package com.socoolheeya.bluebank.account.data.repository

import com.socoolheeya.bluebank.account.data.domain.GroupMeeting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupMeetingRepository : JpaRepository<GroupMeeting, Long> {
    fun findByAccountId(accountId: Long): GroupMeeting?
    fun findByStatus(status: GroupMeeting.MeetingStatus): List<GroupMeeting>
}
