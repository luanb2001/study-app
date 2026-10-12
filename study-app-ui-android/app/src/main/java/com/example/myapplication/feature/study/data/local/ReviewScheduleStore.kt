package com.example.myapplication.feature.study.data.local

import android.content.Context
import com.example.myapplication.feature.account.data.local.AccountSessionStore
import com.example.myapplication.feature.account.data.local.DeviceIdentityStore
import com.example.myapplication.feature.study.domain.model.ReviewSchedule

class ReviewScheduleStore(
    context: Context,
    private val accountId: String =
        AccountSessionStore(context).load()?.accountId
            ?: DeviceIdentityStore(context).installationId(),
    private val studyDatabase: StudyDatabase = StudyDatabase(context)
) {

    fun load(): List<ReviewSchedule> = this@ReviewScheduleStore.studyDatabase.loadReviews(this@ReviewScheduleStore.accountId)

    fun save(schedules: List<ReviewSchedule>) = this@ReviewScheduleStore.studyDatabase.replaceReviews(this@ReviewScheduleStore.accountId, schedules)

    fun enqueueUpsert(review: ReviewSchedule) = this@ReviewScheduleStore.studyDatabase.enqueueReviewUpsert(this@ReviewScheduleStore.accountId, review)

    fun enqueueDelete(reviewId: String) = this@ReviewScheduleStore.studyDatabase.enqueueReviewDelete(this@ReviewScheduleStore.accountId, reviewId)

    fun deleteReviews(reviews: List<ReviewSchedule>) =
        this@ReviewScheduleStore.studyDatabase.deleteReviews(this@ReviewScheduleStore.accountId, reviews.map { it.id })
}
