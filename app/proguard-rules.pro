# Add project specific ProGuard rules here.

# Keep kotlinx.serialization generated serializers and companions
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.holdup.app.**$$serializer { *; }
-keepclassmembers class com.holdup.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.holdup.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data model classes and enums serialized to DataStore
-keep class com.holdup.app.data.model.** { *; }
-keepclassmembers enum com.holdup.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
