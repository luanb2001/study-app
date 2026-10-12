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

O app Android pode ser usado sem conta durante 30 dias a partir do primeiro
uso. Nesse período, estudos, agendamentos e revisões ficam somente no SQLite
local, sem envio ao backend. Antes do prazo, criar ou acessar uma conta vincula
os registros desse aparelho à conta e inicia a sincronização. Ao fim dos 30
dias, a conta passa a ser obrigatória e os dados locais ainda não vinculados
são apagados. A sessão Pomodoro também é vinculada à partição local da conta,
mas permanece específica deste aparelho e não é sincronizada. O cadastro
verifica o e-mail; a conta oferece recuperação de senha e renovação automática
de sessão.

## Direção arquitetural

A arquitetura é local-first durante o período de avaliação e offline-first
depois do vínculo: o backend torna-se o registro sincronizado privado da conta.
O app usa um identificador local do Android, sem solicitar IMEI nem transmitir
esse identificador ao servidor. As decisões e razões estão descritas no
[ADR 0001](./docs/architecture/0001-offline-first-and-user-data.md).
