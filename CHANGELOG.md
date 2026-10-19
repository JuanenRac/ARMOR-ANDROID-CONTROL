# Changelog

All notable changes to this project are documented here.

## [0.7.1] - The server link is an interface and refusals carry their code

- The app's link to the server is now an interface (`ArmorApi`); the HTTP client is one implementation of it, and the view model and the alarm notifier depend on the interface, so a test can stand in for the server.
- A refusal from the server carries its status, its short code and its words as one value (`ApiError`), read once, instead of every screen digging the code out of the message text.
- Batteries in the Solar tab show, when the cells are shown, the lowest and highest cell of each module for the day with the cell number and the time, and the widest gap of the whole bank today; they come from the server and are written in the eight languages.

## [0.7.0] - A switch for the update check at start

- The update check at start is now a switch in More > Updates, off by default: nothing asks GitHub on its own unless it is turned on, and a note says that the weather and the radar use external weather services. The button "Check for updates" still works at any time. In the eight languages.

## [0.6.9] - Updates are checked against the installed key, and smaller safeguards

- A downloaded update is compared with the installed app before Android's installer is opened: if it is signed with another key the app says so in plain words and drops the file, instead of leading the person through the permission screens to a refusal. A key that was rotated from the installed one still matches, and when either side cannot be read Android decides, as before.
- A role the app does not know only looks: besides the viewer and the wall-panel roles, any other role the server might name later gets no arm, disarm or camera buttons (the server refuses what is not allowed anyway). An empty role, not told yet, is unchanged.
- After too many wrong PINs the message says how long to wait ("try again in 45 s", "in 3 min") when the server tells it, and keeps the old sentence when it does not.
- The sign-in notes when the server address is a public IP address: it works until the provider changes it, so the app suggests a name that follows the house (dynamic DNS). A name or an address of the house shows nothing.
- Nothing of the app is taken in a backup or a transfer to another phone, also by the rules Android 12 and later read (the saved address, the sealed sign-in and the push token stay on the phone).
- A test reads every text of the screens and fails when one has words of its own outside the translation, so a new text cannot stay in Spanish in the other languages; a file that only held an unused helper was removed.
- The new phrases are in the eight languages, and the tests cover the comparison of keys, the roles, the wait, the address and the translation of the texts.

## [0.6.8] - Restored sessions

- **A kept session now really is accepted after the app is reopened:** the cookie put back from the vault was sent in the old versioned form (`$Version="1"; name="value"`), and the server read its value with the quotes and never found the session, so the sign-in was asked for again. It is now sent as a plain `name=value` cookie.
- More tests: the HTTP client against a real local server (headers, refusals, redirects, encoded paths, tickets, sign-in steps, PIN and panel commands), the state of the screens (roles that only look, sign-in, second factor, proof for critical actions, sign-out) and every parser against answers of the wrong shape.

## [0.6.7] - Connections in the history

- The History screen has a second, read-only list: the devices and apps that connect to the server (Studio, this app, the panels of the nodes, the voice service), when each was last seen, its open sessions and its sign-ins and refusals, refreshed while it is open. The address is shown only when the signed-in user is an administrator.
- The app says what it is and which version in every call to the server (`X-Armor-Client`), so it appears in the history of connections as itself.
- The new phrases are in the eight languages, and a test reads the list tolerantly (unknown kinds and malformed entries are left out).

## [0.6.6] - Remember me on the sign-in

- The sign-in has an optional **Remember me** box (off by default): the user and the password are sealed with a Keystore key of their own, for that server only, and filled in again when the session has ended.
- Unticking the box forgets the saved user and password at once, and they are only kept after the server accepted the sign-in.
- The note under the sign-in says which of the two is true (password not kept, or kept encrypted), in the eight languages.
- Tests cover the vault: the right server, a different key, line breaks, nothing readable on disk, and clearing.

## [0.6.5] - Node health and arming modes

