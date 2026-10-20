# ---- StbEmu-Clone ProGuard-Regeln -------------------------------------------
#
# WICHTIG: Die JavaScript-Bridge des STB-Emulators wird ueber Reflection und
# addJavascriptInterface() an das WebView gebunden. ProGuard darf diese
# Klassen, Methoden und deren @JavascriptInterface-Anmerkungen NICHT entfernen
# oder umbenennen - sonst bricht jedes Portal sofort mit "undefined is not a function".

-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions

# 1) Komplette JS-API-Schicht erhalten
-keep class com.mclone.stbemu.core.js.** { *; }
-keep interface com.mclone.stbemu.core.js.** { *; }

# 2) Der JS-Bridge-Dispatcher nutzt Reflection ueber getMethods()
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 3) Firmware-/Modellprofile werden per Annotation-Key (@Model) in einer
#    Multibinding-Map aufgeloest - Namen duerfen sich nicht aendern.
-keep @interface com.mclone.stbemu.core.stb.StbModelAnnotation
-keep class * implements com.mclone.stbemu.core.stb.FirmwareProfile { *; }

# 4) Native Einstiegspunkte (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
    void JNI_OnLoad();
}

# 5) Room / SQLCipher
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn org.slf4j.**

# 6) Jackson
-keepattributes *Annotation*
-dontwarn com.fasterxml.jackson.databind.ext.**
-keep class com.fasterxml.jackson.databind.** { *; }
-dontwarn java.beans.**

# 7) OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
