# Regras e funcionamento do Study App

Este documento descreve o comportamento implementado no aplicativo Android no estado atual do código. É uma referência das regras reais do produto, não uma especificação de funcionalidades futuras.

## 1. Visão geral

O Study App ajuda a organizar assuntos, executar sessões Pomodoro, registrar estudos, planejar estudos futuros, acompanhar revisões espaçadas e consultar o progresso.

- O aplicativo é Android nativo, construído com Kotlin e Jetpack Compose.
- A interface e os textos atuais estão em português.
- A navegação principal contém **Início**, **Calendário**, **Assuntos** e **Perfil**.
- Os dados são locais ao dispositivo, armazenados em `SharedPreferences`. Não há conta, API conectada, sincronização remota ou compartilhamento entre dispositivos implementado. A geração de flashcards usa atualmente um mock assíncrono por trás de `FlashcardGenerator`, pronto para ser substituído por uma implementação de API.
- A tela de onboarding aparece na primeira utilização. Sua conclusão ou opção **Pular** é salva localmente.
- Os dados de demonstração existem no código, mas estão desativados (`Data.ENABLED = false`).

## 2. Onboarding e navegação

O onboarding apresenta cinco temas: organização dos assuntos, foco com Pomodoro, revisões, planejamento e acompanhamento de progresso. É possível avançar, voltar, deslizar entre etapas ou pular. Ao terminar, o aplicativo salva a conclusão.

Na primeira abertura após concluir o onboarding, o Android pode solicitar permissão para notificações (Android 13 ou superior). A navegação inferior permite alternar entre as quatro telas principais; o detalhe de um assunto mantém **Assuntos** selecionado.

Além das telas principais, os fluxos de navegação incluem iniciar estudo, registrar estudo realizado, agendar estudo e executar/retomar uma sessão Pomodoro.

## 3. Assuntos e registros de estudo

Um assunto é identificado pelo seu nome. Nas buscas do repositório, a comparação é indiferente a maiúsculas/minúsculas; o nome digitado nos formulários é salvo sem espaços nas extremidades. O histórico é formado pelos registros de estudo, não por um cadastro separado de assuntos.

### Registrar um estudo já realizado

O registro manual exige:

- assunto não vazio;
- resumo não vazio;
- data igual ou anterior a hoje;
- número de sessões inteiro maior que zero;
- duração total de 1 a 1.440 minutos.

A data inicial é hoje, mas pode ser alterada para qualquer data passada ou hoje. Ao salvar, o registro entra no histórico e inicia uma revisão para o dia seguinte à data informada. O registro manual não é tratado como conclusão de uma revisão; se já havia calendário de revisão para aquele assunto, o próximo registro manual reinicia o ciclo no primeiro intervalo.

### Histórico por assunto

A tela **Assuntos** mostra ações para iniciar, registrar ou agendar um estudo e apresenta o histórico agrupado por:

- **Dias**: cada data;
- **Semanas**: segunda-feira a domingo;
- **Mês**: mês e ano.

Os grupos aparecem do mais recente para o mais antigo; os assuntos dentro de cada grupo são ordenados alfabeticamente. A lista carrega mais itens conforme o usuário rola, em lotes de 20. Cada linha soma as sessões e os minutos dos registros daquele assunto no período.

O detalhe do assunto mostra os registros do mais recente para o mais antigo, com data, descrição, sessões e duração. Um registro pode ser apagado após confirmação. A exclusão é permanente no histórico local. Se era o último registro daquele assunto, o calendário de revisão do assunto também é removido; se ainda houver outros registros, a revisão é mantida.

## 4. Pomodoro

Uma sessão Pomodoro possui um assunto, duração de estudo, duração de intervalo e quantidade de sessões. Os valores padrão são **25 minutos**, **5 minutos** e **4 sessões**.

### Validação e execução

- As durações de estudo e intervalo devem ser números inteiros de 1 a 1.440 minutos.
- A quantidade de sessões deve ser um inteiro maior que zero.
- Não há limite superior definido para a quantidade de sessões.
- A configuração fica bloqueada enquanto há uma sessão ativa ou pausada. **Resetar** descarta a sessão e libera a configuração.
- Só pode haver uma sessão Pomodoro global por vez. As ações para iniciar estudo ficam indisponíveis enquanto outra sessão estiver ativa; se uma navegação tentar iniciar outra, o aplicativo abre a sessão já existente.
- A contagem usa o relógio monotônico do Android e continua sendo gerida por um serviço em primeiro plano, inclusive quando a tela do app não está aberta.
- O usuário pode pausar, retomar, resetar ou sair da tela. Sair da tela não encerra a sessão.
- Ao mudar entre estudo e intervalo, o dispositivo vibra. O temporizador também é apresentado em uma notificação contínua.