- A Node health screen in More: for every node its kind, firmware, uptime, why it last restarted (a crash is marked as unexpected), free memory, link and Wi-Fi signal in words, broker connections, restarts of the last day, tasks short of stack, the last error and the warnings the server raised; the nodes that need a look come first, and the nodes that sent no report are named.
- The Alarm panels screen offers a mode picker (Away, Home, Night and the owner's own, with a summary of the chosen one) and a button that arms in it, for a panel whose firmware says it understands the modes; an older panel keeps the two buttons and says why. The card and the recent orders name the mode.
- Eight languages for every new phrase, and unit tests for the readings and the wording.

## [0.6.4] - Silence the siren of a panel from the phone

- The alarm panel screen shows a Silence siren button while a siren sounds (administrators, when the server allows commands); it asks for the PIN, which the server checks and never sends to the node, and the system stays armed.
- The event list names the new event siren silenced with the PIN; phrases in the seven translation tables; model tests cover the new wording.

## [0.6.3] - A zone in trouble is shown, and the app is signed with its own release key

- A zone that the alarm node cannot read is shown as cannot be read (in the amber of a warning) in the alarm panels, and the warning that the server raises for it has its text in the eight languages.
- The app is signed with the owner's own release key, kept outside the repository, instead of the key of a development computer. Android treats it as a different publisher: the first update from an older version asks to uninstall the app and install this one once; after that, updates install over it.

## [0.6.2] - Signing in from outside the house is explained, and the status screen shows the whole system

- **Signing in from outside the house:** typing a public address (or a name) without https:// used to leave the Enter button disabled with no explanation. The sign-in now says why, offers a one-tap "Use https", and a guide (VPN or public address with https) explains every step, in the eight languages.
- **The status screen shows more:** the alarm panels (how many, armed, sounding, without signal), the services of the system (running and failed), the cameras that are recording, and the security and remote access (second factor, VPN, sessions from outside); each card opens its screen.
- The status screen refreshes the new cards every ten seconds while it is open.

## [0.6.1] - The Security screen scrolls

- The Security screen (second factor, open sessions, remote access) now scrolls, so every card below the first can be reached on a phone screen.

## [0.6.0] - Security: second factor, open sessions and the VPN and the dynamic DNS name from the phone

- **Two-step sign-in:** when the account has the second factor on, the sign-in asks for the six-digit code of the authenticator app (or a recovery code) as a second step, and says why when the server asks for it from outside the house and it has not been set up.
- **A Security screen in More:** set up the second factor with your password (the key and a link that opens the app), confirm it with the first code, keep the ten recovery codes shown once, and turn it off; see the sessions that are open (the house or outside, address, last use) and end one or all the others; and a button that shows how the server sees this phone.
- **A new proof for critical actions from outside:** a refused critical action opens a dialog for the password (or a code), and the action is tried again by itself.
- **The VPN, for an administrator:** set it up (the name or address the phone will use, the port, the network of the house), add a device and copy its configuration (it holds a private key, so it is shown with a warning), show it again or remove the device; the card says which UDP port to forward on the router.
- **A name that follows the house:** DuckDNS, Cloudflare or any address to call; the token is typed once and never shown again, and the card shows what is kept up to date and the last address told, with buttons to update it now or stop it.
- **The panels of the nodes through the server** are registered and trusted from Studio; the card says so and where.
- **The delete button of the library** is no longer offered to a viewer or a wall panel (the server refused it anyway).
- **95 new phrases in the seven tables**, checked by the test that every phrase of the source is in every table.
- **Tests:** 130 (before 125): the sessions, the second factor, the view from outside, the VPN and the DNS state are read as sent, the codes of the errors have their own phrases, and every phrase is in every language.

## [0.5.9] - The real error of an action reaches the person, a duress alarm stays silent and every role sees what it may

- **The error of an action is told as it is.** A wrong PIN, a locked PIN, a wrong password, a refused order and the rest were all shown as *the server could not do it*: the message of the error was lost to a name clash in the code. They show now, which also stops a person from trying a locked PIN again and again.
- **The alarm of a duress PIN is silent on the phone:** it is not notified, spoken, listed in the alarms or the history, or counted (the server tells the people who must know in silence; a phone that sounded in front of whoever forced the person to disarm would defeat it).
- **Cameras for every role:** a viewer could not watch any camera (the server asks for a stream ticket from that role); the app now asks for one, for everyone, and falls back to the plain address with a server that has none. The *views* picker is now how many cameras a page shows, with arrows for the rest: before, the cameras after the first few were not shown at all.
- **Roles:** only an administrator is offered the arming of the alarm panels (the server refuses anybody else), a viewer or wall-panel user no longer has the photo and recording buttons, and the dialog for disarming a panel says to type your PIN (it said there was none).
- **Notifications:** the channels an older server names for its push messages exist here too, with the sound of an alarm, so that a critical alarm that arrives with the app closed still sounds; signing out drops the phone's Firebase token, so a phone that could not tell the server (a viewer, a server that is down) stops receiving alarms.
- **Changing the server in Settings signs the person out of the previous one** (before, the next refresh failed with *the session expired*).
- The spoken alarms use the language of the app and try again when the voice engine failed once; the stop of a held camera key goes out even when the screen closed with the finger down; the search for missed alarms stops at an empty page.
- Texts that were not translated now are: the errors of the video, *N people*, *N units*, *fault*, *seen by ... still active*, the kind *interior* and the geocoding language of the weather; a node that is an alarm panel or a touch panel is named as such in the node set-up.
- Robustness: a media item or a camera with a missing field no longer fails the whole list, a failed connection is closed, and the tables of texts and the key of the session are safe to use from the service and the screen at once. The page of the weather radar no longer interprets a place name as HTML.
- **HTTPS with the installation's own authority:** the app trusts the certificate authorities the person installed on the phone as well as the system's (a network security configuration), so the TLS profile of ARMOR-DEVOPS, whose authority is made locally, can be used; plain HTTP is still limited by the app to a private address.
- The documents say what the app does today: the sign-in with a Studio user and its roles, the stream tickets, the duress alarm, the Bluetooth pairing with its PIN and the five kinds of node, and that the APK is signed with the maintainer's Android debug certificate until a release key is made (the README no longer says otherwise).
- Tests: 125 unit tests; new ones cover the silent duress alarm (event and alarm parsers, the notices), the end of the search for missed events and the kinds of node.

## [0.5.8] - Push alarms through the installation's own Firebase project

**Push when the app is closed:** the phone registers its Firebase token with the server once signed in and takes it back at sign-out; the server's alarms then reach it as notifications on the channel of their severity (critical ones sound like an alarm), whether or not the app is open.
It is the installation's own: the build reads `app/google-services.json` (never kept in the repository) and, without that file, has no push at all and works as before. The steps to get the file are in docs/PUSH_NOTIFICATIONS.md.
A new server route lets a phone take its token back with a POST, for HTTP stacks that send no body with DELETE.

## [0.5.7] - Portuguese for the voice assistant

- The voice assistant of the server is now told the language of the app also in Portuguese, so a phone set to Portuguese gets its answers in Portuguese instead of English.

## [0.5.6] - Public address in Network and a read-only role

- The Network screen shows the public IP address of the house, the provider and the place (with the country in the language of the app), as the network node reports it.
- A user with the viewer role now only looks: the app reads the role of the session, the arm and disarm button, the acknowledge buttons, the switches of the devices, the commands of the alarm panels, voice orders, the camera pad, deleting recordings and the network sweep are refused or off, with a clear message.
- A server that does not say the role leaves all the buttons, and the server still refuses what is not allowed.
- Tests cover the read-only role and the parsing of the public address.

## [0.5.5] - The app in eight languages

- The whole app is now in English, Spanish, German, French, Italian, Japanese, Chinese and Portuguese: every screen, button, message, notification and error, about 830 phrases in seven translation tables.
- A language picker in More > Settings; without a choice the app follows the phone's language when it is one of the eight, and English otherwise. The change shows at once, with no restart.
- The alarm notifications and the spoken alarm use the language of the app, and the voice commands are heard and answered in it (the server's voice assistant is told English when the language is Portuguese, which it does not know yet).
- The weather shows its days and months in the language of the app, written by the phone's own calendar.
- The Bluetooth set-up of a node now tells you to type the PIN that the node shows on its USB console when Android asks to pair.
- Tests keep it complete: every phrase of the source has its text in all seven tables, a translation keeps the holes of the original, the tables have the same phrases and the language choice falls back to English.

## [0.5.4] - Check before arming, sounds by severity and spoken alarms

- Arming now runs the server's check first and lists what would leave the perimeter blind or noisy: a silent radar node, an open zone, a camera that does not answer, an open door or window.
- When something blocks, the dialog says so and the button reads "Arm anyway", so arming stays possible but never by accident.
- Critical alarms and warnings now use separate notification channels, so you can silence the quiet ones without losing the loud ones.
- Alarms can be spoken aloud when they arrive, with the text of the alarm.
- A new Sounds and voice panel in More chooses which severities make a sound and whether alarms are spoken.

## [0.5.3] - Sturdier parsing, per-origin alarm tracking, a media viewer and a watcher that survives a reboot

- A field the server sends as null no longer shows as the word "null": every model reads text through one helper.
- An alarm is tracked per origin and expires after a limit, so a repeated condition wakes the phone again and an old alarm does not.
- Events are merged and de-duplicated, and a history request asks only for what is newer.
- Photos and recordings open in a viewer inside the app, and deleting one asks first.
- The live picture reads the structure of each JPEG instead of guessing its end, and decodes at a smaller size when the screen is small.
- The Bluetooth set-up honours the back button, limits the networks it scans and checks the job before it reports success.
- The address of a node accepts only four plain octets, and the panel of a node is opened with a token bridge that is closed with the screen.
- The background watch starts again after the phone reboots, as a foreground service of a declared type.
- Signing out forgets the session, and the login cookie is saved only when it changed.
- The update check downloads once, follows only https redirects and shows a single progress.
- Settings are edited as a draft and saved together, and the state updates are atomic so two refreshes cannot lose each other's changes.

## [0.5.2] - Roller shutters, gate and garage door motors and water heaters

- **Devices:** the new kinds of the server - motorised roller shutter, gate motor, garage door motor and water heater - with their names, icons and cards; the motors and the shutter have the open and close buttons (the water heater, the switch).
- Tests for the labels of the new kinds and the words of their commands.

## [0.5.1] - A sounding central wakes the phone, and the PIN to disarm

- **An alarm of an ARMOR-ALARM node** (sounding, a zone in tamper, locked out, gone quiet) is told like the other alarms, armed or not, and its source is named by node and zone. Before, `source_type` `alarm` was not on the list and the phone stayed silent. The app has not been compiled with this change: only the unit test was written.
- **Disarming asks for the PIN** (the system, and an alarm panel) in a dialog with a numeric field; a wrong, locked or missing PIN is told in words. The alarm of a disarm under duress has its sentence. The app has not been compiled with this change.

## [0.5.0] - The alarm panels, and the electrical devices of the house

- **More > Alarm panels:** the ARMOR-ALARM nodes. One card per panel with its phase and mode (disarmed, leaving, armed, entry delay, ALARM; away or at home), the siren, its zones (kind and closed, open, tamper or left out) and its last events told as how long ago; the node finder is there too. The screen reads the panels every three seconds while it is open.
- **Arm and disarm from the card**, only when the server (`ARMOR_ALARM_COMMANDS=1`), the node and the broker all allow it (the screen says so when the server does not). A disarm carries no PIN, so the app asks first; a command the server refuses is explained in Spanish, and what the node answered (accepted, a zone is open, it did not answer) shows on the card. An accepted command is not an armed alarm: the card follows what the node says next.
- **The alarms of a panel** have their own sentences: it is sounding, a zone shows tamper, the panel is locked out after wrong PINs, the node stopped answering.
- **Devices:** the circuit-breaker and energy-meter kinds with their voltage, current and energy, and a command to a device that matters (a circuit of the board, a critical one) asks first and goes with the confirmation the server wants.
- **Alarms:** the solar alarms (cells unbalanced, battery hot, cold or worn, inverter hot) and a watched device that came back onto the network are named in Spanish.
- **Tests:** the parser and the wording of the panels, the summary line, and every new alarm code having a sentence of its own.

## [0.4.9] - The configurator of the nodes inside the app, and more commands

- **More > Configure nodes:** the configuration panel of a node (radar, solar, electrical) inside the app - its own page, with every menu and parameter, the login, the firmware update and the import and export of the settings - as if it were opened in a browser. Choose one of the nodes the network node found, or type its address (only an address of the local network is opened). The page can ask for a file (the settings to import, a firmware) and the phone's file picker answers; the exported settings are saved in Downloads; and the panel only goes where the node is. Back goes back through the pages of the panel and then out.
- **Assistant:** fifteen commands instead of four, with fifteen phrases to tap - the alarms, the nodes, the cameras, who the radars see, the solar system, the consumption, the network, the time, help, and lights on and off - next to arm, disarm, the state and silence.
- The alarm of a camera that saw movement (the observation service) is named in Spanish.

## [0.4.8] - The Assistant: written and spoken commands

- **More > Assistant:** send commands to the system as text or by voice. Type one, tap one of the four suggestions (*System state*, *Arm the system*, *Disarm the system*, *Silence the alarm*) or press the microphone: the phone's own speech recognition (the system's dialog, so the app asks for no microphone permission) turns what you say into text and it goes the same way. The server asks the voice gateway what it means and carries out what is accepted with your session. Arming and disarming ask for a second turn, shown as *Confirm* and *Cancel* under the question. The answer is also said aloud by the phone's voice (a switch turns it off) and the conversation can be cleared. It says so when the system has no voice gateway installed.

## [0.4.7] - The weather map without its diagnostic line

- The plain-text line under the weather map (state of the page, errors, WebView version), which found why the map had no height, is gone now that the map works; the map keeps the height the app gives it.


## [0.4.6] - The weather map had no height

- **Real bug, found with the diagnostics of 0.4.4:** the page reported `map size 356x0`: the WebView gave the page a viewport of no height (width was right), and a map that takes all the height of nothing is a map of nothing, so only the buttons showed over a black box. The app now tells the page the height of its box (`?h=`) and the page gives the map that height outright, whatever its viewport says.

## [0.4.5] - The alarm watcher backs off when the server is down, and the version is checked

- **Alarm watcher:** while the server does not answer, the interval between attempts doubles (10 s, 20, 40, up to a minute) with a little jitter instead of staying at 10 s, and the notice says how long it has really been unreachable.
- **Version check:** the build stops when `versionName` and the project's manifest (`armor.project.json`) differ, so a release can no longer carry a number the manifest does not say.


## [0.4.4] - The weather map says what it is doing, and the video no longer grows a list of bytes

- **Weather radar:** under the map, in plain text that stays readable even when the page paints nothing, the app shows the state of the page (loading, loaded, percentage), the errors of its JavaScript, the failed requests and the version of the phone's WebView, so a map that does not appear says why. The map now fills its box with fixed positioning and measures its size again when the WebView gives the final one (it could be measured when it was still empty and then never draw), and a failure of the page's script is reported before anything else runs.
- **Camera video:** a picture of the stream is read into a byte buffer with a limit (8 MB) instead of a list of single bytes, which was slow and used far more memory per picture.

## [0.4.3] - The weather map no longer comes out black on phones that paint it badly

- **Weather radar:** on some phones the WebView paints the 3D-transformed pictures of Leaflet as black squares, so the whole map was black. The map now uses plain 2D transforms and no animations, the WebView has the page's own colour behind it (a page that does not paint is told apart from a dark map), and the bottom of the map says how many pictures of the base map arrived and how many failed, so a map that stays dark says why.

## [0.4.2] - The weather radar loads again, the session is remembered and the site radar fits the screen

- **Weather radar:** the radar page is now served from a private https address that the app answers from its own assets, instead of being opened as a file (recent Android versions refuse the file permissions it relied on). If it still cannot load, the screen says so and offers *Reintentar* instead of showing an empty box. A map inside the list pans with the finger instead of scrolling the list.
- **Signed in between launches:** the app keeps the server's session cookie, sealed with a key held in the Android Keystore, and opens already signed in (also for the alarm watcher service). The password is never stored. A session the server has ended, or a changed password, is refused and simply asks for the login again; signing out forgets it.
- **Radar tab:** the 2D and 3D views now fit the real content (ground, buildings and the field of every radar) and are centred on it, instead of fitting a box that included the empty space around the origin, which left the site small, far away and off-centre.

## [0.4.1] - The radar map still showed nothing on a phone that could not reach the CDN

- **Real bug, reported by the user:** 0.4.0 had already fixed the WebView's cross-origin block, but the radar still showed nothing on their phone - not even the base map, which needs no special permission at all. The actual cause: Leaflet itself (the map library) was loaded from `cdnjs.cloudflare.com` at runtime; Studio never has this problem because it gets Leaflet from its own build (an npm import), not a CDN. Leaflet is now bundled in the app (`assets/leaflet/`), so the map never depends on reaching that CDN.

## [0.4.0] - Weather with its live radar, Services, node finding and the switches of the electrical nodes

- **Weather** (More menu): the weather of the place you choose - by name - because guessing it from the network would be wrong. Shows the conditions now, the rain of the next hour, warnings the forecast implies, the next 24 hours, ten days, the air quality and pollen, the sun and the moon, and a live radar of rain (last two hours, animated) and of clouds (Meteosat infrared) over a map. Nothing is asked of the Internet until a place is chosen, and only its coordinates leave the phone; it never goes through ARMOR-SERVER.
- **Services** (More menu): every program of the system and every field node, running or not, grouped by family (Core, Network, AI and voice, Field nodes), with its state, systemd unit details, PID, port, memory, restarts and how long it has been up. Read only. Needs ARMOR-SERVER 0.3.9 or later.
- **Find nodes on the network** (Solar and Electrical menus): a list of what the network node has found that looks like an ARMOR node and is not yet known here, with a button to search now and a link to the node's own panel.
- **Electrical:** each node's transfer switches (which source is in use, the state of both sides, whether it is closing, a fault), and a notice when a node has its switching disabled.
- **Network:** the list of outages now shows when each one ended, not only when it started and how long it lasted.
- **The radar did not show anything:** the bundled page that draws it could not fetch the rain and cloud services at all - Android's WebView blocks a `file://` page from making cross-origin requests unless that is explicitly turned on. Also logs its own errors now, and says on screen when a service could not be reached instead of staying silent.

## [0.3.9] - An eye to check what was typed, on the node's own screens

- **Eye on every password of a node's set-up** (the set-up code's secret, the administrator's password, the node's Wi-Fi, the broker): tap it to check what was typed instead of guessing. The app's own sign-in screen already had it; it was missing here.

