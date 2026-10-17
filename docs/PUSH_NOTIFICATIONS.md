# Push notifications (Firebase)

**Languages:** [English](#english) · [Español](#español) · [Français](#français) · [Italiano](#italiano) · [Deutsch](#deutsch) · [中文](#中文) · [日本語](#日本語) · [Português](#português)

Alarms reach the phone even when the app is closed through **your own** Firebase project (free). Nothing goes through anybody else's: the server sends to Firebase with your key, and Firebase wakes your phone. Push is **off** until you do the steps below; without it the app still watches the server by itself while it runs.

Two files come out of the Firebase console. They are different and neither goes into a repository:

| File | What it is | Where it goes |
|---|---|---|
| `google-services.json` | Tells the **app** which Firebase project it belongs to | `app/google-services.json` of this project, before building the APK |
| `<project>-firebase-adminsdk-….json` | The **service account**: a private key that lets the **server** send | The server machine, named in `ARMOR_FCM_SERVICE_ACCOUNT` |

---

## English

1. **Create the project.** Open <https://console.firebase.google.com>, *Add project*, give it a name (for example `armor-home`). Google Analytics is not needed.
2. **Register the Android app.** In the project press the Android icon (*Add app*). The package name must be exactly `es.electrohobby3d.armor`. The nickname is optional and the SHA-1 is not needed for push.
3. **Download `google-services.json`** from step 2 of that wizard (you can finish the wizard without following its other steps: this project does not use the Google Services plugin).
4. **Put it in the app.** Copy it to `app/google-services.json`. It is ignored by git on purpose. Build the APK yourself (`build.bat` / `build.sh`, or `gradlew assembleRelease`). A build without the file works but has no push.
5. **Get the service account.** Project settings (the gear) → *Service accounts* → *Generate new private key*. A JSON file downloads. In *Cloud Messaging* of the same settings make sure **Firebase Cloud Messaging API (V1)** is enabled.
6. **Give it to the server.** Copy the file to the server machine, for example `/opt/armor/etc/armor-fcm.json`, owned by the user the server runs as, mode `600`. In the server's environment file (`armor.env`) add `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json` and restart `armor-server`.
7. **Check it.** As an administrator, `GET /api/v1/push/status` answers `{"enabled":true,"devices":0}`. Install the APK you built, sign in: `devices` becomes 1. In Studio, Configuration → Notifications, send the test: the phone shows it even with the app closed.
8. **If it does not arrive.** `enabled:false`: the variable is not set or the path is wrong (the server log says why, never with a key in it). `devices:0`: the phone has not signed in on this build, or the build had no `google-services.json`. Nothing shown: allow notifications for the app in Android, and check the *Cloud Messaging API (V1)* is on.

**Security.** Never commit either file or paste them in an issue. To cut off a leaked service account, delete its key in the console (*Service accounts* → *Manage service account permissions* → *Keys*). The phone tokens are kept in the server's data directory and a phone is removed when it signs out if the server can be reached and the user may do it (a viewer's sign-out also drops the phone's Firebase token, so the server's next message to it finds no one). The text of a notification is in the language of the server, not of the phone. The APKs on this repository's releases are built with the maintainer's own project: push works only against that maintainer's server, so build your own APK for your installation.

## Español

