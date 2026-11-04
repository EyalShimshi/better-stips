# Shrink and optimize, but keep names so crash reports stay readable without a mapping file.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# Glance creates the widget's refresh callback from its class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# Classes that libraries create by name, whose own rules (written as "-keep class X") no longer keep the constructor
# under AGP 9's R8 (#7). gradle.properties restores the old reading; these stay explicit in case it ever goes.
# WorkManager (through Glance) opens its Room database at every app start: Room calls WorkDatabase_Impl's constructor.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
# WorkManager creates an input merger by name for every job (the widget's).
-keep class * extends androidx.work.InputMerger { <init>(); }
