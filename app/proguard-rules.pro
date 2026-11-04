# Shrink and optimize, but keep names so crash reports stay readable without a mapping file.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# Glance creates the widget's refresh callback from its class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
