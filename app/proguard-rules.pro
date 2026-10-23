# ARMOR-ANDROID-CONTROL - rules for R8 (the release build is shrunk and obfuscated).
# The app reads its JSON by hand (org.json), has no reflection of its own and no serialisation library, so nothing of its own needs to be kept. Firebase, the coroutines and Compose
# ship the rules they need with their libraries.

# A crash report of a release should still say where it happened.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The services and receivers the manifest names are found by name by Android.
-keep class es.electrohobby3d.armor.AlarmWatcherService { *; }
-keep class es.electrohobby3d.armor.BootReceiver { *; }
-keep class es.electrohobby3d.armor.PushService { *; }
