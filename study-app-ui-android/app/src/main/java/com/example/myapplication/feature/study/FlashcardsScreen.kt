package com.example.myapplication.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface FlashcardLoadState {
    data object Loading : FlashcardLoadState
    data object Error : FlashcardLoadState
    data class Success(val cards: List<Flashcard>) : FlashcardLoadState
}

@Composable
fun FlashcardsScreen(
    subject: String,
    studies: List<StudyEntry>,
    flashcardGenerator: FlashcardGenerator,
    onBack: () -> Unit,
    onComplete: (ReviewDifficulty) -> ReviewSchedule,
    onFinish: () -> Unit
) {
    val request = remember(subject, studies) {
        FlashcardGenerationRequest(
            subject = subject,
            studySummaries = studies
                .sortedByDescending { it.date }
                .map { it.description.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        )
    }
    var retryToken by remember { mutableIntStateOf(0) }
    var loadState by remember {
        mutableStateOf<FlashcardLoadState>(FlashcardLoadState.Loading)
    }
    LaunchedEffect(request, retryToken, flashcardGenerator) {
        loadState = FlashcardLoadState.Loading
        val result = flashcardGenerator.generate(request)
        loadState = result.fold(
            onSuccess = { cards -> cards.toLoadState() },
            onFailure = { FlashcardLoadState.Error }
        )
    }

    var currentCardIndex by rememberSaveable(subject) { mutableIntStateOf(0) }
    var isAnswerRevealed by rememberSaveable(subject) { mutableStateOf(false) }
    var weakestDifficulty by rememberSaveable(subject) {
        mutableStateOf(ReviewDifficulty.EASY)
    }
    var completedSchedule by remember(subject) { mutableStateOf<ReviewSchedule?>(null) }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    }
    val cards = (loadState as? FlashcardLoadState.Success)?.cards.orEmpty()
    val selectedCardIndex = currentCardIndex.coerceAtMost(cards.lastIndex.coerceAtLeast(0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.flashcards_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = subject,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        FlashcardReviewContent(
            loadState = loadState,
            completedSchedule = completedSchedule,
            dateFormatter = dateFormatter,
            currentCardIndex = selectedCardIndex,
            isAnswerRevealed = isAnswerRevealed,
            onRetry = { retryToken += 1 },
            onRevealAnswer = { isAnswerRevealed = true },
            onRateCard = { difficulty ->
                val weakest = weakestDifficulty.moreDifficultThan(difficulty)

                if (selectedCardIndex == cards.lastIndex) {
                    completedSchedule = onComplete(weakest)
                } else {
                    weakestDifficulty = weakest
                    currentCardIndex = selectedCardIndex + 1
                    isAnswerRevealed = false
                }

            },
            onFinish = onFinish
        )
    }
}

@Composable
private fun FlashcardReviewContent(
    loadState: FlashcardLoadState,
    completedSchedule: ReviewSchedule?,
    dateFormatter: DateTimeFormatter,
    currentCardIndex: Int,
    isAnswerRevealed: Boolean,
    onRetry: () -> Unit,
    onRevealAnswer: () -> Unit,
    onRateCard: (ReviewDifficulty) -> Unit,
    onFinish: () -> Unit
) {
    when {
        completedSchedule != null -> CompletedReviewContent(
            schedule = completedSchedule,
            dateFormatter = dateFormatter,
            onFinish = onFinish
        )

        loadState is FlashcardLoadState.Success -> FlashcardSessionContent(
            cards = loadState.cards,
            currentCardIndex = currentCardIndex,
            isAnswerRevealed = isAnswerRevealed,
            onRevealAnswer = onRevealAnswer,
            onRateCard = onRateCard
        )

        loadState == FlashcardLoadState.Error -> FlashcardGenerationError(onRetry)
        loadState == FlashcardLoadState.Loading -> FlashcardGenerationLoading()
    }
}

@Composable
private fun CompletedReviewContent(
    schedule: ReviewSchedule,
    dateFormatter: DateTimeFormatter,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.flashcards_completed_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = stringResource(
                    R.string.flashcards_next_review,
                    schedule.dueDate.format(dateFormatter)
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.flashcards_finish))
            }
        }
    }
}

@Composable
private fun FlashcardGenerationLoading() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Text(
            text = stringResource(R.string.flashcards_generating),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FlashcardGenerationError(onRetry: () -> Unit) {
    Text(
        text = stringResource(R.string.flashcards_generation_error),
        color = MaterialTheme.colorScheme.error
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.flashcards_retry))
    }
}

@Composable
private fun FlashcardSessionContent(
    cards: List<Flashcard>,
    currentCardIndex: Int,
    isAnswerRevealed: Boolean,
    onRevealAnswer: () -> Unit,
    onRateCard: (ReviewDifficulty) -> Unit
) {
    val currentCard = cards[currentCardIndex]
    Text(
        text = stringResource(
            R.string.flashcard_progress,
            currentCardIndex + 1,
            cards.size
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    FlashcardCard(card = currentCard, isAnswerRevealed = isAnswerRevealed)
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

    if (isAnswerRevealed) {
        FlashcardDifficultyOptions(onRateCard)
    } else {
        Button(onClick = onRevealAnswer, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reveal_flashcard_answer))
        }
    }

}

@Composable
private fun FlashcardCard(card: Flashcard, isAnswerRevealed: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large)
        ) {
            Text(
                text = stringResource(R.string.flashcard_question_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = card.question,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isAnswerRevealed) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                Text(
                    text = stringResource(R.string.flashcard_answer_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = card.answer,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

        }
    }
}

@Composable
private fun FlashcardDifficultyOptions(
    onRateCard: (ReviewDifficulty) -> Unit
) {
    Text(
        text = stringResource(R.string.flashcard_difficulty_prompt),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DifficultyButton(
            label = stringResource(R.string.flashcard_again),
            modifier = Modifier.weight(1f),
            onClick = { onRateCard(ReviewDifficulty.AGAIN) }
        )
        DifficultyButton(
            label = stringResource(R.string.flashcard_hard),
            modifier = Modifier.weight(1f),
            onClick = { onRateCard(ReviewDifficulty.HARD) }
        )
        DifficultyButton(
            label = stringResource(R.string.flashcard_easy),
            modifier = Modifier.weight(1f),
            onClick = { onRateCard(ReviewDifficulty.EASY) }
        )
    }
}

@Composable
private fun DifficultyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun List<Flashcard>.toLoadState(): FlashcardLoadState =
    takeIf { it.isNotEmpty() }
        ?.let(FlashcardLoadState::Success)
        ?: FlashcardLoadState.Error

private fun ReviewDifficulty.moreDifficultThan(
    other: ReviewDifficulty
): ReviewDifficulty =
    if (ordinal < other.ordinal) this else other
