<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  🇵🇹 <b>Português</b>
</p>

### Cliente móvel de operador para o ARMOR-SERVER: alarmes, câmaras, radar, energia, a rede e a configuração dos nós, em oito idiomas

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 or later">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-376%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.8-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Verificação de honestidade - o que funciona hoje:** As regras de segurança do endereço, de decisão de alarmes, do vigia de alarmes, de tradução e do papel só de leitura estão cobertas por 376 testes unitários e a aplicação compila (`gradlew testDebugUnitTest assembleDebug`). Funciona num telemóvel real contra a bancada de testes do CM5: o acesso, o monitor de câmaras, o armar e os alarmes, o radar, a configuração de um nó por Bluetooth e as atualizações a partir do GitHub estão em uso. O que **ainda não** está provado: os ecrãs solar, elétrico e de centrais de alarme com equipamentos reais (só existe um nó radar), o push por Firebase (está na aplicação e precisa de um projeto Firebase próprio; ainda não foi experimentado num telemóvel) e a vigilância de alarmes em segundo plano sob as regras de bateria de cada fabricante.

---

## 📋 Num relance

| | |
|---|---|
| Versão | `0.7.8` |
| Maturidade | Estabelecido |
| Função | Cliente de operador Android para o ARMOR-SERVER (família: Clients, pai: ARMOR-SERVER) |
| Tecnologia | Kotlin / Jetpack Compose, Android 8.0+ |
| Testes | 376 (`gradlew testDebugUnitTest`) |
| Línguas | 8 (1345 frases por língua, cada frase verificada em todas as tabelas) |
| Funciona em | Um telemóvel com Android 8.0+ (minSdk 26) |
| Em hardware real | Usada num telemóvel real contra a bancada de testes do CM5; os ecrãs solar, elétrico e de centrais de alarme esperam nós reais |
| Licença | GPL-3.0-or-later |

