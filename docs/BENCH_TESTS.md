# Bench tests

The software of these checks is done and tested on a computer; what is left needs a real board, a real phone or the CM5. Nothing here has been run on hardware yet.

| Check | Connect | Run | Expected |
|---|---|---|---|
| Set up a node over Bluetooth | the APK and a real radar node | open "Set up a node" and configure it | the real node is configured |
| One notification | a phone and a Firebase project | arm, raise an alarm with the app in the background and with it open | exactly one notification with the alarm sound in both cases (the server must send data-only messages with an event id) |
| New token | a phone and a Firebase project | clear the data of Google Play services | the server receives the new token without opening the app |
| Memory with six cameras | a phone | show six cameras in a grid for 10 minutes and run `adb shell dumpsys meminfo es.electrohobby3d.armor` | memory does not grow and the app does not close |
| HTTPS to the CM5 | the CM5 with its own certificate authority | sign in over HTTPS, then turn cleartext off except for the allowed domains | the app still connects |