## [0.3.8] - Configuring a node over Bluetooth no longer ends with the node vanishing

- **Real bug, reported by the user:** after giving a node the Wi-Fi of the house over Bluetooth, the node restarted and neither joined the Wi-Fi nor could be found again, and the app only said "open it by its address". The app now looks for the node again by Bluetooth after it restarts and reads, from its `hello`, whether it joined (its address and network) or why it did not (network not found, wrong password, or only "it did not connect"), with buttons to check again or to change the configuration.
- **Fixed address or DHCP on the Wi-Fi too:** the address block (DHCP or fixed address, mask, gateway, DNS) is offered whichever connection is chosen; before it was only shown for the cable.
- **The form is checked before it is sent:** a Wi-Fi name is needed, a Wi-Fi password has 8 to 63 characters, and a fixed address, mask and gateway must be valid and in the same network.
- Needs the node firmware that reports why it did not join (ARMOR-RADAR 0.3.2, ARMOR-SOLAR 0.1.1, ARMOR-ELECTRICAL 0.0.7); with an older firmware the app says only that it could not tell.
- **Tests:** the patch with a fixed address on Wi-Fi, the form checks and the reading of the outcome from the `hello`.

## [0.3.7] - A GitHub rate limit looked like a broken update check

- **Real bug, reported by the user:** checking for updates failed with "GitHub devolvió el código HTTP 403." - technically accurate, but it gave the operator no way to tell a genuine problem from GitHub's own unauthenticated rate limit (60 requests/hour, shared by every device on the same home network's public IP, not per-app or per-phone) simply being spent for the hour. `describeHttpFailure()` now reads the same `X-RateLimit-Remaining`/`X-RateLimit-Reset` headers GitHub's own response already carries and, only when they say the ceiling is genuinely spent, explains that in plain Spanish with a real estimate of when to try again - any other HTTP failure still reports its real status code plainly, nothing is hidden.
- **Tests:** 6 new JVM tests for the message itself (spent limit with/without a usable reset header, a reset timestamp already in the past, a genuine unrelated 403, budget still remaining, an unrelated HTTP status) - a pure function, no mocked `HttpURLConnection` needed.

