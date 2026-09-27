# ReControle Mobile

Aplicativo Android do **ReControle**, projeto de conclusão do curso técnico da ETEC Jorge Street. A solução organiza ocorrências em laboratórios de TI: docentes registram um problema, acompanham o status e consultam o histórico pelo celular. Este repositório contém o **aplicativo móvel**; o [painel web](https://github.com/LukasPio/ReControle_Web) fica em outro repositório.

## O que o aplicativo faz

- Cadastro, login e verificação de e-mail com Firebase Authentication.
- Abertura de ocorrências com descrição, laboratório, categoria e foto.
- Lista de chamados, filtro por laboratório, detalhes e acompanhamento de status.
- Armazenamento dos chamados no Firebase Realtime Database e cache local com Room.
- Notificações locais sobre mudanças de status: um Worker consulta periodicamente os chamados do usuário e compara os estados. **Não se trata de Firebase Cloud Messaging ou entrega instantânea por push.**

## Minha contribuição

Fui responsável pelo aplicativo Android, incluindo telas em **Kotlin e Jetpack Compose**, integração com Firebase, captura e exibição de imagens, persistência local com **Room** e monitoramento de status com **WorkManager**. O projeto foi desenvolvido no contexto de um trabalho em equipe; o painel web é uma parte separada da solução.

## Tecnologias e organização

| Camada | Tecnologias e uso |
| --- | --- |
| Interface | Kotlin, Jetpack Compose, Material 3 e Navigation Compose |
| Autenticação e dados | Firebase Authentication e Realtime Database |
| Persistência no aparelho | Room para ocorrências e histórico de notificações |
| Tarefas em segundo plano | WorkManager para consultar mudanças de status |

O código principal está em `app/src/main/java/com/lucas/recontrole/`: `screens/` contém as telas, `logic/` as operações com ocorrências, `db/` e `dao/` o armazenamento local e `notification/` o monitoramento de status.

## Como executar

1. Instale o Android Studio e o Android SDK compatível com `compileSdk 35`. Abra a raiz do repositório no Android Studio e sincronize o Gradle.
2. Para testar sem acessar os dados do projeto original, crie **seu próprio projeto Firebase**, com Authentication por e-mail/senha e Realtime Database. Cadastre o aplicativo Android com o identificador `com.lucas.recontrole`, baixe um novo `google-services.json` e substitua o arquivo em `app/`.
3. Configure regras de acesso adequadas para o seu projeto Firebase. O código usa o nó `reports` no Realtime Database; criar uma conta de teste isolada evita depender de contas e dados reais.
4. Execute a configuração `app` em um emulador ou aparelho com Android 7.0 (API 24) ou superior. Conceda câmera e notificações quando solicitadas.

Também é possível compilar com `./gradlew assembleDebug` após configurar o SDK e o Firebase. O APK será gerado em `app/build/outputs/apk/debug/`.

## Fluxo para avaliar

Crie uma conta de teste, confirme o e-mail, faça login, registre um chamado com laboratório/categoria/descrição e observe a lista e os filtros. O acompanhamento de status depende de alterações no Realtime Database; o WorkManager executa verificações periódicas com rede disponível, sujeitas ao agendamento do Android.

> Projeto acadêmico. O comportamento real depende das regras e da configuração do Firebase usado no teste.