**Ir para:** [Visão geral](#-visão-geral) · [Arquitetura](#-arquitetura) · [Segurança](#-segurança-e-proteção) · [Interfaces](#-interfaces) · [Configuração](#-configuração) · [Início rápido](#-início-rápido) · [Testes](#-testes) · [Problemas](#-resolução-de-problemas) · [Estrutura](#-estrutura-do-repositório) · [Relacionados](#-projetos-relacionados)

## 🚦 Estado do projeto

Cada linha é uma parte deste repositório e a luz diz até onde chega hoje. É escrita com o que o repositório consegue demonstrar (testes, compilações, execuções), não com o que está previsto.

🟢 Feito e verificado · 🟡 Funciona, com limites conhecidos · 🟠 Feito a meio ou ainda por verificar · 🔴 Por fazer ou a não funcionar

| Área | Estado | O que significa aqui |
|---|---|---|
| Software | 🟢 | A app compila (`gradlew testDebugUnitTest assembleDebug`) e funciona num telemóvel real contra a bancada de testes CM5. |
| Testes | 🟢 | Passam 376 testes unitários (`gradlew testDebugUnitTest`): segurança dos endereços, decisão de alarme, vigilante de alarmes, traduções e papel só de leitura. |
| Interface | 🟡 | O início de sessão, o monitor de câmaras, o armar e os alarmes e o radar estão em uso num telemóvel real; a configuração de um nó por Bluetooth não foi confirmada com um nó real, e os ecrãs solar, elétrico e de painel de alarme não encontraram equipamentos reais. |
| Hardware | 🟡 | Usada num telemóvel real contra a bancada de testes CM5; a vigilância de alarmes em segundo plano sob as regras de bateria de cada fabricante não está provada. |
| Segurança | 🟡 | As regras de segurança dos endereços e do papel só de leitura estão cobertas por testes unitários; não houve revisão independente. |
| Atualizações | 🟡 | As atualizações a partir do GitHub estão em uso no telemóvel. |
| Integração | 🟠 | O push através do Firebase está na app, mas precisa de um projeto Firebase próprio e não foi experimentado num telemóvel. |
| Documentação | 🟢 | Oito idiomas e o registo de alterações estão atualizados. |

## 🎯 Visão geral

* **Acesso com o login do próprio servidor:** IP, porta, utilizador e palavra-passe; a palavra-passe cria uma sessão HttpOnly; só fica guardada no telemóvel se marcar **Lembrar-me** (desligado por defeito: utilizador e palavra-passe cifrados com uma chave própria do Keystore, só para esse servidor, e esquecidos assim que desmarcar). Um papel **viewer** só observa: todo o botão que altera algo está desligado.
* **Bloqueio com o telemóvel:** a aplicação pede a impressão digital, o rosto ou o PIN do próprio telemóvel antes de armar, desarmar, silenciar uma sirene, desbloquear e as outras ordens críticas (ativado por predefinição só se o telemóvel tiver impressão digital ou rosto) e, se o ativares, para abrir a aplicação. O teu administrador pode exigi-lo a partir do Studio mas nunca baixar o que escolheste; um telemóvel sem bloqueio de ecrã nunca fica de fora e o ecrã bloqueado oferece sempre iniciar sessão de novo com a palavra-passe. Opcionalmente (desativado por defeito, com um aviso visível) a chave que sela a sessão guardada pode ser ligada ao desbloqueio do telemóvel; então a vigilância de alarmes em segundo plano espera, depois de um reinício, que desbloqueie o telemóvel.
* **Monitor de câmaras:** de 1 a 16 mosaicos, uma vista ampliada, MJPEG em direto que mantém a proporção, um comando PTZ limitado, fotografias e gravações, com visualizador e confirmação de apagamento das gravações.
* **Armar e desarmar,** depois de uma **verificação antes de armar** (o que está aberto, mudo ou em baixo) e com o PIN para desarmar; **alarmes** a reconhecer, com um distintivo para os que esperam; **dispositivos** (fumo, gás, inundação, porta, janela, movimento, clima, tomadas, luzes, sirenes, fechaduras, estores, motores de portão e garagem, esquentadores) com o seu estado e Ligar / Desligar / Alternar.
* **Alarmes que o alcançam:** um som para cada gravidade, um **alarme falado** opcional, avisos de um nó que chega a ALTO, uma câmara muda, um alarme de dispositivo e (armado) um nó desligado, e **uma central que toca acorda o telemóvel**; a vigilância em segundo plano sobrevive a um reinício e recua quando o servidor está em baixo.
* **Radar em direto, em 2D e 3D:** o separador Radar desenha o local como foi desenhado no Studio e as pessoas que os radares veem a mover-se nele, atualizado a cada segundo e meio; arraste, aperte e rode a vista 3D.
* **Energia solar e elétrica:** os totais (sol, consumo, baterias com a sua carga, se há rede), cada inversor e bateria com as células a pedido, e os canais e alarmes dos nós elétricos.
* **Centrais de alarme:** o estado de cada nó ARMOR-NODE-ALARM, as suas zonas e eventos, e comandos de armar / desarmar quando o servidor os permite.
* **Rede:** os dispositivos da rede da casa, o estado da internet e o **endereço público** com o fornecedor e o local, como o nó de rede os comunica.
* **Configurar um nó de campo por Bluetooth** (a partir do ecrã de acesso ou *Mais > Configurar um nó*): encontra os nós que anunciam `ARMOR-xxxxxx`, cria o administrador de um novo ou entra (com o PIN do nó), procura redes Wi-Fi e define o nome do nó, o Wi-Fi de um router ou um endereço fixo e o broker; um **localizador de nós** lista os que já estão na rede.
* **O assistente:** comandos escritos e falados (lista fechada, em oito idiomas, com uma confirmação ligada a si) para perguntar o estado da casa ou armar, desarmar, silenciar e ligar ou desligar luzes.
* **Biblioteca de provas,** estado do perímetro e dos nós, um **histórico** de cada alerta, nó, câmara, dispositivo, alarme e mudança de modo, o **mapa do tempo**, e os **serviços** e atualizações da instalação.
* **Oito idiomas** (inglês, espanhol, alemão, francês, italiano, japonês, chinês e português), escolhidos em *Mais > Definições* ou seguindo o telemóvel; verifica-se que cada frase está nas oito tabelas.
* **Cuidado com a palavra-passe:** HTTP simples só é permitido para um *literal* IPv4 de LAN privada ou loopback; um nome de anfitrião que só começa como um endereço privado (`10.attacker.example`) ou um endereço com zero à esquerda é recusado. O aspeto é feito de ícones: superfícies quase pretas, um destaque ciano, âmbar para a atenção.
* **Segurança a partir do telemóvel:** o segundo fator e os seus códigos de recuperação, as sessões abertas, uma verificação de como o servidor vê o telemóvel, a nova prova de identidade para ações críticas de fora e, para um administrador, a VPN (dispositivos com a sua configuração) e o nome de DNS dinâmico.
* **Todo o sistema no ecrã de estado e a entrada a partir de fora:** o ecrã de estado resume as centrais de alarme, os serviços, as câmaras que gravam e a segurança e o acesso remoto, e cada cartão abre o seu ecrã; e quando o endereço escrito está fora de casa e sem https, o início de sessão diz porque não pode continuar e um guia explica as duas vias seguras (VPN, ou endereço público com https).
* **Saúde dos nós e modos de armar:** um ecrã Saúde dos nós em Mais (tempo ligado, motivo do reinício, memória, ligação e sinal, ligações ao broker, reinícios, último erro e os avisos do servidor de cada nó) e, no ecrã das Centrais de alarme, um seletor de modo (Ausente, Em casa, Noite e os seus) que arma uma central no modo escolhido quando o seu firmware os conhece. Oito idiomas.
* **Ligações no histórico:** o ecrã Histórico tem uma segunda lista, só de leitura, com os dispositivos e aplicações que se ligam ao servidor (o Studio, esta app, os painéis dos nós, o serviço de voz), quando cada um foi visto pela última vez, as suas sessões abertas e os seus inícios de sessão e recusas; a app diz agora ao servidor o que é, para aparecer como ela própria. Os endereços só os veem os administradores.
* **Manutenção de baterias (administradores):** as operações simples numa bateria Pylontech - relógio, contadores e registos, reinício, capacidade, teste de MOSFET, falha de fusível, saídas - nos mesmos dois passos do Studio (o servidor faz o nó ler a bateria, o ecrã mostra exatamente o que será feito e depois "Fazer agora" com o número de série escrito outra vez), com o resultado e o que mudou; os limites, o firmware e a consola de especialista ficam no Studio.
* **Controlo dos nós (administradores):** Mais > Controlo dos nós dá as mesmas operações do Studio - reiniciar, pausar, retomar, pôr em espera (trabalho desligado, continua ligado), identificar, diagnóstico e manutenção ligada ou desligada - a um nó ou a todos, pedindo que se escreva de novo o nome do nó (ou a palavra RESTART, PAUSE ou STANDBY para todos) e a nova prova de identidade, mostrando até onde chegou cada nó e avisando que uma central de alarme ou um radar em pausa ou em espera deixa a casa sem proteção. Não existe reposição de fábrica, apagamento nem sono profundo.
* **Uma janela de alarme na aplicação aberta:** enquanto um alarme aguarda uma pessoa, um aviso sobre todos os ecrãs exceto o dos alarmes pulsa (vermelho e rápido se for crítico, âmbar nos restantes, parado se o telemóvel tiver as animações desligadas), diz qual é o pior alarme e quantos aguardam, e deixa usar a aplicação por baixo; pode abrir os alarmes, confirmar a partir do aviso ou ser fechado até chegar outro alarme.
* **A partir de fora de casa:** dois endereços (o privado na Wi-Fi de casa, o de fora com dados móveis ou a VPN) escolhidos conforme onde está o telemóvel, tentando o outro se um não responder e com uma mensagem que diz o que falha; a chave do certificado da instalação é memorizada na primeira vez (ou emparelhada com o texto do QR do Studio) e uma diferente é recusada, e o http simples é recusado para um servidor a que já se chegou por https; o painel de um nó pode abrir-se através do servidor; os alarmes podem chegar como push só com dados, anunciados uma vez venham por push ou pela vigilância; a app aberta e a vigilância partilham uma leitura do servidor; as câmaras param fora do ecrã e voltam com esperas crescentes.
* **Avisos à sua medida, com sons e uma voz próprios:** nas definições, silenciar todos os avisos, mostrá-los sem som nem vibração (e sem os dizer em voz alta) e desativá-los por gravidade (alta, baixa) ou por tipo (alertas do radar, câmaras que não respondem, nós desligados ou calados, e os alarmes de dispositivos, solar, elétrico, rede e painéis de alarme); um alarme crítico avisa sempre, a menos que todos os avisos estejam silenciados. Um som para cada gravidade e cada tipo de aviso, dos sons do telemóvel ou de um ficheiro seu (menos de 3 MB), cada um com um botão de reprodução; e a voz que diz os alarmes (mulher, homem ou qualquer; criança, adulto ou idoso; normal, herói de ação ou senhor das trevas; velocidade; e se os avisos de gravidade baixa também são ditos) com um botão de teste.
* **Um banner que leva ao ecrã certo e uma vigilância mais calma:** o banner de um alarme abre o ecrã que o trata (câmaras, dispositivos, inversores ou baterias, o ecrã elétrico, a rede ou os painéis de alarme); o telemóvel diz a sua língua ao registar-se para push para o aviso ser escrito nela, e um novo token do Firebase chega logo ao servidor a partir do segundo plano. Uma câmara com duas lentes numa só transmissão são duas janelas da lista de câmaras. Enquanto os avisos push funcionam, a vigilância em segundo plano consulta o servidor a cada 120 s em vez de a cada 10 (*Definições > Vigilância com avisos push*: 10, 60, 120, 300 ou 600 s).
* **Modos de armar do servidor (Ausente, Em casa, Noite):** quando o servidor tem os modos de armar ativados (Studio, Configuração > Segurança), o botão de armar do ecrã de estado abre um seletor para armar num modo, mudar de um para outro ou desarmar; um modo sem nada atribuído pede confirmação, uma mudança para um modo que defende menos pede o PIN como desarmar, e o estado mostra o modo em vigor. Armar a partir de desarmado executa primeiro a verificação antes de armar, como um armar normal. Com os modos desligados a app arma e desarma como antes.

## 🧩 Papel no A.R.M.O.R.

O ARMOR-ANDROID-CONTROL é a janela do telemóvel para o mesmo [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) que o [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) usa: lê e ordena através dele e de nada mais. É onde os alarmes o seguem (uma central que toca acorda o telemóvel), onde pergunta ao assistente por voz, e a ferramenta para configurar um nó novo por Bluetooth no terreno, ao lado dele.

| Nome | Como | Para quê |
|---|---|---|
| ARMOR-SERVER | HTTP `/api/v1` (55 paths), MJPEG | Tudo o que a app lê e ordena |
| Nós de campo | Bluetooth (`ARMOR-xxxxxx`) | Configura um nó novo estando ao lado dele |
| GitHub | HTTPS | Verifica e descarrega a atualização assinada da própria app |
| Open-Meteo | HTTPS | A previsão, a qualidade do ar e a pesquisa de locais públicas do mapa do tempo |

## 🏛️ Arquitetura

<p align="center">
  <img src="images/architecture.svg" alt="Arquitetura de ARMOR-ANDROID-CONTROL: quem o chama, o que contém e de que depende" width="100%">
</p>

## 🔒 Segurança e proteção

| Ameaça | Defesa |
|---|---|
| Uma palavra-passe guardada no telemóvel | Só com *Lembrar-me* (desligado por omissão): utilizador e palavra-passe selados com uma chave Keystore própria, só para esse servidor, esquecidos ao desmarcar; sem cópia de segurança (`allowBackup=false`) |
| HTTP simples para a internet | Permitido só para um *literal* IPv4 de rede privada ou loopback; um anfitrião como `10.attacker.example` ou um endereço com zero à esquerda (`010.0.0.1` lê-se em octal) é recusado; o http simples é recusado para um anfitrião alcançado por https |
| Um homem do meio em https | A confiança são as autoridades do sistema mais as que instalou no telemóvel; a chave do certificado da instalação é memorizada à primeira vez (ou emparelhada pelo texto do QR do Studio) e uma diferente é recusada |
| Uma ordem crítica com o telemóvel noutras mãos | A impressão digital, o rosto ou o PIN do telemóvel antes de armar, desarmar, silenciar uma sirene, desbloquear e as outras ordens críticas (ativo por omissão só se o telemóvel tiver impressão digital ou rosto); o administrador pode exigi-lo no Studio, nunca baixar o que escolheu; um telemóvel sem bloqueio nunca fica de fora |
| Um visualizador altera algo | O papel *visualizador* vê tudo mas todos os botões que alteram algo estão desligados (testes de papel) |
| Um PIN de desarmar errado | O PIN só vai para o servidor ao desarmar; `wrong_pin`, `pin_locked`, `pin_not_set` e `elevation_required` são traduzidos em palavras simples |
| Uma atualização falsa da app | Os SHA-256 dos certificados de assinatura (histórico) da app instalada e do candidato são comparados antes de chamar o instalador do Android; se não partilharem nenhum, diz-se claramente |
| Um alarme que o telemóvel não vê | Um serviço em primeiro plano (`AlarmWatcherService`) consulta a cada 10 s, espera 15 s pelo desbloqueio, recua até 60 s, diz *inacessível* após 6 falhas e sobrevive ao reinício; um alarme com mais de 15 minutos não é anunciado |
| Uma palavra-passe de câmara no telemóvel | As câmaras veem-se pelo fluxo do servidor: a app não tem palavra-passe de câmara nem endereço RTSP |

### Permissões

`INTERNET`, `ACCESS_NETWORK_STATE`: o servidor. `USE_BIOMETRIC`: o bloqueio do telemóvel. `POST_NOTIFICATIONS`: os alarmes. `FOREGROUND_SERVICE` (+ `DATA_SYNC`, `SPECIAL_USE`) e `RECEIVE_BOOT_COMPLETED`: a vigilância de alarmes. `REQUEST_INSTALL_PACKAGES`: a atualização da app. `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`: a configuração de um nó; `BLUETOOTH`, `BLUETOOTH_ADMIN` e `ACCESS_FINE_LOCATION` só até ao Android 11 (`maxSdkVersion=30`), para a procura Bluetooth.

Não houve revisão de segurança independente. Leia [docs/CLIENT_BOUNDARY.md](docs/CLIENT_BOUNDARY.md) e [SECURITY.md](SECURITY.md).

## 🔌 Interfaces

| Parte | O que define |
|---|---|
| ARMOR-SERVER | 55 caminhos distintos de `/api/v1` em `network/ArmorApiClient.kt` (a interface é `network/ArmorApi.kt`); o radar lê-se a cada 1,5 s; as câmaras por MJPEG através do servidor |
| Dois endereços | O privado no Wi-Fi de casa e o de fora (dados móveis ou a VPN), escolhidos conforme onde está o telemóvel; se um não responder tenta-se o outro e a mensagem diz o que falha |
| Bluetooth | Encontra os nós que anunciam `ARMOR-xxxxxx`, cria o administrador de um novo ou entra com o PIN do nó, procura redes Wi-Fi e define o nome, o Wi-Fi ou um endereço fixo e o broker; o protocolo é `model/NodeBle.kt`; um localizador de nós lista os que já estão na rede |
| Voz | `POST /api/v1/voice/command`: uma lista fechada de comandos em oito línguas, com uma confirmação ligada a si (`model/Voice.kt`, `VoiceScreens.kt`) |
| Push | `PushService`: alarmes como mensagens só com dados, anunciados uma vez venham por push ou pela vigilância; os passos do Firebase estão em [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) (oito línguas) |

## ⚙️ Configuração

| Nome | Valor | O que define |
|---|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 26 | Android 8.0 ou posterior no telemóvel |
| `versionName` | = `armor.project.json` | A compilação falha se o manifesto e a versão diferirem |
| JDK / Android SDK | 17+ / 37 | Para compilar a app e executar os testes |

Na app (não em ficheiros): a língua (as oito ou seguir o telemóvel), os dois endereços, *Lembrar-me*, o bloqueio do telemóvel, o alarme falado e as notificações, em *Mais > Definições*. Limites no código: a vigilância consulta a cada 10 s, recua até 60 s e ignora alarmes com mais de 15 minutos.

## 🚀 Início rápido

**Requisitos:** JDK 17+, o Android SDK (compile SDK 37), um telemóvel com Android 8.0+ (minSdk 26).

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 376 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

As versões são publicadas no GitHub, de onde a própria aplicação procura atualizações; o APK é assinado com a chave de publicação própria do responsável, guardada fora do repositório, e a aplicação compara o certificado de assinatura de uma transferência com o seu antes de passar o ficheiro ao instalador do Android. Veja o [limite do cliente](docs/CLIENT_BOUNDARY.md).

## 🧪 Testes

```powershell
.\gradlew.bat testDebugUnitTest
python -I tools/armor_ci_validate.py
```

| Parte | Testes | O que cobre |
|---|---|---|
| Alarmes | 67 | `AlarmPolicy`, `AlarmBanner`, `AlarmPanels`, `AlarmCodes`, `AlarmRoute`, `NoticeFilter`, `PushEvents`, `DevicesAndAlarms` |
| Ligação ao servidor, leitura, dados de teste | 59 | `ApiClient`, `ApiError`, `ServerFixtures`, `ParsersTolerant`, `SavePayload`, `JsonText`, `Preflight`, `SharedFeed`, `Connections` |
| Modelo de vista e papéis | 41 | `ArmorViewModel`, `Viewer`, `ArmProfiles`, `NodeCensus`, `NodeHealth` |
| Bloqueio, cofre da sessão, chaves | 38 | `DeviceLock`, `SessionVault`, `CredentialVault`, `KeyBinding`, `Security` |
| Segurança de endereços, certificados | 32 | `ServerEndpoint`, `ServerAddresses`, `Literals`, `NodeAddress`, `CertificatePins`, `RemoteAccess` |
| Energia e rede | 34 | `Solar`, `Battery`, `Electrical`, `Network` |
| Nós por Bluetooth, controlo de nós | 26 | `NodeBle`, `NodeControl` |
| Atualizações do GitHub | 17 | `GitHubReleaseUpdaterHttpFailure`, `SignerCheck`, `SemanticVersion`, `ReleaseMetadataParser` |
| Radar e câmaras | 14 | `RadarGeometry`, `CameraTraits`, `CameraLenses` |
| Traduções, palavras simples, voz | 14 | `Tr`, `Friendly`, `Voice` |

376 testes em 47 ficheiros, executados com `testDebugUnitTest`; um servidor falso (`FakeServer.kt`) e dados tirados de um servidor real (`tools/capture_fixtures.mjs`) alimentam-nos. Verifica-se que cada frase está nas oito tabelas de língua.

## 🏗️ Compilação e publicação

As versões são publicadas no GitHub, de onde a própria app verifica as atualizações; a app procura o recurso `ARMOR-ANDROID-CONTROL-release.apk`. O APK é assinado com a chave de versão do mantenedor, guardada fora do repositório, e a app compara o certificado de assinatura de uma transferência com o seu antes de passar o ficheiro ao instalador do Android. O `versionCode` sobe antes de cada APK, senão o Android não o trata como atualização.

## 🩺 Resolução de problemas

| Vê | Porque | Faça |
|---|---|---|
| `wrong_pin`, `pin_locked`, `pin_not_set`, `elevation_required` | O servidor recusa o PIN de desarmar ou pede uma nova prova de identidade | Escreva o PIN de novo, espere o fim do bloqueio ou confirme a sua identidade |
| O início de sessão diz que não pode continuar | O endereço está fora de casa e não tem https (o http simples é recusado) | Siga o guia: uma VPN, ou um endereço público com https |
| A ligação é recusada depois de ter funcionado | A chave do certificado da instalação mudou | Emparelhe de novo com o texto do QR do Studio se o mudou de propósito |
| Os alarmes chegam tarde ou não chegam em segundo plano | As regras de bateria do fabricante param a vigilância; o push precisa de um projeto Firebase próprio | Exclua a app da poupança de bateria; siga [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) |
| A atualização é recusada | O certificado de assinatura da transferência não partilha nenhum com a app instalada | Não o instale; use o APK das versões deste repositório |

## 📂 Estrutura do repositório

<p align="center">
  <img src="images/repository-structure.svg" alt="Estrutura do repositório de ARMOR-ANDROID-CONTROL" width="100%">
</p>

<details>
<summary>Versão em texto da árvore</summary>

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/   a app: ecrãs, regras, rede e atualizações
├── app/src/main/assets/                         oito tabelas de língua, códigos de alarme, mapa do tempo
├── app/src/test/                                376 testes e um servidor falso
├── docs/                                        os guias deste repositório
├── tools/                                       as verificações de CI partilhadas e as ferramentas do projeto
├── .github/                                     fluxo de CI e modelos de issues e pull requests
├── images/                                      faixa, ícone e as duas imagens deste README
├── build.sh build.bat                           compilar pelo fluxo de projeto partilhado
├── test.sh test.bat                             executar os testes pelo fluxo partilhado
├── run.sh run.bat                               arrancar pelo fluxo de projeto partilhado
├── armor.project.json                           manifesto: versão, função, maturidade, comando de build
├── CHANGELOG.md                                 o que mudou em cada versão
├── SECURITY.md SUPPORT.md LICENSE               política de segurança, apoio e licença
├── CONTRIBUTING.md CODE_OF_CONDUCT.md           como contribuir e o código de conduta
├── build.gradle.kts settings.gradle.kts         a compilação Gradle do projeto
├── gradle.properties                            definições do Gradle
├── gradlew gradlew.bat gradle/                  o wrapper do Gradle
├── build-test.sh build-test.bat                 executar os testes pelo fluxo partilhado
└── .gitattributes .gitignore                    definições do Git
```

</details>

## 🗺️ Roteiro e limites conhecidos

**Limites conhecidos hoje**

* Os ecrãs de solar, elétrico e painel de alarme não conheceram equipamento real (só existe um nó radar).
* A configuração Bluetooth de um nó não foi confirmada com um nó real, e o push via Firebase não foi experimentado num telemóvel.
* A vigilância de alarmes em segundo plano sob as regras de bateria de cada fabricante não está provada.
* Sem revisão de segurança independente.

**A seguir**

* Experimentar os ecrãs de solar, elétrico e painel de alarme com nós reais, e a configuração Bluetooth com um nó real.
* Experimentar o push via Firebase com o projeto próprio de uma instalação, e a vigilância de alarmes em vários fabricantes.

## 🌍 Idiomas

O README e os textos existem em inglês, espanhol, francês, italiano, alemão, chinês, japonês e português europeu.

## 🔗 Projetos relacionados

O **A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) é um sistema de segurança perimetral feito de repositórios independentes. Cada um tem a sua própria versão, os seus próprios testes e o seu próprio README; esta é a família:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contratos de mensagens, validadores, vetores de conformidade e tipos gerados
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Firmware de nó de campo para ESP32-S3 com três radares e o seu próprio painel web
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protocolos de inversores e baterias solares e as mensagens de um nó porta de ligação
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Nó elétrico: contadores, a mensagem das leituras da rede e as regras de comutação
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Nó e painel de alarme: zonas, armar, atrasos, sirene e PIN, com o servidor ou sem ele
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Painel tátil: o estado do sistema num ecrã de parede, armar e reconhecer, e a casa do assistente de voz
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - A rede local: os seus dispositivos, a internet e o que muda
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordenador central: telemetria, alarmes, dispositivos, leituras solares e câmaras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Consola web: câmaras, radar, alarmes, energia solar e o desenhador 2D/3D do local
* **ARMOR-ANDROID-CONTROL** (este repositório) - Cliente de operador Android com radar 2D/3D em direto
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - Cliente de operador para iPhone
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Política de inferência visual que explica as suas decisões e nunca atua
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zonas de câmara, máscaras e regras de deteção para o Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intenções de voz offline com uma confirmação que não pode ser falsificada
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Caixas, eletrónica e a matriz de aceitação em bancada
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Implantação, a bancada de testes do CM5, cópias de segurança e TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulador de telemetria offline com falhas repetíveis
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Deteta, instala e atualiza os repositórios do próprio ecossistema
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Arquitetura, base de segurança e a matriz de capacidades

## 📚 Documentação

* [Limites do cliente](docs/CLIENT_BOUNDARY.md): o que o cliente faz e nunca faz
* [Notificações push](docs/PUSH_NOTIFICATIONS.md): os passos do Firebase, em oito línguas
* [Testes de bancada](docs/BENCH_TESTS.md): verificações com um telemóvel real
* [Matriz de capacidades: o que está demonstrado e o que não está](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catálogo de projetos: versões e como os repositórios dependem uns dos outros](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Registo de alterações deste repositório](CHANGELOG.md)

## 🤝 Contribuir e comunidade

* Antes de um pull request, execute `.\gradlew.bat testDebugUnitTest` and `python -I tools/armor_ci_validate.py`: tem de passar. Consulte [CONTRIBUTING.md](CONTRIBUTING.md) e o [código de conduta](CODE_OF_CONDUCT.md).
* O README existe em oito idiomas e é gerado a partir das fontes do ARMOR-DOCS: uma alteração ao texto faz-se lá.
* Uma alteração a uma mensagem entre componentes começa no ARMOR-COMMON.
* Não descreva como a funcionar numa placa aquilo que só foi compilado.
* Comunique uma vulnerabilidade em privado: [SECURITY.md](SECURITY.md). Perguntas, ideias e relatórios: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENÇA

GPL-3.0-or-later - consulte [LICENSE](LICENSE).