## [0.3.6] - Checking GitHub for a new version

- **A real update channel, same design as HYDRA-UMC-ANDROID-CONTROL's own:** on every cold start the app asks GitHub's own release feed (`GET /repos/JuanenRac/ARMOR-ANDROID-CONTROL/releases/latest`) whether a newer *stable* semver tag exists, and offers it through a dialog and through a new *Más > Actualizaciones* screen. Nothing downloads or installs on its own: the operator has to tap Descargar e instalar, and Android's own package installer still makes the final signature check before replacing the app. A draft or prerelease tag is never offered.
- The APK is downloaded straight from that release's own asset (must be named exactly `ARMOR-ANDROID-CONTROL-release.apk`), verified to be a real Android package for this exact `applicationId` and genuinely newer than what is installed before the installer is ever opened; a real, bounded manual-redirect follow keeps GitHub's own required headers on the hop from `api.github.com`/`github.com` to its asset host. Uses `java.net.HttpURLConnection`, the same stdlib-only HTTP the rest of this app already talks to ARMOR-SERVER with - no new networking dependency.
- **Tests:** 7 on the JVM (stable-tag parsing and ordering, the release-metadata trust gate: rejects a non-stable tag, a missing asset, a non-HTTPS URL, a draft/prerelease, or a release that is not actually newer). The debug APK builds. The update flow itself has never run on a phone.