### Ciclo e conclusão

Cada sessão de estudo concluída soma sua duração e uma sessão ao total. Entre sessões há um intervalo; depois da última sessão não é iniciado um intervalo final. Ao concluir o ciclo, o aplicativo mostra os minutos estudados, permite adicionar outra sequência de sessões e oferece um resumo opcional.

**Adicionar mais sessões** inicia uma nova sequência usando as durações e a quantidade configuradas, preservando os minutos e sessões já concluídos no total da sessão. Para registrar o estudo, o usuário deve pressionar **Finalizar estudo**. O resumo pode ficar vazio e é salvo sem texto nesse caso. A finalização registra a data de hoje, duração total concluída, quantidade de sessões e resumo; a sessão só pode ser finalizada uma vez.

Resetar antes de finalizar descarta o progresso ainda não registrado. Estudos parcialmente concluídos não são salvos automaticamente no histórico.

### Sessão retomada e origem

O estado do Pomodoro (inclusive fase, tempo restante, totais acumulados e origem) é salvo separadamente dos registros de estudo e restaurado ao reabrir o aplicativo.

- Um estudo livre não é marcado como revisão.
- Uma revisão iniciada pelo plano da tela **Início** é marcada como revisão.
- Um estudo iniciado a partir de um agendamento conserva o identificador do agendamento. Quando concluído e registrado, esse agendamento é removido. Resetar ou deixar de finalizar não o remove.

## 5. Revisão espaçada

Cada assunto com estudo registrado tem, no máximo, um calendário de revisão. A primeira revisão vence **1 dia após a data do registro**. Quando uma revisão é concluída em um Pomodoro iniciado pelo plano de revisões do **Início**, o próximo vencimento é calculado a partir da data do novo registro:

| Etapa | Intervalo após o estudo/revisão registrada |
|---|---:|
| Primeira revisão | 1 dia |
| Segunda revisão concluída | 7 dias |
| Terceira revisão concluída | 21 dias |
| Revisões seguintes | 60 dias |

O intervalo de 60 dias se repete indefinidamente. Ao concluir uma revisão, o aplicativo avança no intervalo da agenda existente. Para um assunto sem agenda anterior, ou para um registro não marcado como revisão, a agenda volta ao intervalo inicial de 1 dia.

Uma revisão está pendente quando sua data é hoje ou anterior. Datas anteriores a hoje são exibidas como atrasadas; revisões vencendo hoje são exibidas como revisão do dia. O plano da tela **Início** ordena as revisões pela data de vencimento.

Ao iniciar uma revisão pelo **Início**, o usuário escolhe entre Pomodoro (25/5 minutos e 4 sessões) e flashcards. O fluxo de flashcards envia o assunto e os resumos não vazios do histórico para a interface assíncrona `FlashcardGenerator`; a implementação atual gera perguntas mockadas. O contrato recebe `FlashcardGenerationRequest` e devolve uma lista de `Flashcard`, permitindo substituir o mock por um cliente de API sem alterar a tela. A interface trata carregamento, falha e nova tentativa.

Cada cartão mostra primeiro a pergunta; depois de revelar a resposta, o usuário avalia com **Errei**, **Difícil** ou **Fácil**. A avaliação mais difícil do ciclo define o próximo intervalo: **Errei** reinicia em 1 dia, **Difícil** mantém o intervalo atual e **Fácil** avança para o próximo (1, 7, 21 ou 60 dias). A conclusão atualiza o calendário sem criar um registro de estudo fictício.

No **Calendário**, uma revisão pode ser reagendada para hoje ou uma data futura. Reagendar altera a data de vencimento, sem alterar o índice do intervalo.

## 6. Agendamentos de estudo

Um agendamento contém data, assunto, quantidade de sessões, minutos de estudo por sessão e minutos de intervalo. Os padrões são 25/5 minutos e 4 sessões.

- A data deve ser hoje ou futura.
- O assunto deve ser preenchido; pode ser digitado ou escolhido entre os assuntos encontrados no histórico e nos agendamentos existentes.
- Estudo e intervalo devem ser inteiros de 1 a 1.440 minutos; a quantidade de sessões deve ser maior que zero.
- Não há limite superior definido para a quantidade de sessões.
- É possível criar mais de um agendamento para o mesmo assunto ou dia.

