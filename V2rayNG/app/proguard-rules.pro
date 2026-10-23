# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# The hev-socks5-tunnel native library resolves TProxyService's JNI methods by
# their original names at JNI_OnLoad; R8 must never rename or strip them,
# otherwise the core crashes with UnsatisfiedLinkError when the service starts.
-keep class com.v2ray.ang.service.TProxyService { *; }
-keep class com.v2ray.ang.service.TProxyService$Companion { *; }
-keepclasseswithmembernames class * { native <methods>; }
# Gson-reflected model surface: config generation and MMKV storage deserialize
# these types reflectively. Without keeps + Signature, R8 full mode strips
# constructors / merges classes into abstract forms and Gson fails with
# "Abstract classes can't be instantiated" when building the V2ray config.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.v2ray.ang.dto.** { *; }
