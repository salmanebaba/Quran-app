# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Keep your data models!
-keep class com.salmanebaba.quran.QuranResponse { *; }
-keep class com.salmanebaba.quran.QuranDataWrapper { *; }
-keep class com.salmanebaba.quran.Surah { *; }
-keep class com.salmanebaba.quran.Ayah { *; }
-keep class com.salmanebaba.quran.SurahMetadata { *; }
-keep class com.salmanebaba.quran.Bookmark { *; }
-keep class com.salmanebaba.quran.** { *; }
-keepclassmembers class com.salmanebaba.quran.** { <fields>; <init>(...); }

# If you have other models, add them here
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
