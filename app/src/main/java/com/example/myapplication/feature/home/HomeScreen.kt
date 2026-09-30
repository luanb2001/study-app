package com.example.myapplication.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.study.ReviewSchedule
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.feature.study.StudyProgressSummary
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.theme.spacing
import java.time.LocalDate

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    studyRepository: StudyRepository,
    onStartStudy: () -> Unit = {},
    onScheduleStudy: () -> Unit = {},
    onStartPomodoro: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        TodayReviews(
            studyRepository = studyRepository,
            onStartPomodoro = onStartPomodoro
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        StartStudyCard(
            onStartStudy = onStartStudy,
            onScheduleStudy = onScheduleStudy
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        ProgressSummary(summary = studyRepository.progressSummary())

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    }
}

@Composable
private fun TodayReviews(
    studyRepository: StudyRepository,
    onStartPomodoro: (String) -> Unit
) {
    var selectedReview by rememberSaveable { mutableStateOf<String?>(null) }
    val reviews = studyRepository.dueReviews()
    val selectedReviewIsAvailable = reviews.any { it.subject == selectedReview }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        ) {
            Text(
                text = stringResource(R.string.today_reviews),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            if (reviews.isEmpty()) {
                EmptyState(R.string.reviews_empty)
            } else {
                reviews.forEach { review ->
                    val isSelected = selectedReview == review.subject
                    ReviewItem(
                        review = review,
                        isSelected = isSelected,
                        onClick = { selectedReview = review.subject }
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                }
            }

            Button(
                enabled = selectedReviewIsAvailable,
                onClick = {
                    selectedReview?.let(onStartPomodoro)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.start_review))
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: ReviewSchedule,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isOverdue = review.dueDate.isBefore(LocalDate.now())
    val statusText = stringResource(
        if (isOverdue) R.string.overdue else R.string.review_today
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .then(
                if (isSelected) {
                    Modifier.background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            ),
            shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (isOverdue) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isOverdue) FontWeight.Medium else FontWeight.Normal,
                    color = if (isOverdue) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
            }
        }
    }
}

@Composable
private fun StartStudyCard(
    onStartStudy: () -> Unit,
    onScheduleStudy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                text = stringResource(R.string.start_study_question),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Text(
                text = stringResource(R.string.start_study_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Button(
                onClick = onStartStudy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.start_study))
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            OutlinedButton(
                onClick = onScheduleStudy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.schedule_study))
            }
        }
    }
}

@Composable
private fun ProgressSummary(
    summary: StudyProgressSummary
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.progress),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (summary.subjectCount == 0) {
                EmptyState(R.string.progress_empty)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    ProgressItem(
                        value = summary.streakDays.toString(),
                        label = stringResource(R.string.streak_days),
                        modifier = Modifier.weight(1f)
                    )
                    ProgressItem(
                        value = stringResource(
                            R.string.study_hours_value,
                            summary.monthlyStudyHours
                        ),
                        label = stringResource(R.string.this_month),
                        modifier = Modifier.weight(1f)
                    )
                    ProgressItem(
                        value = summary.subjectCount.toString(),
                        label = stringResource(R.string.subject_count),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressItem(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
