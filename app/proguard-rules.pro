-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-keepclassmembernames,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes EnclosingMethod
-keep class com.twort.fuelapp.data.model.** { *; }

# Glance
-keep class androidx.glance.** { *; }
