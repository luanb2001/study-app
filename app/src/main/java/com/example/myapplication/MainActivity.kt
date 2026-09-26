package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.spacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.feature.study.RegisterStudyScreen
import com.example.myapplication.navigation.AppNavigation
import com.example.myapplication.navigation.Routes

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                StudyApp()
            }
        }
    }
}

@Composable
fun StudyApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigation()
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onRegisterStudy = {
                        navController.navigate(Routes.REGISTER_STUDY)
                    }
                )
            }

            composable(Routes.REGISTER_STUDY) {
                RegisterStudyScreen()
            }
        }
    }
}
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onRegisterStudy: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        // Saudação
        Text(
            text = "Olá! User",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Text(
            text = "Veja o que você precisa estudar hoje.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        // Revisões de hoje
        TodayReviews()

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        // Registrar estudo
        RegisterStudyCard(onRegisterStudy = onRegisterStudy)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        // Resumo do progresso
        ProgressSummary()

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    }
}

@Composable
fun TodayReviews() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Revisões de hoje",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        ReviewItem(
            subject = "Java Streams",
            status = "Revisar hoje"
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        ReviewItem(
            subject = "RabbitMQ",
            status = "Revisar hoje"
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        ReviewItem(
            subject = "Spring Transactions",
            status = "Atrasado"
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.large)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    // TODO: iniciar revisão
                }
            ) {
                Text("Começar revisão")
            }
        }
    }
}

@Composable
fun ReviewItem(
    subject: String,
    status: String
) {
    val isOverdue = status == "Atrasado"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador visual do status
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

            Spacer(
                modifier = Modifier.width(MaterialTheme.spacing.medium)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier = Modifier.height(MaterialTheme.spacing.extraSmall)
                )

                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isOverdue) {
                        FontWeight.Medium
                    } else {
                        FontWeight.Normal
                    },
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
fun RegisterStudyCard(
    onRegisterStudy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        ) {
            Text(
                text = "Estudou alguma coisa hoje?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(MaterialTheme.spacing.small)
            )

            Text(
                text = "Registre o que você estudou e deixe o app cuidar das próximas revisões.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(MaterialTheme.spacing.medium)
            )

            Button(
                onClick = onRegisterStudy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrar estudo")
            }
        }
    }
}

@Composable
fun ProgressSummary() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Seu progresso",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.medium)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                MaterialTheme.spacing.small
            )
        ) {
            ProgressItem(
                value = "12",
                label = "dias seguidos",
                modifier = Modifier.weight(1f)
            )

            ProgressItem(
                value = "18h",
                label = "este mês",
                modifier = Modifier.weight(1f)
            )

            ProgressItem(
                value = "27",
                label = "assuntos",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ProgressItem(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
    ) {
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

            Spacer(
                modifier = Modifier.height(MaterialTheme.spacing.extraSmall)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BottomNavigation() {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {
                // TODO: navegar para Hoje
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Hoje"
                )
            },
            label = {
                Text("Hoje")
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = {
                // TODO: navegar para Calendário
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Calendário"
                )
            },
            label = {
                Text("Calendário")
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = {
                // TODO: navegar para Assuntos
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Assuntos"
                )
            },
            label = {
                Text("Assuntos")
            }
        )

        NavigationBarItem(
            selected = false,
            onClick = {
                // TODO: navegar para Perfil
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil"
                )
            },
            label = {
                Text("Perfil")
            }
        )
    }
}
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme {
        StudyApp()
    }
}