## [0.3.5]

- A GitHub Actions CI baseline (`.github/workflows/ci.yml`): validates the manifest, the version, CHANGELOG.md's heading, the seven README translations' structure and its own local Markdown links, then runs this project's real build/test through `tools/armor_project_tool.py build-test .` (vendored from ARMOR-COMMON, alongside `tools/armor_ci_validate.py` and `tools/_armor_readme_parity.py`, which do the manifest/docs checking).

## [0.3.4] - The local network

- **A *Red* screen** (in *Más*, and a tile on the Status screen): whether the internet is there and, when it is not, whose fault it is (the provider's or this side's), its latency and loss, the outages of the day, the devices of the network with their address, kind and maker (the ones nobody marked as known, and the ones with a port a house rarely wants open, stand out) and what changed. Reading only; it reads what ARMOR-NETWORK nodes report to the server.
- The alarms of the network (the internet or the local network down, a device that was never seen, two machines for one address, a port that opened, a node that went silent) have their own wording and wake the operator, like the solar and electrical ones.

## [0.3.3] - The electrical network

- **An *Eléctrica* screen** (in *Más*, and a tile on the Status screen): the power of the grid input, whether the house draws from the network or feeds it, every channel the electrical nodes measure (voltage, current, power, energy, frequency, power factor, the state of a switch) and the alarms of the meters, refreshed every five seconds while it is open. Reading only.
- **The alarms of the electrical nodes** (a meter's alarm, the mains out of range, the grid lost, a node that stopped answering, a fault of a source switch) have their own wording and wake the operator, like the solar ones.
- **An inverter's card** shows its second PV input and, for a parallel system, its units and the total power.
- **Tests:** 62 on the JVM (the electrical answers, the wording, the second input and the units, the new alarm codes); the debug APK builds. Not run on a phone.


## [0.3.2] - Configuring the three kinds of node over Bluetooth

- **One screen for the radar, the solar and the electrical node.** They answer the same Bluetooth protocol (the same service and characteristics), so *Configure a node* finds any of them by its `ARMOR-XXXXXX` name and sets its name, Wi-Fi, address, broker and Bluetooth mode. After connecting, the screen says **which kind of node** it is (the node declares it in its `hello`; a node that predates that is told by its identifier when it says so).
- **Plain words for what a node refuses:** asking a Wi-Fi-only board for the Ethernet cable, or leaving it with neither Wi-Fi nor its own access point, now says why instead of showing the field code.
- **Tests:** the kinds of node and the refusals are checked on the JVM with the rest of the protocol.
- **Not done:** the radio side has never talked to a node of any kind (no phone and no board have met yet).

## [0.3.1] - The names of the new inverter and battery models

- The solar screens name the models of the server's new catalogue (Axpert, PIP, Revo, InfiniSolar, LV5048, SunGoldPower, the Pylontech and Pytes families) and read an ANT-BMS combination (`ant-bms-24s-200a` shows as ANT-BMS 24S · 200 A); a model it does not know is shown by its identifier.
- A battery's screen shows its **health** (its capacity against new).

## [0.3.0] - Solar alarms wake the phone

- **A solar alarm is announced** like a device alarm, once when it is raised and whether or not the system is armed: an inverter fault (high), a battery that is low or that reports a protection acting (warning or high), and equipment that stopped answering (warning). The notice reads like the others (*ALARMA ALTA · Avería en un inversor solar · casa/axpert-1*), in the app and in the background watch. Acknowledging or clearing it announces nothing.
- **The Alarms screen names solar alarms** by the equipment's own name (the one declared in Studio) instead of its `node/device` path, and the four solar codes have their wording (*Avería en un inversor solar*, *Batería solar baja*, *Una batería solar avisa de un problema*, *Un equipo solar ha dejado de responder*).
- Tests: 55 (was 53). Not tried on a phone.

## [0.2.9] - Solar inverters and batteries in the app

- **A Solar screen** (*Más > Solar*, and a tile on the Status screen that shows the sun, the consumption and the batteries' charge): the sums the server makes (sun, consumption, batteries with their charge and whether they charge or discharge, whether the grid is present and the inverter's mode) and, below them, every inverter and every battery stack that has reported, with its numbers. It is refreshed every five seconds while it is open.
- **An inverter** shows the grid, the load, the panels, the battery side, the temperature and its warnings by name; **a battery** shows its charge, voltage, current, power, temperatures, the range of its cells with the spread in millivolts, capacities and cycles, an alarm line when a protection is acting, and, on demand, **every cell** with the highest and the lowest marked and the temperature sensors (an ANT-BMS lists them all).
- **Honest about the state of the data:** equipment declared in Studio that has not reported yet is listed as *waiting for data*; a device that has gone quiet says *sin señal*; a reading the server made up to try the menus says *ejemplo*. A field the server did not send shows a dash, never a zero.
- Tests: 53 (was 48), for the reading of the server's answer (inverter, battery with cells and sensors, totals, the waiting list, a null total, an odd or empty answer) and the wording. Exercised in an Android emulator against a local server with declared equipment and example readings; never against a real inverter or battery.

## [0.2.8] - Live radar, in 2D and in 3D

- **A Radar tab** in the bottom bar (second place): the site as Studio designed it, with the people the radars see moving on it, refreshed every second and a half while the tab is open. The design is read from the server (`GET /api/v1/site`): the ground, the buildings (walls, floors, gable and hip roofs), the objects on the ground, the cameras' and radars' fields, in the colours chosen in Studio.
- **2D**: north up, drag and pinch, double tap to centre, a 5 m grid and a scale bar; each person is a dot with their track number, a pulse when they count for the alarm and grey when an ignore zone hides them.
- **3D**: a real projection with depth: drag to turn, pinch to zoom, double tap to reset; the buildings with their roofs, trees, posts, masts and fences, the radars' fields on the ground and a standing figure for each person.
- The people are placed with the same rules as Studio's live radar map (which radar of the design produces which node's radar number, which side is positive x, the rated sector of each model), tested with the same numbers. A radar of the design that is not wired to a node in Studio shows nothing; a node that reports people no radar is wired to is said so on the screen. Not drawn on the phone yet: the ignore-zone rectangles, the trails, the doors and windows of the buildings and the devices' places.
- Tests: 48 (was 39), for the geometry and the reading of the site document. Exercised in an Android 14 emulator against a private server with a designed site and two simulated people walking (both views). Not run on a phone, and never with a real radar.

