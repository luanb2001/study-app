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
| `POST` | `/api/auth/register` | Cria conta pendente e envia código de verificação |
| `POST` | `/api/auth/login` | Autentica e retorna token de acesso |
| `POST` | `/api/auth/verify-email` | Verifica e-mail com código de uso único e cria sessão |
| `POST` | `/api/auth/resend-verification` | Reenvia código de verificação |
| `POST` | `/api/auth/password/forgot` | Solicita código de recuperação sem revelar se a conta existe |
| `POST` | `/api/auth/password/reset` | Redefine senha com código de uso único |
| `POST` | `/api/auth/refresh` | Rotaciona o refresh token e retorna nova sessão |
| `POST` | `/api/auth/logout` | Revoga o refresh token da sessão |
| `POST` | `/api/sync` | Envia alterações pendentes e recebe alterações desde o cursor |

`POST /api/flashcards/generate` recebe `{"subject":"Java","studySummaries":["Resumo"]}`.
O gerador atual é determinístico/de demonstração e deve ser substituído por integração
com um serviço de geração antes de ser usado como IA.

Os vencimentos são 1, 7, 21 e 60 dias. Para flashcards, `AGAIN` reinicia em 1 dia,
`HARD` mantém o intervalo e `EASY` avança de etapa; a conclusão não cria registro de
estudo fictício. Assuntos são comparados sem diferenciar maiúsculas/minúsculas.

Todas as rotas, exceto `/api/auth/**`, exigem `Authorization: Bearer <token>`.
Senhas têm no mínimo 8 caracteres e são armazenadas com BCrypt. O access token
JWT expira em 15 minutos. O refresh token opaco dura 30 dias, é armazenado como
hash SHA-256, rotacionado a cada uso e revogado ao redefinir a senha ou sair.
Perder o refresh token exige autenticação novamente.
Contas criadas antes da implantação deste fluxo ficam não verificadas até o
usuário pedir um novo código e concluir a verificação.

O segredo JWT é obrigatório. Gere um valor aleatório com pelo menos 32 bytes e
configure-o em `JWT_SECRET` antes de iniciar o backend. O H2 é armazenado em
`backend/data/`, ignorado pelo Git. Configure JDBC usando `DATABASE_URL`,
`DATABASE_USERNAME` e `DATABASE_PASSWORD`.

Verificação de e-mail e recuperação usam códigos numéricos de seis dígitos,
armazenados como hashes BCrypt, válidos por 15 minutos e limitados a cinco
tentativas. Há um intervalo mínimo de 60 segundos entre emissões do mesmo tipo
de código por conta. Configure entrega SMTP com `SMTP_HOST`, `SMTP_PORT`,
`SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS` e `SMTP_FROM`.
Sem SMTP funcional, cadastro e solicitação de códigos falham explicitamente;
não há código de bypass ou exposição do código pela API. Use credenciais de
aplicação SMTP e TLS em produção.

O endpoint `/api/sync` usa versões por registro, alterações idempotentes,
cursor incremental e tombstones para exclusões. Uma divergência de versão
retorna HTTP 409 com o estado atual do servidor; o app Android apresenta os
dois estados para o usuário escolher. O cliente não envia o identificador do
proprietário: o backend deriva a conta do token.

Na inicialização, os registros legados sem proprietário (`user_id IS NULL`) em
estudos, agendamentos e revisões são removidos. A restrição global antiga de
assunto único das revisões também é descartada para permitir o isolamento por
conta. Faça backup antes de atualizar qualquer instalação que ainda precise
desses dados: a remoção é intencional e irreversível.

## Direção arquitetural

O backend recebe dados depois que o usuário vincula uma conta ao aparelho. O
período inicial de 30 dias é local, sem envio de registros ou do identificador
do dispositivo. Após o vínculo, o backend é o registro persistente
compartilhado entre dispositivos; a identidade do proprietário vem da
autenticação, nunca de um `userId` informado pelo cliente. Veja o
[ADR 0001](../docs/architecture/0001-offline-first-and-user-data.md).

## Camadas

Os controllers traduzem HTTP para chamadas de serviço. Os services coordenam
casos de uso, regras de negócio e transações; não executam SQL nem acessam
`JdbcTemplate` diretamente. A persistência JDBC e o mapeamento das consultas
ficam nos repositories do pacote `repository`. Ao adicionar acesso a dados,
crie ou estenda um repository e injete-o no service responsável.

## Convenções de código

- Separe blocos de controle (`if`, `for`, `do`/`while` e `switch`) e pipelines
  de `Stream` com uma linha em branco antes e depois do bloco ou da expressão
  completa. Preserve juntas expressões condicionais e chamadas encadeadas.
- Extraia métodos privados para nomear etapas não triviais e reduzir repetição.
  Crie classes utilitárias sem estado compartilhado quando isso deixar a
  responsabilidade mais clara.
- Evite classes internas; mantenha-as apenas quando forem pequenas e fizerem
  sentido exclusivo dentro da classe externa.
- Nomeie dependências pelo tipo: `studyDataRepository`,
  `authenticationRepository`, `studyService`. Evite nomes genéricos como
  `repository` ou `service`.
- Use `this` para acessar campos da própria instância e nas chamadas a métodos
  privados. Use `super` para acessar membros herdados e executar a
  implementação do pai quando ela também for necessária.
- Em argumentos padrão do construtor, use os próprios parâmetros: a instância
  ainda não está disponível durante essa avaliação.
