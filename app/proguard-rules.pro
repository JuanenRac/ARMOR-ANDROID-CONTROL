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

# WorkManager (brought by the widget library) opens its Room database by name and calls the constructor without arguments. R8 in full mode removes a constructor that no code calls,
# so the app closed at start with "Failed to create an instance of androidx.work.impl.WorkDatabase". The generated database classes and their constructors are kept.
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
