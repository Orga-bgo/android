# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep root manager classes
-keep class de.babixgo.monopolygo.RootManager { *; }
-keep class de.babixgo.monopolygo.AccountManager { *; }

# OkHttp and Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# AccountInfo Klasse
-keep class de.babixgo.monopolygo.AccountManager$AccountInfo { *; }