1. **Crea el proyecto.** Entra en <https://console.firebase.google.com>, *Añadir proyecto*, ponle un nombre (por ejemplo `armor-casa`). No hace falta Google Analytics.
2. **Registra la app Android.** En el proyecto pulsa el icono de Android (*Añadir app*). El nombre del paquete debe ser exactamente `es.electrohobby3d.armor`. El apodo es opcional y el SHA-1 no hace falta para el push.
3. **Descarga `google-services.json`** en el paso 2 del asistente (puedes cerrar el asistente sin seguir los demás pasos: este proyecto no usa el plugin de Google Services).
4. **Ponlo en la app.** Cópialo a `app/google-services.json`. Git lo ignora a propósito. Compila tú el APK (`build.bat` / `build.sh`, o `gradlew assembleRelease`). Una compilación sin el archivo funciona pero no tiene push.
5. **Consigue la cuenta de servicio.** Configuración del proyecto (el engranaje) → *Cuentas de servicio* → *Generar nueva clave privada*. Se descarga un JSON. En *Cloud Messaging* de esa misma configuración comprueba que **Firebase Cloud Messaging API (V1)** esté habilitada.
6. **Dásela al servidor.** Copia el archivo a la máquina del servidor, por ejemplo `/opt/armor/etc/armor-fcm.json`, de la propiedad del usuario con el que corre el servidor y con permisos `600`. En el archivo de entorno del servidor (`armor.env`) añade `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json` y reinicia `armor-server`.
7. **Compruébalo.** Como administrador, `GET /api/v1/push/status` responde `{"enabled":true,"devices":0}`. Instala el APK que compilaste e inicia sesión: `devices` pasa a 1. En Studio, Configuración → Notificaciones, envía la prueba: el móvil la muestra aunque la app esté cerrada.
8. **Si no llega.** `enabled:false`: la variable no está puesta o la ruta es incorrecta (el registro del servidor dice por qué, nunca con una clave dentro). `devices:0`: el móvil no ha iniciado sesión en esta compilación, o la compilación no tenía `google-services.json`. No se muestra nada: permite las notificaciones de la app en Android y comprueba que la *Cloud Messaging API (V1)* esté activa.

**Seguridad.** No subas nunca ninguno de los dos archivos ni los pegues en una incidencia. Para anular una cuenta de servicio filtrada, borra su clave en la consola (*Cuentas de servicio* → *Administrar permisos de la cuenta de servicio* → *Claves*). Los tokens de los móviles se guardan en el directorio de datos del servidor y un móvil se quita al cerrar sesión si se puede avisar al servidor (al cerrar sesión también se descarta el token de Firebase del móvil, así que los mensajes del servidor ya no lo encuentran). El texto de las notificaciones va en el idioma del servidor, no en el del móvil. Los APK de las releases de este repositorio se compilan con el proyecto del mantenedor: el push solo funciona contra el servidor del mantenedor, así que compila tu propio APK para tu instalación.

## Français

1. **Créez le projet.** Ouvrez <https://console.firebase.google.com>, *Ajouter un projet*, donnez-lui un nom (par exemple `armor-maison`). Google Analytics n'est pas nécessaire.
2. **Enregistrez l'application Android.** Dans le projet, cliquez sur l'icône Android (*Ajouter une application*). Le nom du package doit être exactement `es.electrohobby3d.armor`. Le pseudo est facultatif et le SHA-1 n'est pas nécessaire pour le push.
3. **Téléchargez `google-services.json`** à l'étape 2 de l'assistant (vous pouvez fermer l'assistant sans suivre les autres étapes : ce projet n'utilise pas le plugin Google Services).
4. **Placez-le dans l'application.** Copiez-le dans `app/google-services.json`. Git l'ignore volontairement. Compilez vous-même l'APK (`build.bat` / `build.sh`, ou `gradlew assembleRelease`). Une compilation sans ce fichier fonctionne mais n'a pas de push.
5. **Obtenez le compte de service.** Paramètres du projet (la roue dentée) → *Comptes de service* → *Générer une nouvelle clé privée*. Un fichier JSON se télécharge. Dans *Cloud Messaging* des mêmes paramètres, vérifiez que **Firebase Cloud Messaging API (V1)** est activée.
6. **Donnez-le au serveur.** Copiez le fichier sur la machine du serveur, par exemple `/opt/armor/etc/armor-fcm.json`, appartenant à l'utilisateur qui exécute le serveur, avec les droits `600`. Dans le fichier d'environnement du serveur (`armor.env`) ajoutez `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json` et redémarrez `armor-server`.
7. **Vérifiez.** En administrateur, `GET /api/v1/push/status` répond `{"enabled":true,"devices":0}`. Installez l'APK compilé et connectez-vous : `devices` passe à 1. Dans Studio, Configuration → Notifications, envoyez le test : le téléphone l'affiche même application fermée.
8. **Si rien n'arrive.** `enabled:false` : la variable n'est pas définie ou le chemin est faux (le journal du serveur dit pourquoi, jamais avec une clé dedans). `devices:0` : le téléphone ne s'est pas connecté avec cette compilation, ou elle n'avait pas de `google-services.json`. Rien d'affiché : autorisez les notifications de l'application dans Android et vérifiez que la *Cloud Messaging API (V1)* est activée.