Os agendamentos ficam visíveis no **Calendário**. Na tela **Início**, aparecem os de hoje ou de datas futuras, ordenados por data e assunto. A notificação diária considera agendamentos com data de hoje ou anterior. Um agendamento pode ser cancelado no Calendário após confirmação.

Iniciar um estudo agendado carrega sua configuração no Pomodoro e oculta esse item do plano enquanto a sessão associada está ativa. Ele só é removido definitivamente após concluir e registrar a sessão.

## 7. Calendário

O Calendário abre no mês atual e seleciona hoje. É possível navegar entre meses pelos botões ou por gesto horizontal; ao trocar de mês, o dia selecionado é mantido quando possível, limitado ao último dia do mês. Não há restrição de navegação para meses passados ou futuros.

Os dias podem conter registros de estudo, agendamentos e revisões. Selecionar um dia apresenta todos os registros, agendamentos e revisões daquela data. Registros podem ser apagados após confirmação; agendamentos podem ser cancelados após confirmação; revisões podem ser reagendadas para hoje ou depois.

Quando há vários itens em um dia, a grade do mês mostra marcadores resumidos; o painel do dia selecionado apresenta os itens. Um contorno destaca hoje e outro destaca a seleção.

## 8. Início e indicadores de progresso

O plano da tela **Início** pode combinar sessão Pomodoro ativa, revisões vencidas e agendamentos de hoje ou futuros. A sessão ativa é selecionada prioritariamente. Caso contrário, o usuário seleciona um item do plano para continuar/iniciar; sem itens, o botão fica desativado.

O resumo apresenta:

- **Dias seguidos**: sequência de datas com pelo menos um registro, contando a partir de hoje se houve estudo hoje ou, na ausência dele, a partir de ontem. Se nenhuma dessas datas tiver estudo, o valor é zero.
- **Este mês**: soma dos minutos registrados no mês e ano atuais, convertida para horas inteiras por divisão por 60 (a fração de hora é descartada).
- **Assuntos**: quantidade de nomes distintos, ignorando diferenças entre maiúsculas e minúsculas.

Se não houver assuntos registrados, o resumo exibe o estado vazio.

## 9. Perfil e atividade anual

O Perfil apresenta um calendário anual de atividade. Cada dia soma a quantidade de sessões informada nos registros daquele dia e ano — não o número de registros nem os minutos.

- A intensidade indica zero, 1, 2–4 ou 5 ou mais sessões.
- O ano atual é exibido inicialmente; é possível consultar anos anteriores.
- Não é possível avançar para um ano futuro.
- O total anual corresponde à soma de sessões registradas naquele ano.

## 10. Lembretes e notificações

O aplicativo agenda um lembrete diário para **07:00**, usando o alarme do Android. Ele é reagendado para o dia seguinte após ser disparado e após reinicialização do aparelho, alteração do horário ou mudança de fuso horário.

O lembrete lista revisões com vencimento hoje ou anterior e agendamentos com data hoje ou anterior. Se não houver nenhum item, a notificação existente é cancelada. No Android 13 ou superior, sem permissão de notificações, o alarme ainda é reagendado, mas a notificação não é exibida.

O cronômetro Pomodoro usa uma notificação contínua separada, que indica a fase e o estado/contagem da sessão. Os lembretes são alarmes do Android não configurados como alarmes exatos; portanto, o horário de entrega pode depender das políticas de energia do sistema.

## 11. Persistência e limites atuais

Os estudos, agendamentos, revisões e identificadores de registros removidos são gravados localmente em `SharedPreferences`, com listas serializadas em JSON. O estado Pomodoro usa outro arquivo local de preferências. A conclusão do onboarding também fica salva no armazenamento de preferências do app.

Não há sincronização remota nem recuperação de dados por conta. Remover os dados do aplicativo ou desinstalá-lo pode apagar as informações locais, conforme as regras de backup do Android. As rotas/endpoints citados nos comentários da interface `StudyRepository` são referências para um backend futuro; não representam comunicação de rede disponível atualmente.

## 12. Configuração técnica do aplicativo

- `applicationId`: `com.example.myapplication`
- Versão declarada: `1.0` (`versionCode` 1)
- Android mínimo: API 26
- SDK alvo e de compilação: API 37
- Interface: Jetpack Compose com Material 3
- Permissões declaradas: vibração, notificações, recebimento de inicialização do dispositivo, serviço em primeiro plano e tipo especial de serviço em primeiro plano.
