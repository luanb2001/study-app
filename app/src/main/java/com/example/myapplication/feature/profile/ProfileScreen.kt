package com.example.myapplication.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.myapplication.R
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.ui.theme.spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val contributionCellSize = 10.dp
private val contributionCellGap = 2.dp
private val contributionColumnStep = contributionCellSize + contributionCellGap
private val contributionRowHeight = 14.dp

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    var selectedYear by remember { mutableIntStateOf(today.year) }
    val year = Year.of(selectedYear)
    val firstDay = year.atDay(1)
    val lastDay = year.atDay(year.length())
    val firstWeek = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastWeek = lastDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekCount = (ChronoUnit.DAYS.between(firstWeek, lastWeek) / 7 + 1).toInt()
    val gridScrollState = rememberScrollState()
    val density = LocalDensity.current
    val currentMonthWeekIndex = ChronoUnit.DAYS.between(
        firstWeek,
        year.atMonth(if (selectedYear == today.year) today.monthValue else 1).atDay(1)
    ).toInt() / 7

    LaunchedEffect(selectedYear, currentMonthWeekIndex, density) {
        val focusedWeek = if (selectedYear == today.year) {
            (currentMonthWeekIndex - 2).coerceAtLeast(0)
        } else {
            0
        }
        val scrollOffset = with(density) {
            (contributionColumnStep * focusedWeek).toPx().roundToInt()
        }
        gridScrollState.scrollTo(scrollOffset)
    }
    val locale = Locale.forLanguageTag("pt-BR")
    val entries = StudyRepository.all()
    val sessionCounts = remember(entries, selectedYear) {
        entries
            .filter { it.date.year == selectedYear }
            .groupingBy { it.date }
            .eachCount()
    }
    val contributionCount = sessionCounts.values.sum()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large)
    ) {
        Text(
            text = stringResource(R.string.nav_profile),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(
                    R.string.profile_contributions_in_year,
                    contributionCount,
                    selectedYear
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(
                    onClick = { selectedYear -= 1 },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.profile_previous_year)
                    )
                }
                IconButton(
                    onClick = { selectedYear += 1 },
                    enabled = selectedYear < today.year,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.profile_next_year)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(8.dp)
                )
                .padding(MaterialTheme.spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.padding(top = 16.dp, end = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(contributionCellGap)
                ) {
                    listOf(
                        stringResource(R.string.calendar_weekday_monday),
                        "",
                        stringResource(R.string.calendar_weekday_wednesday),
                        "",
                        stringResource(R.string.calendar_weekday_friday),
                        "",
                        ""
                    ).forEach { weekday ->
                        Box(
                            modifier = Modifier
                                .height(contributionRowHeight)
                                .width(26.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (weekday.isNotEmpty()) {
                                Text(
                                    text = weekday,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(gridScrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(contributionColumnStep * weekCount)
                            .height(16.dp)
                    ) {
                        for (monthNumber in 1..12) {
                            val monthDate = year.atMonth(monthNumber).atDay(1)
                            val weekIndex =
                                ChronoUnit.DAYS.between(firstWeek, monthDate).toInt() / 7
                            Text(
                                text = monthDate.format(
                                    DateTimeFormatter.ofPattern("MMM", locale)
                                ),
                                modifier = Modifier.offset(x = contributionColumnStep * weekIndex),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(contributionCellGap)) {
                        repeat(weekCount) { weekIndex ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(contributionCellGap)
                            ) {
                                repeat(7) { dayIndex ->
                                    val date = firstWeek.plusDays((weekIndex * 7L) + dayIndex)
                                    if (date.year == selectedYear) {
                                        val sessions = sessionCounts[date] ?: 0
                                        Box(
                                            modifier = Modifier.size(
                                                width = contributionCellSize,
                                                height = contributionRowHeight
                                            ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ContributionDay(
                                                date = date,
                                                sessions = sessions,
                                                isToday = date == today
                                            )
                                        }
                                    } else {
                                        Spacer(
                                            modifier = Modifier.size(
                                                width = contributionCellSize,
                                                height = contributionRowHeight
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.profile_less),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ContributionLegend(1)
                ContributionLegend(2)
                ContributionLegend(5)
                Text(
                    text = stringResource(R.string.profile_more),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContributionDay(date: LocalDate, sessions: Int, isToday: Boolean) {
    val shape = RoundedCornerShape(3.dp)
    val primary = MaterialTheme.colorScheme.primary
    val fill = when {
        sessions == 0 -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        sessions == 1 -> primary.copy(alpha = 0.3f)
        sessions <= 4 -> primary.copy(alpha = 0.65f)
        else -> primary
    }
    val description = stringResource(
        R.string.profile_day_accessibility,
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))),
        sessions
    )

    Box(
        modifier = Modifier
            .size(contributionCellSize)
            .background(fill, shape)
            .then(
                if (isToday) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, shape)
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = description }
    )
}

@Composable
private fun ContributionLegend(sessions: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val fill = when (sessions) {
        1 -> primary.copy(alpha = 0.3f)
        in 2..4 -> primary.copy(alpha = 0.65f)
        else -> primary
    }
    Box(
        modifier = Modifier
            .size(contributionCellSize)
            .background(fill, RoundedCornerShape(2.dp))
    )
}