**Sécurité.** Ne versionnez jamais l'un de ces fichiers et ne les collez pas dans un ticket. Pour neutraliser un compte de service divulgué, supprimez sa clé dans la console (*Comptes de service* → *Gérer les autorisations du compte de service* → *Clés*). Les jetons des téléphones sont gardés dans le répertoire de données du serveur et un téléphone est retiré à la déconnexion si le serveur peut être prévenu (la déconnexion supprime aussi le jeton Firebase du téléphone : les messages du serveur ne le trouvent plus). Le texte des notifications est dans la langue du serveur, pas celle du téléphone. Les APK des releases de ce dépôt sont compilés avec le projet du mainteneur : le push ne fonctionne que contre son serveur, compilez donc votre propre APK pour votre installation.

## Italiano

1. **Crea il progetto.** Apri <https://console.firebase.google.com>, *Aggiungi progetto*, dagli un nome (per esempio `armor-casa`). Google Analytics non serve.
2. **Registra l'app Android.** Nel progetto premi l'icona Android (*Aggiungi app*). Il nome del pacchetto deve essere esattamente `es.electrohobby3d.armor`. Il soprannome è facoltativo e lo SHA-1 non serve per il push.
3. **Scarica `google-services.json`** al passo 2 della procedura guidata (puoi chiuderla senza seguire gli altri passi: questo progetto non usa il plugin Google Services).
4. **Mettilo nell'app.** Copialo in `app/google-services.json`. Git lo ignora di proposito. Compila tu l'APK (`build.bat` / `build.sh`, o `gradlew assembleRelease`). Una compilazione senza il file funziona ma non ha push.
5. **Ottieni l'account di servizio.** Impostazioni del progetto (l'ingranaggio) → *Account di servizio* → *Genera nuova chiave privata*. Si scarica un file JSON. In *Cloud Messaging* delle stesse impostazioni verifica che **Firebase Cloud Messaging API (V1)** sia abilitata.
6. **Dallo al server.** Copia il file sulla macchina del server, per esempio `/opt/armor/etc/armor-fcm.json`, di proprietà dell'utente con cui gira il server e con permessi `600`. Nel file di ambiente del server (`armor.env`) aggiungi `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json` e riavvia `armor-server`.
7. **Verifica.** Da amministratore, `GET /api/v1/push/status` risponde `{"enabled":true,"devices":0}`. Installa l'APK compilato e accedi: `devices` diventa 1. In Studio, Configurazione → Notifiche, invia il test: il telefono lo mostra anche con l'app chiusa.
8. **Se non arriva.** `enabled:false`: la variabile non è impostata o il percorso è sbagliato (il registro del server dice perché, mai con una chiave dentro). `devices:0`: il telefono non ha effettuato l'accesso con questa compilazione, o non aveva `google-services.json`. Non si vede nulla: consenti le notifiche dell'app in Android e controlla che la *Cloud Messaging API (V1)* sia attiva.

**Sicurezza.** Non committare mai nessuno dei due file e non incollarli in una segnalazione. Per invalidare un account di servizio trapelato, elimina la sua chiave nella console (*Account di servizio* → *Gestisci autorizzazioni dell'account di servizio* → *Chiavi*). I token dei telefoni sono nella directory dati del server e un telefono viene rimosso quando esce, se il server può essere avvisato (l'uscita elimina anche il token Firebase del telefono: i messaggi del server non lo trovano più). Il testo delle notifiche è nella lingua del server, non in quella del telefono. Gli APK delle release di questo repository sono compilati con il progetto del manutentore: il push funziona solo col suo server, quindi compila il tuo APK per la tua installazione.

