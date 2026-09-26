package com.example.myapplication.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import com.example.myapplication.ui.theme.spacing

@Composable
fun RegisterStudyScreen(
    modifier: Modifier = Modifier,
    onRegisterStudy: () -> Unit = {}
) {
    var subject by remember {
        mutableStateOf("")
    }

    var notes by remember {
        mutableStateOf("")
    }

    var duration by remember {
        mutableStateOf("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Registrar estudo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.extraSmall)
        )

        Text(
            text = "Registre o que você estudou hoje.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.extraLarge)
        )

        Text(
            text = "O que você estudou?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        OutlinedTextField(
            value = subject,
            onValueChange = {
                subject = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Ex: Java Streams")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.large)
        )

        Text(
            text = "O que você aprendeu?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Escreva um breve resumo...")
            },
            minLines = 4
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.large)
        )

        Text(
            text = "Tempo de estudo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        OutlinedTextField(
            value = duration,
            onValueChange = {
                duration = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Ex: 90 minutos")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.extraLarge)
        )

        Button(
            onClick = onRegisterStudy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar estudo")
        }
    }
}