## [0.2.7] - A new look: icons first, plain words

- **A redesigned app** in the look of A.R.M.O.R. (the same dark surfaces and cyan as Studio) and with the structure of the other apps of the family: a splash with the mark, a top bar with the name, the state of the system and the connection, and a bottom bar of five icons (Status, Alarms, Devices, Cameras, More) that shows a word only under the one that is open.
- **Icons instead of text everywhere**: arming is one big power button on the status card; alarms, devices and cameras have an icon per kind (smoke, door, motion, plug, light, siren, lock...); the camera pad, photo, record and full-screen are icon buttons; devices are tiles with a switch.
- **Sign out** is an icon in the top bar, in *More* and in the account box, always asking first. New **account box** (user, server, connection, alerts with the app closed), a proper **About** with the mark, what the app does and who made it, and a launcher icon.
- **Fewer technical words**: no revision numbers, ports or ids on the screens; messages of the server and the network are turned into sentences ("Usuario o contraseña incorrectos", "No se puede conectar con el servidor. Comprueba que el móvil está en la misma wifi") and times are shown in the phone's time zone.
- The back button goes up one level. The sign-in screen has a show/hide password eye and keeps the last user.
- Tests: 39 (was 35), for the plain-words rules and the time zone. Nothing of the server's API changed. Checked in an Android 14 emulator against a local server (sign-in, status, alarms, More, settings, About); the cameras and devices screens were not exercised with real cameras or devices, and it was not run on a phone.