## Deutsch

1. **Projekt anlegen.** Öffnen Sie <https://console.firebase.google.com>, *Projekt hinzufügen*, geben Sie einen Namen ein (zum Beispiel `armor-zuhause`). Google Analytics ist nicht nötig.
2. **Android-App registrieren.** Klicken Sie im Projekt auf das Android-Symbol (*App hinzufügen*). Der Paketname muss genau `es.electrohobby3d.armor` lauten. Der Spitzname ist optional, SHA-1 wird für Push nicht gebraucht.
3. **`google-services.json` herunterladen** in Schritt 2 des Assistenten (Sie können ihn beenden, ohne die übrigen Schritte zu befolgen: dieses Projekt nutzt das Google-Services-Plugin nicht).
4. **In die App legen.** Kopieren Sie sie nach `app/google-services.json`. Git ignoriert sie absichtlich. Bauen Sie die APK selbst (`build.bat` / `build.sh` oder `gradlew assembleRelease`). Ein Build ohne die Datei funktioniert, hat aber kein Push.
5. **Dienstkonto holen.** Projekteinstellungen (das Zahnrad) → *Dienstkonten* → *Neuen privaten Schlüssel generieren*. Eine JSON-Datei wird heruntergeladen. Prüfen Sie unter *Cloud Messaging* derselben Einstellungen, dass **Firebase Cloud Messaging API (V1)** aktiviert ist.
6. **Dem Server geben.** Kopieren Sie die Datei auf den Server-Rechner, zum Beispiel nach `/opt/armor/etc/armor-fcm.json`, im Besitz des Benutzers, unter dem der Server läuft, mit Rechten `600`. Tragen Sie in die Umgebungsdatei des Servers (`armor.env`) `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json` ein und starten Sie `armor-server` neu.
7. **Prüfen.** Als Administrator antwortet `GET /api/v1/push/status` mit `{"enabled":true,"devices":0}`. Installieren Sie die gebaute APK und melden Sie sich an: `devices` wird 1. In Studio unter Konfiguration → Benachrichtigungen den Test senden: Das Handy zeigt ihn auch bei geschlossener App.
8. **Wenn nichts ankommt.** `enabled:false`: Die Variable fehlt oder der Pfad ist falsch (das Server-Log sagt warum, nie mit einem Schlüssel darin). `devices:0`: Das Handy hat sich mit diesem Build nicht angemeldet oder der Build hatte keine `google-services.json`. Nichts angezeigt: Benachrichtigungen der App in Android erlauben und prüfen, ob die *Cloud Messaging API (V1)* aktiv ist.

**Sicherheit.** Committen Sie keine der beiden Dateien und fügen Sie sie in kein Ticket ein. Um ein geleaktes Dienstkonto unbrauchbar zu machen, löschen Sie seinen Schlüssel in der Konsole (*Dienstkonten* → *Dienstkontoberechtigungen verwalten* → *Schlüssel*). Die Handy-Tokens liegen im Datenverzeichnis des Servers und ein Handy wird beim Abmelden entfernt, wenn der Server erreicht werden kann (beim Abmelden wird auch das Firebase-Token des Handys verworfen, die Nachrichten des Servers finden es dann nicht mehr). Der Text der Benachrichtigungen ist in der Sprache des Servers, nicht des Handys. Die APKs der Releases dieses Repositorys sind mit dem Projekt des Maintainers gebaut: Push funktioniert nur gegen dessen Server, bauen Sie also für Ihre Installation Ihre eigene APK.

## 中文

