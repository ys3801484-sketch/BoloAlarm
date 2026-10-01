# BoloAlarm Production R8 / ProGuard Rules
# Specifically targeted rules without blanket suppressions or broad -dontwarn

# Preserve Room Database, DAOs, and Entities
-keep class com.example.data.model.** { *; }
-keep interface com.example.data.dao.** { *; }
-keep class com.example.data.AppDatabase { *; }
-keep class * extends androidx.room.RoomDatabase

# Preserve Room generated code
-keep class * extends androidx.room.RoomDatabaseComponent
-keep class * extends androidx.room.migration.Migration

# Text to Speech Engine Callbacks and Voice Metadata
-keepclassmembers class * implements android.speech.tts.TextToSpeech$OnInitListener {
    public void onInit(int);
}
-keepclassmembers class * extends android.speech.tts.UtteranceProgressListener {
    public void onStart(java.lang.String);
    public void onDone(java.lang.String);
    public void onError(java.lang.String);
    public void onError(java.lang.String, int);
}

# Preserve Android Lifecycle & ViewModel classes
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}

# Line number table preservation for release crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
