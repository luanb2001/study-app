package com.example.myapplication.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.spacing

private data class OnboardingStep(
    val title: Int,
    val description: Int,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val steps = listOf(
        OnboardingStep(
            R.string.onboarding_step_subjects_title,
            R.string.onboarding_step_subjects_description,
            Icons.Default.AutoStories
        ),
        OnboardingStep(
            R.string.onboarding_step_focus_title,
            R.string.onboarding_step_focus_description,
            Icons.Default.Timer
        ),
        OnboardingStep(
            R.string.onboarding_step_reviews_title,
            R.string.onboarding_step_reviews_description,
            Icons.Default.Replay
        ),
        OnboardingStep(
            R.string.onboarding_step_schedule_title,
            R.string.onboarding_step_schedule_description,
            Icons.Default.CalendarMonth
        ),
        OnboardingStep(
            R.string.onboarding_step_progress_title,
            R.string.onboarding_step_progress_description,
            Icons.Default.Insights
        )
    )
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .heightIn(min = 500.dp, max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 620.dp)
                    .padding(
                        horizontal = MaterialTheme.spacing.large,
                        vertical = MaterialTheme.spacing.medium
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(onClick = onFinish) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(currentStep) {
                            var horizontalDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { horizontalDrag = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    horizontalDrag += dragAmount
                                },
                                onDragEnd = {
                                    if (horizontalDrag <= -80f && currentStep < steps.lastIndex) {
                                        currentStep++
                                    } else if (horizontalDrag >= 80f && currentStep > 0) {
                                        currentStep--
                                    }
                                }
                            )
                        }
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            val enter = if (targetState > initialState) {
                                slideInHorizontally(tween(300)) { it } + fadeIn(tween(180))
                            } else {
                                slideInHorizontally(tween(300)) { -it } + fadeIn(tween(180))
                            }
                            val exit = if (targetState > initialState) {
                                slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(180))
                            } else {
                                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(180))
                            }
                            ContentTransform(enter, exit, sizeTransform = null)
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "onboarding_step"
                    ) { animatedStep ->
                        val step = steps[animatedStep]
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                            Text(
                                text = stringResource(
                                    R.string.onboarding_step_counter,
                                    animatedStep + 1,
                                    steps.size
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                            Text(
                                text = stringResource(step.title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                            Text(
                                text = stringResource(step.description),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MaterialTheme.spacing.medium),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                        }
                    }

                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                        steps.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentStep) 10.dp else 8.dp)
                                    .background(
                                        color = if (index == currentStep) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.onboarding_previous))
                        }
                    }
                    Button(
                        onClick = {
                            if (currentStep == steps.lastIndex) {
                                onFinish()
                            } else {
                                currentStep++
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(
                                if (currentStep == steps.lastIndex) {
                                    R.string.onboarding_start
                                } else {
                                    R.string.onboarding_next
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