1. **创建项目。** 打开 <https://console.firebase.google.com>，点击“添加项目”，起个名字（例如 `armor-home`）。不需要 Google Analytics。
2. **注册 Android 应用。** 在项目中点击 Android 图标（“添加应用”）。包名必须严格为 `es.electrohobby3d.armor`。昵称可选，推送不需要 SHA-1。
3. **下载 `google-services.json`**，在向导的第 2 步（可以不做其余步骤直接结束向导：本项目不使用 Google Services 插件）。
4. **放进应用。** 复制到 `app/google-services.json`。git 会特意忽略它。请自己构建 APK（`build.bat` / `build.sh`，或 `gradlew assembleRelease`）。没有该文件也能构建，但没有推送。
5. **获取服务账号。** 项目设置（齿轮图标）→“服务账号”→“生成新的私钥”，会下载一个 JSON 文件。在同一设置的“Cloud Messaging”中确认已启用 **Firebase Cloud Messaging API (V1)**。
6. **交给服务器。** 把文件复制到服务器所在机器，例如 `/opt/armor/etc/armor-fcm.json`，属主为运行服务器的用户，权限 `600`。在服务器的环境文件（`armor.env`）中加入 `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json`，然后重启 `armor-server`。
7. **验证。** 以管理员身份，`GET /api/v1/push/status` 返回 `{"enabled":true,"devices":0}`。安装你构建的 APK 并登录：`devices` 变为 1。在 Studio 的“配置 → 通知”里发送测试：即使应用已关闭，手机也会显示。
8. **收不到时。** `enabled:false`：未设置该变量或路径错误（服务器日志会说明原因，且从不包含密钥）。`devices:0`：手机没有用此构建登录，或该构建没有 `google-services.json`。没有显示：在 Android 中允许该应用的通知，并确认 *Cloud Messaging API (V1)* 已启用。

**安全。** 切勿提交这两个文件，也不要贴到问题反馈里。要使泄露的服务账号失效，请在控制台删除其密钥（“服务账号”→“管理服务账号权限”→“密钥”）。手机令牌保存在服务器的数据目录中，手机退出登录时，如果能通知到服务器，就会被移除（退出登录还会丢弃手机的 Firebase 令牌，服务器之后的消息就找不到它了）。通知文字使用服务器的语言，而不是手机的语言。本仓库 Release 中的 APK 是用维护者自己的项目构建的：推送只对维护者的服务器有效，所以请为你的安装自己构建 APK。

## 日本語

1. **プロジェクトを作成します。** <https://console.firebase.google.com> を開き、「プロジェクトを追加」で名前を付けます（例：`armor-home`）。Google アナリティクスは不要です。
2. **Android アプリを登録します。** プロジェクトで Android アイコン（「アプリを追加」）を押します。パッケージ名は必ず `es.electrohobby3d.armor` にします。ニックネームは任意で、プッシュに SHA-1 は不要です。
3. **`google-services.json` をダウンロードします。** ウィザードのステップ 2 で行います（残りの手順は実行せずに閉じて構いません。このプロジェクトは Google Services プラグインを使いません）。
4. **アプリに置きます。** `app/google-services.json` にコピーします。git は意図的に無視します。APK は自分でビルドします（`build.bat` / `build.sh`、または `gradlew assembleRelease`）。このファイルなしのビルドも動作しますが、プッシュはありません。
5. **サービスアカウントを取得します。** プロジェクトの設定（歯車）→「サービスアカウント」→「新しい秘密鍵を生成」。JSON ファイルがダウンロードされます。同じ設定の「Cloud Messaging」で **Firebase Cloud Messaging API (V1)** が有効になっていることを確認します。
6. **サーバーに渡します。** ファイルをサーバーのマシンにコピーします（例：`/opt/armor/etc/armor-fcm.json`）。所有者はサーバーを実行するユーザー、権限は `600` にします。サーバーの環境ファイル（`armor.env`）に `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json` を追加し、`armor-server` を再起動します。
7. **確認します。** 管理者として `GET /api/v1/push/status` を呼ぶと `{"enabled":true,"devices":0}` が返ります。ビルドした APK をインストールしてサインインすると `devices` が 1 になります。Studio の「設定 → 通知」でテストを送ると、アプリを閉じていても端末に表示されます。
8. **届かない場合。** `enabled:false`：変数が未設定か、パスが違います（サーバーのログに理由が出ますが、鍵は含まれません）。`devices:0`：このビルドで端末がサインインしていないか、ビルドに `google-services.json` がありませんでした。何も表示されない：Android でアプリの通知を許可し、*Cloud Messaging API (V1)* が有効か確認します。

