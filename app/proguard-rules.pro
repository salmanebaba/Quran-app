# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Keep your data models! 
# This is likely why reading is empty in the build
-keep class com.example.quran.QuranResponse { *; }
-keep class com.example.quran.QuranDataWrapper { *; }
-keep class com.example.quran.Surah { *; }
-keep class com.example.quran.Ayah { *; }
-keep class com.example.quran.SurahMetadata { *; }

# If you have other models, add them here
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
