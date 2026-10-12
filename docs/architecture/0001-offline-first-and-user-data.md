# ADR 0001: Dados offline-first e isolamento por conta

- **Status:** Implementado (limitações operacionais abaixo)
- **Data:** 2026-10-12

## Contexto

O app Android já funcionava sem rede e mantinha estudos, agendamentos, revisões
e a sessão Pomodoro no dispositivo. O backend REST existia, mas não possuía
autenticação ou isolamento de dados por usuário.

Uma arquitetura exclusivamente dependente do servidor interromperia os fluxos
principais sem conexão. Manter apenas dados locais, por outro lado, não permite
recuperá-los após troca ou perda do dispositivo, nem sincronizá-los entre
dispositivos.

## Decisão

O Study App adota uma fase **local-first sem conta por 30 dias** desde o
primeiro uso e, após o vínculo, uma arquitetura **offline-first com
sincronização por conta**:

- Durante a fase inicial, o banco local do Android é a única fonte de dados. O
  app não envia registros nem o identificador do dispositivo ao backend.
- A fase inicial termina 30 dias corridos após o primeiro uso. Antes do prazo,
  criar uma conta ou entrar vincula todos os registros da partição local deste
  aparelho à conta autenticada e inicia a sincronização. Depois do prazo, a
  autenticação é obrigatória; dados que não foram vinculados são apagados.
- O identificador local é o Android ID (escopado a app/assinatura/usuário), com
  UUID aleatório como fallback. Não se solicita IMEI nem permissão de telefone;
  o identificador é usado somente para particionar dados localmente e não é
  enviado ao servidor.
- Após o vínculo, o banco local continua sendo a fonte imediata de leitura e
  escrita da interface. As telas continuam funcionando sem conexão.
- O backend será o registro persistente compartilhado da conta e o ponto de
  sincronização entre dispositivos. Depois de sincronizados, os dados deverão
  pertencer a uma única conta.
- A primeira versão multiusuário não terá compartilhamento de dados entre
  contas. Estudos, agendamentos e revisões serão privados da conta proprietária.
- O identificador da conta será obtido pelo backend a partir da identidade
  autenticada. O cliente não poderá escolher ou sobrescrever o proprietário
  enviando um `userId`.
- Os registros sincronizáveis têm identificadores estáveis para que uma nova
  tentativa de envio não crie duplicatas. Identificadores de revisão são
  determinísticos por conta e assunto, preservando uma revisão por assunto em
  dispositivos que começaram offline.
- O Android usará uma fila local de alterações pendentes e sincronização
  incremental. A interface não aguardará chamadas de rede para refletir uma
  alteração local após a vinculação. Na fase de avaliação, nenhuma fila é
  enviada até o usuário autenticar.
- Exclusões também serão sincronizadas; não poderão ser representadas apenas
  removendo o item localmente, pois uma cópia antiga em outro dispositivo
  poderia restaurá-lo.

## Consequências

- Android migra os estudos, agendamentos e revisões JSON existentes para SQLite,
  preservando dados locais. Durante o trial, cada instalação usa sua partição;
  ao autenticar, os registros atuais são transferidos atomicamente para a conta
  e a fila é reconstruída a partir do estado local mais recente.
- A limpeza local é agendada para o prazo com WorkManager e também executada
  quando o app abre depois do vencimento. Ela remove estudos, agendamentos,
  revisões, operações pendentes e a sessão Pomodoro ainda não vinculados.
- Ao autenticar, a sessão Pomodoro em andamento também passa para a partição
  local da conta. Ela não é enviada ao backend nem sincronizada entre aparelhos:
  o cronômetro usa o relógio monotônico do dispositivo e representa estado
  transitório, não um histórico de estudos.
- A duração é medida pelo relógio local e persistida em preferências sem backup.
  Sem um serviço remoto não é possível garantir resistência a reinstalação,
  limpeza dos dados ou manipulação do relógio; o trial é uma regra de produto,
  não um mecanismo antifraude.
- O backend exige JWT para as rotas de dados e associa consultas e mutações à
  conta autenticada. O segredo JWT vem de `JWT_SECRET`; o access token expira em
  15 minutos e o refresh token rotativo e opaco dura 30 dias.
- Cadastro exige verificação do endereço de e-mail antes de liberar o acesso.
  Recuperação de senha e verificação usam códigos aleatórios de seis dígitos,
  com expiração de 15 minutos, hash BCrypt, limite de tentativas e cooldown de
  envio; SMTP é configurado por variáveis de ambiente. Contas existentes antes
  dessa mudança precisam verificar o endereço antes de voltar a entrar.
- O protocolo usa `/api/sync`, eventos idempotentes, versões por registro,
  cursor incremental e tombstones. Conflitos retornam HTTP 409 e são resolvidos
  com escolha explícita entre os payloads do servidor e do dispositivo.
- Chamadas REST existentes também publicam eventos no mesmo log de
  sincronização, para manter a leitura incremental consistente.