**セキュリティ。** どちらのファイルもコミットしたり、課題に貼り付けたりしないでください。漏えいしたサービスアカウントを無効にするには、コンソールでその鍵を削除します（「サービスアカウント」→「サービスアカウントの権限を管理」→「鍵」）。端末のトークンはサーバーのデータディレクトリに保存され、サインアウトすると、サーバーに連絡できる場合は削除されます（サインアウトは端末の Firebase トークンも破棄するため、以後サーバーのメッセージは届きません）。通知の文面はサーバーの言語であり、端末の言語ではありません。このリポジトリのリリースの APK はメンテナー自身のプロジェクトでビルドされているため、プッシュはそのサーバーに対してのみ動作します。ご自身の環境では APK を自分でビルドしてください。

## Português

1. **Crie o projeto.** Abra <https://console.firebase.google.com>, *Adicionar projeto*, dê-lhe um nome (por exemplo `armor-casa`). O Google Analytics não é necessário.
2. **Registe a app Android.** No projeto prima o ícone do Android (*Adicionar app*). O nome do pacote tem de ser exatamente `es.electrohobby3d.armor`. A alcunha é opcional e o SHA-1 não é preciso para o push.
3. **Descarregue o `google-services.json`** no passo 2 do assistente (pode fechar o assistente sem seguir os outros passos: este projeto não usa o plugin Google Services).
4. **Ponha-o na app.** Copie-o para `app/google-services.json`. O git ignora-o de propósito. Compile você o APK (`build.bat` / `build.sh`, ou `gradlew assembleRelease`). Uma compilação sem o ficheiro funciona mas não tem push.
5. **Obtenha a conta de serviço.** Definições do projeto (a roda dentada) → *Contas de serviço* → *Gerar nova chave privada*. Descarrega-se um ficheiro JSON. Em *Cloud Messaging* das mesmas definições confirme que a **Firebase Cloud Messaging API (V1)** está ativada.
6. **Entregue-o ao servidor.** Copie o ficheiro para a máquina do servidor, por exemplo `/opt/armor/etc/armor-fcm.json`, pertencente ao utilizador com que o servidor corre e com permissões `600`. No ficheiro de ambiente do servidor (`armor.env`) acrescente `ARMOR_FCM_SERVICE_ACCOUNT=/opt/armor/etc/armor-fcm.json` e reinicie o `armor-server`.
7. **Verifique.** Como administrador, `GET /api/v1/push/status` responde `{"enabled":true,"devices":0}`. Instale o APK que compilou e inicie sessão: `devices` passa a 1. No Studio, Configuração → Notificações, envie o teste: o telemóvel mostra-o mesmo com a app fechada.
8. **Se não chegar.** `enabled:false`: a variável não está definida ou o caminho está errado (o registo do servidor diz porquê, nunca com uma chave lá dentro). `devices:0`: o telemóvel não iniciou sessão nesta compilação, ou a compilação não tinha `google-services.json`. Nada é mostrado: permita as notificações da app no Android e confirme que a *Cloud Messaging API (V1)* está ativa.

**Segurança.** Nunca faça commit de nenhum dos dois ficheiros nem os cole numa issue. Para anular uma conta de serviço divulgada, apague a sua chave na consola (*Contas de serviço* → *Gerir permissões da conta de serviço* → *Chaves*). Os tokens dos telemóveis ficam no diretório de dados do servidor e um telemóvel é removido ao terminar a sessão se o servidor puder ser avisado (terminar a sessão também descarta o token do Firebase do telemóvel, por isso as mensagens do servidor já não o encontram). O texto das notificações está no idioma do servidor, não no do telemóvel. Os APK das releases deste repositório são compilados com o projeto do mantenedor: o push só funciona com o servidor dele, por isso compile o seu próprio APK para a sua instalação.
