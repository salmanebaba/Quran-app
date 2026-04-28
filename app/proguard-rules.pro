# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Keep your data models!
-keep class com.salmane.quran.QuranResponse { *; }
-keep class com.salmane.quran.QuranDataWrapper { *; }
-keep class com.salmane.quran.Surah { *; }
-keep class com.salmane.quran.Ayah { *; }
-keep class com.salmane.quran.SurahMetadata { *; }
-keep class com.salmane.quran.Bookmark { *; }
-keep class com.salmane.quran.** { *; }
-keepclassmembers class com.salmane.quran.** { <fields>; <init>(...); }

# If you have other models, add them here
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