- Registros antigos do H2 com `user_id IS NULL` são removidos na inicialização,
  por autorização explícita do produto para descartar esses dados. A antiga
  restrição global de unicidade do assunto nas revisões também é removida para
  garantir unicidade no escopo adequado. Essa limpeza é irreversível; faça
  backup antes da atualização caso os dados ainda sejam necessários. Essa
  decisão cobre os registros órfãos do backend; o JSON local do Android continua
  sendo importado para SQLite, pois pertence ao dispositivo e pode ser
  reivindicado pela conta autenticada.
- Dados no SQLite do dispositivo pertencem à primeira conta autenticada quando
  ainda não há proprietário local; o vínculo atual transfere os registros da
  instalação em uso, não agrega automaticamente dados de outros dispositivos.
  Compartilhamento entre contas no mesmo aparelho não é oferecido.

## Alternativas consideradas

### Backend como única fonte de dados

Rejeitada para a experiência do app porque tornaria os fluxos principais
dependentes de conexão e exigiria substituir o comportamento local funcional.

### Dados exclusivamente locais

Rejeitada como arquitetura de produto porque não oferece recuperação nem
sincronização entre dispositivos.

### Compartilhamento de dados entre usuários

Adiado. Não é necessário para a primeira versão e amplia o modelo de autorização
e privacidade antes de existir um caso de uso definido.

## Racional das decisões

- **Offline-first, com o backend como registro sincronizado da conta:** mantém
  os fluxos essenciais disponíveis sem rede, sem abrir mão da recuperação e do
  uso em mais de um dispositivo.
- **Trial local sem conta:** permite experimentar o app sem cadastro e evita
  enviar dados pessoais antes de o usuário optar por criar/acessar uma conta.
  O SQLite já é necessário para uso offline e serve como origem única durante
  esse período; o fluxo de vínculo transfere snapshots atuais, sem reproduzir
  no servidor todo o histórico de operações temporárias.
- **Prazo de 30 dias e limpeza local:** aplica o limite solicitado sem manter
  dados abandonados indefinidamente. A limpeza é local porque, antes da conta,
  o servidor deliberadamente não conhece a instalação nem seus dados.
- **Android ID em vez de IMEI:** IMEI é um identificador restrito e excessivo
  para um app de estudos. O ID escopado do Android não exige permissão de
  telefone e não precisa sair do aparelho; o UUID é fallback para ambientes em
  que o valor do sistema não esteja disponível.
- **Não transmitir o Device ID:** o backend não precisa conhecer o identificador
  para vincular dados; o app move a partição local e sincroniza registros com
  identidade da conta. Isso evita criar rastreamento permanente entre serviços.
- **Armazenamento sem backup durante o trial:** evita restaurar em outro
  dispositivo registros associados ao identificador local anterior. Após a
  conta, os dados podem ser recuperados pela sincronização autenticada.
- **Isolamento por conta definido no servidor:** evita confiar em um `userId`
  controlado pelo cliente e impede que uma conta leia ou altere dados de outra.
- **Identificadores estáveis, versões, cursor e tombstones:** permitem repetir
  envios sem duplicar registros, baixar somente mudanças novas e propagar
  exclusões sem ressuscitar dados antigos.
- **Escolha explícita em conflitos:** não há base de produto para decidir
  automaticamente qual dispositivo tem a versão correta; pedir uma escolha
  evita descarte silencioso de alterações.
- **Access token curto e refresh token opaco rotativo:** reduz o tempo útil de
  um access token exposto; guardar somente o hash do refresh token no servidor
  permite revogação e impede reutilização do token anterior.
- **SQLiteOpenHelper no Android e JDBC/H2 no backend:** preserva a stack já
  presente no app e na API sem acrescentar uma camada ORM; SQLite oferece
  transações locais e consultas por conta, e JDBC reutiliza as consultas
  account-scoped existentes. A troca do H2 por banco de produção continua sendo
  decisão operacional separada.
- **Códigos de verificação/recuperação de uso único:** são simples de digitar no
  app sem depender de links profundos, têm validade curta e são armazenados
  somente como hash para limitar o impacto de vazamento do banco.
- **SMTP configurável, sem mecanismo de bypass:** permite usar provedores
  diferentes sem acoplar o domínio a um fornecedor; falhas de entrega são
  explícitas, pois não é seguro ativar contas ou redefinir senhas sem provar
  acesso ao e-mail.
- **Descartar órfãos legados do backend:** esses registros não têm vínculo
  confiável com uma conta; atribuí-los ao primeiro usuário poderia expor dados
  de outra pessoa, enquanto uma migração manual não foi solicitada. A remoção
  foi autorizada pelo produto.
- **Pomodoro local, associado à partição da instalação ou da conta:** mantém a
  sessão retomável após autenticação e isolada ao sair da conta, sem tentar
  reproduzir em outro aparelho um prazo baseado no relógio monotônico local.
