# Study App

Este repositório contém os projetos do Study App:

- [Backend](./backend/README.md): API REST em Java 25 e Spring Boot.
- [App Android](./study-app-ui-android/APP_RULES.md): aplicativo Kotlin com Jetpack Compose.

Execute cada projeto a partir da pasta correspondente:

```bash
cd backend
mvn spring-boot:run
```

```bash
cd study-app-ui-android
./gradlew test
```

O app Android ainda usa armazenamento local; a integração HTTP está pendente.