## [0.2.6] - Configure a field node over Bluetooth

- **Configurar un nodo por Bluetooth**, from the login screen and from *Más > Conexión*, so the same app does everything: it finds the nodes that advertise `ARMOR-xxxxxx`, connects (Android pairs when asked), creates the administrator of a new node with its set-up code (or with the fleet secret, from which it computes the code of the node's MAC) or signs in to one that has users, searches for Wi-Fi networks, and sets the node's name, the router's Wi-Fi or a fixed address, the broker and the Bluetooth mode, then restarts it. It is for nodes with no Ethernet cable or no address yet.
- New permissions: Bluetooth scan and connect (never used for location), and location only up to Android 11, where scanning needs it. The Bluetooth link is compiled and its framing, requests, set-up code and settings are unit-tested (35 tests, 10 new); it has **never run against a node or a phone**.

## [0.2.5] - Arm and disarm, alarms, devices and device alarms on the phone

- **Arm and disarm** from the phone, after a confirmation that says what changes; the server records it with the signed-in user's name.
- **Alarms menu:** what needs a person right now with the source (the device's own name), how serious and since when; acknowledge one or all; the closed record. The bottom bar shows how many are waiting.
- **Devices menu:** every sensor and actuator the server holds (smoke, gas, flood, door, window, motion, climate, plugs, lights, sirens, locks, valves) with its state, filters (all, sensors, actuators, needing attention) and On / Off / Toggle for what can be commanded. Devices are added and placed from Studio.
- **A smoke, CO, gas, flood or panic alarm wakes the phone** even while the system is disarmed, and a door, window, motion, glass-break or vibration alarm does while armed; the event is announced once. Node and camera alarms are not announced a second time.
- The overview shows the alarms waiting and the devices needing attention; the history reads device and alarm events. Recordings, history and connection are now under *Más* to keep the bottom bar at five places.
- An older server without alarms or devices keeps working: those screens stay empty instead of failing the refresh.
- 25 unit tests (was 7).

## [0.2.4] - PTZ that stops

- PTZ keys work by holding (press moves, release stops after at least 300 ms, repeated every second), commands go out in order, only failures are shown, and each camera tile has a PTZ toggle that shows the pad over the picture. Not run on a phone.

## [0.2.3] - History, alarms and camera health

- New **Historial** section (every alert, node, camera and mode change) and the radar list moved into **Estado**; camera tiles say when a camera does not answer.
- Alarm notifications: a node reaching HIGH, a camera that stops answering and, while armed, a node that goes offline or silent. An event is never announced twice and the first launch does not replay the history.
- Optional background watch (a foreground service with a permanent notification) that uses the current session; when it expires, or Android ends the service, it says so instead of failing silently.
- Everything refreshes every ten seconds while the app is on screen. 14 unit tests. **Not run on a phone**: the notification permission flow and the service lifecycle are unverified.

## [0.2.2]

- Plain HTTP is now accepted only towards private-LAN or loopback IPv4 literals; host names that
  merely look like a private address and addresses with leading zeros are refused.
- Applied the ARMOR look (near-black surfaces, cyan accent, amber for attention).
- Added unit tests for the endpoint-safety rules and documented the client boundary.

## [0.2.1]

- Added server IP, port, username and password login before the mobile console.
- Reused the ARMOR-SERVER HttpOnly Studio session for live-camera controls,
  PTZ, snapshots, recording and evidence; no operator token is requested or
  stored on the phone.

## [0.2.0]

- Replaced the Android shell with an ARMOR-SERVER mobile operator client.
- Added real MJPEG camera monitoring, PTZ, snapshots, recording control and
  evidence-library operations through the server session boundary.
- Added a project Gradle wrapper and endpoint-validation regression test.
