# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ML Kit common
-keep class com.google.mlkit.common.internal.CommonComponentRegistrar {
    public <init>();
}
-keep class com.google.mlkit.common.sdkinternal.MlKitContext { *; }

# ML Kit Text Recognition
-keep class com.google.mlkit.vision.text.internal.TextRegistrar {
    public <init>();
}
-keep class com.google.mlkit.vision.common.internal.VisionCommonRegistrar {
    public <init>();
}

# General Firebase/ML Kit components
-keep class com.google.firebase.components.ComponentRegistrar
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}

# Keep ML Kit model and internal files
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_**.** { *; }
-dontwarn com.google.android.gms.internal.mlkit_**.**

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
