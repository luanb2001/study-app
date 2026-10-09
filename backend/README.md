# Study App Backend

API REST independente para o app Study App, implementada com **Java 25 LTS**,
Spring Boot 3.5, JDBC e banco H2.

## Requisitos e execução

- JDK 25
- Maven 3.8+

```bash
cd backend
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8080`. No emulador Android, use
`http://10.0.2.2:8080`. Execute os testes com `mvn test`.

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/studies` | Lista registros de estudo |
| `GET` | `/api/subjects/{subject}/studies` | Histórico de um assunto |
| `POST` | `/api/studies` | Registra estudo (`?isReview=true` conclui revisão Pomodoro) |
| `DELETE` | `/api/studies/{studyId}` | Exclui registro e a revisão se não houver mais estudos do assunto |
| `GET` | `/api/studies/scheduled` | Lista agendamentos |
| `POST` | `/api/studies/scheduled` | Cria agendamento |
| `DELETE` | `/api/studies/scheduled/{scheduledStudyId}` | Cancela agendamento |
| `GET` | `/api/studies/summary` | Sequência de dias, horas do mês e assuntos |
| `GET` | `/api/reviews?dueOnOrBefore=AAAA-MM-DD` | Lista revisões, opcionalmente vencidas até a data |
| `PUT` | `/api/reviews/{reviewId}` | Reagenda revisão para hoje ou depois |
| `POST` | `/api/reviews/{reviewId}/complete` | Conclui revisão Pomodoro e registra estudo |
| `POST` | `/api/flashcards/generate` | Gera cartões com base nos resumos fornecidos |
| `POST` | `/api/reviews/{reviewId}/flashcards/complete` | Atualiza revisão com dificuldade `AGAIN`, `HARD` ou `EASY` |

`POST /api/flashcards/generate` recebe `{"subject":"Java","studySummaries":["Resumo"]}`.
O gerador atual é determinístico/de demonstração e deve ser substituído por integração
com um serviço de geração antes de ser usado como IA.

Os vencimentos são 1, 7, 21 e 60 dias. Para flashcards, `AGAIN` reinicia em 1 dia,
`HARD` mantém o intervalo e `EASY` avança de etapa; a conclusão não cria registro de
estudo fictício. Assuntos são comparados sem diferenciar maiúsculas/minúsculas.

O H2 é armazenado em `backend/data/`, ignorado pelo Git. Configure JDBC usando
`DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`. A API ainda não possui
autenticação nem sincronização multiusuário; não a exponha publicamente sem esses controles.
