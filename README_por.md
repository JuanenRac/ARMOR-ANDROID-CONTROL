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
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-319%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.4-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**Verificação de honestidade - o que funciona hoje:** As regras de segurança do endereço, de decisão de alarmes, do vigia de alarmes, de tradução e do papel só de leitura estão cobertas por 257 testes unitários e a aplicação compila (`gradlew testDebugUnitTest assembleDebug`). Funciona num telemóvel real contra a bancada de testes do CM5: o acesso, o monitor de câmaras, o armar e os alarmes, o radar, a configuração de um nó por Bluetooth e as atualizações a partir do GitHub estão em uso. O que **ainda não** está provado: os ecrãs solar, elétrico e de centrais de alarme com equipamentos reais (só existe um nó radar), o push por Firebase (está na aplicação e precisa de um projeto Firebase próprio; ainda não foi experimentado num telemóvel) e a vigilância de alarmes em segundo plano sob as regras de bateria de cada fabricante.

---

## 📋 Num relance

| | |
|---|---|
| Versão | `0.7.4` |
| Maturidade | Estabelecido |
| Testes | 319 (`gradlew testDebugUnitTest`) |
| Tecnologia | Kotlin / Jetpack Compose |
| Em hardware real | Usada num telemóvel real contra a bancada de testes do CM5; os ecrãs solar, elétrico e de centrais de alarme esperam nós reais |

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
* **Segurança a partir do telemóvel:** o segundo fator e os seus códigos de recuperação, as sessões abertas, uma verificação de como o servidor vê o telemóvel, a nova prova de identidade para ações críticas de fora e, para um administrador, a VPN (dispositivos com a sua configuração) e o nome de DNS dinâmico.**Todo o sistema no ecrã de estado e a entrada a partir de fora:** o ecrã de estado resume as centrais de alarme, os serviços, as câmaras que gravam e a segurança e o acesso remoto, e cada cartão abre o seu ecrã; e quando o endereço escrito está fora de casa e sem https, o início de sessão diz porque não pode continuar e um guia explica as duas vias seguras (VPN, ou endereço público com https).
* **Saúde dos nós e modos de armar:** um ecrã Saúde dos nós em Mais (tempo ligado, motivo do reinício, memória, ligação e sinal, ligações ao broker, reinícios, último erro e os avisos do servidor de cada nó) e, no ecrã das Centrais de alarme, um seletor de modo (Ausente, Em casa, Noite e os seus) que arma uma central no modo escolhido quando o seu firmware os conhece. Oito idiomas.
* **Ligações no histórico:** o ecrã Histórico tem uma segunda lista, só de leitura, com os dispositivos e aplicações que se ligam ao servidor (o Studio, esta app, os painéis dos nós, o serviço de voz), quando cada um foi visto pela última vez, as suas sessões abertas e os seus inícios de sessão e recusas; a app diz agora ao servidor o que é, para aparecer como ela própria. Os endereços só os veem os administradores.
* **Manutenção de baterias (administradores):** as operações simples numa bateria Pylontech - relógio, contadores e registos, reinício, capacidade, teste de MOSFET, falha de fusível, saídas - nos mesmos dois passos do Studio (o servidor faz o nó ler a bateria, o ecrã mostra exatamente o que será feito e depois "Fazer agora" com o número de série escrito outra vez), com o resultado e o que mudou; os limites, o firmware e a consola de especialista ficam no Studio.
* **Controlo dos nós (administradores):** Mais > Controlo dos nós dá as mesmas operações do Studio - reiniciar, pausar, retomar, pôr em espera (trabalho desligado, continua ligado), identificar, diagnóstico e manutenção ligada ou desligada - a um nó ou a todos, pedindo que se escreva de novo o nome do nó (ou a palavra RESTART, PAUSE ou STANDBY para todos) e a nova prova de identidade, mostrando até onde chegou cada nó e avisando que uma central de alarme ou um radar em pausa ou em espera deixa a casa sem proteção. Não existe reposição de fábrica, apagamento nem sono profundo.
* **Uma janela de alarme na aplicação aberta:** enquanto um alarme aguarda uma pessoa, um aviso sobre todos os ecrãs exceto o dos alarmes pulsa (vermelho e rápido se for crítico, âmbar nos restantes, parado se o telemóvel tiver as animações desligadas), diz qual é o pior alarme e quantos aguardam, e deixa usar a aplicação por baixo; pode abrir os alarmes, confirmar a partir do aviso ou ser fechado até chegar outro alarme.
* **A partir de fora de casa:** dois endereços (o privado na Wi-Fi de casa, o de fora com dados móveis ou a VPN) escolhidos conforme onde está o telemóvel, tentando o outro se um não responder e com uma mensagem que diz o que falha; a chave do certificado da instalação é memorizada na primeira vez (ou emparelhada com o texto do QR do Studio) e uma diferente é recusada, e o http simples é recusado para um servidor a que já se chegou por https; o painel de um nó pode abrir-se através do servidor; os alarmes podem chegar como push só com dados, anunciados uma vez venham por push ou pela vigilância; a app aberta e a vigilância partilham uma leitura do servidor; as câmaras param fora do ecrã e voltam com esperas crescentes.

