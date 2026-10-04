package com.example.myapplication.feature.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.myapplication.R
import com.example.myapplication.AppThemeMode
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.ui.components.EmptyState
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
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileScreen(
    studyRepository: StudyRepository,
    modifier: Modifier = Modifier,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChange: (AppThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var selectedYear by remember { mutableIntStateOf(today.year) }
    var showSettings by remember { mutableStateOf(false) }
    var showThemeOptions by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val entries = studyRepository.all()
    val progressSummary = studyRepository.progressSummary()
    val totalSessions = entries.sumOf { it.sessionCount }
    val reviewCount = studyRepository.reviewSchedules().size
    val sessionCounts = remember(entries, selectedYear) {
        entries
            .filter { it.date.year == selectedYear }
            .groupBy { it.date }
            .mapValues { (_, dayEntries) -> dayEntries.sumOf { it.sessionCount } }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_profile),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.profile_settings)
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_study_streak),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = progressSummary.streakDays.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.streak_days),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                ProfileStatCard(
                    icon = Icons.Default.AccessTime,
                    value = stringResource(R.string.study_hours_value, progressSummary.monthlyStudyHours),
                    label = stringResource(R.string.this_month),
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    icon = Icons.Default.MenuBook,
                    value = progressSummary.subjectCount.toString(),
                    label = stringResource(R.string.subject_count),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                ProfileStatCard(
                    icon = Icons.Default.BarChart,
                    value = totalSessions.toString(),
                    label = stringResource(R.string.profile_sessions),
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    icon = Icons.Default.CheckCircle,
                    value = reviewCount.toString(),
                    label = stringResource(R.string.profile_reviews),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ActivityHeatmapCard(
            year = selectedYear,
            today = today,
            sessionCounts = sessionCounts,
            onPreviousYear = { selectedYear -= 1 },
            onNextYear = { selectedYear += 1 }
        )
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
                    .padding(bottom = MaterialTheme.spacing.large)
            ) {
                Text(
                    text = stringResource(R.string.profile_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.medium)
                )
                SettingsOption(
                    icon = when (themeMode) {
                        AppThemeMode.DARK -> Icons.Default.DarkMode
                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                        AppThemeMode.SYSTEM -> Icons.Default.Settings
                    },
                    title = stringResource(R.string.profile_theme),
                    value = stringResource(
                        when (themeMode) {
                            AppThemeMode.DARK -> R.string.profile_theme_dark
                            AppThemeMode.LIGHT -> R.string.profile_theme_light
                            AppThemeMode.SYSTEM -> R.string.profile_theme_system
                        }
                    ),
                    onClick = { showThemeOptions = true }
                )
                HorizontalDivider()
                SettingsOption(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.notifications),
                    onClick = {
                        showSettings = false
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }
                )
                HorizontalDivider()
                SettingsOption(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.profile_about),
                    onClick = {
                        showSettings = false
                        showAbout = true
                    }
                )
            }
        }
    }

    if (showThemeOptions) {
        AlertDialog(
            onDismissRequest = { showThemeOptions = false },
            title = { Text(stringResource(R.string.profile_theme)) },
            text = {
                Column {
                    listOf(
                        AppThemeMode.LIGHT to R.string.profile_theme_light,
                        AppThemeMode.DARK to R.string.profile_theme_dark,
                        AppThemeMode.SYSTEM to R.string.profile_theme_system
                    ).forEach { (mode, label) ->
                        Text(
                            text = stringResource(label),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemeOptions = false
                                }
                                .padding(vertical = MaterialTheme.spacing.medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeOptions = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.profile_about)) },
            text = { Text(stringResource(R.string.profile_about_description)) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }
}

@Composable
private fun ActivityHeatmapCard(
    year: Int,
    today: LocalDate,
    sessionCounts: Map<LocalDate, Int>,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit
) {
    val calendarYear = Year.of(year)
    val firstDay = calendarYear.atDay(1)
    val lastDay = calendarYear.atDay(calendarYear.length())
    val firstWeek = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastWeek = lastDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekCount = (ChronoUnit.DAYS.between(firstWeek, lastWeek) / 7 + 1).toInt()
    val gridScrollState = rememberScrollState()
    val density = LocalDensity.current
    val currentMonthWeekIndex = ChronoUnit.DAYS.between(
        firstWeek,
        calendarYear.atMonth(if (year == today.year) today.monthValue else 1).atDay(1)
    ).toInt() / 7

    LaunchedEffect(year, currentMonthWeekIndex, density) {
        val firstVisibleWeek = if (year == today.year) {
            (currentMonthWeekIndex - 2).coerceAtLeast(0)
        } else {
            0
        }
        val scrollOffset = with(density) {
            (contributionColumnStep * firstVisibleWeek).toPx().roundToInt()
        }
        gridScrollState.scrollTo(scrollOffset)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_activity_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.profile_contributions_in_year_label,
                            year
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onPreviousYear,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.profile_previous_year)
                    )
                }
                IconButton(
                    onClick = onNextYear,
                    enabled = year < today.year,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.profile_next_year)
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.profile_activity_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            if (sessionCounts.isEmpty()) {
                EmptyState(R.string.profile_activity_empty, year)
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp, end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(contributionCellGap)
                    ) {
                        val weekdayLabels = listOf(
                            R.string.calendar_weekday_monday,
                            null,
                            R.string.calendar_weekday_wednesday,
                            null,
                            R.string.calendar_weekday_friday,
                            null,
                            null
                        )
                        weekdayLabels.forEach { labelResource ->
                            Box(
                                modifier = Modifier
                                    .height(contributionRowHeight)
                                    .width(26.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                labelResource?.let {
                                    Text(
                                        text = stringResource(it),
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
                                val monthDate = calendarYear.atMonth(monthNumber).atDay(1)
                                val weekIndex =
                                    ChronoUnit.DAYS.between(firstWeek, monthDate).toInt() / 7
                                Text(
                                    text = monthDate.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMM",
                                            Locale.forLanguageTag("pt-BR")
                                        )
                                    ),
                                    modifier = Modifier.offset(
                                        x = contributionColumnStep * weekIndex
                                    ),
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
                                        val date = firstWeek.plusDays(weekIndex * 7L + dayIndex)
                                        if (date.year == year) {
                                            Box(
                                                modifier = Modifier.size(
                                                    width = contributionCellSize,
                                                    height = contributionRowHeight
                                                ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ContributionDay(
                                                    date = date,
                                                    sessions = sessionCounts[date] ?: 0,
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
}

@Composable
private fun ProfileStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MaterialTheme.spacing.medium)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    val description = pluralStringResource(
        R.plurals.profile_day_accessibility,
        sessions,
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