## 🧩 Papel no A.R.M.O.R.

O ARMOR-ANDROID-CONTROL é a janela do telemóvel para o mesmo [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) que o [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) usa: lê e ordena através dele e de nada mais. É onde os alarmes o seguem (uma central que toca acorda o telemóvel), onde pergunta ao assistente por voz, e a ferramenta para configurar um nó novo por Bluetooth no terreno, ao lado dele.

## 📂 Estrutura do repositório

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, NodeControlScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmBanner.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, DeviceLock.kt, PhoneLock.kt, LockScreens.kt, PinDialog.kt, MjpegFeed.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md   (the Firebase steps, in eight languages)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ Ambiente de desenvolvimento

**Requisitos:** JDK 17+, o SDK do Android (compile SDK 37), um telemóvel com Android 8.0+ (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 319 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

As versões são publicadas no GitHub, de onde a própria aplicação procura atualizações; o APK é assinado com a chave de publicação própria do responsável, guardada fora do repositório, e a aplicação compara o certificado de assinatura de uma transferência com o seu antes de passar o ficheiro ao instalador do Android. Veja o [limite do cliente](docs/CLIENT_BOUNDARY.md).

## 🔗 Projetos relacionados

O **A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) é um sistema de segurança perimetral feito de repositórios independentes. Cada um tem a sua própria versão, os seus próprios testes e o seu próprio README; esta é a família:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contratos de mensagens, validadores, vetores de conformidade e tipos gerados
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Firmware de nó de campo para ESP32-S3 com três radares e o seu próprio painel web
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protocolos de inversores e baterias solares e as mensagens de um nó de ligação
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Nó elétrico: contadores, a mensagem das leituras da rede e as regras de comutação
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Nó e central de alarme: zonas, armar, atrasos, sirene e PIN, com o servidor ou sem ele
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Painel tátil: o estado do sistema num ecrã de parede, armar e reconhecer, e a casa do assistente de voz
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - A rede local: os seus dispositivos, a internet e o que muda
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordenador central: telemetria, alarmes, dispositivos, leituras solares e câmaras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Consola web: câmaras, radar, alarmes, energia solar e o desenhador de local 2D/3D
* **ARMOR-ANDROID-CONTROL** (este repositório) - Cliente Android de operador com um radar 2D/3D em direto
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Política de inferência visual que explica as suas decisões e nunca atua
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zonas de câmara, máscaras e regras de deteção para o Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intenções de voz offline com uma confirmação que não se pode falsificar
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Caixas, eletrónica e a matriz de aceitação de bancada
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Implementação, a bancada de testes CM5, cópias de segurança e TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulador de telemetria offline com falhas repetíveis
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Deteta, instala e atualiza os próprios repositórios do ecossistema
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Arquitetura, linha de base de segurança e a matriz de capacidades

## 📚 Documentação e comunidade

Onde ler mais:

* [Matriz de capacidades: o que está provado e o que não](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catálogo de projetos: versões e como os repositórios dependem uns dos outros](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Registo de alterações deste repositório](CHANGELOG.md)
* [Licença (GPL-3.0-or-later)](LICENSE)
* Perguntas, ideias e relatórios: electrohobby3d@gmail.com

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENÇA

GPL-3.0-or-later - veja [LICENSE](LICENSE).
